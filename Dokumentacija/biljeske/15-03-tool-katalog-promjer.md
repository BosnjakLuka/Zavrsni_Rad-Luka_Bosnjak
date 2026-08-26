# 15.3 — Katalog stvarnog alata i promjer

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Omogućiti unos stvarnog alata kroz katalog kada su poznati broj alata, naziv i
promjer, bez automatskog seedanja glodala Ø6 mm ili Ø8 mm.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/Tool.java` — nepoznati tip,
  rezna duljina i broj oštrica mogu biti `null`.
- `src/main/java/hr/lukabosnjak/validation/ReferenceDataValidator.java` —
  promjer ostaje obvezan, ostali tehnički atributi validiraju se samo kada su
  poznati.
- `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcToolRepository.java` i
  `JdbcMachiningJobRepository.java` — nullable mapiranje i spremanje.
- `src/main/resources/db/schema.sql` i
  `src/main/resources/db/migration/15-03-tool-optional-attributes.sql` —
  nullable stupci za nepoznate atribute.
- `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java` —
  registracija migracije.
- `src/main/java/hr/lukabosnjak/ui/controller/ToolFormController.java` i
  `src/main/resources/hr/lukabosnjak/ui/view/tool-form.fxml` — opcionalni
  unos nepoznatih atributa i jasna uputa operatoru.
- `src/test/java/hr/lukabosnjak/validation/ReferenceDataValidatorPartialToolTest.java`
  — test djelomično poznatog alata.

## Stvarna implementacija

Katalog sada prihvaća alat s poznatim brojem, nazivom i pozitivnim promjerom
dok tip, rezna duljina i broj oštrica mogu ostati prazni. `Tool.diameter` se
sprema i učitava iz baze te ostaje dio odabranog `Tool` objekta u
`ProgramGenerationRequest` i `MachiningJob`; ne uvodi se duplicirani unos
promjera u glavnom UI-u.

Nisu dodani seed zapisi za Ø6 mm ili Ø8 mm. Te vrijednosti ostaju kandidati dok
operator ne potvrdi stvarni alat i broj alata.

## Razlog odabranog rješenja

Omogućeno je bilježenje poznatih podataka bez izmišljanja tehničkih atributa.
Promjer je obvezan jer je potreban budućem sloju kompenzacije alata, dok
trenutačni generator još ne implementira G41/G42 ni geometrijski offset.

## Arhitektonska povezanost

JavaFX forma samo prikuplja poznate podatke. `ReferenceDataManagementService`
i `ReferenceDataValidator` odlučuju o poslovnoj valjanosti, JDBC sloj
sprema nullable vrijednosti, a `Tool` ostaje domenski podatak neovisan o UI-u.

## Važne odluke i ograničenja

- `Tool.diameter` je obvezan pozitivan podatak.
- Tip, rezna duljina i broj oštrica mogu biti nepoznati (`null`).
- Ø6 mm i Ø8 mm nisu automatski potvrđeni ni seedani.
- Stvarni tool number, promjer i fizička kompatibilnost nisu testirani na
  ZK-1325 / RichAuto A11.
- Implementacija cutter compensation pripada Koraku 16.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit: hr.lukabosnjak.validation.ReferenceDataValidatorPartialToolTest
IDE JUnit: hr.lukabosnjak.persistence.jdbc.JdbcRepositoriesIntegrationTest
IDE JUnit: hr.lukabosnjak.ui.controller.FxmlControllerContractTest
```

### Stvarni rezultat

IDE build je uspješan. Sva tri ciljana testna razreda završila su s exit code 0.

### Što nije testirano

Fizički unos i obrada stvarnog alata na ZK-1325 / RichAuto A11 nisu testirani.
Nije potvrđeno koji se od promjera Ø6 mm ili Ø8 mm stvarno koristi.

## Otvorena pitanja

- Koji su stvarni tool number i promjer alata za prvi fizički test?
- Jesu li tip, rezna duljina i broj oštrica poznati za taj alat?
- Kako će konkretni RichAuto A11 primijeniti promjer u cutter compensation
  workflowu?

## Moguće poglavlje završnog rada

Katalog referentnih podataka, validacija i priprema za kompenzaciju alata.

## Kandidati za isječke koda

### Kandidat: Validacija poznatih i nepoznatih atributa alata

**Datoteka:** `src/main/java/hr/lukabosnjak/validation/ReferenceDataValidator.java`  
**Klasa/metoda:** `ReferenceDataValidator#validate(Tool)`  
**Zašto je važan:** Prikazuje da se nepoznati podaci ne izmišljaju, dok se
  promjer kao kritični podatak i dalje obvezno provjerava.  
**Moguće poglavlje:** Validacija i upravljanje kataloškim podacima

```java
requirePositive(tool.getDiameter(), "Promjer alata mora biti veći od 0 mm.");
requirePositiveWhenKnown(tool.getCuttingLength(),
        "Rezna duljina alata mora biti veća od 0 mm.");
requirePositiveIntegerWhenKnown(tool.getFluteCount(),
        "Broj oštrica mora biti veći od 0.");
```

**Ideja opisa u radu:** Pravila validacije razlikuju obvezne identifikacijske
  podatke od tehničkih vrijednosti koje mogu biti nepoznate.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
