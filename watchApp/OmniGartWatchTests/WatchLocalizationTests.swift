import XCTest
@testable import OmniGartWatch

/// Ensures the watch String Catalog is compiled into the app bundle and that
/// keys from every watch UI surface resolve (start, Zone 2 live/summary,
/// treadmill, strength / live workout). There are no watch complications or
/// widgets in this target — coverage is the OmniGartWatch screens only.
final class WatchLocalizationTests: XCTestCase {
    /// Keys sampled from each screen that previously showed raw identifiers.
    private let keysByScreen: [(screen: String, keys: [String])] = [
        (
            "start",
            [
                "app_name",
                "watch_subtitle",
                "watch.zone2.start",
                "treadmill_run_title",
                "treadmill_walk_title",
                "watch_strength_start",
                "watch.workout.strength_traditional",
                "watch.workout.yoga",
                "watch.workout.pilates",
            ]
        ),
        (
            "zone2_live",
            [
                "watch.zone2.title",
                "watch.zone2.elapsed",
                "watch.zone2.active_kcal",
                "watch.zone2.hr_label",
                "watch.zone2.zone_below",
                "watch.zone2.zone_in",
                "watch.zone2.zone_above",
                "watch.zone2.time_in_zone",
                "watch.zone2.cadence",
                "watch.zone2.distance",
                "watch.zone2.start",
                "watch.zone2.stop",
                "watch.zone2.haptics_toggle",
            ]
        ),
        (
            "zone2_summary",
            [
                "watch.zone2.summary.title",
                "watch.zone2.summary.wellness",
                "watch.zone2.summary.duration",
                "watch.zone2.summary.time_in_zone",
                "watch.zone2.summary.distance",
                "watch.zone2.summary.avg_cadence",
                "watch.zone2.summary.active_kcal",
                "watch.zone2.summary.total_kcal",
                "watch.zone2.summary.avg_hr",
                "watch.zone2.summary.max_hr",
                "watch.zone2.summary.hr_chart",
                "watch.zone2.summary.done",
            ]
        ),
        (
            "treadmill",
            [
                "treadmill_run_title",
                "treadmill_walk_title",
                "treadmill_no_connection",
                "treadmill_speed",
                "treadmill_pace",
                "treadmill_incline",
                "treadmill_distance",
                "treadmill_hr",
                "treadmill_zone",
                "treadmill_calories",
                "treadmill_start",
                "treadmill_scan",
                "treadmill_resume",
                "treadmill_pause",
                "treadmill_finish",
            ]
        ),
        (
            "strength_live_workout",
            [
                "tab_workouts",
                "workouts_adjust_equipment",
                "watch_equipment_sync_hint",
                "watch_strength_start",
                "watch.strength.set",
                "watch.strength.target",
                "watch.strength.weight",
                "watch.strength.reps",
                "watch.strength.rest",
                "watch.strength.done",
                "watch.workout.pause",
                "watch.workout.resume",
                "watch.workout.stop",
                "watch.workout.segment",
                "watch.workout.segments",
                "watch.workout.duration",
                "watch.workout.summary.wellness",
            ]
        ),
    ]

    func testEveryWatchScreenKeyResolvesFromBundle() {
        let bundle = Bundle.main
        var failures: [String] = []
        for (screen, keys) in keysByScreen {
            for key in keys {
                let value = bundle.localizedString(forKey: key, value: "«missing»", table: nil)
                if value == "«missing»" {
                    failures.append("[\(screen)] \(key) missing from Localizable table")
                } else if value == key {
                    failures.append(
                        "[\(screen)] \(key) unresolved (raw key) — catalog not compiled into OmniGartWatch"
                    )
                } else if value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    failures.append("[\(screen)] \(key) resolved empty")
                }
            }
        }
        XCTAssertTrue(
            failures.isEmpty,
            "Watch localization failures:\n" + failures.joined(separator: "\n")
        )
    }

    func testAppNameIsBrand() {
        let value = Bundle.main.localizedString(forKey: "app_name", value: "", table: nil)
        XCTAssertEqual(value, "OmniGart")
    }
}
