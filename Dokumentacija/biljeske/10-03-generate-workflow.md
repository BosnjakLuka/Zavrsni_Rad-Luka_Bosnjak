# 10.3 — Generate workflow za jedan element

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Povezati Generate kontrolu s postojećim validation, geometry, bounds/fit i G-code slojevima za jedan element, bez spremanja, layouta ili quantity UI-a.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/ProgramGenerationRequest.java` i `ProgramGenerationService.java` — request granica i orkestracija single-element toka.
- `src/main/java/hr/lukabosnjak/service/ReferenceDataService.java` i `ReferenceDataAccessException.java` — read-only učitavanje strojeva i alata.
- `src/main/java/hr/lukabosnjak/app/Main.java` — composition root za controllere i konzervativni preview profil.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` i `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — odabir stroja/alata, Generate i preview.
- `src/test/java/hr/lukabosnjak/service/ProgramGenerationServiceTest.java` i `ReferenceDataServiceTest.java` — ciljani service testovi.
- `Dokumentacija/biljeske/00_indeks.md`, `00_odluke.md` i ova bilješka — evidencija koraka.

## Stvarna implementacija

`ProgramGenerationService` prima `ProgramGenerationRequest`, stvara prolazni `MachiningJob` s internom količinom `1`, pokreće postojeći `MachiningJobValidator`, generira `ToolPath`, provjerava fit na ploči i radnom području stroja te predaje putanju postojećem `GCodeGenerator`u. Rezultat je `GCodeProgram` koji controller prikazuje metodom `text()`.

Controller čita JavaFX input, sastavlja request i prikazuje G-code ili poruku postojeće validacije. Ne sadrži SQL, izračun trokuta, bounds izračun, layout ili sastavljanje G-code stringa. Read-only `ReferenceDataService` puni izbor strojeva iz H2 i učitava alate samo za odabrani stroj.

## Razlog odabranog rješenja

Use-case servis koordinira stvarni tok između postojećih slojeva, bez mega-servisa i bez vezivanja UI-ja na JDBC. Poseban read-only servis daje controlleru samo podatke potrebne za odabir reference, a ne persistence detalje.

## Arhitektonska povezanost

UI mapira input u request i prikazuje rezultat. Service sloj orkestrira validation, geometry, fit i G-code. JDBC ostaje u `persistence.jdbc`; G-code sloj prima pripremljeni `ToolPath` i parametre, bez UI i layout ovisnosti.

## Važne odluke i ograničenja

- Iteracija 1 uvijek koristi `quantity = 1` unutar servicea, bez korisničkog polja.
- Preview profil koristi G21, G17, G90 i M30, negativnu Z konvenciju te preciznost 3; F, S, G54, M03 i M05 nisu emitirani.
- Skup fizički potvrđenih RichAuto mogućnosti je prazan. Softverski preview nije fizički testiran na ZK-1325 / RichAuto A11.
- Save, Export i Saved Programs nisu implementirani ovim korakom; nema quantity, layouta, capacityja, requiredSheetsa ni batch G-codea.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni Maven repository> -Dtest=ProgramGenerationServiceTest,ReferenceDataServiceTest,NumericInputParserTest test
```

### Stvarni rezultat

Maven je pokrenut s OpenJDK-om 26.0.2 i započeo kompilaciju 63 produkcijske datoteke s `release 26`, ali je završio s `BUILD FAILURE` prije izvršavanja testova na postojećoj ovisnosti `h2-2.4.240.jar` (`Fatal error compiling ... h2-2.4.240.jar`). Novi testovi nisu izvršeni.

### Što nije testirano

Nisu izvršeni ciljani unit testovi ni JavaFX runtime tok. Nije izvršen fizički test G-codea na stroju, niti Save, Export ili Saved Programs tok.

## Otvorena pitanja

- Prije ponovnog Maven testa treba dijagnosticirati postojeći problem pristupa/kompilacije H2 JAR ovisnosti.
- Aplikacijska H2 baza mora sadržavati spremljeni CNC stroj i aktivni pripadajući alat da korisnik može generirati preview.

## Moguće poglavlje završnog rada

Integracija korisničkog unosa, validacije, geometrijske putanje i G-code generatora.

## Kandidati za isječke koda

### Kandidat: Orkestracija Generate use-casea

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java`
**Klasa/metoda:** `ProgramGenerationService#generate`
**Zašto je važan:** Pokazuje redoslijed validation → ToolPath → fit → G-code bez poslovne logike u controlleru.
**Moguće poglavlje:** Servisni sloj i generiranje CNC programa

```java
machiningJobValidator.validate(job);
ToolPath toolPath = toolPathService.generate(request.shape());
singleShapeFitValidator.validate(toolPath, request.materialSheet(), request.machine());
return gCodeGenerator.generate(toolPath, request.machiningParameters());
```

**Ideja opisa u radu:** Use-case servis spaja postojeće specijalizirane slojeve, a svaki sloj zadržava vlastitu odgovornost.

### Kandidat: Interna količina jednog elementa

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java`
**Klasa/metoda:** `ProgramGenerationService#generate`
**Zašto je važan:** Jasno provodi ograničenje prve iteracije bez quantity inputa u UI-ju.
**Moguće poglavlje:** Iterativni razvoj aplikacije

```java
private static final int SINGLE_ELEMENT_QUANTITY = 1;
...
request.machiningParameters(), request.shape(), "Preview", SINGLE_ELEMENT_QUANTITY,
```

**Ideja opisa u radu:** Jedan element je eksplicitna granica workflowa, a ne skrivena posljedica UI-ja.

## Kandidat za sliku, dijagram ili tablicu

Nema — vizualna snimka integriranog workflowa nije stvarno izrađena ni potvrđena.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
