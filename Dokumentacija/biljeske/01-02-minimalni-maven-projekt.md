# 1.2 — Kreiranje minimalnog Maven projekta

**Datum:** 2026-08-25  
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Uspostaviti minimalan Maven temelj za nemodularnu desktop JavaFX aplikaciju na potvrđenom JDK-u 26, uz JUnit podršku i bez prerane H2, FXML ili framework konfiguracije.

## Promijenjene datoteke

- `pom.xml` — dodane JavaFX/JUnit ovisnosti i zaključana compiler, test i JavaFX run konfiguracija.
- `src/test/java/hr/lukabosnjak/.gitkeep` — uspostavljena pratljiva standardna Maven testna struktura bez trivijalnog testa.
- `AGENTS.md` — zabilježeni potvrđeni JDK, Maven i artifactId.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena odluka o JDK-u i build alatu.
- `Dokumentacija/biljeske/00_indeks.md` — dodan ovaj razvojni korak.

## Stvarna implementacija

`pom.xml` sada koristi `maven.compiler.release` 26, JavaFX Controls 26.0.2 i JUnit Jupiter 6.0.3. Maven Compiler, Surefire i JavaFX Maven plugin imaju eksplicitne verzije. Postojeći `Main.java` nije izmijenjen, a testna package struktura postoji bez umjetnog testa.

## Razlog odabranog rješenja

Maven je već bio prisutan kao build format projekta i jednostavan je za reproduciranje u studentskom projektu. `javafx-controls` pruža potrebne UI API-je i tranzitivno povlači JavaFX Base/Graphics. JUnit je ograničen na testni scope. Verzije pluginova su zaključane kako rezultat ne bi ovisio o Mavenovim zadanim, promjenjivim verzijama.

## Arhitektonska povezanost

Ovaj korak uspostavlja samo build temelj. Ne uvodi domenske, UI controller, persistence, geometry, layout ni G-code odgovornosti.

## Važne odluke i ograničenja

- Potvrđeni projektni JDK je Oracle OpenJDK 26.0.2; stvarno korišten lokalni runtime pri provjeri prijavljuje 26.0.2.1.
- Maven je odabrani build alat, a artifactId je `cnc-optimizer`.
- Projekt je zasad nemodularan i nema `module-info.java`.
- FXML pristup još nije odlučen, pa `javafx-fxml` nije dodan.
- H2 i dodatni frameworki nisu dio ovog koraka.

## Build i testiranje

### Izvršene naredbe

```text
java.exe -version
javac.exe -version
mvn.cmd -version
mvn.cmd -Dmaven.repo.local=target/.m2/repository test
```

Naredbe su izvršene izravnim lokalnim putanjama do IntelliJ-evog JDK-a i ugrađenog Mavena jer nisu bili dostupni kroz terminalski `PATH`.

### Stvarni rezultat

- Java runtime: Oracle OpenJDK 26.0.2.1.
- Java compiler: `javac 26.0.2.1`.
- Maven: 3.9.16, pokrenut na navedenom Oracle JDK-u.
- Maven je razriješio konfigurirane dependencyje, kompilirao jedan postojeći source s `release 26` i završio s `BUILD SUCCESS`.
- Testnih klasa još nema, pa nisu izvršeni pojedinačni JUnit testovi.

### Što nije testirano

- JavaFX prozor nije pokrenut; minimalna JavaFX ulazna točka pripada koraku 1.3.
- FXML, H2 i aplikacijski slojevi nisu implementirani niti testirani.

## Otvorena pitanja

- Hoće li UI koristiti FXML ili programatski JavaFX odlučit će se u kasnijem zasebnom koraku.
- Terminalski `PATH` i `JAVA_HOME` izvan IntelliJ-a još nisu trajno konfigurirani.

## Moguće poglavlje završnog rada

Tehnologije i razvojno okruženje / Organizacija i izgradnja projekta.

## Kandidati za isječke koda

Kandidat za isječak koda: nema — u ovom koraku nema Java koda dovoljno značajnog za završni rad.

## Kandidat za sliku, dijagram ili tablicu

Tablica odabranih tehnologija i njihovih uloga može uključiti Oracle OpenJDK 26, Maven, JavaFX i JUnit.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, tokene, osobne podatke ni osobne lokalne putanje.
