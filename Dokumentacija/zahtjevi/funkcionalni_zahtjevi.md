# Audit funkcionalnih zahtjeva

**Datum audita:** 2026-08-27  
**Opseg:** trenutačni Java/JavaFX kod, FXML resursi, H2/JDBC persistence i automatizirani testovi  
**Glavni izvor istine:** produkcijski kod i testovi; `Dokumentacija/plan_implementacije.md`, razvojne bilješke i stariji FR popis služe samo za usporedbu.

## Legenda statusa

- **IMPLEMENTIRANO / TESTIRANO** — ponašanje postoji u produkcijskom kodu i pokriveno je relevantnim testom koji je izvršen u sklopu punog Maven testnog skupa.
- **IMPLEMENTIRANO / NIJE TESTIRANO** — ponašanje postoji u produkcijskom kodu, ali njegov cijeli korisnički ili UI tok nema odgovarajući automatizirani ni evidentirani ručni test.
- **ITERACIJA 2 / TESTNA FAZA** — zahtjev pripada Koraku 19 ili kasnijem koraku i nije funkcionalnost stabilne single-element aplikacije.

Aktualna softverska provjera izvršena je naredbom Maven `test`: **141 test, 0 neuspjeha, 0 pogrešaka i 0 preskočenih testova**. Taj rezultat nije fizički test na ZK-1325 / RichAuto A11.

## Stabilna aplikacija — Iteracija 1

### FZ-01 — Registracija korisnika

**Zahtjev:** Aplikacija mora omogućiti registraciju novog korisnika s jedinstvenim normaliziranim korisničkim imenom, imenom, prezimenom i lozinkom duljine od 8 do 128 znakova te mu dodijeliti aktivnu rolu `OPERATOR`.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/AuthService.java` — `AuthService#register`, `normalizeUsername`, `validateRegistration`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcUserRepository.java` — `JdbcUserRepository#save`.  
**Povezani UI:** `registration.fxml`; `RegistrationController#handleRegister`.  
**Povezani dio:** `AuthService`, `UserRepository`, `RoleRepository`, `PasswordHasher`.  
**Testni dokaz:** `AuthServiceIntegrationTest#registersNormalizedActiveOperatorAndLogsInAndOut` i `#rejectsDuplicateUsernameAndInvalidRegistrationData`.  
**Napomena za završni rad:** Registracija je lokalna desktop funkcionalnost bez vanjskog identity frameworka.

### FZ-02 — Prijava korisnika

**Zahtjev:** Aplikacija mora omogućiti aktivnom korisniku prijavu ispravnim korisničkim imenom i lozinkom te odbiti nepostojećeg, deaktiviranog ili pogrešno autentificiranog korisnika.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/AuthService.java` — `AuthService#login`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcUserRepository.java` — `JdbcUserRepository#findByUsername`.  
**Povezani UI:** `login.fxml`; `LoginController#handleLogin`.  
**Povezani dio:** `AuthService`, `PasswordHasher`, `SessionContext`, `UserRepository`.  
**Testni dokaz:** sva tri testa u `AuthServiceIntegrationTest`.  
**Napomena za završni rad:** Neispravni podaci daju istu poruku, dok deaktivirani račun ima posebno objašnjenje.

### FZ-03 — Odjava korisnika

**Zahtjev:** Aplikacija mora omogućiti odjavu brisanjem aktivne memorijske sesije i povratkom na ekran za prijavu.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/AuthService.java` — `AuthService#logout`; `src/main/java/hr/lukabosnjak/service/SessionContext.java` — `SessionContext#logout`.  
**Povezani UI:** `MainFormController#handleLogout`, `SavedProgramsController#handleLogout`, `CatalogController#handleLogout`; `Main#showLogin`.  
**Povezani dio:** `AuthService`, `SessionContext`, `ApplicationNavigation`.  
**Testni dokaz:** `AuthServiceIntegrationTest#registersNormalizedActiveOperatorAndLogsInAndOut`.  
**Napomena za završni rad:** Sesija vrijedi samo tijekom jednog procesa aplikacije.

### FZ-04 — Aktivna korisnička sesija i zaštita navigacije

