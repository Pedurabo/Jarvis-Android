# Jarvis Android

**An AI-powered Kampala Life & Uganda Visitor Assistant · Kotlin / Jetpack Compose · Active R&D**

Jarvis is a native Android assistant intended to make everyday life and travel across Kampala and Uganda easier. It combines natural-language conversation with source-aware place discovery, contextual navigation and user-controlled actions. Its intended audience includes residents, tourists, expatriates, international students, business travellers and newcomers.

> **Status — 31 October 2026:** Experimental prototype; **not production-ready**. The public `main` branch contains the Kampala geographic-discovery integration and supporting backend code described below. Some deployment, data, test and physical-device checks remain outstanding. Planned capabilities are identified as planned rather than presented as delivered.

## Product vision

**One assistant. One conversation. Practical help across Uganda.**

Jarvis aims to help people find local services, businesses, institutions and visitor resources, understand published information, and review journeys without manually stitching together unrelated apps. Any future booking, purchase or payment action must require the user's explicit consent and provider confirmation.

Two connected experiences share the same long-term discovery foundation:

- **Kampala Life Assistant:** Local services, businesses, institutions, landmarks, shopping and everyday resident needs.
- **Uganda Visitor & International Communities Assistant:** Arrival, immigration guidance, accommodation, tourism, transport, language-aware discovery and newcomer assistance.

## What is present in the current repository

### Android, AI and conversation

- Native Kotlin / Jetpack Compose chat interface, message presentation, persistent composer, source links and conversation controls.
- Device-local conversation storage through Room.
- Experimental local Llama and Gemma inference paths; Gemma uses a separately installed companion service.
- A model-selection UI and experimental conversation-interpretation components. These experiments do not independently authorise task changes, navigation or external actions.
- Contextual destination follow-ups and conservative handling of unknown or ambiguous destinations.
- Approval-protected Android actions; constructing geographic discovery components does not itself initiate a network request.

Model availability depends on the device, installed model assets and companion-service configuration. The presence of an inference path in source code is not a guarantee that every model runs successfully on every device.

### Explicit Kampala geographic discovery

The Android source includes an experimental `KampalaExplicitGeoDiscovery` flow and an HTTPS provider adapter for the Jarvis geographic API.

- Recognises explicit category-and-area search requests; ordinary conversation and ordinary destination questions should not become geographic searches.
- Uses predefined search bounds, not live GPS.
- The current area resolver includes **Central Kampala** and seven experimentally enabled areas: **Ntinda, Nakawa, Kansanga, Kabalagala, Rubaga, Muyenga and Bugoloobi**.
- **Old Kampala remains disabled** in the active resolver because the earlier subdivision work did not establish complete coverage. Unknown or inactive areas must fail closed rather than receive invented coordinates.
- Category parsing includes health, education, finance, accommodation, civic and selected retail, food and leisure categories. The Android parser's category vocabulary is broader than the current Node route's category allow-list; those categories must not be assumed to work end to end.
- Provider responses are validated for bounds, coordinates, public-listing status, HTTPS source URLs, record limits and pagination. Provider-listed data is not independent verification of a real-world place.

**Geographic-boundary limitation:** Central Kampala uses a predefined approximate rectangle. The seven experimental area bounds are rectangles derived from third-party Uganda parish feature extents, not official KCCA-verified polygons. A rectangle can include places outside the underlying parish polygon. These bounds are not live GPS, official administrative boundaries or proof of full area coverage.

### Node.js geographic backend in this repository

The repository includes an experimental Node.js/Express backend under `backend/`. Its geographic router is mounted at `/geo` and exposes:

- `GET /geo/search` — category search within supplied south/west/north/east bounds.
- The route requires a geographic snapshot configured through `JARVIS_GEO_SNAPSHOT`. The snapshot dataset is not included in this repository. Without a configured, loadable snapshot, the route returns an unavailable response rather than fabricated empty results.
- The current route allow-list contains 11 categories: hospital, clinic, pharmacy, school, university, bank, fuel station, hotel, guest house, police and embassy.

The same backend also contains the existing experimental `POST /chat` proxy and `GET /health` health endpoint. Configure secrets through environment variables; do not commit `.env` files or credentials.

The Android client has an HTTPS geographic API adapter and a configured service origin. **A configured URL and an implemented adapter do not prove that the deployed endpoint, snapshot, category coverage or phone-to-server request has been verified.** Hosted availability and data freshness must be checked independently.

### Separately maintained Uganda geography service

