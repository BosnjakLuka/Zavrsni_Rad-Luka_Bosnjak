# 18.A — Audit aplikacijskog `.nc` koda i fizička potvrda referentnih programa

**Datum:** 2026-08-27  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / PROGRAMERSKI PREGLEDANO / REFERENTNI I APLIKACIJSKI PROGRAM FIZIČKI TESTIRANI / GATE 1 ZATVOREN / SPREMNO ZA ITERACIJU 2

## Cilj

Usporediti `.nc` kod koji je aplikacija spremila u `Dokumentacija/testni_cnc_kodovi/` s referentnim i ranije ručno programiranim kodovima za ZK-1325 / RichAuto A11 te donijeti odluku o njegovoj spremnosti za fizički test.

## Promijenjene datoteke

- `Dokumentacija/biljeske/18-04-audit-aplikacijskog-nc-koda-i-fizicka-potvrda.md` — novi audit i odluka.
- `Dokumentacija/biljeske/00_indeks.md` — dodan indeksni zapis za ovaj korak.
- `Dokumentacija/biljeske/00_odluke.md` — dodana potvrđena odluka o statusu aplikacijskog izlaza.

Postojeći `.nc` programi nisu mijenjani.

## Stvarna implementacija

Aplikacija u trenutnom release candidateu generira jedan zatvoren i povezan toolpath, primjenjuje aplikacijski geometrijski offset alata te generira RichAuto profilni izlaz. Datoteka `Dokumentacija/testni_cnc_kodovi/test01.nc` stvarno sadrži:

```text
G21
G17
G90
G54
M03
G00 Z5.000
G00 X3.000 Y3.000
G01 Z-1.000 F150.000
G01 X97.000 Y3.000 F500.000
G01 X97.000 Y97.000
G01 X3.000 Y97.000
G01 X3.000 Y3.000
G00 Z5.000
M05
M30
```

Kod koristi milimetre, G17 XY ravninu, apsolutno pozicioniranje, G54, pokretanje i zaustavljanje spindle-a, safe Z, plunge feed, rezni feed i ispravan završetak programa. Ne sadrži `G41`, `G42`, `G40`, `D` ni `S` naredbu.

Usporedba s `Dokumentacija/reference/` pokazuje istu osnovnu CNC strukturu kao u referentnim programima: `G90`, `G17`, `G21`, `G54`, feed naredbe, spindle naredbe, Z prolaze, `G01`/`G02`/`G03` putanje i `M30`. Razlike u koordinatama, zaglavlju i prisutnosti `S` ovise o konkretnom programu i nisu same po sebi greška.

Programer je pregledao aplikacijski kod i potvrdio da je `test01.nc` dobar te da odgovara obrascu kodova koji se koriste za taj CNC.

Postojeći ručno programirani kodovi iz referentnog skupa prethodno su prebačeni na USB i fizički pokrenuti na ZK-1325 s RichAuto A11 kontrolerom. Prema dostavljenoj potvrdi, radili su bez problema.

## Odluka audita

`test01.nc` je na temelju statičkog pregleda, usporedbe s referentnim kodovima i programerske potvrde ocijenjen kao **strukturno ispravan i prikladan kandidat za fizički test** na ZK-1325 / RichAuto A11.

Nije pronađen nedostatak u trenutnom G-kodu koji bi opravdao izmjenu generatora. `test01.nc` je nakon pregleda fizički pokrenut na ciljanom stroju i test je prema potvrdi operatora prošao bez problema.

## Razlike i tumačenje

| Stavka | Aplikacijski `test01.nc` | Referentni/ručni kodovi | Zaključak |
|---|---|---|---|
| Jedinice | `G21` | prisutno u većini referenci; izostavljeno u kratkoj pravokutnoj referenci | Nije problem ako je ponašanje potvrđeno na stroju |
| Pozicioniranje | `G90` | `G90` ili `G90 G94` | Ista osnovna konvencija XY koordinata |
| WCS | `G54` | `G54` | Podudara se po strukturi; konkretan work zero mora biti namješten |
| Feed | `F150`, `F500` | različite stvarne vrijednosti po programu | Vrijednosti nisu univerzalne; potvrđuju se za konkretan posao |
| Spindle | `M03`, `M05`, bez `S` | neki programi imaju `S`, neki kratki programi nemaju | Ne dodavati `S` bez potvrde operatora |
| Kompenzacija | aplikacijski offset, bez `G41/G42/G40` | u dostavljenim kodovima nema `G41/G42/G40` | Ne uvoditi controller-side kompenzaciju |
| Završetak | `M30` | `M30` | Podudara se |

## Razlog odabranog rješenja

Aplikacijski izlaz ne mora biti byte-for-byte kopija ručnog programa. Bitno je da poštuje potvrđeni format, geometriju, redoslijed sigurnih pokreta i parametre konkretnog zadatka. Trenutni izlaz to čini, a razlika u koordinatama proizlazi iz aplikacijske kompenzacije putanje centra alata.

Ne zaključavaju se nove fizičke vrijednosti. Referentni preset `18000 / 500 / 150 / 1 / 1 / 5` ostaje softverska testna vrijednost dok operator ne potvrdi stvarne uvjete.

## Arhitektonska povezanost

Generator u `gcode` sloju emitira tekst iz već pripremljenog `ToolPatha`. Kompenzacija promjera alata ostaje u `geometry` sloju, a servis koordinira validaciju, kompenzaciju i generiranje. Audit ne mijenja te odgovornosti niti uvodi controller-side `G41/G42/G40` logiku.

## Važne odluke i ograničenja

