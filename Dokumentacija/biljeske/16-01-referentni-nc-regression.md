# 16.1 — Referentni `.nc` i RichAuto regression test

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Pohraniti dostavljeni referentni program za pravokutnik 100 × 200 mm i
usporediti ga s determinističkim outputom postojećeg RichAuto A11 generatora
bez slijepog byte-for-byte kopiranja.

## Promijenjene datoteke

- `Dokumentacija/reference/referentni-pravokutnik-100x200.nc` — referentni
  `.nc` artefakt.
- `src/test/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGeneratorTest.java` —
  regression test s eksplicitnim reference profilom i vrijednostima.
- `Dokumentacija/biljeske/00_indeks.md` i `00_odluke.md` — dokumentiranje
  reference i razlika outputa.

## Stvarna implementacija

Referentni program je spremljen bez izmjene sadržaja. Regression test generira
istu geometriju i koristi `F500`, `F150`, dubinu `1.0` i safe Z `5.0`.

Output generatora odgovara referenci u redoslijedu plungea, rezanja,
retracta, `M03`, `M05` i `M30`, uz sljedeće razlike:

| Razlika | Klasifikacija |
|---|---|
| Generator emitira `G21` i `G17` | Namjerno i konfigurabilno kroz postojeći profil/ugovor |
| `G90` i `G54` emitiraju se kao zasebni retci umjesto `G90 G54` | Namjerno formatiranje; funkcionalno značenje nije fizički potvrđeno |
| `S` nije emitiran | Namjerno jer reference nema `S`, a `emitSpindleSpeed` je isključen |
| `F150` i `F500` emitiraju se na odgovarajućim feed pokretima | Namjerno i konfigurabilno kroz `emitFeedRate`; odgovara referenci |

Nema promjene generatora samo radi kopiranja reference.

## Razlog odabranog rješenja

Reference služi kao regression fixture i stručni pregled, ne kao dokaz
kompatibilnosti. Profilne opcije ostaju odvojene kako bi se nakon provjere
konkretnog A11 moglo uključiti ili isključiti `F`, `S`, `G54` i spindle naredbe
bez promjene geometrijskog generatora.

## Arhitektonska povezanost

Fixture pripada dokumentacijskom/testnom sloju. Test poziva postojeći G-code
generator s geometrijskim `ToolPath` objektom; ne uvodi SQL, UI, layout ili
tool compensation logiku.

## Važne odluke i ograničenja

- Referentni program je stručna referenca, nije output naše aplikacije i nije
  fizički testiran na stroju.
- `G21` i `G17` ostaju profilni redci, a `G90`/`G54` zasebni deterministički
  retci postojećeg generatora.
- `S` se ne dodaje jer ga referentni program ne sadrži.
- Cutter compensation nije dio ovog podkoraka.
- RichAuto A11 ponašanje i kompatibilnost nisu potvrđeni.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit: hr.lukabosnjak.gcode.RichAutoA11GCodeGeneratorTest
```

### Stvarni rezultat

IDE build i testni razred završili su uspješno.

### Što nije testirano

Referentni ili generirani `.nc` nije pokrenut na ZK-1325 / RichAuto A11.
Nije testirano fizičko ponašanje G21, G17, G90, G54, F, M03, M05, M30,
Z-smjera ili work zeroa.

## Otvorena pitanja

- Treba potvrditi čita li konkretni A11 `F`, `G54` i spindle naredbe.
- Treba odlučiti koji će profil biti dopušten za prvi fizički test.
- Cutter compensation ostaje odluka Koraka 16.2.

## Moguće poglavlje završnog rada

Generiranje i regresijsko testiranje RichAuto A11 G-koda.

## Kandidati za isječke koda

### Kandidat: Deterministički reference-profile output

**Datoteka:** `src/test/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGeneratorTest.java`  
**Klasa/metoda:** `RichAutoA11GCodeGeneratorTest#generatesReferenceProfileShapeWithReferenceFeedAndDepthValues`  
**Zašto je važan:** Pokazuje usporedbu stvarne referentne strukture s outputom
  aplikacijskog generatora bez uklanjanja profilnih razlika.  
**Moguće poglavlje:** G-code generator i testiranje

```java
MachiningParameters parameters = new MachiningParameters(
        null, 18000.0, 500.0, 150.0, 1.0, 1.0, 5.0);
ToolPath rectangle = new ToolPath(List.of(
        new LineSegment(new Point2(0.0, 0.0), new Point2(100.0, 0.0)),
        new LineSegment(new Point2(100.0, 0.0), new Point2(100.0, 200.0)),
        new LineSegment(new Point2(100.0, 200.0), new Point2(0.0, 200.0)),
        new LineSegment(new Point2(0.0, 200.0), new Point2(0.0, 0.0))));

assertEquals("""
        G21
        G17
        G90
        G54
        M03
        G00 Z5.000
        G00 X0.000 Y0.000
        G01 Z-1.000 F150.000
        G01 X100.000 Y0.000 F500.000
        G01 X100.000 Y200.000
        G01 X0.000 Y200.000
        G01 X0.000 Y0.000
        G00 Z5.000
        M05
        M30
        """, generator.generate(rectangle, parameters).text());
```

**Ideja opisa u radu:** Regression test zaključava redoslijed i značenje
  referentnih feed/Z vrijednosti na softverskoj razini.

## Kandidat za sliku, dijagram ili tablicu

Tablica razlika reference i generatora u ovoj bilješci.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
