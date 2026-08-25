# 4.1 — Shape enumovi i entitet

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Dodati samo potvrđene V1 tipove oblika i zajednički `Shape` entitet koji čuva njihove podatke bez geometrijskih izračuna, UI naziva ili persistence detalja.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/enums/ShapeType.java` — dodani su potvrđeni osnovni oblici.
- `src/main/java/hr/lukabosnjak/domain/enums/ShapeSubtype.java` — dodan je jedini potvrđeni V1 podtip `EQUILATERAL`.
- `src/main/java/hr/lukabosnjak/domain/entities/Shape.java` — dodan je zajednički domenski entitet oblika.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježene su reprezentacija i semantika oblika.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/03-01-domain-core.md` i povezane package bilješke — usklađene su sa stvarnom lokacijom postojećih entiteta.
- `Dokumentacija/biljeske/04-01-shape-enums-entity.md` — dokumentirana je implementacija i provjera.

## Stvarna implementacija

`ShapeType` sadrži isključivo `SQUARE`, `RECTANGLE`, `CIRCLE` i `TRIANGLE`, a `ShapeSubtype` samo `EQUILATERAL`. `Shape` je obična mutable Java klasa s punim konstruktorom, getterima i setterima za ID, tip, nullable podtip, tri objektne `Double` dimenzije te vremenske oznake stvaranja, izmjene i nullable brisanja.

Semantika V1 je: kvadrat koristi A kao stranicu; pravokutnik A kao širinu i B kao visinu; krug A kao promjer; `TRIANGLE` s `EQUILATERAL` podtipom koristi A kao stranicu. Klasa ne izračunava točke, putanje, površinu ni G-kod.

## Razlog odabranog rješenja

Enumovi ograničavaju model na potvrđene vrijednosti i izbjegavaju proizvoljne Stringove. Jedan `Shape` odgovara planiranom zajedničkom persistence zapisu te izbjegava dupliciranje identiteta i vremenskih oznaka kroz četiri zasebna entiteta.

## Arhitektonska povezanost

`Shape` pripada `domain.entities`, ovisi samo o `domain.enums` i `java.time`, te nema JavaFX, JDBC, H2 ili geometrijske ovisnosti. Budući UI prevodit će generičke dimenzije u semantičke nazive polja, validation sloj provjeravat će kombinacije tipa/podtipa/dimenzija, a geometry sloj iz njih će stvarati geometriju i ToolPath.

## Važne odluke i ograničenja

- `shapeSubtype` je `null` kada oblik nema smislen potvrđeni podtip.
- `dimensionB` i `dimensionC` mogu biti `null`; `dimensionA` je objektni `Double` prema potvrđenom modelu.
- Nisu izmišljeni dodatni podtipovi ni uvedeni `Square`, `Rectangle`, `Circle` ili `Triangle` persistence entiteti.
- UI ne smije korisniku prikazivati tehničke nazive `dimensionA`, `dimensionB` i `dimensionC`.
- Validacija kombinacija i geometrijski izračuni nisu dio ovog koraka.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
```

### Stvarni rezultat

Maven je izvan sandbox ograničenja, uz Oracle OpenJDK 26.0.2.1, kompilirao svih deset glavnih izvora s Java releaseom 26 i završio s `BUILD SUCCESS`. Projekt trenutačno nema testnih klasa.

### Što nije testirano

Nisu testirani validation, JDBC/H2 mapiranje, UI semantičke oznake ni geometrijski izračuni jer nisu implementirani u ovom milestoneu. Nisu dodani testovi enum konstanti, konstruktora, gettera i settera jer ne bi dokazivali poslovno ponašanje.

## Otvorena pitanja

- Pravila dopuštenih kombinacija tipa, podtipa i dimenzija definirat će se u validation milestoneu.
- Mapiranje generičkih dimenzija u semantičke UI kontrole definirat će se pri izradi forme.

## Moguće poglavlje završnog rada

Domenski model geometrijskih oblika.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — enumovi i `Shape` trenutačno su namjerno jednostavni podatkovni tipovi bez algoritamske ili validacijske logike.

## Kandidat za sliku, dijagram ili tablicu

`Dokumentacija/Dijagrami/finalna-vezija_uml_dijagrama.drawio.png` — postojeći UML dijagram može se usporediti s implementiranim entitetom i enumovima.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
