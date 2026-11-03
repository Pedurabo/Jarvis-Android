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
    "embassy",
    // JARVIS-EXPANDED-CATEGORIES-V2
    "apartment",
    "attraction",
    "bar",
    "cafe",
    "convenience_shop",
    "department_store",
    "fast_food",
    "fitness_centre",
    "hostel",
    "motel",
    "nightclub",
    "pub",
    "restaurant",
    "shopping_mall",
    "sports_centre",
    "supermarket"
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

// JARVIS-GEO-NAME-SEARCH-V1
// Matches the existing Android provider request contract.
router.get("/name-search", (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");

    if (!index) {
        return res.status(503).json({
            status: "UNAVAILABLE",
            message: "Geographic data is not configured.",
            verification: "MAP_LISTINGS_ONLY"
        });
    }

    const rawName = req.query.query;

    if (
        typeof rawName !== "string" ||
        rawName.trim().length < 2 ||
        rawName.trim().length > 120
    ) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "A public place name is required."
        });
    }

    const category = req.query.category;

    if (
        category !== undefined &&
        (
            typeof category !== "string" ||
            !ALLOWED_CATEGORIES.has(category)
        )
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

    // JARVIS-NATIONAL-NAME-SEARCH-V1
    // All four coordinates or none. Partial bounds are invalid.
    const supplied = [
        req.query.south,
        req.query.west,
        req.query.north,
        req.query.east
    ].filter(value => value !== undefined).length;

    if (supplied !== 0 && supplied !== 4) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "Provide all four coordinates or none."
        });
    }

    let bounds = null;

    if (supplied === 4) {
        if ([south, west, north, east].some(v => v === null)) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Invalid geographic coordinates."
            });
        }

        bounds = makeBounds(south, west, north, east);

        if (!bounds) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Invalid or oversized geographic bounds."
            });
        }
    }

    let limit = 20;

    if (req.query.limit !== undefined) {
        if (
            typeof req.query.limit !== "string" ||
            !/^[1-9]\d*$/.test(req.query.limit)
        ) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Invalid result limit."
            });
        }

        limit = Number(req.query.limit);

        if (!Number.isSafeInteger(limit) || limit > 50) {
            return res.status(400).json({
                status: "INVALID_REQUEST",
                message: "Limit must be between 1 and 50."
            });
        }
    }

    try {
        const result = geo.nameSearch(index, {
            query: rawName,
            category,
            bounds,
            limit
        });

        return res.status(200).json(result);
    } catch (_) {
        return res.status(500).json({
            status: "UNAVAILABLE",
            message: "Geographic name search failed."
        });
    }
});

const path = require("path");
const districtResolver = require("./uganda-district-resolver");

const DISTRICT_CATALOG_PATH = path.join(
    __dirname,
    "..",
    "data",
    "offline-import",
    "area-resolution-v1",
    "uganda-area-catalog-v1.json"
);

let districtAreas = null;

try {
    districtAreas = districtResolver.loadDistrictCatalog(
        DISTRICT_CATALOG_PATH
    );
} catch (_) {
    districtAreas = null;
}

// JARVIS-P2D1-DISTRICT-RESOLUTION
router.get("/district-resolve", (req, res) => {
    const result = districtResolver.resolveDistrict(
        districtAreas,
        req.query.name
    );

    const statusCode =
        result.status === "INVALID_REQUEST" ? 400 :
        result.status === "UNAVAILABLE" ? 503 :
        200;

    return res.status(statusCode).json(result);
});


// JARVIS-P2D3-DISTRICT-CATEGORY-SEARCH
const fs = require("node:fs");
const districtCategoryService = require("./uganda-district-category-search");

const DISTRICT_BOUNDARY_ROOT = path.join(
    __dirname, "..", "data", "district-boundaries-pilot"
);

let districtBoundaryManifest = null;

try {
    districtBoundaryManifest = JSON.parse(
        fs.readFileSync(
            path.join(DISTRICT_BOUNDARY_ROOT, "manifest.json"),
            "utf8"
        )
    );
} catch (_) {
    districtBoundaryManifest = null;
}

router.get("/district-category-search", (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");

    const rawLimit = req.query.limit;
    const limit = rawLimit === undefined
        ? 10
        : (
            typeof rawLimit === "string" &&
            /^[1-9][0-9]*$/.test(rawLimit)
                ? Number(rawLimit)
                : NaN
        );

    const result = districtCategoryService.districtCategorySearch({
        districtAreas,
        boundaryRoot: DISTRICT_BOUNDARY_ROOT,
        manifest: districtBoundaryManifest,
        index,
        districtName: req.query.district,
        category: req.query.category,
        limit
    });

    const statusCode =
        result.status === "INVALID_REQUEST" ? 400 :
        result.status === "UNAVAILABLE" ? 503 :
        200;

    return res.status(statusCode).json(result);
});

module.exports = router;
