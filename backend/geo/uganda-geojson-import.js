"use strict";

/**
 * CDI.6.0-A6.9-D5G
 *
 * Explicit offline ingestion of OSM-derived GeoJSON.
 *
 * This does NOT read PBF files or download OSM data.
 * The caller must supply a trusted GeoJSON export
 * and its actual extraction timestamp.
 *
 * All emitted locations remain map-listed, not
 * independently verified institutions.
 *
 * Attribution: (c) OpenStreetMap contributors, ODbL.
 */

const fs = require("fs");
const path = require("path");
const geo = require("./uganda-places-index");

const MAX_INPUT_BYTES = 32 * 1024 * 1024;
const MAX_FEATURES = 100000;

const TAG_CATEGORIES = Object.freeze([
  ["amenity", "hospital", "hospital"],
  ["amenity", "clinic", "clinic"],
  ["amenity", "pharmacy", "pharmacy"],
  ["amenity", "school", "school"],
  ["amenity", "university", "university"],
  ["amenity", "bank", "bank"],
  ["amenity", "fuel", "fuel_station"],
  ["amenity", "police", "police"],
  ["tourism", "hotel", "hotel"],
  ["tourism", "guest_house", "guest_house"],
  ["office", "diplomatic", "embassy"]
]);

function categoryOf(tags) {
  if (!tags || typeof tags !== "object") {
    return null;
  }

  for (const [key, value, category] of TAG_CATEGORIES) {
    if (tags[key] === value) return category;
  }

  return null;
}

function osmIdentity(feature) {
  const tags = feature.properties;

  // Require a structured OSM identifier.
  // Never derive identity from a feature's array index.
  const rawType = tags.osm_type;
  const rawId = tags.osm_id;

  if (
    !["node", "way", "relation"].includes(rawType) ||
    !Number.isSafeInteger(rawId) ||
    rawId <= 0
  ) {
    throw new Error("Missing valid OSM identity");
  }

  return {
    osmType: rawType,
    osmId: rawId
  };
}

function extractAddress(tags) {
  const parts = [
    tags["addr:housenumber"],
    tags["addr:street"],
    tags["addr:city"]
  ].filter(value =>
    typeof value === "string" &&
    value.trim().length > 0
  );

  const address = parts.join(", ");

  return address.length > 0 ? address : null;
}

function featureToPlace(feature) {
  if (!feature || feature.type !== "Feature") {
    throw new Error("Invalid GeoJSON feature");
  }

  const tags = feature.properties;

  if (!tags || typeof tags !== "object") {
    throw new Error("Missing OSM tags");
  }

  const category = categoryOf(tags);

  // Unsupported categories are deliberately not imported.
  if (!category) return null;

  if (
    typeof tags.name !== "string" ||
    tags.name.trim().length === 0
  ) {
    // Unnamed OSM features are not presented as businesses.
    return null;
  }

  // Only preprocessed point or representative-point
  // features are accepted.
  //
  // A future PBF converter must explicitly calculate
  // representative points for ways and relations.
  if (
    !feature.geometry ||
    feature.geometry.type !== "Point" ||
    !Array.isArray(feature.geometry.coordinates) ||
    feature.geometry.coordinates.length !== 2
  ) {
    throw new Error("Named supported feature lacks point geometry");
  }

  const [longitude, latitude] =
    feature.geometry.coordinates;

  const identity = osmIdentity(feature);

  return {
    ...identity,
    name: tags.name,
    category,
    latitude,
    longitude,
    publicAddress: extractAddress(tags),
    sourceUrl:
      `https://www.openstreetmap.org/` +
      `${identity.osmType}/${identity.osmId}`
  };
}

function convert(collection, generatedAt) {
  if (
    !collection ||
    collection.type !== "FeatureCollection" ||
    !Array.isArray(collection.features) ||
    collection.features.length > MAX_FEATURES
  ) {
    throw new Error("Invalid or oversized GeoJSON collection");
  }

  if (
    typeof generatedAt !== "string" ||
    !Number.isFinite(Date.parse(generatedAt)) ||
    !/^\d{4}-\d{2}-\d{2}T/.test(generatedAt)
  ) {
    throw new Error("An explicit extraction timestamp is required");
  }

  const places = [];

  for (const feature of collection.features) {
    const place = featureToPlace(feature);

    if (place !== null) {
      places.push(place);
    }
  }

  const snapshot = {
    schemaVersion: 1,
    generatedAt,
    places
  };

  // Reuse the validated D5F contract, not a parallel
  // set of looser ingestion rules.
  const index = geo.createIndex(snapshot);

  return Object.freeze({
    snapshot,
    acceptedRecords: index.places.length,
    inputFeatures: collection.features.length,
    skippedFeatures:
      collection.features.length - index.places.length
  });
}

function importFile(input, output, generatedAt) {
  const inputPath = path.resolve(input);
  const outputPath = path.resolve(output);

  if (inputPath === outputPath) {
    throw new Error("Input and output must differ");
  }

  if (fs.existsSync(outputPath)) {
    throw new Error("Refusing to overwrite existing snapshot");
  }

  const stat = fs.statSync(inputPath);

  if (
    !stat.isFile() ||
    stat.size > MAX_INPUT_BYTES
  ) {
    throw new Error("Input file exceeds size limit");
  }

  const collection = JSON.parse(
    fs.readFileSync(inputPath, "utf8")
  );

  const converted = convert(collection, generatedAt);
  const serialized =
    JSON.stringify(converted.snapshot) + "\n";

  if (
    Buffer.byteLength(serialized, "utf8") >
    64 * 1024 * 1024
  ) {
    throw new Error("Output snapshot exceeds size limit");
  }

  // Exclusive creation prevents accidental overwrite.
  // A failed write removes only the file created here.
  let descriptor = null;

  try {
    descriptor = fs.openSync(outputPath, "wx", 0o600);
    fs.writeFileSync(descriptor, serialized, "utf8");
    fs.closeSync(descriptor);
    descriptor = null;
  } catch (error) {
    if (descriptor !== null) {
      fs.closeSync(descriptor);
      fs.unlinkSync(outputPath);
    }
    throw error;
  }

  return converted;
}

module.exports = {
  categoryOf,
  osmIdentity,
  featureToPlace,
  convert,
  importFile
};

if (require.main === module) {
  // Never execute an ingestion automatically at startup.
  if (process.argv.length !== 5) {
    process.stderr.write(
      "Usage: node uganda-geojson-import.js " +
      "<input.geojson> <output.json> <extraction-ISO-time>\n"
    );
    process.exitCode = 2;
  } else {
    try {
      const result = importFile(
        process.argv[2],
        process.argv[3],
        process.argv[4]
      );

      console.log(
        "Snapshot validated: " +
        result.acceptedRecords + " records accepted; " +
        result.skippedFeatures + " skipped."
      );
    } catch (error) {
      console.error(
        "Ingestion failed: " + error.message
      );
      process.exitCode = 1;
    }
  }
}