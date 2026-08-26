# 11.1 — Composition root i softverski end-to-end tok

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Uvesti jedno jasno mjesto za ručno povezivanje postojećih ovisnosti te softverski provjeriti cijeli single-element tok od ulaza do ponovnog učitavanja spremljenog joba.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` — jedini ručni composition root produkcijskog objektnog grafa.
- `src/main/java/hr/lukabosnjak/app/Main.java` — JavaFX lifecycle i delegiranje inicijalizacije/controller factoryja composition rootu.
- `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java` — javni overload za inicijalizaciju već otvorene testne veze.
- `src/test/java/hr/lukabosnjak/app/ApplicationCompositionRootIntegrationTest.java` — softverski end-to-end round-trip.
- `Dokumentacija/biljeske/00_indeks.md`, `00_odluke.md` i ova bilješka — razvojna evidencija i arhitektonska odluka.

## Stvarna implementacija

`ApplicationCompositionRoot` jednom stvara repository ugovore s JDBC implementacijama, zajedničke validatore, `ToolPathService`, bounds/fit validator, postojeći konzervativni RichAuto preview profil i generator, application servicee te `MainFormController`. Produkcijski factory koristi `DatabaseConfig::getConnection`, dok package-private testni factory prima izolirani `ConnectionProvider`. `Main` više ne poznaje pojedinačne konstruktore slojeva.

Integration test inicijalizira svježu in-memory H2 bazu postojećim `DatabaseInitializerom`, sprema jasno označene softverske reference i generira G-code za jednakostranični trokut stranice 30 mm na ploči 500 × 500 mm. Generirani preview sprema se kao `MachiningJob` s količinom 1. Novi composition-root/repository kontekst zatim ponovno učitava job i provjerava oblik, materijal, ploču, parametre, reference, količinu i identičan G-code tekst.

## Razlog odabranog rješenja

Jedan eksplicitan objektni graf uklanja duplicirano stvaranje ovisnosti iz `Main` klase bez frameworka ili skrivenog runtime ponašanja. Package-private testni ulaz omogućuje isti wiring nad in-memory bazom, a ne uvodi opći DI container ili dodatni arhitektonski sloj.

## Arhitektonska povezanost

`app` sloj posjeduje composition root. SQL ostaje u JDBC implementacijama, validacija u validation sloju, geometrija u `ToolPathServiceu`, fit u zasebnom validatoru, a tekstualni output u G-code sloju. Controller dobiva već sastavljene service ovisnosti. Layout nije uveden.

## Važne odluke i ograničenja

- Projekt koristi ručni composition root bez Springa ili drugog DI frameworka.
- Testni machining parametri imaju `SOFTWARE_TEST_` nazive i nisu preporuka za fizičku obradu.
- Konzervativni profil i prošli softverski test ne potvrđuju ponašanje ZK-1325 / RichAuto A11 stroja.
- Quantity ostaje 1; nema layouta, capacityja, requiredSheetsa ni batch G-codea.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni Maven repository> -Dtest=ApplicationCompositionRootIntegrationTest test
<IntelliJ Maven> -Dmaven.repo.local=<lokalni Maven repository> test
```

### Stvarni rezultat

Ciljani test: 1 test, 0 failures, 0 errors, `BUILD SUCCESS`. Cijeli suite: 110 testova, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Obje uspješne provjere izvršene su s OpenJDK-om 26.0.2. Početni sandbox pokušaj nije mogao zatvoriti H2 JAR resurs; ponavljanje izvan sandboxa s istim kodom i dependencyjima uspjelo je.

### Što nije testirano

Nije pokrenut JavaFX vizualni runtime test. Generirani G-code nije fizički testiran na ZK-1325 / RichAuto A11 stroju.

## Otvorena pitanja

- Fizička Z-konvencija i RichAuto profil ostaju za kontrolirani test na ciljnom stroju.
- Layout i batch tok ostaju nakon GATE 1.

## Moguće poglavlje završnog rada

Arhitektura aplikacije, ručno povezivanje ovisnosti i integracijsko testiranje.

## Kandidati za isječke koda

### Kandidat: Ručni composition root

**Datoteka:** `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java`
**Klasa/metoda:** `ApplicationCompositionRoot#ApplicationCompositionRoot`
**Zašto je važan:** Prikazuje stvarne veze repository, validation, geometry, G-code, service i UI slojeva bez DI frameworka.
**Moguće poglavlje:** Arhitektura i organizacija aplikacije

```java
MachiningJobValidator machiningJobValidator = new MachiningJobValidator(
        shapeValidator, materialSheetValidator, machiningParametersValidator);
ToolPathService toolPathService = new ToolPathService(shapeValidator);
SingleShapeFitValidator singleShapeFitValidator = new SingleShapeFitValidator(
        new ToolPathBoundsCalculator(), materialSheetValidator);
RichAutoA11GCodeGenerator gCodeGenerator =
        new RichAutoA11GCodeGenerator(conservativePreviewProfile());
programGenerationService = new ProgramGenerationService(
        machiningJobValidator, toolPathService, singleShapeFitValidator, gCodeGenerator);
```

**Ideja opisa u radu:** Konstruktor vidljivo povezuje slojeve redoslijedom kojim ih use-case koristi, bez refleksije i framework konfiguracije.

### Kandidat: Softverski end-to-end round-trip

**Datoteka:** `src/test/java/hr/lukabosnjak/app/ApplicationCompositionRootIntegrationTest.java`
**Klasa/metoda:** `ApplicationCompositionRootIntegrationTest#generatesPersistsAndReloadsSingleEquilateralTriangleFromFreshContext`
**Zašto je važan:** Dokazuje integraciju od requesta preko generatora do svježeg repository konteksta i ponovnog učitavanja.
**Moguće poglavlje:** Integracijsko testiranje

```java
GCodeProgram previewResult = firstContext.programGenerationService().generate(input);
MachiningJob saved = firstContext.machiningJobRepository().save(new MachiningJob(
        null, references.user(), references.machine(), references.tool(), sheet,
        parameters, shape, "Software E2E triangle", 1, previewResult.text(), null, null));
ApplicationCompositionRoot freshContext =
        ApplicationCompositionRoot.forConnectionProvider(connectionProvider);
MachiningJob reloaded = freshContext.machiningJobRepository()
        .findById(saved.getMachiningJobId()).orElseThrow();
```

**Ideja opisa u radu:** Test koristi novi objektni graf za reload, pa rezultat nije samo povratna vrijednost istog repository objekta.

## Kandidat za sliku, dijagram ili tablicu

Nema — novi dijagram ili potvrđena snimka nisu izrađeni.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
