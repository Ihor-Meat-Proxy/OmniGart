from content_i18n.icu_util import (
    extract_simple_placeholders,
    plural_categories_used,
    validate_icu,
)


def test_simple_placeholders():
    assert extract_simple_placeholders("Hello {name}, {count} reps") == {
        "name",
        "count",
    }


def test_plural_uk_categories():
    src = "{count, plural, one {# set} other {# sets}}"
    # Ukrainian must define few/many when pluralising
    bad = "{count, plural, one {# підхід} other {# підходів}}"
    issues = validate_icu(src, bad, "uk", ["one", "few", "many", "other"])
    assert any("few" in i.message or "many" in i.message for i in issues)

    good = (
        "{count, plural, one {# підхід} few {# підходи} "
        "many {# підходів} other {# підходів}}"
    )
    assert validate_icu(src, good, "uk", ["one", "few", "many", "other"]) == []


def test_placeholder_mismatch():
    issues = validate_icu("Go {distance}", "Allez {dist}", "fr", ["one", "other"])
    assert any(i.message.startswith("placeholder mismatch") for i in issues)


def test_plural_categories_used():
    text = "{n, plural, one {a} few {b} other {c}}"
    assert plural_categories_used(text)["n"] == {"one", "few", "other"}
