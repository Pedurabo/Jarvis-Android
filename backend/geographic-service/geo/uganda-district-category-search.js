"use strict";

const geo = require("./uganda-places-index");
const resolver = require("./uganda-district-resolver");
const boundaryLoader = require("./uganda-district-boundary-loader");

function districtCategorySearch({
    districtAreas,
    boundaryRoot,
    manifest,
    index,
    districtName,
    category,
    limit = 10
}) {
    if (
        typeof districtName !== "string" ||
        !districtName.trim() ||
        typeof category !== "string" ||
        !geo.CATEGORIES.has(category) ||
        !Number.isInteger(limit) ||
        limit < 1 ||
        limit > 50
    ) {
        return { status: "INVALID_REQUEST", places: [] };
    }

    const resolution = resolver.resolveDistrict(
        districtAreas,
        districtName
    );

    if (resolution.status !== "RESOLVED") {
        return {
            status: resolution.status,
            places: [],
            candidates: resolution.candidates || []
        };
    }

    const district = resolution.district;

    const boundary = boundaryLoader.loadBoundary(
        boundaryRoot,
        manifest,
        district
    );

    if (boundary.status !== "READY") {
        return {
            status: "UNVERIFIED",
            district,
            boundaryStatus: boundary.status,
            places: []
        };
    }

    if (!index || !Array.isArray(index.places)) {
        return {
            status: "UNAVAILABLE",
            district,
            places: []
        };
    }

    const results = [];

    for (const place of index.places) {
        if (place.category !== category) continue;

        const classification =
            boundaryLoader.classifyDistrictPoint(
                boundary,
                place.longitude,
                place.latitude
            );

        if (classification === "UNVERIFIED") {
            return {
                status: "UNVERIFIED",
                district,
                places: []
            };
        }

        // Include boundary points as well as interior points.
        if (
            classification !== "INSIDE" &&
            classification !== "BOUNDARY"
        ) {
            continue;
        }

        results.push(place);

        if (results.length > limit) {
            return {
                status: "INCOMPLETE",
                district,
                places: [],
                message:
                    "More matching places exist; refine the request.",
                snapshotGeneratedAt: index.generatedAt,
                attribution: index.attribution
            };
        }
    }

    return {
        status: "SUCCESS",
        district,
        places: results,
        snapshotGeneratedAt: index.generatedAt,
        attribution: index.attribution,
        license: index.license,
        verification: "MAP_LISTINGS_ONLY"
    };
}

module.exports = { districtCategorySearch };
