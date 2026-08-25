# Važne projektne odluke

Ovdje se zapisuju samo potvrđene odluke koje mijenjaju arhitekturu, tehnologiju, ponašanje aplikacije ili način na koji će se aplikacija kasnije opisivati u završnom radu.

Ne zapisuj obične implementacijske detalje, privremene eksperimente ni nepotvrđene pretpostavke. Postojeće odluke iz `AGENTS.md` ne kopiraj bez nove potrebe; ovdje se bilježi njihov nastanak ili kasnija promjena.

## Transakcijska granica spremanja MachiningJob agregata

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `JdbcMachiningJobRepository.save` posjeduje jednu JDBC transakciju za insert novih `MaterialSheet`, `MachiningParameters` i `Shape` snapshot zapisa te završni insert `MachiningJob`. Potpuno ponovno učitavanje spremljenog agregata izvodi se na istoj vezi prije commita. Bilo koja SQL ili runtime pogreška prije uspješnog commita uzrokuje rollback cijele transakcije.

**Razlog:** Snapshot zapisi nemaju samostalan život izvan jednog joba. Jedna transakcija sprječava djelomično spremljene snapshote ako završni job insert ne uspije te drži SQL i rekonstrukciju agregata unutar persistence sloja.

**Razmotrene alternative:** Uzastopno pozivanje javnih snapshot repository metoda nije odabrano jer svaka otvara vlastitu vezu i ne može sudjelovati u istoj transakciji. Spremanje `MaterialType`, `CncMachine`, `Tool` i `User` zajedno s jobom nije odabrano jer su to unaprijed postojeće reference stvarnog workflowa. `update` nije uveden jer quick-access ponovno učitava podatke, a ne mijenja postojeći job.

**Utjecaj na implementaciju:** Snapshot JDBC implementacije imaju package-private insert metode koje koriste otvorenu vezu bez commita ili zatvaranja. `MachiningJobRepository` izlaže samo `save`, `findById` i `findAll`; service i controller ne trebaju SQL za ponovno sastavljanje cijelog joba.

---

## Konvencija jednostavnih JDBC repositoryja

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** Repository ugovori nalaze se u `persistence.repository`, a JDBC implementacije u `persistence.jdbc` te koriste nazive `XRepository` i `JdbcXRepository`. Ne postoji generički `CrudRepository`; svaki ugovor izlaže samo operacije potrebne workflowu. U milestoneu 7.1 `save` je insert-only, prima novi entitet bez ID-a i vraća ponovno učitani entitet s bazno generiranim identitetom i timestampovima.

**Razlog:** Specifični ugovori čuvaju mali i razumljiv persistence API, a odvojene JDBC implementacije zadržavaju SQL izvan domene i JavaFX controllera. Ponovno učitavanje nakon inserta provjerava stvarno mapiranje baze u domenski objekt.

**Razmotrene alternative:** Generički puni CRUD nije odabran jer bi unaprijed uveo nepotrebne operacije. Spremanje cijelog `MachiningJob` agregata pripada zasebnom transakcijskom milestoneu 7.2. Mutiranje ulaznog entiteta generiranim ID-em nije odabrano.

**Utjecaj na implementaciju:** Repositoryji koriste `PreparedStatement`, try-with-resources i `ConnectionProvider`. `Role` ima samo dohvat po nazivu, a `User` samo insert i dohvat po korisničkom imenu, što je minimalna persistence podloga za odluku 6.1 bez autentikacijske i autorizacijske logike.

---

## Lokalna autentikacija, uloge i vlasništvo nad poslovima u V1

**Datum:** 2026-08-25
**Status:** ODLUČENO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

