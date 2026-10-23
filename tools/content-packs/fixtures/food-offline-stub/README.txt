Open Food Facts offline databases are large and update frequently.
They are never bundled in the app binary. The release pipeline publishes
signed packs under packs/food-offline/v/{semver}/ on the content CDN.
