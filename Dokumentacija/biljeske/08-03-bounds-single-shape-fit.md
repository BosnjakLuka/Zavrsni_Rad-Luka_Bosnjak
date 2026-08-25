# 8.3 — Bounds i single-shape fit

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Izračunati točan XY opseg već generiranog ToolPatha i provjeriti stane li jedan oblik unutar odabrane ploče materijala i XY radnog područja stroja, bez ponavljanja shape formula ili uvođenja layout pravila.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/geometry/Bounds2.java` — dodana je nepromjenjiva reprezentacija minimalnih i maksimalnih XY koordinata.
- `src/main/java/hr/lukabosnjak/geometry/ToolPathBoundsCalculator.java` — dodan je izračun opsega linijskih i kružnih segmenata.
- `src/main/java/hr/lukabosnjak/validation/SingleShapeFitValidator.java` — dodana je poslovna provjera ploče i radnog područja stroja.
- `src/test/java/hr/lukabosnjak/geometry/ToolPathBoundsCalculatorTest.java` — dodani su numerički bounds testovi.
- `src/test/java/hr/lukabosnjak/validation/SingleShapeFitValidatorTest.java` — dodani su fit i poruka testovi.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — usklađena je razvojna dokumentacija.

## Stvarna implementacija

`Bounds2` čuva `minX`, `minY`, `maxX` i `maxY` te iz njih računa širinu i visinu. Konstruktor odbija nekonačne ili obrnute granice.

`ToolPathBoundsCalculator` prolazi kroz postojeće `PathSegment` objekte. Kod linije uključuje početnu i završnu točku. Kod luka dodatno provjerava koje od četiri kardinalne točke kružnice pripadaju stvarnom CW ili CCW sweepu te ih uključuje u bounds. Zbog toga se puni opseg kruga dobiva iz dva `ArcSegment` zapisa iako njihove krajnje točke same ne sadrže najnižu i najvišu točku kružnice.

`SingleShapeFitValidator` koristi calculator i postojeći `MaterialSheetValidator`. Putanja stane samo ako su `minX` i `minY` nenegativni, a `maxX` i `maxY` ne prelaze širinu/visinu ploče ni `workAreaX/workAreaY` stroja. Jednakost na granici je dopuštena. Ploča se provjerava prije stroja i svako odstupanje vraća konkretnu `ValidationException` poruku.

## Razlog odabranog rješenja

Izračun granica pripada geometry sloju jer ovisi o tipu i smjeru segmenata. Poslovna odluka stane li putanja na odabrane resurse pripada validation sloju. Takva podjela izbjegava dupliciranje formula za trokut i krug te omogućuje da isti calculator kasnije koristi bounds ili layout kod bez prebacivanja poslovnih poruka u geometriju.

## Arhitektonska povezanost

Geometry sloj proizvodi samo numerički `Bounds2`. Validation sloj uspoređuje taj rezultat s domenom `MaterialSheet` i `CncMachine`. Nema JavaFX-a, SQL-a, G-code teksta, margina, razmaka ni algoritma raspoređivanja. `Main` nije mijenjan.

## Važne odluke i ograničenja

- Fit provjerava cijeli planirani XY opseg, uključujući translaciju i negativan lokalni položaj, a ne samo `bounds.width()` i `bounds.height()`.
- Dimenzije točno jednake dostupnoj granici prolaze.
- Provjeravaju se samo X i Y; Z radno područje, dubina obrade i fizička koordinatna konvencija nisu dio ovog koraka.
- Nisu uvedene margine, spacing, rotacija, kapacitet ploče ni layout.
- `00_odluke.md` je dopunjen jer način računanja lučnih ekstrema i značenje fit granice postaju ugovor budućih potrošača ToolPatha.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dtest=ToolPathBoundsCalculatorTest,SingleShapeFitValidatorTest test
mvn clean test
rg -n "sqrt|Math\.PI|Math\.sin|Math\.cos|Math\.hypot|ShapeType" src/main/java/hr/lukabosnjak/validation/SingleShapeFitValidator.java
rg -n "javafx|java\.sql|G00|G01|G02|G03|Layout|margin|spacing" src/main/java/hr/lukabosnjak/geometry/Bounds2.java src/main/java/hr/lukabosnjak/geometry/ToolPathBoundsCalculator.java src/main/java/hr/lukabosnjak/validation/SingleShapeFitValidator.java
```

Maven naredbe izvršene su IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Prvi pokušaj ciljane Maven provjere zaustavljen je pri kompilaciji testova zbog suvišne zatvorene zagrade u dva `assertDoesNotThrow` poziva; testovi nisu bili pokrenuti. Nakon ispravka testne sintakse ista ciljana naredba završila je s `BUILD SUCCESS`: 13 testova, 0 failurea, 0 errora i 0 preskočenih testova.

Završni `mvn clean test` od nule je kompilirao 49 glavnih i 13 testnih izvora te završio s `BUILD SUCCESS`: 62 testa, 0 failurea, 0 errora i 0 preskočenih testova. Statičke provjere nisu pronašle shape formule u fit validatoru niti JavaFX, JDBC, G-code ili layout pojmove u novim produkcijskim izvorima.

### Što nije testirano

Bounds i fit još nisu povezani s budućim service/UI ili end-to-end workflowom. Nisu testirani layout, margine, spacing, Z granice, G-code ni fizičko ponašanje na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Budući application service treba povezati tok `Shape -> ToolPath -> bounds/fit` prije G-code generiranja.
- Margine, razmak i raspoređivanje ostaju odluke Iteracije 2 nakon GATE 1.

## Moguće poglavlje završnog rada

Izračun geometrijskih granica i provjera iskoristivosti radnog prostora.

## Kandidati za isječke koda

### Kandidat: Bounds kružnog luka prema smjeru sweepa

**Datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathBoundsCalculator.java`  
**Klasa/metoda:** `ToolPathBoundsCalculator#includeArcExtrema` i `isOnSweep`  
**Zašto je važan:** Pokazuje kako se bounds luka dobiva iz segmenta, centra i smjera bez poznavanja izvornog Shape tipa.  
**Moguće poglavlje:** Geometrijski izračun opsega putanje.

```java
for (double angle : CARDINAL_ANGLES) {
    if (isOnSweep(angle, startAngle, endAngle, arc.direction())) {
        bounds.include(new Point2(
                arc.center().x() + radius * Math.cos(angle),
                arc.center().y() + radius * Math.sin(angle)));
    }
}
```

**Ideja opisa u radu:** U bounds ulaze samo kardinalni ekstremi koji se stvarno nalaze na usmjerenom luku, pa isti algoritam radi za CW i CCW segmente.

### Kandidat: Poslovna granica jednog oblika

**Datoteka:** `src/main/java/hr/lukabosnjak/validation/SingleShapeFitValidator.java`  
**Klasa/metoda:** `SingleShapeFitValidator#fitsWithin`  
**Zašto je važan:** Jasno odvaja numerički bounds od pravila da planirani opseg mora ostati u pozitivnom XY području i unutar dostupnih dimenzija.  
**Moguće poglavlje:** Validacija radnog prostora CNC posla.

```java
return bounds.minX() >= 0
        && bounds.minY() >= 0
        && bounds.maxX() <= limitX
        && bounds.maxY() <= limitY;
```

**Ideja opisa u radu:** Provjera koristi apsolutne minimalne i maksimalne koordinate, zato ispravno odbija i premalu površinu i negativno transliranu putanju.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
