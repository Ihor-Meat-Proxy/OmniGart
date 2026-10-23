# Install OmniGart on a personal iPhone and Apple Watch

Guide for loading the **iOS** and **watchOS** apps onto a physical **iPhone** and **Apple Watch** from Xcode. Covers a **free Apple ID (Personal Team)** and a **paid Apple Developer Program** account (TestFlight).

Wellness app installs only — not App Store distribution (see [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) / [RELEASE_TESTFLIGHT.md](RELEASE_TESTFLIGHT.md)).

## Prerequisites

- Mac with Xcode that can build the project’s deployment targets (**iOS 18** / **watchOS 11** in `iosApp/project.yml`; newer simulators/OS are fine).
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) and SwiftLint (`brew install xcodegen swiftlint`).
- Repo cloned; Android SDK not required for Apple-only installs.
- iPhone + Apple Watch unpaired from other conflicting provisioning as needed; **Developer Mode** enabled on both (Settings → Privacy & Security → Developer Mode) when prompted.
- Cable or wireless debugging to the iPhone; Watch paired and unlocked nearby.

## Generate the Xcode project

```bash
cd /path/to/OmniGart
./gradlew linkDebugFrameworkIosArm64 linkDebugFrameworkWatchosArm64 \
  || ./gradlew linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkWatchosSimulatorArm64
cd iosApp
xcodegen generate
open OmniGart.xcodeproj
```

Select your **Team** on targets **OmniGart** and **OmniGartWatch** (Signing & Capabilities). Set a unique bundle id suffix if the default `com.omnigart.ios` / `.watchkitapp` is already taken on your team.

## Free Apple ID (Personal Team)

### What to expect

| Topic | Personal Team behaviour |
|-------|-------------------------|
| Cost | Free Apple ID |
| Signing validity | About **7 days** — reinstall/resign after expiry |
| App count | Low simultaneous app limit (Apple enforces a small cap) |
| Devices | Register the iPhone (and Watch via the phone) in Xcode |
| TestFlight | **Not available** |
| HealthKit | Usually works for **development** installs when the HealthKit capability is present (`OmniGart.entitlements` / `OmniGartWatch.entitlements`) |
| WorkoutKit | Code is gated by `OMNIGART_ENABLE_WORKOUTKIT` (Info.plist). Leave `1` if signing succeeds; set to **`0`** if archive/sign fails so the app still builds without calling WorkoutKit APIs ([WORKOUTKIT.md](WORKOUTKIT.md)) |
| Push / Associated Domains / some background modes | Often **unavailable** or limited on Personal Team — OmniGart keeps product features behind flags where needed |
| Distribution | Direct Run from Xcode only |

### Install steps

1. Connect the iPhone; trust the computer; unlock the Watch.
2. In Xcode, scheme **OmniGart**, destination = your iPhone (Watch app embeds automatically).
3. Run (▶). Approve any provisioning prompts.
4. On the iPhone: Settings → General → VPN & Device Management → trust your developer certificate if asked.
5. Launch OmniGart; when Health permission sheets appear, accept only what you are comfortable sharing (JIT explainers are wellness-oriented).
6. Confirm the Watch app appears under the Watch app list / home screen after the phone install finishes syncing.

### If signing fails

- Change bundle identifiers to something unique under your Personal Team.
- Set `OMNIGART_ENABLE_WORKOUTKIT` to `0` in `iosApp/OmniGart/Info.plist` and clean build.
- Remove capabilities Xcode added that Personal Team rejects; keep HealthKit if possible.
- Delete an old Personal Team app from the device if you hit the app-count cap.

## Paid Apple Developer Program

### What you gain

- Longer-lived development and distribution provisioning.
- **TestFlight** internal/external testing for iPhone + Watch.
- Fewer capability surprises (HealthKit, WorkoutKit, background delivery as declared).

### Local Run

Same as Personal Team, but select your paid team. Prefer automatic signing.

### TestFlight (short path)

1. Archive **OmniGart** (Product → Archive) with a Release configuration that embeds the Watch app.
2. Distribute to TestFlight via Organizer / App Store Connect.
3. Install from TestFlight on the iPhone; install the Watch app from the Watch tab / automatic install.
4. Details and store hygiene: [RELEASE_TESTFLIGHT.md](RELEASE_TESTFLIGHT.md).

## Verify after install

- Phone shell launches; Watch companion launches (summary / start UI).
- HealthKit permission prompts use the OmniGart usage strings (en-GB source).
- Optional: with WorkoutKit enabled and `FeatureFlags.workoutKitSchedulingEnabled` turned on in a debug build, schedule a run into the system Workout app ([WORKOUTKIT.md](WORKOUTKIT.md)).
- Zone 2 live watch workout lands in follow-up PRs ([plans/watch-workoutkit-zone2.md](plans/watch-workoutkit-zone2.md)).

## Troubleshooting

| Symptom | Likely fix |
|---------|------------|
| `No such module 'Shared'` | Run the Gradle `linkDebugFramework*` / sync tasks; confirm `FRAMEWORK_SEARCH_PATHS` under `shared/build/xcode-frameworks/...` |
| Watch app missing | Build the phone scheme that embeds Watch; keep Watch nearby and unlocked |
| HealthKit denied forever | Reset privacy for OmniGart in Settings → Health / reset location & privacy (device) |
| WorkoutKit compile/sign errors on free team | `OMNIGART_ENABLE_WORKOUTKIT=0` |
| 7-day Personal Team expiry | Run again from Xcode to resign |

## Related

- [WORKOUTKIT.md](WORKOUTKIT.md) — system Workout scheduling limits  
- [SENSORS.md](SENSORS.md) — BLE / Watch optical HR notes  
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) — store compliance  
- Root [README.md](../README.md) — build overview  
