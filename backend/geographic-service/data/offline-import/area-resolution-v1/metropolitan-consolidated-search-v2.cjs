"use strict";

const fs = require("node:fs");

const normalize = s => String(s || "")
    .normalize("NFKC")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase();

function assert(condition, message) {
    if (!condition) throw new Error(message);
}

function validPoint(p) {
    return p &&
        Number.isFinite(p.latitude) &&
        Number.isFinite(p.longitude) &&
        p.latitude >= -90 &&
        p.latitude <= 90 &&
        p.longitude >= -180 &&
        p.longitude <= 180;
}

function validIdentity(id) {
    return /^(node|way|relation)\/[1-9][0-9]*$/
        .test(String(id || ""));
}

function distanceKm(a, b) {
    const rad = Math.PI / 180;

    const dLat =
        (b.latitude - a.latitude) * rad;

    const dLon =
        (b.longitude - a.longitude) * rad;

    const h =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(a.latitude * rad) *
        Math.cos(b.latitude * rad) *
        Math.sin(dLon / 2) ** 2;

    return 12742.0176 *
        Math.asin(Math.min(1, Math.sqrt(h)));
}

function publicCandidate(entry) {
    return {
        identity: entry.identity,
        name: entry.name,
        place: entry.place || null,
        latitude: entry.latitude,
        longitude: entry.longitude,
        sourceUrl: entry.sourceUrl,
        geographicStatus: entry.geographicStatus,
        districtMatches: entry.districtMatches || [],
        districtBorders: entry.districtBorders || [],
        divisionMatches: entry.divisionMatches || [],
        coordinateBasis: entry.coordinateBasis,
        administrativeMembershipVerified:
            false
    };
}

