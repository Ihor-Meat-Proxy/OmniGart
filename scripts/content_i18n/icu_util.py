"""ICU MessageFormat placeholder / plural helpers (subset used by OmniGart)."""
from __future__ import annotations

import re
from dataclasses import dataclass

# Simple placeholders: {name}, {0}, {count}
SIMPLE_PLACEHOLDER = re.compile(r"\{([A-Za-z_][A-Za-z0-9_]*|\d+)\}")

# Complex: {count, plural, one {...} few {...} other {...}}
COMPLEX_ARG = re.compile(
    r"\{([A-Za-z_][A-Za-z0-9_]*)\s*,\s*(plural|select|selectordinal)\s*,",
    re.DOTALL,
)

PLURAL_CATEGORY = re.compile(
    r"(?:=?\d+|zero|one|two|few|many|other)\s*\{",
)


@dataclass(frozen=True)
class IcuIssue:
    message: str


def extract_simple_placeholders(text: str) -> set[str]:
    """Return simple {arg} names, excluding nested plural/select skeletons."""
    # Strip complex blocks first so inner braces do not confuse matching.
    stripped = _strip_complex(text)
    return set(SIMPLE_PLACEHOLDER.findall(stripped))


def _strip_complex(text: str) -> str:
    out = []
    i = 0
    while i < len(text):
        m = COMPLEX_ARG.search(text, i)
        if not m:
            out.append(text[i:])
            break
        out.append(text[i : m.start()])
        # walk braces from m.start()
        depth = 0
        j = m.start()
        while j < len(text):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    j += 1
                    break
            j += 1
        # keep a marker so positions stay meaningful for simple args outside
        out.append("{__complex__}")
        i = j
    return "".join(out)


def complex_arg_names(text: str) -> set[str]:
    return {m.group(1) for m in COMPLEX_ARG.finditer(text)}


def plural_categories_used(text: str) -> dict[str, set[str]]:
    """Map plural/select arg name -> set of category labels used."""
    result: dict[str, set[str]] = {}
    for m in COMPLEX_ARG.finditer(text):
        name, kind = m.group(1), m.group(2)
        # find matching closing brace block
        depth = 0
        start = m.start()
        end = start
        for j in range(start, len(text)):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    end = j + 1
                    break
        block = text[start:end]
        cats = set()
        for cm in re.finditer(
            r"(zero|one|two|few|many|other|=\d+)\s*\{",
            block,
        ):
            cats.add(cm.group(1))
        if kind in ("plural", "selectordinal"):
            result[name] = cats
    return result


def validate_icu(
    source: str,
    translated: str,
    locale: str,
    expected_plural_cats: list[str],
) -> list[IcuIssue]:
    issues: list[IcuIssue] = []
    src_simple = extract_simple_placeholders(source)
    dst_simple = extract_simple_placeholders(translated)
    src_complex = complex_arg_names(source)
    dst_complex = complex_arg_names(translated)

    if src_simple != dst_simple:
        issues.append(
            IcuIssue(
                f"placeholder mismatch: source={sorted(src_simple)} "
                f"locale={sorted(dst_simple)}"
            )
        )
    if src_complex != dst_complex:
        issues.append(
            IcuIssue(
                f"ICU arg mismatch: source={sorted(src_complex)} "
                f"locale={sorted(dst_complex)}"
            )
        )

    # Brace balance
    if translated.count("{") != translated.count("}"):
        issues.append(IcuIssue("unbalanced braces in ICU/message string"))

    expected = set(expected_plural_cats)
    for arg, cats in plural_categories_used(translated).items():
        # Exact =N forms are always allowed; named categories must be valid for locale
        named = {c for c in cats if not c.startswith("=")}
        if "other" not in named and named:
            issues.append(
                IcuIssue(f"plural arg '{arg}' missing required 'other' category")
            )
        unknown = named - expected - {"zero", "two"}  # zero/two optional extras
        # Restrict to CLDR set for locale (plus optional zero/two if in expected)
        illegal = named - expected
        if illegal:
            issues.append(
                IcuIssue(
                    f"plural arg '{arg}' uses categories {sorted(illegal)} "
                    f"not in CLDR set for {locale}: {sorted(expected)}"
                )
            )
        # Soft: if source used few/many and locale needs them, require presence when
        # source had a plural form for this arg
        src_cats = plural_categories_used(source).get(arg, set())
        if src_cats and expected.intersection({"few", "many"}):
            for need in ("few", "many"):
                if need in expected and need not in named and "other" in named:
                    # Missing few/many for slavic locales is an error when plural exists
                    issues.append(
                        IcuIssue(
                            f"plural arg '{arg}' for {locale} should define '{need}' "
                            f"(CLDR: {sorted(expected)})"
                        )
                    )
    return issues
