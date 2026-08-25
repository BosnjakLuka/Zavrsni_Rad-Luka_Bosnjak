# 5.1 — Minimalna H2/JDBC infrastruktura

**Datum:** 2026-08-25  
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Uvesti minimalnu embedded H2/JDBC infrastrukturu za budući persistence sloj, uz file-based aplikacijsku bazu, in-memory bazu za testove i jasno ograničen životni ciklus JDBC veze.

## Promijenjene datoteke

- `pom.xml` — dodana H2 dependency verzije 2.4.240.
- `.gitignore` — lokalni direktorij `data` isključen iz Git praćenja.
- `src/main/java/hr/lukabosnjak/config/DatabaseConfig.java` — centralna JDBC konfiguracija i factory za novu vezu.
- `src/test/java/hr/lukabosnjak/config/DatabaseConfigTest.java` — razvojni in-memory smoke test veze.
- `Dokumentacija/h2-console-development.md` — razvojna uputa za H2 Console.
- `Dokumentacija/biljeske/00_odluke.md` — evidentirana odluka o minimalnoj H2/JDBC infrastrukturi.
- `Dokumentacija/biljeske/00_indeks.md` — dodan ovaj razvojni korak.

## Stvarna implementacija

H2 2.4.240 dodan je kao Maven dependency. `DatabaseConfig` centralizira aplikacijski JDBC URL, korisnika i razvojnu praznu lozinku te pri svakom pozivu vraća novu vezu. Aplikacija koristi file-based URL `jdbc:h2:file:./data/cnc-optimizer`, dok smoke test prosljeđuje zaseban in-memory URL i zatvara vezu pomoću try-with-resources. Nisu dodani tablice, SQL, ORM, repositoryji ni globalno otvorena veza.

## Razlog odabranog rješenja

Ručni JDBC odgovara potvrđenoj arhitekturi i ostavlja životni ciklus veze vidljivim. Package-private overload omogućuje testiranje istog factoryja s in-memory bazom bez izlaganja aplikacijskih vjerodajnica ili zamjene file-based konfiguracije.

## Arhitektonska povezanost

`DatabaseConfig` pripada infrastrukturnom `config` paketu. Domenski i JavaFX kod nisu promijenjeni, a budući persistence kod mora sam zatvarati dobivene veze pomoću try-with-resources.

## Važne odluke i ograničenja

- Aplikacijska baza je embedded, file-based H2 pod relativnom putanjom `./data/cnc-optimizer`.
- Integracijski testovi koriste zasebne in-memory H2 baze.
- Relativna putanja ovisi o working directoryju procesa.
- H2 Console je isključivo razvojni alat.
- U ovom koraku nema sheme ni SQL-a.

## Build i testiranje

### Izvršene naredbe

```text
Get-ChildItem -Force -Name mvnw,mvnw.cmd,.mvn -ErrorAction SilentlyContinue
Get-Command mvn -ErrorAction SilentlyContinue
java -version
```

### Stvarni rezultat

Maven Wrapper nije pronađen, globalna naredba `mvn` nije dostupna, a ni naredba `java` nije dostupna u PATH-u izvršnog okruženja. Smoke test zato u ovom koraku nije izvršen i nema tvrdnje o prolasku builda.

### Što nije testirano

Nisu izvršeni Maven build ni `DatabaseConfigTest`. Nije ručno pokrenut H2 Console niti je otvorena file-based aplikacijska baza.

## Otvorena pitanja

- Potrebno je osigurati Maven Wrapper ili dostupnu Maven instalaciju prije izvršavanja automatiziranih testova.

## Moguće poglavlje završnog rada

Sloj za pristup podacima i konfiguracija embedded baze podataka.

## Kandidati za isječke koda

### Kandidat: Factory za kratkoživuću JDBC vezu

**Datoteka:** `src/main/java/hr/lukabosnjak/config/DatabaseConfig.java`  
**Klasa/metoda:** `DatabaseConfig#getConnection`  
**Zašto je važan:** Prikazuje centralizaciju konfiguracije i stvaranje nove veze bez globalnog JDBC stanja.  
**Moguće poglavlje:** Implementacija persistence infrastrukture

```java
public static Connection getConnection() throws SQLException {
    return getConnection(JDBC_URL);
}

static Connection getConnection(String jdbcUrl) throws SQLException {
    return DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
}
```

**Ideja opisa u radu:** Factory skriva detalje konfiguracije, ali vlasništvo nad vraćenom vezom ostavlja pozivatelju koji je mora zatvoriti.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
