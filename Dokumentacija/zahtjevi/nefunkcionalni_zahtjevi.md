# Audit nefunkcionalnih zahtjeva

**Datum audita:** 2026-08-27  
**Opseg:** arhitektura, sigurnost, pouzdanost, persistence, format izlaza, obradivost pogrešaka, održivost i testabilnost trenutačne aplikacije  
**Glavni izvor istine:** produkcijski kod i izvršeni testovi, a ne planirane ili povijesne tvrdnje.

## Legenda statusa

- **IMPLEMENTIRANO / TESTIRANO** — svojstvo je vidljivo u produkcijskom kodu i provjereno automatiziranim testom ili ponovljivim statičkim auditom izvršenim u ovom zadatku.
- **IMPLEMENTIRANO / NIJE TESTIRANO** — tehnička izvedba postoji, ali svojstvo nije potvrđeno odgovarajućim uporabnim, vizualnim, end-to-end ili drugim mjerodavnim testom.
- **ITERACIJA 2 / TESTNA FAZA** — svojstvo ovisi o quantity/layout/batch funkcionalnostima koje još nisu dio stabilne aplikacije.

Puni Maven testni skup na dan audita završio je rezultatom: **141 test, 0 neuspjeha, 0 pogrešaka, 0 preskočenih testova, BUILD SUCCESS**. Softverski test nije dokaz fizičke kompatibilnosti ili sigurnosti rada stroja.

## Nefunkcionalni zahtjevi stabilne aplikacije

### NFZ-01 — Razumljiv korisnički tijek

**Zahtjev:** Aplikacija mora ponuditi razumljiv JavaFX tijek od prijave i odabira podataka do generiranja, pregleda, spremanja, ponovnog otvaranja i `.nc` izvoza bez potrebe za ručnim pisanjem G-koda.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** FXML resursi u `src/main/resources/hr/lukabosnjak/ui/view/`; `src/main/java/hr/lukabosnjak/app/Main.java` — navigacijske metode; controlleri u `src/main/java/hr/lukabosnjak/ui/controller/`.  
**Povezani UI:** svi postojeći JavaFX ekrani.  
**Povezani dio:** UI koordinira service sloj preko `ApplicationCompositionRoot`.  
**Testni dokaz:** `FxmlControllerContractTest` provjerava FXML veze, ali nije proveden mjerodavan usability ni potpuni ručni vizualni test.  
**Napomena za završni rad:** Može se opisati implementirani tijek, ali ne tvrditi dokazana jednostavnost korištenja bez testa s korisnicima ili dokumentiranog ručnog scenarija.

### NFZ-02 — Jasne validacijske poruke

