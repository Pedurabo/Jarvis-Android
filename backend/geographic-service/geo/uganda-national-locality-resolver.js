"use strict";

const fs = require("node:fs");
const path = require("node:path");
const express = require("express");

const router = express.Router();

const catalogPath =
    process.env.JARVIS_AREA_CATALOG ||
    path.join(
        __dirname,
        "..",
        "data",
        "offline-import",
        "area-resolution-v1",
        "uganda-area-catalog-v1.json"
    );

function normalize(value) {
    return String(value || "")
        .normalize("NFKC")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

function validArea(area) {
    if (!area || typeof area !== "object") return false;

    if (!["city", "town", "village", "hamlet", "suburb",
           "neighbourhood", "quarter"].includes(area.place)) {
        return false;
    }

    if (!["node", "way", "relation"].includes(area.osmType)) {
        return false;
    }

    const id = String(area.osmId || "");

    if (!/^[1-9][0-9]*$/.test(id)) return false;

    if (area.sourceUrl !==
        `https://www.openstreetmap.org/${area.osmType}/${id}`) {
        return false;
    }

    const point = area.point;

    return point &&
        Number.isFinite(point.latitude) &&
        Number.isFinite(point.longitude) &&
        Math.abs(point.latitude) <= 90 &&
        Math.abs(point.longitude) <= 180 &&
        typeof area.name === "string" &&
        normalize(area.name).length > 0;
}

function buildIndex(document) {
    if (
        document?.schemaVersion !== 1 ||
        !Array.isArray(document.areas)
    ) {
        throw new Error("Invalid national locality catalog");
    }

    const index = new Map();

    for (const area of document.areas) {
        if (!validArea(area)) continue;

        const key = normalize(area.name);
        const identity = `${area.osmType}/${area.osmId}`;

        if (!index.has(key)) {
            index.set(key, new Map());
        }

        index.get(key).set(identity, {
            identity,
            name: area.name,
            place: area.place,
            latitude: area.point.latitude,
            longitude: area.point.longitude,
            sourceUrl: area.sourceUrl,
            geographicStatus:
                "NATIONAL_CATALOG_POINT_UNVERIFIED",
            administrativeMembershipVerified: false,
            coordinateBasis: "OSM_MAPPED_POINT"
        });
    }

    return index;
}

function resolve(index, name) {
    if (
        typeof name !== "string" ||
        name.trim().length < 2 ||
        name.trim().length > 100
    ) {
        return {
            status: "INVALID_REQUEST",
            candidates: []
        };
    }

    const matches = [
        ...(index.get(normalize(name))?.values() || [])
    ];

    const priority = {
        city: 0,
        town: 1,
        village: 2,
        suburb: 3,
        neighbourhood: 4,
        quarter: 5,
        hamlet: 6
    };

    matches.sort((a, b) =>
        priority[a.place] - priority[b.place] ||
        a.identity.localeCompare(b.identity)
    );

    if (!matches.length) {
        return {
            status: "LOCATION_NOT_INDEXED",
            candidates: []
        };
    }

    return {
        status: "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
        candidates: matches.slice(0, 20),
        requiresExplicitSelection: true,
        totalCandidates: matches.length
    };
}

let index = null;

try {
    const document = JSON.parse(
        fs.readFileSync(catalogPath, "utf8")
    );
    index = buildIndex(document);
} catch (error) {
    console.error(
        "National locality catalog unavailable:",
        error.message
    );
}

router.get("/national-locality-resolve", (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");

    if (!index) {
        return res.status(503).json({
            status: "UNAVAILABLE",
            candidates: []
        });
    }

    const result = resolve(index, req.query.name);

    const httpStatus =
        result.status === "INVALID_REQUEST" ? 400 :
        result.status === "LOCATION_NOT_INDEXED" ? 404 :
        200;

    return res.status(httpStatus).json(result);
});

module.exports = { router, buildIndex, resolve };
