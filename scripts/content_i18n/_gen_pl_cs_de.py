#!/usr/bin/env python3
"""Generate pl/cs/de content locale JSON from en-GB.json.

Translates unique English values (Bing via `translators`) with curated
overrides for exercise/template names and glossary sports terms.
Never pivots through Russian. Never writes ru*.
"""
from __future__ import annotations

import json
import re
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
EN_PATH = REPO / "content/exercises/locales/en-GB.json"
OUT_DIR = REPO / "content/exercises/locales"
CACHE_DIR = Path(__file__).resolve().parent / "data" / "_mt_cache"
CACHE_DIR.mkdir(parents=True, exist_ok=True)

LOCALES = ("pl", "cs", "de")

# --- Curated exercise + template names (sports-science terminology) ---------
NAMES: dict[str, dict[str, str]] = {
    # exercise names
    "A-skip drill": {
        "pl": "Skip A",
        "cs": "A-skip",
        "de": "A-Skip-Drill",
    },
    "Ab-wheel rollout": {
        "pl": "Rollout z kółkiem",
        "cs": "Rollout s ab-wheel",
        "de": "Ab-Wheel-Rollout",
    },
    "Band-assisted pull-up": {
        "pl": "Podciąganie z pomocą gumy",
        "cs": "Shyb s dopomocí gumy",
        "de": "Klimmzug mit Bandunterstützung",
    },
    "Back extension (Roman chair)": {
        "pl": "Prostowanie tułowia (Roman chair)",
        "cs": "Záklon na Roman chair",
        "de": "Rückenstrecker (Roman Chair)",
    },
    "Back squat": {
        "pl": "Przysiad ze sztangą z tyłu",
        "cs": "Dřep s činkou na zádech",
        "de": "Kniebeuge (Langhantel hinten)",
    },
    "Backstroke": {
        "pl": "Styl grzbietowy",
        "cs": "Znak",
        "de": "Rückenschwimmen",
    },
    "Band pull-apart": {
        "pl": "Rozciąganie gumy przed sobą",
        "cs": "Roztažení gumy před tělem",
        "de": "Band Pull-Apart",
    },
    "Barbell bench press": {
        "pl": "Wyciskanie sztangi na ławce",
        "cs": "Bench press s velkou činkou",
        "de": "Bankdrücken (Langhantel)",
    },
    "Barbell curl": {
        "pl": "Uginanie ramion ze sztangą",
        "cs": "Bicepsový zdvih s velkou činkou",
        "de": "Langhantel-Curl",
    },
    "Barbell row": {
        "pl": "Wiosłowanie sztangą",
        "cs": "Přítah velké činky v předklonu",
        "de": "Langhantelrudern",
    },
    "Bench dip (feet assisted)": {
        "pl": "Pompki na ławce (z pomocą nóg)",
        "cs": "Dipy na lavičce (s oporou nohou)",
        "de": "Bankdips (füßunterstützt)",
    },
    "Bird dog": {
        "pl": "Bird dog",
        "cs": "Bird dog",
        "de": "Bird Dog",
    },
    "Box squat": {
        "pl": "Przysiad na skrzynię",
        "cs": "Dřep na box",
        "de": "Box-Kniebeuge",
    },
    "Breaststroke": {
        "pl": "Żabka",
        "cs": "Prsa",
        "de": "Brustschwimmen",
    },
    "Bridge pose": {
        "pl": "Setu Bandhasana (mostek)",
        "cs": "Most (jógová pozice)",
        "de": "Setu Bandhasana (Brücke)",
    },
    "Brisk walk": {
        "pl": "Energiczny marsz",
        "cs": "Svižná chůze",
        "de": "Zügiges Gehen",
    },
    "Bulgarian split squat": {
        "pl": "Bułgarski przysiad wykroczny",
        "cs": "Bulharský dřep",
        "de": "Bulgarische Kniebeuge",
    },
    "Butt kicks": {
        "pl": "Uderzenia piętami w pośladki",
        "cs": "Zakopávání",
        "de": "Fersen-Kicks",
    },
    "Butterfly": {
        "pl": "Motylek",
        "cs": "Motýlek",
        "de": "Schmetterling",
    },
    "Cable chest fly": {
        "pl": "Rozpiętki na wyciągu",
        "cs": "Rozpažování na kladkách",
        "de": "Kabelzug-Flys",
    },
    "Cable woodchop": {
        "pl": "Rąbanie drewna na wyciągu",
        "cs": "Dřevorubec na kladce",
        "de": "Kabelzug-Woodchop",
    },
    "Standing calf raise": {
        "pl": "Wspięcia na palce stojąc",
        "cs": "Výpony ve stoji",
        "de": "Wadenheben im Stehen",
    },
    "Cat-cow": {
        "pl": "Kot–krowa (Marjaryasana–Bitilasana)",
        "cs": "Kočka–kráva",
        "de": "Cat-Cow (Katze–Kuh)",
    },
    "Catch-up drill": {
        "pl": "Ćwiczenie catch-up",
        "cs": "Catch-up drill",
        "de": "Catch-up-Drill",
    },
    "Chest-supported row": {
        "pl": "Wiosłowanie z podparciem klatki",
        "cs": "Přítah s oporou hrudníku",
        "de": "Brustgestütztes Rudern",
    },
    "Chest-to-bar pull-up": {
        "pl": "Podciąganie do klatki",
        "cs": "Shyb na prsa (chest-to-bar)",
        "de": "Chest-to-Bar-Klimmzug",
    },
    "Child's pose": {
        "pl": "Balasana (pozycja dziecka)",
        "cs": "Balasana (pozice dítěte)",
        "de": "Balasana (Kindhaltung)",
    },
    "Chin-up": {
        "pl": "Podciąganie podchwytem",
        "cs": "Shyb podhmatem",
        "de": "Klimmzug im Untergriff",
    },
    "Close-grip bench press": {
        "pl": "Wyciskanie wąskim chwytem",
        "cs": "Bench press úzkým úchopem",
        "de": "Enggriff-Bankdrücken",
    },
    "Conventional deadlift": {
        "pl": "Klasyczny martwy ciąg",
        "cs": "Konvenční mrtvý tah",
        "de": "Konventionelles Kreuzheben",
    },
    "Dead bug": {
        "pl": "Dead bug",
        "cs": "Dead bug",
        "de": "Dead Bug",
    },
    "Deficit deadlift": {
        "pl": "Martwy ciąg z deficytu",
        "cs": "Mrtvý tah z deficitu",
        "de": "Deficit-Kreuzheben",
    },
    "Diamond push-up": {
        "pl": "Pompki diamentowe",
        "cs": "Diamantové kliky",
        "de": "Diamant-Liegestütze",
    },
    "Parallel-bar dip": {
        "pl": "Pompki na poręczach",
        "cs": "Dipy na bradlech",
        "de": "Barren-Dips",
    },
    "Downward-facing dog": {
        "pl": "Adho Mukha Svanasana (pies głową w dół)",
        "cs": "Adho Mukha Svanasana (pes hlavou dolů)",
        "de": "Adho Mukha Svanasana (herabschauender Hund)",
    },
    "Dumbbell bench press": {
        "pl": "Wyciskanie hantli na ławce",
        "cs": "Tlak jednoruček na lavici",
        "de": "Kurzhantel-Bankdrücken",
    },
    "Dumbbell curl": {
        "pl": "Uginanie ramion z hantlami",
        "cs": "Bicepsový zdvih s jednoručkami",
        "de": "Kurzhantel-Curl",
    },
    "Incline dumbbell press": {
        "pl": "Wyciskanie hantli na skosie",
        "cs": "Šikmý tlak s jednoručkami",
        "de": "Schrägbankdrücken mit Kurzhanteln",
    },
    "Easy run": {
        "pl": "Łatwy bieg",
        "cs": "Snadný běh",
        "de": "Lockerer Lauf",
    },
    "Face pull": {
        "pl": "Face pull",
        "cs": "Face pull",
        "de": "Face Pull",
    },
    "Farmer's carry": {
        "pl": "Spacer farmera",
        "cs": "Farmářská chůze",
        "de": "Farmer’s Walk",
    },
    "Dumbbell floor press": {
        "pl": "Wyciskanie hantli z podłogi",
        "cs": "Tlak jednoruček z podlahy",
        "de": "Floor Press mit Kurzhanteln",
    },
    "Freestyle (front crawl)": {
        "pl": "Kraul",
        "cs": "Kraul",
        "de": "Kraul (Freistil)",
    },
    "Front squat": {
        "pl": "Przysiad przedni",
        "cs": "Dřep s činkou vpředu",
        "de": "Frontkniebeuge",
    },
    "Glute bridge": {
        "pl": "Mostek biodrowy",
        "cs": "Glute bridge",
        "de": "Gesäßbrücke",
    },
    "Goblet lateral squat": {
        "pl": "Przysiad boczny goblet",
        "cs": "Boční goblet dřep",
        "de": "Goblet-Seitkniebeuge",
    },
    "Goblet squat": {
        "pl": "Przysiad goblet",
        "cs": "Goblet dřep",
        "de": "Goblet-Kniebeuge",
    },
    "Good morning": {
        "pl": "Good morning",
        "cs": "Good morning",
        "de": "Good Morning",
    },
    "Hack squat (machine)": {
        "pl": "Hack squat (maszyna)",
        "cs": "Hack squat (stroj)",
        "de": "Hackenschmidt-Kniebeuge (Maschine)",
    },
    "Hanging knee raise": {
        "pl": "Unoszenie kolan w zwisie",
        "cs": "Zvedání kolen ve visu",
        "de": "Hängendes Anheben der Knie",
    },
    "High-knees drill": {
        "pl": "Skip C (wysokie kolana)",
        "cs": "Vysoká kolena",
        "de": "High-Knees-Drill",
    },
    "Hip abduction machine": {
        "pl": "Odwodzenie bioder (maszyna)",
        "cs": "Abdukce kyčlí na stroji",
        "de": "Hüftabduktion (Maschine)",
    },
    "Hip adduction machine": {
        "pl": "Przywodzenie bioder (maszyna)",
        "cs": "Addukce kyčlí na stroji",
        "de": "Hüftadduktion (Maschine)",
    },
    "Hip hinge (dowel drill)": {
        "pl": "Zawias biodrowy (z kijem)",
        "cs": "Kyčelní kyvadlo (s tyčí)",
        "de": "Hüftbeuge (Stab-Drill)",
    },
    "Hip thrust": {
        "pl": "Hip thrust",
        "cs": "Hip thrust",
        "de": "Hip Thrust",
    },
    "Incline push-up": {
        "pl": "Pompki z rękami na podwyższeniu",
        "cs": "Kliky s rukama výše",
        "de": "Schräge Liegestütze",
    },
    "Interval run": {
        "pl": "Bieg interwałowy",
        "cs": "Intervalový běh",
        "de": "Intervalllauf",
    },
    "Inverted row": {
        "pl": "Wiosłowanie w podporze (inverted row)",
        "cs": "Australské shyby",
        "de": "Inverted Row",
    },
    "Kettlebell swing": {
        "pl": "Swing kettlebell",
        "cs": "Kettlebell swing",
        "de": "Kettlebell-Swing",
    },
    "Kickboard flutter/dolphin kick": {
        "pl": "Kopnięcia z deską (flutter/delfin)",
        "cs": "Kopy s deskou (flutter/delfín)",
        "de": "Beinschlag mit Kickboard (Flutter/Delfin)",
    },
    "Knee push-up": {
        "pl": "Pompki na kolanach",
        "cs": "Kliky na kolenou",
        "de": "Liegestütze auf den Knien",
    },
    "Landmine press": {
        "pl": "Wyciskanie landmine",
        "cs": "Landmine press",
        "de": "Landmine-Press",
    },
    "Lat pulldown": {
        "pl": "Ściąganie drążka wyciągu górnego",
        "cs": "Stahování kladky",
        "de": "Latzug",
    },
    "Lateral lunge": {
        "pl": "Wypad boczny",
        "cs": "Boční výpad",
        "de": "Seitlicher Ausfallschritt",
    },
    "Lateral raise": {
        "pl": "Unoszenie bokiem",
        "cs": "Upažení",
        "de": "Seitheben",
    },
    "Lying/seated leg curl": {
        "pl": "Uginanie nóg (maszyna)",
        "cs": "Zakopávání (stroj)",
        "de": "Beinbeuger (Maschine)",
    },
    "Leg extension": {
        "pl": "Prostowanie nóg (maszyna)",
        "cs": "Předkopávání",
        "de": "Beinstrecker",
    },
    "Leg press": {
        "pl": "Wypychanie nogami",
        "cs": "Leg press",
        "de": "Beinpresse",
    },
    "Low lunge": {
        "pl": "Niski wypad (Anjaneyasana)",
        "cs": "Nízký výpad",
        "de": "Tiefer Ausfallschritt",
    },
    "Machine chest press": {
        "pl": "Wyciskanie na maszynie (klatka)",
        "cs": "Tlak na stroji (hruď)",
        "de": "Brustpresse (Maschine)",
    },
    "Machine shoulder press": {
        "pl": "Wyciskanie na maszynie (barki)",
        "cs": "Tlak na stroji (ramena)",
        "de": "Schulterpresse (Maschine)",
    },
    "Mountain pose": {
        "pl": "Tadasana (pozycja góry)",
        "cs": "Tadasana (pozice hory)",
        "de": "Tadasana (Berghaltung)",
    },
    "Nordic curl (assisted)": {
        "pl": "Nordic curl (z pomocą)",
        "cs": "Nordic curl (s dopomocí)",
        "de": "Nordic Curl (assistiert)",
    },
    "One-arm dumbbell row": {
        "pl": "Wiosłowanie hantlem jednorącz",
        "cs": "Přítah jednoručky",
        "de": "Einarmiges Kurzhantelrudern",
    },
    "Overhead press (barbell)": {
        "pl": "Wyciskanie sztangi nad głowę",
        "cs": "Tlak velké činky nad hlavu",
        "de": "Überkopfdrücken (Langhantel)",
    },
    "Overhead press (dumbbell)": {
        "pl": "Wyciskanie hantli nad głowę",
        "cs": "Tlak jednoruček nad hlavu",
        "de": "Überkopfdrücken (Kurzhantel)",
    },
    "Paddle freestyle": {
        "pl": "Kraul z łapkami",
        "cs": "Kraul s pádly",
        "de": "Kraul mit Paddles",
    },
    "Pallof press": {
        "pl": "Pallof press",
        "cs": "Pallof press",
        "de": "Pallof Press",
    },
    "Pause back squat": {
        "pl": "Przysiad z pauzą (sztanga z tyłu)",
        "cs": "Dřep s pauzou (činka na zádech)",
        "de": "Pause-Kniebeuge (hinten)",
    },
    "Pause bench press": {
        "pl": "Wyciskanie na ławce z pauzą",
        "cs": "Bench press s pauzou",
        "de": "Pause-Bankdrücken",
    },
    "Pause deadlift": {
        "pl": "Martwy ciąg z pauzą",
        "cs": "Mrtvý tah s pauzou",
        "de": "Pause-Kreuzheben",
    },
    "Pause front squat": {
        "pl": "Przysiad przedni z pauzą",
        "cs": "Přední dřep s pauzou",
        "de": "Pause-Frontkniebeuge",
    },
    "Pec deck": {
        "pl": "Pec deck (motyl)",
        "cs": "Pec deck",
        "de": "Pec Deck",
    },
    "Pigeon pose": {
        "pl": "Eka Pada Rajakapotasana (gołąb)",
        "cs": "Holubí pozice",
        "de": "Taubenhaltung",
    },
    "Double-leg stretch": {
        "pl": "Double-leg stretch",
        "cs": "Double-leg stretch",
        "de": "Double-Leg Stretch",
    },
    "The hundred": {
        "pl": "The Hundred",
        "cs": "The Hundred",
        "de": "The Hundred",
    },
    "Leg circles": {
        "pl": "Leg circles",
        "cs": "Leg circles",
        "de": "Leg Circles",
    },
    "Pilates plank / leg-pull prep": {
        "pl": "Plank pilates / leg-pull prep",
        "cs": "Pilates plank / leg-pull prep",
        "de": "Pilates-Plank / Leg-Pull Prep",
    },
    "Roll-up": {
        "pl": "Roll-up",
        "cs": "Roll-up",
        "de": "Roll-up",
    },
    "Saw": {
        "pl": "Saw",
        "cs": "Saw",
        "de": "Saw",
    },
    "Shoulder bridge": {
        "pl": "Shoulder bridge",
        "cs": "Shoulder bridge",
        "de": "Shoulder Bridge",
    },
    "Side-kick series": {
        "pl": "Side-kick series",
        "cs": "Side-kick series",
        "de": "Side-Kick Series",
    },
    "Single-leg stretch": {
        "pl": "Single-leg stretch",
        "cs": "Single-leg stretch",
        "de": "Single-Leg Stretch",
    },
    "Swan": {
        "pl": "Swan",
        "cs": "Swan",
        "de": "Swan",
    },
    "Plank": {
        "pl": "Deska",
        "cs": "Prkno",
        "de": "Unterarmstütz (Plank)",
    },
    "Pull buoy freestyle": {
        "pl": "Kraul z pull buoy",
        "cs": "Kraul s pull buoy",
        "de": "Kraul mit Pullbuoy",
    },
    "Pull-up": {
        "pl": "Podciąganie",
        "cs": "Shyb",
        "de": "Klimmzug",
    },
    "Push press": {
        "pl": "Push press",
        "cs": "Push press",
        "de": "Push Press",
    },
    "Push-up": {
        "pl": "Pompka",
        "cs": "Klik",
        "de": "Liegestütz",
    },
    "Reverse lunge (dumbbell)": {
        "pl": "Wypad w tył z hantlami",
        "cs": "Výpad vzad s jednoručkami",
        "de": "Rückwärtiger Ausfallschritt (Kurzhantel)",
    },
    "Ring dip": {
        "pl": "Pompki na kółkach",
        "cs": "Dipy na kruzích",
        "de": "Ring-Dips",
    },
    "Romanian deadlift (barbell)": {
        "pl": "Rumuński martwy ciąg (sztanga)",
        "cs": "Rumunský mrtvý tah (velká činka)",
        "de": "Rumänisches Kreuzheben (Langhantel)",
    },
    "Romanian deadlift (dumbbell)": {
        "pl": "Rumuński martwy ciąg (hantle)",
        "cs": "Rumunský mrtvý tah (jednoručky)",
        "de": "Rumänisches Kreuzheben (Kurzhantel)",
    },
    "Russian twist (bodyweight)": {
        "pl": "Russian twist (masa ciała)",
        "cs": "Russian twist (vlastní váha)",
        "de": "Russian Twist (Körpergewicht)",
    },
    "Seated cable row": {
        "pl": "Wiosłowanie siedząc na wyciągu",
        "cs": "Sedící přítah na kladce",
        "de": "Sitzendes Kabelrudern",
    },
    "Seated forward fold": {
        "pl": "Paschimottanasana (skłon w siadzie)",
        "cs": "Předklon v sedu",
        "de": "Sitzende Vorbeuge",
    },
    "Dumbbell shrug": {
        "pl": "Szrugsy z hantlami",
        "cs": "Krčení ramen s jednoručkami",
        "de": "Kurzhantel-Shrugs",
    },
    "Side plank": {
        "pl": "Deska boczna",
        "cs": "Boční prkno",
        "de": "Seitlicher Unterarmstütz",
    },
    "Single-leg hip thrust": {
        "pl": "Hip thrust jednonóż",
        "cs": "Hip thrust na jedné noze",
        "de": "Einbeiniger Hip Thrust",
    },
    "Single-leg RDL": {
        "pl": "RDL jednonóż",
        "cs": "RDL na jedné noze",
        "de": "Einbeiniges RDL",
    },
    "Sit-to-stand": {
        "pl": "Wstawanie z krzesła",
        "cs": "Vstávání ze židle",
        "de": "Aufstehen vom Stuhl",
    },
    "Smith-machine squat": {
        "pl": "Przysiad w suwnicy Smitha",
        "cs": "Dřep ve Smithově stroji",
        "de": "Kniebeuge an der Multipresse",
    },
    "Split squat (bodyweight)": {
        "pl": "Przysiad wykroczny (masa ciała)",
        "cs": "Split squat (vlastní váha)",
        "de": "Ausfallkniebeuge (Körpergewicht)",
    },
    "Step-up": {
        "pl": "Wchodzenie na podwyższenie",
        "cs": "Výstupy na box",
        "de": "Step-ups",
    },
    "Suitcase carry": {
        "pl": "Noszenie walizkowe",
        "cs": "Suitcase carry",
        "de": "Suitcase Carry",
    },
    "Sumo deadlift": {
        "pl": "Martwy ciąg sumo",
        "cs": "Sumo mrtvý tah",
        "de": "Sumo-Kreuzheben",
    },
    "Tempo run": {
        "pl": "Bieg tempo",
        "cs": "Tempový běh",
        "de": "Tempolauf",
    },
    "Three-legged dog": {
        "pl": "Pies z uniesioną nogą",
        "cs": "Pes se zdviženou nohou",
        "de": "Dreibeiniger Hund",
    },
    "Trap-bar deadlift": {
        "pl": "Martwy ciąg z trap barem",
        "cs": "Mrtvý tah s trap barem",
        "de": "Trap-Bar-Kreuzheben",
    },
    "Tree pose": {
        "pl": "Vrksasana (pozycja drzewa)",
        "cs": "Vrksasana (pozice stromu)",
        "de": "Vrksasana (Baumhaltung)",
    },
    "Triangle pose": {
        "pl": "Trikonasana (trójkąt)",
        "cs": "Trikonasana (trojúhelník)",
        "de": "Trikonasana (Dreieck)",
    },
    "Triceps pushdown": {
        "pl": "Prostowanie ramion na wyciągu",
        "cs": "Stahování kladky na triceps",
        "de": "Trizeps-Pushdown",
    },
    "Walking lunge": {
        "pl": "Wypady marszowe",
        "cs": "Chodící výpady",
        "de": "Ausfallschritte im Gehen",
    },
    "Warrior I": {
        "pl": "Virabhadrasana I (wojownik I)",
        "cs": "Víra I (Warrior I)",
        "de": "Virabhadrasana I (Krieger I)",
    },
    "Warrior II": {
        "pl": "Virabhadrasana II (wojownik II)",
        "cs": "Víra II (Warrior II)",
        "de": "Virabhadrasana II (Krieger II)",
    },
    "Weighted pull-up": {
        "pl": "Podciąganie z obciążeniem",
        "cs": "Shyb s přídavnou zátěží",
        "de": "Klimmzug mit Zusatzgewicht",
    },
    "Wheel pose": {
        "pl": "Urdhva Dhanurasana (koło)",
        "cs": "Urdhva Dhanurasana (kolo)",
        "de": "Urdhva Dhanurasana (Rad)",
    },
    "World's greatest stretch": {
        "pl": "World’s greatest stretch",
        "cs": "World’s greatest stretch",
        "de": "World’s Greatest Stretch",
    },
    "Wrist curl": {
        "pl": "Uginanie nadgarstków",
        "cs": "Zdvihy zápěstí",
        "de": "Handgelenkcurl",
    },
}

