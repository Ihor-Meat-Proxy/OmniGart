import XCTest
@testable import OmniGart

final class HealthKitInjectorSmokeTests: XCTestCase {
    func testInjectorTypesResolve() {
        XCTAssertNotNil(HealthKitPlatformBridge.heartRateType())
        XCTAssertNotNil(HealthKitPlatformBridge.restingHeartRateType())
        XCTAssertNotNil(HealthKitPlatformBridge.dietaryWaterType())
        XCTAssertNotNil(HealthKitPlatformBridge.sleepType())
    }

    func testKeychainRoundTrip() {
        let store = KeychainTokenStore(service: "com.omnigart.test.\(UUID().uuidString)")
        store.write(key: "access", value: "token-value")
        XCTAssertEqual(store.read(key: "access"), "token-value")
        store.delete(key: "access")
        XCTAssertNil(store.read(key: "access"))
    }
}
