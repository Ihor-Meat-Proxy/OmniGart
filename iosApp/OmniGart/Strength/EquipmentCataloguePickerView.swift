import Shared
import SwiftUI
import UIKit

/// Searchable multi-select equipment grid; illustration keys are `equip_<id>`.
struct EquipmentCataloguePickerView: View {
    let core: OmniGartCore
    let profileId: String
    var onChange: () -> Void = {}

    @State private var query = ""
    @State private var tick = 0
    @Environment(\.dismiss) private var dismiss

    private var library: RoutineLibraryService {
        _ = tick
        return core.routineLibrary
    }

    private var owned: Set<String> {
        _ = tick
        return Set(core.strengthPrograms.equipment(profileId: profileId).ownedIds.map { String(describing: $0) })
    }

    private var sections: [EquipmentGroupSection] {
        _ = tick
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return library.equipmentSections()
        }
        let hits = library.searchEquipment(query: trimmed)
        guard !hits.isEmpty else { return [] }
        return [
            EquipmentGroupSection(
                group: EquipmentGroup.accessories,
                nameKey: "equipment_search_results",
                items: hits
            )
        ]
    }

    private let columns = [GridItem(.adaptive(minimum: 96), spacing: OmniGartMetrics.space2)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
                TextField("equipment_search_placeholder", text: $query)
                    .textFieldStyle(.roundedBorder)
                    .accessibilityIdentifier("equipment_search")
                presetRow
                ForEach(sections, id: \.nameKey) { section in
                    Text(LocalizedStringKey(section.nameKey))
                        .font(.subheadline.weight(.semibold))
                        .accessibilityIdentifier("equipment_group_\(section.group.name)")
                    LazyVGrid(columns: columns, spacing: OmniGartMetrics.space2) {
                        ForEach(section.items, id: \.id) { item in
                            equipmentCell(item)
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle(Text("workouts_equipment_title"))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("onboarding_finish") { dismiss() }
            }
        }
    }

    private var presetRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: OmniGartMetrics.space2) {
                ForEach(library.equipmentPresets(), id: \.name) { preset in
                    Button {
                        _ = library.applyBrowsePreset(profileId: profileId, preset: preset)
                        tick += 1
                        onChange()
                    } label: {
                        Text(LocalizedStringKey(preset.nameKey))
                            .font(.caption)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.OmniGart.colorSurfaceLightElevated)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .accessibilityIdentifier("equipment_browse_preset_\(preset.name)")
                }
            }
        }
    }

    private func equipmentCell(_ item: EquipmentDefinition) -> some View {
        let selected = owned.contains(item.id)
        return Button {
            _ = core.strengthPrograms.toggleEquipment(
                profileId: profileId,
                equipmentId: item.id,
                enabled: !selected
            )
            tick += 1
            onChange()
        } label: {
            VStack(spacing: 6) {
                EquipmentIllustrationView(illustrationKey: item.illustrationKey)
                    .frame(width: 56, height: 56)
                Text(LocalizedStringKey(item.nameKey))
                    .font(.caption2)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Color.primary)
            }
            .padding(8)
            .frame(maxWidth: .infinity)
            .background(selected ? Color.OmniGart.colorBrandAccent.opacity(0.15) : Color.OmniGart.colorSurfaceLightElevated)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(selected ? Color.OmniGart.colorBrandAccent : Color.clear, lineWidth: 2)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("equipment_grid_\(item.id)")
    }
}

/// Neutral art stand-in when `equip_<id>` art is not yet in the bundle (art PR supplies assets).
struct EquipmentIllustrationView: View {
    let illustrationKey: String

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.OmniGart.colorSurfaceLightCanvas)
            if UIImage(named: illustrationKey) != nil {
                Image(illustrationKey)
                    .resizable()
                    .scaledToFit()
                    .padding(6)
            } else {
                Image(systemName: "dumbbell.fill")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .accessibilityHidden(true)
    }
}

struct ExerciseIllustrationView: View {
    let illustrationKey: String

    var body: some View {
        ZStack {
            Circle()
                .fill(Color.OmniGart.colorSurfaceLightElevated)
            if UIImage(named: illustrationKey) != nil {
                Image(illustrationKey)
                    .resizable()
                    .scaledToFit()
                    .clipShape(Circle())
                    .padding(4)
            } else {
                Image(systemName: "figure.strengthtraining.traditional")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .accessibilityHidden(true)
    }
}
