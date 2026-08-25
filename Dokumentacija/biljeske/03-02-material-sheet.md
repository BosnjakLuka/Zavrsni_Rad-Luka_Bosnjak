# 3.2 — Domenska klasa MaterialSheet

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Dodati domenski entitet ploče materijala s objektnom vezom prema `MaterialType` i minimalnom invarijantom valjanih dimenzija u milimetrima.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MaterialSheet.java` — dodan je entitet ploče i provjera dimenzija.
- `src/test/java/hr/lukabosnjak/domain/entities/MaterialSheetTest.java` — dodani su testovi stvarne domenske logike.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježene su odluke o odnosu i invarijanti.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/03-02-material-sheet.md` — dokumentirana je implementacija i provjera.

## Stvarna implementacija

`MaterialSheet` sadrži ID, objektni odnos prema `MaterialType`, širinu, visinu, debljinu te vremenske oznake stvaranja i ažuriranja. Puni konstruktor prikladan je ručnom JDBC mapiranju. Konstruktor i setteri prihvaćaju samo konačne dimenzije veće od nule.

## Razlog odabranog rješenja

Objekt `MaterialType` jasnije predstavlja domenski odnos od samog stranog ključa. Persistence sloj će kasnije biti odgovoran za pretvaranje tog odnosa u ID baze. Lokalna provjera dimenzija čuva valjano stanje neovisno o budućem UI-ju.

## Arhitektonska povezanost

Entitet uvozi samo `java.time.LocalDateTime` i ovisi o drugom domenskom entitetu. Ne sadrži JavaFX, JDBC, H2, SQL ni ORM detalje. UI validacija i persistence mapiranje ostaju zasebne odgovornosti.

## Važne odluke i ograničenja

- Sve tri dimenzije izražene su u milimetrima i moraju biti konačne vrijednosti veće od nule.
- Nevaljana dimenzija uzrokuje standardnu `IllegalArgumentException`; nije uveden paket prilagođenih iznimki.
- Nullability za `materialType`, ID i vremenske oznake nije dodatno zaključana ovim korakom.
- Nisu dodani repository, SQL, DTO, UI ni drugi entiteti.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
```

### Stvarni rezultat

`mvn clean test` izvršen je na Oracle OpenJDK-u 26.0.2.1. Maven je ponovno kompilirao tri glavna izvora i jednu testnu klasu s Java releaseom 26. Izvršeno je 8 parametriziranih testnih slučajeva: 8 je prošlo, bez neuspjeha, pogrešaka ili preskočenih testova. Build je završio s `BUILD SUCCESS`.

### Što nije testirano

JDBC mapiranje, H2 ograničenja, UI unos i prikaz validacijskih poruka nisu implementirani niti testirani.

## Otvorena pitanja

- Nullability veze prema `MaterialType` i pravila vremenskih oznaka potvrdit će se uz persistence sloj.
- Maksimalne dimenzije i usporedba s radnim područjem stroja pripadaju kasnijim validacijskim i service koracima.

## Moguće poglavlje završnog rada

Domenski model materijala i zaštita domenskih invarijanti.

## Kandidati za isječke koda

### Kandidat: Minimalna invarijanta dimenzija ploče

**Datoteka:** `src/main/java/hr/lukabosnjak/domain/entities/MaterialSheet.java`
**Klasa/metoda:** `MaterialSheet#requirePositiveDimension`
**Zašto je važan:** Pokazuje kako entitet štiti valjano stanje bez preuzimanja UI validacije.
**Moguće poglavlje:** Implementacija domenskog modela

```java
private static double requirePositiveDimension(String fieldName, double value) {
    if (!Double.isFinite(value) || value <= 0) {
        throw new IllegalArgumentException(fieldName + " must be finite and greater than 0");
    }
    return value;
}
```

**Ideja opisa u radu:** Konstruktor i setteri koriste istu malu provjeru kako nijedan javni put izmjene dimenzija ne bi ostavio entitet u nevaljanom stanju.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
