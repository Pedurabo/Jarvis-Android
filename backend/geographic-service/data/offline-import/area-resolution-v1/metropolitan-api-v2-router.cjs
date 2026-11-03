"use strict";

function createMetropolitanRouter(express, engine) {
    if (
        !express ||
        typeof express.Router !== "function" ||
        !engine ||
        typeof engine.resolveLocation !== "function" ||
        typeof engine.searchLocality !== "function" ||
        typeof engine.searchArea !== "function" ||
        typeof engine.categoryList !== "function"
    ) {
        throw new TypeError("Invalid router dependencies");
    }

    const router = express.Router();

    function optionalString(query, key, maxLength = 120) {
        const value = query[key];

        if (value === undefined) {
            return null;
        }

        if (
            typeof value !== "string" ||
            !value.trim() ||
            value.length > maxLength
        ) {
            throw new RangeError(
                "Invalid parameter: " + key
            );
        }

        return value.trim();
    }

    function numberValue(query, key, fallback, min, max, integer) {
        const value = query[key];

        if (value === undefined) {
            return fallback;
        }

        if (
            typeof value !== "string" ||
            !value.trim()
        ) {
            throw new RangeError(
                "Invalid parameter: " + key
            );
        }

        const number = Number(value);

        if (
            !Number.isFinite(number) ||
            number < min ||
            number > max ||
            (integer && !Number.isInteger(number))
        ) {
            throw new RangeError(
                "Out-of-range parameter: " + key
            );
        }

        return number;
    }

    function identityValue(query) {
        const identity = optionalString(
            query, "selectedIdentity", 100
        );

        if (
            identity !== null &&
            !/^(node|way|relation)\/[1-9][0-9]*$/
                .test(identity)
        ) {
            throw new RangeError(
                "Invalid selectedIdentity"
            );
        }

        return identity;
    }

    function locationQuery(query) {
        const name = optionalString(query, "name");

        if (!name) {
            throw new RangeError(
                "Location name is required"
            );
        }

        return {
            name,
            district: optionalString(
                query, "district", 80
            ),
            division: optionalString(
                query, "division", 80
            ),
            selectedIdentity: identityValue(query)
        };
    }

    function searchQuery(query) {
        return {
            ...locationQuery(query),
            category: optionalString(
                query, "category", 100
            ),
            radiusKm: numberValue(
                query, "radiusKm", 5,
                0.01, 20, false
            ),
            limit: numberValue(
                query, "limit", 20,
                1, 100, true
            )
        };
    }

    function areaQuery(query) {
        return {
            district: optionalString(
                query, "district", 80
            ),
            division: optionalString(
                query, "division", 80
            ),
            category: optionalString(
                query, "category", 100
            ),
            limit: numberValue(
                query, "limit", 20,
                1, 100, true
            )
        };
    }

    function sendResult(res, result) {
        if (
            !result ||
            typeof result !== "object" ||
            typeof result.status !== "string"
        ) {
            throw new Error(
                "Unexpected engine response"
            );
        }

        if (result.status.startsWith("INVALID_")) {
            return res.status(400).json(result);
        }

        if (result.status === "LOCATION_NOT_INDEXED") {
            return res.status(404).json(result);
        }

        return res.status(200).json(result);
    }

    function handler(operation) {
        return (req, res) => {
            try {
                const result = operation(req.query);
                return sendResult(res, result);
            } catch (error) {
                if (error instanceof RangeError) {
                    return res.status(400).json({
                        status: "INVALID_REQUEST",
                        message: error.message
                    });
                }

                console.error(
                    "METROPOLITAN_API_ERROR:",
                    error.stack || error.message
                );

                return res.status(500).json({
                    status: "INTERNAL_ERROR"
                });
            }
        };
    }

    router.get("/resolve", handler(query =>
        engine.resolveLocation(
            locationQuery(query)
        )
    ));

    router.get("/locality-search", handler(query =>
        engine.searchLocality(
            searchQuery(query)
        )
    ));

    router.get("/area-search", handler(query =>
        engine.searchArea(
            areaQuery(query)
        )
    ));

    router.get("/categories", handler(() => {
        const categories = engine.categoryList();

        return {
            status: "SEARCH_COMPLETED",
            categories,
            categoryCount: categories.length,
            source:
                "STATIC_OSM_METROPOLITAN_SNAPSHOT",
            availabilityVerified: false
        };
    }));

    return router;
}

module.exports = {
    createMetropolitanRouter
};