# 18.2 — Dropdown odabir stroja za katalog alata

**Datum:** 2026-08-27
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Omogućiti jasan odabir CNC stroja iznad liste alata kako bi katalog odmah prikazao alate povezane s odabranim strojem.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/ui/controller/CatalogController.java` — dropdown je postao izvor odabira stroja za dohvat i dodavanje alata.
- `src/main/resources/hr/lukabosnjak/ui/view/catalog.fxml` — dodani labela i `ComboBox` iznad liste alata.
- `src/test/java/hr/lukabosnjak/ui/controller/FxmlControllerContractTest.java` — dodana provjera postojanja selektora i njegova action handlera.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran prompt 18.2.
- `Dokumentacija/biljeske/18-02-katalog-odabir-stroja-za-alate.md` — ova bilješka.

## Stvarna implementacija

Kartica alata sada sadrži dropdown svih katalogiziranih strojeva u formatu `naziv — kontroler — aktivno/deaktivirano`. Prvi stroj automatski se odabire kada prethodni odabir ne postoji. Osvježavanje čuva odabrani stroj prema identifikatoru, a promjena dropdowna odmah dohvaća samo njegove alate.

Dodavanje alata koristi stroj iz dropdowna. Lista alata zadržava format `Tbroj — Øpromjer mm — naziv — status`. Nakon zatvaranja kataloške forme controller osvježava podatke, pa novo ili promijenjeno stanje postaje vidljivo bez dodatnog ručnog osvježavanja.

## Razlog odabranog rješenja

Prethodni ekran oslanjao se na selekciju u odvojenoj listi strojeva, ali nije imao listener ni početni odabir, zbog čega je lista alata ostajala prazna. Eksplicitni dropdown čini vezu stroja i alata vidljivom i uklanja skriveni preduvjet.

## Arhitektonska povezanost

`CatalogController` samo koordinira odabir i poziva postojeći `ReferenceDataManagementService#loadTools`. Dohvat ostaje u service/persistence toku; u controller nisu uvedeni SQL ni nova poslovna pravila.

## Važne odluke i ograničenja

- Dropdown prikazuje aktivne i deaktivirane strojeve jer katalog služi upravljanju referentnim podacima.
- Lista CNC strojeva ostaje namijenjena uređivanju i aktivaciji/deaktivaciji strojeva.
- Dropdown određuje samo koji se alati prikazuju i kojem se stroju dodaje novi alat.
- Bootstrap alati ostaju jasno označeni softverski testni podaci i nisu fizički potvrđeni.

## Build i testiranje

### Izvršene naredbe

```text
mvn -q "-Dtest=FxmlControllerContractTest,ReferenceDataManagementServiceTest" test
IntelliJ bundled mvn.cmd -q -Dmaven.repo.local=<postojeći korisnički Maven cache> -Dtest=FxmlControllerContractTest,ReferenceDataManagementServiceTest test
IntelliJ bundled mvn.cmd -q -Dmaven.repo.local=<postojeći korisnički Maven cache> test
```

### Stvarni rezultat

Globalna naredba `mvn` nije bila dostupna. Prvi sandbox pokušaj s bundled Mavenom nije mogao pristupiti H2 JAR-u u postojećem Maven cacheu. Iste provjere zatim su uspješno izvršene s IntelliJ bundled Mavenom i potvrđenim JDK-om izvan tog ograničenja.

Ciljani `FxmlControllerContractTest` i `ReferenceDataManagementServiceTest` prošli su bez greške. Puni Maven skup izvršio je 142 testa: 0 neuspjeha, 0 pogrešaka i 0 preskočenih testova.

### Što nije testirano

JavaFX ekran nije ručno pokrenut, pa vizualni raspored, stvarno klikanje dropdowna i prikaz alata u prozoru nisu vizualno potvrđeni. Nije izvršen fizički test na ZK-1325 / RichAuto A11, što nije ni potrebno za ovu UI promjenu.

## Otvorena pitanja

- Nema otvorenih pitanja unutar ovog koraka. Katalog programa i prethodno dokumentirana RBAC/soft-delete pravila ostaju zaseban zadatak.

## Moguće poglavlje završnog rada

JavaFX korisničko sučelje i koordinacija povezanih kataloških podataka.

## Kandidati za isječke koda

### Kandidat: Očuvanje odabira i dohvat alata po stroju

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/CatalogController.java`
**Klasa/metoda:** `CatalogController#refresh`, `loadToolsForSelectedMachine`
**Zašto je važan:** Pokazuje kako UI čuva selekciju prema domenskom identifikatoru i delegira dohvat alata service sloju.
**Moguće poglavlje:** JavaFX UI koordinacija i slojevita arhitektura

```java
CncMachine selectedMachine = machines.stream()
        .filter(machine -> selectedMachineId != null
                && selectedMachineId.equals(machine.getCncMachineId()))
        .findFirst()
        .orElse(machines.isEmpty() ? null : machines.getFirst());
toolMachineComboBox.setValue(selectedMachine);
loadToolsForSelectedMachine();
```

**Ideja opisa u radu:** Controller upravlja stanjem prikaza, dok postojeći service ostaje izvor povezanih alata.

## Kandidat za sliku, dijagram ili tablicu

Korisnički priložena snimka početnog stanja kataloga može poslužiti kao „prije” prikaz; lokalna privremena putanja nije zapisana.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
