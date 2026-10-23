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

## Experimental Jarvis: Kampala Life Assistant

Jarvis is an Android assistant being developed to support everyday life
in Kampala, Uganda, for residents and international visitors.

The experimental version combines a modern Jetpack Compose conversation
interface, local AI inference through Gemma, and an online Kampala places
directory. This is an active development milestone, not a production release.

### Current experimental architecture

- Android application: `com.example.jarvis.gemmatest`
- UI: Kotlin and Jetpack Compose
- Inference client: `GemmaCompanionClient`
- Model runtime: separately installed Gemma Companion application
- Model: Gemma 4 E2B via LiteRT-LM and GPU
- IPC: Android Binder/AIDL with signature-protected companion service
- Kampala discovery: `KampalaExperimentalDiscovery`
- Directory backend: `jarvis-kampala-preview.onrender.com`
- Firebase: bypassed for the isolated experimental chat startup

The original Jarvis application and its Firebase-dependent architecture
remain separate from this experimental implementation.

### Implemented and tested

- Successful Gemma GPU inference on a physical Samsung Galaxy S20
- Successful Jarvis-to-Gemma Binder IPC response generation
- Multi-message conversation interface
- Session conversation context for general Gemma prompts
- Jarvis branding: **Your Kampala Life Assistant**
- Modern dark-mode conversation layout
- Rounded message composer and distinct conversation messages
- Markdown response presentation
- Numbered lists, headings, inline emphasis, and fenced code blocks
- Functional code-block clipboard copying
- Response-copy action and scroll-to-bottom UI controls
- Experimental Kampala directory integration
- Offline Gradle debug builds

### Current limitations

- The experimental app requires the separately installed Gemma Companion
  with its privately imported model.
- Model weights are not included in this repository.
- The original full Jarvis assistant pipeline is not yet restored inside
  the Firebase-independent experimental application.
- Llama/Gemma fallback exists in experimental routing code but has not
  been validated through the current experimental chat interface.
- Kampala discovery depends on an online backend.
- Directory intent classification and geographic relevance require work.
- Unsupported landmark searches can still reach Gemma and produce
  generic, ungrounded responses.
- Live prices, inventory, reservations, and directions are not guaranteed.
- Model-generated citations must not be treated as verified sources.
- Keyboard handling, response copying, and scroll navigation require
  further complete on-device acceptance testing.
- The Markdown renderer is lightweight and does not implement all
  CommonMark features.

### Next priorities

1. Complete remaining UI/UX interaction refinements.
2. Strengthen Kampala local-first intent recognition and source provenance.
3. Improve geographic filtering and reliable directory fallback notices.
4. Validate Llama/Gemma switching and recovery behavior.
5. Integrate broader Kampala Life services with verifiable information.

### Development safety

Keep private model files, signing keys, Firebase configuration files,
local credentials, generated APKs, and build directories out of Git.

The experimental implementation is maintained on a separate Git branch
to protect the working production baseline.
## CDI.5.44 — Persistent Chat History and Conversation Management

### Implemented

- Automatic device-local storage of experimental chat messages
- Restoration of conversation history after app/process restart
- Atomic JSON file writes in Android private application storage
- User and assistant messages persisted in conversation order
- Clear chat action in the Jarvis header
- Confirmation dialog with Cancel and Clear chat options
- Saved conversation clearing after confirmation
- Cancellation and stale-response protection during conversation clearing
- Existing Compose chat formatting and Gemma Companion IPC preserved
- Existing Kampala discovery routing and directory trust safeguard preserved

### Physical-device verification

Tested on a Samsung Galaxy S20:

- Cancel preserves existing messages: PASS
- Clear chat removes visible messages: PASS
- Deleted history remains deleted after reopening: PASS
- New conversations survive restarting Jarvis: PASS

Experimental chat restoration was also verified after an ADB force-stop
and Activity relaunch.

### Persistence limitations

- One conversation is stored locally; multiple named chats are not yet supported.
- Storage is private to the Android application, not synchronized to Firebase.
- Clearing the chat does not delete copies stored outside the application.
- App uninstallation or clearing application storage removes local history.
- Long conversation histories are limited by the experimental store's
  message-count, message-length, and file-size safeguards.
- This checkpoint has been verified in the experimental application,
  not released as a production Jarvis feature.

### Kampala reliability status

- Verified local directory integration and source-backed results are working.
- The hosted catalog has been expanded to 12 records.
- Directory aliases and matching are maintained in the backend.
- Unsupported or unrecognized destination queries still require
  additional intent-routing safeguards.
- No ride-booking, live-price, or payment integration is claimed.

### Next development priorities

1. Improve handling of unsupported Kampala destination queries.
2. Give genuine directory results clearer, tappable source presentation.
3. Expand the verified Kampala directory.
4. Introduce multiple conversations and history management when needed.