# Template names + short UI labels that also appear as values
TEMPLATE_AND_SHORT: dict[str, dict[str, str]] = {
    "5K beginner run block": {
        "pl": "Blok biegowy 5K dla początkujących",
        "cs": "Blok běhu 5K pro začátečníky",
        "de": "5-km-Laufblock für Einsteiger",
    },
    "Easy + drills": {
        "pl": "Łatwy bieg + drill’e",
        "cs": "Snadný běh + drily",
        "de": "Locker + Drills",
    },
    "Intervals": {
        "pl": "Interwały",
        "cs": "Intervaly",
        "de": "Intervalle",
    },
    "Easy longish": {
        "pl": "Łatwy dłuższy bieg",
        "cs": "Snadný delší běh",
        "de": "Locker, etwas länger",
    },
    "Travel bodyweight circuit (3×/week)": {
        "pl": "Obwód z masą ciała w podróży (3×/tydzień)",
        "cs": "Cestovní okruh s vlastní váhou (3×/týden)",
        "de": "Reise-Körpergewichts-Zirkel (3×/Woche)",
    },
    "Travel circuit": {
        "pl": "Obwód podróżny",
        "cs": "Cestovní okruh",
        "de": "Reise-Zirkel",
    },
    "Lift A": {"pl": "Siłowy A", "cs": "Síla A", "de": "Kraft A"},
    "Lift B": {"pl": "Siłowy B", "cs": "Síla B", "de": "Kraft B"},
    "Run drills + easy": {
        "pl": "Drill’e biegowe + łatwy bieg",
        "cs": "Běžecké drily + snadný běh",
        "de": "Lauf-Drills + locker",
    },
    "Concurrent lift + easy run": {
        "pl": "Siłowy + łatwy bieg (równolegle)",
        "cs": "Souběžná síla + snadný běh",
        "de": "Kraft + lockerer Lauf (parallel)",
    },
    "Posterior + open": {
        "pl": "Tylny łańcuch + otwarcie",
        "cs": "Zadní řetězec + otevření",
        "de": "Hintere Kette + Öffnen",
    },
    "Legs + hips": {
        "pl": "Nogi + biodra",
        "cs": "Nohy + kyčle",
        "de": "Beine + Hüften",
    },
    "Push + brace": {
        "pl": "Push + usztywnienie",
        "cs": "Push + zpevnění",
        "de": "Push + Rumpfspannung",
    },
    "Desk-worker strength + mobility": {
        "pl": "Siła + mobilność dla pracujących przy biurku",
        "cs": "Síla + mobilita pro práci u stolu",
        "de": "Kraft + Mobilität für Schreibtischarbeit",
    },
    "Day A": {"pl": "Dzień A", "cs": "Den A", "de": "Tag A"},
    "Day B": {"pl": "Dzień B", "cs": "Den B", "de": "Tag B"},
    "Day C": {"pl": "Dzień C", "cs": "Den C", "de": "Tag C"},
    "Full-body beginner (3×/week)": {
        "pl": "Całe ciało — początkujący (3×/tydzień)",
        "cs": "Celé tělo — začátečníci (3×/týden)",
        "de": "Ganzkörper Einsteiger (3×/Woche)",
    },
    "Hinge day": {
        "pl": "Dzień zawiasu",
        "cs": "Den kyvadla",
        "de": "Hüftbeuge-Tag",
    },
    "Squat + thrust": {
        "pl": "Przysiad + thrust",
        "cs": "Dřep + thrust",
        "de": "Kniebeuge + Thrust",
    },
    "Mixed posterior": {
        "pl": "Mieszany tylny łańcuch",
        "cs": "Smíšený zadní řetězec",
        "de": "Gemischte hintere Kette",
    },
    "Posterior-chain emphasis (3×/week)": {
        "pl": "Akcent na tylny łańcuch (3×/tydzień)",
        "cs": "Důraz na zadní řetězec (3×/týden)",
        "de": "Fokus hintere Kette (3×/Woche)",
    },
    "Home A": {"pl": "Dom A", "cs": "Domácí A", "de": "Zuhause A"},
    "Home B": {"pl": "Dom B", "cs": "Domácí B", "de": "Zuhause B"},
    "Home C": {"pl": "Dom C", "cs": "Domácí C", "de": "Zuhause C"},
    "Minimal-equipment home (3×/week)": {
        "pl": "Dom — minimalny sprzęt (3×/tydzień)",
        "cs": "Domácí trénink s minimem vybavení (3×/týden)",
        "de": "Zuhause mit Minimalausrüstung (3×/Woche)",
    },
    "DB Lower 1": {"pl": "DB dolne 1", "cs": "DB dolní 1", "de": "KH Unterkörper 1"},
    "DB Lower 2": {"pl": "DB dolne 2", "cs": "DB dolní 2", "de": "KH Unterkörper 2"},
    "DB Upper 1": {"pl": "DB górne 1", "cs": "DB horní 1", "de": "KH Oberkörper 1"},
    "DB Upper 2": {"pl": "DB górne 2", "cs": "DB horní 2", "de": "KH Oberkörper 2"},
    "Dumbbell hypertrophy (4×/week)": {
        "pl": "Hipertrofia z hantlami (4×/tydzień)",
        "cs": "Hypertrofie s jednoručkami (4×/týden)",
        "de": "Kurzhantel-Hypertrophie (4×/Woche)",
    },
    "Older-adult functional strength": {
        "pl": "Siła funkcjonalna seniorów",
        "cs": "Funkční síla pro seniory",
        "de": "Funktionelle Kraft für Ältere",
    },
    "Function A": {"pl": "Funkcja A", "cs": "Funkce A", "de": "Funktion A"},
    "Function B": {"pl": "Funkcja B", "cs": "Funkce B", "de": "Funktion B"},
    "Pilates core foundations": {
        "pl": "Podstawy core w pilates",
        "cs": "Základy core v pilates",
        "de": "Pilates-Core-Grundlagen",
    },
    "Core A": {"pl": "Core A", "cs": "Core A", "de": "Core A"},
    "Core B": {"pl": "Core B", "cs": "Core B", "de": "Core B"},
    "Legs": {"pl": "Nogi", "cs": "Nohy", "de": "Beine"},
    "Push/pull/legs (6×/week)": {
        "pl": "Push/pull/legs (6×/tydzień)",
        "cs": "Push/pull/legs (6×/týden)",
        "de": "Push/Pull/Legs (6×/Woche)",
    },
    "Pull": {"pl": "Pull", "cs": "Pull", "de": "Pull"},
    "Push": {"pl": "Push", "cs": "Push", "de": "Push"},
    "Strength focus novice linear (3×/week)": {
        "pl": "Siła — liniowa progresja nowicjusza (3×/tydzień)",
        "cs": "Síla — lineární progrese začátečníka (3×/týden)",
        "de": "Kraftfokus lineare Anfängerprogression (3×/Woche)",
    },
    "Strength A": {"pl": "Siła A", "cs": "Síla A", "de": "Kraft A"},
    "Strength B": {"pl": "Siła B", "cs": "Síla B", "de": "Kraft B"},
    "Strength C": {"pl": "Siła C", "cs": "Síla C", "de": "Kraft C"},
    "Swim technique block (3×/week)": {
        "pl": "Blok techniki pływackiej (3×/tydzień)",
        "cs": "Blok plavecké techniky (3×/týden)",
        "de": "Schwimmtechnik-Block (3×/Woche)",
    },
    "Catch focus": {
        "pl": "Fokus: chwyt wody",
        "cs": "Fokus: zachycení vody",
        "de": "Fokus: Wasserfassen",
    },
    "Pull focus": {
        "pl": "Fokus: ciąg",
        "cs": "Fokus: tah",
        "de": "Fokus: Zugphase",
    },
    "Mixed strokes": {
        "pl": "Mieszane style",
        "cs": "Smíšené styly",
        "de": "Gemischte Schwimmstile",
    },
    "Lower 1": {"pl": "Dolne 1", "cs": "Dolní 1", "de": "Unterkörper 1"},
    "Lower 2": {"pl": "Dolne 2", "cs": "Dolní 2", "de": "Unterkörper 2"},
    "Upper/lower intermediate (4×/week)": {
        "pl": "Góra/dół — średniozaawansowany (4×/tydzień)",
        "cs": "Horní/dolní — středně pokročilí (4×/týden)",
        "de": "Ober-/Unterkörper Fortgeschrittene (4×/Woche)",
    },
    "Upper 1": {"pl": "Górne 1", "cs": "Horní 1", "de": "Oberkörper 1"},
    "Upper 2": {"pl": "Górne 2", "cs": "Horní 2", "de": "Oberkörper 2"},
    "Yoga mobility flow": {
        "pl": "Joga — flow mobilności",
        "cs": "Jóga — mobility flow",
        "de": "Yoga-Mobilitätsflow",
    },
    "Flow A": {"pl": "Flow A", "cs": "Flow A", "de": "Flow A"},
    "Flow B": {"pl": "Flow B", "cs": "Flow B", "de": "Flow B"},
    # frequent short coaching labels
    "Regression": {"pl": "Regresja", "cs": "Regrese", "de": "Regression"},
    "Unilateral": {"pl": "Jednostronnie", "cs": "Unilaterálně", "de": "Unilateral"},
    "Using momentum": {
        "pl": "Używanie pędu",
        "cs": "Používání hybnosti",
        "de": "Schwung holen",
    },
    "Compound": {"pl": "Złożone", "cs": "Vícekloubové", "de": "Mehrgelenkig"},
    "Inhale down, exhale up.": {
        "pl": "Wdech w dół, wydech w górę.",
        "cs": "Nádech dolů, výdech nahoru.",
        "de": "Einatmen beim Absenken, ausatmen beim Hochdrücken.",
    },
    "Exhale on the pull.": {
        "pl": "Wydech przy ciągnięciu.",
        "cs": "Výdech při tahu.",
        "de": "Ausatmen beim Ziehen.",
    },
    "Exhale to press.": {
        "pl": "Wydech przy wyciskaniu.",
        "cs": "Výdech při tlaku.",
        "de": "Ausatmen beim Drücken.",
    },
    "Exhale to extend.": {
        "pl": "Wydech przy prostowaniu.",
        "cs": "Výdech při natažení.",
        "de": "Ausatmen beim Strecken.",
    },
    "Exhale to stand.": {
        "pl": "Wydech przy wstaniu.",
        "cs": "Výdech při vstávání.",
        "de": "Ausatmen beim Aufstehen.",
    },
    "Exhale to rise.": {
        "pl": "Wydech przy unoszeniu.",
        "cs": "Výdech při zvedání.",
        "de": "Ausatmen beim Hochkommen.",
    },
    "Exhale on the curl.": {
        "pl": "Wydech przy uginaniu.",
        "cs": "Výdech při zdvihu.",
        "de": "Ausatmen beim Curl.",
    },
    "Exhale on the squeeze.": {
        "pl": "Wydech przy ściśnięciu.",
        "cs": "Výdech při stahu.",
        "de": "Ausatmen beim Zusammenführen.",
    },
    "Exhale on the press.": {
        "pl": "Wydech przy wyciskaniu.",
        "cs": "Výdech při tlaku.",
        "de": "Ausatmen beim Drücken.",
    },
    "Steady breathing.": {
        "pl": "Spokojny oddech.",
        "cs": "Klidné dýchání.",
        "de": "Ruhige Atmung.",
    },
    "Acute lumbar pain": {
        "pl": "Ostry ból lędźwiowy",
        "cs": "Akutní bederní bolest",
        "de": "Akuter Lumbalschmerz",
    },
    "Shoulder pain": {
        "pl": "Ból barku",
        "cs": "Bolest ramene",
        "de": "Schulterschmerz",
    },
    "Wrist pain": {
        "pl": "Ból nadgarstka",
        "cs": "Bolest zápěstí",
        "de": "Handgelenkschmerz",
    },
    "Acute knee pain": {
        "pl": "Ostry ból kolana",
        "cs": "Akutní bolest kolene",
        "de": "Akuter Knieschmerz",
    },
    "Acute groin strain": {
        "pl": "Ostre naciągnięcie pachwiny",
        "cs": "Akutní natažení třísla",
        "de": "Akute Leistenverletzung",
    },
    "Add load": {
        "pl": "Dodaj obciążenie",
        "cs": "Přidej zátěž",
        "de": "Last erhöhen",
    },
    "Hips sagging": {
        "pl": "Opadanie bioder",
        "cs": "Propadající boky",
        "de": "Durchhängende Hüften",
    },
    "Remove pause": {
        "pl": "Usuń pauzę",
        "cs": "Odstraň pauzu",
        "de": "Pause entfernen",
    },
    "Static": {"pl": "Statycznie", "cs": "Staticky", "de": "Statisch"},
    "Bodyweight": {
        "pl": "Masa ciała",
        "cs": "Vlastní váha",
        "de": "Körpergewicht",
    },
    "Shrugging": {
        "pl": "Szrugsowanie barków",
        "cs": "Krčení ramen",
        "de": "Hochziehen der Schultern",
    },
    "Full stroke": {
        "pl": "Pełny styl",
        "cs": "Plný záběr",
        "de": "Vollzug",
    },
    "Floor": {"pl": "Podłoga", "cs": "Podlaha", "de": "Boden"},
    "Uneven lockout": {
        "pl": "Nierówny lockout",
        "cs": "Nerovný lockout",
        "de": "Ungleichmäßiger Lockout",
    },
    "Rounding lumbar spine": {
        "pl": "Zaokrąglanie odcinka lędźwiowego",
        "cs": "Kulatá bederní páteř",
        "de": "Runden der Lendenwirbelsäule",
    },
    "Hands under shoulders; knees under hips.": {
        "pl": "Dłonie pod barkami; kolana pod biodrami.",
        "cs": "Dlaně pod rameny; kolena pod kyčlemi.",
        "de": "Hände unter den Schultern; Knie unter den Hüften.",
    },
    "Partial ROM": {
        "pl": "Częściowy ROM",
        "cs": "Částečný ROM",
        "de": "Teil-ROM",
    },
    "Knee pain kneeling": {
        "pl": "Ból kolana w klęku",
        "cs": "Bolest kolene v kleku",
        "de": "Knieschmerz im Knien",
    },
    "Machine": {"pl": "Maszyna", "cs": "Stroj", "de": "Maschine"},
    "Loaded": {"pl": "Z obciążeniem", "cs": "Se zátěží", "de": "Belastet"},
    "Barbell load": {
        "pl": "Obciążenie sztangą",
        "cs": "Zátěž velkou činkou",
        "de": "Langhantelbelastung",
    },
    "Swinging torso": {
        "pl": "Bujanie tułowiem",
        "cs": "Houpání trupem",
        "de": "Schwingender Oberkörper",
    },
    "Heels lifting": {
        "pl": "Odrywanie pięt",
        "cs": "Zvedání pat",
        "de": "Abheben der Fersen",
    },
    "Collapsing chest": {
        "pl": "Zapadanie klatki",
        "cs": "Propadající hrudník",
        "de": "Einfallender Brustkorb",
    },
    "Use table if safe": {
        "pl": "Użyj stołu, jeśli bezpiecznie",
        "cs": "Použij stůl, pokud je to bezpečné",
        "de": "Tisch nutzen, wenn sicher",
    },
    "Use backpack if no DB": {
        "pl": "Użyj plecaka, jeśli brak hantli",
        "cs": "Použij batoh, pokud nemáš jednoručky",
        "de": "Rucksack nutzen, wenn keine Kurzhanteln",
    },
    "Band OK": {
        "pl": "Guma OK",
        "cs": "Guma OK",
        "de": "Band OK",
    },
    # Template rationales
    "Mostly easy running with one quality stimulus reflects polarised descriptive distributions; weekly mileage increases softly with an honest caveat that fixed 10% rules are weakly evidenced.": {
        "pl": "Przewaga łatwego biegania z jednym bodźcem jakościowym odzwierciedla opisowe rozkłady spolaryzowane; tygodniowy kilometraż rośnie łagodnie — z zastrzeżeniem, że sztywne reguły „+10%” mają słabe dowody.",
        "cs": "Převaha snadného běhu s jedním kvalitativním podnětem odráží polarizované popisné distribuce; týdenní kilometráž roste mírně — s upřímnou výhradou, že pevná pravidla „+10 %“ mají slabou evidenci.",
        "de": "Überwiegend lockeres Laufen mit einem Qualitätsreiz spiegelt polarisierte deskriptive Verteilungen wider; die Wochenkilometer steigen behutsam — mit dem ehrlichen Hinweis, dass feste 10-%-Regeln schwach belegt sind.",
    },
    "Short full-body circuits maintain stimulus while travelling; pair with walking for energy expenditure per ACSM activity guidance.": {
        "pl": "Krótkie obwody na całe ciało utrzymują bodziec w podróży; łącz z marszem dla wydatku energetycznego zgodnie z wytycznymi ACSM dotyczącymi aktywności.",
        "cs": "Krátké celotělové okruhy udržují podnět na cestách; kombinuj s chůzí pro výdej energie dle ACSM doporučení k aktivitě.",
        "de": "Kurze Ganzkörper-Zirkel halten den Reiz auf Reisen aufrecht; kombiniere sie mit Gehen für den Energieumsatz gemäß ACSM-Aktivitätsleitlinien.",
    },
    "Two lifting days plus two easy runs balance musculoskeletal and cardiorespiratory fitness per ACSM multi-component guidance; hard intervals limited.": {
        "pl": "Dwa dni siłowe i dwa łatwe biegi równoważą sprawność mięśniowo-szkieletową i krążeniowo-oddechową zgodnie z wieloskładnikowymi wytycznymi ACSM; twarde interwały ograniczone.",
        "cs": "Dva silové dny a dva snadné běhy vyvažují pohybovou a kardiorespirační zdatnost dle vícesložkových doporučení ACSM; tvrdé intervaly jsou omezené.",
        "de": "Zwei Krafttage plus zwei lockere Läufe balancieren muskuloskelettale und kardiorespiratorische Fitness laut ACSM-Mehrkomponenten-Leitlinie; harte Intervalle bleiben begrenzt.",
    },
    "Combines posterior-chain and upper-back strength with thoracic/hip mobility to offset sedentary postures, within ACSM multi-component recommendations.": {
        "pl": "Łączy siłę tylnego łańcucha i górnych pleców z mobilnością piersiową/biodrową, by równoważyć postawy siedzące — w ramach wieloskładnikowych zaleceń ACSM.",
        "cs": "Kombinuje sílu zadního řetězce a horních zad s hrudní/kyčelní mobilitou jako protiváhu sedavým posturám — v rámci vícesložkových doporučení ACSM.",
        "de": "Kombiniert Kraft der hinteren Kette und des oberen Rückens mit thorakaler/hüftiger Mobilität als Ausgleich zu Sitzhaltungen — innerhalb der ACSM-Mehrkomponenten-Empfehlungen.",
    },
    "Three full-body sessions spread weekly hard-set volume so major muscles are trained ~2–3×/week, matching frequency evidence when volume is accumulated and ACSM guidance for multi-joint work.": {
        "pl": "Trzy sesje na całe ciało rozkładają tygodniową objętość ciężkich serii tak, by duże grupy były trenowane ~2–3×/tydzień — zgodnie z dowodami o częstotliwości przy akumulowanej objętości i wytycznymi ACSM dla pracy wielostawowej.",
        "cs": "Tři celotělové jednotky rozkládají týdenní objem těžkých sérií tak, aby hlavní svaly byly trénovány ~2–3×/týden — v souladu s evidencí frekvence při kumulovaném objemu a ACSM doporučením pro vícekloubovou práci.",
        "de": "Drei Ganzkörper-Einheiten verteilen das wöchentliche Volumen harter Sätze so, dass große Muskelgruppen ~2–3×/Woche trainiert werden — passend zur Frequenzevidenz bei akkumuliertem Volumen und ACSM-Leitlinien für mehrgelenkige Arbeit.",
    },
    "Hinge and hip-extension emphasis raises glute/hamstring weekly sets within established hypertrophy volume bands; hip thrust included given EMG comparisons versus squats.": {
        "pl": "Akcent na zawias i wyprost biodra podnosi tygodniowe serie pośladków/tylnych ud w ustalonych zakresach objętości hipertrofii; hip thrust uwzględniony ze względu na porównania EMG względem przysiadów.",
        "cs": "Důraz na kyvadlo a extenzi kyčle zvyšuje týdenní série hýždí/hamstringů v zavedených hypertrofických pásmech objemu; hip thrust je zařazen díky EMG srovnáním vůči dřepům.",
        "de": "Fokus auf Hüftbeuge und Hüftextension erhöht die wöchentlichen Gesäß-/Beinbeuger-Sätze innerhalb etablierter Hypertrophie-Volumenbänder; Hip Thrust ist wegen EMG-Vergleichen zu Kniebeugen enthalten.",
    },
    "Band + bodyweight full-body work preserves multi-joint stimulus when a gym is unavailable, aligned with ACSM guidance that some resistance is better than none.": {
        "pl": "Praca na całe ciało z gumą i masą ciała zachowuje bodziec wielostawowy, gdy siłownia jest niedostępna — zgodnie z wytycznymi ACSM, że jakiś opór jest lepszy niż żaden.",
        "cs": "Celotělová práce s gumou a vlastní váhou zachovává vícekloubový podnět, když není posilovna — v souladu s ACSM, že nějaký odpor je lepší než žádný.",
        "de": "Ganzkörperarbeit mit Band und Körpergewicht erhält den mehrgelenkigen Reiz ohne Gym — im Einklang mit ACSM, dass etwas Widerstand besser ist als keiner.",
    },
    "Upper/lower with dumbbells targets ~10–16 weekly hard sets per region using double progression in 8–12 rep ranges.": {
        "pl": "Góra/dół z hantlami celuje w ~10–16 ciężkich serii tygodniowo na region przy podwójnej progresji w zakresie 8–12 powtórzeń.",
        "cs": "Horní/dolní s jednoručkami cílí na ~10–16 těžkých sérií týdně na oblast při dvojité progresi v rozsahu 8–12 opakování.",
        "de": "Ober-/Unterkörper mit Kurzhanteln zielt auf ~10–16 harte Wochensätze pro Region mit Doppelprogression in 8–12 Wiederholungen.",
    },
    "Sit-to-stand, carries, and supported patterns emphasise function and balance with conservative RIR, consistent with ACSM guidance for older adults' multi-component training.": {
        "pl": "Wstawanie z krzesła, noszenia i wzorce z wsparciem akcentują funkcję i równowagę przy konserwatywnym RIR — zgodnie z wytycznymi ACSM dla wieloskładnikowego treningu osób starszych.",
        "cs": "Vstávání ze židle, nošení a podporované vzorce zdůrazňují funkci a rovnováhu s konzervativním RIR — v souladu s ACSM pro vícesložkový trénink starších dospělých.",
        "de": "Aufstehen vom Stuhl, Carries und gestützte Muster betonen Funktion und Balance mit konservativem RIR — konsistent mit ACSM-Leitlinien für Mehrkomponenten-Training Älterer.",
    },
    "Controlled trunk endurance and motor control sessions complement ACSM neuromotor recommendations without high spinal load.": {
        "pl": "Kontrolowane sesje wytrzymałości tułowia i kontroli motorycznej uzupełniają neuromotoryczne zalecenia ACSM bez wysokiego obciążenia kręgosłupa.",
        "cs": "Kontrolované jednotky vytrvalosti trupu a motorické kontroly doplňují neuromotorická doporučení ACSM bez vysokého zatížení páteře.",
        "de": "Kontrollierte Rumpfausdauer- und Motorkontroll-Einheiten ergänzen ACSM-Neuromotorik-Empfehlungen ohne hohe Wirbelsäulenlast.",
    },
    "PPL twice weekly delivers high frequency and volume for hypertrophy seekers who recover well; volume should still respect diminishing returns from Pelland et al.": {
        "pl": "PPL dwa razy w tygodniu daje wysoką częstotliwość i objętość osobom celującym w hipertrofię z dobrą regeneracją; objętość i tak powinna respektować malejące korzyści według Pelland et al.",
        "cs": "PPL dvakrát týdně přináší vysokou frekvenci a objem zájemcům o hypertrofii s dobrou regenerací; objem má i tak respektovat klesající výnosy dle Pelland et al.",
        "de": "PPL zweimal wöchentlich liefert hohe Frequenz und Volumen für Hypertrophie-Suchende mit guter Erholung; das Volumen sollte trotzdem die abnehmenden Erträge nach Pelland et al. beachten.",
    },
    "Novice linear progression on squat/hinge/press/pull compounds with conservative accessories, consistent with ACSM RT overview emphasis on multi-joint loading.": {
        "pl": "Liniowa progresja nowicjusza na złożonych przysiad/zawias/wyciskanie/ciągnienie z konserwatywnymi akcesoriami — zgodnie z przeglądem ACSM RT podkreślającym obciążenie wielostawowe.",
        "cs": "Lineární progrese začátečníka na vícekloubových dřep/kyvadlo/tlak/tah s konzervativními doplňky — v souladu s ACSM RT důrazem na vícekloubové zatížení.",
        "de": "Lineare Anfängerprogression auf Kniebeuge-/Hüftbeuge-/Druck-/Zug-Mehrgelenkübungen mit konservativen Isolationen — konsistent mit dem ACSM-RT-Überblick zum mehrgelenkigen Belasten.",
    },
    "Prioritise drill density before yardage so aerobic volume does not entrench poor mechanics; easy aerobic dominance still applies when conditioning.": {
        "pl": "Priorytet gęstości drill’ów przed metrażem, by objętość tlenowa nie utrwalała złej mechaniki; przy kondycji nadal obowiązuje dominacja łatwej pracy tlenowej.",
        "cs": "Upřednostni hustotu drilů před metráží, aby aerobní objem neupevňoval špatnou mechaniku; při kondici stále platí dominance snadné aerobní práce.",
        "de": "Priorisiere Drill-Dichte vor Meterzahl, damit aerobes Volumen schlechte Technik nicht verfestigt; bei Kondition gilt weiterhin die Dominanz lockerer aerober Arbeit.",
    },
    "Four-day upper/lower keeps ~2× weekly frequency per muscle while allowing higher per-session volume within Schoenfeld/Pelland dose–response bands.": {
        "pl": "Czterodniowy góra/dół utrzymuje ~2× tygodniową częstotliwość na mięsień przy wyższej objętości na sesję w zakresach dawka–odpowiedź Schoenfeld/Pelland.",
        "cs": "Čtyřdenní horní/dolní drží ~2× týdenní frekvenci na sval při vyšším objemu na jednotku v pásmech dávka–odpověď Schoenfeld/Pelland.",
        "de": "Vier Tage Ober-/Unterkörper halten ~2× wöchentliche Frequenz pro Muskel bei höherem Volumen pro Einheit innerhalb der Schoenfeld/Pelland-Dosis-Wirkungs-Bänder.",
    },
    "Flexibility and neuromotor practice on ≥2–3 days/week aligns with ACSM quantity/quality guidance; poses stay beginner-accessible.": {
        "pl": "Praktyka gibkości i neuromotoryki ≥2–3 dni/tydzień jest zgodna z wytycznymi ACSM quantity/quality; pozycje pozostają dostępne dla początkujących.",
        "cs": "Procvičování flexibility a neuromotoriky ≥2–3 dny/týden odpovídá ACSM doporučením quantity/quality; pozice zůstávají přístupné začátečníkům.",
        "de": "Flexibilitäts- und neuromotorische Praxis an ≥2–3 Tagen/Woche entspricht ACSM-Quantity/Quality-Leitlinien; Positionen bleiben einsteigerfreundlich.",
    },
}

