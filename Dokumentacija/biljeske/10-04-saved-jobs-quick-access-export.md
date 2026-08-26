# 10.4 — Saved jobs, quick access i `.nc` export

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Omogućiti pregled, otvaranje, ponovno učitavanje i export postojećih spremljenih single-element jobova, bez SQL-a i file-writinga u JavaFX controlleru. Stvarni Save ostaje blokiran do autentikacijske sesije autora.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/SavedJobService.java`, `SavedJobAccessException.java` — read-only dohvat jobova i pretvorba spremljenog G-codea.
- `src/main/java/hr/lukabosnjak/service/ProgramExportService.java` — service granica nad postojećim `NcExportService`om.
- `src/main/java/hr/lukabosnjak/service/MaterialReferenceDataService.java` — učitavanje postojećih vrsta materijala.
- `src/main/java/hr/lukabosnjak/app/Main.java`, `ui/controller/MainFormController.java` i FXML view — composition root, quick access, FileChooser i ponovno učitavanje forme.
- `src/test/java/hr/lukabosnjak/service/SavedJobServiceTest.java` i `ProgramExportServiceTest.java` — ciljani testovi.
- `Dokumentacija/biljeske/00_indeks.md` i ova bilješka — razvojna evidencija.

## Stvarna implementacija

Saved Programs učitava postojeće jobove kroz `SavedJobService`. Open ponovno dohvaća odabrani job, puni naziv, vrstu materijala, stroj, alat, ploču, machining parametre i semantička polja oblika te prikazuje spremljeni G-code bez regeneriranja.

Export koristi JavaFX `FileChooser` samo za izbor odredišta. Controller odabrani `Path` predaje `ProgramExportService`u, koji delegira postojeći `NcExportService`; controller ne koristi `Files` ni ne upisuje sadržaj datoteke. Dodani su izbor vrste materijala i polje naziva programa kao priprema za budući save request.

Save nije implementiran: poruka izričito navodi da je potrebna prijavljena sesija autora. Time se ne uvodi hardkodirani, tehnički ili proizvoljno odabrani korisnik.

## Razlog odabranog rješenja

Postojeći `MachiningJobRepository` već pruža kompletno ponovno učitavanje spremljenog agregata, pa service samo uspostavlja UI granicu i pretvara spremljeni G-code u postojeći `GCodeProgram`. `ProgramExportService` čuva JavaFX controller izvan file-writing detalja, a ponovno koristi već testirani export ugovor.

## Arhitektonska povezanost

Controller koordinira view, servise i FileChooser. `SavedJobService` posjeduje repository pozive, a `ProgramExportService` posjeduje poziv G-code export servisu. JDBC SELECT/INSERT i zapis datoteke nisu u controlleru. Domain, geometry, layout i generator nisu izmijenjeni.

## Važne odluke i ograničenja

- Save zahtijeva `createdBy` iz buduće prijavljene sesije; ovaj milestone ga ne zaobilazi.
- `quantity` ostaje spremljena vrijednost `1` za single-element tok; nema quantity UI-a, layouta, capacityja, requiredSheetsa ni batch G-codea.
- Export ne znači fizičku potvrdu RichAuto A11 kompatibilnosti.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni Maven repository> -Dtest=SavedJobServiceTest,ProgramExportServiceTest test
```

### Stvarni rezultat

Maven je pokrenut s OpenJDK-om 26.0.2 i započeo kompilaciju 67 produkcijskih datoteka s `release 26`, ali je završio s `BUILD FAILURE` prije testne faze na postojećoj ovisnosti `h2-2.4.240.jar` (`Fatal error compiling ... h2-2.4.240.jar`). Novi testovi nisu izvršeni.

### Što nije testirano

Nisu izvršeni unit testovi ni JavaFX runtime tok s FileChooserom. Nije testiran stvarni `.nc` export na ciljnom stroju ni Save nakon buduće autentikacije.

## Otvorena pitanja

- Prije ponovnog Maven testa treba riješiti postojeći problem pristupa/kompilacije H2 JAR ovisnosti.
- Stvarni Save workflow čeka autentikacijski/session milestone koji daje dopuštenog autora posla.

## Moguće poglavlje završnog rada

Persistence workflow i ponovno korištenje spremljenih CNC programa.

## Kandidati za isječke koda

### Kandidat: Quick access ponovno učitavanje

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`
**Klasa/metoda:** `MainFormController#populateForm`
**Zašto je važan:** Prikazuje kako se spremljeni agregat vraća u semantička UI polja bez SQL-a, geometrije ili regeneriranja G-codea.
**Moguće poglavlje:** Korisničko sučelje i ponovno otvaranje posla

```java
machineComboBox.setValue(job.getCncMachine());
loadToolsForSelectedMachine();
toolComboBox.setValue(job.getTool());
...
displayedProgram = savedJobService.gCodeProgramOf(job);
gCodePreview.setText(displayedProgram.text());
```

**Ideja opisa u radu:** Controller obnavlja stanje prikaza iz već potpuno učitanog joba, dok service posjeduje dohvat i pretvorbu G-codea.

### Kandidat: Export service granica

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramExportService.java`
**Klasa/metoda:** `ProgramExportService#export`
**Zašto je važan:** Pokazuje da UI ne izvodi zapis datoteke, nego predaje postojeći program specijaliziranom G-code servisu.
**Moguće poglavlje:** Izvoz CNC programa

```java
public void export(GCodeProgram program, Path destination) throws IOException {
    ncExportService.export(program, destination);
}
```

**Ideja opisa u radu:** Odabir odredišta ostaje UI odgovornost, a format i write semantika pripadaju G-code/export sloju.

## Kandidat za sliku, dijagram ili tablicu

Nema — vizualna snimka quick-access toka nije stvarno izrađena ni potvrđena.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
