"use strict";

/**
 * CDI.6.0-A6.9-D5F
 *
 * Provider-independent Uganda OSM place index foundation.
 *
 * No HTTP server, downloader, live network request,
 * device GPS access, or background refresh is created here.
 *
 * Snapshots must be intentionally supplied by a future,
 * separately validated ingestion process.
 *
 * OSM attribution: (c) OpenStreetMap contributors, ODbL.
 */

const fs = require("fs");

const CATEGORIES = new Set([
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

const TYPES = new Set(["node", "way", "relation"]);

function finite(value) {
  return typeof value === "number" &&
    Number.isFinite(value);
}

function validBounds(b) {
  if (!b || typeof b !== "object") return false;

  const { south, west, north, east } = b;

  if (![south, west, north, east].every(finite)) {
    return false;
  }

  return south >= -90 &&
    north <= 90 &&
    west >= -180 &&
    east <= 180 &&
    south < north &&
    west < east &&
    north - south <= 0.1 + 1e-9 &&
    east - west <= 0.1 + 1e-9;
}

function normalizePlace(raw) {
  if (!raw || typeof raw !== "object") {
    throw new Error("Invalid place record");
  }

  const type = raw.osmType;
  const id = raw.osmId;
  const category = raw.category;

  if (!TYPES.has(type)) {
    throw new Error("Unsupported OSM element type");
  }

  if (!Number.isSafeInteger(id) || id <= 0) {
    throw new Error("Invalid OSM identifier");
  }

  if (!CATEGORIES.has(category)) {
    throw new Error("Unsupported place category");
  }

  if (typeof raw.name !== "string") {
    throw new Error("Missing public place name");
  }

  const name = raw.name
    .replace(/[\u0000-\u001f\u007f-\u009f]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (name.length < 1 || name.length > 180) {
    throw new Error("Invalid place name");
  }

  const latitude = raw.latitude;
  const longitude = raw.longitude;

  if (
    !finite(latitude) ||
    !finite(longitude) ||
    latitude < -90 ||
    latitude > 90 ||
    longitude < -180 ||
    longitude > 180
  ) {
    throw new Error("Invalid coordinates");
  }

  const canonicalUrl =
    `https://www.openstreetmap.org/${type}/${id}`;

  // The source URL must identify the same OSM record.
  // No arbitrary URLs or private listing URLs.
  if (raw.sourceUrl !== canonicalUrl) {
    throw new Error("Noncanonical OSM source URL");
  }

  let publicAddress = null;

  if (raw.publicAddress != null) {
    if (
      typeof raw.publicAddress !== "string" ||
      raw.publicAddress.length > 250
    ) {
      throw new Error("Invalid published address");
    }

    publicAddress =
      raw.publicAddress.replace(
        /[\u0000-\u001f\u007f-\u009f]/g, " "
      ).trim() || null;
  }

  return Object.freeze({
    providerId: `${type}/${id}`,
    name,
    category,
    latitude,
    longitude,
    publicAddress,
    sourceUrl: canonicalUrl,
    sourceKind: "OPENSTREETMAP",
    verification: "MAP_LISTED_NOT_INDEPENDENTLY_VERIFIED"
  });
}

function createIndex(snapshot) {
  if (!snapshot || typeof snapshot !== "object") {
    throw new Error("Missing snapshot");
  }

  if (snapshot.schemaVersion !== 1) {
    throw new Error("Unsupported snapshot schema");
  }

  const generatedAt = snapshot.generatedAt;

  if (
    typeof generatedAt !== "string" ||
    !/^\d{4}-\d{2}-\d{2}T/.test(generatedAt) ||
    !Number.isFinite(Date.parse(generatedAt))
  ) {
    throw new Error("Missing snapshot timestamp");
  }

  if (
    !Array.isArray(snapshot.places) ||
    snapshot.places.length > 500000
  ) {
    throw new Error("Invalid snapshot collection");
  }

  const seen = new Set();
  const places = [];

  for (const raw of snapshot.places) {
    const place = normalizePlace(raw);

    if (seen.has(place.providerId)) {
      throw new Error("Duplicate OSM record");
    }

    seen.add(place.providerId);
    places.push(place);
  }

  return Object.freeze({
    generatedAt,
    attribution: "(c) OpenStreetMap contributors",
    license: "ODbL",
    places: Object.freeze(places)
  });
}

function loadSnapshot(filename) {
  const stat = fs.statSync(filename);

  if (!stat.isFile() || stat.size > 64 * 1024 * 1024) {
    throw new Error("Snapshot file exceeds configured limit");
  }

  const source = fs.readFileSync(filename, "utf8");
  return createIndex(JSON.parse(source));
}

function search(index, options) {
  if (
    !index ||
    !Array.isArray(index.places) ||
    !options ||
    !validBounds(options.bounds) ||
    !CATEGORIES.has(options.category) ||
    !Number.isInteger(options.limit) ||
    options.limit < 1 ||
    options.limit > 50
  ) {
    throw new Error("Invalid geographic search request");
  }

  if (options.query != null || options.pageToken != null) {
    throw new Error("Unsupported search or pagination");
  }

  const b = options.bounds;
  const results = [];

  for (const place of index.places) {
    if (place.category !== options.category) continue;

    if (
      place.latitude < b.south ||
      place.latitude > b.north ||
      place.longitude < b.west ||
      place.longitude > b.east
    ) {
      continue;
    }

    results.push(place);

    // One extra record detects incomplete coverage of
    // the requested result window.
    if (results.length > options.limit) break;
  }

  if (results.length > options.limit) {
    return {
      status: "INCOMPLETE",
      places: [],
      message: "Narrow the search area.",
      snapshotGeneratedAt: index.generatedAt,
      attribution: index.attribution
    };
  }

  return {
    status: "SUCCESS",
    places: results,
    snapshotGeneratedAt: index.generatedAt,
    attribution: index.attribution,
    license: index.license,
    verification: "MAP_LISTINGS_ONLY"
  };
}

module.exports = {
  CATEGORIES,
  validBounds,
  normalizePlace,
  createIndex,
  loadSnapshot,
  search
};