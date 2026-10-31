"use strict";

const assert = require("assert");
const fs = require("fs");
const os = require("os");
const path = require("path");

const geo = require("./uganda-places-index");

const bounds = {
  south: 0.30,
  west: 32.55,
  north: 0.35,
  east: 32.60
};

function place(id, category = "hospital") {
  return {
    osmType: "node",
    osmId: id,
    name: `Example ${id}`,
    category,
    latitude: 0.32,
    longitude: 32.57,
    publicAddress: null,
    sourceUrl:
      `https://www.openstreetmap.org/node/${id}`
  };
}

function snapshot(places) {
  return {
    schemaVersion: 1,
    generatedAt: "2026-10-26T00:00:00Z",
    places
  };
}

function query(index, options = {}) {
  return geo.search(index, Object.assign({
    category: "hospital",
    bounds,
    limit: 10
  }, options));
}

let passed = 0;

function test(name, action) {
  action();
  passed++;
  process.stdout.write(`PASS ${name}\n`);
}

test("valid bounds accepted", () => {
  assert.strictEqual(geo.validBounds(bounds), true);
});

test("oversized bounds rejected", () => {
  assert.strictEqual(
    geo.validBounds({
      south: 0,
      west: 32,
      north: 1,
      east: 33
    }),
    false
  );
});

test("public OSM record is attributed", () => {
  const index = geo.createIndex(
    snapshot([place(123)])
  );

  const result = query(index);

  assert.strictEqual(result.status, "SUCCESS");
  assert.strictEqual(result.places.length, 1);
  assert.strictEqual(
    result.places[0].sourceUrl,
    "https://www.openstreetmap.org/node/123"
  );
  assert.strictEqual(
    result.places[0].verification,
    "MAP_LISTED_NOT_INDEPENDENTLY_VERIFIED"
  );
});

test("unknown place category is rejected", () => {
  assert.throws(() => {
    geo.createIndex(
      snapshot([place(123, "private_residence")])
    );
  });
});

test("noncanonical URL rejected", () => {
  const record = place(123);
  record.sourceUrl = "https://untrusted.example/123";

  assert.throws(() => {
    geo.createIndex(snapshot([record]));
  });
});

test("duplicate OSM identifiers rejected", () => {
  assert.throws(() => {
    geo.createIndex(
      snapshot([place(123), place(123)])
    );
  });
});

test("empty result does not invent records", () => {
  const index = geo.createIndex(snapshot([]));

  const result = query(index);

  assert.strictEqual(result.status, "SUCCESS");
  assert.strictEqual(result.places.length, 0);
});

test("more than limit reports incomplete", () => {
  const records = [];

  for (let id = 1; id <= 11; id++) {
    records.push(place(id));
  }

  const index = geo.createIndex(snapshot(records));

  const result = query(index);

  assert.strictEqual(result.status, "INCOMPLETE");
  assert.strictEqual(result.places.length, 0);
});

test("different category does not leak results", () => {
  const index = geo.createIndex(
    snapshot([place(123, "school")])
  );

  assert.strictEqual(
    query(index).places.length,
    0
  );
});

test("free-text and pagination are not invented", () => {
  const index = geo.createIndex(snapshot([]));

  assert.throws(() => query(index, {
    query: "hospital"
  }));

  assert.throws(() => query(index, {
    pageToken: "next"
  }));
});

test("snapshot loads only when explicitly requested", () => {
  const folder = fs.mkdtempSync(
    path.join(os.tmpdir(), "jarvis-geo-test-")
  );

  const filename = path.join(folder, "snapshot.json");

  try {
    fs.writeFileSync(
      filename,
      JSON.stringify(snapshot([place(123)])),
      "utf8"
    );

    const index = geo.loadSnapshot(filename);

    assert.strictEqual(index.places.length, 1);
  } finally {
    try { fs.unlinkSync(filename); } catch (_) {}
    try { fs.rmdirSync(folder); } catch (_) {}
  }
});

test("malformed snapshot fails closed", () => {
  assert.throws(() => geo.createIndex({
    schemaVersion: 9,
    generatedAt: "invalid",
    places: []
  }));
});

console.log("");
console.log(`ALL ${passed} GEO INDEX TESTS PASSED`);