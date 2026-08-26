# 15.2 — Process/profile postavke bez obveznog ručnog unosa

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO NI FIZIČKI TESTIRANO

## Cilj

Pojednostaviti operatorski Generate tok tako da glavni ekran ne zahtijeva ručni
unos svih šest machining vrijednosti, uz zadržavanje efektivnih vrijednosti u
`MachiningParameters` snapshotu.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/MachiningParametersPreset.java` —
  mali immutable preset s jasno označenim referentnim vrijednostima.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` —
  registracija preseta i njegovo prosljeđivanje glavnom controlleru.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` —
  automatsko popunjavanje preseta i zadržavanje postojećeg snapshot toka.
- `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` —
  tehničke vrijednosti premještene u neobavezni collapsed `TitledPane`.
- `Dokumentacija/biljeske/00_indeks.md` i `00_odluke.md` —
  dokumentiranje koraka i arhitektonske odluke.

## Stvarna implementacija

Composition root koristi `MachiningParametersPreset.referenceDefaults()`.
Glavni ekran pri inicijalizaciji automatski popunjava svih šest vrijednosti, ali
ih prikazuje unutar zatvorenog naprednog panela. Operator može odmah odabrati
oblik, materijal, dimenzije ploče, stroj i alat te pokrenuti Generate bez
ručnog upisivanja tehničkih parametara. Postojeći `readGenerationRequest`
stvara `MachiningParameters`, a Save i dalje sprema upravo korištene vrijednosti
u `MachiningJob`.

Preset vrijednosti su označene kao referentne/testne i nisu fizički potvrđene na
ZK-1325 / RichAuto A11.

## Razlog odabranog rješenja

Jedan mali immutable preset dovoljan je za Iteraciju 1 i ne uvodi novi
subsystem, bazne tablice ni promjenu modela. `RichAutoA11Profile` ostaje
odgovoran za format i emitiranje G-code naredbi, a preset samo osigurava
efektivne machining ulaze.

## Arhitektonska povezanost

Preset pripada service sloju. Controller samo prima preset kroz composition
root, popunjava UI i koordinira postojeći generation workflow. Generator,
validacija, persistence i `MachiningParameters` model nisu premješteni u UI.

## Važne odluke i ograničenja

- `MachiningParameters` ostaje obvezan snapshot domene i persistencea.
- Tehničke vrijednosti nisu uklonjene iz generatora ni glavnog modela.
- Referentne vrijednosti nisu dokaz fizičke prikladnosti.
- Nisu zaključani RichAuto `F`/`S` read/ignore, Z-smjer, work zero ni stvarne
  postavke prvog reza.
- Quantity, layout, batch output i cutter compensation nisu dio ovog koraka.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit: hr.lukabosnjak.service.MachiningParametersPresetTest
IDE JUnit: hr.lukabosnjak.ui.controller.FxmlControllerContractTest
```

### Stvarni rezultat

IDE build je uspješan. Oba ciljana JUnit testa završila su s exit code 0.

### Što nije testirano

Vizualni JavaFX workflow i fizički test na ZK-1325 / RichAuto A11 nisu izvršeni
u ovom koraku.

## Otvorena pitanja

- Treba potvrditi stvarne machining vrijednosti i RichAuto postavke s operatorom
  i na konkretnom stroju prije označavanja preseta kao fizički potvrđenog.
- Napredne vrijednosti još se mogu ručno promijeniti prije Generate; njihova
  validacija ostaje postojeća.

## Moguće poglavlje završnog rada

Operatorski workflow, slojevita arhitektura i upravljanje procesnim
postavkama.

## Kandidati za isječke koda

### Kandidat: Referentni process preset

**Datoteka:** `src/main/java/hr/lukabosnjak/service/MachiningParametersPreset.java`  
**Klasa/metoda:** `MachiningParametersPreset#referenceDefaults`,
`MachiningParametersPreset#toParameters`  
**Zašto je važan:** Pokazuje kako se testne vrijednosti eksplicitno označavaju i
pretvaraju u domenski snapshot bez vezivanja modela uz JavaFX.  
**Moguće poglavlje:** Upravljanje machining postavkama i slojevita arhitektura

```java
public static MachiningParametersPreset referenceDefaults() {
    return new MachiningParametersPreset(
            "Referentne testne postavke", true,
            18000.0, 500.0, 150.0, 1.0, 1.0, 5.0);
}

public MachiningParameters toParameters() {
    return new MachiningParameters(
            null, spindleSpeed, feedRate, plungeRate, cutDepth, stepDown, safeZ);
}
```

**Ideja opisa u radu:** Procesne vrijednosti imaju jasno označen izvor, a
generator i persistence i dalje rade s eksplicitnim snapshotom posla.

### Kandidat: UI koordinacija preseta

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`  
**Klasa/metoda:** `MainFormController#applyMachiningPreset`  
**Zašto je važan:** Prikazuje da controller samo prenosi service vrijednosti u
UI, bez izračuna machining politike ili G-code logike.  
**Moguće poglavlje:** JavaFX operatorski workflow

```java
private void applyMachiningPreset() {
    MachiningParameters presetParameters = machiningParametersPreset.toParameters();
    spindleSpeedInput.setText(Double.toString(presetParameters.getSpindleSpeed()));
    feedRateInput.setText(Double.toString(presetParameters.getFeedRate()));
    plungeRateInput.setText(Double.toString(presetParameters.getPlungeRate()));
    cutDepthInput.setText(Double.toString(presetParameters.getCutDepth()));
    stepDownInput.setText(Double.toString(presetParameters.getStepDown()));
    safeZInput.setText(Double.toString(presetParameters.getSafeZ()));
}
```

**Ideja opisa u radu:** UI je pojednostavljen, ali efektivne vrijednosti ostaju
vidljive i objašnjive.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
