# 18.3 — Spremljeni programi ispod kataloških kartica

**Datum:** 2026-08-27
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Dodati postojeći read-only tok spremljenih programa u katalog, kao zasebnu široku sekciju neposredno ispod kartica vrsta materijala, CNC strojeva i alata.

## Promijenjene datoteke

- `src/main/resources/hr/lukabosnjak/ui/view/catalog.fxml` — dodana donja sekcija s popisom, G-code pregledom i akcijama.
- `src/main/java/hr/lukabosnjak/ui/controller/CatalogController.java` — učitavanje, pregled, otvaranje i izvoz spremljenih programa.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` i `Main.java` — injektirani postojeći servisi i povećana početna veličina kataloga.
- `src/test/java/hr/lukabosnjak/ui/controller/FxmlControllerContractTest.java` i `ApplicationCompositionRootIntegrationTest.java` — provjera FXML položaja i stvaranja controllera.
- `Dokumentacija/zahtjevi/funkcionalni_zahtjevi.md` i `Dokumentacija/biljeske/00_indeks.md` — usklađeni zahtjev `FZ-23` i indeks.
- `Dokumentacija/biljeske/18-03-spremljeni-programi-ispod-kataloga.md` — ova bilješka.

## Stvarna implementacija

Ispod `FlowPanea` koji sadrži tri postojeće kataloške kartice dodana je zasebna sekcija „Spremljeni programi”. Ona prikazuje identifikator, naziv, vrijeme nastanka, korisničko ime autora, oblik i CNC stroj. Odabirom zapisa prikazuje se spremljeni G-code, a program se može otvoriti u postojećem generatoru ili izvesti kao `.nc`.

Sekcija koristi postojeće `SavedJobService` i `ProgramExportService`. Pristup podacima i akcijama dodatno traži postojeću dozvolu `MANAGE_REFERENCE_DATA`, koju imaju `ADMIN` i `ENGINEER`. Nisu uvedeni novi SQL upiti ni duplikat persistence logike.

## Razlog odabranog rješenja

Korisniku su spremljeni programi sada dostupni na istom ekranu kao upravljački katalozi, ali su strukturno odvojeni i uvijek deklarirani ispod tri gornje kartice. Ponovna uporaba postojećih servisa zadržava slojevite granice i isti G-code/export tok kao zaseban ekran spremljenih programa.

## Arhitektonska povezanost

JavaFX controller koordinira selekciju, preview, navigaciju i `FileChooser`. Dohvat poslova ostaje u `SavedJobService`/repository toku, a zapis `.nc` datoteke u `ProgramExportService`/gcode sloju. Controller ne sadrži SQL ni generiranje G-koda.

## Važne odluke i ograničenja

- Sekcija je izvan `FlowPanea`, pa nije pored niti iznad triju kataloških kartica.
- Ovaj korak implementira pregled, otvaranje i izvoz postojećih aktivnih zapisa kakve vraća trenutačni repository.
- Uređivanje, soft-delete, role-ovisne izmjene i pokazatelji iskorištenja iz `FZ-30`/`FZ-31` nisu implementirani ovim korakom.
- Nisu promijenjeni `V1BootstrapService`, H2 shema ni fizičke postavke CNC stroja.

## Build i testiranje

### Izvršene naredbe

```text
IntelliJ bundled mvn.cmd -q -Dmaven.repo.local=<postojeći korisnički Maven cache> -Dtest=FxmlControllerContractTest,ApplicationCompositionRootIntegrationTest,SavedJobServiceTest test
IntelliJ bundled mvn.cmd -q -Dmaven.repo.local=<postojeći korisnički Maven cache> test
```

### Stvarni rezultat

Ciljani testovi prošli su bez greške. Puni Maven skup izvršio je 143 testa: 0 neuspjeha, 0 pogrešaka i 0 preskočenih testova. FXML test potvrđuje da je lista spremljenih programa deklarirana nakon i izvan `FlowPanea` s tri kataloške kartice.

### Što nije testirano

Katalog nije ručno otvoren u JavaFX prozoru, pa vizualni raspored, odabir programa, G-code preview, navigacija i `FileChooser` nisu ručno vizualno potvrđeni. Nije izvršen fizički test na ZK-1325 / RichAuto A11, što nije potrebno za ovu UI promjenu.

## Otvorena pitanja

- Nema otvorenih pitanja unutar ovog read-only koraka. Planirana prava uređivanja i soft-delete pripadaju zasebnoj implementaciji.

## Moguće poglavlje završnog rada

Integracija kataloga, povijesti spremljenih CNC programa i slojevita JavaFX arhitektura.

## Kandidati za isječke koda

### Kandidat: Zaštićeno učitavanje spremljenih programa u katalog

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/CatalogController.java`
**Klasa/metoda:** `CatalogController#refreshSavedPrograms`
**Zašto je važan:** Pokazuje UI koordinaciju centralne provjere prava i postojećeg service dohvata bez SQL-a u controlleru.
**Moguće poglavlje:** Autorizacija i slojevita arhitektura korisničkog sučelja

```java
authorizationService.require(AuthorizationService.Permission.MANAGE_REFERENCE_DATA);
savedProgramsList.getItems().setAll(savedJobService.loadAll());
savedProgramPreview.clear();
```

**Ideja opisa u radu:** Katalog prikazuje programe samo ovlaštenim rolama, dok persistence ostaje iza servisne granice.

## Kandidat za sliku, dijagram ili tablicu

Korisnički priložena snimka zasebnog ekrana spremljenih programa može poslužiti kao referenca početnog stanja; lokalna privremena putanja nije zapisana. Nova integrirana sekcija još nije vizualno snimljena.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
