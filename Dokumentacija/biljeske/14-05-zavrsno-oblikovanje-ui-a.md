# 14.5 — Završno oblikovanje UI-a i spremanje programa

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Dovršiti osnovni generator workflow tako da generirani program može biti
stvarno spremljen, pregledan kroz Saved Programs i izvezen.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/SavedJobService.java` — spremanje joba
  kroz repository sloj.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` —
  spremanje validiranih vrijednosti trenutne forme i stanje akcijskih gumba.
- `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — hrvatski naziv
  sekcije i identifikatori akcijskih gumba.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran korak 14.5 i usklađen 14.4.

## Stvarna implementacija

Nakon uspješnog generiranja aktiviraju se spremanje i export. Spremanje provjerava
naziv, prijavljenog korisnika i ponovno čita trenutne vrijednosti forme, a zatim
sprema `MachiningJob` s G-code tekstom kroz `SavedJobService`. Količina ostaje
interna vrijednost 1 bez novog UI elementa. Bez generiranog programa akcije
ostaju onemogućene.

## Razlog odabranog rješenja

Generator sada dovršava stvarni tok spremanja bez SQL-a u controlleru i koristi
postojeći transakcijski repository te postojeći Saved Programs ekran.

## Arhitektonska povezanost

Controller koordinira unos i prikaz. `SavedJobService` orkestrira pristup
repositoryju, a JDBC sloj ostaje jedino mjesto SQL-a.

## Važne odluke i ograničenja

- Podržan je jedan element Iteracije 1; quantity/layout UI nije uveden.
- RichAuto A11 kompatibilnost nije fizički testirana.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
```

### Stvarni rezultat

IDE build je uspješan.

### Što nije testirano

JavaFX raspored i cijeli workflow nisu ručno vizualno testirani. Puni Maven
suite nije pokrenut u ovom okruženju.

## Otvorena pitanja

- Potrebna je ručna provjera workflowa `login -> generator -> generate -> save
  -> Saved Programs -> reopen -> export -> logout`.

## Moguće poglavlje završnog rada

JavaFX workflow i persistence spremljenih CNC programa.

## Kandidati za isječke koda

### Kandidat: Spremanje generator snapshot-a

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`  
**Klasa/metoda:** `MainFormController#handleSave`  
**Zašto je važan:** Povezuje korisnički unos, prijavljenog autora, G-code i
  persistence service bez SQL-a u controlleru.  
**Moguće poglavlje:** JavaFX workflow i slojevita arhitektura

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
