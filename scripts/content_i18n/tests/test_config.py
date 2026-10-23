from content_i18n.config import (
    load_locales_config,
    required_locales,
    source_locale,
    target_locales,
)


def test_locales_yml_source_of_truth():
    cfg = load_locales_config()
    assert source_locale(cfg) == "en-GB"
    required = required_locales(cfg)
    assert required[0] == "en-GB" or "en-GB" in required
    for loc in [
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
    ]:
        assert loc in required
    assert "ru" not in required
    assert "ru" not in target_locales(cfg)


def test_forbidden_includes_ru():
    cfg = load_locales_config()
    forbidden = {str(x).lower() for x in cfg.get("forbidden") or []}
    assert "ru" in forbidden
