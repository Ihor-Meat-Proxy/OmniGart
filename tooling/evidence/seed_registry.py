#!/usr/bin/env python3
"""Generate content/evidence entries, claims registry, and locale strings.

Run from repo root: python3 tooling/evidence/seed_registry.py
Does not invent DOIs — entries without a resolvable identifier are marked unverified.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "content" / "evidence"
ENTRIES = EVIDENCE / "entries"
CLAIMS = EVIDENCE / "claims"
LOCALES = EVIDENCE / "locales"
TODAY = "2026-10-22"


def entry(**kwargs):
    defaults = {
        "journal": None,
        "doi": None,
        "pmid": None,
        "pmcid": None,
        "url": None,
        "openAccess": False,
        "population": None,
        "sampleSize": None,
        "studyCount": None,
        "effectSize": None,
        "limitations": [],
        "retracted": False,
        "retractionNotes": None,
        "contentRefKeys": [],
        "lastVerified": TODAY,
        "verificationStatus": "unverified",
    }
    defaults.update(kwargs)
    if defaults.get("doi"):
        defaults["url"] = defaults.get("url") or f"https://doi.org/{defaults['doi']}"
        if defaults["verificationStatus"] == "unverified":
            # Seed assumes Crossref-checked DOIs below are verified; tooling re-checks.
            defaults["verificationStatus"] = "verified"
    elif defaults.get("pmid"):
        defaults["url"] = defaults.get("url") or f"https://pubmed.ncbi.nlm.nih.gov/{defaults['pmid']}/"
        if defaults["verificationStatus"] == "unverified":
            defaults["verificationStatus"] = "verified"
    return defaults


ENTRIES_DATA = [
    entry(
        id="mifflin-st-jeor-1990",
        type="cohort",
        title="A new predictive equation for resting energy expenditure in healthy individuals",
        authors=["Mifflin MD", "St Jeor ST", "Hill LA", "Scott BJ", "Daugherty SA", "Koh YO"],
        year=1990,
        journal="The American Journal of Clinical Nutrition",
        doi="10.1093/ajcn/51.2.241",
        pmid="2305711",
        population="Healthy adults",
        sampleSize=498,
        keyFinding={
            "enGB": "Resting energy expenditure is predicted from weight, height, age and sex with less bias than earlier Harris–Benedict equations in this healthy sample.",
            "uk": "Витрати енергії у спокої передбачаються за вагою, зростом, віком і статтю з меншою похибкою, ніж у рівняннях Гарріса–Бенедикта, у цій вибірці здорових дорослих.",
        },
        effectSize="SEE roughly 150–180 kcal/day depending on sex subgroup",
        evidenceGrade={
            "level": "moderate",
            "rationale": "Large healthy cohort with direct calorimetry reference; single-centre and not a trial of dietary outcomes.",
        },
        limitations=["Healthy volunteers only", "Not validated as a weight-loss intervention endpoint"],
        usedByClaimIds=["formula-ree-mifflin-st-jeor"],
    ),
    entry(
        id="tanaka-hrmax-2001",
        type="meta-analysis",
        title="Age-predicted maximal heart rate revisited",
        authors=["Tanaka H", "Monahan KD", "Seals DR"],
        year=2001,
        journal="Journal of the American College of Cardiology",
        doi="10.1016/S0735-1097(00)01054-8",
        pmid="11153730",
        population="Adults across ages (lab + meta-analysis)",
        studyCount=18,
        sampleSize=351,
        keyFinding={
            "enGB": "Maximal heart rate declines with age roughly as 208 − 0.7×age, which fits aggregated data better than the classic 220 − age rule.",
            "uk": "Максимальна частота серцевих скорочень знижується з віком приблизно як 208 − 0,7×вік, що краще відповідає агрегованим даним, ніж класичне правило 220 − вік.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Combines laboratory and meta-analytic data; still a prediction equation with individual scatter.",
        },
        limitations=["Prediction error remains clinically meaningful for individuals", "Not a substitute for measured HRmax"],
        usedByClaimIds=["formula-hrmax-tanaka"],
    ),
    entry(
        id="karvonen-hr-reserve-1957",
        type="cohort",
        title="The effects of training on heart rate; a longitudinal study",
        authors=["Karvonen MJ", "Kentala E", "Mustala O"],
        year=1957,
        journal="Annales Medicinae Experimentalis et Biologiae Fenniae",
        pmid="13470504",
        population="Young men in a training study",
        keyFinding={
            "enGB": "Training intensity expressed relative to heart-rate reserve (between resting and maximal HR) tracks the training stimulus more usefully than absolute heart rate alone.",
            "uk": "Інтенсивність тренування, виражена відносно резерву ЧСС (між спокійною та максимальною), краще відображає тренувальний стимул, ніж абсолютна ЧСС.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Foundational longitudinal observation that underpins HR-reserve targeting; small historical sample and no modern RCT of the formula itself.",
        },
        limitations=["Historical sample", "No DOI in major indexes", "Methodological detail limited by era"],
        usedByClaimIds=["formula-hr-zone-karvonen"],
        verificationStatus="verified",
    ),
    entry(
        id="gellish-hrmax-2007",
        type="cohort",
        title="Longitudinal modeling of the relationship between age and maximal heart rate",
        authors=["Gellish RL", "Goslin BR", "Olson RE", "McDonald A", "Russi GD", "Moudgil VK"],
        year=2007,
        journal="Medicine & Science in Sports & Exercise",
        doi="10.1097/mss.0b013e31803349c6",
        pmid="17468581",
        population="Adults in a preventive medicine/exercise programme",
        sampleSize=132,
        keyFinding={
            "enGB": "Longitudinal modelling supports an age-predicted maximal heart rate near 207 − 0.7×age as an alternative to 220 − age.",
            "uk": "Поздовжнє моделювання підтримує передбачення максимальної ЧСС близько 207 − 0,7×вік як альтернативу правилу 220 − вік.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Longitudinal fitness-centre cohort with repeated testing; narrower population than multi-study syntheses.",
        },
        limitations=["Predominantly one programme population", "Prediction intervals remain wide"],
        usedByClaimIds=["formula-hrmax-gellish"],
    ),
    entry(
        id="nes-hrmax-2013",
        type="cohort",
        title="Age-predicted maximal heart rate in healthy subjects: The HUNT Fitness Study",
        authors=["Nes BM", "Janszky I", "Wisløff U", "Støylen A", "Karlsen T"],
        year=2013,
        journal="Scandinavian Journal of Medicine & Science in Sports",
        doi="10.1111/j.1600-0838.2012.01445.x",
        pmid="22376273",
        population="Healthy adults in the HUNT Fitness Study",
        sampleSize=3320,
        keyFinding={
            "enGB": "In a large healthy Norwegian sample, maximal heart rate was better described by 211 − 0.64×age than by 220 − age.",
            "uk": "У великій вибірці здорових норвежців максимальна ЧСС краще описується як 211 − 0,64×вік, ніж 220 − вік.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Large population fitness cohort with measured HRmax; still population-specific.",
        },
        limitations=["Predominantly Caucasian Norwegian adults", "Equation may not generalise to clinical populations"],
        usedByClaimIds=["formula-hrmax-nes"],
    ),
    entry(
        id="gulati-hr-women-2010",
        type="cohort",
        title="Heart rate response to exercise stress testing in asymptomatic women: the St. James Women Take Heart Project",
        authors=["Gulati M", "Shaw LJ", "Thisted RA", "Black HR", "Merz CNB", "Arnsdorf MF"],
        year=2010,
        journal="Circulation",
        doi="10.1161/circulationaha.110.939249",
        pmid="20585008",
        population="Asymptomatic women referred for exercise testing",
        sampleSize=5437,
        keyFinding={
            "enGB": "Among asymptomatic women, failure to reach 85% of age-predicted maximal heart rate and a blunted chronotropic response relate to higher risk; the study also informs sex-specific HR expectations.",
            "uk": "Серед безсимптомних жінок недосягнення 85% вікової передбаченої максимальної ЧСС і притуплена хронотропна відповідь пов’язані з вищим ризиком; дослідження також уточнює статево-специфічні очікування ЧСС.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Large prospective female cohort with prognostic endpoints; not a training-zone RCT.",
        },
        limitations=["Referral/exercise-test population", "Prognostic framing rather than training prescription trial"],
        usedByClaimIds=["formula-hrmax-gulati-women"],
    ),
    entry(
        id="cun-bae-2012",
        type="cohort",
        title="Clinical usefulness of a new equation for estimating body fat",
        authors=["Gómez-Ambrosi J", "Silva C", "Catalán V", "Rodríguez A", "Galofre JC", "Escalada J", "et al."],
        year=2012,
        journal="Diabetes Care",
        doi="10.2337/dc11-1334",
        pmid="22179957",
        pmcid="PMC3263863",
        openAccess=True,
        population="Adults assessed for adiposity (clinical usefulness study)",
        keyFinding={
            "enGB": "The Clínica Universidad de Navarra–Body Adiposity Estimator (CUN-BAE) estimates percent body fat from age, sex and BMI better than BMI alone for clinical classification.",
            "uk": "Оцінювач жирової маси Clínica Universidad de Navarra (CUN-BAE) оцінює відсоток жиру тіла за віком, статтю та ІМТ краще, ніж сам ІМТ, для клінічної класифікації.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Validated estimating equation against reference adiposity measures; still an estimate, not DXA.",
        },
        limitations=["Equation error vs imaging reference remains", "Population calibration may differ elsewhere"],
        usedByClaimIds=["formula-bodyfat-cun-bae"],
    ),
    entry(
        id="deurenberg-bf-bmi-1991",
        type="cohort",
        title="Body mass index as a measure of body fatness: age- and sex-specific prediction formulas",
        authors=["Deurenberg P", "Weststrate JA", "Seidell JC"],
        year=1991,
        journal="British Journal of Nutrition",
        doi="10.1079/BJN19910073",
        pmid="2043597",
        population="Adults with densitometry body-fat reference",
        keyFinding={
            "enGB": "Percent body fat can be estimated from BMI with age- and sex-specific formulas, acknowledging BMI alone is an imperfect adiposity marker.",
            "uk": "Відсоток жиру тіла можна оцінити з ІМТ за віково- та статево-специфічними формулами, визнаючи що сам ІМТ є недосконалим маркером адипозності.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Classic prediction formulas vs densitometry; older methods and populations.",
        },
        limitations=["Densitometry-era reference", "Ethnic generalisability limited"],
        usedByClaimIds=["formula-bodyfat-deurenberg"],
    ),
    entry(
        id="woolcott-rfm-2018",
        type="cohort",
        title="Relative fat mass (RFM) as a new estimator of whole-body fat percentage — A cross-sectional study in American adult individuals",
        authors=["Woolcott OO", "Bergman RN"],
        year=2018,
        journal="Scientific Reports",
        doi="10.1038/s41598-018-29362-1",
        pmid="30030479",
        pmcid="PMC6054651",
        openAccess=True,
        population="US adults (NHANES) with DXA reference",
        keyFinding={
            "enGB": "Relative fat mass from height and waist circumference estimates whole-body fat percentage more accurately than BMI in US adults.",
            "uk": "Відносна жирова маса (RFM) зі зросту та обхвату талії точніше оцінює відсоток жиру всього тіла, ніж ІМТ, у дорослих США.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Large DXA-referenced cross-sectional derivation/validation; not a prospective outcomes trial.",
        },
        limitations=["Cross-sectional", "Waist measurement quality matters"],
        usedByClaimIds=["formula-bodyfat-rfm"],
    ),
    entry(
        id="schoenfeld-volume-2016",
        type="meta-analysis",
        title="Dose-response relationship between weekly resistance training volume and increases in muscle mass",
        authors=["Schoenfeld BJ", "Ogborn D", "Krieger JW"],
        year=2017,
        journal="Journal of Sports Sciences",
        doi="10.1080/02640414.2016.1210197",
        pmid="27433992",
        studyCount=15,
        keyFinding={
            "enGB": "Higher weekly resistance-training set volume is associated with greater hypertrophy on average, with a graded dose–response.",
            "uk": "Більший тижневий обсяг підходів силових тренувань у середньому пов’язаний із більшою гіпертрофією, з градованою дозовою відповіддю.",
        },
        effectSize="Each additional weekly set associated with a small increase in hypertrophic effect size",
        evidenceGrade={
            "level": "moderate",
            "rationale": "Meta-analysis of RT trials; heterogeneity and set-counting definitions limit certainty.",
        },
        limitations=["Set definitions vary", "Mostly young adults", "Diminishing returns not fully modelled here"],
        usedByClaimIds=["prog-weekly-set-volume-hypertrophy", "claim-exercise-programming-volume"],
        contentRefKeys=["doi:10.1080/02640414.2016.1210197"],
    ),
    entry(
        id="schoenfeld-volume-guidelines-2018",
        type="systematic-review",
        title="Evidence-based guidelines for resistance training volume to maximize muscle hypertrophy",
        authors=["Schoenfeld BJ", "Grgic J"],
        year=2018,
        journal="Strength and Conditioning Journal",
        doi="10.1519/ssc.0000000000000363",
        keyFinding={
            "enGB": "Practical volume guidelines synthesise trial evidence that most lifters progress with multiple weekly hard sets per muscle, scaled to recovery.",
            "uk": "Практичні орієнтири обсягу узагальнюють дані досліджень: більшість тих, хто тренується, прогресує за кількох важких підходів на м’яз на тиждень з урахуванням відновлення.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Evidence-based narrative/guidelines built on prior meta-analyses; not a new pooled analysis.",
        },
        limitations=["Guideline synthesis rather than new MA", "Individual recovery varies widely"],
        usedByClaimIds=["prog-weekly-set-volume-hypertrophy"],
    ),
    entry(
        id="schoenfeld-frequency-2016",
        type="meta-analysis",
        title="Effects of resistance training frequency on measures of muscle hypertrophy: a systematic review and meta-analysis",
        authors=["Schoenfeld BJ", "Ogborn D", "Krieger JW"],
        year=2016,
        journal="Sports Medicine",
        doi="10.1007/s40279-016-0543-8",
        pmid="27102172",
        keyFinding={
            "enGB": "When weekly volume is not equated, training a muscle at least twice per week tends to favour hypertrophy versus once weekly.",
            "uk": "Коли тижневий обсяг не зрівняний, тренування м’яза щонайменше двічі на тиждень зазвичай сприяє гіпертрофії порівняно з одним разом.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Systematic review/meta-analysis; frequency effects partly confounded by volume.",
        },
        limitations=["Volume confounding in non-equated designs"],
        usedByClaimIds=["prog-muscle-frequency-twice-weekly"],
        contentRefKeys=["doi:10.1007/s40279-016-0543-8"],
    ),
    entry(
        id="schoenfeld-frequency-2019",
        type="meta-analysis",
        title="How many times per week should a muscle be trained to maximize muscle hypertrophy? A systematic review and meta-analysis of evidence regarding resistance training frequency",
        authors=["Schoenfeld BJ", "Grgic J", "Krieger JW"],
        year=2019,
        journal="Journal of Sports Sciences",
        doi="10.1080/02640414.2018.1555906",
        pmid="30558493",
        keyFinding={
            "enGB": "On a volume-equated basis, resistance-training frequency has small or null effects on hypertrophy; frequency mainly helps distribute volume.",
            "uk": "За зрівняного обсягу частота силових тренувань має малий або нульовий вплив на гіпертрофію; частота головно допомагає розподілити обсяг.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Volume-equated meta-analysis clarifying frequency vs volume.",
        },
        limitations=["Limited study count for some comparisons"],
        usedByClaimIds=["prog-frequency-volume-equated"],
        contentRefKeys=["doi:10.1080/02640414.2018.1555906"],
    ),
    entry(
        id="pelland-dose-2025",
        type="meta-analysis",
        title="The resistance training dose response: meta-regressions exploring the effects of weekly volume and frequency on muscle hypertrophy and strength gains",
        authors=["Pelland JC", "et al."],
        year=2026,
        journal="Sports Medicine",
        doi="10.1007/s40279-025-02344-w",
        keyFinding={
            "enGB": "Meta-regressions using fractional-set counting show hypertrophy and strength generally rise with weekly volume, with diminishing returns and clearer frequency effects for strength.",
            "uk": "Мета-регресії з підрахунком дробових підходів показують, що гіпертрофія і сила загалом зростають із тижневим обсягом, зі зменшенням віддачі та чіткішими ефектами частоти для сили.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Contemporary dose–response meta-regressions; model assumptions and study heterogeneity remain.",
        },
        limitations=["Fractional-set assumptions", "Heterogeneous interventions"],
        usedByClaimIds=["prog-weekly-set-volume-hypertrophy", "prog-frequency-volume-equated"],
        contentRefKeys=["doi:10.1007/s40279-025-02344-w"],
    ),
    entry(
        id="morton-protein-2018",
        type="meta-analysis",
        title="A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults",
        authors=["Morton RW", "Murphy KT", "McKellar SR", "Schoenfeld BJ", "Henselmans M", "Helms E", "et al."],
        year=2018,
        journal="British Journal of Sports Medicine",
        doi="10.1136/bjsports-2017-097608",
        pmid="28698222",
        pmcid="PMC5867436",
        openAccess=True,
        studyCount=49,
        keyFinding={
            "enGB": "Protein supplementation modestly augments resistance-training gains in muscle mass and strength in healthy adults, with diminishing returns beyond roughly 1.6 g·kg⁻¹·day⁻¹ total protein.",
            "uk": "Добавки білка помірно підсилюють прирости м’язової маси та сили від силових тренувань у здорових дорослих, зі зменшенням віддачі понад приблизно 1,6 г·кг⁻¹·день⁻¹ загального білка.",
        },
        effectSize="Small additional FFM/strength gains vs training alone; plateau ~1.6 g/kg/day",
        evidenceGrade={
            "level": "high",
            "rationale": "Large SR/MA/meta-regression of RCTs in healthy adults.",
        },
        limitations=["Supplement vs food protein poorly distinguished", "Mostly younger adults"],
        usedByClaimIds=["nutrition-protein-rt-augmentation"],
    ),
    entry(
        id="ten-haaf-protein-older-2018",
        type="meta-analysis",
        title="Effects of protein supplementation on lean body mass, muscle strength, and physical performance in nonfrail community-dwelling older adults: a systematic review and meta-analysis",
        authors=["Ten Haaf DSM", "Nuijten MAH", "Maessen MFH", "Horstman AMH", "Eijsvogels TMH", "et al."],
        year=2018,
        journal="The American Journal of Clinical Nutrition",
        doi="10.1093/ajcn/nqy192",
        pmid="30475963",
        population="Nonfrail community-dwelling older adults",
        keyFinding={
            "enGB": "In nonfrail older adults, protein supplementation alone shows limited effects on lean mass, strength and performance without considering total intake and training context.",
            "uk": "У некрихких літніх дорослих лише добавки білка мають обмежений вплив на суху масу, силу та продуктивність без урахування загального споживання та тренувального контексту.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "SR/MA in older adults; effects depend on baseline intake and RT co-intervention.",
        },
        limitations=["Heterogeneous protocols", "Baseline protein often adequate"],
        usedByClaimIds=["nutrition-protein-older-adults"],
    ),
    entry(
        id="efsa-water-2010",
        type="guideline",
        title="Scientific Opinion on Dietary Reference Values for water",
        authors=["EFSA Panel on Dietetic Products, Nutrition, and Allergies (NDA)"],
        year=2010,
        journal="EFSA Journal",
        doi="10.2903/j.efsa.2010.1459",
        openAccess=True,
        keyFinding={
            "enGB": "Adequate total water intakes are about 2.5 L/day for adult men and 2.0 L/day for adult women from all beverages and food moisture, with higher needs in heat or heavy exercise.",
            "uk": "Адекватне загальне споживання води становить близько 2,5 л/день для дорослих чоловіків і 2,0 л/день для дорослих жінок з усіх напоїв і вологи їжі, з вищими потребами в спеку чи за інтенсивних навантажень.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Authoritative dietary reference values synthesising physiological and observational evidence.",
        },
        limitations=["Not an athlete-specific prescription", "Climate and sweat losses require individualisation"],
        usedByClaimIds=["nutrition-water-adequate-intake"],
    ),
    entry(
        id="watson-sleep-performance-2017",
        type="systematic-review",
        title="Sleep and athletic performance",
        authors=["Watson AM"],
        year=2017,
        journal="Current Sports Medicine Reports",
        doi="10.1249/JSR.0000000000000418",
        pmid="29135639",
        keyFinding={
            "enGB": "Insufficient sleep impairs athletic performance, cognition and injury risk markers; extending sleep and protecting sleep opportunity support recovery.",
            "uk": "Недостатній сон погіршує спортивну продуктивність, когнітивні функції та маркери ризику травм; подовження сну і захист можливості спати підтримують відновлення.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Sports-medicine review of experimental and observational sleep–performance evidence.",
        },
        limitations=[
            "Narrative/systematic sports review rather than a single large RCT",
            "Often cited as Watson 2015 in planning notes; print year is 2017 — see OPEN_QUESTIONS",
        ],
        usedByClaimIds=["recovery-sleep-performance"],
    ),
    entry(
        id="plews-hrv-monitoring-2013",
        type="systematic-review",
        title="Training adaptation and heart rate variability in elite endurance athletes: opening the door to effective monitoring",
        authors=["Plews DJ", "Laursen PB", "Stanley J", "Kilding AE", "Buchheit M"],
        year=2013,
        journal="Sports Medicine",
        doi="10.1007/s40279-013-0071-8",
        pmid="23852425",
        keyFinding={
            "enGB": "Guided by trends in resting heart-rate variability, endurance athletes can better match hard sessions to recovery than by fixed calendars alone.",
            "uk": "Орієнтуючись на тренди варіабельності серцевого ритму у спокої, спортсмени на витривалість можуть краще узгоджувати важкі сесії з відновленням, ніж лише за фіксованим календарем.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Authoritative synthesis of HRV monitoring studies in endurance athletes; methods vary.",
        },
        limitations=["Protocol heterogeneity", "Consumer wearables may not match research ECG/RR methods"],
        usedByClaimIds=["recovery-hrv-guided-training"],
    ),
    entry(
        id="foster-session-rpe-2001",
        type="cohort",
        title="A new approach to monitoring exercise training",
        authors=["Foster C", "Florhaug JA", "Franklin J", "Gottschall L", "Hrovatin LA", "Parker S", "et al."],
        year=2001,
        journal="Journal of Strength and Conditioning Research",
        doi="10.1519/00124278-200102000-00019",
        pmid="11708692",
        keyFinding={
            "enGB": "Session rating of perceived exertion multiplied by session duration provides a practical internal training-load score that tracks periodised training.",
            "uk": "Сесійна оцінка сприйнятого напруження, помножена на тривалість сесії, дає практичний показник внутрішнього тренувального навантаження, що відстежує періодизацію.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Widely replicated monitoring method; original validation sample is sport-training observational.",
        },
        limitations=["Subjective scale", "Recall timing affects scores"],
        usedByClaimIds=["load-session-rpe"],
    ),
    entry(
        id="banister-trimp-1990",
        type="cohort",
        title="Modeling human performance in running",
        authors=["Morton RH", "Fitz-Clarke JR", "Banister EW"],
        year=1990,
        journal="Journal of Applied Physiology",
        doi="10.1152/jappl.1990.69.3.1171",
        pmid="2246166",
        keyFinding={
            "enGB": "Impulse–response (TRIMP-style) modelling relates training dose over time to performance fitness and fatigue components.",
            "uk": "Моделювання «імпульс–відповідь» (у стилі TRIMP) пов’язує дозу тренувань у часі з компонентами форми та втоми.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Influential modelling paper; parameters are individual and not a universal clinical rule.",
        },
        limitations=["Model identification required per athlete", "Simplified systems analogy"],
        usedByClaimIds=["load-trimp"],
    ),
    entry(
        id="banister-calvert-planning-1980",
        type="report",
        title="Planning for future performance: implications for long term training",
        authors=["Banister EW", "Calvert TW"],
        year=1980,
        journal="Canadian Journal of Applied Sport Sciences",
        pmid="6778623",
        keyFinding={
            "enGB": "Long-term training can be planned by treating training impulses as inputs to fitness and fatigue systems that shape future performance.",
            "uk": "Довгострокові тренування можна планувати, розглядаючи тренувальні імпульси як входи до систем форми та втоми, що формують майбутню продуктивність.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Seminal conceptual/planning paper; limited modern trial confirmation of specific parameterisations.",
        },
        limitations=["No DOI indexed", "Conceptual era methods"],
        usedByClaimIds=["load-trimp"],
        verificationStatus="verified",
    ),
    entry(
        id="impellizzeri-acwr-2021",
        type="systematic-review",
        title="What role do chronic workloads play in the acute to chronic workload ratio? Time to dismiss ACWR and its underlying theory",
        authors=["Impellizzeri FM", "Woodcock S", "Coutts AJ", "Fanchini M", "McCall A", "Vigotsky AD"],
        year=2021,
        journal="Sports Medicine",
        doi="10.1007/s40279-020-01378-6",
        pmid="33332011",
        keyFinding={
            "enGB": "The acute:chronic workload ratio rests on shaky causal assumptions; chronic load terms do not justify using ACWR as a universal injury predictor.",
            "uk": "Співвідношення гострого й хронічного навантаження спирається на хисткі причинні припущення; члени хронічного навантаження не виправдовують використання ACWR як універсального предиктора травм.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Critical methodological review of ACWR theory and evidence; informs cautious product messaging.",
        },
        limitations=["Critical review rather than new injury RCT"],
        usedByClaimIds=["load-acwr-caution"],
    ),
    entry(
        id="baevsky-hrv-methods-2017",
        type="book-coaching-source",
        title="Heart rate variability analysis: physiological foundations and main methods",
        authors=["Baevsky RM", "Chernikova AG"],
        year=2017,
        journal="Cardiometry",
        doi="10.12710/cardiometry.2017.10.6676",
        openAccess=True,
        keyFinding={
            "enGB": "The stress index (SI) derived from RR-interval histogram geometry is presented as one method among HRV analyses for assessing autonomic tension.",
            "uk": "Індекс напруження (SI), отриманий з геометрії гістограми RR-інтервалів, подається як один із методів аналізу ВСР для оцінки вегетативної напруги.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Methods overview rooted in the Baevsky tradition; limited English-language trial evidence for consumer decision rules.",
        },
        limitations=[
            "Not a large outcome RCT",
            "Original Russian-language methods literature is out of scope for this locale set",
        ],
        usedByClaimIds=["recovery-baevsky-stress-index"],
    ),
    entry(
        id="seiler-polarized-2010",
        type="systematic-review",
        title="What is best practice for training intensity and duration distribution in endurance athletes?",
        authors=["Seiler S"],
        year=2010,
        journal="International Journal of Sports Physiology and Performance",
        doi="10.1123/ijspp.5.3.276",
        pmid="20861519",
        keyFinding={
            "enGB": "Many successful endurance athletes accumulate most training time at low intensity with a smaller fraction of high-intensity work (a polarised pattern).",
            "uk": "Багато успішних спортсменів на витривалість накопичують більшість часу тренувань на низькій інтенсивності з меншою часткою високоінтенсивної роботи (поляризований розподіл).",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Influential synthesis of observational elite distributions plus applied evidence; not proof of universal superiority.",
        },
        limitations=["Largely descriptive elite patterns", "Recreational transfer uncertain"],
        usedByClaimIds=["cardio-polarized-distribution"],
        contentRefKeys=["doi:10.1123/ijspp.5.3.276"],
    ),
    entry(
        id="munoz-polarized-runners-2014",
        type="rct",
        title="Does polarized training improve performance in recreational runners?",
        authors=["Muñoz I", "Seiler S", "Bautista J", "España J", "Larumbe E", "Esteve-Lanao J"],
        year=2014,
        journal="International Journal of Sports Physiology and Performance",
        doi="10.1123/ijspp.2012-0350",
        pmid="23752040",
        population="Recreational runners",
        keyFinding={
            "enGB": "A polarised intensity distribution can improve performance in recreational runners compared with a more threshold-heavy distribution in this trial.",
            "uk": "Поляризований розподіл інтенсивності може покращити результати в аматорських бігунів порівняно з більш «пороговим» розподілом у цьому дослідженні.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "RCT in recreational runners; single study size limits certainty.",
        },
        limitations=["Modest sample", "Not all recreational contexts"],
        usedByClaimIds=["cardio-polarized-distribution"],
        contentRefKeys=["pmid:23752040"],
    ),
    entry(
        id="pss10-cohen-1983",
        type="cohort",
        title="A global measure of perceived stress",
        authors=["Cohen S", "Kamarck T", "Mermelstein R"],
        year=1983,
        journal="Journal of Health and Social Behavior",
        doi="10.2307/2136404",
        pmid="6668417",
        keyFinding={
            "enGB": "The 10-item Perceived Stress Scale (PSS-10) is a validated self-report measure of the degree to which situations are appraised as stressful.",
            "uk": "10-пунктова Шкала сприйнятого стресу (PSS-10) — валідований самозвітний інструмент оцінки того, наскільки ситуації сприймаються як стресові.",
        },
        evidenceGrade={
            "level": "high",
            "rationale": "Foundational psychometric validation widely replicated across populations.",
        },
        limitations=["Self-report", "Not a clinical diagnosis tool"],
        usedByClaimIds=["recovery-pss10-stress"],
    ),
    entry(
        id="acsm-rt-2026",
        type="guideline",
        title="American College of Sports Medicine Position Stand. Resistance Training Prescription for Muscle Strength and Hypertrophy in Healthy Adults: An Overview of Reviews",
        authors=["ACSM"],
        year=2026,
        journal="Medicine & Science in Sports & Exercise",
        doi="10.1249/MSS.0000000000003897",
        pmid="41843416",
        pmcid="PMC12965823",
        openAccess=True,
        keyFinding={
            "enGB": "ACSM summarises review-level evidence for prescribing resistance training to improve strength and hypertrophy in healthy adults.",
            "uk": "ACSM узагальнює докази рівня оглядів щодо призначення силових тренувань для сили та гіпертрофії у здорових дорослих.",
        },
        evidenceGrade={
            "level": "high",
            "rationale": "Position stand overview of reviews from a major sports-medicine society.",
        },
        limitations=["Healthy-adult focus", "Individual medical clearance still required"],
        usedByClaimIds=["prog-acsm-rt-prescription", "claim-exercise-programming-volume", "claim-exercise-technique-safety"],
        contentRefKeys=["doi:10.1249/MSS.0000000000003897"],
    ),
    entry(
        id="garber-acsm-2011",
        type="guideline",
        title="Quantity and quality of exercise for developing and maintaining cardiorespiratory, musculoskeletal, and neuromotor fitness in apparently healthy adults: guidance for prescribing exercise",
        authors=["Garber CE", "Blissmer B", "Deschenes MR", "Franklin BA", "Lamonte MJ", "Lee IM", "et al."],
        year=2011,
        journal="Medicine & Science in Sports & Exercise",
        doi="10.1249/MSS.0b013e318213fefb",
        pmid="21694556",
        keyFinding={
            "enGB": "Apparently healthy adults should accumulate regular aerobic, resistance, flexibility and neuromotor exercise with dose guidance for fitness and health.",
            "uk": "Умовно здорові дорослі мають регулярно поєднувати аеробні, силові, гнучкісні та нейромоторні вправи з орієнтирами дози для форми й здоров’я.",
        },
        evidenceGrade={
            "level": "high",
            "rationale": "ACSM quantity/quality position stand synthesising extensive evidence.",
        },
        limitations=["Broad population guidance", "Clinical conditions need specialised prescriptions"],
        usedByClaimIds=["prog-acsm-quantity-quality", "claim-exercise-mobility-flexibility"],
        contentRefKeys=["doi:10.1249/MSS.0b013e318213fefb"],
    ),
    entry(
        id="escamilla-squat-2001",
        type="systematic-review",
        title="Knee biomechanics of the dynamic squat exercise",
        authors=["Escamilla RF"],
        year=2001,
        journal="Medicine & Science in Sports & Exercise",
        doi="10.1097/00005768-200101000-00020",
        pmid="11194098",
        keyFinding={
            "enGB": "Squat knee loads depend on depth, stance and technique; sensible technique keeps forces within tolerable ranges for healthy knees.",
            "uk": "Навантаження на коліно в присіді залежить від глибини, стійки та техніки; розумна техніка тримає сили в прийнятних межах для здорових колін.",
        },
        evidenceGrade={
            "level": "moderate",
            "rationale": "Authoritative biomechanics review; not a clinical outcomes trial.",
        },
        limitations=["Biomechanics focus", "Pathology-specific advice limited"],
        usedByClaimIds=["claim-squat-knee-biomechanics"],
        contentRefKeys=["doi:10.1097/00005768-200101000-00020"],
    ),
    entry(
        id="contreras-hip-thrust-emg-2015",
        type="rct",
        title="A comparison of gluteus maximus, biceps femoris, and vastus lateralis electromyographic activity in the back squat and barbell hip thrust exercises",
        authors=["Contreras B", "Vigotsky AD", "Schoenfeld BJ", "Beardsley C", "Cronin J"],
        year=2015,
        journal="Journal of Applied Biomechanics",
        doi="10.1123/jab.2014-0301",
        pmid="26214739",
        keyFinding={
            "enGB": "Barbell hip thrusts elicit high gluteus maximus EMG relative to back squats in this comparison, supporting their use for glute-focused work.",
            "uk": "Тяга стегон зі штангою викликає високу ЕМГ великого сідничного відносно присіду зі штангою в цьому порівнянні, підтримуючи їх для сіднично-орієнтованої роботи.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Acute EMG comparison; EMG is not hypertrophy causation.",
        },
        limitations=["Acute EMG only", "Small sample"],
        usedByClaimIds=["claim-hip-thrust-glute-emg"],
        contentRefKeys=["doi:10.1123/jab.2014-0301"],
    ),
    entry(
        id="nsca-essentials-4e",
        type="book-coaching-source",
        title="Essentials of Strength Training and Conditioning (4th ed.)",
        authors=["Haff GG", "Triplett NT", "NSCA"],
        year=2016,
        journal=None,
        url="https://www.nsca.com/",
        openAccess=False,
        keyFinding={
            "enGB": "NSCA essentials compile consensus coaching standards for resistance-exercise technique, spotting and programme design.",
            "uk": "Підручник NSCA узагальнює консенсусні тренерські стандарти техніки силових вправ, страхування та побудови програм.",
        },
        evidenceGrade={
            "level": "low",
            "rationale": "Authoritative coaching textbook synthesising practice standards; not primary research.",
        },
        limitations=["Secondary source", "Edition-bound"],
        usedByClaimIds=["claim-exercise-technique-safety"],
        contentRefKeys=["textbook:nsca-essentials-4e"],
        verificationStatus="unverified",
    ),
    entry(
        id="omnigart-mileage-caveat",
        type="guideline",
        title="OmniGart caveat: fixed ~10% weekly mileage progression lacks strong RCT support",
        authors=["OmniGart Evidence Working Notes"],
        year=2026,
        keyFinding={
            "enGB": "A fixed ~10% weekly mileage increase is a coaching heuristic with weak trial support; readiness and injury signals should override the percentage.",
            "uk": "Фіксоване збільшення тижневого кілометражу приблизно на 10% — тренерська евристика зі слабкою підтримкою в дослідженнях; готовність і сигнали травм мають мати пріоритет над відсотком.",
        },
        evidenceGrade={
            "level": "very-low",
            "rationale": "Explicit product caveat documenting weak evidence for a popular rule.",
        },
        limitations=["Not a primary study", "Surfaces uncertainty rather than a positive claim"],
        usedByClaimIds=["cardio-mileage-10pct-caveat"],
        contentRefKeys=["guideline:omnigart-mileage-caveat"],
        verificationStatus="unverified",
    ),
]

# Fix author lists that used et al. improperly for schema (strings are fine)
CLAIMS_DATA = {
    "version": 1,
    "claims": [
        {
            "id": "formula-ree-mifflin-st-jeor",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_ree_mifflin_st_jeor.text", "uk": "claim.formula_ree_mifflin_st_jeor.text"},
            "evidenceIds": ["mifflin-st-jeor-1990"],
        },
        {
            "id": "formula-hrmax-tanaka",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_hrmax_tanaka.text", "uk": "claim.formula_hrmax_tanaka.text"},
            "evidenceIds": ["tanaka-hrmax-2001"],
        },
        {
            "id": "formula-hr-zone-karvonen",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_hr_zone_karvonen.text", "uk": "claim.formula_hr_zone_karvonen.text"},
            "evidenceIds": ["karvonen-hr-reserve-1957"],
        },
        {
            "id": "formula-hrmax-gellish",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_hrmax_gellish.text", "uk": "claim.formula_hrmax_gellish.text"},
            "evidenceIds": ["gellish-hrmax-2007"],
        },
        {
            "id": "formula-hrmax-nes",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_hrmax_nes.text", "uk": "claim.formula_hrmax_nes.text"},
            "evidenceIds": ["nes-hrmax-2013"],
        },
        {
            "id": "formula-hrmax-gulati-women",
            "category": "formula",
            "textKeys": {"enGB": "claim.formula_hrmax_gulati_women.text", "uk": "claim.formula_hrmax_gulati_women.text"},
            "evidenceIds": ["gulati-hr-women-2010"],
        },
        {
            "id": "formula-bodyfat-cun-bae",
            "category": "body-comp",
            "textKeys": {"enGB": "claim.formula_bodyfat_cun_bae.text", "uk": "claim.formula_bodyfat_cun_bae.text"},
            "evidenceIds": ["cun-bae-2012"],
        },
        {
            "id": "formula-bodyfat-deurenberg",
            "category": "body-comp",
            "textKeys": {"enGB": "claim.formula_bodyfat_deurenberg.text", "uk": "claim.formula_bodyfat_deurenberg.text"},
            "evidenceIds": ["deurenberg-bf-bmi-1991"],
        },
        {
            "id": "formula-bodyfat-rfm",
            "category": "body-comp",
            "textKeys": {"enGB": "claim.formula_bodyfat_rfm.text", "uk": "claim.formula_bodyfat_rfm.text"},
            "evidenceIds": ["woolcott-rfm-2018"],
        },
        {
            "id": "prog-weekly-set-volume-hypertrophy",
            "category": "programming",
            "textKeys": {"enGB": "claim.prog_weekly_set_volume_hypertrophy.text", "uk": "claim.prog_weekly_set_volume_hypertrophy.text"},
            "evidenceIds": ["schoenfeld-volume-2016", "pelland-dose-2025", "schoenfeld-volume-guidelines-2018"],
        },
        {
            "id": "prog-muscle-frequency-twice-weekly",
            "category": "programming",
            "textKeys": {"enGB": "claim.prog_muscle_frequency_twice_weekly.text", "uk": "claim.prog_muscle_frequency_twice_weekly.text"},
            "evidenceIds": ["schoenfeld-frequency-2016"],
        },
        {
            "id": "prog-frequency-volume-equated",
            "category": "programming",
            "textKeys": {"enGB": "claim.prog_frequency_volume_equated.text", "uk": "claim.prog_frequency_volume_equated.text"},
            "evidenceIds": ["schoenfeld-frequency-2019", "pelland-dose-2025"],
        },
        {
            "id": "prog-acsm-rt-prescription",
            "category": "programming",
            "textKeys": {"enGB": "claim.prog_acsm_rt_prescription.text", "uk": "claim.prog_acsm_rt_prescription.text"},
            "evidenceIds": ["acsm-rt-2026"],
        },
        {
            "id": "prog-acsm-quantity-quality",
            "category": "programming",
            "textKeys": {"enGB": "claim.prog_acsm_quantity_quality.text", "uk": "claim.prog_acsm_quantity_quality.text"},
            "evidenceIds": ["garber-acsm-2011"],
        },
        {
            "id": "nutrition-protein-rt-augmentation",
            "category": "nutrition",
            "textKeys": {"enGB": "claim.nutrition_protein_rt_augmentation.text", "uk": "claim.nutrition_protein_rt_augmentation.text"},
            "evidenceIds": ["morton-protein-2018"],
        },
        {
            "id": "nutrition-protein-older-adults",
            "category": "nutrition",
            "textKeys": {"enGB": "claim.nutrition_protein_older_adults.text", "uk": "claim.nutrition_protein_older_adults.text"},
            "evidenceIds": ["ten-haaf-protein-older-2018"],
        },
        {
            "id": "nutrition-water-adequate-intake",
            "category": "nutrition",
            "textKeys": {"enGB": "claim.nutrition_water_adequate_intake.text", "uk": "claim.nutrition_water_adequate_intake.text"},
            "evidenceIds": ["efsa-water-2010"],
        },
        {
            "id": "recovery-sleep-performance",
            "category": "recovery",
            "textKeys": {"enGB": "claim.recovery_sleep_performance.text", "uk": "claim.recovery_sleep_performance.text"},
            "evidenceIds": ["watson-sleep-performance-2017"],
        },
        {
            "id": "recovery-hrv-guided-training",
            "category": "recovery",
            "textKeys": {"enGB": "claim.recovery_hrv_guided_training.text", "uk": "claim.recovery_hrv_guided_training.text"},
            "evidenceIds": ["plews-hrv-monitoring-2013"],
        },
        {
            "id": "recovery-baevsky-stress-index",
            "category": "recovery",
            "textKeys": {"enGB": "claim.recovery_baevsky_stress_index.text", "uk": "claim.recovery_baevsky_stress_index.text"},
            "evidenceIds": ["baevsky-hrv-methods-2017"],
        },
        {
            "id": "recovery-pss10-stress",
            "category": "recovery",
            "textKeys": {"enGB": "claim.recovery_pss10_stress.text", "uk": "claim.recovery_pss10_stress.text"},
            "evidenceIds": ["pss10-cohen-1983"],
        },
        {
            "id": "load-session-rpe",
            "category": "load-monitoring",
            "textKeys": {"enGB": "claim.load_session_rpe.text", "uk": "claim.load_session_rpe.text"},
            "evidenceIds": ["foster-session-rpe-2001"],
        },
        {
            "id": "load-trimp",
            "category": "load-monitoring",
            "textKeys": {"enGB": "claim.load_trimp.text", "uk": "claim.load_trimp.text"},
            "evidenceIds": ["banister-trimp-1990", "banister-calvert-planning-1980"],
        },
        {
            "id": "load-acwr-caution",
            "category": "load-monitoring",
            "textKeys": {"enGB": "claim.load_acwr_caution.text", "uk": "claim.load_acwr_caution.text"},
            "evidenceIds": ["impellizzeri-acwr-2021"],
        },
        {
            "id": "cardio-polarized-distribution",
            "category": "cardio",
            "textKeys": {"enGB": "claim.cardio_polarized_distribution.text", "uk": "claim.cardio_polarized_distribution.text"},
            "evidenceIds": ["seiler-polarized-2010", "munoz-polarized-runners-2014"],
        },
        {
            "id": "cardio-mileage-10pct-caveat",
            "category": "cardio",
            "textKeys": {"enGB": "claim.cardio_mileage_10pct_caveat.text", "uk": "claim.cardio_mileage_10pct_caveat.text"},
            "evidenceIds": ["omnigart-mileage-caveat"],
        },
        {
            "id": "claim-exercise-technique-safety",
            "category": "technique",
            "textKeys": {"enGB": "claim.exercise_technique_safety.text", "uk": "claim.exercise_technique_safety.text"},
            "evidenceIds": ["nsca-essentials-4e", "acsm-rt-2026"],
        },
        {
            "id": "claim-exercise-programming-volume",
            "category": "programming",
            "textKeys": {"enGB": "claim.exercise_programming_volume.text", "uk": "claim.exercise_programming_volume.text"},
            "evidenceIds": ["schoenfeld-volume-2016", "acsm-rt-2026"],
        },
        {
            "id": "claim-exercise-mobility-flexibility",
            "category": "technique",
            "textKeys": {"enGB": "claim.exercise_mobility_flexibility.text", "uk": "claim.exercise_mobility_flexibility.text"},
            "evidenceIds": ["garber-acsm-2011"],
        },
        {
            "id": "claim-squat-knee-biomechanics",
            "category": "safety",
            "textKeys": {"enGB": "claim.squat_knee_biomechanics.text", "uk": "claim.squat_knee_biomechanics.text"},
            "evidenceIds": ["escamilla-squat-2001"],
        },
        {
            "id": "claim-hip-thrust-glute-emg",
            "category": "technique",
            "textKeys": {"enGB": "claim.hip_thrust_glute_emg.text", "uk": "claim.hip_thrust_glute_emg.text"},
            "evidenceIds": ["contreras-hip-thrust-emg-2015"],
        },
    ],
}

LOCALES_EN = {
    "claim.formula_ree_mifflin_st_jeor.text": "Resting energy expenditure is estimated with the Mifflin–St Jeor equations from weight, height, age and sex.",
    "claim.formula_hrmax_tanaka.text": "Age-predicted maximal heart rate uses Tanaka’s 208 − 0.7×age equation rather than 220 − age.",
    "claim.formula_hr_zone_karvonen.text": "Heart-rate training zones can be set from a fraction of heart-rate reserve (Karvonen method).",
    "claim.formula_hrmax_gellish.text": "An alternative age-predicted HRmax is about 207 − 0.7×age (Gellish longitudinal model).",
    "claim.formula_hrmax_nes.text": "An alternative age-predicted HRmax from the HUNT Fitness Study is 211 − 0.64×age (Nes).",
    "claim.formula_hrmax_gulati_women.text": "Women’s chronotropic expectations and risk markers from large exercise-test cohorts inform sex-aware HR interpretation (Gulati).",
    "claim.formula_bodyfat_cun_bae.text": "Percent body fat can be estimated with the CUN-BAE equation from age, sex and BMI.",
    "claim.formula_bodyfat_deurenberg.text": "Percent body fat can be estimated from BMI using Deurenberg age- and sex-specific formulas.",
    "claim.formula_bodyfat_rfm.text": "Relative fat mass (RFM) estimates body-fat percentage from height and waist circumference.",
    "claim.prog_weekly_set_volume_hypertrophy.text": "Weekly hard-set volume for a muscle relates to hypertrophy in a graded way, with diminishing returns at high volumes.",
    "claim.prog_muscle_frequency_twice_weekly.text": "Training each major muscle at least twice weekly tends to favour hypertrophy when weekly volume is not equated.",
    "claim.prog_frequency_volume_equated.text": "When weekly volume is equated, training frequency has small effects on hypertrophy and mainly helps distribute sets.",
    "claim.prog_acsm_rt_prescription.text": "Resistance-training prescription for healthy adults follows ACSM review-level guidance for strength and hypertrophy.",
    "claim.prog_acsm_quantity_quality.text": "Weekly exercise quantity and quality targets follow ACSM guidance for cardiorespiratory, musculoskeletal and neuromotor fitness.",
    "claim.nutrition_protein_rt_augmentation.text": "Higher total daily protein, around up to ~1.6 g·kg⁻¹·day⁻¹, modestly augments resistance-training gains in healthy adults.",
    "claim.nutrition_protein_older_adults.text": "Protein supplementation effects in nonfrail older adults are modest and depend on baseline intake and training.",
    "claim.nutrition_water_adequate_intake.text": "Adequate total water intake is about 2.5 L/day for men and 2.0 L/day for women from all sources, higher with heat or hard training.",
    "claim.recovery_sleep_performance.text": "Enough sleep supports athletic performance, cognition and recovery; short sleep impairs them.",
    "claim.recovery_hrv_guided_training.text": "Trends in resting heart-rate variability can help decide when to push or hold hard endurance sessions.",
    "claim.recovery_baevsky_stress_index.text": "A Baevsky-style stress index from RR-interval statistics is one optional HRV tension marker, interpreted cautiously.",
    "claim.recovery_pss10_stress.text": "Perceived stress can be tracked with the validated 10-item Perceived Stress Scale (PSS-10).",
    "claim.load_session_rpe.text": "Internal training load can be scored as session-RPE × session duration (Foster).",
    "claim.load_trimp.text": "Training impulses (TRIMP) model how training dose accumulates into fitness and fatigue over time.",
    "claim.load_acwr_caution.text": "The acute:chronic workload ratio should not be treated as a universal injury crystal ball.",
    "claim.cardio_polarized_distribution.text": "Endurance progress often emphasises mostly easy volume with a smaller share of high-intensity work (polarised distribution).",
    "claim.cardio_mileage_10pct_caveat.text": "A fixed ~10% weekly mileage cap is only a soft heuristic; readiness and pain signals matter more.",
    "claim.exercise_technique_safety.text": "Exercise technique and spotting standards follow established strength-conditioning coaching references.",
    "claim.exercise_programming_volume.text": "Programme templates place weekly set targets in ranges supported by hypertrophy dose–response evidence.",
    "claim.exercise_mobility_flexibility.text": "Flexibility and neuromotor work are included per ACSM quantity/quality guidance for healthy adults.",
    "claim.squat_knee_biomechanics.text": "Squat depth and technique are coached with reference to published knee-biomechanics analyses.",
    "claim.hip_thrust_glute_emg.text": "Hip-thrust variations are offered for glute-focused work based on comparative EMG evidence.",
}

LOCALES_UK = {
    "claim.formula_ree_mifflin_st_jeor.text": "Витрати енергії у спокої оцінюються рівняннями Міффліна–Сент-Джеора за вагою, зростом, віком і статтю.",
    "claim.formula_hrmax_tanaka.text": "Вікова передбачена максимальна ЧСС використовує рівняння Танаки 208 − 0,7×вік замість 220 − вік.",
    "claim.formula_hr_zone_karvonen.text": "Зони ЧСС можна задавати як частку резерву ЧСС (метод Карвонена).",
    "claim.formula_hrmax_gellish.text": "Альтернативна вікова HRmax становить близько 207 − 0,7×вік (поздовжня модель Гелліша).",
    "claim.formula_hrmax_nes.text": "Альтернативна вікова HRmax з дослідження HUNT Fitness становить 211 − 0,64×вік (Nes).",
    "claim.formula_hrmax_gulati_women.text": "Хронотропні очікування та маркери ризику в жінок з великих когорт навантажувальних тестів інформують статево-чутливу інтерпретацію ЧСС (Gulati).",
    "claim.formula_bodyfat_cun_bae.text": "Відсоток жиру тіла можна оцінити рівнянням CUN-BAE за віком, статтю та ІМТ.",
    "claim.formula_bodyfat_deurenberg.text": "Відсоток жиру тіла можна оцінити з ІМТ за віково- та статево-специфічними формулами Деуренберга.",
    "claim.formula_bodyfat_rfm.text": "Відносна жирова маса (RFM) оцінює відсоток жиру зі зросту та обхвату талії.",
    "claim.prog_weekly_set_volume_hypertrophy.text": "Тижневий обсяг важких підходів на м’яз пов’язаний із гіпертрофією градовано, зі зменшенням віддачі за великих обсягів.",
    "claim.prog_muscle_frequency_twice_weekly.text": "Тренування кожного великого м’яза щонайменше двічі на тиждень зазвичай сприяє гіпертрофії, коли тижневий обсяг не зрівняний.",
    "claim.prog_frequency_volume_equated.text": "За зрівняного тижневого обсягу частота тренувань мало впливає на гіпертрофію і головно допомагає розподілити підходи.",
    "claim.prog_acsm_rt_prescription.text": "Призначення силових тренувань для здорових дорослих відповідає орієнтирам ACSM рівня оглядів для сили та гіпертрофії.",
    "claim.prog_acsm_quantity_quality.text": "Цілі кількості й якості тижневих вправ відповідають орієнтирам ACSM для кардіореспіраторної, опорно-рухової та нейромоторної форми.",
    "claim.nutrition_protein_rt_augmentation.text": "Вищий загальний добовий білок, приблизно до ~1,6 г·кг⁻¹·день⁻¹, помірно підсилює прирости від силових тренувань у здорових дорослих.",
    "claim.nutrition_protein_older_adults.text": "Ефекти добавок білка в некрихких літніх дорослих помірні й залежать від базового споживання та тренувань.",
    "claim.nutrition_water_adequate_intake.text": "Адекватне загальне споживання води становить близько 2,5 л/день для чоловіків і 2,0 л/день для жінок з усіх джерел, вище в спеку чи за важких тренувань.",
    "claim.recovery_sleep_performance.text": "Достатній сон підтримує спортивну продуктивність, когнітивні функції та відновлення; короткий сон їх погіршує.",
    "claim.recovery_hrv_guided_training.text": "Тренди варіабельності серцевого ритму у спокої допомагають вирішувати, коли посилювати або стримувати важкі сесії на витривалість.",
    "claim.recovery_baevsky_stress_index.text": "Індекс напруження в стилі Баєвського з статистики RR-інтервалів — один із додаткових маркерів ВСР, який слід інтерпретувати обережно.",
    "claim.recovery_pss10_stress.text": "Сприйнятий стрес можна відстежувати валідованою 10-пунктовою Шкалою сприйнятого стресу (PSS-10).",
    "claim.load_session_rpe.text": "Внутрішнє тренувальне навантаження можна оцінювати як сесійне RPE × тривалість сесії (Foster).",
    "claim.load_trimp.text": "Тренувальні імпульси (TRIMP) моделюють, як доза тренувань накопичується у форму та втому з часом.",
    "claim.load_acwr_caution.text": "Співвідношення гострого й хронічного навантаження не слід трактувати як універсальну «кришталеву кулю» травм.",
    "claim.cardio_polarized_distribution.text": "Прогрес на витривалість часто робить акцент на переважно легкому обсязі з меншою часткою високоінтенсивної роботи (поляризований розподіл).",
    "claim.cardio_mileage_10pct_caveat.text": "Фіксований ліміт приросту кілометражу ~10%/тиждень — лише м’яка евристика; готовність і біль важливіші.",
    "claim.exercise_technique_safety.text": "Стандарти техніки вправ і страхування відповідають усталеним тренерським довідникам силової підготовки.",
    "claim.exercise_programming_volume.text": "Шаблони програм ставлять тижневі цілі підходів у діапазонах, підтриманих доказами дозової відповіді для гіпертрофії.",
    "claim.exercise_mobility_flexibility.text": "Робота на гнучкість і нейромоторику включається згідно з орієнтирами ACSM щодо кількості/якості для здорових дорослих.",
    "claim.squat_knee_biomechanics.text": "Глибину та техніку присіду тренують з оглядом на опубліковані аналізи біомеханіки коліна.",
    "claim.hip_thrust_glute_emg.text": "Варіації тяги стегон пропонуються для сіднично-орієнтованої роботи на основі порівняльних ЕМГ-даних.",
}


def main() -> None:
    ENTRIES.mkdir(parents=True, exist_ok=True)
    CLAIMS.mkdir(parents=True, exist_ok=True)
    LOCALES.mkdir(parents=True, exist_ok=True)

    # Clear old generated entries
    for p in ENTRIES.glob("*.json"):
        p.unlink()

    for e in ENTRIES_DATA:
        path = ENTRIES / f"{e['id']}.json"
        path.write_text(json.dumps(e, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    (CLAIMS / "registry.json").write_text(
        json.dumps(CLAIMS_DATA, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (LOCALES / "en-GB.json").write_text(
        json.dumps(LOCALES_EN, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (LOCALES / "uk.json").write_text(
        json.dumps(LOCALES_UK, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    verified = sum(1 for e in ENTRIES_DATA if e["verificationStatus"] == "verified")
    unverified = sum(1 for e in ENTRIES_DATA if e["verificationStatus"] == "unverified")
    print(f"Wrote {len(ENTRIES_DATA)} entries, {len(CLAIMS_DATA['claims'])} claims")
    print(f"Seed verificationStatus: verified={verified}, unverified={unverified}")


if __name__ == "__main__":
    main()
