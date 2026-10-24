import Shared
import SwiftUI

struct FoodAddSearchSection: View {
    @Binding var query: String
    @Binding var portionGrams: String
    let results: [CatalogueFood]
    var onServing: (CatalogueFood, QuickServing) -> Void
    var onGrams: (CatalogueFood) -> Void

    var body: some View {
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
            ForEach(results, id: \.id) { food in
                VStack(alignment: .leading, spacing: 4) {
                    Text(food.name)
                    Text(String(format: "%.0f kcal / 100 g", food.energyKcalPer100g))
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    HStack {
                        Button("food_serving_100g") { onServing(food, .per100G) }.font(.caption)
                        Button("food_serving_one") { onServing(food, .oneServing) }.font(.caption)
                        Button("food_serving_pack") { onServing(food, .pack) }.font(.caption)
                        Button("food_portion_grams") { onGrams(food) }.font(.caption)
                    }
                }
                .accessibilityIdentifier("food_catalogue_\(food.id)")
            }
        }
    }
}

struct FoodAddRecentsFavouritesSection: View {
    let recents: [FoodLogEntry]
    let favourites: [FoodLogEntry]
    var onRepeat: (FoodLogEntry) -> Void

    var body: some View {
        Section("food_recents_title") {
            if recents.isEmpty {
                Text("food_recents_empty").font(.footnote)
            } else {
                ForEach(recents, id: \.id) { entry in
                    Button { onRepeat(entry) } label: { Text(entry.name) }
                        .accessibilityIdentifier("food_recent_\(entry.id)")
                }
            }
        }
        Section("food_favourites_title") {
            if favourites.isEmpty {
                Text("food_favourites_empty").font(.footnote)
            } else {
                ForEach(favourites, id: \.id) { entry in
                    Button { onRepeat(entry) } label: { Text(entry.name) }
                        .accessibilityIdentifier("food_favourite_\(entry.id)")
                }
            }
        }
    }
}

struct FoodAddQuickCustomSection: View {
    @Binding var quickName: String
    @Binding var quickKcal: String
    @Binding var quickProtein: String
    @Binding var quickCarbs: String
    @Binding var quickFat: String
    @Binding var customName: String
    @Binding var customKcal: String
    var onQuickAdd: () -> Void
    var onCustomSave: () -> Void

    var body: some View {
        Section("food_quick_add_title") {
            TextField("food_name_placeholder", text: $quickName)
                .accessibilityIdentifier("food_name")
            macroField("food_energy_kcal", text: $quickKcal, id: "food_energy")
            macroField("food_protein_g", text: $quickProtein, id: "food_protein")
            macroField("food_carbs_g", text: $quickCarbs, id: "food_carbs")
            macroField("food_fat_g", text: $quickFat, id: "food_fat")
            Button("food_add", action: onQuickAdd)
                .accessibilityIdentifier("food_add_confirm")
        }
        Section("food_custom_name") {
            TextField("food_custom_name", text: $customName)
                .accessibilityIdentifier("food_custom_name")
            TextField("food_energy_kcal", text: $customKcal)
                .keyboardType(.decimalPad)
            Button("food_custom_save", action: onCustomSave)
                .accessibilityIdentifier("food_custom_save")
        }
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

struct FoodAddBarcodeSection: View {
    @Binding var barcode: String
    var onLookup: (String) -> Void
    var onScan: () -> Void

    var body: some View {
        Section("food_barcode_title") {
            TextField("food_barcode_manual", text: $barcode)
                .accessibilityIdentifier("food_barcode_manual")
            Button("food_barcode_lookup") { onLookup(barcode) }
                .accessibilityIdentifier("food_barcode_lookup")
            Button("food_barcode_title", action: onScan)
        }
    }
}
