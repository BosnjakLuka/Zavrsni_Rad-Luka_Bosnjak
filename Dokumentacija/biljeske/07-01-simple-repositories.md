# 7.1 — Jednostavni JDBC repositoryji

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Definirati repository konvenciju i implementirati jednostavne JDBC repositoryje za V1 domenske objekte, bez spremanja cijelog `MachiningJob` agregata i bez automatskog punog CRUD-a.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/persistence/repository/` — dodano osam specifičnih repository ugovora.
- `src/main/java/hr/lukabosnjak/persistence/jdbc/` — dodane JDBC implementacije, provider veza i zajednička insert podrška.
- `src/test/java/hr/lukabosnjak/persistence/jdbc/JdbcRepositoriesIntegrationTest.java` — dodana četiri H2 integration scenarija.
- `Dokumentacija/biljeske/00_odluke.md` — zapisana repository konvencija.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran milestone 7.1.
- `Dokumentacija/biljeske/07-01-simple-repositories.md` — dokumentirana implementacija i provjera.

## Stvarna implementacija

Dodani su ugovori i JDBC implementacije za `MaterialType`, `CncMachine`, `Tool`, `MaterialSheet`, `MachiningParameters`, `Shape`, `Role` i `User`. Svaki ugovor sadrži samo odobrene operacije. `ToolRepository` uključuje `findAllByMachineId` i `findByMachineIdAndToolNumber`, dok minimalni auth opseg čine `RoleRepository.findByName`, `UserRepository.save` i `UserRepository.findByUsername`.

`save` je insert-only: odbija entitet koji već ima ID, izvršava parametrizirani insert, čita generirani ključ te vraća zapis ponovno mapiran iz baze. JDBC resursi zatvaraju se try-with-resources konstrukcijom. `Tool`, `MaterialSheet` i `User` učitavaju povezane domenske objekte SQL joinom; `Shape` čuva nullable dimenzije i mapira postojeće enumove.

## Razlog odabranog rješenja

Specifična sučelja izbjegavaju operacije koje trenutačni workflow ne koristi. `ConnectionProvider` omogućuje istim implementacijama korištenje aplikacijske veze i izolirane in-memory H2 baze u testovima bez unošenja JDBC-a u domenske klase. Ponovno čitanje nakon inserta vraća stvarne identity i timestamp vrijednosti baze.

## Arhitektonska povezanost

Repository ugovori i sav SQL ostaju unutar persistence sloja. Domenski entiteti ne ovise o JDBC-u, a controlleri i `Main` nisu mijenjani. `MachiningJob` i njegova buduća transakcija nisu uključeni.

## Važne odluke i ograničenja

- Ne postoji generički `CrudRepository`; metode se dodaju samo prema workflowu.
- `save` ne obavlja update i ne mutira ulazni entitet, nego vraća spremljeni zapis.
- Repository metode eksplicitno prijavljuju `SQLException`; zaseban exception model nije uveden bez potrebe.
- `Tool.type` ostaje postojeći `String`; nisu izmišljene `ToolType` vrijednosti.
- Auth servis, hashiranje, session, first-run tok i autorizacija nisu implementirani.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dtest=JdbcRepositoriesIntegrationTest test
mvn test
```

Uspješne naredbe izvršene su IntelliJ bundled Mavenom uz projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani integration test završio je s `BUILD SUCCESS`: 4 testa, 0 failurea, 0 errora i 0 preskočenih testova. Nakon završne izmjene pokrenut je cijeli suite: 7 testova, 0 failurea, 0 errora, 0 preskočenih testova i `BUILD SUCCESS`.

Prije uspješnog pokretanja dva pokušaja nisu došla do testova: prvi zbog nepostavljenog `JAVA_HOME`, a drugi zato što IntelliJ JBR 25 ne podržava Maven `release 26`. Provjera je zatim izvršena potvrđenim OpenJDK 26 SDK-om.

### Što nije testirano

Repositoryji nisu povezani sa service ili JavaFX slojem, file-based aplikacijskom bazom ni budućim transakcijskim spremanjem `MachiningJob` agregata. Autentikacija i autorizacija nisu testirane. Nije provedeno fizičko testiranje na CNC stroju i ovaj milestone ne donosi tvrdnju o RichAuto kompatibilnosti.

## Otvorena pitanja

- Transakcijska granica i spremanje snapshot zapisa pripadaju milestoneu 7.2.
- Strategija hashiranja lozinki i first-run administratorski tok ostaju za zaseban auth milestone.

## Moguće poglavlje završnog rada

Persistence sloj, JDBC mapiranje domenskog modela i provjera relacijskih ograničenja.

## Kandidati za isječke koda

### Kandidat: Dohvat alata s pripadajućim strojem

**Datoteka:** `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcToolRepository.java`  
**Klasa/metoda:** `JdbcToolRepository#findAllByMachineId`  
**Zašto je važan:** Prikazuje specifični repository upit, parametrizirani SQL i mapiranje relacije `CncMachine 1:N Tool`.  
**Moguće poglavlje:** Implementacija persistence sloja

```java
statement.setLong(1, cncMachineId);
try (ResultSet resultSet = statement.executeQuery()) {
    while (resultSet.next()) {
        tools.add(map(resultSet));
    }
}
```

**Ideja opisa u radu:** Repository dohvaća samo alate odabranog stroja i iz rezultata konstruira alate s pripadajućim domenskim objektom stroja.

### Kandidat: Provjera složene jedinstvenosti broja alata

**Datoteka:** `src/test/java/hr/lukabosnjak/persistence/jdbc/JdbcRepositoriesIntegrationTest.java`  
**Klasa/metoda:** `JdbcRepositoriesIntegrationTest#enforcesToolNumberUniquenessPerMachineAndSupportsMachineQueries`  
**Zašto je važan:** Dokazuje stvarno ponašanje složenog DB ograničenja i dopuštenu ponovnu uporabu broja na drugom stroju.  
**Moguće poglavlje:** Integritet podataka i integration testiranje

```java
assertThrows(SQLException.class,
        () -> tools.save(tool(firstMachine, 1, "Duplicate tool")));
Tool otherMachineTool = tools.save(tool(secondMachine, 1, "Other machine tool"));
```

**Ideja opisa u radu:** Negativni i pozitivni slučaj zajedno potvrđuju da je `tool_number` jedinstven unutar jednog stroja, a ne globalno.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
