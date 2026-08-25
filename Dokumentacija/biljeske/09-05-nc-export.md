# 9.5 — Odvojeni `.nc` export service

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO NA STROJU

## Cilj

Omogućiti odvojeno zapisivanje već generiranog `GCodeProgram` sadržaja u plain-text `.nc` datoteku s eksplicitnim ASCII charsetom i odredištem koje će kasnije odabrati UI, bez automatskog pristupa USB uređajima.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/gcode/NcExportService.java` — dodan je servis za validiran i eksplicitan US-ASCII zapis na predani path.
- `src/test/java/hr/lukabosnjak/gcode/NcExportServiceTest.java` — dodani su temp-file round-trip, ekstenzija, charset i overwrite testovi.
- `Dokumentacija/biljeske/09-04-arc-gcode.md`, `00_odluke.md`, `00_indeks.md` i ova bilješka — dokumentiran je export ugovor i riješeno prethodno otvoreno pitanje.

## Stvarna implementacija

`NcExportService#export` prima ne-null `GCodeProgram` i odredišni `Path`. Naziv mora završavati `.nc` ekstenzijom uz case-insensitive provjeru, pa su prihvaćeni i `.nc` i `.NC`, dok se nazivi bez te završne ekstenzije odbijaju prije zapisa.

Servis uzima već determinističan `program.text()` i kodira ga pomoću eksplicitnog `StandardCharsets.US_ASCII` encodera s `CodingErrorAction.REPORT`. Cijeli tekst kodira se prije otvaranja odredišne datoteke, zato ne-ASCII znak uzrokuje `IllegalArgumentException` bez djelomične datoteke ili tihe zamjene znaka. Dobiveni bajtovi zapisuju se bez BOM-a pomoću `CREATE_NEW` i `WRITE`; postojeća datoteka se ne prepisuje.

Brojevi se u exportu ne parsiraju niti ponovno formatiraju. Decimalna točka i preciznost dolaze iz postojećeg `GCodeFormattera`, a servis bajtovski čuva konačni tekst. Ne pretražuje diskove, ne bira USB, ne stvara direktorije i ne mijenja predani path.

## Razlog odabranog rješenja

Odvajanje generiranja od datotečnog zapisa zadržava `GCodeProgram` kao jedini prijenosni rezultat i omogućuje da budući UI odabere lokaciju bez unošenja JavaFX odgovornosti u generator. US-ASCII obuhvaća standardne znakove korištene u trenutačnom G-code outputu i uklanja ovisnost o platformskom charsetu. Prethodno kodiranje i create-new zapis sprječavaju djelomične ili nenamjerno prepisane datoteke.

## Arhitektonska povezanost

Servis pripada postojećem `gcode` sloju jer zapisuje gotov CNC program. Ne ovisi o JavaFX-u, controlleru, domeni, geometryju, layoutu, SQL-u ni persistenceu. UI će kasnije koordinirati izbor patha i prikaz I/O pogreške. `Main` nije mijenjan.

## Važne odluke i ograničenja

- `.nc` provjera odnosi se na zadnju ekstenziju i nije osjetljiva na veličinu slova.
- Charset je eksplicitno US-ASCII, bez BOM-a; ne-ASCII sadržaj se odbija umjesto zamjene.
- Servis čuva postojeći LF tekst iz `GCodePrograma` i ne koristi default Locale za brojeve.
- Postojeće odredište se ne prepisuje; UI još nema potvrđeni overwrite tok.
- Roditeljski direktorij mora već postojati; servis ga ne stvara.
- Zapisuje se samo na eksplicitno predani path; nema USB detekcije ni automatskog kopiranja.
- Nisu implementirani JavaFX file chooser, korisničke poruke, service workflow ni spremanje odredišnog patha u bazu.
- Stvarni `.nc` sadržaj nije učitan ni izvršen na ZK-1325 / RichAuto A11.
- `00_odluke.md` je ažuriran jer charset, odredište i overwrite ponašanje čine javni ugovor izvoza.

## Build i testiranje

### Izvršene naredbe

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-26.0.2.1'
$mavenCommand = Join-Path $env:ProgramFiles 'JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" '-Dtest=NcExportServiceTest' test
& $mavenCommand "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean test
```

Maven je izvršen IntelliJ bundled Mavenom uz potvrđeni projektni OpenJDK 26 SDK i postojeći lokalni Maven repository.

### Stvarni rezultat

Ciljani `NcExportServiceTest` završio je s `BUILD SUCCESS`: 5 testova, 0 failurea, 0 errora i 0 preskočenih testova. JUnit `@TempDir` test zapisao je `.nc` datoteku, pročitao ju natrag kao US-ASCII i usporedio tekst i bajtove. Pod hrvatskim default Localeom sačuvane su decimalne točke. Dodatno su potvrđeni `.NC`, odbijanje pogrešne ekstenzije i ne-ASCII sadržaja te očuvanje postojeće datoteke.

Završni `clean test` od nule je kompilirao 57 glavnih i 20 testnih izvora te završio s `BUILD SUCCESS`: 97 testova, 0 failurea, 0 errora i 0 preskočenih testova.

### Što nije testirano

Nije korišten JavaFX file chooser, stvarni korisnički direktorij, USB uređaj ni drugi removable media. Nisu testirane dozvole stvarnog filesystema, nedostatak prostora, mrežni path, fizički prijenos na kontroler ni prihvaćanje datoteke na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Budući UI treba odabrati `.nc` destination path i korisniku jasno prikazati invalid extension, postojeću datoteku i I/O pogreške.
- Potvrđeni overwrite tok može se dodati tek kada UI ima eksplicitnu korisničku potvrdu.

## Moguće poglavlje završnog rada

Izvoz determinističnog CNC programa u prijenosnu `.nc` datoteku.

## Kandidati za isječke koda

### Kandidat: Validirani US-ASCII `.nc` zapis

**Datoteka:** `src/main/java/hr/lukabosnjak/gcode/NcExportService.java`
**Klasa/metoda:** `NcExportService#export` i `encodeAscii`
**Zašto je važan:** Prikazuje odvajanje tekstualnog programa od filesystem zapisa, eksplicitni charset i zaštitu od nevaljanog sadržaja ili tihog prepisivanja.
**Moguće poglavlje:** Izvoz i prijenos CNC programa.

```java
requireNcExtension(destination);
byte[] content = encodeAscii(program.text());
Files.write(
        destination,
        content,
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE);
```

**Ideja opisa u radu:** Tek nakon uspješne provjere ekstenzije i potpunog ASCII kodiranja servis stvara novu datoteku na točno predanom odredištu.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
