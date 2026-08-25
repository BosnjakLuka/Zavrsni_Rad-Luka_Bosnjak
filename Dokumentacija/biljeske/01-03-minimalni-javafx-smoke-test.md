# 1.3 — Minimalni JavaFX smoke test

**Datum:** 2026-08-25  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Dodati najmanju aplikacijsku ulaznu točku koja inicijalizira JavaFX i prikazuje prazan Stage, bez početka razvoja stvarnog korisničkog sučelja.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/Main.java` — generirani konzolni primjer zamijenjen minimalnom JavaFX `Application` klasom.
- `Dokumentacija/biljeske/00_indeks.md` — dodan rezultat ovog koraka.
- `Dokumentacija/biljeske/01-03-minimalni-javafx-smoke-test.md` — dokumentiran prompt, implementacija i stvarne provjere.

## Stvarna implementacija

`Main` sada nasljeđuje JavaFX `Application`. Standardna `main` metoda poziva `launch(args)`, a `start(Stage)` postavlja praznu scenu veličine 640 × 480 i prikazuje Stage. Nema kontrola, controllera, FXML-a ni poslovne logike.

## Razlog odabranog rješenja

Prazan `Group` daje sceni valjani root bez uvođenja stvarnog UI sadržaja. Eksplicitne dimenzije omogućuju vidljiv desktop prozor, dok standardni JavaFX lifecycle ostaje kratak i razumljiv.

## Arhitektonska povezanost

`Main` je isključivo aplikacijska ulazna točka. Ovaj korak još ne uvodi UI controller ni ovisnosti prema service, persistence, geometry, layout ili gcode slojevima.

## Važne odluke i ograničenja

- Smoke test je programatski samo zato što još nema stvarnog UI-ja; nije donesena konačna odluka o FXML-u.
- `pom.xml` nije mijenjan jer je postojeća JavaFX/Maven konfiguracija bila dovoljna.
- Nisu dodani controller, FXML, baza, service, model ni G-code.

## Build i testiranje

### Izvršene naredbe

```text
mvn test
mvn javafx:run
```

Naredbe su izvršene IntelliJ-evim Mavenom 3.9.16 na lokalnom Oracle OpenJDK-u 26.0.2.1, uz odvojeni privremeni Maven repozitorij jer alati nisu dostupni kroz terminalski `PATH`.

### Stvarni rezultat

- Čisti `mvn test` kompilirao je `Main.java` s `release 26` i završio s `BUILD SUCCESS`.
- `mvn javafx:run` došao je do JavaFX run cilja, učitao `javafx.graphics` native library i ostao aktivan bez aplikacijskog exceptiona dok smoke test nije ručno zaustavljen.
- JavaFX je ispisao upozorenje da će buduće Java verzije zahtijevati eksplicitni native access; upozorenje nije spriječilo trenutačno pokretanje.

### Što nije testirano

- Agentovo okruženje nije omogućilo pouzdanu vizualnu potvrdu sadržaja i dimenzija desktop Stagea. Vizualni prikaz praznog prozora zato ostaje NIJE TESTIRANO dok ga korisnik ne potvrdi u IntelliJ-u ili lokalnom terminalu.
- Nema JUnit testova jer u ovom koraku nema neinteraktivne poslovne logike vrijedne unit testa.

## Otvorena pitanja

- Lokalno potvrditi da se prikazuje prazan prozor veličine 640 × 480 i da se može normalno zatvoriti.
- Odluka o FXML-u ili programatskom UI-ju ostaje za zaseban budući korak.
- Prije kasnijeg produkcijskog pakiranja razmotriti eksplicitni `--enable-native-access=javafx.graphics` ako upozorenje i dalje postoji na odabranoj Java verziji.

## Moguće poglavlje završnog rada

Implementacija aplikacijske ulazne točke / JavaFX lifecycle.

## Kandidati za isječke koda

### Kandidat: Pokretanje JavaFX aplikacije i prikaz praznog Stagea

**Datoteka:** `src/main/java/hr/lukabosnjak/Main.java`  
**Klasa/metoda:** `Main#main`, `Main#start`  
**Zašto je važan:** Pokazuje minimalni JavaFX lifecycle i jasnu granicu između aplikacijske ulazne točke i budućeg UI-ja.  
**Moguće poglavlje:** Implementacija / JavaFX korisničko sučelje

```java
public static void main(String[] args) {
    launch(args);
}

@Override
public void start(Stage stage) {
    stage.setScene(new Scene(new Group(), 640, 480));
    stage.show();
}
```

**Ideja opisa u radu:** Metoda `main` pokreće JavaFX runtime, nakon čega framework poziva `start`. Metoda `start` stvara praznu scenu i prikazuje početni Stage bez poslovne ili controller logike.

## Kandidat za sliku, dijagram ili tablicu

Snimka praznog početnog Stagea nakon lokalne vizualne potvrde.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, tokene, osobne podatke ni osobne lokalne putanje.
