# Jarvis Android

Jarvis is an experimental Android assistant under active development, with a growing focus on helping people discover places and services in Kampala, Uganda.

## Current Status

The Kampala Places Discovery feature is working end to end on a Samsung Android device. Its initial public directory contains two sourced destinations:

- Acacia Mall — Kisementi, Kampala
- Gaddafi National Mosque — Old Kampala

Supported requests include "Find shopping places in Kisementi" and "Find a mosque in Old Kampala".

These searches use the public Places API without paid AI generation. The directory is limited, and opening hours or current availability are not confirmed.

## Android Features and Architecture

The codebase includes:

- Kotlin and Jetpack Compose user interfaces
- Assistant request routing and model integration components
- Conversation and memory management
- Local and cloud AI integration components
- Skill registration and approval safeguards for device actions
- Voice assistant components
- Google Maps integration components
- Response validation and Kotlin answer-safety policies
- Kampala Places request classification, networking, and formatting

Not all components are fully configured or independently verified for production use.

## Technology Stack

- Kotlin and Android SDK
- Gradle and Android Studio
- Jetpack Compose
- Hilt dependency injection
- Retrofit, OkHttp, and Gson
- Kotlin coroutines
- JVM unit tests
- Node.js backend services

## Kampala Places API

Public URL:

https://jarvis-kampala-preview.onrender.com

Available endpoints:

- GET /health — service health
- GET /places — directory records
- GET /places?q=mosque — keyword search
- GET /places?category=shopping — category search
- GET /places?area=Kisementi — area search

The public API is deployed from this repository's separate hosting-preview branch. It does not expose the paid AI chat endpoint.

The service runs on Render's free tier and may take longer to respond after inactivity.

## Project Layout

- app/src/main — Android application code and resources
- app/src/test — JVM unit tests
- app/src/androidTest — instrumented tests
- backend — local Node.js backend sources
- core — additional source components
- gradle — Gradle wrapper and dependency configuration

## Development

Open the project in Android Studio with a compatible Android SDK and JDK.

Windows PowerShell commands:

Run JVM tests:

    .\gradlew.bat :app:testDebugUnitTest

Compile Android Kotlin:

    .\gradlew.bat :app:compileDebugKotlin

Install the debug application on an authorized connected device:

    .\gradlew.bat :app:installDebug

A fresh checkout may need additional configuration or local assets before it builds successfully.

## Security and Local Configuration

Never commit private credentials, signing keys, or environment secrets.

The project excludes local configuration and artifacts such as:

- local.properties
- backend/.env
- app/google-services.json
- Signing keys and keystores
- Build output, local logs, and backup files
- Local AI model binaries pending distribution review

The public Places API does not require paid OpenAI requests. Other assistant functionality may require separately configured services.

## Verification

Completed development checks include:

- Kotlin compilation and JVM unit tests
- Live public Places API retrieval
- Acacia Mall discovery on Android
- Gaddafi National Mosque discovery on Android
- Directory-result formatting and source display
- Installation and successful searches using Places-specific network timeouts

Continued testing is needed for network interruptions and Render cold starts.

## Known Limitations

- Only two initial directory records
- Limited natural-language place-search patterns
- No verified live business hours or availability
- Some external integrations require configuration
- Cloud AI access may require separate billing
- Clean-checkout reproducibility has not yet been established

## Roadmap

1. Improve natural-language discovery.
2. Expand the sourced Kampala directory.
3. Improve network resilience and offline handling.
4. Improve source links and directory presentation.
5. Add richer location and navigation features.
6. Validate clean builds and prepare future releases.

## Project Status

Active development. This is not yet a production-ready city assistant.

<!-- JARVIS_CDI44_STATUS_BEGIN -->
## Current Development: Kampala Discovery and Evidence Reliability

Jarvis Android is an experimental Kotlin and Jetpack Compose assistant
being developed as a practical Kampala, Uganda daily-life assistant.

The product vision includes local business and market discovery,
restaurants, accommodation, transport, navigation, public services,
tourism, and other useful city information for residents and visitors.

### Implemented and Tested

- Kampala Places discovery through a source-aware directory.
- Kampala Knowledge responses with source information for supported topics.
- Structured handling and coverage reporting for multi-part local requests.
- Geographic factual safeguards to reduce unsupported place descriptions.
- Model-output integrity and response-relevance checks.
- Curated baseline facts for supported questions such as Kampala's location.
- Separate response origins for directory results, knowledge results,
  curated facts, notices, and unverified AI-generated answers.
- Conservative, provenance-based confidence presentation.
- Android device-action approval safeguards.
- Offline JVM regression tests and Samsung physical-device testing.

### Latest Checkpoint: CDI.4.4-R

The CDI.4.4-R implementation introduces `CURATED_FACT` response provenance
and integrates it through the assistant controller, confidence policy,
and ViewModel.

The complete offline debug JVM test suite passed on 20 October 2026:

- Gradle result: BUILD SUCCESSFUL
- Tasks: 34 (12 executed, 22 up-to-date)
- Duration: 1 minute 50 seconds

Samsung installation and physical-device verification of CDI.4.4-R
remain pending.

### Evidence and Confidence

Directory and Knowledge results preserve their existing evidence origins.
Reviewed baseline facts are classified separately from ordinary AI output.
Medium Confidence is not a calibrated probability of correctness.

Directory records do not establish live prices, opening hours, product
inventory, booking availability, or real-time navigation.

### Known Limitations

- Kampala service-directory coverage remains incomplete.
- Some geographic and historical requests lack verified reference data.
- General on-device model answers may contain technical inaccuracies.
- Source attribution for curated baseline facts needs further development.
- Live ride ordering, transactions, and reservations require verified
  integrations and explicit user consent.
- The conversation interface requires further modernization.
- Production readiness and clean-checkout reproducibility remain unverified.

### Next Priorities

1. Finish CDI.4.4-R Samsung acceptance testing.
2. Improve general technical-answer accuracy and response relevance.
3. Expand verified Kampala data and source provenance.
4. Consolidate evidence resolution and improve confidence presentation.
5. Modernize the conversational UI and practical service workflows.

**Status:** Active research and development; not production-ready.
<!-- JARVIS_CDI44_STATUS_END -->
