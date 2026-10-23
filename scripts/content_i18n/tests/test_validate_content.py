import json
from pathlib import Path

from content_i18n.config import REPO_ROOT, load_locales_config, required_locales
from content_i18n.validate import run


def test_all_content_locales_present_and_aligned():
    cfg = load_locales_config()
    locales_dir = REPO_ROOT / cfg["paths"]["content_locales"]
    source = json.loads((locales_dir / "en-GB.json").read_text(encoding="utf-8"))
    assert len(source) > 1000
    for loc in required_locales(cfg):
        path = locales_dir / f"{loc}.json"
        assert path.is_file(), loc
        data = json.loads(path.read_text(encoding="utf-8"))
        assert set(data) == set(source), loc
        assert all(str(v).strip() for v in data.values()), loc


def test_no_ru_locale_file():
    locales_dir = REPO_ROOT / "content/exercises/locales"
    names = {p.name for p in locales_dir.glob("*.json")}
    assert "ru.json" not in names
    assert not any(n.startswith("ru-") for n in names)


def test_validator_passes_on_repo():
    assert run([]) == 0


def test_glossary_has_required_columns():
    import csv

    cfg = load_locales_config()
    path = REPO_ROOT / cfg["paths"]["glossary"]
    with path.open(encoding="utf-8", newline="") as fh:
        rows = list(csv.DictReader(fh))
    assert len(rows) >= 180
    cols = set(rows[0].keys())
    for loc in required_locales(cfg):
        assert loc in cols
    assert "ru" not in cols
