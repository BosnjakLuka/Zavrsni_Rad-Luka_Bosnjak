# Važne projektne odluke

Ovdje se zapisuju samo potvrđene odluke koje mijenjaju arhitekturu, tehnologiju, ponašanje aplikacije ili način na koji će se aplikacija kasnije opisivati u završnom radu.

Ne zapisuj obične implementacijske detalje, privremene eksperimente ni nepotvrđene pretpostavke. Postojeće odluke iz `AGENTS.md` ne kopiraj bez nove potrebe; ovdje se bilježi njihov nastanak ili kasnija promjena.

## Domenski model ploče materijala

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO

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