# Phrase-level post-edits applied after MT (longer first)
POST_EDITS: dict[str, list[tuple[re.Pattern[str], str]]] = {
    "pl": [
        (re.compile(r"\bkucnięcie\b", re.I), "przysiad"),
        (re.compile(r"\bkucanie\b", re.I), "przysiad"),
        (re.compile(r"\bsquat\b", re.I), "przysiad"),
        (re.compile(r"\bdeadlift\b", re.I), "martwy ciąg"),
        (re.compile(r"\bpull-?ups?\b", re.I), "podciąganie"),
        (re.compile(r"\bpush-?ups?\b", re.I), "pompki"),
        (re.compile(r"\blunges?\b", re.I), "wypady"),
        (re.compile(r"\bbench press\b", re.I), "wyciskanie na ławce"),
    ],
    "cs": [
        (re.compile(r"\bdřepání\b", re.I), "dřep"),
        (re.compile(r"\bsquat\b", re.I), "dřep"),
        (re.compile(r"\bdeadlift\b", re.I), "mrtvý tah"),
        (re.compile(r"\bpull-?ups?\b", re.I), "shyby"),
        (re.compile(r"\bpush-?ups?\b", re.I), "kliky"),
        (re.compile(r"\blunges?\b", re.I), "výpady"),
        (re.compile(r"\bbench press\b", re.I), "bench press"),
    ],
    "de": [
        (re.compile(r"\bSquat\b"), "Kniebeuge"),
        (re.compile(r"\bDeadlift\b"), "Kreuzheben"),
        (re.compile(r"\bPull-?ups?\b"), "Klimmzüge"),
        (re.compile(r"\bPush-?ups?\b"), "Liegestütze"),
        (re.compile(r"\bLunges?\b"), "Ausfallschritte"),
        (re.compile(r"\bBench[- ]?[Pp]ress\b"), "Bankdrücken"),
    ],
}