**Odluka:** V1 uključuje klasičnu lokalnu registraciju i prijavu bez OAutha i vanjskih identity providera. Postoje uloge `USER` i `ADMIN`. Javna registracija stvara aktivan `USER` račun. Pri prvom pokretanju, prije redovnog login toka, poseban first-run setup omogućuje stvaranje prvog `ADMIN` računa i unos njegove lozinke; zadana administratorska lozinka ne smije biti hardkodirana ni spremljena u repozitoriju. Lozinke se nikada ne spremaju kao čisti tekst, nego samo kao sigurni hash s podacima potrebnima za provjeru. `MACHINING_JOB.created_by_user_id` identificira prijavljenog autora: `USER` vidi i upravlja svojim poslovima, a `ADMIN` vidi sve poslove i upravlja osnovnim referentnim podacima.

**Razlog:** Funkcionalni zahtjevi prethodno nisu definirali runtime izvor za `created_by_user_id`, iako domenski i relacijski modeli već sadrže `ROLE` i `APP_USER`. Lokalna autentikacija daje tom odnosu stvarno značenje, omogućuje vlasništvo nad spremljenim poslovima i zadržava sigurnosni opseg dovoljno malim za desktop V1.

**Razmotrene alternative:** Razmotren je jedan seedani lokalni korisnik bez login UI-a, ali nije odabran jer je potvrđena potreba za registracijom i prijavom. Razmotrena je samo jedna uloga, ali su odabrane `USER` i `ADMIN`. OAuth, Google prijava, vanjski identity servisi i opsežan enterprise RBAC nisu odabrani.

**Utjecaj na implementaciju:** Budući auth milestone mora obuhvatiti registraciju, prijavu, sigurno hashiranje i provjeru lozinki, current-user/session stanje, first-run administratorski tok i autorizacijske testove. JavaFX controller samo koordinira UI; provjera vjerodajnica, pravila pristupa i persistence ostaju izvan controllera. DDL se ne izrađuje u ovoj odluci i smije se planirati tek nakon ovog pre-DDL audita.

---

## Minimalna H2/JDBC infrastruktura

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

**Odluka:** Aplikacija koristi embedded, file-based H2 preko ručnog JDBC-a s URL-om `jdbc:h2:file:./data/cnc-optimizer`. Integracijski testovi koriste zasebne in-memory H2 baze. `DatabaseConfig` centralizira JDBC URL, korisnika i razvojnu lozinku te za svaki poziv stvara novu vezu; pozivatelj je zatvara pomoću try-with-resources.

**Razlog:** File-based baza čuva aplikacijske podatke između pokretanja, dok in-memory baza izolira testove i ne ostavlja lokalne datoteke. Kratkoživuće veze izbjegavaju globalno JDBC stanje i jasno određuju vlasništvo nad resursom.

**Razmotrene alternative:** ORM i connection pool nisu odabrani jer nisu potrebni za potvrđeni opseg. In-memory baza nije odabrana za aplikacijski runtime jer ne čuva podatke nakon gašenja procesa.

**Utjecaj na implementaciju:** Budući persistence kod dobiva novu vezu iz `DatabaseConfig` i zatvara je u istom toku. Relativni file path razrješava se prema working directoryju. H2 Console ostaje zaseban razvojni alat, a SQL shema i inicijalizacija dolaze u kasnijim milestoneovima.

---

## MachiningJob agregat i snapshot granica

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `MachiningJob` povezuje korisnika koji je stvorio posao, stroj, alat, ploču materijala, parametre obrade, oblik, naziv, količinu, spremljeni G-kod i vremenske oznake. `MaterialSheet`, `MachiningParameters` i `Shape` predstavljaju snapshot podatke jednog posla, dok su `User`, `CncMachine` i `Tool` dijeljene reference koje se očekuju unaprijed spremljene. `MaterialType` se također očekuje unaprijed spremljen preko veze iz `MaterialSheet`.
**Razlog:** Snapshoti čuvaju dimenzije ploče, parametre obrade i oblik korištene za konkretan posao neovisno o budućim poslovnim unosima, dok se stabilni zajednički zapisi korisnika, stroja, alata i vrste materijala ne dupliciraju za svaki posao.
**Razmotrene alternative:** Spremanje samo ID-eva u domenskoj klasi nije odabrano jer bi model svodio na SQL strukturu. Dijeljenje jednog promjenjivog `MaterialSheet`, `MachiningParameters` ili `Shape` zapisa između više poslova nije odabrano za V1 jer bi moglo promijeniti povijesno značenje već spremljenog posla.
**Utjecaj na implementaciju:** Domenski model samo drži objektne veze; budući persistence sloj transakcijski sprema snapshot zapise i posao, dok `User`, `CncMachine`, `Tool` i `MaterialType` moraju već imati bazni identitet. Buduća validacija provjerava da alat pripada odabranom stroju. U Iteraciji 1 workflow koristi `quantity = 1`, ali model zadržava opće `int quantity` polje bez UI unosa prije GATE 1. `gCode` je spremnik budućeg rezultata i ne generira se u agregatu.

