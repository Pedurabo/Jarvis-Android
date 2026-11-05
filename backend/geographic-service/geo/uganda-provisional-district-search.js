"use strict";

const geo = require("./uganda-places-index");
const resolver = require("./uganda-district-resolver");

const TILE_SIZE = 0.05;
const MAX_TILES = 500;
const MAX_RESULTS = 50;
const EPSILON = 1e-10;

function validEnvelope(bounds) {
    return bounds &&
        ["south", "west", "north", "east"].every(
            key => typeof bounds[key] === "number" &&
                Number.isFinite(bounds[key])
        ) &&
        bounds.south >= -90 &&
        bounds.north <= 90 &&
        bounds.west >= -180 &&
        bounds.east <= 180 &&
        bounds.south < bounds.north &&
        bounds.west < bounds.east;
}

function makeTiles(bounds) {
    if (!validEnvelope(bounds)) {
        return null;
    }

    const rows = Math.ceil(
        (bounds.north - bounds.south) / TILE_SIZE
    );

    const columns = Math.ceil(
        (bounds.east - bounds.west) / TILE_SIZE
    );

    if (
        rows < 1 ||
        columns < 1 ||
        rows * columns > MAX_TILES
    ) {
        return null;
    }

    const tiles = [];

    for (let row = 0; row < rows; row++) {
        for (let column = 0; column < columns; column++) {
            const south = bounds.south + row * TILE_SIZE;
            const west = bounds.west + column * TILE_SIZE;

            const tile = {
                south,
                west,
                north: Math.min(
                    bounds.north,
                    bounds.south + (row + 1) * TILE_SIZE
                ),
                east: Math.min(
                    bounds.east,
                    bounds.west + (column + 1) * TILE_SIZE
                )
            };

            if (!geo.validBounds(tile)) {
                return null;
            }

            tiles.push(tile);
        }
    }

    return tiles;
}

function searchProvisional(index, districtAreas, options) {
    if (
        !index ||
        !Array.isArray(index.places) ||
        !options ||
        typeof options.district !== "string" ||
        !geo.CATEGORIES.has(options.category)
    ) {
        return {
            status: "INVALID_REQUEST",
            places: []
        };
    }

    const limit = options.limit === undefined
        ? 20
        : options.limit;

    if (
        !Number.isInteger(limit) ||
        limit < 1 ||
        limit > MAX_RESULTS
    ) {
        return {
            status: "INVALID_REQUEST",
            places: []
        };
    }

    const resolved = resolver.resolveDistrict(
        districtAreas,
        options.district
    );

    if (resolved.status !== "RESOLVED") {
        return {
            status: resolved.status,
            candidates: resolved.candidates || [],
            places: []
        };
    }

    const district = resolved.district;
    const bounds = district.mappedBounds;

    const tiles = makeTiles(bounds);

    if (!tiles) {
        return {
            status: "UNAVAILABLE",
            district,
            reason: "DISTRICT_ENVELOPE_UNSUPPORTED",
            places: []
        };
    }

    const unique = new Map();

    for (const tile of tiles) {
        const result = geo.search(index, {
            bounds: tile,
            category: options.category,
            limit: MAX_RESULTS
        });

        if (result.status !== "SUCCESS") {
            return {
                status: "INCOMPLETE",
                district,
                reason: "SEARCH_WINDOW_INCOMPLETE",
                districtMembershipVerified: false,
                places: []
            };
        }

        for (const place of result.places) {
            if (
                !place ||
                typeof place.providerId !== "string"
            ) {
                return {
                    status: "UNAVAILABLE",
                    district,
                    reason: "INVALID_PLACE_RECORD",
                    places: []
                };
            }

            if (
                place.latitude < bounds.south - EPSILON ||
                place.latitude > bounds.north + EPSILON ||
                place.longitude < bounds.west - EPSILON ||
                place.longitude > bounds.east + EPSILON
            ) {
                continue;
            }

            unique.set(place.providerId, place);

            if (unique.size > limit) {
                return {
                    status: "INCOMPLETE",
                    district,
                    reason: "RESULT_LIMIT_EXCEEDED",
                    districtMembershipVerified: false,
                    places: []
                };
            }
        }
    }

    return {
        status: "PROVISIONAL",
        district,
        searchType: "DISTRICT_BOUNDING_BOX",
        districtMembershipVerified: false,
        boundaryVerified: false,
        verification: "MAP_LISTINGS_ONLY",
        message:
            "These are map-listed places within the district's " +
            "mapped bounding box. District membership and " +
            "individual listings have not been independently verified.",
        places: [...unique.values()],
        snapshotGeneratedAt: index.generatedAt,
        attribution: index.attribution,
        license: index.license
    };
}

module.exports = {
    makeTiles,
    searchProvisional
};
