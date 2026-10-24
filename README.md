# Jarvis Android

**Your Kampala Life Assistant · Kotlin / Jetpack Compose · Active development**

Jarvis is an experimental Android assistant being built for residents of Kampala, Uganda, and people visiting the city. It combines local conversational AI with a source-aware Kampala Places directory and user-controlled Android actions.

**Status as of 24 October 2026:** Active prototype, **not production-ready**. The current navigation and chat flows have been tested on a physical Samsung Galaxy S20. The latest application checkpoint is [`fbc7245`](https://github.com/Pedurabo/Jarvis-Android/commit/fbc7245a78b6fa8ce8c6f47ec691eeb0640034fe) (*Connect verified Kampala directory destinations to navigation*).

## What works today

### Kampala places and evidence

- Search the Kampala Places directory by supported keyword, area, category, and named destination.
- Show directory-backed place details, addresses, source URLs, and source-check dates when supplied by the service.
- Handle unrecognized searches and missing directory records without inventing addresses.
- Validate directory response structure, required listing fields, HTTPS source URLs, and nonfuture verification dates before displaying directory entries.
- Distinguish sourced directory responses and notices from local AI-generated answers. Provenance labels indicate an evidence category, **not independently verified accuracy**.
- Retrieve supported source-grounded Kampala Knowledge content, including Kampala history.

Directory coverage is limited. A structurally valid directory listing is not a guarantee that a business is open, that its address remains current, or that a source is independently authoritative.

### Navigation: tested end to end

Jarvis supports a user-controlled journey from a recognised Kampala area or **one exact matching directory destination**:

1. Ask *“How do I get to Muyenga?”* or *“Take me to Acacia Mall.”*
2. For a directory destination, Jarvis searches the Places service and requires a unique exact-name match before using its listed address.
3. Jarvis asks where the trip begins; a reply such as *“From Ntinda”* completes the two-turn request.
4. A native **Your journey** card displays the origin and destination, including when a previously saved directions message is restored.
5. **Open directions** opens Google Maps only after the user taps it. Google Maps resolves the locations and calculates the actual route.

Successful Samsung Galaxy S20 checks covered Ntinda → Muyenga, Nakasero → Kololo, Ntinda → Acacia Mall, the Maps handoff, no automatic launch, restored journey cards, normal source links, and unknown-destination safety.

**Navigation boundaries:** Jarvis does not calculate or verify turn-by-turn directions, road conditions, distances, or travel times. Its current neighbourhood recognition is limited, destination search uses conservative exact matching, and multiple matching directory records are not yet interactively disambiguated. Navigation conversation context is temporary in-process state and is not guaranteed to survive an app process restart.

### Android and local AI

- Jetpack Compose conversational interface with unified header, model dropdown, persistent composer, improved message spacing, source presentation, scrolling, and a **Latest** jump control.
- Device-local conversation persistence and restoration using Room in the current app.
- Local Llama 3.2 1B and Gemma 4 E2B paths, including a separately installed Gemma Companion service for Gemma inference.
- Samsung-tested model switching and core chat functionality during the navigation regression checks.
- Earlier experimental work includes Gemma Binder/AIDL diagnostics, retry handling, chat clearing, source links, and Maps search actions; those older milestone notes do not all describe the current primary app architecture.

Models and private weights are not distributed with this repository. General model responses may be incomplete or incorrect; on-device inference and companion-service reliability are still under development.

## Verification and checkpoints

These are development acceptance tests, **not a complete automated end-to-end test suite**.

| Checkpoint | Change | Reported verification |
| --- | --- | --- |
| [`16b9bf8`](https://github.com/Pedurabo/Jarvis-Android/commit/16b9bf8) | Directory input safeguards and initial directions guard | Android build and Samsung |
| [`17f9f42`](https://github.com/Pedurabo/Jarvis-Android/commit/17f9f42) | Preserve destination across a starting-point follow-up | Unit tests, build, 4/4 Samsung checks |
| [`784240c`](https://github.com/Pedurabo/Jarvis-Android/commit/784240c) | User-controlled Google Maps directions URL | 6/6 Samsung checks |
| [`31664ec`](https://github.com/Pedurabo/Jarvis-Android/commit/31664ec) | Native journey card in Compose | Unit tests, build, 6/6 Samsung checks |
| [`fbc7245`](https://github.com/Pedurabo/Jarvis-Android/commit/fbc7245) | Directory-backed named destination navigation | Unit tests, build, 6/6 Samsung checks |

The latest reported directory-navigation acceptance test covered Acacia Mall discovery, the journey card, Google Maps opening after a tap, Muyenga navigation, unrecognized-destination handling, and ordinary directory/chat/model regressions. The committed source reflects these changes; a fresh clean-checkout build has **not** been independently verified.

## Technology

Kotlin · Android SDK · Jetpack Compose · Room · Hilt · Kotlin coroutines · Gradle · Retrofit / OkHttp · local on-device inference · Gemma Companion IPC · JVM unit tests.

## Kampala Places preview service

[Jarvis Kampala Preview API](https://jarvis-kampala-preview.onrender.com)

The experimental service provides endpoints such as `GET /health` and `GET /places`, supporting keyword, category, and area filters. Availability, supported data, and freshness depend on the running service; free-tier hosting may cold-start. This is not a guaranteed live inventory or booking service.

## Developer workflow

Use a compatible JDK, Android SDK, Gradle wrapper, and private local model/companion configuration.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Run physical-device verification **after** the build. For manual testing, install the debug APK without clearing app data and launch the app normally; connected Android instrumentation tests may reinstall or replace the app. Do not assume an APK was installed just because `assembleDebug` succeeded.

Do not commit `local.properties`, `.env`, `google-services.json`, signing credentials, device logs, proprietary model weights, backups, build outputs, or private user data.

## Known limitations

- Limited Kampala directory coverage and exact-match named-place navigation. Multiple branches with the same name need safe disambiguation.
- Source URLs and checked dates are useful provenance, not proof of real-time correctness or independently audited listings.
- Routes, distances, traffic, and ETAs come from the mapping provider, not Jarvis.
- Model-generated factual or technical answers are not comprehensively fact-checked.
- Local AI setup and the companion app require device-specific configuration.
- Not all UI, failure/retry, process-restart, offline, or service-unavailable scenarios have complete automated regression coverage.
- No general-purpose ride booking, food ordering, payments, hotel reservations, live market pricing, or guaranteed real-time availability.

## Next development priorities

1. **CDI.5.53-B:** Handle multiple matching named destinations safely, using real directory records and explicit branch selection.
2. Add focused automated tests for navigation state transitions and directory-result ambiguity.
3. Restore pending navigation context safely across app restarts where appropriate.
4. Expand genuinely sourced Kampala places and services, with clearer freshness and coverage signals.
5. Continue improving the primary Compose experience and local model reliability.

## Long-term direction

Jarvis aims to help people discover and navigate Kampala's markets, shopping, food, hotels and short stays, transport options, tourist attractions, government offices, embassies, NGOs, faith communities, apartments, and vetted home services. Integrations with shopping, rides, ordering, reservations, and payments are **product goals**, not claims of currently available functionality. Any future external booking or payment action should require explicit user consent.

---

**Active research and development. Not a released or production-ready city assistant.**
