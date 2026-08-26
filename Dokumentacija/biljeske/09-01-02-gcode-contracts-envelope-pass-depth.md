# 9.1–9.2 — G-code ugovori, RichAuto profil, programski okvir i step-down

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Uvesti osnovni G-code ugovor i determinističan tekstualni format, eksplicitno odvojiti konfigurirano emitiranje od fizičke potvrde RichAuto ponašanja te pripremiti header/footer i čisti izračun dubinskih prolaza bez preuranjenog generiranja ToolPath naredbi.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/gcode/GCodeProgram.java` i `GCodeGenerator.java` — dodani su nepromjenjivi rezultat i ugovor budućeg generatora.
- `src/main/java/hr/lukabosnjak/gcode/GCodeFormatter.java` — dodano je determinističko formatiranje G-code brojeva.
- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java` — dodana je konfiguracija outputa, fizičkih potvrda i Z koordinatne konvencije.
- `src/main/java/hr/lukabosnjak/gcode/RichAutoA11ProgramEnvelope.java` — dodana je konfigurabilna izrada header/footer redaka.
- `src/main/java/hr/lukabosnjak/gcode/PassDepthCalculator.java` — dodan je čisti izračun pozitivnih kumulativnih dubina.
- `src/test/java/hr/lukabosnjak/gcode/` — dodano je pet unit test razreda za novi sloj.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — dokumentirane su nove odluke i stvarni rezultati provjere.

## Stvarna implementacija

`GCodeProgram` čuva obrambeno kopiran, neprazan popis redaka bez ugrađenih line separatora. Metoda `text()` ne koristi platformski separator nego spaja retke s LF znakom i dodaje točno jedan završni LF. `GCodeGenerator` definira budući tok `ToolPath + MachiningParameters -> GCodeProgram`, ali ovaj milestone namjerno nema konkretnu implementaciju koja bi vratila program bez putanje.

`GCodeFormatter` koristi `BigDecimal.valueOf`, `RoundingMode.HALF_UP` i `toPlainString`. Rezultat ima točno konfiguriran broj decimala, decimalnu točku neovisnu o hrvatskom Localeu, nema scientific notation ni negativnu nulu. Negativna preciznost i nefinite vrijednosti se odbijaju.

`RichAutoA11Profile` odvojeno čuva četiri opcije emitiranja (`F`, `S`, `G54`, spindle naredbe), milimetre, apsolutno pozicioniranje, numeričku preciznost, Z konvenciju i nepromjenjiv skup fizički potvrđenih mogućnosti. Prazan skup ne znači da se naredba ne smije softverski emitirati, nego samo da nije evidentirana fizička potvrda. Dvije Z konvencije pretpostavljaju nulu na površini materijala i eksplicitno biraju pozitivan ili negativan smjer rezanja; druga vrijednost nikada se ne izvodi skriveno iz `MachiningParameters`.

`RichAutoA11ProgramEnvelope` deterministički vraća `G21`, `G17`, `G90`, opcionalni `G54`, opcionalni zasebni `S` i opcionalni `M03`. Footer sadrži `M05` samo kada su spindle naredbe uključene, a uvijek završava s `M30`. `F` se ne emitira prije stvarnog plunge ili reznog pomaka. `PassDepthCalculator` radi s decimalnim reprezentacijama pozitivnih `cutDepth` i `stepDown` veličina, vraća nepromjenjivu listu i kao zadnji element uvijek koristi točan ciljni `cutDepth`.

## Razlog odabranog rješenja

Programski okvir je odvojen od konkretnog generatora kako milestone 9.2 ne bi predstavljao header i footer bez rezne putanje kao valjan dovršen CNC program. Profil sadržava softversku konfiguraciju i evidenciju fizičke potvrde kao različite podatke, čime testovi outputa ne postaju tvrdnja o stvarnom kontroleru. Pozitivne dubine ostaju jednostavne domenske veličine, dok profil jedini poznaje njihovo pretvaranje u koordinatu.

## Arhitektonska povezanost

Sve nove produkcijske klase nalaze se u potvrđenom `gcode` sloju. Ugovor generatora smije konzumirati postojeći `ToolPath` i `MachiningParameters`, ali nove klase ne računaju geometriju, layout, SQL ni persistence. `Main`, JavaFX UI, domenski modeli i geometry implementacija nisu mijenjani.

## Važne odluke i ograničenja

- `G21`, `G17`, `G90` i `M30` dio su implementiranog softverskog programskog okvira, ne dokaz fizičke kompatibilnosti.
- `G54`, `S`, `M03` i `M05` ovise samo o opcijama emitiranja; fizičke potvrde ne mijenjaju output.
- Podržani su samo milimetri i apsolutno pozicioniranje; nisu implementirane G20 konverzije ni G91 relativni pomaci.
- Odabrana Z konvencija je konfiguracija generatora. Z-smjer i work zero nisu fizički testirani.
- Nisu implementirani konkretni `RichAutoA11GCodeGenerator`, `G00`, `G01`, `G02`, `G03`, plunge, retract ni povezivanje pass-depth rezultata s putanjom.
- `00_odluke.md` je ažuriran jer razdvajanje emitiranja/fizičke potvrde i Z koordinatna konvencija predstavljaju važne arhitektonske i sigurnosne odluke.

## Build i testiranje

### Izvršene naredbe