**Zahtjev:** Aplikacija mora za očekivane pogreške unosa prikazati konkretnu poruku koja opisuje nevaljano polje ili prekršeno pravilo.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/NumericInputParser.java`; klase u `src/main/java/hr/lukabosnjak/validation/`; service iznimke u `src/main/java/hr/lukabosnjak/service/`; controller catch blokovi koji poruku postavljaju u `statusLabel`.  
**Povezani UI:** statusne oznake na loginu, registraciji, glavnoj formi, katalogu, korisnicima i spremljenim programima.  
**Povezani dio:** UI parser, validation i service slojevi.  
**Testni dokaz:** `NumericInputParserTest`, svi validator testovi i testovi service iznimki.  
**Napomena za završni rad:** Tekst poruka je automatizirano provjeren ispod UI razine; njihov vizualni prikaz nije ručno testiran.

### NFZ-03 — Slojevita podjela odgovornosti

**Zahtjev:** Aplikacija mora razdvojiti odgovornosti na UI, service, validation, domain, geometry/toolpath, gcode i persistence slojeve.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** paketi pod `src/main/java/hr/lukabosnjak/`; `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` povezuje konkretne implementacije UI, service, validation, domain, geometry, gcode i persistence slojeva.  
**Povezani UI:** controlleri pozivaju servise i upravljaju JavaFX kontrolama.  
**Povezani dio:** svi navedeni slojevi.  
**Testni dokaz:** statički audit importova i SQL lokacija; `ApplicationCompositionRootIntegrationTest`; `FxmlControllerContractTest`.  
**Napomena za završni rad:** Stari opis sa šest područja je nepotpun jer aktualna arhitektura izdvaja validation, geometry/toolpath, gcode i composition root.

### NFZ-04 — Neovisnost domene i geometrije o infrastrukturnim frameworkovima

**Zahtjev:** Aplikacija mora osigurati da domenske i geometrijske klase ne ovise o JavaFX kontrolama ni JDBC API-ju.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** klase u `src/main/java/hr/lukabosnjak/domain/` koriste domenske enumove i Java tipove; klase u `src/main/java/hr/lukabosnjak/geometry/` koriste domenske podatke i vlastite vrijednosne objekte bez JavaFX/JDBC importova.  
**Povezani UI:** nema izravne ovisnosti.  
**Povezani dio:** domain i geometry/toolpath.  
**Testni dokaz:** statički audit importova i zasebni domain/geometry testovi.  
**Napomena za završni rad:** Time su geometrijski izračuni testabilni bez pokretanja JavaFX-a ili baze.

### NFZ-05 — Odvajanje geometrije od G-koda

**Zahtjev:** Aplikacija mora najprije izraditi i po potrebi kompenzirati `ToolPath`, a tek zatim ga predati generatoru tekstualnog G-koda.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `generate` slijedi tok validacija → `ToolPathService` → `ToolPathCompensationService` → fit validacija → `GCodeGenerator`; `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java` prima gotov `ToolPath`.  
**Povezani UI:** `MainFormController` samo pokreće service poziv i prikazuje rezultat.  
**Povezani dio:** service, validation, geometry/toolpath i gcode.  
**Testni dokaz:** `ProgramGenerationServiceTest`, geometry i gcode testovi, `FourShapeCompensationIntegrationTest`.  
**Napomena za završni rad:** Gcode sloj ne računa oblik, kompenzaciju ni layout.

### NFZ-06 — Parametrizirani SQL i izolirani persistence

**Zahtjev:** Aplikacija mora SQL i JDBC detalje držati u persistence/config sloju, a sve korisničke i domenske vrijednosti vezati kroz `PreparedStatement` parametre.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcRepositorySupport.java` — `executeInsert`; svi `Jdbc*Repository.java` u istom paketu koriste `PreparedStatement` i `?` parametre za vrijednosti; SQL se ne nalazi u `src/main/java/hr/lukabosnjak/ui/controller/`.  
**Povezani UI:** nema SQL-a ni JDBC-a.  
**Povezani dio:** `persistence.jdbc`, repository sučelja i `DatabaseInitializer`.  
**Testni dokaz:** statički audit svih produkcijskih JDBC poziva; `JdbcRepositoriesIntegrationTest`, `JdbcMachiningJobRepositoryIntegrationTest` i config integracijski testovi.  
**Napomena za završni rad:** `JdbcUserRepository` dinamički spaja samo dvije interne, fiksne assignment konstante (`active = ?`, `role_id = ?`); korisničke vrijednosti i dalje su parametrizirane.

### NFZ-07 — Zaštita lozinki

