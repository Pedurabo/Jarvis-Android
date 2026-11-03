"use strict";

const fs = require("node:fs");

const DISTRICTS = [
    "Kampala", "Wakiso", "Mukono", "Mpigi"
];

const DIVISIONS = [
    "Central", "Kawempe", "Makindye",
    "Nakawa", "Rubaga"
];

const normalize = value => String(value || "")
    .normalize("NFKC")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase();

function validPoint(p) {
    return p &&
        Number.isFinite(p.latitude) &&
        Number.isFinite(p.longitude) &&
        p.latitude >= -90 && p.latitude <= 90 &&
        p.longitude >= -180 && p.longitude <= 180;
}

function distanceKm(a, b) {
    const rad = Math.PI / 180;
    const lat = (b.latitude - a.latitude) * rad;
    const lon = (b.longitude - a.longitude) * rad;

    const h =
        Math.sin(lat / 2) ** 2 +
        Math.cos(a.latitude * rad) *
        Math.cos(b.latitude * rad) *
        Math.sin(lon / 2) ** 2;

    return 12742.0176 *
        Math.asin(Math.min(1, Math.sqrt(h)));
}

function namesOf(item) {
    return [
        item.name,
        ...(Array.isArray(item.alternateNames)
            ? item.alternateNames
            : [])
    ];
}

function validIdentity(value) {
    return /^(node|way|relation)\/[1-9][0-9]*$/
        .test(String(value || ""));
}

function summary(item) {
    return {
        identity: item.identity,
        name: item.name,
        place: item.place || null,
        latitude: item.latitude,
        longitude: item.longitude,
        coordinateBasis: item.coordinateBasis,
        districts: item.districtMatches || [],
        divisions: item.divisionMatches || [],
        sourceUrl: item.sourceUrl
    };
}

function serviceSummary(item, km = null) {
    return {
        name: item.name || null,
        category: item.category,
        osmIdentity: item.osmIdentity || null,
        sourceUrl: item.sourceUrl || null,
        latitude: item.latitude,
        longitude: item.longitude,
        distanceKm: km === null
            ? null
            : Number(km.toFixed(3)),
        districts: item.districtMatches || [],
        divisions: item.divisionMatches || [],
        verification:
            "OSM_SNAPSHOT_LISTING_NOT_LIVE_VERIFIED"
    };
}

