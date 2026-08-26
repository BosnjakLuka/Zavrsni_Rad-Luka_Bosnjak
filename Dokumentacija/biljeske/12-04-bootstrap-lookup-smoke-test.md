# 12.4 — Database/UI lookup smoke test

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO NI FIZIČKI TESTIRANO

## Cilj

Potvrditi da nakon inicijalizacije i V1 bootstrapa application/service i repository slojevi mogu dohvatiti očekivane referentne podatke.

## Promijenjene datoteke

- `src/test/java/hr/lukabosnjak/service/V1BootstrapServiceIntegrationTest.java` — smoke test dohvaćanja rola, materijala i stroja.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran korak.

## Stvarna implementacija

Integration test pokreće bootstrap nad praznom in-memory H2 bazom, provjerava tri role preko repositoryja, pet materijala preko `MaterialReferenceDataServicea` i jedan stroj preko `ReferenceDataServicea`. Test također potvrđuje da nema Tool zapisa.

## Razlog odabranog rješenja

Smoke test potvrđuje stvarnu granicu potrebnu UI-u bez pokretanja JavaFX Stagea i bez miješanja SQL-a u controller.

## Arhitektonska povezanost

Repositoryji ostaju vlasnici JDBC dohvata, service sloj pruža podatke prikladne UI-u, a test ne ovisi o JavaFX runtimeu.

## Važne odluke i ograničenja

- Test ne očekuje `APP_USER` zapise prije Koraka 13.
- Test ne očekuje Tool zapis dok stvarni alat nije potvrđen.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> -Dtest=V1BootstrapServiceIntegrationTest,DatabaseSchemaIntegrationTest,JdbcRepositoriesIntegrationTest,JdbcMachiningJobRepositoryIntegrationTest,ApplicationCompositionRootIntegrationTest test
```

### Stvarni rezultat

`V1BootstrapServiceIntegrationTest` prošao je 3 testa, a cijeli ciljani paket 12 testova: 0 failures, 0 errors, `BUILD SUCCESS`.

### Što nije testirano

Nije pokrenut vizualni JavaFX smoke test s glavnom formom. Nije izvršen fizički CNC test.

## Otvorena pitanja

- Prava prikaza i izmjene referentnih podataka bit će ograničena tek nakon odluke i implementacije RBAC-a u Koraku 13.

## Moguće poglavlje završnog rada

Integracijsko testiranje granice service sloja i persistencea.

## Kandidati za isječke koda

### Kandidat: Smoke test servisnog dohvata

**Datoteka:** `src/test/java/hr/lukabosnjak/service/V1BootstrapServiceIntegrationTest.java`  
**Klasa/metoda:** `V1BootstrapServiceIntegrationTest#smokeTestLoadsSeededRolesMaterialsAndMachineThroughRepositoriesAndServices`  
**Zašto je važan:** Dokazuje da UI-ju potrebni podaci dolaze kroz postojeće granice slojeva.  
**Moguće poglavlje:** Strategija testiranja aplikacije

```java
assertEquals(5, new MaterialReferenceDataService(materialTypes).loadMaterialTypes().size());
List<CncMachine> loadedMachines = new ReferenceDataService(machines, tools).loadMachines();
assertEquals(1, loadedMachines.size());
```

**Ideja opisa u radu:** Test povezuje bootstrap, repository i service sloj bez SQL-a u JavaFX controlleru.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