**Zahtjev:** Aplikacija mora lozinke spremati kao verzionirani PBKDF2-HMAC-SHA256 zapis s nasumičnim saltom i provjeravati ih bez pohrane čistog teksta.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/PasswordHasher.java` — `hash`, `verify`, `derive`; `src/main/java/hr/lukabosnjak/service/AuthService.java` — `register`, `login`; `src/main/resources/db/schema.sql` — `APP_USER.password_hash`.  
**Povezani UI:** `PasswordField` u loginu i registraciji; controlleri čiste polja i privremene `char[]` nizove.  
**Povezani dio:** auth service i user persistence.  
**Testni dokaz:** `PasswordHasherTest`, `AuthServiceIntegrationTest`.  
**Napomena za završni rad:** Implementacija koristi 600 000 iteracija, salt od 16 bajtova i izvedeni ključ od 256 bita; to je aktualna implementacija, ne univerzalna trajna sigurnosna preporuka.

### NFZ-08 — Validacija prije generiranja

**Zahtjev:** Aplikacija mora spriječiti generiranje konačnog programa prije provjere oblika, ploče, parametara, stroja, alata, kompenzirane putanje i granica obrade.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `generate`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `handleGenerate` čisti preview nakon nevaljanog zahtjeva.  
**Povezani UI:** glavna generatorska forma.  
**Povezani dio:** service, validation, geometry/toolpath i gcode.  
**Testni dokaz:** `ProgramGenerationServiceTest` i svi validation testovi.  
**Napomena za završni rad:** Ovo je softverska zaštita od nevaljanih podataka, a ne zamjena za fizičku simulaciju ili provjeru stroja.

### NFZ-09 — Mjerne jedinice i deterministički format

**Zahtjev:** Aplikacija mora geometrijske veličine obrađivati u milimetrima i G-kod numerički formatirati s decimalnom točkom, fiksnom preciznošću i bez znanstvene notacije.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — UI oznake `(mm)`; `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java` — `Units.MILLIMETERS`; `RichAutoA11ProgramEnvelope.java` emitira `G21`; `GCodeFormatter.java` — `format`.  
**Povezani UI:** dimenzijska polja glavnog i kataloških obrazaca.  
**Povezani dio:** domain konvencija, geometry i gcode.  
**Testni dokaz:** `GCodeFormatterTest`, `RichAutoA11ProgramEnvelopeTest`, geometry testovi.  
**Napomena za završni rad:** Softverska uporaba `G21` ne potvrđuje konfiguraciju konkretnog fizičkog kontrolera.

### NFZ-10 — Atomičnost spremanja naloga

**Zahtjev:** Sustav mora spremiti ploču, parametre, oblik i `MachiningJob` u jednoj transakciji te poništiti sve pripadajuće inserte ako spremanje naloga ne uspije.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java` — `save` koristi `setAutoCommit(false)`, `commit` i `rollback` na istoj vezi.  
**Povezani UI:** spremanje se pokreće iz `MainFormController#handleSave`.  
**Povezani dio:** persistence i `SavedJobService`.  
**Testni dokaz:** `JdbcMachiningJobRepositoryIntegrationTest#rollsBackAllOwnedSnapshotsWhenJobInsertFails`.  
**Napomena za završni rad:** Transakcija sprječava orphan snapshot zapise pri djelomičnom neuspjehu.

### NFZ-11 — Siguran i predvidljiv `.nc` export

**Zahtjev:** Aplikacija mora izvoziti samo US-ASCII sadržaj u novu `.nc` datoteku i ne smije neprimjetno prepisati postojeću datoteku.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/gcode/NcExportService.java` — `export`, `requireNcExtension`, `encodeAscii` koriste `StandardOpenOption.CREATE_NEW`.  
**Povezani UI:** export akcije i `FileChooser` na glavnom i saved-programs ekranu.  
**Povezani dio:** `ProgramExportService`, gcode export.  
**Testni dokaz:** svih pet testova u `NcExportServiceTest`; `ProgramExportServiceTest`.  
**Napomena za završni rad:** Format završetka redaka dolazi iz determinističkog `GCodeProgram#text` zapisa.

### NFZ-12 — Trajna H2 pohrana i ponovljiva inicijalizacija

**Zahtjev:** Aplikacija mora inicijalizirati potpunu H2 shemu, primijeniti podržane migracije i idempotentno osigurati referentne razvojne podatke bez dupliciranja pri ponovnom pokretanju.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/config/DatabaseInitializer.java` — `initialize`; `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java` — `initialize` i `ensure*` metode; `src/main/java/hr/lukabosnjak/config/DatabaseConfig.java`.  
**Povezani UI:** inicijalizacija se izvodi prije prikaza login ekrana u `Main#init`.  
**Povezani dio:** config, service bootstrap i persistence.  
**Testni dokaz:** `DatabaseInitializerIntegrationTest`, `DatabaseSchemaIntegrationTest`, `V1BootstrapServiceIntegrationTest`, restart test u `ApplicationCompositionRootIntegrationTest`.  
**Napomena za završni rad:** Seedani alati izričito su softverski testni, a ne potvrđeni fizički inventar stroja.

