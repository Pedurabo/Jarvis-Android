"use strict";

function validPosition(position) {
    return Array.isArray(position) &&
        position.length >= 2 &&
        Number.isFinite(position[0]) &&
        Number.isFinite(position[1]) &&
        position[0] >= -180 &&
        position[0] <= 180 &&
        position[1] >= -90 &&
        position[1] <= 90;
}

function samePosition(a, b) {
    return a[0] === b[0] && a[1] === b[1];
}

function validRing(ring) {
    return Array.isArray(ring) &&
        ring.length >= 4 &&
        ring.every(validPosition) &&
        samePosition(ring[0], ring[ring.length - 1]);
}

function validPolygon(coordinates) {
    return Array.isArray(coordinates) &&
        coordinates.length >= 1 &&
        coordinates.every(validRing);
}

function validGeometry(geometry) {
    if (!geometry || typeof geometry !== "object") {
        return false;
    }

    if (geometry.type === "Polygon") {
        return validPolygon(geometry.coordinates);
    }

    if (geometry.type === "MultiPolygon") {
        return Array.isArray(geometry.coordinates) &&
            geometry.coordinates.length > 0 &&
            geometry.coordinates.every(validPolygon);
    }

    return false;
}

function pointOnSegment(point, a, b) {
    const [x, y] = point;
    const [x1, y1] = a;
    const [x2, y2] = b;

    const cross =
        (x - x1) * (y2 - y1) -
        (y - y1) * (x2 - x1);

    const scale = Math.max(
        1,
        Math.abs(x2 - x1),
        Math.abs(y2 - y1)
    );

    if (Math.abs(cross) > 1e-10 * scale) {
        return false;
    }

    const dot =
        (x - x1) * (x - x2) +
        (y - y1) * (y - y2);

    return dot <= 1e-10;
}

function classifyRing(point, ring) {
    let inside = false;

    const [x, y] = point;

    for (let i = 0, j = ring.length - 1;
         i < ring.length;
         j = i++) {

        const a = ring[j];
        const b = ring[i];

        if (pointOnSegment(point, a, b)) {
            return "BOUNDARY";
        }

        const intersects =
            (a[1] > y) !== (b[1] > y) &&
            x < (
                (b[0] - a[0]) *
                (y - a[1]) /
                (b[1] - a[1]) +
                a[0]
            );

        if (intersects) {
            inside = !inside;
        }
    }

    return inside ? "INSIDE" : "OUTSIDE";
}

function classifyPolygon(point, polygon) {
    const outer = classifyRing(point, polygon[0]);

    if (outer !== "INSIDE") {
        return outer;
    }

    for (const hole of polygon.slice(1)) {
        const result = classifyRing(point, hole);

        if (result === "BOUNDARY") {
            return "BOUNDARY";
        }

        if (result === "INSIDE") {
            return "OUTSIDE";
        }
    }

    return "INSIDE";
}

function classifyPoint(geometry, longitude, latitude) {
    const point = [longitude, latitude];

    if (!validGeometry(geometry) ||
        !validPosition(point)) {
        return "INVALID_GEOMETRY_OR_POINT";
    }

    const polygons = geometry.type === "Polygon"
        ? [geometry.coordinates]
        : geometry.coordinates;

    let boundaryFound = false;

    for (const polygon of polygons) {
        const result = classifyPolygon(point, polygon);

        if (result === "INSIDE") {
            return "INSIDE";
        }

        if (result === "BOUNDARY") {
            boundaryFound = true;
        }
    }

    return boundaryFound ? "BOUNDARY" : "OUTSIDE";
}

module.exports = {
    validGeometry,
    classifyPoint
};