def curated() -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    out.update(NAMES)
    out.update(TEMPLATE_AND_SHORT)
    return out


def load_cache(locale: str) -> dict[str, str]:
    path = CACHE_DIR / f"{locale}.json"
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return {}


def save_cache(locale: str, data: dict[str, str]) -> None:
    path = CACHE_DIR / f"{locale}.json"
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


SEP = " ||| "


def mt_translate_one(text: str, locale: str, retries: int = 5) -> str:
    import translators as ts

    last_err: Exception | None = None
    for attempt in range(retries):
        try:
            return ts.translate_text(
                text,
                translator="bing",
                from_language="en",
                to_language=locale,
            )
        except Exception as exc:  # noqa: BLE001
            last_err = exc
            time.sleep(1.2 * (attempt + 1))
    raise RuntimeError(f"MT failed for {locale!r}: {text!r}") from last_err


def mt_translate_batch(texts: list[str], locale: str) -> list[str]:
    """Translate a small batch joined by SEP; fall back to one-by-one on mismatch."""
    if len(texts) == 1:
        return [mt_translate_one(texts[0], locale)]
    joined = SEP.join(texts)
    try:
        raw = mt_translate_one(joined, locale)
    except RuntimeError:
        return [mt_translate_one(t, locale) for t in texts]
    parts = [p.strip() for p in raw.split("|||")]
    if len(parts) != len(texts):
        # separator mangled — retry individually
        return [mt_translate_one(t, locale) for t in texts]
    return parts


