"use strict";

const assert = require("assert");
const fs = require("fs");
const os = require("os");
const path = require("path");

const importer = require("./uganda-geojson-import");
const geo = require("./uganda-places-index");

const DATE = "2026-10-26T12:00:00Z";

function feature(id, amenity = "hospital") {
  return {
    type: "Feature",
    geometry: {
      type: "Point",
      coordinates: [32.57, 0.32]
    },
    properties: {
      osm_type: "node",
      osm_id: id,
      name: `Test Location ${id}`,
      amenity,
      "addr:city": "Kampala"
    }
  };
}

function collection(features) {
  return {
    type: "FeatureCollection",
    features
  };
}

let passed = 0;

function test(name, action) {
  action();
  passed++;
  console.log("PASS " + name);
}

test("named OSM hospital converts correctly", () => {
  const result = importer.convert(
    collection([feature(101)]),
    DATE
  );

  assert.strictEqual(result.acceptedRecords, 1);
  assert.strictEqual(
    result.snapshot.places[0].sourceUrl,
    "https://www.openstreetmap.org/node/101"
  );
  assert.strictEqual(
    result.snapshot.places[0].publicAddress,
    "Kampala"
  );
});

test("recognized categories match index", () => {
  assert.strictEqual(
    importer.categoryOf({ tourism: "hotel" }),
    "hotel"
  );

  assert.strictEqual(
    importer.categoryOf({ amenity: "fuel" }),
    "fuel_station"
  );
});

test("private residences are not imported", () => {
  const privateFeature = feature(102, "residential");

  const result = importer.convert(
    collection([privateFeature]),
    DATE
  );

  assert.strictEqual(result.acceptedRecords, 0);
  assert.strictEqual(result.skippedFeatures, 1);
});

test("unnamed features are skipped", () => {
  const item = feature(103);
  delete item.properties.name;

  const result = importer.convert(
    collection([item]),
    DATE
  );

  assert.strictEqual(result.acceptedRecords, 0);
});

test("duplicate OSM identities fail closed", () => {
  assert.throws(() => importer.convert(
    collection([feature(104), feature(104)]),
    DATE
  ));
});

test("invalid coordinates fail closed", () => {
  const item = feature(105);
  item.geometry.coordinates = [400, 200];

  assert.throws(() => importer.convert(
    collection([item]),
    DATE
  ));
});

test("missing point geometry fails closed", () => {
  const item = feature(106);
  item.geometry = {
    type: "Polygon",
    coordinates: []
  };

  assert.throws(() => importer.convert(
    collection([item]),
    DATE
  ));
});

test("invalid snapshot date is rejected", () => {
  assert.throws(() => importer.convert(
    collection([feature(107)]),
    "unknown"
  ));
});

test("output works in D5F bounded index", () => {
  const converted = importer.convert(
    collection([feature(108)]),
    DATE
  );

  const index = geo.createIndex(converted.snapshot);

  const result = geo.search(index, {
    category: "hospital",
    bounds: {
      south: 0.30,
      west: 32.55,
      north: 0.35,
      east: 32.60
    },
    limit: 10
  });

  assert.strictEqual(result.status, "SUCCESS");
  assert.strictEqual(result.places.length, 1);
});

test("file ingestion requires explicit invocation", () => {
  const folder = fs.mkdtempSync(
    path.join(os.tmpdir(), "jarvis-osm-import-")
  );

  const input = path.join(folder, "input.geojson");
  const output = path.join(folder, "snapshot.json");

  try {
    fs.writeFileSync(
      input,
      JSON.stringify(collection([feature(109)])),
      "utf8"
    );

    const result = importer.importFile(
      input, output, DATE
    );

    assert.strictEqual(result.acceptedRecords, 1);
    assert.strictEqual(
      geo.loadSnapshot(output).places.length,
      1
    );

    assert.throws(() => {
      importer.importFile(input, output, DATE);
    });
  } finally {
    if (fs.existsSync(input)) fs.unlinkSync(input);
    if (fs.existsSync(output)) fs.unlinkSync(output);
    fs.rmdirSync(folder);
  }
});

console.log("");
console.log(`ALL ${passed} OSM INGESTION TESTS PASSED`);