The [Jarvis Uganda Geographic Service](https://github.com/Pedurabo/jarvis-uganda-geo-service) is a separately maintained project with its own metropolitan discovery API and route contract, including locality resolution, locality search, area search and category listing under its documented `/geo/v2` endpoints.

That service is distinct from the `/geo/search` router included in this repository. Their endpoint paths, data snapshots, category coverage and deployment status must not be treated as interchangeable. Integration requires an explicitly compatible deployment and end-to-end verification.

### Source-aware directory information and navigation

- The app contains a limited Kampala Places preview integration for selected name, area and category searches.
- Directory responses can present source and provenance information and safe fallbacks when records are missing.
- Selected published-destination handling includes Watoto Church Downtown; a map listing does not independently verify an address.
- Contextual journey handling can retain a destination while the user clarifies an origin.
- Google Maps handoff is user-controlled. Google Maps—not Jarvis—resolves roads, directions, distances and travel times.

**Limit:** The directory and geographic data are not comprehensive. A returned listing is not proof of current operation, opening hours, stock, availability or an authoritative address. The latest compact Maps action still requires its external-launch acceptance check on a physical device.

## Planned Kampala & Uganda discovery coverage

The target is broad, source-backed discovery across Kampala's five divisions, Greater Kampala, neighbouring subcounties and relevant places throughout Uganda. This is a development goal, not a claim that every category or locality is currently searchable.

| Domain | Planned coverage |
| --- | --- |
| Landmarks & civic | Named buildings, roads and junctions, monuments, museums, Parliament, KCCA, ministries, government offices, courts and police stations |
| Education & health | Schools, universities, training centres, hospitals, clinics, pharmacies and emergency services |
| Government & travel documents | Driving permit and licensing services, passport and immigration offices, public-service branches |
| International & diplomatic | Embassies, high commissions, consulates, airline offices and airport services |
| Telecommunications | Licensed mobile operators, official MTN/Airtel and other service branches, SIM/eSIM, broadband/fibre and internet providers |
| Finance & practical services | Banks, ATMs, foreign exchange, fuel stations, parking, repairs, laundry, cleaning, catering and business services |
| Accommodation | Hotels, guesthouses, serviced/furnished apartments, publicly listed apartment buildings, hostels, BnBs and short-stay rentals |
| Retail, food & recreation | Markets, commodity prices, malls, supermarkets, restaurants, groceries, food delivery, golf, sports and events |
| Tourism & transport | Airport transfers, ride services, vehicle hire, accredited guides, safari operators, parks and visitor attractions |
| Faith & community | Places of worship, NGOs, nonprofits, public cultural associations and community resources |

Future discovery should support category aliases, place aliases, area context, nearby requests with permission, source links, provider freshness and safe clarification where multiple branches match. Shopping, ride, food and accommodation integrations depend on actual provider support and user-approved handoffs.

### International-community and language-aware discovery (planned)

The intended opt-in experience will help visitors discover public services that explicitly advertise relevant languages, cuisines or cultural services for Indian, Somali, Ethiopian, Eritrean, Sudanese, South Sudanese, Nigerian, Turkish, English-speaking and other communities. Potential categories include restaurants, groceries, accommodation, interpreters, community organisations, events and legitimate professional services.

Listings must be based on verifiable, self-described services—not inferred ethnicity, nationality, neighbourhood demographics or presumed ownership. Jarvis will not map private residences, identify individuals or describe accommodation as exclusive to a community without reliable explicit evidence.

## Evidence, privacy and safety principles

1. **Place data is not verification.** Distinguish provider-listed, officially confirmed, user-supplied and unverified records; show sources and freshness where available.
2. **GPS is not a business database.** Device location requires permission; discovery additionally requires suitable map/places providers and institutional sources.
3. **Do not fabricate live status.** Do not invent availability, prices, ETAs, opening hours, emergency contacts, bookings or current travel regulations.
4. **Keep actions user-controlled.** External maps, reservations, purchases and payments must be intentional. Transaction completion requires provider confirmation.
5. **Protect privacy.** Avoid exposing private residential addresses, sensitive personal data or inferred community identities. Prefer public business and authorised visitor information.

## Architecture and technology

**Android:** Kotlin · Android SDK · Jetpack Compose · Room · Hilt · Coroutines · Gradle · Retrofit/OkHttp · local AI inference · Gemma Companion IPC · JVM unit tests.

**Repository backend:** Node.js · Express · OpenAI SDK · Helmet · express-rate-limit · Morgan · static geographic snapshot loading.

**Geographic data:** OpenStreetMap-derived records and third-party locality features are used in experimental components. Source provenance and geometric limitations must remain visible; no dataset should be described as complete or officially verified unless that has been established.

## Validation status and known limitations

- The latest reported local `:app:testDebugUnitTest` run completed **1,155 tests with 15 failures**. Failures include geographic command/provider routing, unsupported-area handling and fresh-journey ordering. These failures have not yet been resolved; this README does not claim that the full JVM suite is passing.
- The latest reported Node route-test run could not complete because its required snapshot test path was missing. The importer and places-index test groups passed their reported checks, but that does not validate a configured live service.
- A clean-checkout build of the current GitHub branch has not been independently established here.
- The newest compact Google Maps action still needs its external-launch physical-device acceptance check.
- Broad provider-backed GPS search, comprehensive geographic coverage, official boundary verification, robust multi-branch resolution, provider freshness, persistent journey state, hotel/ride booking, real-time shopping/pricing, tourism reservations and community directories remain incomplete or planned.

A successful compilation alone does not establish correct provider responses, hosted backend availability, external Maps launch or complete physical-device behaviour.

## Development workflow

Use a compatible JDK, Android SDK, Gradle wrapper, and locally configured inference services or model weights.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

For the Node backend, install dependencies from `backend/package-lock.json` and configure the required environment variables. Geographic route tests require a valid snapshot fixture/configuration; do not replace missing source data with fabricated production records.

Manual physical-device verification follows successful automated tests. Install a debug APK with `adb install -r` or the Gradle install task, then launch the app. Keep instrumentation tests separate from manual Samsung-device verification.

Never commit local SDK paths, credentials, `.env`, `google-services.json`, model weights, user data, device logs, build artifacts or backups.

## Roadmap

1. Resolve the outstanding Android JVM test failures without weakening geographic safety and routing assertions.
2. Verify the geographic API deployment, snapshot configuration, endpoint contract and category coverage end to end.
3. Verify the latest compact Maps action launches the intended external journey on a physical device.
4. Consolidate directory, published-place and geographic discovery behind a reusable evidence-aware place/service resolver.
5. Expand credible map-provider and official-source discovery with coverage, freshness and branch-disambiguation indicators.
6. Extend institutional, accommodation, telecom, tourism, everyday-life and opt-in language/community services.
7. Improve offline behaviour, privacy, performance, accessibility and end-to-end regression coverage.

---

**Active research and development. Jarvis is not a released, fully verified or production-ready Uganda city assistant.**
