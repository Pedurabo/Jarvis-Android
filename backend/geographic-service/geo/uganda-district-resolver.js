"use strict";

const fs = require("fs");

function normalizeName(value) {
    if (typeof value !== "string") return null;

    const normalized = value
        .normalize("NFKC")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();

    if (
        normalized.length < 2 ||
        normalized.length > 120 ||
        /[\u0000-\u001f\u007f-\u009f]/.test(normalized)
    ) {
        return null;
    }

    return normalized;
}

function isDistrict(area) {
    return area &&
        area.boundary === "administrative" &&
        String(area.adminLevel) === "4" &&
        ["node", "way", "relation"].includes(area.osmType) &&
        /^[1-9]\d*$/.test(String(area.osmId)) &&
        area.sourceUrl ===
            `https://www.openstreetmap.org/${area.osmType}/${area.osmId}`;
}

function publicDistrict(area) {
    return {
        name: area.name,
        identity: `${area.osmType}/${area.osmId}`,
        sourceUrl: area.sourceUrl,
        adminLevel: String(area.adminLevel),
        geometryType: area.geometryType || null,
        mappedBounds: area.mappedBounds || null,
        hasMappedAreaGeometry:
            area.hasMappedAreaGeometry === true,

        // An envelope is not a verified district boundary.
        boundaryVerified: false
    };
}

function resolveDistrict(areas, name) {
    const query = normalizeName(name);

    if (!query) {
        return {
            status: "INVALID_REQUEST",
            candidates: []
        };
    }

    if (!Array.isArray(areas)) {
        return {
            status: "UNAVAILABLE",
            candidates: []
        };
    }

    const matches = new Map();

    for (const area of areas) {
        if (!isDistrict(area)) continue;

        if (normalizeName(area.name) !== query) {
            continue;
        }

        const candidate = publicDistrict(area);
        matches.set(candidate.identity, candidate);
    }

    const candidates = [...matches.values()]
        .sort((a, b) =>
            a.identity.localeCompare(b.identity)
        );

    if (candidates.length === 0) {
        return {
            status: "DISTRICT_NOT_INDEXED",
            candidates: []
        };
    }

    if (candidates.length > 1) {
        return {
            status: "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
            candidates
        };
    }

    return {
        status: "RESOLVED",
        district: candidates[0],
        candidates
    };
}

function loadDistrictCatalog(filePath) {
    const document = JSON.parse(
        fs.readFileSync(filePath, "utf8")
    );

    if (!document || !Array.isArray(document.areas)) {
        throw new Error("Invalid Uganda area catalog");
    }

    return document.areas;
}

module.exports = {
    normalizeName,
    isDistrict,
    resolveDistrict,
    loadDistrictCatalog
};
