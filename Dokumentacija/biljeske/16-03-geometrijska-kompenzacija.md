# 16.3 — Aplikacijska geometrijska kompenzacija

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Uvesti kompenzaciju promjera alata prije generiranja G-koda, bez oslanjanja na
nepotvrđeni RichAuto A11 `D`/tool-table workflow.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/geometry/CutSide.java` — eksplicitna unutarnja
  ili vanjska strana reza.
- `src/main/java/hr/lukabosnjak/geometry/ToolPathCompensationService.java` —
  izračun putanje centra alata iz nominalne konture.
- `src/main/java/hr/lukabosnjak/service/ProgramGenerationRequest.java` —
  prosljeđuje eksplicitni `CutSide`.
- `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` —
  primjenjuje kompenzaciju prije fit provjere i G-code generatora.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` —
  registrira compensation servis.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` —
  trenutačni V1 tok eksplicitno koristi `CutSide.INSIDE`.
- `src/test/java/hr/lukabosnjak/geometry/ToolPathCompensationServiceTest.java` —
  testovi pravokutnika s promjerima Ø6 i Ø8.
- `src/test/java/hr/lukabosnjak/service/ProgramGenerationServiceTest.java` i
  `src/test/java/hr/lukabosnjak/app/ApplicationCompositionRootIntegrationTest.java`
  — usklađeni requesti.

## Stvarna implementacija

`ToolPathCompensationService` računa `radius = Tool.diameter / 2`. Linijske
zatvorene konture offsetiraju se presjekom susjednih paralelnih linija, a
kružne konture sastavljene od lukova dobivaju promijenjeni radijus. `CutSide`
je obvezan ulaz; logika ne zaključuje stranu iz naziva oblika.

U `ProgramGenerationService` nominalna kontura ostaje odvojena od putanje
centra alata. Kompenzirana putanja prolazi `SingleShapeFitValidator` prije
slanja u `RichAutoA11GCodeGenerator`, pa se izlaz iz granica ploče ili XY
radnog područja odbija.

Ne emitiraju se `G41`, `G42`, `G40` ni `D` naredbe. Zato nema controller-side
aktivacije koju bi trebalo resetirati; program završava postojećim generatorovim
footerom. Trenutačni operatorski tok izričito koristi `INSIDE`, jer postojeće
lokalne konture počinju na `(0,0)` i vanjski offset bi odmah izašao iz granica
ploče bez zasebnog margina/placement pravila.

## Razlog odabranog rješenja

RichAuto dokumentacija potvrđuje mogućnost čitanja/ignoriranja G40, ali nije
potvrđen način zadavanja alata i radijusa za G41/G42 na konkretnom A11.
Aplikacijski offset daje determinističku putanju i ne pretpostavlja Haas/LinuxCNC
`D` semantiku. Promjeri Ø6 i Ø8 koriste se samo kao tehnički softverski testni
slučajevi dok se stvarni alat ne potvrdi.

## Arhitektonska povezanost

Kompenzacija pripada geometry/toolpath sloju. Service sloj koordinira redoslijed
nominalna kontura → putanja centra alata → fit validacija → G-code. G-code sloj
ostaje odgovoran samo za pretvorbu pripremljenog `ToolPath` objekta u tekst.
Nisu uvedeni SQL, JavaFX geometrija ni layout algoritam.

## Važne odluke i ograničenja

- Aplikacija koristi geometrijski offset kao V1 strategiju.
- `Tool.diameter` je jedini izvor promjera; radius je uvijek polovica promjera.
- Smjer postojećih kontura je determinističan; strana reza ipak je zaseban
  eksplicitni `CutSide`.
- V1 podržava homogene linijske konture i kružne konture sastavljene od lukova;
  miješane linijsko-lučne konture nisu uvedene.
- G41/G42/G40 i RichAuto `D` workflow ostaju izvan implementacije dok se ne
  potvrde na konkretnom kontroleru.
- Vanjski rez na konturi koja počinje na rubu ploče zahtijeva budući placement
  ili marginu; trenutačni V1 tok zato koristi eksplicitni unutarnji rez.

## Build i testiranje

### Izvršene naredbe

```text
mvn -q -Dtest=hr.lukabosnjak.geometry.ToolPathCompensationServiceTest,hr.lukabosnjak.service.ProgramGenerationServiceTest,hr.lukabosnjak.app.ApplicationCompositionRootIntegrationTest test
IDE JUnit: hr.lukabosnjak.geometry.ToolPathCompensationServiceTest
IDE JUnit: hr.lukabosnjak.service.ProgramGenerationServiceTest
IDE JUnit: hr.lukabosnjak.app.ApplicationCompositionRootIntegrationTest
```

### Stvarni rezultat

Maven naredba nije izvršena jer `mvn` nije dostupan u okruženju. IDE ciljani
testovi završili su uspješno:

- `ToolPathCompensationServiceTest`: 2 testa
- `ProgramGenerationServiceTest`: 3 testa
- `ApplicationCompositionRootIntegrationTest`: 2 testa

### Što nije testirano

Nije izvršen puni Maven suite. Nisu testirani svi oblici u zasebnom
kompenzacijskom audit-testu. Nije proveden fizički test na ZK-1325 /
RichAuto A11, niti je potvrđeno ponašanje G41/G42/D registra na kontroleru.

## Otvorena pitanja

- Treba dodati zaseban audit za square, rectangle, circle i equilateral triangle.
- Treba potvrditi treba li budući unutarnji/vanjski odabir biti vidljiv operatoru.
- Za vanjski rez treba definirati placement/margin pravilo prije layout Iteracije 2.

## Moguće poglavlje završnog rada

Geometrijska kompenzacija alata i priprema putanje centra alata.

## Kandidati za isječke koda

### Kandidat: Offset nominalne konture u putanju centra alata

**Datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathCompensationService.java`  
**Klasa/metoda:** `ToolPathCompensationService#compensate`  
**Zašto je važan:** Prikazuje korištenje stvarnog promjera alata i odvajanje
  geometrijske kompenzacije od G-code teksta.  
