# 14.3 — Kataloški tokovi

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Dovršiti minimalne kataloge `MaterialType`, `CncMachine` i `Tool` potrebne
prije generiranja programa, bez izmišljanja tehničkih vrijednosti alata.

## Promijenjene datoteke

- `ReferenceDataManagementService.java` — centralizirani katalog access,
  dohvat, dodavanje i uređivanje.
- `MaterialTypeRepository.java`, `CncMachineRepository.java`,
  `ToolRepository.java` — update ugovori.
- `JdbcMaterialTypeRepository.java`, `JdbcCncMachineRepository.java`,
  `JdbcToolRepository.java` — prepared-statement update operacije.
- `MaterialTypeFormController.java`, `CncMachineFormController.java`,
  `ToolFormController.java` — create/edit način rada.
- `CatalogController.java` i `catalog.fxml` — pregled zapisa i akcije.
- `ReferenceDataManagementServiceTest.java`, `ReferenceDataServiceTest.java`
  — prilagodba testnih repository stubova.

## Stvarna implementacija

Katalog sada učitava postojeće vrste materijala, CNC strojeve i alate, a
postojeće forme podržavaju dodavanje i uređivanje. Pristup katalogu provjerava
centralni `AuthorizationService`, pa controlleri ne sadrže SQL.

Podaci alata i dalje se unose isključivo kroz stvarne vrijednosti obrasca.
Nisu uvedeni lažni presetovi za Ø6/Ø8 niti nepotvrđeni parametri.

## Razlog odabranog rješenja

Dodavanje i uređivanje pokrivaju stvarne potrebe prije generiranja, dok
brisanje i složeni CRUD nisu potrebni za Iteraciju 1.

## Arhitektonska povezanost

UI controller poziva `ReferenceDataManagementService`; service validira,
normalizira i provjerava pravo, a JDBC repository izvršava parametrizirani SQL.

## Važne odluke i ograničenja

- Samo `ADMIN` i `ENGINEER` mogu dodavati ili uređivati katalog.
- `OPERATOR` koristi postojeće odabire bez uređivanja.
- Ne uvode se nepotvrđene vrijednosti alata, stroja ili kontrolera.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit run: ReferenceDataManagementServiceTest
IDE JUnit run: FxmlControllerContractTest
```

### Stvarni rezultat

IDE build je uspješan. `ReferenceDataManagementServiceTest` je uspješno
izvršio 3 testa, a FXML ugovorni test 1 test.

### Što nije testirano

Nisu izvršeni puni Maven suite ni JDBC update integration testovi. Katalog nije
vizualno ručno testiran u JavaFX prozoru.

## Otvorena pitanja

- Fizički parametri stvarnog alata trebaju se unijeti tek nakon potvrde
  dokumentacijom ili mjerenjem; ovaj korak ne zaključava takve vrijednosti.

## Moguće poglavlje završnog rada

Validacija, persistence i upravljanje referentnim podacima CNC aplikacije.

## Kandidati za isječke koda

### Kandidat: Katalog service s autorizacijom

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java`  
**Klasa/metoda:** `ReferenceDataManagementService#createTool`, `updateTool`  
**Zašto je važan:** Povezuje role gate, validaciju i persistence bez SQL-a u UI-u.  
**Moguće poglavlje:** Slojevita arhitektura i validacija

```java
requireCatalogAccess();
validator.validate(tool);
return toolRepository.update(tool);
```

**Ideja opisa u radu:** Service sloj je jedinstvena granica za katalog operacije.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