**Zahtjev:** Aplikacija mora čuvati trenutačno prijavljenog korisnika tijekom rada procesa i spriječiti otvaranje glavnog i pomoćnih ekrana bez prijave.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/SessionContext.java` — `currentUser`, `isAuthenticated`; `src/main/java/hr/lukabosnjak/app/Main.java` — `showMain`, `showMainWithJob`, `showSavedPrograms`, `showCatalog`.  
**Povezani UI:** svi ekrani iza `ApplicationNavigation`.  
**Povezani dio:** `SessionContext`, `Main`, `ApplicationCompositionRoot`.  
**Testni dokaz:** `AuthServiceIntegrationTest`; `ApplicationCompositionRootIntegrationTest#preservesBootstrapUserAndSavedProgramAcrossApplicationRestart`.  
**Napomena za završni rad:** Sesija nije trajna i ne obnavlja se nakon ponovnog pokretanja aplikacije.

### FZ-05 — Role i RBAC

**Zahtjev:** Sustav mora ograničiti upravljanje korisnicima na rolu `ADMIN`, a upravljanje materijalima, strojevima i alatima na role `ADMIN` i `ENGINEER`.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/AuthorizationService.java` — `Permission`, `require`, `isAllowed`; `src/main/java/hr/lukabosnjak/service/UserManagementService.java` i `ReferenceDataManagementService.java` pozivaju odgovarajuće provjere.  
**Povezani UI:** `MainFormController#initialize`, `#handleUserManagement`; `CatalogController#initialize`; `Main#showUserManagement`.  
**Povezani dio:** `AuthorizationService`, `SessionContext`, upravljački servisi.  
**Testni dokaz:** `AuthorizationServiceTest#onlyAdminMayManageUsers` i `#operatorMayGenerateButNotManageReferenceData`.  
**Napomena za završni rad:** Dozvola `GENERATE_PROGRAM` postoji i izolirano je testirana, ali je `ProgramGenerationService` izravno ne provjerava; generatoru se pristupa kroz autentificiranu navigaciju.

### FZ-06 — Upravljanje korisnicima

**Zahtjev:** Aplikacija mora administratoru omogućiti pregled korisnika, promjenu njihove role te aktivaciju ili deaktivaciju računa, uz zabranu deaktiviranja vlastitog računa i uklanjanja posljednjeg aktivnog administratora.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/UserManagementService.java` — `loadUsers`, `loadRoles`, `setActive`, `changeRole`, `countActiveAdmins`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcUserRepository.java` — `updateActive`, `updateRole`.  
**Povezani UI:** `user-management.fxml`; `UserManagementController`.  
**Povezani dio:** `UserManagementService`, `AuthorizationService`, `UserRepository`, `RoleRepository`.  
**Testni dokaz:** RBAC ulaz u funkcionalnost pokriva `AuthorizationServiceTest`, ali nema zasebnog testa za promjenu role, aktivnog statusa i zaštitu posljednjeg administratora.  
**Napomena za završni rad:** Role su fiksni sistemski podaci; aplikacija ne nudi CRUD rola ni promjenu vlastite lozinke.

### FZ-07 — Upravljanje vrstama materijala

**Zahtjev:** Aplikacija mora ovlaštenom korisniku omogućiti dodavanje, uređivanje te deaktiviranje i ponovno aktiviranje vrsta materijala.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java` — `createMaterialType`, `updateMaterialType`, `setMaterialTypeActive`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMaterialTypeRepository.java`.  
**Povezani UI:** `catalog.fxml`, `material-type-form.fxml`; `CatalogController`, `MaterialTypeFormController`.  
**Povezani dio:** service, validation i persistence slojevi za `MaterialType`.  
**Testni dokaz:** stvaranje, normalizacija i duplikati testirani su u `ReferenceDataManagementServiceTest`; cijeli edit/toggle UI tok nije testiran.  
**Napomena za završni rad:** Deaktivacija je soft-delete preko `deleted_at`, a aktivni zapisi koriste se u generatoru.

### FZ-08 — Upravljanje CNC strojevima