---

## Privremena reprezentacija tipa alata

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `Tool` privremeno koristi `String type`. Ta vrijednost nije konačna zamjena za `ToolType` enum i nema unaprijed definirane konstante dok se ne evidentiraju i potvrde stvarni alati za ZK-1325.
**Razlog:** U kodu, odlukama, planu, dijagramima i projektnoj dokumentaciji ne postoji potvrđen popis tipova alata. String omogućuje dovršavanje osnovnog domenskog modela bez izmišljanja fizičkih karakteristika ili lažne konačne klasifikacije.
**Razmotrene alternative:** Uvođenje praznog ili pretpostavljenog `ToolType` enuma nije odabrano jer bi zaključalo nepotvrđene vrijednosti. Odgoda cijelog `Tool` modela do inventure bila je sigurnija, ali bi nepotrebno zaustavila ostale podatke modela koji su već potvrđeni.
**Utjecaj na implementaciju:** `Tool.type` ostaje nekontrolirani tekstualni podatak, bez hardkodiranih vrijednosti i bez enum mapiranja. Prije konačnog enuma treba prikupiti oznake ili kataloške nazive alata, reznu geometriju i namjenu, broj alata te potvrdu operatora koje kategorije trebaju postojati u V1. Promjer, rezna duljina i broj oštrica ostaju zasebna polja i ne određuju sami naziv tipa.

---

## V1 reprezentacija oblika

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `Shape` je jedan domenski entitet u paketu `domain.entities`, a potvrđene klasifikacije nalaze se u `domain.enums`. `ShapeType` sadrži samo `SQUARE`, `RECTANGLE`, `CIRCLE` i `TRIANGLE`, dok `ShapeSubtype` u V1 sadrži samo `EQUILATERAL`. Dimenzije se pohranjuju generički: kvadrat koristi A kao stranicu, pravokutnik A kao širinu i B kao visinu, krug A kao promjer, a jednakostranični trokut A kao stranicu.
**Razlog:** Jedan model odgovara planiranom persistence zapisu i izbjegava četiri gotovo prazna entiteta. Enumovi ograničavaju tipove na potvrđene vrijednosti, dok geometrijski izračun ostaje izvan podatkovnog modela.
**Razmotrene alternative:** Zasebni entiteti `Square`, `Rectangle`, `Circle` i `Triangle` nisu odabrani jer bi preuranjeno duplicirali identitet, vremenske oznake i persistence mapiranje. Stringovi za tip i podtip nisu odabrani jer dopuštaju proizvoljne vrijednosti.
**Utjecaj na implementaciju:** `shapeSubtype` je nullable za oblike kojima podtip nije smislen, a `dimensionB` i `dimensionC` mogu biti `null`. Budući UI mora prikazivati semantičke nazive poput stranice, širine, visine i promjera, a validation i geometry slojevi zasebno će tumačiti i provjeravati dimenzije.

---

