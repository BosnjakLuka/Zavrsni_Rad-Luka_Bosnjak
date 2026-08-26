# 11.2 — Obrasci za materijal, CNC stroj i alat

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Omogućiti dodavanje vrste materijala, CNC stroja i alata kroz aplikaciju kako bi se novi zapisi odmah mogli odabrati u glavnom obrascu, bez ručnog SQL-a i bez izmišljanja fizičkih podataka ZK-1325 stroja ili glodala.

## Promijenjene datoteke

- `src/main/resources/hr/lukabosnjak/ui/view/` — dodana su tri FXML modalna obrasca, a glavnom obrascu dodani su gumbi za njihovo otvaranje.
- `src/main/java/hr/lukabosnjak/ui/controller/` — dodani su controlleri obrazaca i koordinacija otvaranja, spremanja te automatskog odabira novog zapisa.
- `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java` — dodana je orkestracija stvaranja i spremanja referentnih podataka.
- `src/main/java/hr/lukabosnjak/validation/ReferenceDataValidator.java` — dodana su pravila obveznih tekstualnih polja, pozitivnih veličina i raspona vretena.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` — novi servis i controlleri povezani su u postojećem composition rootu.
- `src/test/java/hr/lukabosnjak/` — dodani su testovi validacije, service ponašanja, composition roota i FXML/controller ugovora.
- `Dokumentacija/biljeske/00_odluke.md` i `00_indeks.md` — zabilježeni su odluka i ovaj razvojni korak.

## Stvarna implementacija

Uz padajuće izbornike materijala, stroja i alata nalaze se gumbi `Dodaj...`. Svaki gumb otvara zaseban modalni FXML obrazac. Materijal prima naziv i neobavezan opis. Stroj prima identifikacijske podatke, radne raspone, maksimalni posmak i raspon vretena. Alat se dodaje isključivo za prethodno odabrani spremljeni stroj te prima broj, naziv, slobodni tekstualni tip, promjer, reznu duljinu i broj oštrica; novi alat sprema se kao aktivan.

Nakon uspješnog spremanja vraćeni domenski objekt dodaje se u odgovarajući `ComboBox` i automatski odabire. `ReferenceDataManagementService` normalizira tekst, odbija duplikat naziva materijala ili stroja bez obzira na velika/mala slova te odbija ponovljeni broj alata unutar istog stroja. Postojeći JDBC repositoryji i `PreparedStatement` upiti ostaju jedino mjesto stvarnog SQL spremanja.

## Razlog odabranog rješenja

Modalni prozori zadržavaju korisnika u glavnom toku izrade programa, a nakon spremanja odmah uklanjaju problem praznih padajućih izbornika. Poseban service i validator drže poslovna pravila i JDBC izvan JavaFX controllera. Stvarni stroj i alat unose se kroz obrazac jer kontroler RichAuto A11 i naziv modela ZK-1325 nisu dovoljni za pouzdano određivanje fizičkog glodala i graničnih vrijednosti stroja.

## Arhitektonska povezanost

FXML i controlleri pripadaju UI sloju te samo čitaju polja, otvaraju prozore i prikazuju rezultat. `ReferenceDataManagementService` koordinira validaciju i repositoryje. `ReferenceDataValidator` sadrži ponovljiva pravila ulaza, a postojeći JDBC repositoryji zadržavaju SQL. Geometry, layout i G-code slojevi nisu mijenjani.

## Važne odluke i ograničenja

- `Tool.type` ostaje potvrđeni privremeni `String`; nije uveden nepotvrđeni `ToolType` enum.
- Nije dodan defaultni stroj ni alat jer još nisu potvrđeni stvarni max feed, RPM raspon, točan Z hod i specifikacije glodala.
- Forma alata zahtijeva odabrani spremljeni stroj, a broj alata jedinstven je unutar tog stroja.
- Softverski testovi spremanja nisu fizički test stroja i ne potvrđuju RichAuto kompatibilnost, WCS ni orijentaciju osi.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ-Maven>\bin\mvn.cmd '-Dmaven.repo.local=<lokalni-maven-repozitorij>' -DskipTests compile
<IntelliJ-Maven>\bin\mvn.cmd '-Dmaven.repo.local=<lokalni-maven-repozitorij>' '-Dtest=ReferenceDataManagementServiceTest,ReferenceDataValidatorTest,NumericInputParserTest' test
<IntelliJ-Maven>\bin\mvn.cmd '-Dmaven.repo.local=<lokalni-maven-repozitorij>' '-Dtest=FxmlControllerContractTest' test
<IntelliJ-Maven>\bin\mvn.cmd '-Dmaven.repo.local=<lokalni-maven-repozitorij>' test
git diff --check
```

