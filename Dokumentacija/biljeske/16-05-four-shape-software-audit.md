# 16.5 — Softverski audit četiri V1 oblika

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Provjeriti da nakon geometrijske kompenzacije i reference RichAuto profila
kvadrat, pravokutnik, krug i jednakostranični trokut prolaze cjeloviti
single-element softverski tok.

## Promijenjene datoteke

- `src/test/java/hr/lukabosnjak/service/FourShapeCompensationIntegrationTest.java`
  — integracijski test sva četiri oblika.
- `Dokumentacija/biljeske/00_indeks.md` — indeks bilješke.

## Stvarna implementacija

Test za svaki oblik provjerava validaciju, postojanje i zatvorenost izvornog
`ToolPath` objekta, aplikacijsku kompenzaciju s tehničkim promjerom Ø6 mm,
fit kompenzirane putanje na ploču i machine XY granice te generiranje
determinističkog G-code outputa.

Reference-profile output provjerava `F150` za plunge, `F500` za rezanje,
multi-pass Z logiku (`5 mm` dubine u prolazima `2 + 2 + 1 mm`), safe-Z
retracte i `M30`. Isti request generira se dvaput i tekstualni output mora
biti jednak.

Ø6 mm je u ovom testu tehnički softverski slučaj. Ne predstavlja potvrđeni
stvarni alat na ZK-1325.

## Razlog odabranog rješenja

Jedan audit test pokriva zajednički workflow bez dupliciranja četiri gotovo
jednaka testa, a shape mapa jasno pokazuje da su svi podržani oblici
uključeni. Provjera se provodi nad kompenziranom putanjom prije generatora,
što potvrđuje da promjer alata utječe na granice i G-code koordinate.

## Arhitektonska povezanost

Test povezuje postojeće slojeve u service workflowu:
`ShapeValidator` → `ToolPathService` → `ToolPathCompensationService` →
`SingleShapeFitValidator` → `RichAutoA11GCodeGenerator`. Ne uvodi SQL, UI,
layout algoritam ni fizičku kontrolu stroja.

## Važne odluke i ograničenja

- Sva četiri oblika prolaze samo softverski test; to nije fizička potvrda.
- Kompenzacija ostaje aplikacijska, bez G41/G42/G40 i bez `D` naredbi.
- Test koristi eksplicitni `CutSide.INSIDE`.
- Krug se provjerava kroz postojeće ArcSegment/G02/G03 mapiranje.
- Ø6 i Ø8 nisu potvrđeni aktivni alati; ovaj audit koristi Ø6, dok je Ø8
  pokriven zasebnim compensation unit testom.

## Build i testiranje

### Izvršene naredbe

```text
IDE JUnit: hr.lukabosnjak.service.FourShapeCompensationIntegrationTest
```

### Stvarni rezultat

Test je uspješno završen. Obuhvaćena su sva četiri oblika:
square, rectangle, circle i equilateral triangle.

### Što nije testirano

Nije izvršen puni Maven suite jer Maven nije dostupan u okruženju. Nije
proveden fizički test na ZK-1325 / RichAuto A11. Nisu fizički potvrđeni
promjer alata, Z-smjer, work zero ni controller postavke.

## Otvorena pitanja

- Provesti fizički single-element test nakon potvrde stvarnog alata i A11
  postavki.
- Prije Iteracije 2 definirati placement/margin pravilo za vanjske rezove i
  razmak koji uključuje alatni omotač.

## Moguće poglavlje završnog rada

Softversko testiranje geometrije, kompenzacije alata i RichAuto G-code toka.

## Kandidati za isječke koda

### Kandidat: Cjelovita provjera četiri podržana oblika

**Datoteka:** `src/test/java/hr/lukabosnjak/service/FourShapeCompensationIntegrationTest.java`  
**Klasa/metoda:** `FourShapeCompensationIntegrationTest#validatesCompensatesFitsAndGeneratesDeterministicProgramsForAllV1Shapes`  
**Zašto je važan:** Pokazuje da isti validacijski, kompenzacijski i generatorski
  tok radi za sve V1 oblike.  
**Moguće poglavlje:** Testiranje implementacije

```java
for (Map.Entry<ShapeType, Shape> entry : shapes.entrySet()) {
    Shape shape = entry.getValue();
    ToolPath programmedContour = toolPathService.generate(shape);
    ToolPath cutterCenterPath = compensationService.compensate(
            programmedContour, tool, CutSide.INSIDE);
    fitValidator.validate(cutterCenterPath, sheet, machine);

    ProgramGenerationRequest request = new ProgramGenerationRequest(
            machine, tool, sheet, parameters, shape, CutSide.INSIDE);
    GCodeProgram first = generationService.generate(request);
    GCodeProgram second = generationService.generate(request);
    assertEquals(first.text(), second.text());
}
```

**Ideja opisa u radu:** Parametrizirani oblikovni podaci omogućuju provjeru
zajedničkog single-element toka i determinističnosti outputa bez vezivanja
testa uz fizički kontroler.

## Kandidat za sliku, dijagram ili tablicu

Tablica rezultata audita za četiri V1 oblika.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