### NFZ-13 — Obrada očekivanih pogrešaka

**Zahtjev:** Aplikacija mora očekivane pogreške unosa, autentikacije, autorizacije, baze, spremanja, otvaranja i izvoza pretvoriti u razumljive poruke bez rušenja aktivnog korisničkog toka.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** service iznimke u `src/main/java/hr/lukabosnjak/service/` (`AuthenticationException`, `AuthorizationException`, `ReferenceDataAccessException`, `SavedJobAccessException`, `UserManagementException`) i catch blokovi u `src/main/java/hr/lukabosnjak/ui/controller/`.  
**Povezani UI:** statusne oznake na svim funkcionalnim ekranima.  
**Povezani dio:** UI, service, validation, persistence i export.  
**Testni dokaz:** testirani su brojni nevaljani ulazi i wrapping repository pogrešaka, ali nema potpunog JavaFX error-flow testa ni globalnog uncaught-exception handlera.  
**Napomena za završni rad:** Široka stara tvrdnja nije potpuno ostvarena: startup i neuspješno učitavanje FXML-a i dalje završavaju `IllegalStateException`, a neočekivani kvarovi nisu globalno obrađeni.

### NFZ-14 — Održiva i proširiva struktura

**Zahtjev:** Aplikacija mora organizirati kod u klase s prepoznatljivim odgovornostima i omogućiti zamjenu repository implementacija ili G-kod generatora kroz postojeća sučelja.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** repository sučelja u `src/main/java/hr/lukabosnjak/persistence/repository/`; `src/main/java/hr/lukabosnjak/gcode/GCodeGenerator.java`; `src/main/java/hr/lukabosnjak/persistence/jdbc/ConnectionProvider.java`; `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java`.  
**Povezani UI:** controlleri dobivaju ovisnosti preko composition roota.  
**Povezani dio:** svi slojevi.  
**Testni dokaz:** stubovi u service testovima i in-memory/file H2 integracijski testovi dokazuju zamjenjivost dijela ovisnosti; nema zasebnog testa dodavanja novog oblika ili kontrolera.  
**Napomena za završni rad:** Stari NFR je bio preširok. Novi oblik zahtijeva koordinirane promjene `ShapeType`, UI-a, validacije i `ToolPathService` switcha; `MainFormController` sa 507 redaka ostaje veća koordinacijska klasa.

### NFZ-15 — Prilagodljivost JavaFX prikaza

