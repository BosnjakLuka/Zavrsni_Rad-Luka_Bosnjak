# 9.3 — Linearni single-element G-code

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Pretvoriti jedan povezani i zatvoreni line-segment `ToolPath` u determinističan multi-pass G-code program uz siguran Z prije svakog XY repositioninga, kontrolirani plunge, rezanje po postojećim segmentima i retract između prolaza i na kraju.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java` — dodan je konkretni linearni generator koji implementira `GCodeGenerator`.
- `src/test/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGeneratorTest.java` — dodani su string-level testovi toka, Z konvencija i ulaznog ugovora.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — dokumentirani su redoslijed naredbi, ograničenja i stvarni testni rezultat.

## Stvarna implementacija

`RichAutoA11GCodeGenerator` prima `RichAutoA11Profile` i implementira postojeći `generate(ToolPath, MachiningParameters)` ugovor. Prije generiranja provjerava da svaki segment ima tip `LineSegment`, da je početak svakog sljedećeg segmenta jednak kraju prethodnog i da je završna točka putanje jednaka početnoj. `ArcSegment`, nepovezana putanja i otvorena kontura odbijaju se jasnom `IllegalArgumentException` porukom.

Generator sastavlja postojeći RichAuto header, zatim emitira početni `G00 Z<safe>`. Za svaki rezultat postojećeg `PassDepthCalculatora` emitira `G00 X<start> Y<start>`, `G01 Z<cut>` plunge, `G01` pomake do krajnjih točaka svih postojećih segmenata i `G00 Z<safe>` retract. Nakon zadnjeg retracta dodaje postojeći footer. Safe i cut koordinate dobivaju se isključivo iz `ZCoordinateConvention`; pozitivni `cutDepth`, `stepDown` i `safeZ` u `MachiningParameters` ne mijenjaju predznak.

Kada je `emitFeedRate` uključen, svaki plunge sadrži `F<plungeRate>`, a prvi rezni segment svakog prolaza `F<feedRate>`. Ostali rezni segmenti koriste modalni feed. Kada je opcija isključena, generator zadržava iste `G01` pomake bez F riječi. Sve brojčane vrijednosti prolaze kroz postojeći `GCodeFormatter`.

## Razlog odabranog rješenja

Generator koristi samo već pripremljene točke i segmente pa ne duplicira geometriju kvadrata, pravokutnika ili trokuta. Explicitni rapid do početka u svakom prolazu čini sigurnosni redoslijed vidljivim čak i kada je putanja već završila na početnoj točki. Strukturna provjera sprečava nenamjerni rez preko prekida u putanji, dok zasebno odbijanje lukova čuva G02/G03 za milestone u kojem će I/J i smjer biti ciljano testirani.

## Arhitektonska povezanost

Promjena ostaje u `gcode` sloju i ovisi samo o postojećim geometry vrijednostima, `MachiningParameters` modelu i ranije uvedenim G-code komponentama. Generator ne prima `Shape`, ne računa dimenzije ili raspored, ne poziva SQL/persistence i ne sadrži JavaFX logiku. Pretpostavlja da je application/service tok prije poziva proveo postojeću domensku validaciju machining parametara. `Main` nije mijenjan.

## Važne odluke i ograničenja

- Podržana je samo jedna povezana, zatvorena kontura sastavljena od linijskih segmenata.
- Svaki prolaz sadrži retract na safe Z i novi XY rapid do početka; zadnji prolaz također završava retractom.
- F se emitira pri promjeni iz plunge u rezni feed, a ne na svakom segmentu.
- Profil određuje predznak safe i cut Z koordinata. To nije fizička potvrda Z-smjera ili work zeroa.
- Testne konstante imaju prefiks `SOFTWARE_TEST_`, profil koristi prazan skup fizičkih potvrda, a vrijednosti nisu stvarni ni preporučeni parametri ZK-1325.
- Nisu implementirani G02/G03, krug, tool compensation, layout, `.nc` export, service/UI povezivanje ni fizička provjera.
- `00_odluke.md` je ažuriran jer sigurnosni redoslijed i ugovor zatvorene povezane putanje postaju osnova budućeg generatora.

## Build i testiranje

### Izvršene naredbe

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-26.0.2.1'
$mavenCommand = Join-Path $env:ProgramFiles 'JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" '-Dtest=RichAutoA11GCodeGeneratorTest' test
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean test
```

