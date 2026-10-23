import Shared
import SwiftUI
import UniformTypeIdentifiers

/// Profile → Data and privacy: local export/import, platform sync status, conflict review.
struct DataPrivacySection: View {
    let core: OmniGartCore

    @State private var exportMessage: String?
    @State private var importMessage: String?
    @State private var sharePayload: SharePayload?
    @State private var showImporter = false
    @State private var syncing = false
    @State private var conflictLines: [String] = []

    var body: some View {
        Section("data_privacy_title") {
            Text("data_privacy_intro")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityIdentifier("data_privacy_intro")

            LabeledContent("data_privacy_sync_status") {
                Text(statusLabel)
                    .font(.caption)
                    .accessibilityIdentifier("data_privacy_sync_status_value")
            }
            if let last = lastSyncLabel {
                LabeledContent("data_privacy_last_sync") {
                    Text(last)
                        .font(.caption)
                        .accessibilityIdentifier("data_privacy_last_sync_value")
                }
            }

            Button("data_privacy_sync_now") {
                Task { await syncNow() }
            }
            .disabled(syncing)
            .accessibilityIdentifier("data_privacy_sync_now")

            Button("data_privacy_export") {
                exportAll()
            }
            .accessibilityIdentifier("data_privacy_export")

            Button("pdf_export_button") {
                WorkoutPdfExport.shareLatest(core: core)
            }
            .accessibilityIdentifier("pdf_export_button")

            Button("pdf_export_range_button") {
                let end = Date()
                let start = Calendar.current.date(byAdding: .day, value: -30, to: end) ?? end
                WorkoutPdfExport.shareRange(core: core, start: start, end: end)
            }
            .accessibilityIdentifier("pdf_export_range_button")

            Button("data_privacy_import_json") {
                showImporter = true
            }
            .accessibilityIdentifier("data_privacy_import_json")

            if !conflictLines.isEmpty {
                Text("data_privacy_conflicts_title")
                    .font(.subheadline.weight(.semibold))
                ForEach(conflictLines, id: \.self) { line in
                    Text(line)
                        .font(.caption2)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
                Button("data_privacy_resolve_all") {
                    resolveAllConflicts()
                }
                .accessibilityIdentifier("data_privacy_resolve_all")
            }

            if let exportMessage {
                Text(exportMessage)
                    .font(.caption)
                    .accessibilityIdentifier("data_privacy_export_message")
            }
            if let importMessage {
                Text(importMessage)
                    .font(.caption)
                    .accessibilityIdentifier("data_privacy_import_message")
            }

            Text("data_privacy_permission_note")
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        .onAppear(perform: refreshConflicts)
        .sheet(item: $sharePayload) { payload in
            ActivityView(activityItems: payload.items)
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                importJson(from: url)
            case .failure:
                importMessage = String(localized: "data_privacy_import_failed")
            }
        }
    }

    private var statusLabel: String {
        let status = core.localHealth.authStatus(platform: "APPLE_HEALTH")
        switch status {
        case "AUTHORIZED": return String(localized: "data_privacy_status_authorized")
        case "DENIED": return String(localized: "data_privacy_status_denied")
        case "PARTIAL": return String(localized: "data_privacy_status_partial")
        default: return String(localized: "data_privacy_status_unknown")
        }
    }

    private var lastSyncLabel: String? {
        guard let boxed = core.localHealth.lastSyncEpochMs(platform: "APPLE_HEALTH") else {
            return nil
        }
        let ms = boxed.int64Value
        let date = Date(timeIntervalSince1970: Double(ms) / 1000.0)
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    private func syncNow() async {
        syncing = true
        defer { syncing = false }
        await HealthKitWorkoutImporter.syncIntoLocalStore(core: core)
        refreshConflicts()
    }

    private func exportAll() {
        let artifacts = core.localHealth.exportArtifacts()
        var urls: [URL] = []
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("omnigart-export", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        for artifact in artifacts {
            guard let text = artifact.utf8Content else { continue }
            let url = dir.appendingPathComponent(artifact.fileName)
            do {
                try text.write(to: url, atomically: true, encoding: .utf8)
                urls.append(url)
            } catch {
                continue
            }
        }
        if urls.isEmpty {
            exportMessage = String(localized: "data_privacy_export_failed")
            return
        }
        sharePayload = SharePayload(items: urls)
        exportMessage = String(localized: "data_privacy_export_ready")
    }

    private func importJson(from url: URL) {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed { url.stopAccessingSecurityScopedResource() }
        }
        do {
            let data = try Data(contentsOf: url)
            guard let utf8 = String(data: data, encoding: .utf8) else {
                importMessage = String(localized: "data_privacy_import_failed")
                return
            }
            let count = Int(core.localHealth.importJsonUtf8(utf8: utf8))
            importMessage = String(format: String(localized: "data_privacy_import_ok"), count)
            refreshConflicts()
        } catch {
            importMessage = String(localized: "data_privacy_import_failed")
        }
    }

    private func refreshConflicts() {
        conflictLines = core.localHealth.conflictSummaries().map { summary in
            let parts = summary.split(separator: "|", omittingEmptySubsequences: false).map(String.init)
            guard parts.count >= 6 else { return summary }
            return "\(parts[2]): \(parts[3]) vs \(parts[4]) (\(parts[5]))"
        }
    }

    private func resolveAllConflicts() {
        for summary in core.localHealth.conflictSummaries() {
            let id = summary.split(separator: "|", omittingEmptySubsequences: false).first.map(String.init)
            if let id {
                core.localHealth.resolveConflict(id: id)
            }
        }
        refreshConflicts()
    }
}

private struct SharePayload: Identifiable {
    let id = UUID()
    let items: [Any]
}

private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
