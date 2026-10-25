"""Contract tests mirroring OmniLocales.resolve (device → language → en-GB; never ru)."""

from __future__ import annotations

REQUIRED = [
    "en-GB",
    "uk",
    "pl",
    "be",
    "kk",
    "ka",
    "es",
    "fr",
    "de",
    "cs",
    "zh-Hans",
]

LANGUAGE_DEFAULTS = {
    "en": "en-GB",
    "uk": "uk",
    "pl": "pl",
    "be": "be",
    "kk": "kk",
    "ka": "ka",
    "es": "es",
    "fr": "fr",
    "de": "de",
    "cs": "cs",
    "zh": "zh-Hans",
}


def resolve(requested: str) -> str:
    normalized = requested.replace("_", "-").strip()
    if not normalized:
        return "en-GB"
    lower = normalized.lower()
    if lower == "ru" or lower.startswith("ru-"):
        return "en-GB"
    if normalized in REQUIRED:
        return normalized
    if lower == "zh" or lower.startswith("zh-"):
        return "zh-Hans"
    language = normalized.split("-", 1)[0]
    return LANGUAGE_DEFAULTS.get(language, "en-GB")


def test_exact_and_language_fallback() -> None:
    assert resolve("fr") == "fr"
    assert resolve("fr-CA") == "fr"
    assert resolve("de_AT") == "de"
    assert resolve("en-US") == "en-GB"
    assert resolve("pt-BR") == "en-GB"
    assert resolve("zh-CN") == "zh-Hans"
    assert resolve("zh_Hans_CN") == "zh-Hans"


def test_russian_never_selected() -> None:
    assert resolve("ru") == "en-GB"
    assert resolve("ru-RU") == "en-GB"
    assert all(not loc.startswith("ru") for loc in REQUIRED)
