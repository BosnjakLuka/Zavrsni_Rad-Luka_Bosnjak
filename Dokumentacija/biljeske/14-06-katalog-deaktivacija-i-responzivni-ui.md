# 14.6 — Deaktivacija kataloga i responzivni UI

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Omogućiti administratoru deaktivaciju i ponovnu aktivaciju materijala, CNC
strojeva i alata te poboljšati prikaz kataloga u smanjenom prozoru.

## Promijenjene datoteke

- `ReferenceDataManagementService.java` — centralne operacije promjene statusa.
- `MaterialTypeRepository.java`, `CncMachineRepository.java`,
  `ToolRepository.java` — repository ugovori.
- JDBC repositoryji i `schema.sql` — parametrizirane statusne izmjene.
- `CncMachine.java` — aktivni status stroja.
- `CatalogController.java` i `catalog.fxml` — administratorske akcije i
  omotavajući raspored kataloga.
- `ReferenceDataService.java`, `MaterialReferenceDataService.java` —
  generator dohvaća samo aktivne zapise.
- `DatabaseInitializer.java` i migracija `14-06-catalog-active-status.sql` —
  nadogradnja postojećih baza.

## Stvarna implementacija

Admin i engineer s pravom upravljanja katalogom mogu zapis deaktivirati ili
ponovno aktivirati. Materijali koriste postojeći `deleted_at`, dok strojevi i
alati koriste aktivni status. Zapis se fizički ne briše kako bi se očuvale
foreign-key veze i povijesni machining jobovi. Katalog prikazuje status i koristi
`ScrollPane`/`FlowPane` raspored koji se prelama u užem prozoru.

## Razlog odabranog rješenja

Soft-deaktivacija daje administratorsku kontrolu bez gubitka povijesnih
podataka i bez narušavanja referencijalnog integriteta.

## Arhitektonska povezanost

Controller poziva service, service provjerava centralno pravo
`MANAGE_REFERENCE_DATA`, a JDBC repositoryji koriste `PreparedStatement`.

## Važne odluke i ograničenja

- Administratorsko pravo je potpuno unutar odobrenog kataloškog opsega, ali
  fizičko brisanje nije uvedeno zbog povijesti jobova.
- Neaktivni zapisi nisu ponuđeni u generatoru.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit: ReferenceDataManagementServiceTest
IDE JUnit: FxmlControllerContractTest
IDE JUnit: JdbcMachiningJobRepositoryIntegrationTest
```

### Stvarni rezultat

Build i sva tri navedena ciljana testa uspješno su završeni.

### Što nije testirano

Katalog nije ručno vizualno provjeren pri različitim veličinama prozora.
RichAuto A11 fizička kompatibilnost nije testirana.

## Otvorena pitanja

- Potrebna je ručna provjera prelamanja kartica i statusnih akcija u JavaFX
  prozoru.

## Moguće poglavlje završnog rada

Upravljanje referentnim podacima, autorizacija i JavaFX korisničko sučelje.

## Kandidati za isječke koda

### Kandidat: Centralizirana promjena statusa kataloga

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java`  
**Klasa/metoda:** `ReferenceDataManagementService#setToolActive`  
**Zašto je važan:** Prikazuje autorizirani service tok bez SQL-a u controlleru.  
**Moguće poglavlje:** Slojevita arhitektura i autorizacija

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
