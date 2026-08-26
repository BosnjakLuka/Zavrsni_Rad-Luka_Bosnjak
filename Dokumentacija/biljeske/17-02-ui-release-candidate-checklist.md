# 17.2 — UI release-candidate checklist

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO NI FIZIČKI TESTIRANO

## Cilj

Uskladiti glavnu JavaFX formu s release-candidate checklistom za jedan element:
jasno oznaciti obavezne ulaze i koristiti konzistentne hrvatske nazive.

## Promijenjene datoteke

- `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — oznake
  obaveznih polja i hrvatski nazivi naprednih postavki.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` —
  oznacavanje dinamickih dimenzija oblika kao obaveznih.
- `Dokumentacija/biljeske/00_indeks.md` i ova biljeska — razvojni trag.

## Stvarna implementacija

Glavna forma sada oznacava zvjezdicom tip oblika, dimenzije oblika, vrstu
materijala, dimenzije materijalne ploce, stroj i alat. Napredne postavke
preimenovane su u hrvatske nazive, dok su i dalje u zatvorenom neobaveznom
panelu s automatskim referentnim presetom.

## Razlog odabranog rjesenja

Korisnik prije akcije `Generiraj` moze jasno razlikovati obavezne ulaze od
naprednih postavki. Promjena je ogranicena na UI oznake i ne mijenja postojece
validacijske, servisne ili persistence ugovore.

## Arhitektonska povezanost

FXML sadrzi staticke oznake, a controller samo dodaje oznaku dinamickim
poljima oblika. Business validacija ostaje u validation/service sloju.

## Vazne odluke i ogranicenja

- Quantity, layout, capacity i `requiredSheets` nisu uvedeni.
- Machining preset ostaje referentna softverska vrijednost i nije fizicki
  potvrden na ZK-1325 / RichAuto A11.
- Oznaka obaveznosti ne mijenja cinjenicu da se tehnicke vrijednosti na
  glavnom ekranu inicijalno popunjavaju presetom.

## Build i testiranje

### Izvrsene naredbe

```text
IDE build_project (rebuild=true)
IDE run: FxmlControllerContractTest
git diff --check
```

### Stvarni rezultat

IDE rebuild je uspjesan bez problema. FXML contract test je zavrsio s exit
codeom 0. `git diff --check` je uspjesan.

### Sto nije testirano

Nije izveden vizualni rucni pregled svih JavaFX ekrana niti fizicki test na
ZK-1325 / RichAuto A11. Puni Maven suite nije pokrenut jer Maven nije dostupan
u okruzenju.

## Otvorena pitanja

- Prije strojnog testa potrebno je vizualno potvrditi raspored glavne forme i
  svih dijaloga u stvarnom JavaFX runtimeu.

## Moguce poglavlje zavrsnog rada

Korisnicko sucelje i validacija ulaznih podataka.

## Kandidati za isjecke koda

### Kandidat: Oznacavanje dinamickih obaveznih dimenzija

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`  
**Klasa/metoda:** `MainFormController#addShapeInput`  
**Zasto je vazan:** Pokazuje kako se ista UI konvencija primjenjuje i na
  dinamicki generirana polja oblika.  
**Moguce poglavlje:** JavaFX korisnicko sucelje i validacija

```java
fields.add(new Label(label + " *"), 0, row);
```

**Ideja opisa u radu:** Dinamicka polja jasno prenose obaveznost bez
dupliciranja zasebnih FXML kontrola za svaki oblik.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Biljeska ne sadrzi tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
