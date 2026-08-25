# 1.7 — Domain entities package

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Preimenovati podpaket domenskih klasa iz `domain.model` u pregledniji `domain.entities`, uz zadržavanje svih ostalih potvrđenih package naziva.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MaterialType.java` — promijenjene su putanja i package deklaracija bez promjene javnog API-ja.
- `Dokumentacija/biljeske/00_odluke.md` — package konvencija usklađena je s nazivom `domain.entities`.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/01-05-material-type.md` i `01-06-slojevita-package-konvencija.md` — aktualne reference usklađene su s novim nazivom.
- `Dokumentacija/biljeske/01-07-domain-entities-package.md` — dokumentirana je promjena i provjera.

## Stvarna implementacija

`MaterialType` sada ima puni naziv `hr.lukabosnjak.domain.entities.MaterialType`. Polja, konstruktor, getteri i setteri nisu mijenjani. Drugi paketi ili klase nisu dodani.

## Razlog odabranog rješenja

Naziv `entities` jasnije označava klase koje predstavljaju domenske objekte s vlastitim identitetom i životnim ciklusom. U ovom projektu naziv nema ORM značenje jer se koristi ručni JDBC/H2 pristup.

## Arhitektonska povezanost

`domain.entities` ostaje dio domenskog sloja i ne smije ovisiti o JavaFX-u, JDBC-u, H2-u ili persistence implementaciji. Ostala dogovorena struktura ostaje nepromijenjena.

## Važne odluke i ograničenja

- Koristi se podpaket `domain.entities`, ne vršni paket `entities`.
- Klase u tom paketu nisu JPA entiteti i ne dobivaju ORM anotacije.
- DTO-i ostaju predviđeni za `service.dto`, a enumeracije za `domain.enums`.
- Nisu dodani prazni paketi, novi modeli, DTO-i, SQL ni UI.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
```

### Stvarni rezultat

`mvn clean test` izvršen je na Oracle OpenJDK-u 26.0.2.1, ponovno je kompilirao oba Java izvora s Java releaseom 26 i završio s `BUILD SUCCESS`. Projekt nema testnih klasa.

### Što nije testirano

Nema novih unit testova jer se poslovno ponašanje klase nije promijenilo. JDBC mapiranje, H2 persistence i buduće domenske klase nisu implementirani niti testirani.

## Otvorena pitanja

- Konkretni budući entiteti uvodit će se samo kroz zasebno potvrđene zadatke.

## Moguće poglavlje završnog rada

Arhitektura aplikacije i organizacija domenskog sloja.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — promijenjena je samo package deklaracija postojeće klase.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
