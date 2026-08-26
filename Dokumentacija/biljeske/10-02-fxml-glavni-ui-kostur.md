# 10.2 — FXML glavni UI kostur

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Izraditi vizualni kostur glavne JavaFX forme za jedan element, s dinamičkim semantičkim poljima oblika i lokalnim UI porukama, bez povezivanja na G-code, persistence, quantity ili layout.

## Promijenjene datoteke

- `pom.xml` — dodana je ovisnost `javafx-fxml`.
- `src/main/java/hr/lukabosnjak/app/Main.java` — FXML view postaje početni Scene.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` i `NumericInputParser.java` — UI koordinacija, dinamička polja i centralizirano parsiranje brojeva.
- `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — deklarativna glavna forma.
- `src/test/java/hr/lukabosnjak/ui/controller/NumericInputParserTest.java` — testovi parsera.
- `Dokumentacija/biljeske/00_indeks.md`, `00_odluke.md` — indeks i FXML odluka.

## Stvarna implementacija

FXML forma prikazuje ShapeType, semantička polja za četiri podržana oblika, dimenzije ploče, prazne kontrole za stroj i alat, šest machining parametara, G-code preview i sve tražene akcije. UI ne prikazuje `dimensionA`, `dimensionB` ni `dimensionC`, quantity, capacity, requiredSheets ili layout.

Promjena ShapeType zamjenjuje polja: kvadrat koristi stranicu, pravokutnik širinu i visinu, krug promjer, a trokut stranicu. Generate lokalno provjerava da sva vidljiva numerička polja sadrže konačne brojeve. Save, Export i Saved Programs prikazuju poruke da pripadajuće integracije nisu implementirane. Preview se ne popunjava.

## Razlog odabranog rješenja

FXML odvaja view od controllera bez nepotrebne Java view klase. `NumericInputParser` je jedno mjesto za obradu praznih, nebrojčanih i nekonačnih ulaza, pa controller ne ponavlja `try/catch` logiku. Prazni izbori stroja i alata ne izmišljaju strojne ili alatne podatke prije persistence integracije.

## Arhitektonska povezanost

FXML i controller pripadaju UI sloju. Controller ne izvršava SQL, ne stvara domenske objekte, ne poziva validation, ne računa geometriju ili layout te ne generira G-code. Budući service tok može se dodati između controllera i postojećih slojeva bez promjene resource granice viewa.

## Važne odluke i ograničenja

- FXML resource path je `src/main/resources/hr/lukabosnjak/ui/view/`; projekt ostaje non-modularan i nema `module-info.java`.
- `javafx-fxml` dodan je jer se FXML stvarno koristi.
- UI parser prihvaća decimalnu točku i zarez, ali ne zamjenjuje domensku validaciju niti potvrđuje machining parametre.
- G-code, `.nc` export, persistence, Saved Programs, quantity i layout nisu povezani niti testirani ovim korakom.

## Build i testiranje

### Izvršene naredbe

```text
mvn test
<IntelliJ Maven> -Dmaven.repo.local=C:\\Users\\lukab\\.m2\\repository test
<IntelliJ Maven> -Dmaven.repo.local=C:\\Users\\lukab\\.m2\\repository -Dmaven.compiler.release=25 test
```

### Stvarni rezultat

Prva naredba nije pokrenuta jer `mvn` nije dostupan u terminalu. Druga je pokrenula Maven i kopirala resource datoteke, ali je završila s `BUILD FAILURE` prije kompilacije: IntelliJ ugrađeni runtime ne podržava projektni `release 26` (`error: release version 26 not supported`). Treća je pokušala samo provjeru s privremenim `release 25`, ali je kompilacija stala na postojećoj H2 JAR ovisnosti. Zbog toga novi JUnit testovi nisu izvršeni.

### Što nije testirano

Nisu izvršeni unit testovi, puna kompilacija ni vizualni JavaFX runtime test. Nije testirano generiranje G-codea, persistence, spremanje, otvaranje spremljenih programa, `.nc` export ni fizički stroj.

## Otvorena pitanja

- Za pokretanje Maven testova potreban je dostupan JDK 26, potvrđen za projektni `maven.compiler.release`.
- Povezivanje forme na stvarni Generate workflow ostaje zaseban sljedeći korak.

## Moguće poglavlje završnog rada

Implementacija korisničkog sučelja i razdvajanje FXML viewa od JavaFX controllera.

## Kandidati za isječke koda

### Kandidat: Dinamička semantička polja oblika

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`  
**Klasa/metoda:** `MainFormController#updateShapeFields`  
**Zašto je važan:** Pokazuje kako UI prikazuje poslovno razumljive nazive umjesto internih `dimensionA/B/C` polja.  
**Moguće poglavlje:** Implementacija korisničkog sučelja

```java
switch (shapeType) {
    case SQUARE -> addShapeInput(fields, 0, "Stranica (mm)");
    case RECTANGLE -> {
        addShapeInput(fields, 0, "Širina (mm)");
        addShapeInput(fields, 1, "Visina (mm)");
    }
    case CIRCLE -> addShapeInput(fields, 0, "Promjer (mm)");
    case TRIANGLE -> addShapeInput(fields, 0, "Stranica (mm)");
}
```

**Ideja opisa u radu:** Odabir tipa oblika određuje samo prikaz i naziv potrebnih unosa; geometrija se ne računa u UI sloju.

### Kandidat: Centralizirano parsiranje brojeva

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/NumericInputParser.java`  
**Klasa/metoda:** `NumericInputParser#parseRequiredFinite`  
**Zašto je važan:** Jedinstveno obrađuje obavezan ulaz, decimalni zarez i odbijanje nekonačnih vrijednosti.  
**Moguće poglavlje:** Obrada korisničkog unosa

```java
double parsedValue = Double.parseDouble(value.trim().replace(',', '.'));
if (!Double.isFinite(parsedValue)) {
    throw new IllegalArgumentException("Polje '" + fieldLabel + "' mora sadržavati konačan broj.");
}
```

**Ideja opisa u radu:** UI priprema sintaktički ispravan broj, dok poslovna pravila ostaju odgovornost zasebnog validation sloja.

## Kandidat za sliku, dijagram ili tablicu

Nema — vizualna snimka forme nije stvarno izrađena ni potvrđena.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
