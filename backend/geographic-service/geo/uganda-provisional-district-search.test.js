"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");

const geo = require("./uganda-places-index");
const provisional = require("./uganda-provisional-district-search");

const bounds = {
    south: 1.0,
    west: 33.0,
    north: 1.15,
    east: 33.15
};

const districtAreas = [{
    name: "Testdistrict",
    boundary: "administrative",
    adminLevel: "4",
    osmType: "relation",
    osmId: 12345,
    sourceUrl: "https://www.openstreetmap.org/relation/12345",
    mappedBounds: bounds,
    geometryType: "MultiPolygon",
    hasMappedAreaGeometry: true
}];

function place(id, latitude, longitude) {
    return {
        providerId: `node/${id}`,
        name: `Hospital ${id}`,
        category: "hospital",
        latitude,
        longitude,
        sourceUrl: `https://www.openstreetmap.org/node/${id}`,
        verification: "MAP_LISTED_NOT_INDEPENDENTLY_VERIFIED"
    };
}

function index(places) {
    return {
        places,
        generatedAt: "2026-11-05T00:00:00Z",
        attribution: "(c) OpenStreetMap contributors",
        license: "ODbL"
    };
}

test("splits large bounds into valid search tiles", () => {
    const tiles = provisional.makeTiles(bounds);

    assert.ok(tiles.length > 1);

    for (const tile of tiles) {
        assert.equal(geo.validBounds(tile), true);
    }
});

test("returns provisional map listings", () => {
    const result = provisional.searchProvisional(
        index([
            place(1, 1.02, 33.02),
            place(2, 1.10, 33.10)
        ]),
        districtAreas,
        {
            district: "Testdistrict",
            category: "hospital",
            limit: 10
        }
    );

    assert.equal(result.status, "PROVISIONAL");
    assert.equal(result.places.length, 2);
    assert.equal(result.districtMembershipVerified, false);
    assert.equal(result.verification, "MAP_LISTINGS_ONLY");
});

test("deduplicates records crossing tile boundaries", () => {
    const result = provisional.searchProvisional(
        index([
            place(1, 1.05, 33.05)
        ]),
        districtAreas,
        {
            district: "Testdistrict",
            category: "hospital",
            limit: 10
        }
    );

    assert.equal(result.status, "PROVISIONAL");
    assert.equal(result.places.length, 1);
});

test("does not return truncated results as complete", () => {
    const result = provisional.searchProvisional(
        index([
            place(1, 1.02, 33.02),
            place(2, 1.03, 33.03)
        ]),
        districtAreas,
        {
            district: "Testdistrict",
            category: "hospital",
            limit: 1
        }
    );

    assert.equal(result.status, "INCOMPLETE");
    assert.deepEqual(result.places, []);
});

test("does not invent unknown districts", () => {
    const result = provisional.searchProvisional(
        index([]),
        districtAreas,
        {
            district: "Unknown District",
            category: "hospital",
            limit: 10
        }
    );

    assert.equal(result.status, "DISTRICT_NOT_INDEXED");
    assert.deepEqual(result.places, []);
});

test("rejects unsupported categories", () => {
    const result = provisional.searchProvisional(
        index([]),
        districtAreas,
        {
            district: "Testdistrict",
            category: "invalid",
            limit: 10
        }
    );

    assert.equal(result.status, "INVALID_REQUEST");
});

test("rejects envelopes exceeding tile budget", () => {
    const tiles = provisional.makeTiles({
        south: 0,
        west: 30,
        north: 5,
        east: 35
    });

    assert.equal(tiles, null);
});