function createSearchEngine(geoDocument, serviceDocument) {
    if (
        geoDocument?.schemaVersion !== 1 ||
        !Array.isArray(geoDocument.records) ||
        serviceDocument?.schemaVersion !== 1 ||
        serviceDocument.status !==
            "OFFLINE_METRO_SERVICE_INDEX_NOT_DEPLOYED" ||
        !Array.isArray(serviceDocument.records)
    ) {
        throw new Error("Invalid metropolitan indexes");
    }

    const geo = geoDocument.records;
    const services = serviceDocument.records;

    if (geo.length < 2908 || services.length !== 5353) {
        throw new Error(
            "Input data changed; baseline review required"
        );
    }

    const byName = new Map();
    const byId = new Map();
    const categoryTotals = new Map();

    for (const item of geo) {
        if (
            !validIdentity(item.identity) ||
            !validPoint(item) ||
            byId.has(item.identity)
        ) {
            throw new Error(
                "Invalid or duplicate locality identity: " +
                String(item.identity)
            );
        }

        if (
            item.sourceUrl !==
            `https://www.openstreetmap.org/${item.identity}`
        ) {
            throw new Error(
                "Locality source identity mismatch"
            );
        }

        byId.set(item.identity, item);

        for (const name of namesOf(item)) {
            const key = normalize(name);
            if (!key) continue;

            if (!byName.has(key)) {
                byName.set(key, new Map());
            }

            byName.get(key).set(item.identity, item);
        }
    }

    for (const item of services) {
        if (!validPoint(item)) {
            throw new Error("Invalid service coordinates");
        }

        const category = normalize(item.category);

        if (!category) {
            throw new Error("Unclassified service record");
        }

        categoryTotals.set(
            category,
            (categoryTotals.get(category) || 0) + 1
        );
    }

    function districtName(raw) {
        if (raw == null || raw === "") return null;

        return DISTRICTS.find(
            name => normalize(name) === normalize(raw)
        ) || undefined;
    }

    function divisionName(raw) {
        if (raw == null || raw === "") return null;

        const value = normalize(raw);

        if (value === "lubaga") return "Rubaga";

        return DIVISIONS.find(
            name => normalize(name) === value
        ) || undefined;
    }

    function resolveLocation(request) {
        const {
            name,
            district = null,
            division = null,
            selectedIdentity = null
        } = request || {};

        const key = normalize(name);

        if (key.length < 2 || key.length > 120) {
            return { status: "INVALID_LOCATION_NAME" };
        }

        const districtFilter = districtName(district);
        const divisionFilter = divisionName(division);

        if (
            districtFilter === undefined ||
            divisionFilter === undefined ||
            (divisionFilter &&
             districtFilter &&
             districtFilter !== "Kampala")
        ) {
            return { status: "INVALID_GEOGRAPHIC_CONTEXT" };
        }

        const matches = [
            ...(byName.get(key)?.values() || [])
        ].filter(item => {
            if (
                districtFilter &&
                !(item.districtMatches || [])
                    .includes(districtFilter) &&
                !(item.districtBorders || [])
                    .includes(districtFilter)
            ) return false;

            if (
                divisionFilter &&
                !(item.divisionMatches || [])
                    .includes(divisionFilter) &&
                !(item.divisionBorders || [])
                    .includes(divisionFilter)
            ) return false;

            return true;
        });

        const candidates = matches
            .map(summary)
            .sort((a, b) =>
                a.identity.localeCompare(b.identity)
            );

        if (!matches.length) {
            return {
                status: "LOCATION_NOT_INDEXED",
                candidates: []
            };
        }

        if (selectedIdentity != null) {
            const selected = matches.find(
                item => item.identity === selectedIdentity
            );

            if (!selected) {
                return {
                    status: "INVALID_OSM_SELECTION",
                    candidates
                };
            }

            return {
                status: "RESOLVED",
                selected: summary(selected),
                selectionMethod: "EXPLICIT_OSM_IDENTITY"
            };
        }

        if (matches.length > 1) {
            return {
                status: "NEEDS_DISAMBIGUATION",
                candidates
            };
        }

        return {
            status: "RESOLVED",
            selected: summary(matches[0]),
            selectionMethod: "UNIQUE_CONTEXT_MATCH"
        };
    }

    function matchingCategory(item, category) {
        if (category == null || category === "") {
            return true;
        }

        // Exact canonical category matching.
        // Unknown synonyms are not silently guessed.
        return normalize(item.category) === normalize(category)
            .replace(/[\s-]+/g, "_");
    }

    function searchLocality(request) {
        const {
            name,
            district = null,
            division = null,
            selectedIdentity = null,
            category = null,
            radiusKm = 3,
            limit = 20
        } = request || {};

        if (
            !Number.isFinite(radiusKm) ||
            radiusKm <= 0 ||
            radiusKm > 20 ||
            !Number.isInteger(limit) ||
            limit < 1 ||
            limit > 100
        ) {
            return { status: "INVALID_SEARCH_PARAMETERS" };
        }

        const resolved = resolveLocation({
            name, district, division, selectedIdentity
        });

        if (resolved.status !== "RESOLVED") {
            return resolved;
        }

        const center = resolved.selected;

        // A proxy is an envelope midpoint, not a reliable
        // locality centre. Never silently use it for search.
        if (center.coordinateBasis !== "OSM_MAPPED_POINT") {
            return {
                status: "LOCATION_GEOMETRY_REQUIRES_REVIEW",
                location: center,
                reason:
                    "Locality position is a bounding-box proxy"
            };
        }

        const districtFilter = districtName(district);
        const divisionFilter = divisionName(division);

        const results = [];

        for (const item of services) {
            if (!matchingCategory(item, category)) continue;

            if (
                districtFilter &&
                !(item.districtMatches || [])
                    .includes(districtFilter) &&
                !(item.districtBorders || [])
                    .includes(districtFilter)
            ) continue;

            if (
                divisionFilter &&
                !(item.divisionMatches || [])
                    .includes(divisionFilter) &&
                !(item.divisionBorders || [])
                    .includes(divisionFilter)
            ) continue;

            const km = distanceKm(center, item);

            if (km <= radiusKm) {
                results.push({
                    item,
                    km
                });
            }
        }

        results.sort((a, b) =>
            a.km - b.km ||
            String(a.item.name).localeCompare(
                String(b.item.name)
            ) ||
            String(a.item.osmIdentity).localeCompare(
                String(b.item.osmIdentity)
            )
        );

        return {
            status: "SEARCH_COMPLETED",
            location: center,
            selectionMethod: resolved.selectionMethod,
            radiusKm,
            requestedCategory: category,
            totalSnapshotMatches: results.length,
            results: results.slice(0, limit)
                .map(r => serviceSummary(r.item, r.km)),
            geographicMethod:
                "POINT_CENTERED_RADIUS_APPROXIMATION",
            availabilityVerified: false,
            searchLimitations: [
                "Point radius is not an exact locality polygon",
                "OSM listing does not prove current operation"
            ]
        };
    }

    function searchArea(request) {
        const {
            district,
            division = null,
            category = null,
            limit = 20
        } = request || {};

        const d = districtName(district);
        const v = divisionName(division);

        if (
            !d || v === undefined ||
            (v && d !== "Kampala") ||
            !Number.isInteger(limit) ||
            limit < 1 ||
            limit > 100
        ) {
            return { status: "INVALID_AREA_REQUEST" };
        }

        const matches = services.filter(item =>
            (item.districtMatches || []).includes(d) &&
            (!v ||
             (item.divisionMatches || []).includes(v)) &&
            matchingCategory(item, category)
        );

        return {
            status: "SEARCH_COMPLETED",
            district: d,
            division: v,
            requestedCategory: category,
            totalSnapshotMatches: matches.length,
            results: matches.slice(0, limit)
                .map(item => serviceSummary(item)),
            geographicMethod:
                "CANDIDATE_OSM_ADMINISTRATIVE_MEMBERSHIP",
            availabilityVerified: false
        };
    }

    function categoryList() {
        return [...categoryTotals.entries()]
            .map(([category, records]) => ({
                category, records
            }))
            .sort((a, b) =>
                b.records - a.records ||
                a.category.localeCompare(b.category)
            );
    }

    return {
        resolveLocation,
        searchLocality,
        searchArea,
        categoryList,
        stats: {
            geographicRecords: geo.length,
            serviceRecords: services.length,
            geographicNames: byName.size,
            serviceCategories: categoryTotals.size
        },
        // Validation-only exhaustive keys.
        validationNames: [...byName.keys()],
        validationCandidates: key =>
            [...(byName.get(key)?.values() || [])]
                .map(summary)
    };
}

