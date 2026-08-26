# 16.4 — RichAuto profil i reference feed postavke

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Uskladiti RichAuto A11 generator s poznatim referentnim `.nc` formatom bez
pretvaranja neprovjerenih postavki kontrolera u trajne fizičke činjenice.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java` — dodan
  imenovani reference-program profil.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` —
  production preview koristi reference-profile emitiranje.
- `src/test/java/hr/lukabosnjak/gcode/RichAutoA11ProfileTest.java` — test
  reference postavki i odvojenosti od fizičke potvrde.
- `src/test/java/hr/lukabosnjak/service/ProgramGenerationServiceTest.java` —
  provjera da generator dobiva efektivne feed/plunge snapshot vrijednosti.
- `Dokumentacija/biljeske/00_indeks.md` i `00_odluke.md` — razvojna odluka i
  indeks.

## Stvarna implementacija

`RichAutoA11Profile.referenceProgramProfile()` uključuje `G54`, `F` riječi i
`M03`/`M05`, a isključuje `S`, čime odgovara poznatoj strukturi reference:
`G90 G54`, `M03`, `F150`, `F500`, `M05`, `M30`. Generator i dalje uzima
stvarne vrijednosti iz `MachiningParameters`; profil samo određuje hoće li se
pojedina naredba emitirati.

Production composition root više ne koristi profil koji prešutno uklanja
`F`/`G54`/spindle naredbe iz preview outputa, nego imenovani reference profil.
`physicallyConfirmedCapabilities` ostaje odvojen od emission flagova i ostaje
prazan dok se ne provede fizička provjera.

Glavni UI i dalje dobiva machining vrijednosti iz `MachiningParametersPreset`.
Pri spremanju se isti `MachiningParameters` objekt iz generation requesta
sprema u `MachiningJob`, pa snapshot odgovara vrijednostima s kojima je
program generiran i kada ih operator nije ručno upisao.

## Razlog odabranog rješenja

Imenovani profil čini razliku između „emitiraj naredbu u softverskom outputu” i
„kontroler je fizički potvrdio naredbu” vidljivom i ponovljivom. Feed se ne
uklanja iz generatora; uklanja se samo nepotreban ponovljeni ručni unos kroz
presetski workflow.

## Arhitektonska povezanost

RichAuto profil i envelope ostaju u `gcode` sloju. Preset i snapshot pripadaju
service/domain toku, dok controller samo čita polja i sastavlja request.
Nisu uvedeni SQL, nova UI tehnička polja ni layout logika.

## Važne odluke i ograničenja

- Reference-profile output uključuje `G54`, `F150`/`F500` kroz efektivne
  machining parametre i spindle start/stop bez `S` retka.
- Emitiranje nije dokaz da konkretni A11 čita `F`, `G54`, `M03` ili `M05`.
- Fizički potvrđene mogućnosti profila ostaju zaseban skup podataka.
- `MachiningParameters` snapshot ostaje izvor vrijednosti korištenih pri
  generiranju.

## Build i testiranje

### Izvršene naredbe

```text
mvn -q -Dtest=hr.lukabosnjak.gcode.RichAutoA11ProfileTest,hr.lukabosnjak.gcode.RichAutoA11GCodeGeneratorTest,hr.lukabosnjak.service.ProgramGenerationServiceTest,hr.lukabosnjak.app.ApplicationCompositionRootIntegrationTest test
IDE JUnit: hr.lukabosnjak.gcode.RichAutoA11ProfileTest
IDE JUnit: hr.lukabosnjak.gcode.RichAutoA11GCodeGeneratorTest
IDE JUnit: hr.lukabosnjak.service.ProgramGenerationServiceTest
IDE JUnit: hr.lukabosnjak.app.ApplicationCompositionRootIntegrationTest
```

### Stvarni rezultat

Maven naredba nije izvršena jer `mvn` nije dostupan u okruženju. IDE ciljani
testovi završili su uspješno:

- `RichAutoA11ProfileTest`: 8 testova
- `RichAutoA11GCodeGeneratorTest`: 6 testova
- `ProgramGenerationServiceTest`: 3 testa
- `ApplicationCompositionRootIntegrationTest`: 2 testa

Ukupno je u IDE-u uspješno izvršeno 19 ciljanih testova.

### Što nije testirano

Nije izvršen puni Maven suite ni vizualni UI test. Nije proveden fizički test
na ZK-1325 / RichAuto A11 i nije potvrđeno čitanje `F`, `S`, `G54` ili spindle
naredbi na konkretnom kontroleru.

## Otvorena pitanja

- Fizički potvrditi stvarne A11 read/ignore postavke i spindle ponašanje.
- Nakon fizičkog testa ažurirati `physicallyConfirmedCapabilities` samo prema
  stvarnom rezultatu.

## Moguće poglavlje završnog rada

Konfigurabilni G-code profil i reproducibilnost machining postavki.

## Kandidati za isječke koda

### Kandidat: Reference-profile postavke bez fizičke tvrdnje

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java`  
**Klasa/metoda:** `RichAutoA11Profile#referenceProgramProfile`  
**Zašto je važan:** Pokazuje imenovanje i centralizaciju output postavki reference
  programa uz odvojenu evidenciju fizičke potvrde.  
**Moguće poglavlje:** G-code profil i testiranje kontrolera

```java
return new RichAutoA11Profile(
        true,
        false,
        true,
        true,
        Units.MILLIMETERS,
        PositioningMode.ABSOLUTE,
        3,
        ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT,
        ArcCenterMode.RELATIVE_TO_ARC_START,
        Set.of());
```

**Ideja opisa u radu:** Profil reproducira poznati reference output, ali prazan
skup fizički potvrđenih mogućnosti jasno odvaja softversku konfiguraciju od
dokaza ponašanja konkretnog stroja.

## Kandidat za sliku, dijagram ili tablicu

Tablica reference-profile naredbi i statusa fizičke potvrde.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
