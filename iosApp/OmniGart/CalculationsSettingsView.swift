import SwiftUI
import Shared

/// Profile Calculations settings: Simple/Advanced + formula domains.
struct CalculationsSettingsView: View {
    let core: OmniGartCore
    let profileId: String
    @State private var tick = 0

    private var selection: FormulaSelection {
        _ = tick
        return core.formulaSettings.getSelection(profileId: profileId)
    }

    var body: some View {
        let current = selection
        VStack(alignment: .leading, spacing: 8) {
            Text(catalog("calc_settings_title"))
                .font(.headline)
                .accessibilityIdentifier("calc_settings")
            Text(catalog("calc_settings_intro"))
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            HStack {
                modeButton(title: catalog("calc_mode_simple"), advanced: false, current: current)
                modeButton(title: catalog("calc_mode_advanced"), advanced: true, current: current)
            }

            domainBlock(domain: .bmr, titleKey: "calc_domain_bmr", current: current)
            domainBlock(domain: .hrMax, titleKey: "calc_domain_hr_max", current: current)
            domainBlock(domain: .bodyFat, titleKey: "calc_domain_body_fat", current: current)
            domainBlock(domain: .oneRm, titleKey: "calc_domain_one_rm", current: current)
            domainBlock(domain: .hrZones, titleKey: "calc_domain_hr_zones", current: current)
        }
    }

    private func modeButton(title: String, advanced: Bool, current: FormulaSelection) -> some View {
        let selected = (current.mode == .advanced) == advanced
        return Button {
            let mode: FormulaMode = advanced ? .advanced : .simple
            _ = core.formulaSettings.setMode(profileId: profileId, mode: mode)
            tick += 1
        } label: {
            Text(selected ? "● \(title)" : "○ \(title)")
                .font(.subheadline)
                .foregroundStyle(selected ? Color.OmniGart.colorBrandAccent : Color.OmniGart.colorTextLightPrimary)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(advanced ? "calc_mode_advanced" : "calc_mode_simple")
    }

    private func domainBlock(domain: FormulaDomain, titleKey: String, current: FormulaSelection) -> some View {
        let options = FormulaCatalog.shared.optionsFor(domain: domain, mode: current.mode)
        let selectedKey = current.keyFor(domain: domain)
        return VStack(alignment: .leading, spacing: 4) {
            Text(catalog(titleKey))
                .font(.subheadline.weight(.semibold))
            ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                Button {
                    _ = core.formulaSettings.updateDomain(
                        profileId: profileId,
                        domain: domain,
                        key: option.key
                    )
                    tick += 1
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(option.key == selectedKey ? "● \(catalog(option.titleKey))" : "○ \(catalog(option.titleKey))")
                        if option.key == selectedKey {
                            Text("ⓘ \(catalog(option.explanationKey))")
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("calc_option_\(domain)_\(option.key)")
            }
        }
        .padding(.top, 4)
    }

    private func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: "en-GB")
    }
}