### Stvarni rezultat

Prvi compile unutar sandboxa nije uspio zbog zabrane zatvaranja lokalnog H2 JAR resursa; ista provjera izvan sandboxa završila je s `BUILD SUCCESS`. Ciljani testovi servicea, validatora i parsera: 10 testova, 0 failurea i 0 errora. FXML/controller contract test: 1 test, 0 failurea i 0 errora. Završni puni Maven suite: 118 testova, 0 failurea, 0 errora i `BUILD SUCCESS`. Sva četiri FXML dokumenta dodatno su uspješno parsirana kao XML, a `git diff --check` nije prijavio greške sadržaja.

### Što nije testirano

Nije izvršen vizualni JavaFX test klikanja, veličine modalnih prozora ni stvarni unos u korisničku file-based bazu. Nije dodan ni testiran stvarni ZK-1325 zapis ili montirano glodalo. Nisu testirani fizički stroj, RichAuto A11, USB prijenos, WCS, orijentacija osi ni rezanje materijala.

## Otvorena pitanja

- Potrebno je kroz nove forme unijeti potvrđene vrijednosti konkretnog ZK-1325 stroja i stvarnog glodala.
- Vizualno provjeriti prikaz sva tri modala na ciljnom računalu.

## Moguće poglavlje završnog rada

Upravljanje referentnim CNC podacima kroz slojevitu JavaFX aplikaciju.

## Kandidati za isječke koda

### Kandidat: Sigurno stvaranje alata vezanog uz stroj

**Datoteka:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java`
**Klasa/metoda:** `ReferenceDataManagementService#createTool`
**Zašto je važan:** Pokazuje odvajanje validacije, provjere jedinstvenosti i persistence poziva od UI sloja.
**Moguće poglavlje:** Service sloj i integritet referentnih podataka

```java
validator.validate(tool);
long machineId = tool.getCncMachine().getCncMachineId();
if (toolRepository.findByMachineIdAndToolNumber(machineId, tool.getToolNumber()).isPresent()) {
    throw new ValidationException("Odabrani CNC stroj već ima alat s tim brojem.");
}
return toolRepository.save(tool);
```

**Ideja opisa u radu:** Controller predaje domenski unos serviceu, a service prije spremanja potvrđuje vezu sa strojem i jedinstvenost broja alata.

### Kandidat: UI koordinacija dodavanja alata

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`
**Klasa/metoda:** `MainFormController#handleAddTool`
**Zašto je važan:** Prikazuje da JavaFX controller koordinira odabir stroja, modalni prozor i osvježavanje `ComboBoxa` bez SQL-a.
**Moguće poglavlje:** JavaFX korisničko sučelje i koordinacija toka

```java
CncMachine selectedMachine = machineComboBox.getValue();
ToolFormController controller = showDialog(
        "/hr/lukabosnjak/ui/view/tool-form.fxml",
        "Dodavanje alata",
        ToolFormController.class,
        form -> form.setMachine(selectedMachine));
```

**Ideja opisa u radu:** Odabrani stroj prenosi se child controlleru prije prikaza modalnog obrasca, a spremljeni alat vraća se glavnom UI toku.

## Kandidat za sliku, dijagram ili tablicu

Nema — modalni prozori još nisu vizualno snimljeni.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
