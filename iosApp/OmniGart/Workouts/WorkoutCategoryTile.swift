import SwiftUI

/// Dark/light raised rounded tile: coloured SF Symbol top-left, title below.
struct WorkoutCategoryTile: View {
    let category: WorkoutCategory
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Image(systemName: category.systemImage)
                .font(.title2.weight(.semibold))
                .foregroundStyle(category.accent)
                .symbolRenderingMode(.hierarchical)
                .accessibilityHidden(true)
            Text(category.titleKey)
                .font(.body.weight(.medium))
                .foregroundStyle(primaryText)
                .multilineTextAlignment(.leading)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? 3 : 2)
                .minimumScaleFactor(0.85)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, minHeight: tileMinHeight, alignment: .topLeading)
        .padding(OmniGartMetrics.space3)
        .background(tileBackground)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(category.titleKey))
        .accessibilityHint(Text("workout_cat_a11y_open"))
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier("workout_cat_tile_\(category.rawValue)")
    }

    private var tileMinHeight: CGFloat {
        dynamicTypeSize.isAccessibilitySize ? 96 : 88
    }

    private var tileBackground: Color {
        colorScheme == .dark
            ? Color.OmniGart.colorSurfaceDarkRaised
            : Color.OmniGart.colorSurfaceLightRaised
    }

    private var primaryText: Color {
        colorScheme == .dark
            ? Color.OmniGart.colorTextDarkPrimary
            : Color.OmniGart.colorTextLightPrimary
    }
}
