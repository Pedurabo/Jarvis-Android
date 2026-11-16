"use strict";

const fs = require("fs");
const path = require("path");

const DATA_FILE = path.resolve(
    __dirname,
    "../data/diplomatic/uganda-diplomatic-historical-candidates-v1.json"
);

const TYPES = Object.freeze({
    EMBASSY: "Embassy",
    HIGH_COMMISSION: "High commission",
    LIAISON_OFFICE: "Liaison office",
    UNCONFIRMED_DIPLOMATIC_REPRESENTATION:
        "Unconfirmed diplomatic representation"
});

function normalize(value) {
    if (typeof value !== "string") return "";

    return value
        .normalize("NFKC")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, " ")
        .trim()
        .replace(/\s+/g, " ");
}

function loadRecords() {
    const data = JSON.parse(
        fs.readFileSync(DATA_FILE, "utf8").replace(/^\uFEFF/, "")
    );

    if (!data || !Array.isArray(data.records)) {
        throw new Error("Invalid historical diplomatic dataset");
    }

    if (data.records.length !== 5) {
        throw new Error("Unexpected historical record count");
    }

    const ids = new Set();
    const countries = new Set();

    for (const record of data.records) {
        if (
            typeof record.id !== "string" ||
            typeof record.country !== "string" ||
            !Object.prototype.hasOwnProperty.call(
                TYPES, record.missionType
            ) ||
            record.evidenceStatus !== "HISTORICAL_DIRECTORY_ENTRY" ||
            record.currentStatus !== "UNVERIFIED" ||
            record.requiresCurrentVerification !== true ||
            record.selectable !== false ||
            record.latitude !== null ||
            record.longitude !== null ||
            record.address !== null
        ) {
            throw new Error("Unsafe historical diplomatic record");
        }

        const country = normalize(record.country);

        if (
            !country ||
            ids.has(record.id) ||
            countries.has(country)
        ) {
            throw new Error("Duplicate or invalid historical record");
        }

        ids.add(record.id);
        countries.add(country);

        const evidence = record.currentOfficialEvidence;

        if (evidence !== undefined) {
            if (
                evidence === null ||
                evidence.status !== "OFFICIAL_MISSION_LISTING_FOUND" ||
                typeof evidence.url !== "string" ||
                !evidence.url.startsWith("https://") ||
                typeof evidence.publishedAddress !== "string" ||
                evidence.coordinatesVerified !== false ||
                evidence.entranceVerified !== false
            ) {
                throw new Error("Invalid current official evidence");
            }
        }
    }

    return data.records;
}

function lookup(country) {
    const term = normalize(country);

    if (!term || term.length > 80) return null;

    const record = loadRecords().find(
        item => normalize(item.country) === term
    );

    if (!record) return null;

    const official = record.currentOfficialEvidence;

    return {
        id: record.id,
        country: record.country,
        missionType: record.missionType,
        missionLabel: TYPES[record.missionType],
        historicalEvidence: {
            status: record.evidenceStatus,
            document: record.sourceDocument,
            date: record.sourceDate,
            url: record.sourceUrl
        },
        currentEvidence: official ? {
            status: official.status,
            sourceUrl: official.url,
            publishedAddress: official.publishedAddress,
            checkedAt: official.checkedAt
        } : null,
        currentStatus: "UNVERIFIED",
        requiresCurrentVerification: true,
        selectable: false,
        navigationAllowed: false,
        coordinates: null,
        notice: official
            ? "An official mission listing was recorded, but its location and entrance have not been verified for navigation."
            : "Historical directory entry only. Current mission status and location have not been verified."
    };
}

module.exports = {
    lookup,
    loadRecords
};

