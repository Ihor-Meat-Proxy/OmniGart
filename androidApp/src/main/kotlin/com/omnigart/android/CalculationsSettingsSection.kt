package com.omnigart.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.strategy.FormulaCatalog
import com.omnigart.shared.calc.strategy.FormulaDomain
import com.omnigart.shared.calc.strategy.FormulaMode
import com.omnigart.shared.calc.strategy.FormulaSelection
import com.omnigart.shared.i18n.StringCatalog

@Composable
fun CalculationsSettingsSection(core: OmniGartCore, profileId: String, onSelectionChanged: () -> Unit = {}) {
    var selection by remember { mutableStateOf(core.formulaSettings.getSelection(profileId)) }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("calc_settings")
            .padding(top = OmniGartSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text = stringResource(R.string.calc_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = OmniGartColors.colorTextLightPrimary
        )
        Text(
            text = stringResource(R.string.calc_settings_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        ModeRow(
            mode = selection.mode,
            onSelect = { mode ->
                selection = core.formulaSettings.setMode(profileId, mode)
                onSelectionChanged()
            }
        )
        FormulaDomain.entries.forEach { domain ->
            DomainPicker(
                domain = domain,
                selection = selection,
                onSelect = { key ->
                    selection = core.formulaSettings.updateDomain(profileId, domain, key)
                    onSelectionChanged()
                }
            )
        }
    }
}

@Composable
private fun ModeRow(mode: FormulaMode, onSelect: (FormulaMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1)) {
        ModeChoice(
            label = stringResource(R.string.calc_mode_simple),
            selected = mode == FormulaMode.SIMPLE,
            onClick = { onSelect(FormulaMode.SIMPLE) },
            testTag = "calc_mode_simple"
        )
        ModeChoice(
            label = stringResource(R.string.calc_mode_advanced),
            selected = mode == FormulaMode.ADVANCED,
            onClick = { onSelect(FormulaMode.ADVANCED) },
            testTag = "calc_mode_advanced"
        )
    }
}

@Composable
private fun ModeChoice(label: String, selected: Boolean, onClick: () -> Unit, testTag: String) {
    Text(
        text = if (selected) "● $label" else "○ $label",
        style = MaterialTheme.typography.bodyMedium,
        color =
        if (selected) {
            OmniGartColors.colorBrandAccent
        } else {
            OmniGartColors.colorTextLightPrimary
        },
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = OmniGartSpacing.space1)
    )
}

@Composable
private fun DomainPicker(domain: FormulaDomain, selection: FormulaSelection, onSelect: (String) -> Unit) {
    val options = FormulaCatalog.optionsFor(domain, selection.mode)
    val selectedKey = selection.keyFor(domain)
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1)) {
        Text(
            text = domainLabel(domain),
            style = MaterialTheme.typography.titleSmall,
            color = OmniGartColors.colorTextLightPrimary
        )
        options.forEach { option ->
            val selected = option.key == selectedKey
            // formula.* keys live in StringCatalog (dotted keys are not valid Android resource names).
            Text(
                text =
                if (selected) {
                    "● ${StringCatalog.t(option.titleKey)}"
                } else {
                    "○ ${StringCatalog.t(option.titleKey)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color =
                if (selected) {
                    OmniGartColors.colorBrandAccent
                } else {
                    OmniGartColors.colorTextLightPrimary
                },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.key) }
                    .testTag("calc_option_${domain.name}_${option.key}")
            )
            if (selected) {
                Text(
                    text = "ⓘ ${StringCatalog.t(option.explanationKey)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
            }
        }
    }
}

@Composable
private fun domainLabel(domain: FormulaDomain): String = when (domain) {
    FormulaDomain.BMR -> stringResource(R.string.calc_domain_bmr)
    FormulaDomain.HR_MAX -> stringResource(R.string.calc_domain_hr_max)
    FormulaDomain.BODY_FAT -> stringResource(R.string.calc_domain_body_fat)
    FormulaDomain.ONE_RM -> stringResource(R.string.calc_domain_one_rm)
    FormulaDomain.HR_ZONES -> stringResource(R.string.calc_domain_hr_zones)
}