**Zahtjev:** Aplikacija mora glavne sadržajno veće ekrane moći prikazati u prozoru s definiranim minimalnim dimenzijama i skrolanjem sadržaja kada prostor nije dovoljan.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/app/Main.java` — `showView` postavlja minimalne dimenzije; `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` i `catalog.fxml` koriste `ScrollPane` i rastezljive kontrole.  
**Povezani UI:** glavni ekran i katalog.  
**Povezani dio:** JavaFX UI.  
**Testni dokaz:** FXML veze prolaze `FxmlControllerContractTest`, ali raspored nije vizualno provjeren na različitim veličinama prozora i skaliranjima.  
**Napomena za završni rad:** Može se tvrditi implementirana prilagodba, ne i potvrđena potpuna responzivnost.

### NFZ-16 — Automatizirana softverska provjerljivost

**Zahtjev:** Aplikacija mora ključna pravila validacije, geometrije, kompenzacije, G-koda, autentikacije, persistencea, inicijalizacije i integracijskog single-element toka moći provjeriti automatiziranim testovima.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** 38 testnih izvornih datoteka u `src/test/java/`; JUnit i Maven Surefire konfiguracija u `pom.xml`.  
**Povezani UI:** FXML-controller ugovor i numeric parser imaju automatizirane testove; JavaFX vizualno ponašanje nema.  
**Povezani dio:** svi stabilni backend slojevi i composition root.  
**Testni dokaz:** puni Maven rezultat od 141 uspješnog testa.  
**Napomena za završni rad:** Softverska pokrivenost nije isto što i formalna mjera code coveragea, usability test ili fizički CNC test.

### NFZ-17 — Ograničena RichAuto A11 kompatibilnost

**Zahtjev:** Aplikacija mora RichAuto A11 izlaz opisivati kao implementiran i softverski provjeren dok se work zero, osi, Z-smjer, prihvat naredbi i sigurno fizičko ponašanje ne potvrde na ciljnom ZK-1325 / RichAuto A11 stroju.

**Status:** IMPLEMENTIRANO / TESTIRANO — SOFTVERSKI; NIJE TESTIRANO FIZIČKI  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java` odvojeno vodi emission postavke i `physicallyConfirmedCapabilities`; `referenceProgramProfile` vraća prazan skup fizičkih potvrda; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` navodi da program nije fizički testiran.  
**Povezani UI:** `MainFormController#handleGenerate`, `#applyMachiningPreset`.  
**Povezani dio:** gcode profil, generator, service i UI.  
**Testni dokaz:** `RichAutoA11ProfileTest`, `RichAutoA11ProgramEnvelopeTest`, `RichAutoA11GCodeGeneratorTest`, `ReferenceRectangleAuditTest`.  
**Napomena za završni rad:** Nema dokaza da je Korak 18 — prvi kontrolirani fizički test — proveden.

## Usporedba sa starim NFR popisom

| Stara stavka | Aktualni zaključak |
|---|---|
| NFR-1 — jednostavnost korištenja | UI tijek je implementiran, ali jednostavnost nije potvrđena usability ili potpunim ručnim vizualnim testom. |
| NFR-2 — jasna validacija i pogreške | Validacijske poruke i velik dio error mappinga su implementirani i softverski testirani; stvarni prikaz svih scenarija u JavaFX-u nije testiran. |
| NFR-3 — odvajanje UI-a i poslovne logike | Uglavnom točno i implementirano; stvarna podjela ima više jasno izdvojenih slojeva nego stari opis sa šest područja. |
| NFR-4 — modularnost i proširenje | Djelomično točno. Postoje sučelja i odvojene odgovornosti, ali novi oblik nije plug-in dodatak i traži promjene na više poznatih mjesta. |
| NFR-5 — pouzdanost generiranih podataka | Validacija i deterministički generator softverski su testirani; RichAuto kompatibilnost i sigurnost obrade nisu fizički potvrđene. |
| NFR-6 — konzistentne jedinice | Milimetri, `G21` i formatiranje implementirani su i testirani na softverskoj razini. |
| NFR-7 — održivost i čitljivost | Slojevi i specijalizirane klase daju konkretan dokaz, ali održivost je kvalitativna tvrdnja i velika glavna UI klasa ostaje ograničenje. |
| NFR-8 — sigurno rukovanje svim pogreškama | Preširoka tvrdnja. Očekivane poslovne pogreške uglavnom su obrađene, ali nema globalnog handlera, potpunog UI error testa ni zaštite od svakog startup/runtime kvara. |

Stari NFR popis nije obuhvaćao stvarno implementirane sigurnosne i pouzdanosne mehanizme: PBKDF2 zaštitu lozinki, parametrizirani SQL, transakcijsko spremanje, idempotentni bootstrap, restart-safe H2 pohranu, stroga `.nc` export pravila, FXML ugovorni test ni izričito odvajanje softverske od fizičke RichAuto potvrde. Te su stavke dodane kao novi NFZ kandidati.

Za Iteraciju 2 trenutačno nema implementiranog layout algoritma pa se ne može auditirati njegova kvaliteta, učinkovitost, iskorištenje materijala ili performanse. Pseudojezik i AI nisu dio zahtjeva stabilne aplikacije.
