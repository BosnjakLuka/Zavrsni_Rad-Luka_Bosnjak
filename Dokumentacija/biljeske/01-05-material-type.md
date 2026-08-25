# 1.5 — Domenska klasa MaterialType

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Dodati prvi domenski model za vrstu materijala s isključivo potvrđenim podacima i bez vezivanja domenskog sloja uz JavaFX, JDBC ili ORM.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MaterialType.java` — dodana domenska klasa s potvrđenim poljima i jednostavnim pristupom za JDBC mapiranje; putanja je usklađena s naknadno potvrđenom package konvencijom.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je odluka o strukturi modela i načinu mapiranja.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/01-05-material-type.md` — dokumentirana je implementacija i stvarna provjera.

## Stvarna implementacija

Klasa `MaterialType` sadrži `materialTypeId`, `name`, `description`, `createdAt`, `updatedAt` i `deletedAt`. Ima jedan konstruktor sa svim poljima te standardne gettere i settere. `description` i `deletedAt` mogu biti `null`; `Long` omogućuje i privremeno odsutan ID prije spremanja u bazu.

## Razlog odabranog rješenja

Obična mutable Java klasa jednostavna je za ručno JDBC mapiranje: repository može konstruirati objekt iz retka rezultata i naknadno postaviti ID ili vremenske vrijednosti vraćene iz baze. Nema ORM anotacija jer projekt koristi JDBC/H2, a ne ORM.

## Arhitektonska povezanost

`MaterialType` pripada paketu `domain.entities` i uvozi samo `java.time.LocalDateTime`. Ne ovisi o UI-ju, JavaFX-u, JDBC-u, H2-u ni persistence sloju.

## Važne odluke i ograničenja

- `materialTypeId` je objektni `Long` kako bi mogao biti `null` prije dodjele u bazi.
- `deletedAt == null` označava da zapis nije soft-obrisan; vrijednost predstavlja vrijeme logičkog brisanja.
- Klasa ne provodi validaciju null vrijednosti; validacija pripada budućem `validation` sloju.
- Nisu dodani no-arg ili convenience konstruktori, JPA anotacije, `equals`, `hashCode`, `toString`, repository ni SQL.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> test
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
```

### Stvarni rezultat

- Prvi pokušaj u sandboxu pokrenuo je kompilaciju dviju izvornih datoteka, ali završio je s `BUILD FAILURE` jer javac nije mogao zatvoriti resurs JavaFX JAR datoteke.
- Ponovljeni `mvn test` izvan tog ograničenja završio je s `BUILD SUCCESS`, ali nije ponovno kompilirao već nastale `.class` datoteke.
- Završni `mvn clean test` na Oracle OpenJDK-u 26.0.2.1 obrisao je generirani `target`, ponovno kompilirao oba izvora s Java releaseom 26 i završio s `BUILD SUCCESS`.
- Projekt nema testnih klasa; provjereni rezultat potvrđuje čistu kompilaciju `MaterialType` i postojećeg `Main` koda, ne poslovno ponašanje.

### Što nije testirano

Nisu dodani unit testovi jer klasa nema izračune, grananja ni validacijsku logiku; testiranje trivijalnih gettera i settera ne bi dokazivalo poslovno ponašanje. JDBC mapiranje i H2 persistence nisu implementirani niti testirani.

## Otvorena pitanja

- Strategija generiranja ID-a i pravila postavljanja vremenskih oznaka definirat će se uz persistence sloj.
- Validacijska pravila za naziv i vremenske vrijednosti još nisu potvrđena.

## Moguće poglavlje završnog rada

Domenski model i odvajanje domene od persistence tehnologije.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — klasa trenutačno sadrži samo polja, konstruktor i standardne pristupne metode.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