Maven je izvršen IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani `RichAutoA11GCodeGeneratorTest` završio je s `BUILD SUCCESS`: 5 testova, 0 failurea, 0 errora i 0 preskočenih testova. Provjereni su puni multi-pass tekst, oba Z-smjera, isključivanje F riječi te odbijanje luka, nepovezane i otvorene putanje.

Završni `clean test` od nule je kompilirao 56 glavnih i 19 testnih izvora te završio s `BUILD SUCCESS`: 89 testova, 0 failurea, 0 errora i 0 preskočenih testova.

### Što nije testirano

Program nije izvezen u `.nc` datoteku niti pokrenut na CNC kontroleru. Nisu fizički provjereni `G00`, `G01`, `F`, `S`, `G54`, spindle naredbe, brzine, dubine, Z-smjer, work zero, siguran razmak ni cjelokupan redoslijed na ZK-1325 / RichAuto A11. Nije provjeren krug ni G02/G03 output.

## Otvorena pitanja

- Milestone 9.4 treba mapirati postojeći `ArcSegment` na G02/G03 i relativne I/J vrijednosti bez ponovnog računanja kruga.
- Budući service workflow treba prije generatora koordinirati postojeću validaciju i fit provjeru te nakon generiranja omogućiti zaseban `.nc` export.

## Moguće poglavlje završnog rada

Generiranje sigurnog višestrukog linearnog CNC prolaza iz geometrijskog ToolPath modela.

## Kandidati za isječke koda

### Kandidat: Siguran multi-pass redoslijed

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java`
**Klasa/metoda:** `RichAutoA11GCodeGenerator#generate`
**Zašto je važan:** Prikazuje sastavljanje headera, sigurnog Z položaja, step-down prolaza, postojećih segmenata i footera bez računanja Shape geometrije.
**Moguće poglavlje:** Generiranje G-code programa.

```java
lines.addAll(programEnvelope.headerLines(parameters));
lines.add(rapidZ(safeZ));

for (double passDepth : passDepths) {
    lines.add(rapidXy(startPoint));
    lines.add(plunge(profile.zCoordinateConvention().toCutZ(passDepth), parameters.getPlungeRate()));
    lines.addAll(cuttingMoves(segments, parameters.getFeedRate()));
    lines.add(rapidZ(safeZ));
}

lines.addAll(programEnvelope.footerLines());
```

**Ideja opisa u radu:** Svaki prolaz započinje iz eksplicitno uspostavljenog sigurnog stanja i završava retractom prije sljedećeg XY repositioninga ili završetka programa.

### Kandidat: Ugovor povezane i zatvorene putanje

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java`
**Klasa/metoda:** `RichAutoA11GCodeGenerator#requireConnectedClosedLinePath`
**Zašto je važan:** Pokazuje da generator ne izračunava geometriju, ali prije emitiranja provjerava strukturne pretpostavke potrebne za sigurno praćenje krajnjih točaka.
**Moguće poglavlje:** Ugovor između geometry i G-code sloja.

```java
if (!(segment instanceof LineSegment lineSegment)) {
    throw new IllegalArgumentException("Linear G-code generator supports only line segments");
}
if (previousEnd != null && !previousEnd.equals(lineSegment.start())) {
    throw new IllegalArgumentException("Line-segment ToolPath must be connected");
}
```

**Ideja opisa u radu:** Generator prihvaća samo putanju čiji se segmenti mogu slijediti redom bez implicitnog reza preko geometrijskog prekida.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
