import SwiftUI

/// Open Food Facts (ODbL) + USDA public-domain attribution screen.
struct FoodAttributionView: View {
    var body: some View {
        List {
            Section("food_attr_usda_title") {
                Text("food_attr_usda_body")
                    .font(.footnote)
            }
            Section("food_attr_off_title") {
                Text("food_attr_off_body")
                    .font(.footnote)
                if let off = URL(string: "https://world.openfoodfacts.org/") {
                    Link("food_attr_off_link", destination: off)
                }
                if let odbl = URL(string: "https://opendatacommons.org/licenses/odbl/1.0/") {
                    Link("food_attr_odbl_link", destination: odbl)
                }
            }
            Section {
                Text("food_online_privacy_note")
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .navigationTitle(Text("food_attr_nav_title"))
        .accessibilityIdentifier("food_attribution_screen")
    }
}
