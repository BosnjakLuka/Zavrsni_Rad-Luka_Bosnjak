# 14.2 — Application shell i navigacija

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Dodati jednostavnu JavaFX navigaciju između glavnog generatora, spremljenih
programa, kataloga i upravljanja korisnicima bez SPA/router frameworka.

## Promijenjene datoteke

- `ApplicationNavigation.java`, `Main.java` i `ApplicationCompositionRoot.java`
  — nove navigacijske scene i composition-root wiring.
- `SavedProgramsController.java` i `saved-programs.fxml` — zaseban pregled,
  otvaranje i `.nc` export spremljenih programa.
- `CatalogController.java` i `catalog.fxml` — katalog akcija za materijale,
  strojeve i alate uz role gate.
- `MainFormController.java` i `main-form.fxml` — navigacijski gumbi, hrvatski
  nazivi akcija i brzi pregled postojećih programa.
- `FxmlControllerContractTest.java` i `ApplicationCompositionRootIntegrationTest.java`
  — ugovorne provjere novih prikaza i navigacijskog ugovora.

## Stvarna implementacija

`Main` sada prikazuje generator, spremljene programe, katalog i korisničko
upravljanje kao odvojene JavaFX scene. Svaka scena koristi postojeći
composition root i controller factory.

Generator zadržava postojeće odabire iz baze, generiranje, spremanje i export.
Spremljeni programi imaju vlastiti ekran s dohvatom, otvaranjem i `.nc`
exportom. Katalog koristi postojeće forme za dodavanje materijala, stroja i
alata; `ADMIN` i `ENGINEER` mogu ih otvoriti, dok `OPERATOR` može samo koristiti
postojeće odabire na generatoru. Sve scene prikazuju prijavljenog korisnika i
rolu te omogućuju odjavu.

## Razlog odabranog rješenja

Zadržana je postojeća jednostavna zamjena scena u klasi `Main`. Time se dobiva
jasno razdvajanje funkcionalnosti bez uvođenja nepotrebnog router frameworka.

## Arhitektonska povezanost

Navigacija pripada UI/application sloju. Controlleri pozivaju postojeće service
klase, dok dohvat spremljenih programa, export i katalog ostaju u service i
persistence slojevima.

## Važne odluke i ograničenja

- Nisu uvedeni `quantity`, layout ni batch elementi.
- Nije uveden SPA/router framework.
- Role gateovi za katalog i korisnike ostaju centralizirani kroz postojeći
  `AuthorizationService`.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit run: FxmlControllerContractTest
```

### Stvarni rezultat

IDE build je uspješan bez problema. FXML ugovorni test je uspješno izvršen i
potvrdio je postojeće te nova `saved-programs.fxml` i `catalog.fxml` mapiranja.

### Što nije testirano

Nije izvršen puni Maven suite. Scene nisu ručno vizualno pokrenute, a nije
proveden fizički test na CNC stroju.

## Otvorena pitanja

- Za 14.3 treba odlučiti treba li katalog dobiti stvarni pregled zapisa ili su
  postojeće forme i generator odabiri dovoljni za Iteraciju 1.

## Moguće poglavlje završnog rada

Organizacija JavaFX korisničkog sučelja i navigacija aplikacijskog shell-a.

## Kandidati za isječke koda

### Kandidat: Jednostavna navigacija scenama

**Datoteka:** `src/main/java/hr/lukabosnjak/app/Main.java`  
**Klasa/metoda:** `Main#showSavedPrograms`, `Main#showCatalog`  
**Zašto je važan:** Prikazuje navigaciju bez dodatnog router frameworka i
provjeru aktivne sesije prije prikaza ekrana.  
**Moguće poglavlje:** JavaFX arhitektura aplikacije

```java
showView("/hr/lukabosnjak/ui/view/saved-programs.fxml", 900, 620, 760, 480);
```

**Ideja opisa u radu:** Scene se mijenjaju kroz jedan application entry point.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
