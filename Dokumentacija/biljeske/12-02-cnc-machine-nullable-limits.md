# 12.2 — Nullable tehničke granice CNC stroja

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Omogućiti spremanje stvarnog stroja ZK-1325 / RichAuto A11 s potvrđenim radnim područjem X=1250 mm i Y=2500 mm, bez upisivanja nule ili izmišljene vrijednosti za nepotvrđeni Z hod, maksimalni posmak i raspon brzine vretena.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/CncMachine.java` — četiri nepotvrđene tehničke granice promijenjene su iz `double` u nullable `Double`.
- `src/main/resources/db/schema.sql` i `db/migration/12-02-cnc-machine-nullable-limits.sql` — nova shema i postojeće baze dopuštaju SQL `NULL` za iste četiri granice.
- `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java` — nakon provjere sheme izvršava malu idempotentnu migraciju.
- `src/main/java/hr/lukabosnjak/persistence/jdbc/` — machine, tool i machining-job mapping razlikuju SQL `NULL` od brojčane nule.
- `src/main/java/hr/lukabosnjak/validation/` — poznate granice ostaju obvezno valjane i provjerene, a nepoznate ne blokiraju posao.
- `src/main/java/hr/lukabosnjak/ui/controller/` i `src/main/resources/hr/lukabosnjak/ui/view/cnc-machine-form.fxml` — nepotvrđene granice mogu se ostaviti prazne.
- `src/test/java/hr/lukabosnjak/` — prošireni schema, migration, JDBC, validation, parser i FXML integration/unit testovi.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — evidentirana odluka i rezultat koraka.

## Stvarna implementacija

`workAreaX` i `workAreaY` ostaju obvezni primitive `double` i SQL `NOT NULL`. `workAreaZ`, `maxFeedRate`, `minSpindleSpeed` i `maxSpindleSpeed` sada su `Double` te odgovarajući stupci dopuštaju SQL `NULL`. `manufacturer` je već bio nullable, dok `name` i `controller` ostaju obvezni. Model se ne puni približnom Z vrijednošću od 80 mm jer ona nije potvrđena mjerenjem ili dokumentacijom.

Nova SQL migracija samo uklanja `NOT NULL` ograničenja s četiri stupca. H2 prihvaća njezino ponovno izvršavanje, pa `DatabaseInitializer` primjenjuje migraciju i na novu i na već postojeću kompletnu shemu bez uvođenja općeg migration frameworka. JDBC insert koristi tipizirani nullable `DOUBLE`, a sva tri mjesta koja rekonstruiraju `CncMachine` koriste nullable `ResultSet#getObject`.

Obrazac stroja zahtijeva X i Y, dok se Z, max feed i obje RPM granice mogu ostaviti praznima. Prazno polje postaje `null`; unesena vrijednost mora biti konačan broj, a validation sloj zatim zahtijeva da bude pozitivna. `MachiningJobValidator` provjerava max feed te donju i gornju RPM granicu samo kada pojedina granica postoji.

## Razlog odabranog rješenja

`null` izravno predstavlja „nepoznato”, dok bi `0` izgledao kao stvarna tehnička vrijednost i istodobno kvario postojeće provjere. Nullable su samo podaci koji za konkretni stroj još nisu potvrđeni. X/Y ostaju strogo zadani jer su potvrđeni i potrebni za postojeću single-shape fit provjeru.

Jedna mala SQL migracija dovoljna je za postojeću H2 bazu i ne uvodi novi sloj ili biblioteku. Uvjetne provjere zadržavaju sigurnosnu vrijednost poznatih granica bez pretvaranja nepoznatog podatka u proizvoljan limit.

## Arhitektonska povezanost

Domenski model predstavlja poznato/nepoznato bez JDBC ili JavaFX ovisnosti. SQL i nullable mapiranje ostaju u persistence/config sloju. Ponovljiva pravila valjanosti ostaju u validation sloju, a controller samo pretvara prazna UI polja u `null`. Geometry i G-code slojevi nisu mijenjani.

## Važne odluke i ograničenja

