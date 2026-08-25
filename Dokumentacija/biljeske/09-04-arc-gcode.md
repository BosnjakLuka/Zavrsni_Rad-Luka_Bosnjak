# 9.4 — G02/G03 i relativni I/J

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE TESTIRANO NA STROJU

## Cilj

Proširiti postojeći RichAuto generator tako da izravno mapira postojeću `ArcSegment` geometriju u G02/G03 retke s eksplicitnom relativnom I/J konvencijom, bez ponovnog računanja kružnice ili mijenjanja geometry sloja.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java` — dodani su obvezni arc-center mode i zasebne fizičke mogućnosti za G02/G03 i relativni I/J.
- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java` — povezane i zatvorene putanje sada mapiraju linije na G01, a lukove na G02/G03.
- Tri postojeća G-code testna razreda — testni profili usklađeni su s novim poljem, a generator i profil dobili su I/J i direction testove.
- `Dokumentacija/biljeske/09-03-linearni-gcode.md`, `00_odluke.md`, `00_indeks.md` i ova bilješka — dokumentirana je nadogradnja line-only ograničenja i nova konvencija.

## Stvarna implementacija

`RichAutoA11Profile` sada zahtijeva `ArcCenterMode.RELATIVE_TO_ARC_START`. Trenutačno nije ponuđen drugi mod. Fizičke mogućnosti `ARC_MOVES_G02_G03` i `RELATIVE_ARC_CENTER_IJ` nalaze se u zasebnom skupu potvrda, pa njihov izostanak ne sprečava softverski output niti konfiguracija sama predstavlja fizičku potvrdu.

`RichAutoA11GCodeGenerator` zadržava provjeru da je putanja povezana i zatvorena, ali više ne odbija `ArcSegment`. Za `LineSegment` emitira G01 do već postojećeg kraja. Za luk bira G02 kada je smjer `CLOCKWISE`, odnosno G03 kada je `COUNTERCLOCKWISE`. Završni X/Y uzima iz `arc.end()`, a I/J računa kao razliku već postojećeg centra i početka: `center.x - start.x` i `center.y - start.y`. Prvi rezni segment svakog prolaza i dalje dobiva F samo kada profil uključuje feed output.

Postojeća reprezentacija kruga nije promijenjena: `ToolPathService` i dalje daje dvije polukružnice s različitim počecima i krajevima. Generator ne računa promjer, radijus, centar, sweep ni nove točke te ne pokušava emitirati full-circle zapis.

## Razlog odabranog rješenja

Relativni I/J offseti izravno koriste potpune podatke koje već nosi `ArcSegment` i ostaju jednaki nakon translacije cijele putanje. Dvije polukružnice daju nedvosmislene završne točke i zasebne I/J vrijednosti pa ih je jednostavno testirati kao dva tekstualna retka. Eksplicitni profile enum čini konvenciju vidljivom bez tvrdnje da ju konkretni kontroler fizički prihvaća.

## Arhitektonska povezanost

Promjena je ograničena na `gcode` sloj i njegove testove. Geometry sloj i `ToolPathService` nisu mijenjani. Generator samo čita `PathSegment` podatke, ne računa Shape geometriju, layout ili kompenzaciju alata i ne pristupa UI-ju, SQL-u ni persistenceu. `Main` nije mijenjan.

## Važne odluke i ograničenja

- G90 se odnosi na završni X/Y, dok su I/J relativni početku pojedinog luka prema `ArcCenterModeu`.
- `CLOCKWISE -> G02`, `COUNTERCLOCKWISE -> G03` softversko je mapiranje.
- Krug ostaje dvije postojeće polukružnice; `ArcSegment` i dalje zabranjuje `start == end`.
- Generator ne provjerava jednakost početnog i završnog radijusa i ne rekonstruira kružnicu.
- Testni profili koriste `RELATIVE_TO_ARC_START`, označene `SOFTWARE_TEST_*` vrijednosti i prazan skup fizičkih potvrda.
- Apsolutni I/J, R format, linearna aproksimacija, tool compensation i druge arc konvencije nisu implementirane.
- G02/G03, I/J smjer, stvarni sweep i ponašanje na ZK-1325 / RichAuto A11 nisu fizički testirani.
- `00_odluke.md` je ažuriran jer I/J reprezentacija i direction mapiranje postaju trajni ugovor između geometry i G-code sloja.

