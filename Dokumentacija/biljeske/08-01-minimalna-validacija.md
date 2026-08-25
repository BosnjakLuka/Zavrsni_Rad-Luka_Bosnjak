# 8.1 — Minimalna validacija bez frameworka

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Uvesti mali, ponovno upotrebljiv validation sloj za potvrđene domenske i međusobne uvjete jednog CNC posla, bez validation frameworka i bez premještanja pravila u domenske settere, UI ili persistence.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/validation/` — dodani su `ValidationException` te validatori oblika, ploče, parametara obrade i cijelog posla.
- `src/test/java/hr/lukabosnjak/validation/` — dodana su četiri unit test razreda za valjane, granične i nevaljane slučajeve.
- `Dokumentacija/biljeske/08-01-minimalna-validacija.md` — dokumentiran je ovaj razvojni korak.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je korak 8.1.

## Stvarna implementacija

Svaki validator izlaže jednostavnu metodu `validate(...)`. Valjan objekt završava bez povratne vrijednosti, a prva pronađena pogreška baca `ValidationException` s konkretnom hrvatskom porukom. Ne postoji generički validator interface, kolekcija grešaka ni vanjski framework.

`ShapeValidator` tumači potvrđene dimenzije za četiri V1 oblika. Jednakostranični trokut zahtijeva `EQUILATERAL`; neiskorištene dimenzije i podtip kod ostalih oblika zasad se ne odbijaju. `MaterialSheetValidator` provjerava širinu, visinu i debljinu, a `MachiningParametersValidator` svih šest pozitivnih domenskih veličina. Sve provjeravane decimalne vrijednosti moraju biti konačne i veće od nule.

`MachiningJobValidator` sastavlja tri navedena validatora i dodatno provjerava pozitivnu količinu, odabrani stroj i alat, pripadnost alata stroju, aktivnost alata, maksimalni posmak i uključivi raspon brzine vretena. Pripadnost prihvaća istu instancu stroja ili jednake nenulte bazne identitete, što podržava i odvojene objekte rekonstruirane JDBC mapiranjem.

## Razlog odabranog rješenja

Fail-fast API s posebnom vrstom iznimke najmanji je ugovor koji budućem service/UI toku omogućuje razlikovanje poslovne validation pogreške i prikaz konkretne poruke. Odvojeni validatori omogućuju samostalnu provjeru snapshot objekata, dok `MachiningJobValidator` čuva pravila koja ovise o više objekata na jednom mjestu.

## Arhitektonska povezanost

Implementacija pripada potvrđenom paketu `validation` i ovisi samo o domenskim klasama. Ne sadrži JavaFX, JDBC, SQL, geometriju, layout ni G-code. Repositoryji nisu počeli pozivati validatore; budući service workflow ostaje mjesto koordinacije prije spremanja ili generiranja.

## Važne odluke i ograničenja

- `quantity` validator prihvaća svaki pozitivan `int`, ali Iteracija 1 budućem workflowu i dalje dopušta samo internu vrijednost `1`.
- `feedRate == machine.maxFeedRate` je valjan slučaj.
- Minimalna i maksimalna brzina vretena uključene su u dopušteni raspon.
- Pozitivni `cutDepth`, `stepDown` i `safeZ` predstavljaju domenske veličine; iz njih se ne izvodi znak Z koordinate ciljnog stroja.
- Ne validiraju se naziv posla, korisnik, G-code, vrsta materijala, konfiguracija samog stroja ni fizičke karakteristike alata jer nisu dio ovog koraka.
- `00_odluke.md` nije mijenjan jer implementacija ostvaruje već zabilježenu granicu zasebnog validation sloja, bez nove arhitektonske ili tehnološke odluke.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dtest=hr.lukabosnjak.validation.*Test test
mvn test
mvn clean test
```

Naredbe su izvršene IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani validation suite završio je s `BUILD SUCCESS`: 32 testa, 0 failurea, 0 errora i 0 preskočenih testova. Cijeli projektni suite zatim je završio s `BUILD SUCCESS`: 41 test, 0 failurea, 0 errora i 0 preskočenih testova. Završni `mvn clean test` ponovno je od nule kompilirao 39 glavnih i 9 testnih izvora te potvrdio isti uspješan rezultat od 41 testa.

Prvi pokušaj ciljane provjere unutar ograničenog sandboxa nije došao do testova jer compiler nije mogao zatvoriti resurs vanjskog H2 artefakta. Ista provjera zatim je izvršena izvan tog ograničenja i uspješno završena gore navedenim rezultatom.

### Što nije testirano

Validatori još nisu povezani sa service, JavaFX ili end-to-end workflowom jer ti dijelovi nisu implementirani u ovom milestoneu. Nije provjeravano fizičko ponašanje Z koordinate ni parametara na ZK-1325 / RichAuto A11; unit testovi dokazuju samo navedena softverska pravila.

## Otvorena pitanja

- Budući service workflow treba odrediti mjesto poziva `MachiningJobValidator` prije generiranja ili spremanja.
- Korisnički unos količine ostaje odgođen do Iteracije 2; Iteracija 1 validatoru predaje `1`.

## Moguće poglavlje završnog rada

Validacija domenskih podataka i međuobjektnih ograničenja CNC posla.

## Kandidati za isječke koda

### Kandidat: Validacija međusobnih pravila CNC posla

**Datoteka:** `src/main/java/hr/lukabosnjak/validation/MachiningJobValidator.java`  
**Klasa/metoda:** `MachiningJobValidator#validate`  
**Zašto je važan:** Prikazuje kompoziciju pojedinačnih validatora te provjere pripadnosti alata, aktivnosti i ograničenja odabranog stroja.  
**Moguće poglavlje:** Validacija poslovnih pravila.

```java
shapeValidator.validate(job.getShape());
materialSheetValidator.validate(job.getMaterialSheet());
machiningParametersValidator.validate(job.getMachiningParameters());

if (!belongsTo(tool, machine)) {
    throw new ValidationException("Odabrani alat ne pripada odabranom CNC stroju.");
}
if (!tool.isActive()) {
    throw new ValidationException("Odabrani alat nije aktivan.");
}
```

**Ideja opisa u radu:** Validator agregata povezuje lokalne provjere snapshot podataka s pravilima koja ovise o odabranom stroju i alatu, bez prebacivanja te logike u UI ili bazu.

### Kandidat: Semantička validacija podržanih oblika

**Datoteka:** `src/main/java/hr/lukabosnjak/validation/ShapeValidator.java`  
**Klasa/metoda:** `ShapeValidator#validate`  
**Zašto je važan:** Prikazuje kako generičke dimenzije A i B dobivaju značenje ovisno o potvrđenom `ShapeType`.  
**Moguće poglavlje:** Domenski model i validacija geometrijskih oblika.

```java
case RECTANGLE -> {
    requirePositive(
            shape.getDimensionA(), "Širina pravokutnika mora biti veća od 0 mm.");
    requirePositive(
            shape.getDimensionB(), "Visina pravokutnika mora biti veća od 0 mm.");
}
case CIRCLE -> requirePositive(
        shape.getDimensionA(), "Promjer kruga mora biti veći od 0 mm.");
```

**Ideja opisa u radu:** Jedan domenski entitet ostaje generički, dok validation sloj prevodi dimenzije u pravila i poruke specifične za odabrani oblik.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
