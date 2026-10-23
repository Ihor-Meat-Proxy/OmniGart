import XCTest
@testable import OmniGart

final class HealthKitInjectorSmokeTests: XCTestCase {
    func testInjectorTypesResolve() {
        XCTAssertNotNil(HealthKitPlatformBridge.heartRateType())
        XCTAssertNotNil(HealthKitPlatformBridge.restingHeartRateType())
        XCTAssertNotNil(HealthKitPlatformBridge.dietaryWaterType())
        XCTAssertNotNil(HealthKitPlatformBridge.sleepType())
    }

    func testKeychainRoundTrip() throws {
        let store = KeychainTokenStore(service: "com.omnigart.test.\(UUID().uuidString)")
        // Unsigned CI (`CODE_SIGNING_ALLOWED=NO`) cannot write to the Keychain.
        try XCTSkipIf(
            !store.write(key: "access", value: "token-value"),
            "Keychain unavailable without signing entitlements"
        )
        XCTAssertEqual(store.read(key: "access"), "token-value")
        store.delete(key: "access")
        XCTAssertNil(store.read(key: "access"))
    }
}