## Build i testiranje

### Izvršene naredbe

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-26.0.2.1'
$mavenCommand = Join-Path $env:ProgramFiles 'JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" '-Dtest=RichAutoA11ProfileTest,RichAutoA11GCodeGeneratorTest,RichAutoA11ProgramEnvelopeTest' test
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean test
```

Prva ciljana naredba izvršena je dvaput: jednom prije i jednom nakon ispravka compile-time definite-assignment problema. Maven je izvršen IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Prvi ciljani pokušaj zaustavljen je pri kompilaciji glavnog koda: compiler nije mogao dokazati da su dvije lokalne I/J varijable inicijalizirane nakon statement `switcha`; testovi nisu pokrenuti. Nakon zamjene iscrpnim `switch` izrazima ista ciljana naredba završila je s `BUILD SUCCESS`: 19 testova, 0 failurea, 0 errora i 0 preskočenih testova.

Generator testovi softverski potvrđuju šest G03 redaka kroz tri step-down prolaza, relativne vrijednosti `I5.000 J0.000` i `I-5.000 J0.000`, modalni F samo na prvom luku prolaza te dva G02 retka za suprotni smjer. Profile testovi potvrđuju da je arc-center mode obvezan i fizički nepotvrđen.

Završni `clean test` od nule je kompilirao 56 glavnih i 19 testnih izvora te završio s `BUILD SUCCESS`: 92 testa, 0 failurea, 0 errora i 0 preskočenih testova.

### Što nije testirano

Generirani program nije izvezen u `.nc` datoteku ni pokrenut na CNC stroju. Nisu fizički potvrđeni G02/G03, relativni I/J, CW/CCW sweep, G17 ravnina, Z konvencija, feedovi ni cjelokupni program na ZK-1325 / RichAuto A11. Nije provedeno neovisno geometric tolerance ili controller parser testiranje proizvoljnih lukova.

## Otvorena pitanja

- Fizički test na ciljnom stroju mora zasebno potvrditi prihvaća li konkretna A11 konfiguracija relativni I/J i mapira li G02/G03 na očekivani sweep.
- RIJEŠENO U 9.5: odvojeni `.nc` export zapisuje isti `GCodeProgram` bez mijenjanja arc konvencije; service/UI koordinacija još nije implementirana.

## Moguće poglavlje završnog rada

Mapiranje geometrijskih kružnih lukova u determinističan CNC program.

## Kandidati za isječke koda

### Kandidat: ArcSegment u relativni I/J G-code

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java`
**Klasa/metoda:** `RichAutoA11GCodeGenerator#arcMove`
**Zašto je važan:** Prikazuje izravno mapiranje postojećeg kraja, centra i smjera luka bez računanja kružnice u generatoru.
**Moguće poglavlje:** Generiranje kružnih G-code pomaka.

```java
double centerOffsetI = switch (profile.arcCenterMode()) {
    case RELATIVE_TO_ARC_START -> arc.center().x() - arc.start().x();
};
double centerOffsetJ = switch (profile.arcCenterMode()) {
    case RELATIVE_TO_ARC_START -> arc.center().y() - arc.start().y();
};

String command = switch (arc.direction()) {
    case CLOCKWISE -> "G02";
    case COUNTERCLOCKWISE -> "G03";
};
```

**Ideja opisa u radu:** Softver uzima geometrijske podatke iz `ArcSegmenta`, pretvara centar u relativni offset i bira naredbu prema eksplicitnom smjeru bez rekonstruiranja izvornog kruga.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
