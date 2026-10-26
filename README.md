# Jarvis Android

**An AI-powered Kampala Life & Uganda Visitor Assistant · Kotlin / Jetpack Compose · Active R&D**

Jarvis is a native Android assistant designed to make everyday life and travel across Kampala and Uganda easier. It brings natural-language conversation together with source-aware place discovery, contextual navigation, and user-controlled actions. The long-term audience includes residents, tourists, expatriates, international students, business travellers, and newcomers.

> **Status — 26 October 2026:** Active experimental prototype, **not production-ready**. The GitHub repository and the separately developed/tested local Android workspace may be at different checkpoints. Features in the roadmap below are **planned**, not delivered.

## Product vision

**One assistant. One conversation. Practical help across Uganda.**

A user should be able to ask Jarvis to find an apartment in Muyenga, locate an official telecom service centre nearby, identify a hospital or immigration office, compare published service information, and review a route in Google Maps—without manually stitching together unrelated apps. Future booking and payment actions will require explicit user consent and confirmation from the provider.

Two connected experiences will share a single discovery foundation:

- **Kampala Life Assistant:** Local services, businesses, institutions, landmarks, daily shopping and resident needs.
- **Uganda Visitor & International Communities Assistant:** Arrival, immigration guidance, accommodation, tourism, transport, language-aware discovery and newcomer assistance.

## What the current prototype supports

### Android, AI and conversation

- Kotlin / Jetpack Compose chat interface with a compact header, model-selection UI, persistent composer, message presentation, source links and scroll controls.
- Device-local conversation storage via Room in the current application.
- Experimental local Llama and Gemma inference paths, including a separately installed Gemma Companion service.
- Context-sensitive destination follow-ups and conservative handling of unknown or ambiguous places.
- Approval-protected Android actions; no automatic navigation launch.

### Source-aware Kampala information

- A limited Kampala Places preview service supporting selected name, area and category searches.
- Source-aware directory results, published-address notices, response provenance labels and safe fallback when records are missing.
- Selected published-destination handling, including Watoto Church Downtown, without assuming a map result verifies the underlying address.
- Supported source-grounded Kampala knowledge queries.

**Limit:** The current directory is small. A returned listing is not proof of current opening hours, comprehensive coverage, live stock or authoritative address verification.

### Navigation and Maps handoff

- Selected named-place and neighbourhood journeys, including a user-supplied origin and destination clarification.
- A user-controlled Google Maps handoff. Google Maps—not Jarvis—resolves roads, directions, distances and travel times.
- An existing native journey-card path for supported directions.
- **Recent local checkpoint CDI.6.0-A6.8-B2:** A compact **Review route in Google Maps** action for a source-backed journey; targeted tests, full JVM tests and debug APK build passed, and the button was observed on a Samsung Galaxy S20. **The tap-to-external-Maps result for this newer path remains unverified.** Earlier direction-card handoffs have been device-tested.

These recent CDI.6.0 changes were validated in the local development workspace. **Do not assume every local checkpoint is present in the default GitHub branch until it is separately pushed.**

## Planned Kampala & Uganda discovery coverage

The target is **broad, map-provider-backed discovery** across Kampala's five divisions, Greater Kampala, neighbouring subcounties and relevant places throughout Uganda. It is not an exhaustive hardcoded catalogue; no map provider guarantees every business or landmark is present or current.

| Domain | Target search coverage |
| --- | --- |
| Landmarks & civic | Named buildings, roads/junctions, monuments, museums, Parliament, KCCA, ministries, government offices, courts and police stations |
| Education & health | Schools, universities, training centres, hospitals, clinics, pharmacies and emergency services |
| Government & travel documents | Driving permit/licensing, passport and immigration offices, public-service branches |
| International & diplomatic | Embassies, high commissions, consulates, airline offices and airport services |
| Telecommunications | Licensed mobile operators, MTN/Airtel and other official service branches, SIM/eSIM, broadband/fibre and internet providers |
| Finance & practical services | Banks, ATMs, foreign exchange, fuel stations, parking, repairs, laundry, cleaning, catering and business services |
| Accommodation | Hotels, guesthouses, serviced/furnished apartments, publicly listed apartment buildings, hostels, BnBs and short-stay rentals |
| Retail, food & recreation | Markets, commodity prices, malls, supermarkets, restaurants, groceries, food delivery, golf, sports and events |
| Tourism & transport | Airport transfers, ride services, vehicle hire, accredited guides, safari operators, parks and visitor attractions |
| Faith & community | Places of worship, NGOs, nonprofits, public cultural associations and community resources |