**Zahtjev:** Aplikacija mora ovlaštenom korisniku omogućiti dodavanje, uređivanje te deaktiviranje i ponovno aktiviranje CNC strojeva bez izmišljanja nepoznatih tehničkih granica.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java` — `createMachine`, `updateMachine`, `setMachineActive`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcCncMachineRepository.java`.  
**Povezani UI:** `catalog.fxml`, `cnc-machine-form.fxml`; `CatalogController`, `CncMachineFormController`.  
**Povezani dio:** `ReferenceDataValidator`, service i persistence slojevi.  
**Testni dokaz:** stvaranje i validacija testirani su; cijeli edit/toggle UI tok nije testiran.  
**Napomena za završni rad:** Z, feed i spindle granice mogu ostati nepoznate (`null`), dok su naziv, kontroler i XY područje obvezni.

### FZ-09 — Upravljanje alatima

**Zahtjev:** Aplikacija mora ovlaštenom korisniku omogućiti dodavanje, uređivanje te deaktiviranje i ponovno aktiviranje alata povezanih s odabranim CNC strojem, uz jedinstven broj alata unutar stroja.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java` — `createTool`, `updateTool`, `setToolActive`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcToolRepository.java`; `src/main/resources/db/schema.sql` — `uq_tool_machine_number`.  
**Povezani UI:** `catalog.fxml`, `tool-form.fxml`; `CatalogController`, `ToolFormController`.  
**Povezani dio:** `ReferenceDataValidator`, service i persistence slojevi.  
**Testni dokaz:** stvaranje, veza sa strojem i jedinstvenost broja testirani su; cijeli edit/toggle UI tok nije testiran.  
**Napomena za završni rad:** `type`, rezna duljina i broj oštrica ostaju opcionalni; `ToolType` enum nije uveden.

### FZ-10 — Učitavanje aktivnih referentnih podataka

**Zahtjev:** Aplikacija mora u generatorskom toku prikazati aktivne strojeve i materijale te aktivne alate koji pripadaju odabranom stroju.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ReferenceDataService.java` — `loadMachines`, `loadTools`; `src/main/java/hr/lukabosnjak/service/MaterialReferenceDataService.java` — `loadMaterialTypes`.  
**Povezani UI:** `MainFormController#loadReferenceData`, `#loadToolsForSelectedMachine`.  
**Povezani dio:** read-service i repository slojevi.  
**Testni dokaz:** `ReferenceDataServiceTest#returnsMachinesAndToolsForSelectedMachine`; JDBC integracijski testovi.  
**Napomena za završni rad:** Upravljanje katalogom i odabir podataka za posao koriste odvojene servisne tokove.

### FZ-11 — Odabir geometrijskog oblika

**Zahtjev:** Aplikacija mora omogućiti odabir kvadrata, pravokutnika, kruga ili jednakostraničnog trokuta.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/domain/enums/ShapeType.java`; `src/main/java/hr/lukabosnjak/domain/enums/ShapeSubtype.java` — `EQUILATERAL`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `initialize`.  
**Povezani UI:** `main-form.fxml`; `shapeTypeComboBox`.  
**Povezani dio:** domain enumovi, `ShapeValidator`, `ToolPathService`.  
**Testni dokaz:** `ShapeValidatorTest#acceptsAllSupportedShapes`; `FourShapeCompensationIntegrationTest`.  
**Napomena za završni rad:** Trokut je namjerno ograničen na jednakostranični podtip.

### FZ-12 — Dinamički unos dimenzija oblika

**Zahtjev:** Aplikacija mora nakon odabira oblika prikazati samo potrebna polja za unos dimenzija u milimetrima: stranicu kvadrata, širinu i visinu pravokutnika, promjer kruga ili stranicu trokuta.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `updateShapeFields`, `addShapeInput`, `readGenerationRequest`; `src/main/java/hr/lukabosnjak/ui/controller/NumericInputParser.java`.  
**Povezani UI:** dinamički sadržaj `dynamicShapeFields` u `main-form.fxml`.  
**Povezani dio:** UI parser, `Shape`, `ShapeValidator`.  
**Testni dokaz:** `NumericInputParserTest`, `FxmlControllerContractTest`, `ShapeValidatorTest`.  
**Napomena za završni rad:** Krug se definira promjerom, a ne radijusom.

### FZ-13 — Unos podataka o ploči

