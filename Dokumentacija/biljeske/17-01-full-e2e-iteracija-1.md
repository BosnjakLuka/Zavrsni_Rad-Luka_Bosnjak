# 17.1 — Full end-to-end Iteracija 1

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO NI FIZICKI TESTIRANO

## Cilj

Provjeriti i povezati single-element tok od inicijalizacije baze i prijave do
spremanja, ponovnog ucitavanja i `.nc` izvoza nakon restarta aplikacije.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` —
  ucitavanje i osvjezavanje quick-access liste spremljenih programa.
- `src/main/java/hr/lukabosnjak/ui/controller/SavedProgramsController.java` —
  prikaz greske pri neuspjesnom otvaranju spremljenog programa.
- `src/test/java/hr/lukabosnjak/app/ApplicationCompositionRootIntegrationTest.java` —
  file-based restart round-trip provjera.
- `Dokumentacija/biljeske/00_indeks.md` — indeks ovog koraka.

## Stvarna implementacija

Glavni ekran sada ucitava spremljene programe pri inicijalizaciji i osvjezava
quick-access listu nakon uspjesnog spremanja. Saved Programs ekran vise ne
propusta gresku pri ponovnom dohvaianju odabranog posla, nego je prikazuje u
statusnoj poruci. Dodan je integracijski test koji kroz dva composition-root
konteksta koristi istu file-based H2 bazu, provjerava idempotentni bootstrap,
registraciju i ponovno logiranje korisnika, generiranje s kompenzacijom,
spremanje, ponovno ucitavanje i ponovni `.nc` export.

Quantity ostaje interna vrijednost `1`; layout, capacity i `requiredSheets`
nisu uvedeni.

## Razlog odabranog rjesenja

Persistence tok je vec bio implementiran, ali quick-access UI nije pozivao
ucitavanje liste. Osvjezavanje se dodaje na granici glavnog ekrana bez
premjestanja SQL-a ili poslovne logike u controller.

## Arhitektonska povezanost

Controller samo koordinira prikaz i poziva `SavedJobService`. Repository i
service slojevi zadrzavaju odgovornost za H2 dohvat, snapshot agregata i
pretvorbu spremljenog G-koda. Generator, kompenzacija i export ostaju u svojim
postojecim slojevima.

## Vazne odluke i ogranicenja

- Iteracija 1 podrzava jedan element i ne uvodi layout ili kapacitet.
- Bootstrap podaci se provjeravaju prije umetanja i moraju ostati idempotentni.
- RichAuto A11 kompatibilnost nije fizicki testirana.

## Build i testiranje

### Izvrsene naredbe

```text
IDE build_project (rebuild=true)
IDE JUnit run: ApplicationCompositionRootIntegrationTest
IDE JUnit run: V1BootstrapServiceIntegrationTest
git diff --check
mvn -q test
```

### Stvarni rezultat

IDE rebuild je uspjesan. Sva tri testa u
`ApplicationCompositionRootIntegrationTest` uspjesno su izvrsena, ukljucujuci
restart round-trip test. Sva tri testa u `V1BootstrapServiceIntegrationTest`
takoder su uspjesno izvrsena. `git diff --check` je uspjesan. Maven nije
dostupan u okruzenju, pa `mvn -q test` nije pokrenut.

### Sto nije testirano

Nije izvrsen puni Maven suite niti vizualni JavaFX test. Nije proveden fizicki
test na ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Pokrenuti ciljani integracijski test na razvojnom okruzenju s dostupnim Mavenom
  i JDK-om projekta.

## Moguce poglavlje zavrsnog rada

Integracija aplikacijskog toka i restart-safe persistence.

## Kandidati za isjecke koda

### Kandidat: Osvjezavanje quick-access liste nakon spremanja

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java`  
**Klasa/metoda:** `MainFormController#handleSave`  
**Zasto je vazan:** Povezuje uspjesno persistence spremanje s neposrednim
  prikazom spremljenog programa u glavnom UI-u.  
**Moguce poglavlje:** JavaFX workflow i persistence

```java
MachiningJob saved = savedJobService.save(new MachiningJob(
        null, sessionContext.currentUser().orElseThrow(
                () -> new AuthorizationException("Za spremanje je potrebna prijava.")),
        request.machine(), request.tool(), request.materialSheet(),
        request.machiningParameters(), request.shape(), name, 1,
        displayedProgram.text(), null, null));
refreshSavedJobs();
```

**Ideja opisa u radu:** Nakon spremanja UI ponovno cita spremljene poslove kroz
service sloj umjesto da odrzava vlastitu kopiju persistence podataka.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Biljeska ne sadrzi tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