def post_edit(text: str, locale: str) -> str:
    for pat, repl in POST_EDITS.get(locale, []):
        text = pat.sub(repl, text)
    return text


def build_value_map(locale: str, unique_values: list[str]) -> dict[str, str]:
    overrides = curated()
    cache = load_cache(locale)
    out: dict[str, str] = {}
    pending: list[str] = []

    for en in unique_values:
        if en in overrides and locale in overrides[en]:
            out[en] = overrides[en][locale]
        elif en in cache and cache[en].strip():
            out[en] = post_edit(cache[en], locale)
        else:
            pending.append(en)

    print(
        f"[{locale}] curated/cache hits={len(out)} pending_mt={len(pending)}",
        flush=True,
    )
    batch_size = 12
    done = 0
    for start in range(0, len(pending), batch_size):
        batch = pending[start : start + batch_size]
        # Prefer shorter-only batches; long coaching strings solo to avoid SEP issues
        groups: list[list[str]] = []
        cur: list[str] = []
        budget = 0
        for en in batch:
            if len(en) > 90 or budget + len(en) > 350:
                if cur:
                    groups.append(cur)
                groups.append([en])
                cur = []
                budget = 0
            else:
                cur.append(en)
                budget += len(en)
        if cur:
            groups.append(cur)

        for group in groups:
            try:
                trs = mt_translate_batch(group, locale)
            except Exception as exc:  # noqa: BLE001
                print(f"[{locale}] FAIL batch: {exc}", file=sys.stderr, flush=True)
                trs = [""] * len(group)
            for en, tr in zip(group, trs):
                tr = post_edit((tr or "").strip(), locale)
                if not tr:
                    try:
                        tr = post_edit(mt_translate_one(en, locale).strip(), locale)
                    except Exception:  # noqa: BLE001
                        tr = en
                out[en] = tr
                cache[en] = tr
                done += 1
        save_cache(locale, cache)
        print(f"[{locale}] mt {done}/{len(pending)}", flush=True)
        time.sleep(0.25)

    save_cache(locale, cache)
    return out


