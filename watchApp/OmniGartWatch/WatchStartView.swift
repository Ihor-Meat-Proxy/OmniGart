import SwiftUI

struct WatchStartView: View {
    let summaryLines: [String]

    var body: some View {
        NavigationStack {
            VStack(spacing: 8) {
                Text("app_name")
                    .font(.headline)
                    .accessibilityIdentifier("watch_brand")
                Text("watch_subtitle")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                ForEach(summaryLines.prefix(2), id: \.self) { line in
                    Text(line)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }
                Button("watch_start") {}
                    .accessibilityIdentifier("watch_start_button")
            }
            .padding()
            .accessibilityIdentifier("wear_start")
        }
    }
}

#Preview {
    WatchStartView(summaryLines: ["OmniGart", "BMR 1349 kcal"])
}
