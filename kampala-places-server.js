"use strict";

const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

const port = Number(process.env.PORT || 31896);
const catalogPath = path.join(__dirname, "kampala-places.json");

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error("Invalid server port");
}

function loadCatalog() {
  const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));

  if (catalog.schemaVersion !== 1 || !Array.isArray(catalog.places)) {
    throw new Error("Invalid places catalog");
  }

  const ids = new Set();

  for (const place of catalog.places) {
    if (!place || typeof place !== "object") {
      throw new Error("Invalid place record");
    }

    for (const field of [
      "id", "name", "category", "area",
      "address", "sourceUrl", "verifiedAt"
    ]) {
      if (typeof place[field] !== "string" ||
          !place[field].trim()) {
        throw new Error(`Missing or invalid field: ${field}`);
      }
    }

    if (ids.has(place.id)) {
      throw new Error("Duplicate place ID");
    }
    ids.add(place.id);

    const source = new URL(place.sourceUrl);
    if (source.protocol !== "https:") {
      throw new Error("Source URL must use HTTPS");
    }

    const date = new Date(place.verifiedAt);
    if (
      !/^\d{4}-\d{2}-\d{2}$/.test(place.verifiedAt) ||
      Number.isNaN(date.getTime()) ||
      date.toISOString().slice(0, 10) !== place.verifiedAt
    ) {
      throw new Error("Invalid verification date");
    }
  }

  return catalog;
}

const catalog = loadCatalog();

const knowledgeCatalog = JSON.parse(
  fs.readFileSync(path.join(__dirname, "kampala-knowledge.json"), "utf8")
);

if (
  knowledgeCatalog.schemaVersion !== 1 ||
  !Array.isArray(knowledgeCatalog.entries) ||
  knowledgeCatalog.entries.length !== 1
) {
  throw new Error("Invalid knowledge catalog");
}

for (const entry of knowledgeCatalog.entries) {
  if (
    !entry.id ||
    !entry.topic ||
    !entry.title ||
    !entry.answer ||
    !entry.reviewedAt ||
    !Array.isArray(entry.sources) ||
    entry.sources.length === 0
  ) {
    throw new Error("Invalid knowledge entry");
  }

  for (const source of entry.sources) {
    if (!source.publisher || !source.url) {
      throw new Error("Invalid knowledge source");
    }

    const parsedUrl = new URL(source.url);

    if (parsedUrl.protocol !== "https:") {
      throw new Error("Knowledge sources must use HTTPS");
    }
  }
}

function sendJson(res, status, payload) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store"
  });
  res.end(JSON.stringify(payload));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, "http://localhost");

  if (req.method === "GET" && url.pathname === "/health") {
    return sendJson(res, 200, { status: "healthy" });
  }

  if (req.method === "GET" && url.pathname === "/places") {
    const query = (url.searchParams.get("q") || "").trim().toLowerCase();
    const area = (url.searchParams.get("area") || "").trim().toLowerCase();
    const category = (url.searchParams.get("category") || "")
      .trim().toLowerCase();

    if ([query, area, category].some(value => value.length > 100)) {
      return sendJson(res, 400, { error: "Search term too long" });
    }

    const results = catalog.places.filter(place => {
      const haystack = [
        place.name, place.category, place.area, place.address
      ].join(" ").toLowerCase();

      return (!query || haystack.includes(query)) &&
        (!area || place.area.toLowerCase() === area) &&
        (!category || place.category.toLowerCase() === category);
    });

    return sendJson(res, 200, {
      schemaVersion: catalog.schemaVersion,
      count: results.length,
      places: results.slice(0, 50)
    });
  }

  if (req.method === "GET" && url.pathname === "/knowledge") {
    const topic = (url.searchParams.get("topic") || "")
      .trim().toLowerCase();

    if (!topic || topic.length > 100) {
      return sendJson(res, 400, {
        error: "A valid topic is required"
      });
    }

    const entries = knowledgeCatalog.entries.filter(
      entry => entry.topic.toLowerCase() === topic
    );

    return sendJson(res, 200, {
      schemaVersion: knowledgeCatalog.schemaVersion,
      count: entries.length,
      entries
    });
  }

  return sendJson(res, 404, { error: "Not found" });
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Kampala Places test API listening on ${port}`);
});