**Moguće poglavlje:** Geometrija i generiranje putanje alata

```java
double radius = tool.getDiameter() / 2.0;
if (!Double.isFinite(radius) || radius <= 0.0) {
    throw new IllegalArgumentException("Tool diameter must be positive and finite");
}

if (programmedContour.segments().stream().allMatch(LineSegment.class::isInstance)) {
    return compensateLines(programmedContour, radius, cutSide);
}
if (programmedContour.segments().stream().allMatch(ArcSegment.class::isInstance)) {
    return compensateArcs(programmedContour, radius, cutSide);
}
```

**Ideja opisa u radu:** Metoda prima nominalnu konturu, alat i eksplicitnu
stranu reza. Na temelju promjera bira geometrijsku obradu linijske ili kružne
konture, bez stvaranja G-code naredbi u geometry sloju.

### Kandidat: Fit provjera kompenzirane putanje

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java`  
**Klasa/metoda:** `ProgramGenerationService#generate`  
**Zašto je važan:** Pokazuje da se granice provjeravaju nakon kompenzacije, a
  generator dobiva putanju centra alata.  
**Moguće poglavlje:** Service orkestracija i sigurnosne provjere

```java
ToolPath programmedContour = toolPathService.generate(request.shape());
ToolPath cutterCenterPath = toolPathCompensationService.compensate(
        programmedContour, request.tool(), request.cutSide());
singleShapeFitValidator.validate(cutterCenterPath, request.materialSheet(), request.machine());
return gCodeGenerator.generate(cutterCenterPath, request.machiningParameters());
```

**Ideja opisa u radu:** Service sloj održava granice između geometrije,
validacije i G-codea. Nominalna kontura se ne šalje izravno generatoru, nego
se prvo pretvara u putanju centra alata i provjerava.

## Kandidat za sliku, dijagram ili tablicu

Tablica nominalne konture, putanje centra alata i utjecaja promjera Ø6/Ø8.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