Search should support categories, place aliases, locations, nearby requests (with permission), source links, provider freshness, and safe clarification where multiple branches match. Future integrations may include shopping through services such as Jumia/Ubuy and user-approved ride, food or accommodation handoffs, subject to actual provider support.

### International-community and language-aware discovery (planned)

Visitors should be able to **opt in** to finding public services that advertise languages, cuisines or cultural familiarity relevant to Indian, Somali, Ethiopian, Eritrean, Sudanese, South Sudanese, Nigerian, Turkish, English-speaking and other communities: restaurants, groceries, accommodation, interpreters, community organisations, events and legitimate professional services.

Listings will use **verifiable self-described services**, not inferred ethnicity, nationality, neighbourhood demographics or presumed ownership. Jarvis will not map private residences, identify individuals, or describe accommodation as exclusive to a community without reliable explicit evidence.

## Evidence, privacy and safety principles

1. **Place data ≠ verification.** Distinguish provider-listed, officially confirmed, user-supplied and unverified records; show sources and freshness where available.
2. **GPS ≠ a business database.** Device location requires permission; discovery additionally requires map/places providers and suitable institutional sources.
3. **No fabricated live status.** Do not invent availability, prices, ETAs, opening hours, emergency contact details, bookings or current travel regulations.
4. **User-controlled actions.** External maps, reservations, purchases and payments should be intentional, and transactional completion requires provider confirmation.
5. **Privacy by design.** Protect private residential addresses, sensitive personal data and community identities; prefer public business and authorised visitor information.

## Architecture and technology

**Current stack:** Kotlin · Android SDK · Jetpack Compose · Room · Hilt · Coroutines · Gradle · Retrofit/OkHttp · local AI inference · Gemma Companion IPC · JVM unit tests.

**Next architectural milestone — CDI.6.0-A6.9:** Consolidate the existing directory, published-place policies and navigation context into a reusable **place/service resolver** with a structured place model, multiple sources, location/coverage metadata, ambiguity handling, provenance, and optional map handoff. Map/provider integrations and new data coverage will follow incrementally; they are **not yet a comprehensive live directory**.

## Development workflow

Use a compatible JDK, Android SDK, Gradle wrapper, and locally configured inference services or model weights.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Manual physical-device verification follows successful automated tests. Install a debug APK with `adb install -r` or `installDebug`, then launch the app; keep instrumentation test runs separate from manual Samsung verification. A successful build does not imply the APK was installed or the entire workflow works on device.

Never commit local SDK paths, credentials, `.env`, `google-services.json`, model weights, user data, device logs, build artifacts, or backups.

### Kampala Places preview

[Jarvis Kampala Preview API](https://jarvis-kampala-preview.onrender.com) — experimental service; endpoints such as `GET /health` and `GET /places` may provide selected listings. Hosted availability and data freshness are not guaranteed.

## Milestones and limitations

The existing GitHub history records earlier directory/navigation checkpoints, including [`fbc7245`](https://github.com/Pedurabo/Jarvis-Android/commit/fbc7245a78b6fa8ce8c6f47ec691eeb0640034fe) (directory-backed destination navigation). More recent **local** CDI.6.0-A6.7/A6.8 work strengthened origin correction, source-backed destination follow-ups and compact Maps presentation, with automated and selected Samsung UI verification.

**Known gaps:** Broad provider-backed GPS search is not implemented; coverage is limited; multi-branch resolution, persistent journey state, provider freshness, hotel/ride booking, real-time shopping and pricing, tourism reservations and community directories remain development goals. The newest source-backed Maps action still needs its external-launch device acceptance check. A clean-checkout build of the current GitHub branch has not been independently established here.

## Roadmap

1. Verify the latest compact Maps button launches the expected external journey on a physical device.
2. **A6.9:** Audit and integrate existing place-repository interfaces into one reusable evidence-aware resolver.
3. Add credible GPS/map-provider and official-source discovery, coverage/freshness indicators and branch disambiguation.
4. Extend institutions, accommodation, telecom, tourism, everyday-life and opt-in language/community services.
5. Build integrated multi-step assistance, provider-approved actions and a polished reliable Android experience.
6. Expand offline, privacy, performance, accessibility and end-to-end regression testing.

---

**Active research and development. Not yet a released, fully verified or production-ready Uganda city assistant.**
