# 4.2 — Privremeni tip alata i Tool

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Nakon audita projektnih odluka i dokumentacije dodati osnovni `Tool` model bez izmišljanja `ToolType` vrijednosti ili stvarnih podataka alata za ZK-1325.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/Tool.java` — dodan je domenski model alata s privremenim `String type` poljem.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je privremena odluka i uvjeti potrebni za budući konačni enum.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/04-02-tool-type-decision-tool.md` — dokumentirani su audit, implementacija i stvarne provjere.

## Stvarna implementacija

`Tool` je obična mutable Java klasa s privatnim poljima, punim konstruktorom te standardnim getterima i setterima. Sadrži ID, objektni odnos prema `CncMachine`, broj i naziv alata, privremeni tekstualni tip, promjer, reznu duljinu, broj oštrica, status aktivnosti te vremenske oznake stvaranja i izmjene.

Nije dodan `ToolType` enum, popis dopuštenih tekstualnih vrijednosti, seed stvarnih alata ni podaci koji bi se predstavljali kao izmjereni ili potvrđeni na ZK-1325.

## Razlog odabranog rješenja

Audit `00_odluke.md`, izvornog koda, Markdown plana, UML/relacijskog dijagrama i projektnih DOCX dokumenata nije pronašao potvrđen popis tipova. Privremeni `String` omogućuje jednostavan model pogodan budućem ručnom JDBC mapiranju bez zaključavanja izmišljenog enuma.

## Arhitektonska povezanost

`Tool` pripada `domain.entities`, ovisi samo o drugom domenskom entitetu `CncMachine` i `java.time.LocalDateTime`. Ne sadrži JavaFX, JDBC, H2, ORM, SQL, UI, G-kod ni logiku obrade.

## Važne odluke i ograničenja

- `String type` je privremena, izričito odobrena reprezentacija, a ne konačna domena vrijednosti.
- Za budući `ToolType` treba evidentirati oznaku ili kataloški naziv, reznu geometriju i namjenu stvarnih alata, njihov broj te V1 kategorije koje potvrdi operator.
- `diameter`, `cuttingLength` i `fluteCount` ostaju zasebni podaci i ne koriste se za izmišljanje tipa.
- Nullability, pozitivne vrijednosti, jedinstvenost broja unutar stroja i ostala pravila pripadaju budućim validation/persistence koracima.
- Nisu uvedeni stvarni alati, machining parametri, UI, SQL ni `ToolType` enum.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
javap -classpath target/classes -private hr.lukabosnjak.domain.entities.Tool
rg -n -i "javafx|java.sql|javax.persistence|jakarta.persistence|hibernate|ZK-1325|RichAuto" src/main/java/hr/lukabosnjak/domain/entities/Tool.java
rg -n "ToolType|enum\s+Tool" src/main/java src/test/java
```

### Stvarni rezultat

Maven je izvan sandbox ograničenja, uz Oracle OpenJDK 26.0.2.1, kompilirao svih jedanaest glavnih izvora s Java releaseom 26 i završio s `BUILD SUCCESS`. Projekt nema testnih klasa. `javap` je potvrdio sva tražena polja, puni konstruktor i pristupne metode. Statičke pretrage nisu pronašle zabranjene ovisnosti, hardkodirane strojne vrijednosti ni `ToolType` enum.

### Što nije testirano

Nisu testirani stvarni alati, značenje tekstualnog tipa, validation, JDBC/H2 mapiranje, jedinstvenost broja alata, UI ni fizičko ponašanje na ZK-1325. Nisu dodani testovi konstruktora, gettera i settera jer ne bi dokazivali poslovno ponašanje.

## Otvorena pitanja

- Koji stvarni alati postoje i koriste se na ciljnom ZK-1325 stroju?
- Koje njihove potvrđene rezne kategorije trebaju postati konačne `ToolType` vrijednosti u V1?

## Moguće poglavlje završnog rada

Domenski model CNC stroja i alata.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — `Tool` trenutačno sadrži samo podatkovna polja, puni konstruktor i standardne pristupne metode.

## Kandidat za sliku, dijagram ili tablicu

`Dokumentacija/Dijagrami/finalna-vezija_uml_dijagrama.drawio.png` — postojeći UML dijagram već prikazuje vezu `CncMachine`–`Tool` i može se usporediti s modelom.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
