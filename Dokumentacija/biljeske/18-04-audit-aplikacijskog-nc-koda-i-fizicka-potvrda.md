# 18.4 — Audit aplikacijskog `.nc` koda i fizička potvrda referentnih programa

**Datum:** 2026-08-27  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / PROGRAMERSKI PREGLEDANO / REFERENTNI PROGRAMI FIZIČKI TESTIRANI / APLIKACIJSKI `test01.nc` NIJE FIZIČKI TESTIRAN

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

Nije pronađen nedostatak u trenutnom G-kodu koji bi opravdao izmjenu generatora prije prvog fizičkog pokretanja tog konkretnog programa. Kod je vrlo sličan fizički korištenim programima i razumno je očekivati da će se izvršavati na istom stroju, ali to se ne smije zapisati kao fizički potvrđena činjenica dok se `test01.nc` stvarno ne učita i pokrene.

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
- `test01.nc` nije označen kao fizički testiran jer u ovom koraku nema zabilježenog stvarnog učitavanja i pokretanja te datoteke na stroju.
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

### Što nije testirano

- Nije fizički pokrenut konkretni `test01.nc` na stroju u okviru ovog audita.
- Nisu za ovaj konkretni posao zapisani stvarni tool number, promjer alata, work zero, orijentacija osi, radno područje ni machining vrijednosti.
- Nije potvrđeno ponašanje `S` zato što ga `test01.nc` ne emitira.
- Nije potvrđeno controller-side čitanje ili ignoriranje `G40`, jer ga aplikacija ne emitira.

## Otvorena pitanja i nedostaci za popravak

Nedostatak u samom G-kodu nije pronađen. Prije fizičkog testa nedostaje operativni zapis:

- [ ] učitati baš `test01.nc` na ZK-1325 / RichAuto A11;
- [ ] potvrditi stvarni alat i izmjereni promjer;
- [ ] potvrditi WCS/work zero i Z nulu;
- [ ] potvrditi X/Y/Z orijentaciju;
- [ ] potvrditi stvarne `F`, `S`, `G54`, `G40` read/ignore postavke;
- [ ] potvrditi safe Z, cut depth i step-down;
- [ ] izvršiti dry-run/simulaciju i zatim fizički test prema pravilima operatora;
- [ ] zapisati rezultat i eventualne korekcije.

Ako fizički test pokaže problem, prvo treba utvrditi je li uzrok profil naredbi, work zero, Z konvencija, alat/offset ili machining parametri prije izmjene generatora.

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
