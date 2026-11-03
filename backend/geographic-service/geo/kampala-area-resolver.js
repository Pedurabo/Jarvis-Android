"use strict";

const fs = require("node:fs");
const path = require("node:path");
const express = require("express");

const router = express.Router();

const DEFAULT_CATALOG = path.join(
    __dirname,
    "..",
    "data",
    "offline-import",
    "area-resolution-v1",
    "uganda-area-catalog-v1.json"
);

const CATALOG_PATH =
    process.env.JARVIS_AREA_CATALOG || DEFAULT_CATALOG;

const MAX_CONTEXT_KM = 40;
const APPROX_RADIUS_KM = 1;

function normalize(value) {
    return String(value || "")
        .normalize("NFKC")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

function validPoint(point) {
    return point &&
        Number.isFinite(point.latitude) &&
        Number.isFinite(point.longitude) &&
        point.latitude >= -90 &&
        point.latitude <= 90 &&
        point.longitude >= -180 &&
        point.longitude <= 180;
}

function validBounds(bounds) {
    return bounds &&
        ["south", "west", "north", "east"]
            .every(key => Number.isFinite(bounds[key])) &&
        bounds.south >= -90 &&
        bounds.north <= 90 &&
        bounds.west >= -180 &&
        bounds.east <= 180 &&
        bounds.south < bounds.north &&
        bounds.west < bounds.east;
}

function validIdentity(area) {
    if (!area || typeof area !== "object") return false;

    if (!["node", "way", "relation"]
        .includes(area.osmType)) return false;

    const id = String(area.osmId || "");

    return /^[1-9][0-9]*$/.test(id) &&
        area.sourceUrl ===
        `https://www.openstreetmap.org/${area.osmType}/${id}`;
}

function positionOf(area) {
    if (validPoint(area.point)) {
        return area.point;
    }

    if (validBounds(area.mappedBounds)) {
        const b = area.mappedBounds;

        return {
            latitude: (b.south + b.north) / 2,
            longitude: (b.west + b.east) / 2
        };
    }

    return null;
}

function distanceKm(a, b) {
    const rad = Math.PI / 180;

    const dLat = (b.latitude - a.latitude) * rad;
    const dLon = (b.longitude - a.longitude) * rad;

    const h =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(a.latitude * rad) *
        Math.cos(b.latitude * rad) *
        Math.sin(dLon / 2) ** 2;

    return 6371.0088 * 2 *
        Math.asin(Math.min(1, Math.sqrt(h)));
}

function approximateBounds(point) {
    const latDelta = APPROX_RADIUS_KM / 111.195;

    const lonDelta = APPROX_RADIUS_KM / (
        111.195 *
        Math.cos(point.latitude * Math.PI / 180)
    );

    return {
        south: point.latitude - latDelta,
        west: point.longitude - lonDelta,
        north: point.latitude + latDelta,
        east: point.longitude + lonDelta
    };
}

function rank(area) {
    if (["neighbourhood", "suburb", "quarter"]
        .includes(area.place)) return 3;

    if (["city", "town"]
        .includes(area.place)) return 2;

    if (area.boundary === "administrative") {
        return 2;
    }

    if (area.place === "village") return 1;

    return 0;
}

function loadCatalog() {
    const stat = fs.statSync(CATALOG_PATH);

    if (!stat.isFile() ||
        stat.size <= 0 ||
        stat.size > 64 * 1024 * 1024) {
        throw new Error("Invalid catalog size");
    }

    const document = JSON.parse(
        fs.readFileSync(CATALOG_PATH, "utf8")
    );

    if (
        document.schemaVersion !== 1 ||
        !Array.isArray(document.areas) ||
        document.areas.length === 0
    ) {
        throw new Error("Invalid geographic catalog");
    }

    const areas = document.areas.filter(validIdentity);

    const anchor = areas.find(area =>
        area.osmType === "node" &&
        String(area.osmId) === "3500961529" &&
        normalize(area.name) === "ntinda" &&
        area.place === "suburb" &&
        validPoint(area.point)
    );

    if (!anchor) {
        throw new Error("Kampala geographic reference missing");
    }

    return {
        areas,
        reference: anchor.point
    };
}

let catalog = null;
let startupError = null;

try {
    catalog = loadCatalog();
} catch (error) {
    startupError = error.message;
}

function resolve(name, context) {
    if (!catalog) {
        return {
            status: "UNAVAILABLE",
            searchBoundsReady: false
        };
    }

    if (
        typeof name !== "string" ||
        name.trim().length < 2 ||
        name.trim().length > 100
    ) {
        return {
            status: "INVALID_REQUEST",
            searchBoundsReady: false
        };
    }

    if (normalize(context) !== "kampala") {
        return {
            status: "UNSUPPORTED_CONTEXT",
            searchBoundsReady: false
        };
    }

    const query = normalize(name);
    const matches = new Map();

    for (const area of catalog.areas) {
        if (normalize(area.name) !== query) continue;

        const position = positionOf(area);

        if (!position) continue;

        const distance = distanceKm(
            catalog.reference,
            position
        );

        if (distance > MAX_CONTEXT_KM) continue;

        const identity = `${area.osmType}/${area.osmId}`;

        const mappedBounds =
            ["Polygon", "MultiPolygon"]
                .includes(area.geometryType) &&
            validBounds(area.mappedBounds)
                ? area.mappedBounds
                : null;

        const candidate = {
            name: area.name,
            place: area.place || null,
            boundary: area.boundary || null,
            osmType: area.osmType,
            osmId: String(area.osmId),
            sourceUrl: area.sourceUrl,
            geometryType: area.geometryType,
            position,
            distanceKm: Number(distance.toFixed(3)),
            rank: rank(area),
            mappedBounds
        };

        const previous = matches.get(identity);

        if (
            !previous ||
            (!previous.mappedBounds && mappedBounds)
        ) {
            matches.set(identity, candidate);
        }
    }

    const candidates = [...matches.values()]
        .sort((a, b) =>
            b.rank - a.rank ||
            a.distanceKm - b.distanceKm
        );

    if (candidates.length === 0) {
        return {
            status: "NOT_RESOLVED",
            query: name,
            candidates: [],
            searchBoundsReady: false
        };
    }

    const selected = candidates[0];

    const equalRank = candidates.filter(
        candidate => candidate.rank === selected.rank
    );

    if (
        equalRank.length !== 1 ||
        selected.rank < 2
    ) {
        return {
            status: "AMBIGUOUS",
            query: name,
            candidates,
            searchBoundsReady: false
        };
    }

    const searchBounds = selected.mappedBounds ||
        approximateBounds(selected.position);

    const coverage = selected.mappedBounds
        ? "MAPPED_POLYGON_ENVELOPE"
        : "APPROXIMATE_POINT_RADIUS_1KM";

    if (
        !validBounds(searchBounds) ||
        searchBounds.north - searchBounds.south > 0.1 ||
        searchBounds.east - searchBounds.west > 0.1
    ) {
        return {
            status: "BOUNDS_REQUIRE_SUBDIVISION",
            query: name,
            selected,
            candidates,
            searchBoundsReady: false
        };
    }

    return {
        status: "RESOLVED",
        query: name,
        context: "Kampala",
        selected,
        candidates,
        searchBoundsReady: true,
        searchBounds,
        coverage,
        boundaryVerified: false,
        verification: "OSM_MAP_DATA_NOT_INDEPENDENTLY_VERIFIED",
        limitations: [
            "Point-based search bounds are approximate.",
            "A 1 km search radius is not a neighborhood boundary.",
            "OSM listings do not establish current business operation."
        ]
    };
}

router.get("/area-resolve", (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");

    if (startupError || !catalog) {
        return res.status(503).json({
            status: "UNAVAILABLE",
            message: "Geographic area catalog unavailable."
        });
    }

    const result = resolve(
        req.query.name,
        req.query.context
    );

    return res.status(
        result.status === "INVALID_REQUEST" ? 400 : 200
    ).json(result);
});

module.exports = {
    router,
    resolve
};