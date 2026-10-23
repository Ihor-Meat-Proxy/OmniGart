import Shared

/// Bridges Swift `[Int64]` / `[Double]` into KMP `LongArray` / `DoubleArray` exports.
enum KotlinPrimitiveArrays {
    static func longs(_ values: [Int64]) -> KotlinLongArray {
        let array = KotlinLongArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }

    static func doubles(_ values: [Double]) -> KotlinDoubleArray {
        let array = KotlinDoubleArray(size: Int32(values.count))
        for (index, value) in values.enumerated() {
            array.set(index: Int32(index), value: value)
        }
        return array
    }
}