function createConsolidatedSearch(
    baseDocument,
    recoveryDocument,
    serviceDocument,
    existingEngineFactory
) {
    assert(
        baseDocument?.schemaVersion === 1 &&
        Array.isArray(baseDocument.records),
        "Invalid existing geographic index"
    );

    assert(
        recoveryDocument?.schemaVersion === 25 &&
        recoveryDocument.status ===
            "OFFLINE_RECOVERY_CANDIDATES_NOT_DEPLOYED" &&
        Array.isArray(recoveryDocument.candidates),
        "Invalid boundary-recovery index"
    );

    assert(
        serviceDocument?.schemaVersion === 1 &&
        serviceDocument.status ===
            "OFFLINE_METRO_SERVICE_INDEX_NOT_DEPLOYED" &&
        Array.isArray(serviceDocument.records),
        "Invalid service index"
    );

    assert(
        typeof existingEngineFactory === "function",
        "Missing validated V3 engine factory"
    );

    const base = baseDocument.records;
    const recovered = recoveryDocument.candidates;
    const services = serviceDocument.records;

    assert(base.length >= 2908,
        "Existing geographic baseline was reduced");

    assert(
        services.length === 5353,
        "Metropolitan service baseline changed"
    );

    assert(
        recovered.length === 4575,
        "Recovery candidate count differs from Phase 25"
    );

    const existingEngine = existingEngineFactory(
        baseDocument,
        serviceDocument
    );

    const byIdentity = new Map();
    const byName = new Map();

    function addName(name, entry) {
        const key = normalize(name);
        if (!key) return;

        if (!byName.has(key)) {
            byName.set(key, new Map());
        }

        byName.get(key).set(
            entry.identity,
            entry
        );
    }

    function addEntry(entry) {
        assert(
            validIdentity(entry.identity),
            "Invalid OSM identity: " + entry.identity
        );

        assert(
            validPoint(entry),
            "Invalid coordinates: " + entry.identity
        );

        assert(
            entry.sourceUrl ===
                "https://www.openstreetmap.org/" +
                entry.identity,
            "OSM source mismatch: " + entry.identity
        );

        assert(
            !byIdentity.has(entry.identity),
            "Duplicate geographic identity: " +
                entry.identity
        );

        byIdentity.set(entry.identity, entry);

        const names = [
            entry.name,
            ...(entry.alternateNames || [])
        ];

        for (const name of names) {
            addName(name, entry);
        }
    }

    for (const item of base) {
        addEntry({
            ...item,
            geographicStatus:
                "MAPPED_DISTRICT_MEMBERSHIP",
            administrativeMembershipVerified:
                false
        });
    }

    for (const item of recovered) {
        assert(
            Array.isArray(item.alternateNames),
            "Missing recovered aliases"
        );

        addEntry({
            ...item,
            coordinateBasis: "OSM_MAPPED_POINT",
            districtMatches: item.districtMembership || [],
            districtBorders: item.boundaryDistricts || [],
            divisionMatches: [],
            divisionBorders: [],
            geographicStatus:
                "RECOVERED_ADMINISTRATIVE_REVIEW_REQUIRED",
            administrativeMembershipVerified:
                false
        });
    }

    const unresolvedCount = recovered.length;
    const combinedCount = byIdentity.size;

    assert(
        combinedCount === base.length + recovered.length,
        "Geographic merge count mismatch"
    );

    function resolveLocation(request = {}) {
        const {
            name,
            district = null,
            division = null,
            selectedIdentity = null
        } = request;

        const key = normalize(name);

        if (key.length < 2 || key.length > 120) {
            return {
                status: "INVALID_LOCATION_NAME"
            };
        }

        const candidates = [
            ...(byName.get(key)?.values() || [])
        ];

        if (!candidates.length) {
            return {
                status: "LOCATION_NOT_INDEXED",
                candidates: []
            };
        }

        // District and division filters apply to the
        // established index only. A recovered feature
        // may not be assigned a district by assumption.
        const baseCandidates = candidates.filter(
            x => x.geographicStatus ===
                "MAPPED_DISTRICT_MEMBERSHIP"
        );

        const recoveryCandidates = candidates.filter(
            x => x.geographicStatus ===
                "RECOVERED_ADMINISTRATIVE_REVIEW_REQUIRED"
        );

        if (district !== null || division !== null) {
            const context = existingEngine.resolveLocation({
                name,
                district,
                division,
                selectedIdentity:
                    baseCandidates.some(
                        x => x.identity === selectedIdentity
                    )
                        ? selectedIdentity
                        : null
            });

            if (
                context.status ===
                    "INVALID_GEOGRAPHIC_CONTEXT" ||
                context.status ===
                    "INVALID_LOCATION_NAME"
            ) return context;

            if (selectedIdentity !== null) {
                const recoveredSelected =
                    recoveryCandidates.find(
                        x => x.identity === selectedIdentity
                    );

                if (recoveredSelected) {
                    return {
                        status:
                            "ADMINISTRATIVE_CONTEXT_UNVERIFIED",
                        selected:
                            publicCandidate(recoveredSelected),
                        requestedDistrict: district,
                        requestedDivision: division,
                        requiresExplicitSelection: true,
                        note:
                            "The OSM location exists, but its " +
                            "requested administrative membership " +
                            "has not been established."
                    };
                }
            }

            if (
                context.status === "RESOLVED" &&
                (
                    selectedIdentity === null ||
                    context.selected.identity === selectedIdentity
                )
            ) {
                return {
                    status: "RESOLVED",
                    selected: {
                        ...context.selected,
                        geographicStatus:
                            "MAPPED_DISTRICT_MEMBERSHIP"
                    },
                    selectionMethod:
                        context.selectionMethod
                };
            }

            if (
                context.status === "RESOLVED" &&
                selectedIdentity !== null &&
                context.selected.identity ===
                    selectedIdentity
            ) {
                return {
                    status: "RESOLVED",
                    selected: {
                        ...context.selected,
                        geographicStatus:
                            "MAPPED_DISTRICT_MEMBERSHIP"
                    },
                    selectionMethod:
                        "EXPLICIT_OSM_IDENTITY"
                };
            }

            if (
                selectedIdentity !== null &&
                !baseCandidates.some(
                    x => x.identity === selectedIdentity
                )
            ) {
                return {
                    status: "INVALID_OSM_SELECTION",
                    candidates: candidates.map(publicCandidate)
                };
            }

            if (
                recoveryCandidates.length === 0
            ) {
                return context;
            }

            // Do not silently discard conflicting
            // recovered identities when district
            // membership is unresolved.
            return {
                status:
                    "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
                requestedDistrict: district,
                requestedDivision: division,
                candidates:
                    candidates.map(publicCandidate)
            };
        }

        if (selectedIdentity !== null) {
            const selected = candidates.find(
                x => x.identity === selectedIdentity
            );

            if (!selected) {
                return {
                    status: "INVALID_OSM_SELECTION",
                    candidates:
                        candidates.map(publicCandidate)
                };
            }

            return {
                status: "RESOLVED",
                selected: publicCandidate(selected),
                selectionMethod:
                    "EXPLICIT_OSM_IDENTITY"
            };
        }

        // Only a single established candidate can be
        // selected automatically. A recovered candidate
        // must always be chosen explicitly.
        if (
            candidates.length === 1 &&
            recoveryCandidates.length === 0
        ) {
            return {
                status: "RESOLVED",
                selected:
                    publicCandidate(candidates[0]),
                selectionMethod:
                    "UNIQUE_ESTABLISHED_OSM_IDENTITY"
            };
        }

        return {
            status:
                "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
            candidates:
                candidates.map(publicCandidate),
            requiresExplicitSelection:
                recoveryCandidates.length > 0
        };
    }

    function categoryMatches(actual, requested) {
        if (requested == null || requested === "") {
            return true;
        }

        return normalize(actual) ===
            normalize(requested)
                .replace(/[\s-]+/g, "_");
    }

    function searchLocality(request = {}) {
        const {
            name,
            district = null,
            division = null,
            selectedIdentity = null,
            category = null,
            radiusKm = 5,
            limit = 20
        } = request;

        if (
            !Number.isFinite(radiusKm) ||
            radiusKm <= 0 ||
            radiusKm > 20 ||
            !Number.isInteger(limit) ||
            limit < 1 ||
            limit > 100
        ) {
            return {
                status: "INVALID_SEARCH_PARAMETERS"
            };
        }

        const resolution = resolveLocation({
            name,
            district,
            division,
            selectedIdentity
        });

        if (resolution.status !== "RESOLVED") {
            return resolution;
        }

        const selected = resolution.selected;

        if (
            selected.coordinateBasis !==
            "OSM_MAPPED_POINT"
        ) {
            return {
                status:
                    "LOCATION_GEOMETRY_REQUIRES_REVIEW",
                location: selected
            };
        }

        if (
            selected.geographicStatus ===
            "MAPPED_DISTRICT_MEMBERSHIP"
        ) {
            return existingEngine.searchLocality({
                name,
                district,
                division,
                selectedIdentity: selected.identity,
                category,
                radiusKm,
                limit
            });
        }

        // Recovered locations are searched around
        // their real mapped OSM point. No district
        // membership is inferred for returned services.
        const matches = [];

        for (const item of services) {
            if (!categoryMatches(
                item.category, category
            )) continue;

            const km = distanceKm(
                selected, item
            );

            if (km <= radiusKm) {
                matches.push({
                    service: item,
                    distanceKm: km
                });
            }
        }

        matches.sort((a, b) =>
            a.distanceKm - b.distanceKm ||
            String(a.service.name).localeCompare(
                String(b.service.name)
            ) ||
            a.service.snapshotIndex -
                b.service.snapshotIndex
        );

        return {
            status: "SEARCH_COMPLETED",
            location: selected,
            geographicStatus:
                "RECOVERED_ADMINISTRATIVE_REVIEW_REQUIRED",
            searchMethod:
                "APPROXIMATE_POINT_RADIUS",
            radiusKm,
            requestedCategory: category,
            totalSnapshotMatches:
                matches.length,
            results: matches.slice(0, limit).map(
                x => ({
                    name: x.service.name,
                    category: x.service.category,
                    osmIdentity:
                        x.service.osmIdentity,
                    sourceUrl:
                        x.service.sourceUrl,
                    latitude:
                        x.service.latitude,
                    longitude:
                        x.service.longitude,
                    distanceKm: Number(
                        x.distanceKm.toFixed(3)
                    ),
                    verification:
                        "STATIC_OSM_SNAPSHOT_NOT_LIVE_VERIFIED"
                })
            ),
            administrativeMembershipVerified:
                false,
            availabilityVerified: false,
            note:
                "Results are geographically nearby, " +
                "not verified as belonging to a specific " +
                "district or municipality."
        };
    }

    function searchArea(request = {}) {
        // Only the established district/division
        // assignments are valid for area-wide queries.
        return existingEngine.searchArea(request);
    }

    return {
        resolveLocation,
        searchLocality,
        searchArea,
        categoryList:
            existingEngine.categoryList,
        stats: {
            establishedIdentities: base.length,
            recoveredCandidates:
                unresolvedCount,
            consolidatedIdentities:
                combinedCount,
            searchableNameKeys:
                byName.size,
            serviceRecords:
                services.length
        },
        validationNames:
            [...byName.keys()],
        validationCandidates: name =>
            [...(byName.get(normalize(name))?.values()
                || [])].map(publicCandidate),
        exportIndex: () =>
            [...byIdentity.values()]
                .map(publicCandidate)
    };
}

