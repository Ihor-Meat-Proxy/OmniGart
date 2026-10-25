import SwiftUI

struct PlannerScheduleSection: View {
    @Binding var scheduledStart: Date
    let scheduleMessage: String?
    let onSchedule: () -> Void

    var body: some View {
        Section("planner_schedule_title") {
            Text("planner_schedule_body")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            DatePicker(
                "planner_schedule_when",
                selection: $scheduledStart,
                in: Date()...
            )
            .accessibilityIdentifier("planner_schedule_when")
            Button("planner_schedule_zone2", action: onSchedule)
                .accessibilityIdentifier("planner_schedule_zone2")
            if let scheduleMessage {
                Text(scheduleMessage)
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("planner_schedule_status")
            }
        }
    }
}
