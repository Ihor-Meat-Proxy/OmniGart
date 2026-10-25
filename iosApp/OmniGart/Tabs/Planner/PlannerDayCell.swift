import SwiftUI

struct PlannerDayCellModel: Hashable {
    let dateIso: String
    let dayOfMonth: Int32
    let inMonth: Bool
    let hasPlanned: Bool
    let hasCompleted: Bool
}

struct PlannerDayCellView: View {
    let model: PlannerDayCellModel
    let selected: Bool
    let compact: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 2) {
                Text("\(model.dayOfMonth)")
                    .font(compact ? .caption.weight(.semibold) : .footnote.weight(.medium))
                    .foregroundStyle(
                        model.inMonth
                            ? Color.OmniGart.colorTextLightPrimary
                            : Color.OmniGart.colorTextLightSecondary.opacity(0.45)
                    )
                HStack(spacing: 2) {
                    if model.hasPlanned {
                        Circle()
                            .fill(Color.OmniGart.colorBrandAccent)
                            .frame(width: 5, height: 5)
                    }
                    if model.hasCompleted {
                        Circle()
                            .fill(Color.OmniGart.colorReadinessEasy)
                            .frame(width: 5, height: 5)
                    }
                }
                .frame(height: 6)
            }
            .frame(maxWidth: .infinity, minHeight: compact ? 36 : 40)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(selected ? Color.OmniGart.colorBrandAccentMuted : Color.clear)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("planner_day_\(model.dateIso)")
    }
}

struct PlannerWeekStripView: View {
    let days: [PlannerDayCellModel]
    let selectedDateIso: String
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 4) {
            ForEach(days, id: \.dateIso) { day in
                PlannerDayCellView(
                    model: day,
                    selected: day.dateIso == selectedDateIso,
                    compact: true,
                    onSelect: { onSelect(day.dateIso) }
                )
            }
        }
    }
}

struct PlannerMonthGridView: View {
    let days: [PlannerDayCellModel]
    let selectedDateIso: String
    let onSelect: (String) -> Void

    var body: some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
        LazyVGrid(columns: columns, spacing: 6) {
            ForEach(days, id: \.dateIso) { day in
                PlannerDayCellView(
                    model: day,
                    selected: day.dateIso == selectedDateIso,
                    compact: false,
                    onSelect: { onSelect(day.dateIso) }
                )
            }
        }
    }
}
