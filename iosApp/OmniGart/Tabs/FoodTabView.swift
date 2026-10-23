import Shared
import SwiftUI

/// Offline food log with daily wellness totals (manual entry).
struct FoodTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var name = ""
    @State private var energy = "250"
    @State private var protein = "20"
    @State private var carbs = "25"
    @State private var fat = "8"
    @State private var tick = 0

    private var entries: [FoodEntry] {
        _ = tick
        return core.foodEntriesForToday(profileId: profileId)
    }

    private var totals: FoodDayTotals {
        _ = tick
        return core.foodDayTotals(profileId: profileId)
    }

    var body: some View {
        List {
            Section {
                Text("food_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            NutritionTargetCard(core: core, profileId: profileId)
            Section("food_totals_title") {
                labeled("food_energy_kcal", value: String(format: "%.0f", totals.energyKcal))
                labeled("food_protein_g", value: String(format: "%.0f", totals.proteinG))
                labeled("food_carbs_g", value: String(format: "%.0f", totals.carbsG))
                labeled("food_fat_g", value: String(format: "%.0f", totals.fatG))
            }
            .accessibilityIdentifier("food_totals")
            Section("food_log_title") {
                TextField("food_name_placeholder", text: $name)
                    .accessibilityIdentifier("food_name")
                field("food_energy_kcal", text: $energy, id: "food_energy")
                field("food_protein_g", text: $protein, id: "food_protein")
                field("food_carbs_g", text: $carbs, id: "food_carbs")
                field("food_fat_g", text: $fat, id: "food_fat")
                Button("food_add") {
                    let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !trimmed.isEmpty else { return }
                    _ = core.logFood(
                        profileId: profileId,
                        name: trimmed,
                        energyKcal: Double(energy) ?? 0,
                        proteinG: Double(protein) ?? 0,
                        carbsG: Double(carbs) ?? 0,
                        fatG: Double(fat) ?? 0
                    )
                    name = ""
                    tick += 1
                }
                .accessibilityIdentifier("food_add")
            }
            Section("food_today_title") {
                if entries.isEmpty {
                    Text("food_empty")
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                } else {
                    ForEach(entries, id: \.id) { entry in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.name)
                            Text(
                                String(
                                    format: "%.0f kcal · P %.0f · C %.0f · F %.0f",
                                    entry.energyKcal,
                                    entry.proteinG,
                                    entry.carbsG,
                                    entry.fatG
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                        .accessibilityIdentifier("food_entry_\(entry.id)")
                    }
                }
            }
        }
        .navigationTitle(Text("tab_food"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }

    private func labeled(_ key: LocalizedStringKey, value: String) -> some View {
        HStack {
            Text(key)
            Spacer()
            Text(value)
                .monospacedDigit()
        }
    }

    private func field(_ key: LocalizedStringKey, text: Binding<String>, id: String) -> some View {
        HStack {
            Text(key)
            TextField(key, text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .accessibilityIdentifier(id)
        }
    }
}