- Ručno programirani referentni kodovi fizički su radili na ZK-1325 / RichAuto A11 prema dostavljenoj potvrdi.
- `test01.nc` je programerski pregledan i ocijenjen dobrim kandidatom za fizički test.
- `test01.nc` je fizički pokrenut na ZK-1325 / RichAuto A11 i test je prošao bez problema prema potvrdi operatora.
- Stvarni stroj, kontroler, radno područje, orijentaciju X/Y/Z osi, work zero, read/ignore postavke, alat i machining parametre operator mora potvrditi za konkretni test.
- Ne uvodi se `G41`, `G42`, `G40`, `D` ni `S` naredba samo na temelju sličnosti s drugim programima.

## Build i testiranje

### Izvršene naredbe

```text
Get-Content Dokumentacija/testni_cnc_kodovi/*.nc
Get-Content Dokumentacija/reference/*.nc
PowerShell sažetak prisutnosti G/M naredbi u testnim i referentnim datotekama
git -c safe.directory=... log -1 --format=...
```

### Stvarni rezultat

Pregled je pronašao jednu aplikacijsku datoteku `test01.nc` od 15 redaka. Sadrži `G21`, `G17`, `G90`, `G54`, `M03`, `M05` i `M30`, bez `G40`, `G41`, `G42` i `S`. Struktura se podudara s kratkim pravokutnim referentnim programom, uz očekivani aplikacijski offset koordinata.

Trenutni commit je `09ba26bc40c4549fa972d4c3a04c932e00aa4409` (`bugs and fixes`).

Prema naknadnoj potvrdi operatora, aplikacijski program `test01.nc` učitan je na ZK-1325 s kontrolerom RichAuto A11 i fizički test završen je bez problema. Time su 18.3 i 18.4 završeni, a Gate 1 je zatvoren. Generator se ne mijenja.

### Što nije testirano

- U ovoj bilješci nisu navedene konkretne brojčane vrijednosti alata, work zeroa i machining parametara jer nisu dostavljene u tekstualnoj potvrdi; fizički test je prema potvrdi operatora prošao bez problema.
- `S` nije bio dio aplikacijskog `test01.nc` izlaza.
- Controller-side `G40` nije korišten jer aplikacija koristi geometrijski offset bez `G41/G42/G40`.

## Otvorena pitanja i nedostaci za popravak

Nedostatak u samom G-kodu nije pronađen. Fizički test je završen uspješno i projekt je spreman za Iteraciju 2. Za potpunu naknadnu sljedivost može se još dopuniti operativni zapis:

- [x] učitati baš `test01.nc` na ZK-1325 / RichAuto A11;
- [x] izvršiti kontrolirani test prema proceduri operatora;
- [x] potvrditi da je fizički rezultat prošao bez problema;
- [ ] naknadno upisati konkretan tool number, promjer, work zero, orijentaciju osi, read/ignore postavke i brojčane machining vrijednosti ako se zahtijeva detaljan proizvodni zapis.

Ako se u Iteraciji 2 pojavi problem, prvo treba utvrditi je li uzrok profil naredbi, work zero, Z konvencija, alat/offset ili machining parametri prije izmjene generatora.

## Status za Iteraciju 2

Gate 1 je zatvoren: aplikacijski `.nc` može se učitati i izvršiti na ciljanom stroju, putanja je prošla fizički test bez problema, a generator nije zahtijevao korekciju. Projekt je spreman za planiranje i implementaciju Koraka 19 — quantity / početak Iteracije 2.

## Moguće poglavlje završnog rada

Validacija G-code generatora, usporedba s referentnim programima i fizička provjera CNC izlaza.

## Kandidati za isječke koda

### Kandidat: Aplikacijska kompenzacija putanje alata

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java`  
**Klasa/metoda:** `ProgramGenerationService#generate`  
**Zašto je važan:** Pokazuje redoslijed validacije, generiranja nominalne geometrije, kompenzacije prema alatu, provjere uklapanja i generiranja G-koda.  
**Moguće poglavlje:** Arhitektura generatora i geometrijska kompenzacija

```java
ToolPath programmedContour = toolPathService.generate(request.shape());
ToolPath cutterCenterPath = toolPathCompensationService.compensate(
        programmedContour, request.tool(), request.cutSide());
singleShapeFitValidator.validate(cutterCenterPath, request.materialSheet(), request.machine());
return gCodeGenerator.generate(cutterCenterPath, request.machiningParameters());
```

**Ideja opisa u radu:** G-code generator prima već pripremljenu i provjerenu putanju centra alata, pa tekstualni sloj ne računa geometrijski offset.

### Kandidat: RichAuto programski okvir

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java`  
**Klasa/metoda:** `RichAutoA11GCodeGenerator#generate`  
**Zašto je važan:** Prikazuje siguran redoslijed safe Z, XY pozicioniranja, plungiranja, rezanja, retracta i završetka programa.  
**Moguće poglavlje:** Generiranje i provjera CNC programa

```java
lines.addAll(programEnvelope.headerLines(parameters));
lines.add(rapidZ(safeZ));
for (double passDepth : passDepths) {
    lines.add(rapidXy(startPoint));
    lines.add(plunge(profile.zCoordinateConvention().toCutZ(passDepth),
            parameters.getPlungeRate()));
    lines.addAll(cuttingMoves(segments, parameters.getFeedRate()));
    lines.add(rapidZ(safeZ));
}
lines.addAll(programEnvelope.footerLines());
```

**Ideja opisa u radu:** Generator deterministički sastavlja program iz profila, dubinskih prolaza i zatvorene toolpath putanje.

## Kandidat za sliku, dijagram ili tablicu

Tablica usporedbe `test01.nc`, kratke pravokutne reference i fizički korištenih programa. Tablica je uključena u ovu bilješku.

## Git commit/hash

`09ba26bc40c4549fa972d4c3a04c932e00aa4409` — stanje repozitorija u trenutku audita.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
