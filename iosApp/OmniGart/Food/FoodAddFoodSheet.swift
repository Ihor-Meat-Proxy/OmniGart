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
                searchSection
                recentsSection
                favouritesSection
                quickAddSection
                customSection
                barcodeSection
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

    private var searchSection: some View {
        Section("food_search_title") {
            TextField("food_search_placeholder", text: $query)
                .accessibilityIdentifier("food_search")
            HStack {
                Text("food_portion_grams")
                TextField("food_portion_grams", text: $portionGrams)
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.trailing)
                    .accessibilityIdentifier("food_portion_grams")
            }
            ForEach(searchResults, id: \.id) { food in
                VStack(alignment: .leading, spacing: 4) {
                    Text(food.name)
                    Text(String(format: "%.0f kcal / 100 g", food.energyKcalPer100g))
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    HStack {
                        servingButton("food_serving_100g", food: food, serving: .per100G)
                        servingButton("food_serving_one", food: food, serving: .oneServing)
                        servingButton("food_serving_pack", food: food, serving: .pack)
                        Button("food_portion_grams") {
                            logGrams(food: food)
                        }
                        .font(.caption)
                    }
                }
                .accessibilityIdentifier("food_catalogue_\(food.id)")
            }
        }
    }

    private var searchResults: [CatalogueFood] {
        _ = tick
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        return Array(core.foodLogging.search(profileId: profileId, query: query, locale: localeTag))
    }

    private var recentsSection: some View {
        Section("food_recents_title") {
            let recents = Array(core.foodLogging.recents(profileId: profileId, limit: 8))
            if recents.isEmpty {
                Text("food_recents_empty").font(.footnote)
            } else {
                ForEach(recents, id: \.id) { entry in
                    Button {
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
                    } label: {
                        Text(entry.name)
                    }
                    .accessibilityIdentifier("food_recent_\(entry.id)")
                }
            }
        }
    }

    private var favouritesSection: some View {
        Section("food_favourites_title") {
            let favs = Array(core.foodLogging.favourites(profileId: profileId))
            if favs.isEmpty {
                Text("food_favourites_empty").font(.footnote)
            } else {
                ForEach(favs, id: \.id) { entry in
                    Button {
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
                    } label: {
                        Text(entry.name)
                    }
                    .accessibilityIdentifier("food_favourite_\(entry.id)")
                }
            }
        }
    }

    private var quickAddSection: some View {
        Section("food_quick_add_title") {
            TextField("food_name_placeholder", text: $quickName)
                .accessibilityIdentifier("food_name")
            macroField("food_energy_kcal", text: $quickKcal, id: "food_energy")
            macroField("food_protein_g", text: $quickProtein, id: "food_protein")
            macroField("food_carbs_g", text: $quickCarbs, id: "food_carbs")
            macroField("food_fat_g", text: $quickFat, id: "food_fat")
            Button("food_add") {
                let name = quickName.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { return }
                let kcal = Double(quickKcal) ?? 0
                let p = Double(quickProtein) ?? 0
                let c = Double(quickCarbs) ?? 0
                let f = Double(quickFat) ?? 0
                _ = core.foodLogging.quickAddNow(
                    profileId: profileId,
                    add: QuickMacroAdd(name: name, energyKcal: kcal, proteinG: p, carbsG: c, fatG: f),
                    mealGroup: meal
                )
                dualWrite(name: name, energy: kcal, protein: p, carbs: c, fat: f)
                quickName = ""
            }
            .accessibilityIdentifier("food_add_confirm")
        }
    }

    private var customSection: some View {
        Section("food_custom_name") {
            TextField("food_custom_name", text: $customName)
                .accessibilityIdentifier("food_custom_name")
            TextField("food_energy_kcal", text: $customKcal)
                .keyboardType(.decimalPad)
            Button("food_custom_save") {
                let name = customName.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { return }
                let kcal = Double(customKcal) ?? 0
                core.foodLogging.upsertCustomFoodSimple(
                    id: "custom-\(Int(Date().timeIntervalSince1970))",
                    profileId: profileId,
                    name: name,
                    energyKcalPer100g: kcal
                )
                customName = ""
                tick += 1
            }
            .accessibilityIdentifier("food_custom_save")
        }
    }

    private var barcodeSection: some View {
        Section("food_barcode_title") {
            TextField("food_barcode_manual", text: $barcode)
                .accessibilityIdentifier("food_barcode_manual")
            Button("food_barcode_lookup") {
                lookupBarcode(barcode)
            }
            .accessibilityIdentifier("food_barcode_lookup")
            Button("food_barcode_title") { showBarcodeScanner = true }
        }
    }

    private var localeTag: String { "en-GB" }

    private func servingButton(_ key: LocalizedStringKey, food: CatalogueFood, serving: QuickServing) -> some View {
        Button(key) {
            if let entry = core.foodLogging.logQuickServingNow(
                profileId: profileId,
                foodId: food.id,
                serving: serving,
                mealGroup: meal,
                locale: localeTag
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
        .font(.caption)
    }

    private func logGrams(food: CatalogueFood) {
        let grams = Double(portionGrams) ?? 100
        if let entry = core.foodLogging.logFromCatalogueNow(
            profileId: profileId,
            foodId: food.id,
            grams: grams,
            mealGroup: meal,
            locale: localeTag
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

    private func macroField(_ key: LocalizedStringKey, text: Binding<String>, id: String) -> some View {
        HStack {
            Text(key)
            TextField(key, text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .accessibilityIdentifier(id)
        }
    }
}
