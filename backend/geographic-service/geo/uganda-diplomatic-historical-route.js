"use strict";

const express = require("express");
const historical = require("./uganda-diplomatic-historical-search");

const router = express.Router();

router.get("/historical", (req, res) => {
    const country = req.query.country;

    if (
        typeof country !== "string" ||
        country.length > 80 ||
        !/^[a-zA-Z ]+$/.test(country) ||
        !country.trim()
    ) {
        return res.status(400).json({
            status: "INVALID_REQUEST",
            message: "A valid country parameter is required."
        });
    }

    try {
        const result = historical.lookup(country);

        if (!result) {
            return res.status(404).json({
                status: "NOT_FOUND",
                message: "No historical diplomatic record found."
            });
        }

        return res.status(200).json({
            status: "HISTORICAL_INFORMATION",
            record: result,
            navigationAllowed: false,
            selectable: false,
            verificationRequired: true
        });
    } catch (error) {
        console.error(
            "Historical diplomatic lookup failed:",
            error.message
        );

        return res.status(503).json({
            status: "HISTORICAL_DATA_UNAVAILABLE",
            message: "Historical information is temporarily unavailable."
        });
    }
});

module.exports = router;