# 7.2 — Transakcijski MachiningJob repository

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Implementirati spremanje i potpuno ponovno učitavanje `MachiningJob` agregata, uz jednu JDBC transakciju za snapshot podatke koje job posjeduje.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/persistence/repository/MachiningJobRepository.java` — dodan minimalni ugovor `save`, `findById` i `findAll`.
- `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java` — implementirano transakcijsko spremanje i potpuno JDBC mapiranje agregata.
- Tri postojeće snapshot JDBC implementacije i `JdbcRepositorySupport` — omogućeni inserti na istoj otvorenoj vezi.
- `src/test/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepositoryIntegrationTest.java` — dodani rollback i fresh-context round-trip testovi.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — evidentirana odluka i stvarni rezultat.

## Stvarna implementacija

`JdbcMachiningJobRepository.save` provjerava da je job nov, da su `MaterialSheet`, `MachiningParameters` i `Shape` novi snapshoti te da `MaterialType`, `CncMachine`, `Tool` i `User` imaju postojeće ID-eve. Repository zatim otvara jednu vezu, isključuje auto-commit, sprema tri snapshota i job s `g_code`, učitava cijeli agregat na istoj vezi i poziva commit.

Ako bilo koji korak ne uspije prije commita, repository poziva rollback i ponovno baca izvornu pogrešku. `findById` i `findAll` jednim persistence upitom rekonstruiraju autora s ulogom, stroj, alat s njegovim strojem, ploču s vrstom materijala, parametre, oblik, quantity, G-kod i timestampove.

## Razlog odabranog rješenja

Snapshot zapisi pripadaju točno jednom jobu i ne smiju ostati u bazi bez vlasnika. Prosljeđivanje otvorene veze package-private snapshot insert metodama omogućuje atomarnost bez izlaganja `Connection` objekta service ili UI sloju. Reference se koriste samo preko postojećih primarnih ključeva i ne dupliciraju se.

## Arhitektonska povezanost

Transakcija, SQL, CLOB čitanje i sastavljanje domenskog agregata ostaju u persistence sloju. Javni repository ugovor ne izlaže JDBC vezu. `Main`, controlleri, service i domenski modeli nisu mijenjani.

## Važne odluke i ograničenja

- Transakcija počinje nakon dobivanja veze pozivom `setAutoCommit(false)` i završava commitom tek nakon uspješnog ponovnog učitavanja.
- Rollback uklanja sva tri nova snapshota i job insert; postojeće reference ne mijenja.
- `update` nije dodan jer potvrđeni quick-access workflow samo učitava spremljene vrijednosti u formu.
- Provjera pripada li alat odabranom stroju ostaje buduća service/validation odgovornost.
- Identity brojač baze može preskočiti vrijednosti nakon rollbacka; to ne predstavlja djelomično spremljen zapis.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dtest=JdbcMachiningJobRepositoryIntegrationTest test
mvn test
```

Naredbe su izvršene IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani test završio je s `BUILD SUCCESS`: 2 testa, 0 failurea, 0 errora i 0 preskočenih testova. Cijeli suite nakon toga završio je s `BUILD SUCCESS`: 9 testova, 0 failurea, 0 errora i 0 preskočenih testova.

Rollback test namjerno koristi `quantity = 0` kako bi završni `MACHINING_JOB` insert prekršio stvarni CHECK constraint. Nakon pogreške potvrđeno je da su `MACHINING_JOB`, `MATERIAL_SHEET`, `MACHINING_PARAMETERS` i `SHAPE` prazni, dok unaprijed spremljene reference ostaju prisutne. Fresh-context test sprema i novom repository instancom učitava single-element job s `quantity = 1` i punim G-kodom.

### Što nije testirano

Nisu implementirani ni testirani service/UI save-reopen tok, quick-access forma, autentikacija, update/delete, file-based aplikacijska baza ni konkurentno spremanje. G-kod je testni tekst; nije provedeno fizičko testiranje na ZK-1325 / RichAuto A11 stroju.

## Otvorena pitanja

- Poslovna provjera da odabrani alat pripada odabranom stroju ostaje za validation/service milestone.
- Stvarni save/reopen/quick-access UI tok ostaje za milestone 10.4.

## Moguće poglavlje završnog rada

Transakcijsko spremanje agregata i rekonstrukcija domenskog modela ručnim JDBC mapiranjem.

## Kandidati za isječke koda

### Kandidat: Transakcijsko spremanje agregata

**Datoteka:** `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java`  
**Klasa/metoda:** `JdbcMachiningJobRepository#save`  
**Zašto je važan:** Prikazuje jednu transakcijsku granicu za tri snapshot zapisa i njihov vlasnički job te eksplicitni commit/rollback.  
**Moguće poglavlje:** Persistence sloj i transakcije

```java
connection.setAutoCommit(false);
try {
    long materialSheetId = materialSheetRepository.insertOnOpenConnection(
            connection, job.getMaterialSheet());
    long machiningParametersId = machiningParametersRepository.insertOnOpenConnection(
            connection, job.getMachiningParameters());
    long shapeId = shapeRepository.insertOnOpenConnection(connection, job.getShape());
    long jobId = insertJob(connection, job, materialSheetId, machiningParametersId, shapeId);
    MachiningJob savedJob = findById(connection, jobId)
            .orElseThrow(() -> new SQLException("Saved machining job was not found"));
    connection.commit();
    return savedJob;
} catch (SQLException | RuntimeException exception) {
    rollback(connection, exception);
    throw exception;
}
```

**Ideja opisa u radu:** Commit se izvodi tek kada su svi zapisi spremljeni i agregat se može ponovno rekonstruirati; svaka ranija pogreška vraća bazu u prethodno stanje.

### Kandidat: Dokaz rollbacka stvarnim constraintom

**Datoteka:** `src/test/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepositoryIntegrationTest.java`  
**Klasa/metoda:** `JdbcMachiningJobRepositoryIntegrationTest#rollsBackAllOwnedSnapshotsWhenJobInsertFails`  
**Zašto je važan:** Runtime potvrđuje da neuspjeh završnog inserta ne ostavlja orphan snapshot retke.  
**Moguće poglavlje:** Integration testiranje integriteta baze

```java
assertThrows(SQLException.class, () -> repository.save(newJob(references, 0)));
assertEquals(0, countRows("MATERIAL_SHEET"));
assertEquals(0, countRows("MACHINING_PARAMETERS"));
assertEquals(0, countRows("SHAPE"));
```

**Ideja opisa u radu:** Test koristi stvarnu shemu i CHECK constraint kako bi izazvao pogrešku nakon snapshot inserta te izravno provjerava rezultat rollbacka.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
