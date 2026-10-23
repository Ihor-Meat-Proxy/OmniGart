import Foundation

/// Compile- and runtime gates for capabilities that may fail Personal Team signing.
///
/// Free Apple ID (Personal Team) generally supports HealthKit for development installs.
/// If WorkoutKit or a related entitlement fails to sign, set
/// `OMNIGART_ENABLE_WORKOUTKIT` to `0` in the Xcode target’s User-Defined settings
/// (see `docs/INSTALL_DEVICE.md` / `docs/WORKOUTKIT.md`).
enum OmniGartCapabilities {
    /// When false, WorkoutKit types are never instantiated; scheduling returns unsupported.
    static var workoutKitSchedulingCompiledIn: Bool {
        #if OMNIGART_ENABLE_WORKOUTKIT
        return true
        #else
        // Default ON for paid teams and CI; Personal Team can flip the build setting to 0.
        if let raw = Bundle.main.object(forInfoDictionaryKey: "OMNIGART_ENABLE_WORKOUTKIT") as? String {
            return raw != "0" && raw.lowercased() != "false"
        }
        return true
        #endif
    }

    @available(iOS 17.0, watchOS 10.0, *)
    static var workoutKitSchedulingAvailable: Bool {
        workoutKitSchedulingCompiledIn
    }
}
