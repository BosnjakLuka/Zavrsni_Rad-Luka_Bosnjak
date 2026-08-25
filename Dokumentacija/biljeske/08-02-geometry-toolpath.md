# 8.2 — Geometry i ToolPath za četiri V1 oblika

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Uvesti controller-agnostic geometry sloj koji iz validiranog `Shape` modela stvara jasnu, nepromjenjivu geometrijsku putanju za sva četiri V1 oblika, bez JavaFX-a, G-code teksta, bounds ili layout logike.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/geometry/` — dodani su 2D točka, segmenti, smjer luka, ToolPath i servis za generiranje.
- `src/test/java/hr/lukabosnjak/geometry/` — dodani su numerički testovi generatora i translacije.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je V1 geometrijska reprezentacija.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/08-02-geometry-toolpath.md` — dokumentirani su implementacija i stvarne provjere.

## Stvarna implementacija

`Point2`, `LineSegment`, `ArcSegment` i `ToolPath` nepromjenjivi su Java record tipovi. Sealed `PathSegment` ugovor izlaže početnu i završnu točku te translaciju, dok `ArcSegment` dodatno čuva centar i `ArcDirection`. Točke odbijaju nekonačne koordinate, ToolPath odbija praznu putanju, a luk odbija dvosmislen slučaj u kojem su početak i kraj jednaki.

`ToolPath.translated` vraća novi popis pomaknutih segmenata i ostavlja izvornu putanju nepromijenjenom. Kod lukova se zajedno s krajnjim točkama pomiče i centar, dok se smjer ne mijenja.

`ToolPathService` prima postojeći `ShapeValidator`, validira ulaz i zatim preko malih privatnih metoda generira zatvorene CCW konture. Kvadrat i pravokutnik imaju četiri linije, jednakostranični trokut tri linije prema zadanoj formuli, a krug dvije polukružnice oko centra `(r,r)`. Lokalni origin kruga predstavlja donji lijevi kut njegovog budućeg bounding prostora; zbog toga početna točka kružne putanje jest `(0,r)`, a ne `(0,0)`.

## Razlog odabranog rješenja

Nepromjenjivi vrijednosni objekti jasno odvajaju lokalnu geometriju od mutabilnih persistence entiteta i omogućuju sigurnu translaciju za budući layout. Zasebne shape-generator klase nisu uvedene jer bi za četiri kratke formule dodale više strukture nego čitljivosti.

Krug koristi dva luka jer svaki segment ima različit početak i kraj, eksplicitan centar i smjer. Budući G-code generator zato može mapirati već izračunatu geometriju bez ponovnog računanja kruga i bez oslanjanja na potencijalno osjetljiv full-circle zapis.

## Arhitektonska povezanost

Sve nove produkcijske klase nalaze se u `hr.lukabosnjak.geometry`. Sloj ovisi samo o domeni i postojećem reusable validatoru. Ne koristi JavaFX, JDBC, H2, persistence, layout ni G-code naredbe. `Main` i controller sloj nisu mijenjani.

## Važne odluke i ograničenja

- Sve geometrijske koordinate izražene su u milimetrima.
- Translacija je čista operacija koja stvara novi ToolPath.
- `COUNTERCLOCKWISE` opisuje geometrijsku orijentaciju konture, ne potvrđeni smjer fizičke obrade.
- Nisu implementirani bounds, fit na ploču/stroj, margine, tool compensation, layout ni G-code.
- RichAuto A11 nije fizički testiran ovim softverskim milestoneom.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dtest=hr.lukabosnjak.geometry.*Test test
mvn clean test
rg -n "javafx|java.sql|G00|G01|G02|G03" src/main/java/hr/lukabosnjak/geometry
```

Maven naredbe izvršene su IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani geometry suite završio je s `BUILD SUCCESS`: 8 testova, 0 failurea, 0 errora i 0 preskočenih testova. Završni `mvn clean test` od nule je kompilirao 46 glavnih i 11 testnih izvora te završio s `BUILD SUCCESS`: 49 testova, 0 failurea, 0 errora i 0 preskočenih testova.

Statička provjera geometry paketa nije pronašla JavaFX, JDBC ni G00/G01/G02/G03 tekst.

### Što nije testirano

Nisu testirani budući bounds/fit, layout ni G-code potrošači ToolPatha. Nije provjereno fizičko ponašanje kružnih lukova ni smjera obrade na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Bounds i single-shape fit dolaze u zasebnom milestoneu 8.3.
- Budući RichAuto generator mora zasebno definirati mapiranje `ArcDirection` na G02/G03 i relativni I/J centar prema profilu kontrolera.

## Moguće poglavlje završnog rada

Geometrijski model i generiranje putanje alata.

## Kandidati za isječke koda

### Kandidat: Generiranje jednakostraničnog trokuta

**Datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathService.java`  
**Klasa/metoda:** `ToolPathService#generateEquilateralTriangle`  
**Zašto je važan:** Prikazuje pretvaranje domenske duljine stranice u numeričke vrhove i zatvorenu linearnu putanju.  
**Moguće poglavlje:** Generiranje geometrije podržanih oblika.

```java
Point2 pointA = new Point2(0, 0);
Point2 pointB = new Point2(side, 0);
Point2 pointC = new Point2(side / 2, Math.sqrt(3) / 2 * side);

return new ToolPath(List.of(
        new LineSegment(pointA, pointB),
        new LineSegment(pointB, pointC),
        new LineSegment(pointC, pointA)));
```

**Ideja opisa u radu:** Formula visine jednakostraničnog trokuta izravno određuje treći vrh, nakon čega tri segmenta zatvaraju konturu u lokalnom koordinatnom sustavu.

### Kandidat: Krug kao dvije geometrijske polukružnice

**Datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathService.java`  
**Klasa/metoda:** `ToolPathService#generateCircle`  
**Zašto je važan:** Objašnjava nedvosmislenu reprezentaciju pune kružnice neovisnu o RichAuto tekstualnom formatu.  
**Moguće poglavlje:** Reprezentacija kružnih putanja.

```java
double radius = diameter / 2;
Point2 center = new Point2(radius, radius);
Point2 left = new Point2(0, radius);
Point2 right = new Point2(diameter, radius);

return new ToolPath(List.of(
        new ArcSegment(left, right, center, ArcDirection.COUNTERCLOCKWISE),
        new ArcSegment(right, left, center, ArcDirection.COUNTERCLOCKWISE)));
```

**Ideja opisa u radu:** Dva luka čuvaju početak, kraj, centar i smjer bez dvosmislenog full-circle segmenta kojem bi početak i kraj bili isti.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