## Osnovni domenski modeli i granica validacije

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** Osnovni domenski modeli `MaterialType`, `MaterialSheet`, `CncMachine`, `MachiningParameters`, `Role` i `User` nalaze se u paketu `domain.entities`. To su obične mutable Java klase s punim konstruktorom, getterima i setterima, bez ORM anotacija i bez JavaFX/JDBC ovisnosti. `MaterialSheet` koristi objektnu vezu prema `MaterialType`, a `User` prema `Role`. Potpuna pravila valjanosti modela implementirat će se u zasebnom `validation` sloju, a ne u setterima.
**Razlog:** Jedinstven paket i jednostavan javni API čine modele preglednima i pogodnima za buduće ručno JDBC mapiranje, dok odvojena validacija sprječava miješanje podatkovnog modela s pravilima pojedinog workflowa.
**Razmotrene alternative:** Naziv `domain.model` bio je razmatran, ali potvrđeni milestoneovi koriste `domain.entities` za domenske objekte s identitetom. Lokalna provjera dimenzija u `MaterialSheet` setterima zamijenjena je validacijom u zasebnom sloju. JPA/Hibernate, nepromjenjivi modeli, recordi i validacija u setterima nisu odabrani.
**Utjecaj na implementaciju:** Modeli ne hardkodiraju vrijednosti ZK-1325, `cutDepth` ostaje neovisan o `MaterialSheet.thickness`, a login, hashing, session i RBAC nisu dio ovog milestonea. Budući persistence sloj mapirat će objektne veze na strane ključeve.

---

## Domenski model ploče materijala

**Datum:** 2026-08-25
**Status:** ZAMIJENJENO MILESTONEOM 3.1

**Odluka:** `MaterialSheet` u domeni sadrži objektni odnos `MaterialType materialType`, a ne samo strani ključ. Širina, visina i debljina izražene su u milimetrima te moraju biti konačne vrijednosti veće od nule.
**Razlog:** Objektni odnos izravno izražava pripadnost ploče vrsti materijala i ne svodi domenu na SQL strukturu. Ručni JDBC repository kasnije može mapirati povezani `MaterialType`, a pri spremanju dohvatiti njegov ID. Pozitivne konačne dimenzije predstavljaju minimalnu domensku invarijantu, ne UI validaciju.
**Razmotrene alternative:** Polje `Long materialTypeId` bilo bi jednostavnije preslikati iz jednog SQL stupca, ali bi domenski odnos bio manje izražajan. Potpuno prebacivanje provjere dimenzija u UI nije odabrano jer bi dopuštalo nevaljan entitet iz drugih ulaza.
**Utjecaj na implementaciju:** Konstruktor i setteri `MaterialSheet` odbijaju nevaljane dimenzije standardnom `IllegalArgumentException`. UI parsiranje, lokalizirane poruke, SQL i JDBC mapiranje ostaju izvan entiteta.

---

## Slojevita package konvencija s podpaketima

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** Projekt zadržava slojevitu organizaciju, a unutar slojeva koristi podpakete koji jasno opisuju vrstu odgovornosti: `domain.entities`, `domain.enums`, `service.dto`, `persistence.repository`, `persistence.jdbc`, `ui.controller` i `ui.view`. Vršni paketi `app`, `config`, `validation`, `geometry`, `layout`, `gcode` i `service` zadržavaju svoje potvrđene odgovornosti. Paketi se stvaraju tek s prvom stvarno potrebnom datotekom. Naziv `entities` označava domenske objekte s identitetom, ne JPA ili ORM entitete.
**Razlog:** Struktura ostaje razumljiva osobi koja prvi put otvara projekt, a istodobno čuva arhitektonske granice. Podpaketi sprječavaju prenatrpanost slojeva bez uvođenja neodređenih spremišta poput `util` ili miješanja odgovornosti u `io`.
**Razmotrene alternative:** Razmotrena je plitka struktura s vršnim paketima `entities`, `dto`, `repository`, `ui`, `io` i `util`, kao i organizacija po funkcionalnosti. Plitka struktura slabije prikazuje pripadnost sloju, a organizacija po funkcionalnosti preuranjena je za trenutačnu veličinu projekta.
**Utjecaj na implementaciju:** `MaterialType` se nalazi u `domain.entities`. DTO-i će pripadati `service.dto`, SQL i JDBC samo `persistence.jdbc`, a `Main` ostaje minimalna ulazna točka u `app`. Ne stvaraju se prazni paketi ni placeholder klase.