- Potvrđeni su samo ZK-1325 / RichAuto A11, X=1250 mm i Y=2500 mm; ovaj korak ne stvara niti seeda zapis stroja.
- Približnih 80 mm nije spremljeno niti korišteno kao strogi safety limit.
- Poznata opcionalna granica mora biti pozitivna i konačna; min RPM ne smije biti veći od max RPM kada su obje vrijednosti poznate.
- Promjena ne potvrđuje fizičke granice stroja, RichAuto ponašanje, work zero ni sigurnost rezanja.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ-Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> -Dtest=DatabaseSchemaIntegrationTest,DatabaseInitializerIntegrationTest,JdbcRepositoriesIntegrationTest,JdbcMachiningJobRepositoryIntegrationTest,ReferenceDataValidatorTest,MachiningJobValidatorTest,NumericInputParserTest,FxmlControllerContractTest test
<IntelliJ-Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> test
```

### Stvarni rezultat

Ciljani paket: 32 testa, 0 failures, 0 errors, `BUILD SUCCESS`. Puni suite: 122 testa, 0 failures, 0 errors, `BUILD SUCCESS`. Test migracije koristio je privremenu file-based H2 bazu, simulirao staru `NOT NULL` shemu, sačuvao postojeći zapis stroja i potvrdio nullable stupce nakon ponovne inicijalizacije.

Prvi pokušaj kompilacije u sandboxu završio je poznatim `Cannot close compiler resources` problemom nad lokalnim H2 JAR-om. Isti kod i testovi zatim su uspješno izvršeni izvan sandboxa s OpenJDK-om 26.0.2.

### Što nije testirano

Nije izvršen vizualni JavaFX unos stroja. Produkcijska `data/cnc-optimizer.mv.db` nije ručno migrirana tijekom testiranja; migracija će se primijeniti pri sljedećoj normalnoj inicijalizaciji aplikacije. Nije fizički potvrđen Z hod, feed/RPM raspon ni ponašanje ZK-1325 / RichAuto A11 stroja.

## Otvorena pitanja

- Z hod od približno 80 mm ostaje nepotvrđena terenska informacija do mjerenja ili vjerodostojne dokumentacije.
- Stvarni stroj pripada sljedećem idempotentnom bootstrap/seed koraku; ovaj korak namjerno ga ne umeće.

## Moguće poglavlje završnog rada

Modeliranje nepotpunih tehničkih podataka i evolucija H2 sheme.

## Kandidati za isječke koda

### Kandidat: Uvjetna provjera poznatih granica stroja

**Datoteka:** `src/main/java/hr/lukabosnjak/validation/MachiningJobValidator.java`
**Klasa/metoda:** `MachiningJobValidator#validate`
**Zašto je važan:** Pokazuje kako nepoznata vrijednost ne blokira posao, a poznata granica ostaje provediva.
**Moguće poglavlje:** Validacija domenskih i tehničkih ograničenja

```java
Double maxFeedRate = machine.getMaxFeedRate();
if (maxFeedRate != null && job.getMachiningParameters().getFeedRate() > maxFeedRate) {
    throw new ValidationException(
            "Brzina posmaka ne smije biti veća od maksimalne brzine posmaka odabranog stroja.");
}
```

**Ideja opisa u radu:** `null` označava da granica nije poznata, dok svaka spremljena granica i dalje aktivira provjeru.

### Kandidat: Idempotentna korektivna migracija postojeće sheme

**Datoteka:** `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java`
**Klasa/metoda:** `DatabaseInitializer#initialize`
**Zašto je važan:** Prikazuje da se korekcija primjenjuje i na novu i na postojeću kompletnu bazu bez brisanja podataka.
**Moguće poglavlje:** Persistence infrastruktura i evolucija sheme

```java
if (existingTables.isEmpty()) {
    executeScript(connection, SCHEMA_RESOURCE);
}
// provjera kompletne sheme
executeScript(connection, CNC_MACHINE_NULLABLE_LIMITS_MIGRATION);
```

**Ideja opisa u radu:** Nova baza dobiva ispravnu shemu odmah, a postojeća baza prolazi istu sigurnu korekciju pri inicijalizaciji.

## Kandidat za sliku, dijagram ili tablicu

Nema — nije izrađena nova potvrđena snimka ni dijagram.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
