# 6.3 — Automatska inicijalizacija file-based baze

**Datum:** 2026-08-25

**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Omogućiti da aplikacija pri prvom pokretanju sama izvrši postojeći `schema.sql` nad praznom file-based H2 bazom, bez ručnog rada u H2 Consoleu i bez brisanja podataka pri kasnijim pokretanjima.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java` — dodana eksplicitna provjera i inicijalizacija sheme.
- `src/main/java/hr/lukabosnjak/app/Main.java` — initializer se poziva u JavaFX `init()` fazi prije prikaza Stagea.
- `src/test/java/hr/lukabosnjak/config/DatabaseInitializerIntegrationTest.java` — dodan privremeni file-based integration test inicijalizacije i očuvanja podataka.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran milestone 6.3.
- `Dokumentacija/biljeske/06-03-database-initializer.md` — dokumentirana implementacija i stvarni status provjere.

## Stvarna implementacija

`DatabaseInitializer.initialize()` otvara aplikacijsku vezu preko `DatabaseConfig`, čita nazive tablica u `PUBLIC` shemi i izvršava `/db/schema.sql` samo kada baza nema nijednu korisničku tablicu. Ako svih devet V1 tablica već postoji, metoda završava bez promjene podataka. Ako postoji samo dio očekivane sheme, baca `SQLException` s popisom nedostajućih tablica umjesto da prikrije problem ili briše postojeće objekte.

`Main#init()` poziva initializer prije JavaFX `start()` faze. Integration test koristi JUnit privremeni direktorij, stvara novu file-based bazu, provjerava tablice, umeće testni zapis, ponovno pokreće initializer i provjerava da zapis nije izbrisan.

## Razlog odabranog rješenja

Eksplicitna Java klasa jasnije prikazuje first-run tok od JDBC URL opcija. Provjera cijelog skupa očekivanih tablica omogućuje siguran no-op na već inicijaliziranoj bazi, dok odbijanje djelomične sheme sprječava skriveno miješanje različitih verzija ili nastavak rada s neupotrebljivom bazom.

## Arhitektonska povezanost

Initializer pripada infrastrukturnom `config` paketu i koristi JDBC samo za otvaranje veze, metadata pregled i izvršavanje persistence resursa. `Main` ostaje composition-root ulazna točka i samo pokreće inicijalizaciju; SQL se ne nalazi u JavaFX controlleru ni domenskom sloju.

## Važne odluke i ograničenja

- Shema se izvršava samo nad praznom `PUBLIC` shemom.
- Potpuna V1 shema znači no-op; postojeći podaci ostaju netaknuti.
- Djelomična shema prekida startup jasnom iznimkom i ne pokreće automatski popravak ili brisanje.
- Dodatne buduće tablice dopuštene su ako svih devet V1 tablica postoji.
- Ovo nije migracijski framework i ne nadograđuje stariju verziju sheme.

## Build i testiranje

### Izvršene naredbe

```text
Get-ChildItem -Force -Name mvnw,mvnw.cmd,.mvn -ErrorAction SilentlyContinue
Get-Command mvn -ErrorAction SilentlyContinue
$manualOut = 'target/manual-initializer-check'
$schemaH2Jar = Join-Path $env:USERPROFILE '.m2\repository\com\h2database\h2\2.4.240\h2-2.4.240.jar'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\javac.exe' -cp $schemaH2Jar -d $manualOut src/main/java/hr/lukabosnjak/config/DatabaseConfig.java src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\javac.exe' -cp "$schemaH2Jar;$manualOut" -d $manualOut "$manualOut\InitializerHarness.java"
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin\java.exe' -cp "$schemaH2Jar;$manualOut;src/main/resources" hr.lukabosnjak.config.InitializerHarness
```

### Stvarni rezultat

Maven Wrapper i globalni Maven nisu dostupni. Novi infrastrukturni kod uspješno je kompiliran dostupnim IntelliJ Java runtimeom i lokalnim H2 2.4.240 JAR-om. Izolirani harness u ignoriranom `target/` direktoriju zatim je otvorio novu privremenu file-based bazu, pokrenuo initializer, umetnuo `MATERIAL_TYPE` zapis, ponovno pokrenuo initializer i potvrdio da zapis postoji. Naredba je završila s izlaznim kodom 0. JUnit integration test nije izvršen i nema tvrdnje da puni Maven build prolazi.

### Što nije testirano

Nisu izvršeni JUnit testovi ni puni Maven build. Nisu zasebno runtime testirani odbijanje djelomične sheme i startup poziv iz `Main#init()`. Nije pokrenuta stvarna aplikacijska baza pod `./data` i nije testiran JavaFX prozor.

## Otvorena pitanja

- Buduća promjena V1 sheme zahtijevat će zasebnu odluku o migracijama; ovaj initializer namjerno podržava samo praznu ili već potpunu trenutačnu shemu.

## Moguće poglavlje završnog rada

Automatska inicijalizacija lokalne baze podataka pri prvom pokretanju desktop aplikacije.

## Kandidati za isječke koda

### Kandidat: Sigurna odluka o first-run inicijalizaciji

**Datoteka:** `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java`

**Klasa/metoda:** `DatabaseInitializer#initialize(Connection)`

**Zašto je važan:** Prikazuje razliku između prazne, potpune i djelomično inicijalizirane baze bez destruktivnog ponašanja.

**Moguće poglavlje:** Inicijalizacija i integritet lokalne baze

```java
if (existingTables.containsAll(EXPECTED_TABLES)) {
    return;
}

if (!existingTables.isEmpty()) {
    throw incompleteSchemaException(existingTables);
}

executeSchema(connection);
```

**Ideja opisa u radu:** Initializer izvršava shemu samo na prvom pokretanju i odbija nejasno djelomično stanje umjesto da briše ili prepisuje postojeće podatke.

### Kandidat: Provjera očuvanja file-based podataka

**Datoteka:** `src/test/java/hr/lukabosnjak/config/DatabaseInitializerIntegrationTest.java`

**Klasa/metoda:** `DatabaseInitializerIntegrationTest#initializesNewFileDatabaseAndPreservesExistingData`

**Zašto je važan:** Testira stvarnu datotečnu bazu, ponovno pokretanje initializera i očuvanje zapisa.

**Moguće poglavlje:** Integration testiranje persistence infrastrukture

```java
DatabaseInitializer.initialize(jdbcUrl);
insertMaterialType(connection, "Test material");
DatabaseInitializer.initialize(jdbcUrl);
assertEquals(1, countMaterialTypes(connection, "Test material"));
```

**Ideja opisa u radu:** Test dokazuje da uobičajeno ponovno pokretanje ne rekreira shemu i ne briše korisničke podatke.

## Kandidat za sliku, dijagram ili tablicu

Nema novog vizualnog kandidata u ovom koraku.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
