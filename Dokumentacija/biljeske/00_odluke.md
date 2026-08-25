# Važne projektne odluke

Ovdje se zapisuju samo potvrđene odluke koje mijenjaju arhitekturu, tehnologiju, ponašanje aplikacije ili način na koji će se aplikacija kasnije opisivati u završnom radu.

Ne zapisuj obične implementacijske detalje, privremene eksperimente ni nepotvrđene pretpostavke. Postojeće odluke iz `AGENTS.md` ne kopiraj bez nove potrebe; ovdje se bilježi njihov nastanak ili kasnija promjena.

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
