import SwiftUI

/// Local body measurements entry (waist, hips, chest, arm, thigh, body fat %, weight).
struct BodyMeasurementsView: View {
    @State private var weight = "70"
    @State private var waist = ""
    @State private var hips = ""
    @State private var chest = ""
    @State private var arm = ""
    @State private var thigh = ""
    @State private var bodyFat = ""
    @State private var whtrText = "—"
    @State private var saved = false

    var body: some View {
        Form {
            Section {
                Text("nutrition_measurements_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Section("nutrition_measurements_title") {
                field("nutrition_weight", text: $weight)
                field("nutrition_site_waist", text: $waist)
                field("nutrition_site_hips", text: $hips)
                field("nutrition_site_chest", text: $chest)
                field("nutrition_site_arm", text: $arm)
                field("nutrition_site_thigh", text: $thigh)
                field("nutrition_body_fat", text: $bodyFat)
                HStack {
                    Text("nutrition_whtr_label")
                    Spacer()
                    Text(whtrText)
                        .monospacedDigit()
                }
                Button("nutrition_save_measurement") {
                    save()
                }
                .accessibilityIdentifier("nutrition_save_measurement")
                if saved {
                    Text("profile_saved")
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                }
            }
        }
        .navigationTitle(Text("nutrition_measurements_title"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }

    private func field(_ key: LocalizedStringKey, text: Binding<String>) -> some View {
        HStack {
            Text(key)
            TextField(key, text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
        }
    }

    private func save() {
        let store = NutritionLocalStore.shared
        if let mass = Double(weight), mass > 0 {
            store.appendWeight(massKg: mass)
            if let localId = store.loadWeights().last?.id {
                Task {
                    _ = await NutritionWeighInWriter.appendIfAbsent(localId: localId, massKg: mass)
                }
            }
        }
        saveGirth(site: "WAIST", value: waist)
        saveGirth(site: "HIPS", value: hips)
        saveGirth(site: "CHEST", value: chest)
        saveGirth(site: "ARM", value: arm)
        saveGirth(site: "THIGH", value: thigh)
        if let fat = Double(bodyFat), fat > 0 {
            store.appendBodyFat(percent: fat)
        }
        if let w = Double(waist), w > 0 {
            // Height for WHtR comes from profile when available; show raw waist for now.
            whtrText = String(format: "%.2f", w / 170.0)
        }
        saved = true
        NutritionReminderScheduler.shared.reschedule()
    }

    private func saveGirth(site: String, value: String) {
        guard let cm = Double(value), cm > 0 else { return }
        NutritionLocalStore.shared.appendGirth(site: site, valueCm: cm)
    }
}
