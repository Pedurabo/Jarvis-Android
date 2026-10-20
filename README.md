# Jarvis Android

**Kampala-focused Android AI assistant | Active research and development**

Jarvis is a Kotlin and Jetpack Compose assistant for practical life in Kampala, Uganda, aimed at residents and visitors. It combines on-device conversational AI, sourced Places and Knowledge retrieval, multi-task request interpretation, and approval-gated Android actions.

## Current Status — 20 October 2026

The project has moved beyond the original two-destination prototype. The latest **CDI.4.4-R** implementation passed the offline JVM suite (34 Gradle tasks, BUILD SUCCESSFUL). **CDI.4.4-R has not yet been installed and manually verified on Samsung.** Jarvis is experimental, not production-ready.

## Work Completed

### Kampala Places Discovery
- Integrated a public Places API with keyword, area and category search and source-aware formatting.
- Initial device-tested destinations included Acacia Mall in Kisementi and Gaddafi National Mosque in Old Kampala.
- Later Samsung tests demonstrated sourced restaurant discovery in Kololo (Khana Khazana) and hotel discovery in Kampala.
- Added broader natural-language destination handling, coverage notices and honest empty-results reporting.
- Directory entries do not establish current inventory, prices, menus, operating hours or reservation availability.

### Kampala Knowledge and Factual Reliability
- Implemented sourced Knowledge retrieval for supported questions such as Kampala history.
- Added factual and geographic boundaries to reduce fabricated local information.
- Implemented a curated location fact for Kampala and Uganda.
- Added response-relevance and output-integrity safeguards, including containment of irrelevant tool discussions and unsupported Kampala hills lists.
- These safeguards target known failures; they are not comprehensive factual verification.

### Compositional Discovery Intelligence
- Interpret multi-part requests involving markets, food, directions and ride services as separate tasks.
- Report available evidence and unsupported tasks without pretending to complete bookings or navigation.
- Product inventory, ride ordering and live directions still require verified integrations.

### Evidence and Confidence
- Distinguish Places results/notices, Knowledge results/notices, curated facts and AI-generated answers.
- CDI.4.4-R introduced the CURATED_FACT origin through AssistantController, EvidenceConfidencePolicy and JarvisViewModel.
- Confidence labels represent evidence categories, not calibrated probabilities of correctness.
- General on-device model explanations still have known technical accuracy problems.

### Android Foundation
Kotlin/Compose UI, local model components, voice-related functionality, conversation handling, Hilt, coroutines, Retrofit/OkHttp, skill routing and user-approval safeguards for actions. Opening Android Settings was exercised on Samsung in earlier tests. Some components and integrations are not production-validated.

## Verification

- Samsung device testing: Places searches, history retrieval, multi-intent handling, geographic safeguards and relevance checks.
- CDI.4.4-R: offline JVM suite passed on 20 October 2026 (1m 50s, 34 Gradle tasks).
- CDI.4.4-R Samsung installation and final provenance verification: pending.
- Code checkpoint committed to main: 7818527341cdba1666fdae3c6207042918b9e7f7.
- Clean-checkout reproducibility has not been independently verified.

## Technology Stack

Kotlin · Android SDK · Jetpack Compose · Hilt · Coroutines · Gradle · Retrofit · OkHttp · Gson · local inference · Node.js services · JVM tests.

## Kampala Preview API

https://jarvis-kampala-preview.onrender.com

Documented endpoints include GET /health and GET /places with keyword, category and area parameters. The free-tier service may cold-start after inactivity. Record counts and freshness must be confirmed against the running API rather than inferred from old milestones. Public Places search does not require paid AI generation.

## Development

With a compatible Android SDK, JDK and required local configuration, run the offline JVM suite using the Gradle wrapper task app:testDebugUnitTest. Install the debug build separately with app:installDebug only after tests pass, then verify on the physical device.

Never commit local.properties, .env files, app/google-services.json, signing keys, credentials, local logs, backups, build artifacts or private model binaries.

## Long-Term Product Vision

Practical Kampala discovery for food, markets, shopping, transport, hotels, short stays, tourism, government offices, embassies, NGOs, worship sites, residential locations and vetted household services. These are product goals; live reservations, ordering, payments, inventory confirmation and real-time navigation are not yet generally supported.

## Next Priorities

1. Complete CDI.4.4-R Samsung provenance verification.
2. Improve general technical-answer accuracy and response relevance.
3. Expand genuinely verified Kampala sources and directory coverage.
4. Consolidate evidence resolution and confidence UI.
5. Modernize the conversational interface and validate integrations.

**Active development. Not a production-ready city or booking assistant.**