```powershell
mvn -Dtest=GCodeProgramTest,GCodeFormatterTest,RichAutoA11ProfileTest,RichAutoA11ProgramEnvelopeTest,PassDepthCalculatorTest test
mvn '-Dtest=GCodeProgramTest,GCodeFormatterTest,RichAutoA11ProfileTest,RichAutoA11ProgramEnvelopeTest,PassDepthCalculatorTest' test
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-26.0.2.1'
$mavenCommand = Join-Path $env:ProgramFiles 'JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" '-Dtest=GCodeProgramTest,GCodeFormatterTest,RichAutoA11ProfileTest,RichAutoA11ProgramEnvelopeTest,PassDepthCalculatorTest' test
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean test
```

Maven je izvršen IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Prva ciljana naredba nije pokrenula Maven jer je PowerShell zareze protumačio kao vlastitu sintaksu. U drugom pokušaju argument je ispravno naveden, ali globalni `mvn` nije bio dostupan. Prvi pokušaj bundled Mavena zaustavljen je prije builda jer sandbox nije mogao stvoriti zadani lokalni repository na `C:\.m2`; nakon eksplicitnog odabira postojećeg korisničkog repositoryja ciljana provjera završila je s `BUILD SUCCESS`: 22 testa, 0 failurea, 0 errora i 0 preskočenih testova.

Završni `clean test` od nule je kompilirao 55 glavnih i 18 testnih izvora te završio s `BUILD SUCCESS`: 84 testa, 0 failurea, 0 errora i 0 preskočenih testova.

### Što nije testirano

Nije generirana niti izvezena `.nc` datoteka. Header/footer još nije sastavljen s ToolPath naredbama, plunge/retract tokom ni pass-depth Z koordinatama. Nisu fizički testirani `F`, `S`, `G54`, `G21`, `G17`, `G90`, `M03`, `M05`, `M30`, Z-smjer, work zero ni bilo kakvo ponašanje na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Milestone 9.3 treba implementirati konkretni single-element generator i povezati header, putanju, step-down dubine i footer.
- Fizičke mogućnosti profila ostaju nepotvrđene dok se ne provede i zasebno dokumentira test na ciljnom stroju.

## Moguće poglavlje završnog rada

Konfigurabilno i determinističko generiranje CNC programa uz odvajanje softverskog outputa od fizičke verifikacije kontrolera.

## Kandidati za isječke koda

### Kandidat: Determinističan RichAuto programski okvir

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11ProgramEnvelope.java`
**Klasa/metoda:** `RichAutoA11ProgramEnvelope#headerLines`
**Zašto je važan:** Prikazuje jasan redoslijed obveznih i profilom uključenih naredbi bez miješanja s ToolPath geometrijom.
**Moguće poglavlje:** Struktura i konfiguracija G-code programa.

```java
lines.add(unitsCommand(profile.units()));
lines.add("G17");
lines.add(positioningCommand(profile.positioningMode()));
if (profile.emitG54()) {
    lines.add("G54");
}
if (profile.emitSpindleSpeed()) {
    lines.add("S" + formatter.format(parameters.getSpindleSpeed()));
}
if (profile.emitSpindleCommands()) {
    lines.add("M03");
}
```

**Ideja opisa u radu:** Profil određuje tekst koji softver emitira, dok zasebna evidencija fizičkih potvrda ne mijenja ovaj redoslijed.

### Kandidat: Izračun završnog step-down prolaza

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/PassDepthCalculator.java`
**Klasa/metoda:** `PassDepthCalculator#calculate`
**Zašto je važan:** Pokazuje kako se iz pozitivnih veličina dobivaju kumulativne dubine bez dodatnog floating-point prolaza i uz točan završni cilj.
**Moguće poglavlje:** Planiranje dubinskih prolaza CNC obrade.

```java
BigDecimal[] division = target.divideAndRemainder(step);
BigInteger passCount = division[0].toBigIntegerExact();
if (division[1].signum() != 0) {
    passCount = passCount.add(BigInteger.ONE);
}
for (int pass = 1; pass < count; pass++) {
    depths.add(step.multiply(BigDecimal.valueOf(pass)).doubleValue());
}
depths.add(cutDepth);
```

**Ideja opisa u radu:** Ciljna dubina dodaje se jednom kao zadnji element, pa djeljivi i nedjeljivi omjeri ne stvaraju prolaz nakon cilja.

### Kandidat: Eksplicitno mapiranje domenske dubine u Z koordinatu

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java`
**Klasa/metoda:** `RichAutoA11Profile.ZCoordinateConvention#toCutZ` i `toSafeZ`
**Zašto je važan:** Vidljivo odvaja pozitivne domenske veličine od odabrane koordinatne konvencije i sprječava skrivenu pretpostavku o Z-smjeru.
**Moguće poglavlje:** Koordinatne konvencije CNC programa.

```java
MATERIAL_SURFACE_ZERO_NEGATIVE_CUT(-1.0, 1.0),
MATERIAL_SURFACE_ZERO_POSITIVE_CUT(1.0, -1.0);

public double toCutZ(double positiveDepth) {
    return signedCoordinate(positiveDepth, cutSign, "Cut depth");
}
```

**Ideja opisa u radu:** Ista pozitivna dubina može se mapirati na različit predznak bez promjene domenskog modela; izbor i fizička potvrda ostaju odvojeni.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
