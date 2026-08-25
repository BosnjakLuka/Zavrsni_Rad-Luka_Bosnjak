# 1.4 — Osnovna package struktura

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Potvrditi `hr.lukabosnjak` kao base package i smjestiti JavaFX ulaznu točku u paket `app`, bez preuranjenog stvaranja praznih paketa ili implementacije budućih slojeva.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/app/Main.java` — klasa `Main` premještena je u aplikacijski paket.
- `pom.xml` — JavaFX `mainClass` usklađen je s novim punim nazivom klase.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je potvrđena odluka o base packageu i aplikacijskoj ulaznoj točki.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/01-04-osnovna-package-struktura.md` — dokumentirana je implementacija i provjera.

## Stvarna implementacija

`Main` sada pripada paketu `hr.lukabosnjak.app`, a Maven JavaFX plugin upućuje na `hr.lukabosnjak.app.Main`. Nisu stvoreni prazni direktoriji za buduće slojeve i nisu dodane nove klase, ovisnosti ni CNC postavke.

## Razlog odabranog rješenja

Paket `app` jasno označava odgovornost pokretanja i sastavljanja aplikacije. Zadržavanje postojećeg base packagea izbjegava nepotrebno preimenovanje, a odgođeno stvaranje ostalih paketa osigurava da struktura prati stvarno implementirani kod.

## Arhitektonska povezanost

Ulazna točka ostaje odvojena od budućih paketa `ui`, `service`, `validation`, `geometry`, `layout`, `gcode` i `persistence`. Ovaj korak ne uvodi veze među tim slojevima.

## Važne odluke i ograničenja

- Base package je `hr.lukabosnjak`; aplikacijska ulazna točka pripada paketu `hr.lukabosnjak.app`.
- Paketi se stvaraju kada dobiju stvarnu implementaciju, a ne kao prazni direktoriji.
- Paket `exception` nije uveden jer još ne postoji konkretna potreba za zajedničkim iznimkama.
- Nisu uvedeni `Part`, `ToolType`, H2 pristup, geometrija, layout ni G-kod.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> test
mvn -Dmaven.repo.local=<privremeni-repozitorij> javafx:run
```

### Stvarni rezultat

- `mvn test` izvršen je IntelliJ-evim Mavenom na Oracle OpenJDK-u 26.0.2.1 i završio je s `BUILD SUCCESS`. Kompilirana je premještena klasa; projekt još nema testnih klasa.
- `mvn javafx:run` uspješno je pronašao `hr.lukabosnjak.app.Main`, učitao JavaFX `graphics` native library i ostao aktivan bez aplikacijske iznimke dok proces nije ručno zaustavljen.
- JavaFX je ispisao upozorenje o budućoj potrebi za eksplicitnim native accessom; upozorenje nije spriječilo pokretanje.

### Što nije testirano

Vizualni sadržaj i dimenzije JavaFX Stagea nisu pouzdano potvrđeni u agentovu okruženju. Fizičko ponašanje na ciljnom CNC stroju nije dio ovog koraka niti je testirano.

## Otvorena pitanja

- Konkretne klase i veze svakog budućeg sloja definirat će se u zasebnim, malim implementacijskim koracima.
- Odluka o FXML-u i dalje nije donesena.

## Moguće poglavlje završnog rada

Arhitektura aplikacije i organizacija izvornog koda.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — promjena deklaracije paketa i Maven naziva ulazne klase nije dovoljno značajna za samostalan isječak u završnom radu.

## Kandidat za sliku, dijagram ili tablicu

Dijagram dopuštenih ovisnosti među paketima nakon što ti paketi budu stvarno implementirani.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
