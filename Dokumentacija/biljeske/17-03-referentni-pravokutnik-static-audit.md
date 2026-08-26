# 17.3 — Referentni pravokutnik 100 × 200 mm: static release audit

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Usporediti izlaz generatora za pravokutnik 100 × 200 mm s dostavljenim
referentnim `.nc` programom nakon V1 geometrijske kompenzacije alata.

## Promijenjene datoteke

- `src/test/java/hr/lukabosnjak/gcode/ReferenceRectangleAuditTest.java` —
  ciljano softversko generiranje i usporedba s referencom.
- `Dokumentacija/biljeske/00_indeks.md` i ova bilješka — razvojni trag.

## Stvarna implementacija

Audit koristi alat Ø6 mm, `CutSide.INSIDE`, referentni RichAuto profil te
parametre `S=18000`, `F=500`, plunge `150`, dubina `1`, step-down `1` i safe Z
`5`. Nominalni pravokutnik ima granice `X=[0,100]`, `Y=[0,200]`.
Kompenzirana putanja centra alata ima granice `X=[3,97]`, `Y=[3,197]`.

Generirani program sadrži `G21`, `G17`, zasebne retke `G90` i `G54`, `M03`,
safe Z `5`, plunge `Z-1 F150`, rezanje s `F500`, `M05` i `M30`.

## Razlog odabranog rješenja

Reference se koristi kao static regression fixture, dok generator ostaje
odgovoran za izlaz iz pripremljene kompenzirane geometrije. Razlika u
koordinatama je očekivana jer referenca opisuje nominalnu konturu, a V1
generator koristi putanju centra alata.

## Arhitektonska povezanost

Test povezuje `ToolPathService`, `ToolPathCompensationService`,
`ToolPathBoundsCalculator` i `RichAutoA11GCodeGenerator` bez premještanja
geometrije u G-code sloj. Referentni `.nc` ostaje testni/dokumentacijski
artefakt.

## Važne odluke i ograničenja

- V1 koristi aplikacijski geometrijski offset; ne emitira `G41`, `G42`, `G40`
  ni `D`.
- `G21` i `G17` su dodatni profilni retci, a `G90` i `G54` emitiraju se
  zasebno umjesto u jednom retku `G90 G54`.
- `F150` i `F500`, `M03`, `M05` i `M30` podudaraju se po značenju s referencom.
- RichAuto A11 kompatibilnost nije fizički testirana.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=true)
IDE run: ReferenceRectangleAuditTest
git diff --check
```

### Stvarni rezultat

IDE rebuild je uspješan. `ReferenceRectangleAuditTest` završio je s exit
codeom 0 i potvrdio nominalne dimenzije, kompenzirane granice `X=[3,97]`,
`Y=[3,197]`, ključne G-code naredbe i očekivane razlike prema referenci.
`git diff --check` je uspješan.

### Što nije testirano

Nije proveden puni Maven suite ni fizički test na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Prije fizičkog reza treba potvrditi stvarni work zero, smjer Z osi i
  ponašanje kontrolera za profilne naredbe.

## Moguće poglavlje završnog rada

Geometrijska kompenzacija, priprema ToolPatha i regression testiranje G-codea.

## Kandidati za isječke koda

### Kandidat: Audit kompenziranih granica i izlaza

**Datoteka:** `src/test/java/hr/lukabosnjak/gcode/ReferenceRectangleAuditTest.java`  
**Klasa/metoda:** `ReferenceRectangleAuditTest#auditsCompensatedRectangleAgainstReferenceProgram`  
**Zašto je važan:** Jednim testom provjerava nominalnu geometriju, offset
  promjera alata, XY granice i ključne retke generiranog programa.  
**Moguće poglavlje:** Geometrija alata i regression testiranje

```java
Bounds2 bounds = new ToolPathBoundsCalculator().calculate(compensatedPath);
assertEquals(3.0, bounds.minX(), 1e-9);
assertEquals(3.0, bounds.minY(), 1e-9);
assertEquals(97.0, bounds.maxX(), 1e-9);
assertEquals(197.0, bounds.maxY(), 1e-9);
```

**Ideja opisa u radu:** Promjer alata mijenja putanju centra alata, ali ne
  mijenja nominalne dimenzije ulaznog oblika.

## Kandidat za sliku, dijagram ili tablicu

Tablica nominalnih i kompenziranih XY granica te razlika prema referentnom
programu.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
