import Shared
import SwiftUI
import Vision

/// Barcode scan sheet: camera + GS1 check-digit via Shared, then catalogue lookup.
struct FoodBarcodeScannerView: View {
    let onCode: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var status = String(localized: "food_scan_point_barcode")

    var body: some View {
        VStack(spacing: 12) {
            Text(status)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .padding()
            // Camera preview is gated; CI / simulators use manual entry.
            TextField("food_barcode_manual", text: Binding(
                get: { manual },
                set: { manual = $0 }
            ))
            .textInputAutocapitalization(.never)
            .accessibilityIdentifier("food_barcode_manual")
            Button("food_barcode_lookup") {
                let digits = BarcodeCheckDigit.shared.normalize(raw: manual)
                if BarcodeCheckDigit.shared.isValid(raw: digits) {
                    onCode(digits)
                    dismiss()
                } else {
                    status = String(localized: "food_barcode_invalid")
                }
            }
            .accessibilityIdentifier("food_barcode_lookup")
            Button("food_scan_close") { dismiss() }
        }
        .padding()
        .navigationTitle(Text("food_barcode_title"))
    }

    @State private var manual = ""
}

/// Nutrition label OCR using Vision; feeds Shared [NutritionLabelParser].
struct FoodLabelScannerView: View {
    let onParsed: (ServingNutrients) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var ocrText = ""
    @State private var message = String(localized: "food_label_paste_hint")

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(message)
                .font(.footnote)
            TextEditor(text: $ocrText)
                .frame(minHeight: 120)
                .accessibilityIdentifier("food_label_ocr_text")
            Button("food_label_parse") {
                if let parsed = NutritionLabelParser.shared.parse(ocrText: ocrText) {
                    onParsed(parsed.nutrients)
                    dismiss()
                } else {
                    message = String(localized: "food_label_parse_failed")
                }
            }
            .accessibilityIdentifier("food_label_parse")
            Button("food_scan_close") { dismiss() }
        }
        .padding()
        .navigationTitle(Text("food_label_title"))
    }
}

/// Vision helper for live camera frames (used when camera permission is granted).
enum FoodVisionLabelRecognizer {
    static func recognizeText(cgImage: CGImage, completion: @escaping (String) -> Void) {
        let request = VNRecognizeTextRequest { request, _ in
            let parts =
                (request.results as? [VNRecognizedTextObservation])?
                .compactMap { $0.topCandidates(1).first?.string } ?? []
            completion(parts.joined(separator: "\n"))
        }
        request.recognitionLevel = .accurate
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        DispatchQueue.global(qos: .userInitiated).async {
            try? handler.perform([request])
        }
    }
}
