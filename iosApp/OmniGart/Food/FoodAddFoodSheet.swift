import Shared
import SwiftUI

/// Per-meal add flow: search, recents, favourites, quick add, custom, barcode, portion.
struct FoodAddFoodSheet: View {
    let core: OmniGartCore
    let profileId: String
    let meal: MealGroup
    var onLogged: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var portionGrams = "100"
    @State private var quickName = ""
    @State private var quickKcal = "250"
    @State private var quickProtein = "20"
    @State private var quickCarbs = "25"
    @State private var quickFat = "8"
    @State private var customName = ""
    @State private var customKcal = "100"
    @State private var barcode = ""
    @State private var showBarcodeScanner = false
    @State private var tick = 0

    var body: some View {
        NavigationStack {
            List {
                FoodAddSearchSection(
                    query: $query,
                    portionGrams: $portionGrams,
                    results: searchResults,
                    onServing: logServing,
                    onGrams: logGrams
                )
                FoodAddRecentsFavouritesSection(
                    recents: Array(core.foodLogging.recents(profileId: profileId, limit: 8)),
                    favourites: Array(core.foodLogging.favourites(profileId: profileId)),
                    onRepeat: repeatEntry
                )
                FoodAddQuickCustomSection(
                    quickName: $quickName,
                    quickKcal: $quickKcal,
                    quickProtein: $quickProtein,
                    quickCarbs: $quickCarbs,
                    quickFat: $quickFat,
                    customName: $customName,
                    customKcal: $customKcal,
                    onQuickAdd: quickAdd,
                    onCustomSave: saveCustom
                )
                FoodAddBarcodeSection(
                    barcode: $barcode,
                    onLookup: lookupBarcode,
                    onScan: { showBarcodeScanner = true }
                )
            }
            .navigationTitle(Text(mealTitle))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("onboarding_skip") { dismiss() }
                }
            }
            .sheet(isPresented: $showBarcodeScanner) {
                NavigationStack {
                    FoodBarcodeScannerView { code in
                        barcode = code
                        lookupBarcode(code)
                        showBarcodeScanner = false
                    }
                }
            }
        }
        .accessibilityIdentifier("food_add_sheet")
    }

    private var mealTitle: LocalizedStringKey {
        switch meal {
        case .breakfast: "food_meal_breakfast"
        case .lunch: "food_meal_lunch"
        case .dinner: "food_meal_dinner"
        case .snacks: "food_meal_snacks"
        default: "food_add_meal"
        }
    }

    private var searchResults: [CatalogueFood] {
        _ = tick
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        return Array(core.foodLogging.search(profileId: profileId, query: query, locale: "en-GB"))
    }

    private func logServing(food: CatalogueFood, serving: QuickServing) {
        if let entry = core.foodLogging.logQuickServingNow(
            profileId: profileId,
            foodId: food.id,
            serving: serving,
            mealGroup: meal,
            locale: "en-GB"
        ) {
            dualWrite(
                name: entry.name,
                energy: entry.energyKcal,
                protein: entry.proteinG,
                carbs: entry.carbsG,
                fat: entry.fatG
            )
        }
    }

    private func logGrams(food: CatalogueFood) {
        let grams = Double(portionGrams) ?? 100
        if let entry = core.foodLogging.logFromCatalogueNow(
            profileId: profileId,
            foodId: food.id,
            grams: grams,
            mealGroup: meal,
            locale: "en-GB"
        ) {
            dualWrite(
                name: entry.name,
                energy: entry.energyKcal,
                protein: entry.proteinG,
                carbs: entry.carbsG,
                fat: entry.fatG
            )
        }
    }

    private func repeatEntry(_ entry: FoodLogEntry) {
        _ = core.foodLogging.repeatRecentNow(
            profileId: profileId,
            entryId: entry.id,
            mealGroup: meal
        )
        dualWrite(
            name: entry.name,
            energy: entry.energyKcal,
            protein: entry.proteinG,
            carbs: entry.carbsG,
            fat: entry.fatG
        )
    }

    private func quickAdd() {
        let name = quickName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        let kcal = Double(quickKcal) ?? 0
        let protein = Double(quickProtein) ?? 0
        let carbs = Double(quickCarbs) ?? 0
        let fat = Double(quickFat) ?? 0
        _ = core.foodLogging.quickAddNow(
            profileId: profileId,
            add: QuickMacroAdd(name: name, energyKcal: kcal, proteinG: protein, carbsG: carbs, fatG: fat),
            mealGroup: meal
        )
        dualWrite(name: name, energy: kcal, protein: protein, carbs: carbs, fat: fat)
        quickName = ""
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

    private func lookupBarcode(_ code: String) {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              let food = core.foodLogging.findBarcode(profileId: profileId, barcode: trimmed)
        else { return }
        logGrams(food: food)
    }

    private func dualWrite(name: String, energy: Double, protein: Double, carbs: Double, fat: Double) {
        _ = core.logFood(
            profileId: profileId,
            name: name,
            energyKcal: energy,
            proteinG: protein,
            carbsG: carbs,
            fatG: fat
        )
        onLogged()
        dismiss()
    }
}
