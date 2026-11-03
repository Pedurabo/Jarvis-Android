"use strict";

const fs = require("node:fs");
const path = require("node:path");

function normalizeSearch(value) {
    return String(value)
        .toLowerCase()
        .normalize("NFKD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^a-z0-9]+/g, " ")
        .trim()
        .replace(/\s+/g, " ");
}

function matchesSearch(value, query) {
    const text = normalizeSearch(value);
    const term = normalizeSearch(query);

    return Boolean(term) && (` ${text} `).includes(` ${term} `);
}

function loadCatalogs(directory) {
    const places = JSON.parse(
        fs.readFileSync(path.join(directory, "kampala-places.json"), "utf8")
    );

    const knowledge = JSON.parse(
        fs.readFileSync(path.join(directory, "kampala-knowledge.json"), "utf8")
    );

    if (places.schemaVersion !== 1 || !Array.isArray(places.places)) {
        throw new Error("Invalid Kampala places catalog");
    }

    const ids = new Set();

    for (const place of places.places) {
        if (!place || typeof place !== "object") {
            throw new Error("Invalid Kampala place record");
        }

        for (const field of [
            "id", "name", "category", "area",
            "address", "sourceUrl", "verifiedAt"
        ]) {
            if (typeof place[field] !== "string" || !place[field].trim()) {
                throw new Error(`Invalid Kampala place field: ${field}`);
            }
        }

        if (ids.has(place.id)) {
            throw new Error("Duplicate Kampala place ID");
        }
        ids.add(place.id);

        if (
            place.aliases !== undefined &&
            (!Array.isArray(place.aliases) ||
             place.aliases.some(alias =>
                 typeof alias !== "string" || !alias.trim() || alias.length > 100
             ))
        ) {
            throw new Error("Invalid Kampala place aliases");
        }

        const source = new URL(place.sourceUrl);
        if (source.protocol !== "https:") {
            throw new Error("Kampala place source must use HTTPS");
        }

        const date = new Date(place.verifiedAt);
        if (
            !/^\d{4}-\d{2}-\d{2}$/.test(place.verifiedAt) ||
            Number.isNaN(date.getTime()) ||
            date.toISOString().slice(0, 10) !== place.verifiedAt
        ) {
            throw new Error("Invalid Kampala place verification date");
        }
    }

    if (
        knowledge.schemaVersion !== 1 ||
        !Array.isArray(knowledge.entries)
    ) {
        throw new Error("Invalid Kampala knowledge catalog");
    }

    for (const entry of knowledge.entries) {
        if (
            !entry.id || !entry.topic || !entry.title ||
            !entry.answer || !entry.reviewedAt ||
            !Array.isArray(entry.sources) || entry.sources.length === 0
        ) {
            throw new Error("Invalid Kampala knowledge entry");
        }

        for (const source of entry.sources) {
            if (!source.publisher || !source.url) {
                throw new Error("Invalid Kampala knowledge source");
            }

            if (new URL(source.url).protocol !== "https:") {
                throw new Error("Kampala knowledge source must use HTTPS");
            }
        }
    }

    return { places, knowledge };
}

function createKampalaRouter(express, directory = __dirname) {
    if (!express || typeof express.Router !== "function") {
        throw new Error("Express is required to create Kampala routes");
    }

    const { places: catalog, knowledge: knowledgeCatalog } =
        loadCatalogs(directory);

    const router = express.Router();

    router.get("/places", (req, res) => {
        const query = (req.query.q || "").trim();
        const area = (req.query.area || "").trim();
        const category = (req.query.category || "").trim();

        if ([query, area, category].some(value =>
            typeof value !== "string" || value.length > 100
        )) {
            return res.status(400).json({ error: "Invalid or excessive search term" });
        }

        const results = catalog.places.filter(place => {
            const fields = [
                place.name,
                place.category,
                place.area,
                place.address,
                ...(place.aliases || [])
            ];

            return (
                (!query || fields.some(field => matchesSearch(field, query))) &&
                (!area || normalizeSearch(place.area) === normalizeSearch(area)) &&
                (!category ||
                    normalizeSearch(place.category) === normalizeSearch(category))
            );
        });

        return res.json({
            schemaVersion: catalog.schemaVersion,
            count: results.length,
            places: results.slice(0, 50)
        });
    });

    router.get("/knowledge", (req, res) => {
        const topic = (req.query.topic || "").trim();

        if (!topic || topic.length > 100) {
            return res.status(400).json({
                error: "A valid topic is required"
            });
        }

        const entries = knowledgeCatalog.entries.filter(
            entry => entry.topic.toLowerCase() === topic.toLowerCase()
        );

        return res.json({
            schemaVersion: knowledgeCatalog.schemaVersion,
            count: entries.length,
            entries
        });
    });

    return router;
}

module.exports = { createKampalaRouter, loadCatalogs };
