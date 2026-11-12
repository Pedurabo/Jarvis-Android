require('dotenv').config();
const path = require('path');
const express = require('express');
const { OpenAI } = require('openai');
const rateLimit = require('express-rate-limit');
const helmet = require('helmet');
const morgan = require('morgan');

const app = express();

// Unified Jarvis geographic service initialization.
const geographicRoot = path.join(__dirname, "geographic-service");
const geographicAreaDir = path.join(
  geographicRoot,
  "data",
  "offline-import",
  "area-resolution-v1"
);

process.env.JARVIS_GEO_SNAPSHOT =
  process.env.JARVIS_GEO_SNAPSHOT ||
  path.join(geographicRoot, "data", "uganda-places.snapshot.json");

process.env.JARVIS_AREA_CATALOG =
  process.env.JARVIS_AREA_CATALOG ||
  path.join(geographicAreaDir, "uganda-area-catalog-v1.json");

const nationalGeoRouter = require(
  "./geographic-service/geo/uganda-geo-route"
);

const areaResolver = require(
  "./geographic-service/geo/kampala-area-resolver"
);

const metropolitanLoader = require(
  path.join(
    geographicAreaDir,
    "metropolitan-consolidated-search-v2.cjs"
  )
);

const metropolitanEngine = metropolitanLoader.loadConsolidatedSearch(
  path.join(
    geographicAreaDir,
    "greater-kampala-all-mapped-features-v2.json"
  ),
  path.join(
    geographicAreaDir,
    "metropolitan-boundary-recovery-index-v1.json"
  ),
  path.join(
    geographicAreaDir,
    "greater-kampala-services-index-v1.json"
  ),
  path.join(
    geographicAreaDir,
    "greater-kampala-search-engine-v3.cjs"
  )
);

const metropolitanRouter = require(
  path.join(geographicAreaDir, "metropolitan-api-v2-router.cjs")
).createMetropolitanRouter(express, metropolitanEngine);
const port = process.env.PORT || 3000;

// 1. Security & Logging Middleware
app.use(helmet()); // Sets various HTTP headers for security
app.use(morgan('combined')); // Standard Apache combined log output
app.use(express.json());

// 2. Rate Limiting (Prevent abuse)
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: { error: 'Too many requests, please try again later.' }
});
app.use('/chat', limiter);

// 3. OpenAI Client Initialization
const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

/**
 * @endpoint POST /chat
 * @description Secure proxy to OpenAI. Hides API keys from the client.
 */
app.post('/chat', async (req, res) => {
  const { messages, model = "gpt-4-turbo" } = req.body;

  if (!messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: 'Messages array is required' });
  }

  try {
    console.log(`Processing request for model: ${model}`);

    const completion = await openai.chat.completions.create({
      model: model,
      messages: messages,
      temperature: 0.7,
      max_tokens: 1000,
    });

    // Extract the content
    const aiResponse = completion.choices[0].message.content;

    // Log usage (simplified)
    console.log(`Usage: ${JSON.stringify(completion.usage)}`);

    res.json({
      success: true,
      data: {
        role: "assistant",
        content: aiResponse
      },
      usage: completion.usage
    });

  } catch (error) {
    console.error('OpenAI Proxy Error:', error);

    // Don't leak internal API details to the client
    const status = error.status || 500;
    const message = status === 429 ? 'OpenAI rate limit reached' : 'Internal Server Error';

    res.status(status).json({ error: message });
  }
});

// Health check
app.get('/health', (req, res) => res.json({ status: 'healthy' }));

app.use(
  "/",
  require("./kampala/kampala-routes").createKampalaRouter(express)
);
app.use("/geo", areaResolver.router);
app.use("/geo", nationalGeoRouter);
app.use(
  "/geo",
  require("./geographic-service/geo/uganda-national-locality-resolver").router
);
app.use("/geo/v2", metropolitanRouter);
app.listen(port, () => {
  console.log(`Jarvis Backend listening at http://localhost:${port}`);
});