**Zahtjev:** Aplikacija mora omogućiti odabir vrste materijala te unos širine, visine i debljine ploče u milimetrima.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `readGenerationRequest`; `src/main/java/hr/lukabosnjak/validation/MaterialSheetValidator.java` — `validate`; `src/main/java/hr/lukabosnjak/domain/entities/MaterialSheet.java`.  
**Povezani UI:** polja materijala u `main-form.fxml`.  
**Povezani dio:** domain, validation i persistence snapshot `JdbcMaterialSheetRepository`.  
**Testni dokaz:** `MaterialSheetValidatorTest`; JDBC i composition-root integracijski testovi.  
**Napomena za završni rad:** Debljina ploče ostaje odvojena od dubine rezanja.

### FZ-14 — Parametri obrade i referentni preset

**Zahtjev:** Aplikacija mora omogućiti korištenje i izmjenu brzine vretena, posmaka, brzine poniranja, dubine reza, dubine prolaza i sigurnog Z, uz jasno označavanje da početni preset nije fizički potvrđen.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/MachiningParametersPreset.java` — `referenceDefaults`, `toParameters`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `applyMachiningPreset`, `readGenerationRequest`.  
**Povezani UI:** napredne postavke i `machiningPresetLabel` u `main-form.fxml`.  
**Povezani dio:** service preset, domain snapshot i validation.  
**Testni dokaz:** `MachiningParametersPresetTest`, `MachiningParametersValidatorTest`.  
**Napomena za završni rad:** Vrijednosti preseta su softverske reference, a ne potvrđeni parametri sigurnog rezanja.

### FZ-15 — Validacija ulaznih podataka

**Zahtjev:** Sustav mora prije generiranja odbiti prazne, nenumeričke, nekonačne, nulte ili negativne obvezne vrijednosti te nevaljane odnose stroja, alata, oblika i parametara obrade.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/NumericInputParser.java`; `src/main/java/hr/lukabosnjak/validation/` — validator klase; `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `generate`.  
**Povezani UI:** `MainFormController#handleGenerate` prikazuje poruku i čisti nevaljani preview.  
**Povezani dio:** UI parser, validation i service sloj.  
**Testni dokaz:** svi validator testovi, `NumericInputParserTest` i `ProgramGenerationServiceTest`.  
**Napomena za završni rad:** Validacija je slojevita: sintaktičko parsiranje u UI pomoćnoj klasi, a ponovno upotrebljiva pravila izvan controllera.

### FZ-16 — Provjera stane li putanja na ploču i stroj

**Zahtjev:** Sustav mora prije generiranja provjeriti nalazi li se kompenzirana putanja jednog oblika unutar ploče i XY radnog područja odabranog stroja.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/validation/SingleShapeFitValidator.java` — `validate`; `src/main/java/hr/lukabosnjak/geometry/ToolPathBoundsCalculator.java` — `calculate`; `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `generate`.  
**Povezani UI:** rezultat se prikazuje preko `MainFormController#handleGenerate`.  
**Povezani dio:** validation, geometry/toolpath i service sloj.  
**Testni dokaz:** `SingleShapeFitValidatorTest`, `ToolPathBoundsCalculatorTest`, `ProgramGenerationServiceTest`.  
**Napomena za završni rad:** Provjera koristi stvarnu putanju centra alata, ali ne računa kapacitet ni raspored više elemenata.

### FZ-17 — Generiranje geometrijske putanje

**Zahtjev:** Sustav mora iz valjanog odabranog oblika izraditi zatvorenu geometrijsku putanju od linijskih ili kružnih segmenata, odvojeno od tekstualnog G-koda.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/geometry/ToolPathService.java` — `generate`, `generateRectangle`, `generateEquilateralTriangle`, `generateCircle`; `src/main/java/hr/lukabosnjak/geometry/ToolPath.java`, `LineSegment.java`, `ArcSegment.java`.  
**Povezani UI:** nema izravnog geometrijskog izračuna u controlleru; tok pokreće `MainFormController#handleGenerate`.  
**Povezani dio:** domain shape, validation i geometry/toolpath sloj.  
**Testni dokaz:** `ToolPathServiceTest`, `ToolPathTranslationTest`, `FourShapeCompensationIntegrationTest`.  
**Napomena za završni rad:** Krug je modeliran kao dvije povezane polukružne putanje.

### FZ-18 — Geometrijska kompenzacija alata

