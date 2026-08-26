# Saved Programs — quick access i fresh-context round-trip

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Dovršiti pregled spremljenih machining jobova, prikaz spremljenog G-koda,
ponovno učitavanje parametara u generator i `.nc` export odabranog programa.

## Promijenjene datoteke

- `SavedProgramsController.java` i `saved-programs.fxml` — lista, osnovni
  podaci, G-code preview, otvaranje i export.
- `ApplicationNavigation.java`, `Main.java` i `MainFormController.java` —
  quick-access navigacija i učitavanje joba u generator.
- `JdbcMachiningJobRepositoryIntegrationTest.java` — fresh-context round-trip
  provjera kroz novi repository i `SavedJobService`.
- `00_indeks.md` i ova bilješka — razvojni trag.

## Stvarna implementacija

Saved Programs ekran prikazuje ID, naziv, datum, korisnika, oblik i stroj.
Odabirom se prikazuje spremljeni G-code. `Otvori` dohvaća job novim
`SavedJobService` pozivom i otvara generator s učitanim materijalom, strojem,
alatom, parametrima, oblikom i G-code previewem. Export koristi postojeći
`ProgramExportService` i `.nc` ekstenziju.

## Razlog odabranog rješenja

Pregled je izdvojen iz generatora, dok quick access vraća odabrani snapshot u
postojeći generator bez dupliciranja mapiranja u UI sloju.

## Arhitektonska povezanost

Controlleri koordiniraju scene i prikaz. `SavedJobService` dohvaća i pretvara
spremljeni tekst u `GCodeProgram`, repository mapira kompletan job iz H2 baze, a
export ostaje u zasebnom service sloju.

## Važne odluke i ograničenja

- Količina ostaje spremljena kao postojeća interna vrijednost Iteracije 1 i nije
  uvedena kao novi UI element.
- Fresh-context test koristi novi repository/service objekt nad istom H2 bazom.
- RichAuto A11 fizička kompatibilnost nije testirana.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit run: JdbcMachiningJobRepositoryIntegrationTest
```

### Stvarni rezultat

IDE build je uspješan. Oba testa u
`JdbcMachiningJobRepositoryIntegrationTest` uspješno su izvršena, uključujući
fresh-context round-trip s provjerom G-koda i parametara.

### Što nije testirano

JavaFX ekran nije vizualno ručno testiran. Puni Maven suite nije pokrenut, a
`.nc` export nije testiran kroz fizički CNC kontroler.

## Otvorena pitanja

- Vizualno provjeriti raspored liste i G-code previewa u JavaFX prozoru.

## Moguće poglavlje završnog rada

Persistence machining jobova i ponovno učitavanje spremljenog programa.

## Kandidati za isječke koda

### Kandidat: Quick access kroz svježi persistence kontekst

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/SavedProgramsController.java`  
**Klasa/metoda:** `SavedProgramsController#handleOpen`  
**Zašto je važan:** Pokazuje da se odabrani job ponovno dohvaća prije učitavanja u
  generator.  
**Moguće poglavlje:** JavaFX workflow i persistence

```java
navigation.showMainWithJob(savedJobService.loadById(selected.getMachiningJobId()));
```

**Ideja opisa u radu:** UI ne ovisi o samo trenutnom objektu liste, nego koristi
novi service dohvat.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
