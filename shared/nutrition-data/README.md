# nutrition-data

Open Food Facts offline pack format, parser, barcode lookup, and nutrient model for OmniGart.

## Licence

Open Food Facts data is © Open Food Facts contributors and available under the
**Open Database Licence (ODbL)**. You must:

1. Attribute Open Food Facts (see [`ATTRIBUTION.md`](ATTRIBUTION.md)).
2. Share-alike any public database derived from OFF under ODbL.
3. Keep notices with redistributed dumps / packs.

This module’s **code** is AGPL-3.0 (same as OmniGart). Pack **contents** remain ODbL.

## Layout

| Path | Role |
|------|------|
| `src/commonMain/.../model` | Nutrient + product models |
| `src/commonMain/.../pack` | Offline pack format + parser |
| `src/commonMain/.../lookup` | Barcode lookup interface + in-memory index |
| `src/commonTest` | Parser / lookup / nutrient tests |
| `fixtures/` | Tiny synthetic pack (not a full OFF dump) |

CDN delivery of large OFF packs is documented under the content-plane (`channel: off`); this module parses and queries a downloaded/bundled pack slice.
