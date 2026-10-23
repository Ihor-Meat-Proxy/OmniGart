# Manual test plan — Polar H10 (owner device)

Prerequisites: Polar H10 with charged battery, moistened electrodes, iPhone and/or Android phone with a debug OmniGart build that links `OmniGartSensors` / `:android:sensors`, and (for swim) a watch that records a swim workout.

SDK under test: **Polar BLE SDK 8.4.0**.

---

## A. Standard BLE Heart Rate Service (no Polar SDK)

1. Forget the H10 in iOS Settings → Bluetooth / Android Bluetooth (clean slate).
2. Open the app’s sensor scan screen (HRS UUID `180D`).
3. Confirm the H10 appears; connect / pair if prompted.
4. Verify live BPM updates (~1 Hz).
5. Confirm RR intervals appear when the strap sends them (parser flag bit 4).
6. Read body sensor location (`2A38`) — expect chest (1) for H10.
7. Read battery (`180F` / `2A19`) — matches Polar app ±5 %.
8. Walk out of range until disconnect; walk back — auto-reconnect restores notifications.
9. **iOS background:** lock phone for 2 minutes with workout “active”; confirm HR still updates after unlock (state restoration / `bluetooth-central`).
10. **Android background:** start `BleHeartRateForegroundService`; background the app; confirm the connected-device notification stays and HR continues.

---

## B. Polar SDK — live streams

1. Search via Polar adapter; select H10 by device id printed on the strap.
2. Connect; wait for `FEATURE_HR` / online streaming ready.
3. Start HR stream — BPM + RR ms match the HRS path within a few bpm.
4. Start ECG stream — samples arrive near **130 Hz**; stop cleanly.
5. Disconnect / reconnect; streams restart without app kill.

---

## C. H10 offline recording (pool deck → swim → download)

> H10 stores **only one** recording. Delete any previous exercise first.

### Deck

1. Connect to H10 on deck (dry phone).
2. Optionally set device local time via SDK.
3. Call `startRecording(exerciseId, interval_1s, sampleType=HR)`.
4. Confirm `requestRecordingStatus` → ongoing = true.
5. Note phone wall-clock time T0 (deck start).
6. Start the watch swim workout within a few seconds of T0.
7. Put the phone away / leave BLE range.

### Swim

8. Swim the planned set (any distance). H10 must stay on the chest; BLE to phone is **not** required.
9. End the watch swim workout; note T1.

### After

10. On deck, reconnect to H10.
11. If status still ongoing, `stopRecording`.
12. `listExercises` — exactly one entry expected.
13. `fetchExercise` — sample count ≈ swim duration in seconds (± a few).
14. Spot-check: BPM during hard intervals is higher than warm-up.
15. `removeExercise` — list becomes empty.
16. Run merge with watch HR using `ClockOffset` (deck anchor or median overlap).
17. Confirm merged series prefers strap samples when both exist within 750 ms.
18. Write merged samples through `HeartRateSink` (HealthKit / Health Connect when wired) and verify in the OS Health app.

### Optional RR offline

19. Repeat C with `sampleType=RR`. Confirm the downloaded payload is RR intervals (not 1 Hz HR). Document actual shape in OPEN_QUESTIONS if it differs from HR indexing.

---

## D. Failure / edge cases

1. Start a second recording without removing the first — expect SDK error; UI should tell the user to delete.
2. Fetch while disconnected — expect failure; reconnect and retry.
3. Low battery mid-swim — recording should still download residual samples.
4. Clock skew: set phone time +2 minutes before merge; confirm offset estimator or manual offset realigns series.

---

## Pass criteria

| Area | Pass |
|------|------|
| HRS parser | Live BPM + RR match a second receiver (e.g. OS Bluetooth HR) |
| Background | iOS restoration + Android FGS keep link ≥ 10 min |
| Offline swim | One recording downloads; length ≈ swim; delete works |
| Merge | Strap preferred; timeline aligns to watch workout ±2 s after offset |
| Health sink | Samples visible in HealthKit / Health Connect when adapter is present |
