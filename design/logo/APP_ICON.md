# OmniGart app icon & in-app picker

## Mark

The logo is an original **tempered ring**: a circular stroke (Omni) with a diagonal grit cut and temper spark — nodding to Ukrainian **гарт** (temper / grit). Master SVG: [`omnigart-mark.svg`](omnigart-mark.svg).

## Colour variants (6+)

| Id | Intent |
|----|--------|
| `default` | Brand teal |
| `ocean` | Deep sky |
| `forest` | Green grit |
| `sunset` | Warm ember |
| `graphite` | Neutral slate |
| `sakura` | Magenta blossom |

Sources: [`variants/*.svg`](variants/) (each with `-dark` and `-tinted` companions for iOS 18+ appearances). Preview: [`preview-variants.png`](preview-variants.png), mark [`preview-mark.png`](preview-mark.png).

Generated assets:

- iOS: `design/generated/ios/AppIcons/AppIcon-{variant}.appiconset/` (1024 light / dark / tinted)
- Android: `design/generated/android/mipmap/{variant}/` (adaptive foreground/background/monochrome + legacy)

## In-app “App icon” picker

### iOS (`setAlternateIconName`)

1. Ship primary icon in `AppIcon` and alternate sets named e.g. `AppIcon-ocean` in the asset catalog / build settings (`CFBundleAlternateIcons` in `Info.plist`).
2. Picker UI lists preview PNGs; on select call:

```swift
UIApplication.shared.setAlternateIconName(name) { error in
  // name == nil restores primary
}
```

3. System shows a confirmation alert (unavoidable).
4. **Appearances:** Provide light, dark, and tinted 1024px images per Apple Icon Composer / asset catalog appearances (iOS 18+). Tinted renders with the system accent; keep the mark as a simple silhouette for tinted.
5. **Limits:** Icons must be bundled (no download). Watch / Mac Catalyst may not honour phone alternates the same way. Changing icon does not restart the app.

### Android (activity-alias launchers)

1. Declare one `<activity-alias>` per variant targeting `MainActivity`, each with `android:icon` / `android:roundIcon` pointing at that variant’s adaptive icon. Only the active alias has `android:enabled="true"`.
2. On pick, disable all aliases and enable the selected one via `PackageManager.setComponentEnabledSetting`.
3. Prefer adaptive XML with **monochrome** layer for Material You themed icons.
4. **Limits (document in UI):**
   - Launcher shortcut / app row may **blink or restart**; some launchers drop long-press shortcuts.
   - Not all OEMs honour monochrome / themed icons equally.
   - Activity-alias switches can briefly remove the app from recents affinity on some devices.
   - Cannot change icon without a component state change (no silent swap API).

### Shared UX

- Settings → Appearance → App icon → grid of variants (use `preview-variants` crops).
- Show platform-specific footnote (iOS system alert vs Android launcher restart).
- Persist choice in app settings; on Android also persist enabled alias name for process death.

## Regeneration

```bash
cd design/tools && npm run logo
```