function loadConsolidatedSearch(
    geoPath,
    recoveryPath,
    servicePath,
    existingModulePath
) {
    const createExistingEngine =
        require(existingModulePath)
            .createSearchEngine;

    return createConsolidatedSearch(
        JSON.parse(fs.readFileSync(geoPath, "utf8")),
        JSON.parse(fs.readFileSync(recoveryPath, "utf8")),
        JSON.parse(fs.readFileSync(servicePath, "utf8")),
        createExistingEngine
    );
}

module.exports = {
    createConsolidatedSearch,
    loadConsolidatedSearch
};

function runValidation(args) {
    const [
        geoPath,
        recoveryPath,
        servicePath,
        enginePath,
        indexPath,
        reportPath
    ] = args;

    assert(args.length === 6,
        "Expected six paths");

    const engine = loadConsolidatedSearch(
        geoPath,
        recoveryPath,
        servicePath,
        enginePath
    );

    const stats = {
        ...engine.stats,
        namesValidated: 0,
        automaticResolutions: 0,
        namesNeedingDisambiguation: 0,
        explicitSelectionsValidated: 0,
        rejectedInvalidSelections: 0,
        recoverySelectionTests: 0,
        areaSearchTests: 0,
        categoryAreaTests: 0
    };

    for (const name of engine.validationNames) {
        const candidates =
            engine.validationCandidates(name);

        if (
            name.length < 2 ||
            name.length > 120
        ) {
            // Preserve the source record, but do not
            // claim it is a valid search input.
            continue;
        }

        const automatic =
            engine.resolveLocation({ name });

        if (automatic.status === "RESOLVED") {
            assert(
                candidates.length === 1 &&
                candidates[0].geographicStatus ===
                    "MAPPED_DISTRICT_MEMBERSHIP",
                "Unsafe automatic resolution: " +
                    name
            );

            stats.automaticResolutions++;
        } else {
            assert(
                automatic.status ===
                    "NEEDS_GEOGRAPHIC_DISAMBIGUATION",
                "Unexpected resolution: " +
                    name + " / " +
                    automatic.status
            );

            stats.namesNeedingDisambiguation++;
        }

        for (const c of candidates) {
            const explicit =
                engine.resolveLocation({
                    name,
                    selectedIdentity:
                        c.identity
                });

            assert(
                explicit.status === "RESOLVED" &&
                explicit.selected.identity ===
                    c.identity,
                "Explicit selection failed: " +
                    c.identity
            );

            stats.explicitSelectionsValidated++;

            if (
                c.geographicStatus ===
                "RECOVERED_ADMINISTRATIVE_REVIEW_REQUIRED"
            ) {
                stats.recoverySelectionTests++;
            }
        }

        const bad = engine.resolveLocation({
            name,
            selectedIdentity:
                "node/999999999999999"
        });

        assert(
            bad.status === "INVALID_OSM_SELECTION",
            "Invalid identity was accepted"
        );

        stats.rejectedInvalidSelections++;
        stats.namesValidated++;
    }

    for (const district of [
        "Kampala", "Wakiso", "Mukono", "Mpigi"
    ]) {
        const result =
            engine.searchArea({
                district,
                limit: 1
            });

        assert(
            result.status === "SEARCH_COMPLETED",
            "District search failed: " +
                district
        );

        stats.areaSearchTests++;
    }

    for (const division of [
        "Central", "Kawempe", "Makindye",
        "Nakawa", "Rubaga"
    ]) {
        const result =
            engine.searchArea({
                district: "Kampala",
                division,
                limit: 1
            });

        assert(
            result.status === "SEARCH_COMPLETED",
            "Division search failed: " +
                division
        );

        stats.areaSearchTests++;
    }

    for (const category of engine.categoryList()) {
        for (const district of [
            "Kampala", "Wakiso",
            "Mukono", "Mpigi"
        ]) {
            const result = engine.searchArea({
                district,
                category: category.category,
                limit: 1
            });

            assert(
                result.status === "SEARCH_COMPLETED",
                "Category search failed: " +
                    category.category
            );

            stats.categoryAreaTests++;
        }
    }

    const samples = [];

    for (const test of [
        {
            name: "Entebbe",
            category: "hotel"
        },
        {
            name: "Kajjansi",
            category: "restaurant"
        },
        {
            name: "Bugolobi",
            category: "restaurant"
        },
        {
            name: "Ntinda",
            category: "restaurant"
        },
        {
            name: "Kira",
            category: "supermarket"
        },
        {
            name: "Nansana",
            category: "hospital"
        },
        {
            name: "Kigo",
            category: "hotel"
        },
        {
            name: "Mukono",
            category: "restaurant"
        },
        {
            name: "Mpigi",
            category: "bank"
        }
    ]) {
        const resolved =
            engine.resolveLocation({
                name: test.name
            });

        const candidates =
            engine.validationCandidates(
                test.name
            );

        const searchResults = [];

        for (const c of candidates.slice(0, 20)) {
            const result =
                engine.searchLocality({
                    name: test.name,
                    selectedIdentity:
                        c.identity,
                    category: test.category,
                    radiusKm: 5,
                    limit: 5
                });

            assert(
                [
                    "SEARCH_COMPLETED",
                    "LOCATION_GEOMETRY_REQUIRES_REVIEW"
                ].includes(result.status),
                "Locality search regression: " +
                    test.name + "/" +
                    c.identity + "/" +
                    result.status
            );

            searchResults.push({
                identity: c.identity,
                status: result.status,
                matches:
                    result.totalSnapshotMatches ??
                    null
            });
        }

        samples.push({
            name: test.name,
            category: test.category,
            resolutionStatus:
                resolved.status,
            candidateCount:
                candidates.length,
            searchResults
        });
    }

    for (const target of [
        "Entebbe", "Kajjansi"
    ]) {
        const sample = samples.find(
            x => x.name === target
        );

        assert(
            sample &&
            sample.candidateCount > 0 &&
            sample.searchResults.some(
                x => x.status ===
                    "SEARCH_COMPLETED"
            ),
            "Required recovered location failed: " +
                target
        );
    }

    const output = {
        schemaVersion: 26,
        status:
            "OFFLINE_CONSOLIDATED_ENGINE_VALIDATED",
        stats,
        samples,
        limitations: [
            "Recovered candidates are not validated metropolitan district members.",
            "Existing polygon memberships are mapped-source assignments, not independent legal verification.",
            "Recovered candidates require explicit OSM identity selection.",
            "District and division searches exclude administratively unresolved recovery candidates.",
            "Locality radius searches are approximate and do not represent full municipal boundaries.",
            "The recovery discovery envelope is not a metropolitan boundary.",
            "The static service snapshot is not a live business directory.",
            "No production integration has occurred."
        ]
    };

    const consolidated = {
        schemaVersion: 26,
        status:
            "OFFLINE_CONSOLIDATED_CANDIDATE_INDEX",
        establishedRecords:
            engine.stats.establishedIdentities,
        recoveredRecords:
            engine.stats.recoveredCandidates,
        records: engine.exportIndex()
    };

    const files = [
        [
            indexPath,
            JSON.stringify(consolidated)
        ],
        [
            reportPath,
            JSON.stringify(output, null, 2)
        ]
    ];

    const created = [];

    try {
        for (const [file, content] of files) {
            assert(
                !fs.existsSync(file) &&
                !fs.existsSync(file + ".partial"),
                "Output already exists: " + file
            );

            fs.writeFileSync(
                file + ".partial",
                content,
                {
                    flag: "wx",
                    encoding: "utf8"
                }
            );

            created.push(file + ".partial");
        }

        for (const [file] of files) {
            fs.renameSync(
                file + ".partial",
                file
            );

            created.push(file);
        }
    } catch (error) {
        for (const file of created.reverse()) {
            if (fs.existsSync(file)) {
                fs.unlinkSync(file);
            }
        }

        throw error;
    }

    console.log("");
    console.log("JARVIS - PHASE 26 CONSOLIDATED SEARCH");
    console.log("====================================");
    console.log(
        "Established identities:",
        stats.establishedIdentities
    );
    console.log(
        "Recovered candidates:",
        stats.recoveredCandidates
    );
    console.log(
        "Combined unique identities:",
        stats.consolidatedIdentities
    );
    console.log(
        "Searchable name keys:",
        stats.searchableNameKeys
    );
    console.log(
        "Names validated:",
        stats.namesValidated
    );
    console.log(
        "Automatically resolved:",
        stats.automaticResolutions
    );
    console.log(
        "Names requiring selection:",
        stats.namesNeedingDisambiguation
    );
    console.log(
        "Explicit identity tests:",
        stats.explicitSelectionsValidated
    );
    console.log(
        "Recovered identity tests:",
        stats.recoverySelectionTests
    );
    console.log(
        "Area search tests:",
        stats.areaSearchTests
    );
    console.log(
        "Category-area tests:",
        stats.categoryAreaTests
    );

    console.log("");
    console.log("LOCALITY SEARCH REGRESSION");

    for (const sample of samples) {
        console.log("");
        console.log(
            sample.name + ": " +
            sample.resolutionStatus +
            " | candidates=" +
            sample.candidateCount
        );

        for (const result of
            sample.searchResults.slice(0, 10)) {
            console.log(
                "  " + result.identity +
                " | " + result.status +
                " | matches=" +
                (result.matches ?? "not searched")
            );
        }
    }

    console.log("");
    console.log("PHASE 26 VALIDATION PASSED");
    console.log("Index:", indexPath);
    console.log("Report:", reportPath);
    console.log("Production unchanged.");
}

if (require.main === module) {
    try {
        runValidation(
            process.argv.slice(2)
        );
    } catch (error) {
        console.error(
            "PHASE 26 FAILED:",
            error.stack || error.message
        );
        process.exitCode = 1;
    }
}