function loadEngine(geoPath, servicesPath) {
    return createSearchEngine(
        JSON.parse(fs.readFileSync(geoPath, "utf8")),
        JSON.parse(fs.readFileSync(servicesPath, "utf8"))
    );
}

module.exports = {
    createSearchEngine,
    loadEngine
};

async function validate(geoPath, servicesPath,
                        baselinePath, reportPath) {
    const engine = loadEngine(geoPath, servicesPath);
    const baseline = JSON.parse(
        fs.readFileSync(baselinePath, "utf8")
    );

    if (
        baseline.status !==
            "OFFLINE_LOCALITY_SEARCH_VALIDATED_NOT_DEPLOYED" ||
        baseline.stats.searchableNameKeys !==
            engine.stats.geographicNames ||
        baseline.stats.serviceRecords !==
            engine.stats.serviceRecords
    ) {
        throw new Error("Phase 22 baseline mismatch");
    }

    const counts = {
        namesChecked: 0,
        uniqueNameKeys: 0,
        ambiguousNameKeys: 0,
        explicitSelectionsChecked: 0,
        districtContextChecks: 0,
        invalidSelectionsRejected: 0,
        areaSearchChecks: 0,
        categorySearchChecks: 0,
        coordinateProxyBlocked: 0,
        sampleLocalityQueries: 0
    };

    for (const name of engine.validationNames) {
        const candidates = engine.validationCandidates(name);
        const result = engine.resolveLocation({ name });

        counts.namesChecked++;

        if (candidates.length === 1) {
            counts.uniqueNameKeys++;

            if (result.status !== "RESOLVED") {
                throw new Error("Unique identity regression: " + name);
            }
        } else {
            counts.ambiguousNameKeys++;

            if (result.status !== "NEEDS_DISAMBIGUATION") {
                throw new Error(
                    "Ambiguous identity auto-selected: " + name
                );
            }
        }

        for (const candidate of candidates) {
            const explicit = engine.resolveLocation({
                name,
                selectedIdentity: candidate.identity
            });

            if (
                explicit.status !== "RESOLVED" ||
                explicit.selected.identity !== candidate.identity
            ) {
                throw new Error(
                    "Explicit OSM selection regression"
                );
            }

            counts.explicitSelectionsChecked++;

            for (const district of candidate.districts) {
                const contextual = engine.resolveLocation({
                    name,
                    district,
                    selectedIdentity: candidate.identity
                });

                if (
                    contextual.status !== "RESOLVED" ||
                    contextual.selected.identity !== candidate.identity
                ) {
                    throw new Error(
                        "District context selection regression"
                    );
                }

                counts.districtContextChecks++;
            }
        }

        const bad = engine.resolveLocation({
            name,
            selectedIdentity: "node/999999999999999"
        });

        if (bad.status !== "INVALID_OSM_SELECTION") {
            throw new Error("Unknown OSM identity accepted");
        }

        counts.invalidSelectionsRejected++;
    }

    if (
        counts.uniqueNameKeys !==
            baseline.stats.uniqueNameKeys ||
        counts.ambiguousNameKeys !==
            baseline.stats.ambiguousNameKeys ||
        counts.namesChecked !==
            baseline.stats.searchableNameKeys
    ) {
        throw new Error("Name-resolution accounting mismatch");
    }

    const areaTests = [];

    for (const district of DISTRICTS) {
        const result = engine.searchArea({
            district,
            limit: 1
        });

        if (result.status !== "SEARCH_COMPLETED") {
            throw new Error("District search failed: " + district);
        }

        areaTests.push({
            district,
            matches: result.totalSnapshotMatches
        });

        counts.areaSearchChecks++;
    }

    for (const division of DIVISIONS) {
        const result = engine.searchArea({
            district: "Kampala",
            division,
            limit: 1
        });

        if (result.status !== "SEARCH_COMPLETED") {
            throw new Error("Division search failed: " + division);
        }

        areaTests.push({
            district: "Kampala",
            division,
            matches: result.totalSnapshotMatches
        });

        counts.areaSearchChecks++;
    }

    const categories = engine.categoryList();

    if (categories.length !== 26 ||
        categories.reduce((sum, c) =>
            sum + c.records, 0) !== 5353) {
        throw new Error("Category inventory mismatch");
    }

    for (const category of categories) {
        for (const district of DISTRICTS) {
            const result = engine.searchArea({
                district,
                category: category.category,
                limit: 1
            });

            if (result.status !== "SEARCH_COMPLETED") {
                throw new Error("Category filter failed");
            }

            counts.categorySearchChecks++;
        }
    }

    const samples = [
        ["Bugolobi", "Kampala", "restaurant"],
        ["Ntinda", "Kampala", "restaurant"],
        ["Entebbe", "Wakiso", "hotel"],
        ["Kajjansi", "Wakiso", "restaurant"],
        ["Kira", "Wakiso", "supermarket"],
        ["Nansana", "Wakiso", "hospital"],
        ["Mukono", "Mukono", "restaurant"],
        ["Seeta", "Mukono", "hotel"],
        ["Mpigi", "Mpigi", "bank"],
        ["Kigo", "Wakiso", "hotel"]
    ];

    const sampleResults = [];

    for (const [name, district, category] of samples) {
        const resolution = engine.resolveLocation({
            name, district
        });

        const search = engine.searchLocality({
            name,
            district,
            category,
            radiusKm: 5,
            limit: 5
        });

        sampleResults.push({
            name,
            district,
            category,
            resolutionStatus: resolution.status,
            searchStatus: search.status,
            matches: search.totalSnapshotMatches ?? null,
            candidates: resolution.candidates || (
                resolution.selected
                    ? [resolution.selected]
                    : []
            )
        });

        counts.sampleLocalityQueries++;
    }

    // A bounding-envelope centre must never be
    // silently treated as an exact mapped locality point.
    const proxy = engine.validationNames.find(name => {
        const c = engine.validationCandidates(name);
        return c.length === 1 &&
            c[0].coordinateBasis !== "OSM_MAPPED_POINT";
    });

    if (proxy) {
        const result = engine.searchLocality({
            name: proxy
        });

        if (
            result.status !==
            "LOCATION_GEOMETRY_REQUIRES_REVIEW"
        ) {
            throw new Error(
                "Unsafe geometry proxy was accepted"
            );
        }

        counts.coordinateProxyBlocked++;
    }

    const output = {
        schemaVersion: 2,
        status: "OFFLINE_REUSABLE_SEARCH_ENGINE_VALIDATED",
        stats: engine.stats,
        checks: counts,
        areaTests,
        sampleResults,
        availableCategories: categories,
        productionIntegration: false,
        limitations: [
            "The service index is a static OSM snapshot.",
            "Locality searches use an approximate point radius.",
            "District context does not always resolve duplicate names.",
            "Exact municipal boundaries are incomplete.",
            "Administrative division geometry needs further topology verification.",
            "A unique locality name is not independently verified.",
            "Booking, ordering, payment and live prices are not implemented.",
            "Live backend and Android have not been modified."
        ]
    };

    const temp = reportPath + ".partial";

    if (fs.existsSync(temp) ||
        fs.existsSync(reportPath)) {
        throw new Error("Output report already exists");
    }

    fs.writeFileSync(
        temp,
        JSON.stringify(output, null, 2),
        { flag: "wx", encoding: "utf8" }
    );

    fs.renameSync(temp, reportPath);

    console.log("");
    console.log("JARVIS - PHASE 23 REUSABLE SEARCH ENGINE");
    console.log("=======================================");
    console.log("Geographic names tested:", counts.namesChecked);
    console.log("Unique name keys:", counts.uniqueNameKeys);
    console.log("Ambiguous name keys:", counts.ambiguousNameKeys);
    console.log("Explicit selections:", counts.explicitSelectionsChecked);
    console.log("District context tests:", counts.districtContextChecks);
    console.log("Area searches:", counts.areaSearchChecks);
    console.log("Category filter tests:", counts.categorySearchChecks);
    console.log("Available categories:", categories.length);

    console.log("");
    console.log("REAL LOCALITY SEARCH TESTS");

    for (const test of sampleResults) {
        console.log(
            test.name + " (" + test.district + ")" +
            " | category=" + test.category +
            " | location=" + test.resolutionStatus +
            " | search=" + test.searchStatus +
            " | results=" +
            (test.matches === null
                ? "NOT SEARCHED"
                : test.matches)
        );
    }

    console.log("");
    console.log("ALL REUSABLE ENGINE VALIDATIONS PASSED");
    console.log("Report:", reportPath);
    console.log("Module can be imported by Node.js");
    console.log("Production unchanged.");
}

if (require.main === module) {
    const [geoPath, servicesPath,
           baselinePath, reportPath] =
        process.argv.slice(2);

    validate(
        geoPath,
        servicesPath,
        baselinePath,
        reportPath
    ).catch(error => {
        console.error("PHASE 23 FAILED:", error.message);
        process.exitCode = 1;
    });
}