**Zahtjev:** Sustav mora nominalnu konturu pretvoriti u putanju centra alata primjenom unutarnje ili vanjske kompenzacije prema polumjeru odabranog alata.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/geometry/ToolPathCompensationService.java` — `compensate`, `compensateLines`, `compensateArcs`.  
**Povezani UI:** glavni tok trenutačno koristi `CutSide.INSIDE`; nema korisničkog odabira strane reza.  
**Povezani dio:** geometry/toolpath i `ProgramGenerationService`.  
**Testni dokaz:** `ToolPathCompensationServiceTest`, `FourShapeCompensationIntegrationTest`, `ReferenceRectangleAuditTest`.  
**Napomena za završni rad:** Kompenzacija se računa u aplikaciji; generator ne emitira `G41`, `G42`, `G40` ni `D`.

### FZ-19 — Više dubinskih prolaza

**Zahtjev:** Sustav mora podijeliti ukupnu dubinu reza na konačan niz prolaza prema vrijednosti `stepDown` i završiti točno na ciljnoj dubini.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/gcode/PassDepthCalculator.java` — `calculate`; `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java` — `generate`.  
**Povezani UI:** polja `cutDepthInput` i `stepDownInput` u glavnoj formi.  
**Povezani dio:** gcode sloj i machining parametri.  
**Testni dokaz:** `PassDepthCalculatorTest`, `RichAutoA11GCodeGeneratorTest`.  
**Napomena za završni rad:** Decimalni izračun izbjegava dodatni prolaz zbog pogreške binarnog floating-pointa.

### FZ-20 — Generiranje RichAuto A11 G-koda za jedan element

**Zahtjev:** Aplikacija mora iz pripremljenog zatvorenog `ToolPatha` i parametara obrade generirati deterministički G-kod za jedan element prema implementiranom RichAuto A11 profilu.

**Status:** IMPLEMENTIRANO / TESTIRANO — SOFTVERSKI; NIJE TESTIRANO FIZIČKI  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java` — `generate`; `src/main/java/hr/lukabosnjak/gcode/RichAutoA11ProgramEnvelope.java`; `RichAutoA11Profile.java` — `referenceProgramProfile`; `GCodeFormatter.java`.  
**Povezani UI:** `MainFormController#handleGenerate`.  
**Povezani dio:** service, geometry/toolpath i gcode slojevi.  
**Testni dokaz:** gcode testovi, `FourShapeCompensationIntegrationTest`, `ReferenceRectangleAuditTest`.  
**Napomena za završni rad:** Softverski izlaz uključuje konfigurirani profil, ali work zero, osi, Z-smjer i ponašanje konkretnog kontrolera nisu potvrđeni fizičkim testom.

### FZ-21 — Pregled G-koda

**Zahtjev:** Aplikacija mora korisniku prikazati generirani ili spremljeni G-kod prije spremanja, ponovnog korištenja ili izvoza.

**Status:** IMPLEMENTIRANO / NIJE TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `handleGenerate`, `populateForm`; `src/main/java/hr/lukabosnjak/ui/controller/SavedProgramsController.java` — `initialize`; `src/main/java/hr/lukabosnjak/gcode/GCodeProgram.java` — `text`.  
**Povezani UI:** `gCodePreview` u `main-form.fxml` i `saved-programs.fxml`.  
**Povezani dio:** UI, `SavedJobService`, gcode model.  
**Testni dokaz:** serializacija G-koda i FXML ugovor jesu testirani, ali prikaz i pregled kroz stvarni JavaFX runtime nisu ručno ni automatizirano potvrđeni.  
**Napomena za završni rad:** Preview je read-only i namijenjen provjeri prije izvoza.

### FZ-22 — Spremanje programa i naloga

