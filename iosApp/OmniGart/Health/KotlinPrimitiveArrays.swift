import Foundation
import Shared

/// Bridges Swift arrays to Kotlin/Native primitive arrays for LocalHealthBridge.
enum KotlinPrimitiveArrays {
    static func longArray(_ values: [Int64]) -> KotlinLongArray {
        let array = KotlinLongArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }

    static func doubleArray(_ values: [Double]) -> KotlinDoubleArray {
        let array = KotlinDoubleArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }

    static func intArray(_ values: [Int32]) -> KotlinIntArray {
        let array = KotlinIntArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }

    static func booleanArray(_ values: [Bool]) -> KotlinBooleanArray {
        let array = KotlinBooleanArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }

    static func stringArray(_ values: [String]) -> KotlinArray<NSString> {
        KotlinArray(size: Int32(values.count)) { index in
            values[Int(truncating: index)] as NSString
        }
    }
}
