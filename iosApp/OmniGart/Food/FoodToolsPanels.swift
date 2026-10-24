import Shared
import SwiftUI

/// Copy day, scanners, import/export, custom foods, reference overrides.
struct FoodToolsPanel: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var tick: Int
    @State private var showBarcode = false
    @State private var showLabel = false
    @State private var showAttribution = false
    @State private var importMessage = ""
    @State private var importPaste = ""
    @State private var customName = ""
    @State private var customKcal = "100"
    @State private var overrideAmount = ""

    var body: some View {
        Section("food_tools_title") {
            foodToolsButtons
            FoodImportExportFields(
                core: core,
                profileId: profileId,
                importPaste: $importPaste,
                importMessage: $importMessage,
                tick: $tick
            )
            FoodCustomAndOverrideFields(
                core: core,
                profileId: profileId,
                customName: $customName,
                customKcal: $customKcal,
                overrideAmount: $overrideAmount,
                tick: $tick
            )
            if !importMessage.isEmpty {
                Text(importMessage).font(.caption)
            }
        }
        .sheet(isPresented: $showBarcode) { barcodeSheet }
        .sheet(isPresented: $showLabel) { labelSheet }
        .sheet(isPresented: $showAttribution) {
            NavigationStack { FoodAttributionView() }
        }
    }

    private var foodToolsButtons: some View {
        Group {
            Button("food_copy_yesterday", action: copyYesterday)
                .accessibilityIdentifier("food_copy_day")
            Button("food_barcode_title") { showBarcode = true }
            Button("food_label_title") { showLabel = true }
            Button("food_attr_nav_title") { showAttribution = true }
        }
    }

    private var barcodeSheet: some View {
        NavigationStack {
            FoodBarcodeScannerView { code in
                if let food = core.foodLogging.findBarcode(profileId: profileId, barcode: code) {
                    _ = core.foodLogging.logFromCatalogueNow(
                        profileId: profileId,
                        foodId: food.id,
                        grams: 100.0,
                        mealGroup: .snacks,
                        locale: "en-GB"
                    )
                    tick += 1
                }
            }
        }
    }

    private var labelSheet: some View {
        NavigationStack {
            FoodLabelScannerView { nutrients in
                _ = core.foodLogging.quickAddNow(
                    profileId: profileId,
                    add: QuickMacroAdd(
                        name: String(localized: "food_label_entry_name"),
                        energyKcal: nutrients.energyKcal,
                        proteinG: nutrients.proteinG,
                        carbsG: nutrients.carbsG,
                        fatG: nutrients.fatG
                    ),
                    mealGroup: .snacks
                )
                tick += 1
            }
        }
    }

    private func copyYesterday() {
        let endMs = Int64(Date().timeIntervalSince1970 * 1000)
        let startMs = endMs - 86_400_000
        _ = core.copyFoodDayMs(
            profileId: profileId,
            fromStartEpochMs: startMs - 86_400_000,
            fromEndEpochMs: startMs,
            toStartEpochMs: startMs
        )
        tick += 1
        importMessage = String(localized: "food_copy_done")
    }
}

private struct FoodImportExportFields: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var importPaste: String
    @Binding var importMessage: String
    @Binding var tick: Int

    var body: some View {
        TextField("food_import_paste", text: $importPaste, axis: .vertical)
            .lineLimit(2 ... 6)
            .accessibilityIdentifier("food_import_paste")
        Button("food_import_json") {
            let imported = core.foodLogging.importJson(profileId: profileId, text: importPaste)
            importMessage = String(format: String(localized: "food_import_done"), locale: .current, imported)
            tick += 1
        }
        .accessibilityIdentifier("food_import_json")
        Button("food_import_csv") {
            let imported = core.foodLogging.importCsv(profileId: profileId, text: importPaste)
            importMessage = String(format: String(localized: "food_import_done"), locale: .current, imported)
            tick += 1
        }
        .accessibilityIdentifier("food_import_csv")
        Button("food_export_json") {
            importPaste = core.foodLogging.exportJson(profileId: profileId)
            importMessage = String(localized: "food_export_ready")
        }
        .accessibilityIdentifier("food_export_json")
    }
}

private struct FoodCustomAndOverrideFields: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var customName: String
    @Binding var customKcal: String
    @Binding var overrideAmount: String
    @Binding var tick: Int

    var body: some View {
        TextField("food_custom_name", text: $customName)
            .accessibilityIdentifier("food_custom_name")
        TextField("food_energy_kcal", text: $customKcal)
            .keyboardType(.decimalPad)
        Button("food_custom_save", action: saveCustom)
            .accessibilityIdentifier("food_custom_save")
        TextField("food_override_amount", text: $overrideAmount)
            .keyboardType(.decimalPad)
            .accessibilityIdentifier("food_override_amount")
        Button("food_override_vitamin_c", action: applyOverride)
            .accessibilityIdentifier("food_override_vitamin_c")
    }

    private func saveCustom() {
        let name = customName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        core.foodLogging.upsertCustomFoodSimple(
            id: "custom-\(Int(Date().timeIntervalSince1970))",
            profileId: profileId,
            name: name,
            energyKcalPer100g: Double(customKcal) ?? 0
        )
        customName = ""
        tick += 1
    }

    private func applyOverride() {
        if let amount = Double(overrideAmount) {
            core.foodLogging.setReferenceOverrideAmount(
                profileId: profileId,
                nutrientRaw: NutrientId.vitaminC.raw,
                amount: amount
            )
        } else {
            core.foodLogging.clearReferenceOverride(
                profileId: profileId,
                nutrientRaw: NutrientId.vitaminC.raw
            )
        }
        tick += 1
    }
}
