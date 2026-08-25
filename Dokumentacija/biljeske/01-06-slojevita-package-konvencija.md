# 1.6 — Slojevita package konvencija

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

## Cilj

Učiniti buduću strukturu projekta preglednijom pomoću jasnih podpaketa unutar potvrđenih arhitektonskih slojeva, bez stvaranja praznih direktorija ili nepotrebnih klasa.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MaterialType.java` — postojeći model smješten je u podpaket za domenske entitete bez promjene javnog API-ja.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je trajna package konvencija.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/01-05-material-type.md` — postojeće reference usklađene su s aktualnom lokacijom modela.
- `Dokumentacija/biljeske/01-06-slojevita-package-konvencija.md` — dokumentirana je promjena i provjera.

## Stvarna implementacija

Puni naziv modela naknadno je usklađen na `hr.lukabosnjak.domain.entities.MaterialType`. U projektu nema pozivatelja koje je trebalo migrirati. Ostali dogovoreni paketi nisu fizički stvoreni jer još nemaju stvarne klase.

## Razlog odabranog rješenja

Slojevi i njihovi podpaketi istodobno pokazuju arhitektonsku pripadnost i konkretnu vrstu klase. Time se olakšava snalaženje bez neodređenih paketa poput `util`, preširokog `io` ili vršnog `dto` paketa kojem nije vidljiv vlasnički sloj.

## Arhitektonska povezanost

Trajna ciljana organizacija koristi `domain.entities`, `domain.enums`, `service.dto`, `persistence.repository`, `persistence.jdbc`, `ui.controller` i `ui.view`, uz postojeće slojeve `app`, `config`, `validation`, `geometry`, `layout`, `gcode` i `service`. `Main` ostaje minimalna JavaFX ulazna točka u paketu `app`.

## Važne odluke i ograničenja

- Paketi nastaju tek s prvom stvarno potrebnom klasom ili resursom.
- DTO pripada `service.dto`; ne koristi se kao zamjena za domenski model ili JDBC redak.
- Ne uvodi se opći `util`, `io` ili `exception` paket bez konkretne odgovornosti.
- FXML struktura pod `src/main/resources` uvodi se samo ako FXML naknadno bude odabran.
- Nisu dodani DTO-i, konfiguracija, UI, persistence, validacija ni novi domenski modeli.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
```

### Stvarni rezultat

`mvn clean test` izvršen je na Oracle OpenJDK-u 26.0.2.1, ponovno je kompilirao oba Java izvora s Java releaseom 26 i završio s `BUILD SUCCESS`. Projekt nema testnih klasa.

### Što nije testirano

Budući paketi, DTO-i, JavaFX prikazi, JDBC mapiranje i H2 persistence nisu implementirani niti testirani. Nema novih unit testova jer su promijenjene samo package deklaracija i putanja postojeće podatkovne klase.

## Otvorena pitanja

- Konačna odluka o FXML-u ili programatskom UI-ju ostaje za zaseban korak.
- Konkretne klase svakog sloja definirat će se tek uz pripadajuće funkcionalnosti.

## Moguće poglavlje završnog rada

Arhitektura aplikacije i organizacija izvornog koda.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — promijenjena je samo package deklaracija postojeće klase.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
