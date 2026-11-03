"use strict";

const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");

const {
    validGeometry,
    classifyPoint
} = require("./uganda-district-geometry");

function calculateBounds(geometry) {
    const bounds = {
        south: Infinity,
        west: Infinity,
        north: -Infinity,
        east: -Infinity
    };

    function visit(value) {
        if (
            Array.isArray(value) &&
            value.length >= 2 &&
            typeof value[0] === "number" &&
            typeof value[1] === "number"
        ) {
            const [lon, lat] = value;
            bounds.south = Math.min(bounds.south, lat);
            bounds.north = Math.max(bounds.north, lat);
            bounds.west = Math.min(bounds.west, lon);
            bounds.east = Math.max(bounds.east, lon);
            return;
        }

        if (Array.isArray(value)) {
            for (const item of value) visit(item);
        }
    }

    visit(geometry.coordinates);
    return bounds;
}

function boundsEqual(a, b, tolerance = 0.001) {
    if (!a || !b) return false;

    return ["south", "west", "north", "east"].every(
        key =>
            Number.isFinite(a[key]) &&
            Number.isFinite(b[key]) &&
            Math.abs(a[key] - b[key]) <= tolerance
    );
}

function loadBoundary(root, manifest, district) {
    if (
        !manifest ||
        manifest.schemaVersion !== 1 ||
        !Array.isArray(manifest.districts)
    ) {
        return { status: "INVALID_MANIFEST" };
    }

    if (
        !district ||
        typeof district.identity !== "string" ||
        typeof district.name !== "string"
    ) {
        return { status: "INVALID_DISTRICT" };
    }

    const matches = manifest.districts.filter(
        entry => entry.identity === district.identity
    );

    if (matches.length !== 1) {
        return { status: "BOUNDARY_NOT_AVAILABLE" };
    }

    const entry = matches[0];

    if (
        entry.name !== district.name ||
        entry.independentProvenanceVerified !== true ||
        entry.boundaryCurrentnessVerified !== true ||
        entry.productionApproved !== true
    ) {
        return { status: "BOUNDARY_NOT_APPROVED" };
    }

    if (
        typeof entry.filename !== "string" ||
        !/^[a-z0-9-]+\.geojson$/.test(entry.filename) ||
        typeof entry.sha256 !== "string" ||
        !/^[a-f0-9]{64}$/.test(entry.sha256)
    ) {
        return { status: "INVALID_MANIFEST" };
    }

    try {
        const bytes = fs.readFileSync(
            path.join(root, entry.filename)
        );

        const hash = crypto
            .createHash("sha256")
            .update(bytes)
            .digest("hex");

        if (hash !== entry.sha256) {
            return { status: "BOUNDARY_HASH_MISMATCH" };
        }

        const geometry = JSON.parse(
            bytes.toString("utf8")
        );

        if (!validGeometry(geometry)) {
            return { status: "INVALID_BOUNDARY_GEOMETRY" };
        }

        if (
            !boundsEqual(
                calculateBounds(geometry),
                district.mappedBounds
            )
        ) {
            return { status: "BOUNDARY_BOUNDS_MISMATCH" };
        }

        return {
            status: "READY",
            identity: district.identity,
            geometry,
            sha256: hash
        };

    } catch (_) {
        return { status: "BOUNDARY_LOAD_FAILED" };
    }
}

function classifyDistrictPoint(boundary, longitude, latitude) {
    if (!boundary || boundary.status !== "READY") {
        return "UNVERIFIED";
    }

    return classifyPoint(
        boundary.geometry,
        longitude,
        latitude
    );
}

module.exports = {
    calculateBounds,
    boundsEqual,
    loadBoundary,
    classifyDistrictPoint
};
