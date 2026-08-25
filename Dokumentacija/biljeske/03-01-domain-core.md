# 3.1 — Osnovni domenski modeli

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

U jednom milestoneu uspostaviti šest osnovnih domenskih modela u paketu `hr.lukabosnjak.domain.entities`, bez ovisnosti o JavaFX-u, JDBC-u ili ORM-u i bez preuranjene validacijske ili autentikacijske logike.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MaterialType.java` — model vrste materijala premješten je u potvrđeni paket.
- `src/main/java/hr/lukabosnjak/domain/entities/MaterialSheet.java` — model ploče premješten je i uklonjena je validacija iz konstruktora i settera.
- `src/main/java/hr/lukabosnjak/domain/entities/CncMachine.java` — dodan je podatkovni model CNC stroja bez hardkodiranih vrijednosti.
- `src/main/java/hr/lukabosnjak/domain/entities/MachiningParameters.java` — dodan je model parametara obrade.
- `src/main/java/hr/lukabosnjak/domain/entities/Role.java` i `User.java` — dodani su modeli uloga i korisnika bez autentikacijske logike.
- `src/test/java/hr/lukabosnjak/domain/entities/MaterialSheetTest.java` — uklonjen je test validacije koja više ne pripada modelu.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i povezane povijesne bilješke — dokumentacija je usklađena s novom package i validation odlukom.
- `Dokumentacija/biljeske/03-01-domain-core.md` — dokumentiran je ovaj milestone i stvarna provjera.

## Stvarna implementacija

Svih šest klasa obični su mutable Java objekti s privatnim poljima, jednim punim konstruktorom te standardnim getterima i setterima. ID polja koriste `Long`, nullable polja ostaju obične Java reference, a veze su izražene objektima `MaterialType` i `Role`. Klase uvoze samo druge domenske modele i, gdje je potrebno, `java.time.LocalDateTime`.

`CncMachine` ne sadrži zadane podatke ni vrijednosti ZK-1325. `MachiningParameters.cutDepth` i `MaterialSheet.thickness` pohranjuju se kao neovisni podaci. `User.passwordHash` je samo podatkovno polje; login, hashing, session i RBAC nisu implementirani.

## Razlog odabranog rješenja

Puni konstruktor i mutabilna polja omogućuju jednostavno buduće mapiranje cijelog JDBC retka i naknadno postavljanje vrijednosti koje dodijeli baza. Objektne veze čuvaju domensko značenje bez unošenja stranih ključeva i SQL detalja u model. Validacija je namjerno odvojena kako bi se sva pravila mogla dosljedno primijeniti u zasebnom sloju.

## Arhitektonska povezanost

Promjena zahvaća samo `domain.entities`. Domenski kod ne ovisi o UI-ju, JavaFX-u, JDBC-u, H2-u ni persistence implementaciji. Budući `validation` sloj provjeravat će vrijednosti, a budući persistence sloj mapirat će modele i njihove objektne veze na bazu.

## Važne odluke i ograničenja

- Domenski objekti s identitetom nalaze se u `domain.entities`.
- Konstruktori i setteri ne provode potpunu validaciju; nevaljane vrijednosti odbijat će budući `validation` sloj.
- `description`, `deletedAt`, `manufacturer` i `model` mogu biti `null`; dodatna nullability pravila nisu uvedena.
- Nisu dodani no-arg konstruktori, builderi, anotacije, `equals`, `hashCode`, `toString`, SQL, UI ni sigurnosni workflow.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
rg -n -i "javafx|java.sql|javax.persistence|jakarta.persistence|hibernate|ZK-1325|RichAuto" src/main/java/hr/lukabosnjak/domain/entities
rg -n "domain.entities|requirePositiveDimension" src/main/java src/test/java
git diff --check
```

### Stvarni rezultat

Prvi pokušaj unutar sandbox ograničenja završio je s `BUILD FAILURE` jer javac nije mogao zatvoriti JavaFX JAR resurs. Ista naredba zatim je izvršena izvan tog ograničenja s Oracle OpenJDK-om 26.0.2.1: kompilirano je svih sedam glavnih izvora s Java releaseom 26 i build je završio s `BUILD SUCCESS`. Nakon uklanjanja zastarjelog `MaterialSheetTest` testa nema testnih klasa. Obje statičke pretrage završile su bez nalaza, a `git diff --check` nije pronašao whitespace pogreške; ispisao je samo informativna upozorenja o budućoj LF/CRLF konverziji.

### Što nije testirano

Nisu testirani buduća validacija, JDBC/H2 mapiranje, autentikacija, autorizacija ni fizičko ponašanje CNC stroja. Nisu dodani unit testovi gettera, settera i trivijalnih konstruktora jer ne bi dokazivali poslovno ponašanje.

## Otvorena pitanja

- Konkretna validation pravila i poruke definirat će se u zasebnom milestoneu.
- Strategija dodjele ID-eva, vremenskih oznaka i mapiranja odnosa definirat će se uz persistence sloj.

## Moguće poglavlje završnog rada

Domenski model i slojevita arhitektura aplikacije.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — ovaj milestone namjerno sadrži samo podatkovne modele, pune konstruktore i standardne pristupne metode.

## Kandidat za sliku, dijagram ili tablicu

`Dokumentacija/Dijagrami/finalna-vezija_uml_dijagrama.drawio.png` — postojeći UML dijagram može se usporediti s implementiranim klasama; u ovom milestoneu nije izrađen novi vizual.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
