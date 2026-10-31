"use strict";

const express = require("express");
const geo = require("./uganda-places-index");

const router = express.Router();

const ALLOWED_CATEGORIES = new Set([
    "hospital",
    "clinic",
    "pharmacy",
    "school",
    "university",
    "bank",
    "fuel_station",
    "hotel",
    "guest_house",
    "police",
    "embassy"
]);

const SNAPSHOT_PATH =
    process.env.JARVIS_GEO_SNAPSHOT || "";

let index = null;
let startupError = null;

if (SNAPSHOT_PATH) {
    try {
        index = geo.loadSnapshot(SNAPSHOT_PATH);

        if (
            !index ||
            !Array.isArray(index.places) ||
            typeof index.generatedAt !== "string"
        ) {
            throw new Error("Invalid loaded geographic index");
        }
    } catch (_) {
        startupError = "Snapshot could not be loaded";
    }
}

function parseCoordinate(value) {
    if (
        typeof value !== "string" ||
        value.trim() === ""
    ) {
        return null;
    }

    const trimmed = value.trim();

    if (
        !/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(trimmed)
    ) {
        return null;
    }

    const number = Number(trimmed);

    return Number.isFinite(number) ? number : null;
}

function makeBounds(south, west, north, east) {
    const candidates = [
        {
            minLat: south,
            minLon: west,
            maxLat: north,
            maxLon: east
        },
        {
            south,
            west,
            north,
            east
        },
        {
            minLatitude: south,
            minLongitude: west,
            maxLatitude: north,
            maxLongitude: east
        }
    ];

    for (const candidate of candidates) {
        if (geo.validBounds(candidate)) {
            return candidate;
        }
    }

    return null;
}

router.get("/search", (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");

    if (!index) {
        return res.status(503).json({
            status: "UNAVAILABLE",
            message: "Geographic data is not configured.",
            verification: "MAP_LISTINGS_ONLY"
        });
    }

    const category = req.query.category;

    if (
        typeof category !== "string" ||
        !ALLOWED_CATEGORIES.has(category)
    ) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "Unsupported place category."
        });
    }

    const south = parseCoordinate(req.query.south);
    const west = parseCoordinate(req.query.west);
    const north = parseCoordinate(req.query.north);
    const east = parseCoordinate(req.query.east);

    if ([south, west, north, east].some(v => v === null)) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "Four valid geographic coordinates are required."
        });
    }

    const bounds = makeBounds(south, west, north, east);

    if (!bounds) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "Invalid or oversized geographic bounds."
        });
    }

    const rawLimit = req.query.limit;

    let limit = 20;

    if (rawLimit !== undefined) {
        if (
            typeof rawLimit !== "string" ||
            !/^[1-9]\d*$/.test(rawLimit)
        ) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Invalid result limit."
            });
        }

        limit = Number(rawLimit);

        if (!Number.isSafeInteger(limit) || limit > 50) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Limit must be between 1 and 50."
            });
        }
    }

    try {
        const result = geo.search(index, {
            bounds,
            category,
            limit
        });

        if (
            !result ||
            !["SUCCESS", "INCOMPLETE"].includes(result.status)
        ) {
            throw new Error("Unexpected geographic search result");
        }

        return res.status(200).json(result);
    } catch (_) {
        return res.status(500).json({
            status: "UNAVAILABLE",
            message: "Geographic search failed."
        });
    }
});

module.exports = router;