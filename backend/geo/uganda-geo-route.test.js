"use strict";

const assert = require("assert");
const http = require("http");
const express = require("express");
const geo = require("./uganda-places-index");

const snapshotPath = process.env.JARVIS_GEO_SNAPSHOT;

if (!snapshotPath) {
    throw new Error("Test snapshot path missing");
}

const index = geo.loadSnapshot(snapshotPath);

if (!index.places.length) {
    throw new Error("Empty test index");
}

const place = index.places[0];

const app = express();

app.use("/geo", require("./uganda-geo-route"));

function request(server, path) {
    return new Promise((resolve, reject) => {
        const address = server.address();

        const req = http.get({
            hostname: "127.0.0.1",
            port: address.port,
            path
        }, res => {
            let text = "";

            res.setEncoding("utf8");

            res.on("data", chunk => {
                text += chunk;
                if (text.length > 1048576) {
                    req.destroy();
                    reject(new Error("Oversized HTTP response"));
                }
            });

            res.on("end", () => {
                try {
                    resolve({
                        status: res.statusCode,
                        body: JSON.parse(text)
                    });
                } catch (error) {
                    reject(error);
                }
            });
        });

        req.setTimeout(5000, () => {
            req.destroy(new Error("HTTP request timed out"));
        });

        req.on("error", reject);
    });
}

async function main() {
    const server = http.createServer(app);

    try {
        await new Promise((resolve, reject) => {
            server.once("error", reject);
            server.listen(0, "127.0.0.1", resolve);
        });

        const latitude = Number(place.latitude);
        const longitude = Number(place.longitude);

        const south = Math.max(-90, latitude - 0.00001);
        const north = Math.min(90, latitude + 0.00001);
        const west = Math.max(-180, longitude - 0.00001);
        const east = Math.min(180, longitude + 0.00001);

        const query = [
            "category=" + encodeURIComponent(place.category),
            "south=" + south,
            "west=" + west,
            "north=" + north,
            "east=" + east,
            "limit=50"
        ].join("&");

        const good = await request(
            server,
            "/geo/search?" + query
        );

        assert.strictEqual(good.status, 200);
        assert.ok(
            ["SUCCESS", "INCOMPLETE"].includes(good.body.status)
        );
        assert.strictEqual(
            good.body.snapshotGeneratedAt,
            index.generatedAt
        );

        console.log("PASS valid geographic request");

        const invalidCategory = await request(
            server,
            "/geo/search?category=private_residence"
        );

        assert.strictEqual(invalidCategory.status, 400);
        console.log("PASS private categories rejected");

        const badBounds = await request(
            server,
            "/geo/search?category=school" +
            "&south=0&west=0&north=90&east=180"
        );

        assert.strictEqual(badBounds.status, 400);
        console.log("PASS oversized bounds rejected");

        const badLimit = await request(
            server,
            "/geo/search?" + query + "&limit=999"
        );

        assert.strictEqual(badLimit.status, 400);
        console.log("PASS repeated limit rejected");

        const unsupportedLimit = await request(
            server,
            "/geo/search?" +
            query.replace("limit=50", "limit=51")
        );

        assert.strictEqual(unsupportedLimit.status, 400);
        console.log("PASS excessive limit rejected");

        console.log("");
        console.log("ALL 5 GEO HTTP TESTS PASSED");
    } finally {
        await new Promise(resolve => server.close(resolve));
    }
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});