def write_locale(locale: str, en: dict[str, str], value_map: dict[str, str]) -> Path:
    out: dict[str, str] = {}
    missing: list[str] = []
    for key, src in en.items():
        # Prefer key-based name overrides when value matches curated name
        if key.endswith(".name") and src in curated():
            out[key] = curated()[src][locale]
        elif src in value_map and value_map[src].strip():
            out[key] = value_map[src]
        else:
            missing.append(key)
            out[key] = src  # non-empty fallback
    path = OUT_DIR / f"{locale}.json"
    path.write_text(
        json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"wrote {path} keys={len(out)} fallbacks={len(missing)}")
    return path


def main() -> int:
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    assert isinstance(en, dict)
    unique = sorted(set(en.values()), key=lambda s: (len(s), s.lower()))
    print(f"en-GB keys={len(en)} unique_values={len(unique)}")

    for locale in LOCALES:
        vmap = build_value_map(locale, unique)
        write_locale(locale, en, vmap)

    # verify
    for locale in LOCALES:
        data = json.loads((OUT_DIR / f"{locale}.json").read_text(encoding="utf-8"))
        assert set(data) == set(en), f"{locale} key mismatch"
        empty = [k for k, v in data.items() if not str(v).strip()]
        assert not empty, f"{locale} empty: {empty[:5]}"
        print(f"OK {locale}: {len(data)} keys")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
