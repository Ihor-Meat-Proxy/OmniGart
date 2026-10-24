import Shared
import SwiftUI

struct PlannerSuggestionRow: View {
    let suggestion: AdaptiveSuggestion
    let onAccept: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(LocalizedStringKey(suggestion.reasonKey))
                .font(.subheadline)
            Text(actionLabel)
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            HStack {
                Button("planner_suggest_accept", action: onAccept)
                    .accessibilityIdentifier("planner_suggest_accept_\(suggestion.id)")
                Button("planner_suggest_dismiss", action: onDismiss)
                    .accessibilityIdentifier("planner_suggest_dismiss_\(suggestion.id)")
            }
            .font(.caption)
        }
    }

    private var actionLabel: String {
        switch suggestion.action {
        case .move:
            let date = suggestion.proposedDateIso ?? ""
            let base = String(localized: "planner_suggest_action_move")
            return date.isEmpty ? base : "\(base) (\(date))"
        case .swap:
            return String(localized: "planner_suggest_action_swap")
        case .skip:
            return String(localized: "planner_suggest_action_skip")
        default:
            return suggestion.action.name
        }
    }
}