**Zahtjev:** Aplikacija mora prijavljenom korisniku omogućiti spremanje imenovanog programa zajedno s G-kodom te snapshotovima oblika, ploče i parametara obrade i referencama na korisnika, stroj i alat.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `handleSave`; `src/main/java/hr/lukabosnjak/service/SavedJobService.java` — `save`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java` — `save`, `insertJob`.  
**Povezani UI:** glavni ekran i gumb `Spremi`.  
**Povezani dio:** service i transakcijski persistence sloj.  
**Testni dokaz:** `JdbcMachiningJobRepositoryIntegrationTest`, `ApplicationCompositionRootIntegrationTest`.  
**Napomena za završni rad:** Svako spremanje stvara novi snapshot; nema uređivanja ili prepisivanja postojećeg naloga.

### FZ-23 — Pregled spremljenih programa

**Zahtjev:** Aplikacija mora prikazati popis spremljenih programa i omogućiti pregled njihova G-koda i osnovnih podataka.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/SavedJobService.java` — `loadAll`, `gCodeProgramOf`; `src/main/java/hr/lukabosnjak/ui/controller/SavedProgramsController.java` — `initialize`, `refresh`; `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java` — `findAll`.  
**Povezani UI:** `saved-programs.fxml`; quick-access lista u `main-form.fxml`.  
**Povezani dio:** UI, service i persistence.  
**Testni dokaz:** `SavedJobServiceTest`; persistence i composition-root integracijski testovi; FXML ugovor.  
**Napomena za završni rad:** Trenutačni kod prikazuje sve spremljene programe svim prijavljenim rolama; nema filtriranja na vlastite naloge.

### FZ-24 — Ponovno otvaranje spremljenog naloga

**Zahtjev:** Aplikacija mora omogućiti ponovno otvaranje spremljenog naloga i ponovno popuniti generator spremljenim oblikom, materijalom, strojem, alatom, parametrima i G-kodom.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/SavedJobService.java` — `loadById`, `gCodeProgramOf`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `populateForm`, `loadSavedJob`; `src/main/java/hr/lukabosnjak/ui/controller/SavedProgramsController.java` — `handleOpen`; `src/main/java/hr/lukabosnjak/app/Main.java` — `showMainWithJob`.  
**Povezani UI:** glavni ekran i ekran spremljenih programa.  
**Povezani dio:** navigation, service i persistence.  
**Testni dokaz:** `SavedJobServiceTest`; `ApplicationCompositionRootIntegrationTest` round-trip i restart testovi.  
**Napomena za završni rad:** Otvoreni nalog može poslužiti kao predložak za novo spremanje, ali se postojeći zapis ne mijenja.

### FZ-25 — Izvoz `.nc` programa

**Zahtjev:** Aplikacija mora omogućiti izvoz generiranog ili spremljenog G-koda u novu datoteku s ekstenzijom `.nc`.

**Status:** IMPLEMENTIRANO / TESTIRANO  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ProgramExportService.java` — `export`; `src/main/java/hr/lukabosnjak/gcode/NcExportService.java` — `export`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` i `SavedProgramsController.java` — `handleExport`.  
**Povezani UI:** `FileChooser` na glavnom i saved-programs ekranu.  
**Povezani dio:** UI, service granica i gcode export.  
**Testni dokaz:** `NcExportServiceTest`, `ProgramExportServiceTest`, `ApplicationCompositionRootIntegrationTest`.  
**Napomena za završni rad:** Export koristi US-ASCII, zahtijeva `.nc` i namjerno ne prepisuje postojeću datoteku.

## Iteracija 2 / testna faza

### FZ-26 — Unos količine

**Zahtjev:** Aplikacija mora omogućiti korisniku unos cijelog broja jednakih elemenata većeg od nule.

**Status:** ITERACIJA 2 / TESTNA FAZA  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationRequest.java` nema quantity ulaz; `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `SINGLE_ELEMENT_QUANTITY`; `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — `handleSave` postavlja količinu na `1`. `MachiningJob.quantity` i SQL stupac samo pripremaju model za buduću iteraciju.  
**Povezani UI:** ne postoji.  
**Povezani dio:** samo domain/persistence priprema i validacija pozitivne vrijednosti.  
**Testni dokaz:** Nema testa korisničkog quantity toka; postojeći testovi potvrđuju samo internu single-element vrijednost `1` i odbijanje nepozitivnog domenskog polja.  
**Napomena za završni rad:** Postojanje stupca nije dokaz implementirane quantity funkcionalnosti.

### FZ-27 — Kapacitet ploče i potreban broj ploča

**Zahtjev:** Sustav mora za zadanu količinu izračunati kapacitet jedne ploče i potreban broj ploča prema implementiranom pravilu raspoređivanja.