---

## Domenski model vrste materijala

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `MaterialType` je obična Java klasa u paketu `domain.entities` s potvrđenim poljima `Long materialTypeId`, `String name`, nullable `String description`, `LocalDateTime createdAt`, `LocalDateTime updatedAt` i nullable `LocalDateTime deletedAt`. Za ručno JDBC mapiranje koristi puni konstruktor te gettere i settere, bez ORM anotacija.
**Razlog:** Objektni `Long` dopušta da ID bude `null` prije nego što ga dodijeli baza, a nullable `deletedAt` razlikuje aktivan zapis od soft-obrisanog zapisa. Jednostavna mutable klasa lako se konstruira iz JDBC retka i ažurira vrijednostima dobivenima iz baze.
**Razmotrene alternative:** Java `record`, nepromjenjivi model i JPA entitet nisu odabrani. Record i nepromjenjivi model otežali bi naknadno postavljanje bazom dodijeljenog ID-a, a JPA nije dio projekta.
**Utjecaj na implementaciju:** Domenski model ostaje neovisan o JavaFX-u, JDBC-u i H2 detaljima. Validacija i persistence mapiranje uvodit će se zasebnim koracima.

---

## Osnovni Java package i aplikacijska ulazna točka

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** Base package ostaje `hr.lukabosnjak`, a JavaFX ulazna klasa `Main` pripada aplikacijskom paketu `hr.lukabosnjak.app`. Ostali arhitektonski paketi stvaraju se tek kada dobiju stvarne klase.
**Razlog:** Postojeći base package usklađen je s Maven `groupId` vrijednošću, dok paket `app` jasno odvaja pokretanje aplikacije od budućeg UI-ja i poslovnih slojeva. Izbjegava se stvaranje prazne strukture bez implementacije.
**Razmotrene alternative:** Razmotreno je zadržavanje klase `Main` izravno u base packageu i stvaranje svih praznih direktorija. Nisu odabrani jer slabije izražavaju odgovornost ulazne točke odnosno ne predstavljaju stvarne Java pakete bez izvornih datoteka.
**Utjecaj na implementaciju:** Maven JavaFX plugin pokreće `hr.lukabosnjak.app.Main`. Budući paketi moraju poštovati granice UI, service, validation, geometry/toolpath, layout, gcode i persistence slojeva.

---

## Potvrđeni JDK i build alat

**Datum:** 2026-08-25  
**Status:** IMPLEMENTIRANO

**Odluka:** Projekt koristi Oracle OpenJDK 26.0.2 i Maven; Maven `artifactId` je `cnc-optimizer`.  
**Razlog:** To su potvrđene postavke postojećeg Java projekta u IntelliJ IDEA-i, a Maven daje jednostavan i pregledan lifecycle za studentski JavaFX projekt.  
**Razmotrene alternative:** Gradle je razmotren, ali nije odabran jer projekt već ima `pom.xml` i Maven konfiguraciju.  
**Utjecaj na implementaciju:** Maven compiler koristi Java release 26, a dependencyji i build pluginovi definiraju se u korijenskom `pom.xml`. Odluka o FXML-u još nije donesena.

---

## Predložak odluke

### <Kratak naziv odluke>

**Datum:** YYYY-MM-DD  
**Status:** PLANIRANO / IMPLEMENTIRANO / TESTIRANO

**Odluka:** <što je potvrđeno>  
**Razlog:** <zašto je odluka donesena>  
**Razmotrene alternative:** <stvarno razmotrene alternative ili „Nisu razmatrane”>  
**Utjecaj na implementaciju:** <koje dijelove projekta ili kasniji opis aplikacije odluka mijenja>

<!-- Nove potvrđene odluke dodaju se iznad odjeljka predloška. -->
