# 12.3 — Idempotentni V1 bootstrap / seed

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO NI FIZIČKI TESTIRANO

## Cilj

Nakon inicijalizacije baze osigurati minimalne, potvrđene V1 referentne podatke bez izmišljanja korisnika, alata ili machining postavki.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java` — idempotentna koordinacija seeda.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` — pokreće bootstrap nakon inicijalizacije baze.
- `src/main/java/hr/lukabosnjak/persistence/repository/RoleRepository.java` i `persistence/jdbc/JdbcRoleRepository.java` — repository operacije potrebne bootstrapu.
- `src/main/resources/db/schema.sql` — više ne sadrži stari, parcijalni seed rola.
- testovi repositoryja i composition roota — usklađeni s potvrđenom rolom `OPERATOR`.
- `Dokumentacija/biljeske/00_odluke.md` i `00_indeks.md` — evidentirana odluka i korak.

## Stvarna implementacija

`V1BootstrapService` nakon svake aplikacijske inicijalizacije osigurava `ADMIN`, `ENGINEER` i `OPERATOR`, pet zapisa `TEST_MATERIAL_1` do `TEST_MATERIAL_5` te točno jedan bootstrap zapis stroja `ZK-1325` / `RichAuto A11`. Stroj ima poznate XY dimenzije 1250 × 2500 mm; manufacturer, Z hod, feed i RPM granice ostaju `NULL`.

Ponavljanje bootstrap poziva ne stvara duplikate. `APP_USER`, alati i poslovni/snapshot zapisi nisu seedani. Stara `USER` rola uklanja se samo kada nije povezana s korisnikom; povezana rola se ne mijenja automatski.

## Razlog odabranog rješenja

Bootstrap je odvojen od DDL-a kako bi schema sadržavala samo strukturu, a aplikacijski podaci imali jedan vidljiv i ponovljiv ulazni tok. `TEST_MATERIAL_*` nazivi izričito pokazuju da nisu potvrđeni fizički materijali.

## Arhitektonska povezanost

Composition root koordinira početak aplikacije. Service sloj određuje poslovni skup bootstrap podataka, repository sloj izvršava JDBC operacije, a JavaFX controlleri nisu mijenjani i ne sadrže SQL.

## Važne odluke i ograničenja

- Potvrđeni bootstrap ne uvodi dev korisnike prije stvarnog password hashera u Koraku 13.
- Ne uvodi se Tool zapis dok stvarni alat i njegovi atributi nisu potvrđeni.
- Bootstrap ne potvrđuje fizičku kompatibilnost ZK-1325 / RichAuto A11.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> -Dtest=V1BootstrapServiceIntegrationTest,DatabaseSchemaIntegrationTest,JdbcRepositoriesIntegrationTest,JdbcMachiningJobRepositoryIntegrationTest,ApplicationCompositionRootIntegrationTest test
```

### Stvarni rezultat

Ciljani paket: 12 testova, 0 failures, 0 errors, `BUILD SUCCESS`.

### Što nije testirano

Nije izvršen vizualni JavaFX start s produkcijskom file-based bazom. Nisu fizički testirani stroj, kontroler, alati ni machining parametri.

## Otvorena pitanja

- Ako neka buduća postojeća baza ima korisnika s legacy rolom `USER`, prije brisanja/migracije potrebno je potvrditi preslikavanje te role.
- Role prava, default rola registracije i password hashing pripadaju Koraku 13.

## Moguće poglavlje završnog rada

Idempotentna inicijalizacija referentnih podataka u desktop aplikaciji.

## Kandidati za isječke koda

### Kandidat: Idempotentno osiguravanje bootstrap podataka

**Datoteka:** `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java`  
**Klasa/metoda:** `V1BootstrapService#initialize`  
**Zašto je važan:** Pokazuje odvojenu koordinaciju strukture baze i minimalnih V1 podataka.  
**Moguće poglavlje:** Persistence infrastruktura i inicijalizacija aplikacije

```java
public void initialize() throws SQLException {
    removeUnusedLegacyUserRole();
    ensureRoles();
    ensureMaterialTypes();
    ensureZk1325Machine();
}
```

**Ideja opisa u radu:** Svaki poziv provjerava postojeće zapise prije umetanja, pa ponovno pokretanje aplikacije ne duplicira potvrđene bootstrap podatke.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