**Status:** ITERACIJA 2 / TESTNA FAZA  
**Dokaz u kodu:** U `src/main/java/hr/lukabosnjak/` i `src/test/java/hr/lukabosnjak/` nema produkcijske klase, metode ni testa za `capacity` ili `requiredSheets`.  
**Povezani UI:** ne postoji.  
**Povezani dio:** budući layout/service sloj.  
**Testni dokaz:** Nema testa kapaciteta ploče ni potrebnog broja ploča.  
**Napomena za završni rad:** Zahtjev se ne smije prikazati kao implementiran na temelju `plan_implementacije.md`.

### FZ-28 — Raspoređivanje više jednakih elemenata

**Zahtjev:** Aplikacija mora izračunati valjan raspored više jednakih elemenata unutar granica ploče bez tvrdnje da je rezultat matematički ili globalno optimalan.

**Status:** ITERACIJA 2 / TESTNA FAZA  
**Dokaz u kodu:** U `src/main/java/hr/lukabosnjak/` ne postoji produkcijski `layout` paket, algoritam raspoređivanja ni model placementa; u `src/test/java/hr/lukabosnjak/` nema pripadajućeg testa.  
**Povezani UI:** ne postoji.  
**Povezani dio:** budući layout sloj.  
**Testni dokaz:** Nema layout testa.  
**Napomena za završni rad:** Trenutačni fit validator provjerava samo jednu već pripremljenu putanju.

### FZ-29 — Batch G-kod i prikaz rasporeda

**Zahtjev:** Aplikacija mora nakon raspoređivanja generirati putanje i G-kod za sve raspoređene elemente te prikazati rezultate količine i rasporeda.

**Status:** ITERACIJA 2 / TESTNA FAZA  
**Dokaz u kodu:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java` — `generate` izrađuje i gcode generatoru predaje točno jedan `ToolPath`; u `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` nema quantity/layout UI toka.  
**Povezani UI:** ne postoji.  
**Povezani dio:** budući layout, service, gcode i UI tok.  
**Testni dokaz:** Nema batch G-kod ni quantity/layout UI testa.  
**Napomena za završni rad:** Trenutačni generator jest testiran za više dubinskih prolaza jednog elementa, što nije isto što i batch obrada više elemenata.

## Usporedba sa starim FR popisom

| Stara stavka | Aktualni zaključak |
|---|---|
| FR-1 — odabir oblika | Implementirano; sada `FZ-11`. |
| FR-2 — dinamičke dimenzije | Implementirano; sada `FZ-12`. |
| FR-3 — validacija | Implementirano i prošireno na ploču, parametre, stroj, alat i referentne podatke; sada `FZ-15`. |
| FR-4 — dimenzije ploče | Implementirano; sada `FZ-13`. |
| FR-5 — stane li oblik | Implementirano za kompenziranu putanju jednog oblika i granice stroja; sada `FZ-16`. |
| FR-6 — geometrijska putanja | Implementirano, ali stara stavka nije obuhvaćala stvarno dodanu kompenzaciju alata; sada `FZ-17` i `FZ-18`. |
| FR-7 — generiranje i prikaz | Bila je složena od dviju odgovornosti; razdvojeno na `FZ-20` i `FZ-21`. |
| FR-8 — prikaz | Implementirano, ali nije vizualno testirano; `FZ-21`. |
| FR-9 — spremanje | Implementirano kao transakcijski snapshot; `FZ-22`. |
| FR-10 — pregled spremljenih programa | Implementirano; `FZ-23`. |
| FR-11 — quick access/reopen | Implementirano; `FZ-24`. |
| FR-12 do FR-15 | Nisu implementirani; ostaju `FZ-26`–`FZ-29` u Iteraciji 2 / testnoj fazi. |

Stari FR popis dodatno nije sadržavao registraciju, prijavu, odjavu, sesiju, RBAC, upravljanje korisnicima i katalozima, referentni machining preset, geometrijsku kompenzaciju alata, više dubinskih prolaza ni `.nc` export kao samostalan zahtjev. Te su stvarno implementirane funkcionalnosti dodane kao novi kandidati.

Promjena vlastite lozinke nije implementirana. Također nije implementirana stara RBAC zamisao prema kojoj `OPERATOR` vidi samo vlastite spremljene programe, dok `ADMIN` i `ENGINEER` vide sve: aktualni `SavedJobService#loadAll` nema korisnički ni role filter.

Pseudojezik i AI nisu uključeni jer nisu dio stabilne aplikacije ni trenutačnog produkcijskog koda.
