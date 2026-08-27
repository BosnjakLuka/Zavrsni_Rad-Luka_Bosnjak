# Važne projektne odluke

Ovdje se zapisuju samo potvrđene odluke koje mijenjaju arhitekturu, tehnologiju, ponašanje aplikacije ili način na koji će se aplikacija kasnije opisivati u završnom radu.

Ne zapisuj obične implementacijske detalje, privremene eksperimente ni nepotvrđene pretpostavke. Postojeće odluke iz `AGENTS.md` ne kopiraj bez nove potrebe; ovdje se bilježi njihov nastanak ili kasnija promjena.

## PBKDF2 format lozinki i in-memory session

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO

**Odluka:** Lozinke se hashiraju ugrađenim JDK algoritmom `PBKDF2WithHmacSHA256`, s 600.000 iteracija, 16-byte nasumičnim saltom i 256-bitnim izvedenim ključem. Baza čuva verzionirani tekstualni zapis oblika `pbkdf2-sha256$iterations$salt$hash`, pri čemu su salt i hash Base64 kodirani. Verifikacija koristi constant-time usporedbu izvedenih bajtova. Username se normalizira s `trim()` i `lowercase` pravilom, registracijska lozinka ima 8–128 znakova, a session postoji samo u memoriji procesa do logouta ili zatvaranja aplikacije.

Tri development računa koriste unaprijed generirane PBKDF2 zapise i ne spremaju plaintext lozinke u bazu, source dokumentaciju ni razvojne bilješke. Njihova kratka razvojna vjerodajnica iznimka je od registracijskog minimuma i ne predstavlja produkcijsku sigurnosnu preporuku.

**Razlog:** JDK 26 obvezno podržava odabrani algoritam bez nove biblioteke, a radni faktor prati aktualnu OWASP preporuku za PBKDF2-HMAC-SHA256. Salt onemogućuje jednake zapise za jednake lozinke, a verzionirani format omogućuje buduće podizanje parametara.

**Razmotrene alternative:** Nisu odabrani plaintext, brzi SHA-256, trajni session token, dodatna auth biblioteka, pepper bez sigurnog vanjskog spremišta ni spremanje razvojnih lozinki u bilješke.

**Utjecaj na implementaciju:** `PasswordHasher`, `AuthService` i `SessionContext` pripadaju service sloju. `V1BootstrapService` nakon role seeda idempotentno dodaje development korisnike, dok `JdbcUserRepository` ostaje vlasnik SQL-a.

---

## Referentni `.nc` kao regression fixture

**Datum:** 2026-08-26
**Odluka:** Dostavljeni program za pravokutnik 100 × 200 mm sprema se kao
`Dokumentacija/reference/referentni-pravokutnik-100x200.nc`. Generator se ne
mijenja radi byte-for-byte kopiranja: `G21` i `G17` ostaju profilni redci,
`G90` i `G54` zasebni su deterministički retci, `S` ostaje isključen, a
`F150`/`F500` reproduciraju se kroz feed profil.

**Razlog:** Referentni program daje stvarni format za usporedbu, ali nije dokaz
fizičke kompatibilnosti. Konfigurabilni profil zadržava mogućnost prilagodbe
nakon provjere konkretnog RichAuto A11.

**Ograničenje:** Referenca i regression test nisu fizički pokrenuti na
ZK-1325 / RichAuto A11. Cutter compensation nije dio ove odluke.

`RichAutoA11Profile.referenceProgramProfile()` sada centralizira isti
reference-profile output (`G54`, feed riječi, `M03`/`M05`, bez `S`), dok
`physicallyConfirmedCapabilities` ostaje neovisno i prazno do fizičke potvrde.

---

## V1 aplikacijska geometrijska kompenzacija alata

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

**Odluka:** V1 računa kompenzaciju u aplikaciji prije G-code generatora.
`Tool.diameter` je izvor promjera, a `radius = diameter / 2`. Nominalna
programirana kontura ostaje odvojena od putanje centra alata. Strana reza
prosljeđuje se eksplicitnim `CutSide` podatkom i ne zaključuje se iz naziva
oblika. Kompenzirana putanja prolazi fit provjeru prije generiranja G-koda.
G41/G42/G40 i RichAuto `D`/tool-table naredbe ne emitiraju se.

**Razlog:** Nije potvrđeno kako konkretni RichAuto A11 zadaje radijus alata,
offset registar ili lead-in/lead-out za controller-side kompenzaciju.
Aplikacijski offset uklanja tu neprovjerenu ovisnost i daje determinističan
softverski rezultat. Trenutačni V1 operatorski tok eksplicitno koristi
`CutSide.INSIDE`, jer lokalne konture počinju na `(0,0)` i vanjski offset bi bez
margine izašao iz granica ploče.

**Razmotrene alternative:** Controller-side `G41/G42/G40` nije odabran zbog
nepotvrđenog RichAuto `D` workflowa, aktivacije/deaktivacije i lead-in/lead-out
ponašanja. Nije odabrano ni zaključivanje strane reza iz tipa oblika.

**Utjecaj na implementaciju:** Geometry sloj dobiva mali
`ToolPathCompensationService` za linijske i kružne konture. Service sloj
prosljeđuje `Tool` i `CutSide`, a validation sloj provjerava kompenziranu
putanju. Ø6 i Ø8 ostaju tehnički testni slučajevi dok stvarni alat ne bude
fizički potvrđen. Layout Iteracije 2 mora uzeti u obzir kompenzirani omotač i
razmak među elementima.

---

## Nepoznati tehnički atributi kataloškog alata

**Datum:** 2026-08-26
**Odluka:** Katalog dopušta da `Tool.type`, `Tool.cuttingLength` i
`Tool.fluteCount` budu nepoznati (`null`) kada za stvarni alat nisu potvrđeni.
`Tool.toolNumber`, naziv i pozitivan `Tool.diameter` ostaju obvezni za unos
stvarnog alata. Ø6 mm i Ø8 mm ne seedaju se automatski.

**Razlog:** Aplikacija ne smije izmišljati tehničke podatke alata. Promjer je
potreban za budući sloj kompenzacije i zato se sprema uz odabrani `Tool`, dok
se ostali podaci mogu dopuniti kada budu poznati.

**Ograničenje:** Nije potvrđeno koji se alat ni promjer koristi na konkretnom
ZK-1325 / RichAuto A11. Cutter compensation ostaje odluka Koraka 16.

---

## Operatorski process preset za Iteraciju 1

**Datum:** 2026-08-26
**Odluka:** Glavni operatorski ekran koristi mali service-slojni
`MachiningParametersPreset` za početne machining vrijednosti. Tehničkih šest
vrijednosti nisu obvezni ručni input svakog Generate toka, nego se automatski
popunjavaju i ostaju dostupne u neobaveznom naprednom panelu. Prije fizičke
potvrde preseta vrijednosti moraju biti označene kao referentne/testne.
`MachiningParameters` ostaje snapshot koji se sprema uz `MachiningJob`.

**Razlog:** Operatoru se uklanja nepotrebno ponavljanje tehničkog unosa, bez
gubitka podataka potrebnih za validaciju, G-code generiranje i objašnjivost
spremljenog posla. Preset ne preuzima odgovornost `RichAutoA11Profilea`, koji i
dalje određuje format i opcionalno emitiranje naredbi.

**Ograničenje:** Ove vrijednosti nisu fizički potvrđene na ZK-1325 /
RichAuto A11. Nije potvrđeno ni čita li konkretna konfiguracija `F`, `S`, `G54`
ili `G40`.

---

## V1 RBAC, registracija, korisnički računi i session

**Datum:** 2026-08-26
**Status:** ODLUČENO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

**Odluka:** Sistemske role su fiksne `ADMIN`, `ENGINEER` i `OPERATOR`; admin ih dodjeljuje korisnicima, ali ih ne stvara, preimenuje ni briše. Javna registracija odmah stvara aktivan `OPERATOR` račun bez mogućnosti samostalnog izbora više role. `ADMIN` upravlja korisnicima i njihovim rolama, ima pristup svim referentnim podacima i programima te može izvršavati sve operatorske akcije. `ENGINEER` ne upravlja korisnicima ni rolama, ali upravlja materijalima, strojevima i alatima, generira i sprema programe te vidi, ponovno otvara i izvozi sve programe. `OPERATOR` generira i sprema nove programe te vidi, ponovno otvara i izvozi samo vlastite. Ponovno otvoreni job ostaje povijesni snapshot; promijenjeni podaci spremaju se kao novi job umjesto prepisivanja izvornog zapisa.

Korisnici se ne brišu fizički. Admin ih može aktivirati/deaktivirati, uređivati ime i prezime, promijeniti rolu i resetirati lozinku, ali ne može deaktivirati vlastiti prijavljeni račun niti deaktivirati ili degradirati posljednjeg aktivnog administratora. Korisnik mijenja vlastitu lozinku uz provjeru stare, dok admin drugom korisniku može resetirati lozinku bez stare. Korisničko ime ostaje jedinstveno i nepromjenjivo. Session je samo u memoriji procesa i završava logoutom ili zatvaranjem aplikacije; nema `remember me` tokena.

Bootstrap nakon stvarnog hashera dobiva po jedan development/test račun za svaku rolu, s dogovorenim lowercase korisničkim imenima. U repozitoriju, bazi i razvojnim bilješkama ne spremaju se plaintext lozinke. Potvrđen je ugrađeni JDK `PBKDF2WithHmacSHA256` s nasumičnim saltom i radnim faktorom; točan format i parametri zapisa dokumentirat će se uz implementaciju 13.2.

**Razlog:** Matrica daje jasnu razliku između upravljanja sustavom, tehničkim katalozima i vlastitim operatorskim poslovima bez uvođenja općeg permission frameworka. Neizmjenjivi job snapshoti čuvaju povijest generiranih programa, a deaktivacija umjesto brisanja čuva referencijalni integritet.

**Razmotrene alternative:** Nisu odabrani korisnički definirane role, samostalni izbor više role pri registraciji, trajni session token, hard-delete korisnika, prepisivanje spremljenog joba, plaintext lozinke ni vanjski identity provider.

**Utjecaj na implementaciju:** Korak 13 mora u service sloju centralizirati autentikaciju, session i autorizacijska pravila; persistence sloj dobiva potrebne user upite i update operacije, a JavaFX controlleri samo koordiniraju UI. Stara odluka s rolama `USER`/`ADMIN` time je zamijenjena.

---

## V1 bootstrap podaci i uklanjanje legacy USER role

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

**Odluka:** Nakon uspješne inicijalizacije H2 sheme aplikacija idempotentno osigurava role `ADMIN`, `ENGINEER` i `OPERATOR`, pet jasno označenih `TEST_MATERIAL_*` vrsta materijala te jedan stroj `ZK-1325` / `RichAuto A11` s X=1250 mm, Y=2500 mm i `NULL` za nepotvrđene tehničke granice. Bootstrap također osigurava dva jasno označena razvojna testna alata Ø6 i Ø8 mm s development brojevima 9006 i 9008. Ti zapisi nisu potvrđeni stvarni alati stroja; ostali nepoznati atributi ostaju `NULL`. `MATERIAL_SHEET`, `MACHINING_PARAMETERS`, `SHAPE` i `MACHINING_JOB` ne seedaju se u normalnom runtimeu. Stara rola `USER` uklanja se samo ako nije povezana ni s jednim korisnikom; ako jest povezana, bootstrap prekida rad jasnom SQL greškom bez prešutnog mijenjanja korisničkih uloga.

**Razlog:** Plan potvrđuje tri V1 role, ali postojeća razvojna baza naslijedila je `USER`/`ADMIN` seed. Uklanjanje samo neupotrebljene legacy role usklađuje praznu staru bazu bez gubitka korisničkih podataka i bez izmišljanja preslikavanja `USER` u novu rolu.

**Razmotrene alternative:** Nisu odabrani seed stvarnih alata s nepotvrđenim atributima, brisanje povezanih role/user zapisa ni pretpostavljeno automatsko mapiranje stare `USER` role u `OPERATOR` ili `ENGINEER`. Razvojni alati ostaju iznimka jer su eksplicitno označeni kao softverski testni podaci.

**Utjecaj na implementaciju:** `V1BootstrapService` koordinira repositoryje nakon `DatabaseInitializer`a, dok repository sloj izvodi parametrizirani JDBC upis i uvjetno brisanje role. Razvojni alati dostupni su u katalogu i Generate toku, ali ne predstavljaju fizičku potvrdu promjera ili oznake alata.

---

## Nullable reprezentacija nepoznatih granica CNC stroja

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

**Odluka:** `CncMachine.workAreaX` i `workAreaY` ostaju obvezni `double` i SQL `NOT NULL`. `workAreaZ`, `maxFeedRate`, `minSpindleSpeed` i `maxSpindleSpeed` koriste nullable `Double` i SQL `NULL` kada vrijednost nije potvrđena. Poznata opcionalna vrijednost mora biti pozitivna i konačna; job provjera primjenjuje samo granice koje postoje. Približnih 80 mm za ZK-1325 nije potvrđeno mjerenjem ili dokumentacijom i zato se ne sprema niti koristi kao safety limit.

**Razlog:** `null` jednoznačno razlikuje nepoznatu tehničku specifikaciju od stvarne brojčane vrijednosti. Nula ili proizvoljna testna vrijednost pogrešno bi izgledala kao potvrđen podatak i mogla bi neopravdano blokirati ili dopustiti operatorski workflow.

**Razmotrene alternative:** Nisu odabrani `0`, testne vrijednosti u produkcijskom zapisu ni zaseban statusni objekt za svaku granicu. Potpuno uklanjanje provjera također nije odabrano jer se potvrđena granica mora poštovati kada postoji.

**Utjecaj na implementaciju:** Nova i postojeća H2 shema dopuštaju `NULL` samo za četiri nepotvrđene granice; JDBC čuva nullable vrijednosti kroz machine, tool i machining-job mapping. Obrazac dopušta prazna opcionalna polja, dok validation sloj i dalje provjerava svaki poznati limit. Stvarni seed ZK-1325 ostaje zaseban korak.

---

## Ugovor `.nc` izvoza

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `NcExportService` u `gcode` sloju prima već generirani `GCodeProgram` i eksplicitni odredišni `Path`. Prihvaća samo naziv datoteke s `.nc` ekstenzijom bez obzira na veličinu slova, kodira cijeli sadržaj kao US-ASCII bez BOM-a i odbija znak koji se ne može tako zapisati prije stvaranja datoteke. Zapis koristi create-new semantiku i ne prepisuje postojeću datoteku. Servis ne formatira brojeve, ne bira odredište, ne stvara direktorije i ne traži USB ili druge uređaje.

**Razlog:** G-code generator i formatter već proizvode determinističan tekst s decimalnom točkom, pa export treba samo očuvati te znakove u eksplicitnom, kontroleru prikladnom charsetu. Predani `Path` čuva odabir lokacije kao buduću UI odgovornost, a odbijanje postojećeg odredišta sprječava tihi gubitak datoteke bez potvrde korisnika.

**Razmotrene alternative:** Nisu odabrani platformski default charset, lokalizirano ponovno formatiranje brojeva, prešutna zamjena ne-ASCII znakova, automatsko traženje USB uređaja, automatsko stvaranje direktorija ni silent overwrite postojeće datoteke.

**Utjecaj na implementaciju:** Budući JavaFX UI mora korisniku omogućiti izbor konkretnog `.nc` patha i obraditi invalid extension, postojeću datoteku i I/O pogreške. Ako kasnije bude potreban potvrđeni overwrite tok, mora biti eksplicitno uveden nakon korisničke potvrde; trenutačni servis namjerno ga ne izvodi.

---

## Relativni I/J i G02/G03 mapiranje

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE TESTIRANO NA STROJU

**Odluka:** `RichAutoA11Profile` eksplicitno koristi `ArcCenterMode.RELATIVE_TO_ARC_START`. Za svaki postojeći `ArcSegment` generator emitira apsolutni završni X/Y prema G90 te relativni centar `I = center.x - start.x` i `J = center.y - start.y`. `ArcDirection.CLOCKWISE` mapira se na `G02`, a `COUNTERCLOCKWISE` na `G03`. Postojeći krug ostaje zapisan kao dvije polukružnice s različitim početnim i završnim točkama. Profil odvojeno evidentira fizičke mogućnosti `ARC_MOVES_G02_G03` i `RELATIVE_ARC_CENTER_IJ`; sama konfiguracija ne znači fizičku potvrdu.

**Razlog:** `ArcSegment` već daje početak, kraj, centar i smjer pa generator treba samo mapirati geometrijske podatke u tekst. Relativni I/J ostaje stabilan nakon translacije cijele putanje, a dvije polukružnice izbjegavaju osjetljiv full-circle zapis s jednakim početkom i krajem.

**Razmotrene alternative:** Nisu odabrani apsolutni I/J, radijusni R format, linearna aproksimacija kružnice ni ponovno računanje centra ili radijusa u generatoru. Jedan full-circle segment nije uveden jer ga `ArcSegment` namjerno zabranjuje i postojeća geometrija već daje dvije jasne polukružnice.

**Utjecaj na implementaciju:** `RichAutoA11GCodeGenerator` sada prihvaća povezane i zatvorene putanje sastavljene od linija i lukova te odabire G01, G02 ili G03 prema stvarnom tipu segmenta. String-level testovi potvrđuju oba smjera i relativne I/J vrijednosti, ali ponašanje tih naredbi i konvencije nije fizički testirano na ZK-1325 / RichAuto A11.

---

## Linearni single-element G-code tok

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

**Odluka:** `RichAutoA11GCodeGenerator` u Iteraciji 1 prihvaća samo povezani i zatvoreni `ToolPath` sastavljen od `LineSegment` zapisa. Nakon headera prvo emitira rapid `G00` na safe Z dobiven profilnom konvencijom. Za svaki pozitivni kumulativni rezultat `PassDepthCalculatora` zatim eksplicitno emitira safe-state `G00` do početnog XY, kontrolirani `G01` plunge na profilom mapiranu Z dubinu, `G01` rezanje do postojećih krajnjih točaka segmenata te `G00` retract na safe Z. Isti siguran XY rapid ponavlja se u svakom prolazu, a završni retract prethodi footeru. Kada profil uključuje F, plunge postavlja `plungeRate`, a prvi rezni segment svakog prolaza postavlja `feedRate`; ostali segmenti koriste modalnu vrijednost.

**Razlog:** Eksplicitni retract prije svakog XY repositioninga čini softverski redoslijed lako provjerljivim i ne oslanja se na činjenicu da trenutačni zatvoreni oblici završavaju na početnoj točki. Generator samo slijedi postojeću putanju i ne ponavlja Shape geometriju. Provjera povezanosti i zatvorenosti sprječava da se prekid u ulaznoj putanji prešutno pretvori u rezni pomak između nepovezanih točaka.

**Razmotrene alternative:** Nije odabrano preskakanje prividno redundantnog XY rapida između prolaza, ponavljanje F na svakom segmentu ni prihvaćanje otvorenih ili nepovezanih kontura. U milestoneu 9.3 `ArcSegment` se nije aproksimirao linijama; milestone 9.4 naknadno je dodao izravno G02/G03 mapiranje.

**Utjecaj na implementaciju:** Kvadrat, pravokutnik i jednakostranični trokut iz postojećeg `ToolPathServicea` mogu se softverski pretvoriti u single-element G-code. Krug još nije podržan. Testovi koriste isključivo označene softverske vrijednosti i ne potvrđuju stvarne machining parametre, ponašanje naredbi, Z-smjer ni work zero na ZK-1325 / RichAuto A11.

---

## G-code profil, programski okvir i Z konvencija

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

**Odluka:** G-code sloj koristi nepromjenjivi `GCodeProgram`, ugovor `GCodeGenerator`, deterministični `GCodeFormatter` i konfigurabilni `RichAutoA11Profile`. Profil zasebno čuva opcije emitiranja i skup mogućnosti koje su fizički potvrđene na kontroleru; jedno stanje ne određuje drugo. Trenutačni programski okvir emitira `G21`, `G17` i `G90`, opcionalno `G54`, zaseban `S` redak i `M03`, a završava opcionalnim `M05` i obveznim `M30`. Podržani su samo milimetri i apsolutno pozicioniranje jer su postojeće projektne veličine u milimetrima, a `ToolPath` sadrži apsolutne XY koordinate. Pozitivne domenske dubine i safe Z veličine ostaju bez predznaka; odabrana `ZCoordinateConvention` zasebno ih pretvara u G-code Z koordinate uz eksplicitnu pretpostavku da je nula na površini materijala. Step-down kalkulator vraća pozitivne kumulativne dubine i uvijek završava točnom ciljnom dubinom.

**Razlog:** Odvajanje emitiranja od fizičke potvrde omogućuje softverske varijante bez tvrdnje da konkretni ZK-1325 / RichAuto A11 prihvaća `F`, `S`, `G54`, spindle naredbe ili odabrani work zero. Eksplicitna Z konvencija sprječava skriveno pretvaranje pozitivnog `cutDepth` u negativnu koordinatu. Fiksni LF izlaz, `BigDecimal` formatiranje i decimalna točka neovisna o Localeu čine `.nc` tekst ponovljivim.

**Razmotrene alternative:** Nisu odabrani hardkodirani fizički statusi, skrivena pretpostavka negativnog Z rezanja, formatiranje pomoću zadanog Localea ni inkrementalno/inch pozicioniranje bez potrebne konverzije putanje. `F` nije stavljen u header jer će njegovo značenje ovisiti o budućem plunge ili reznom pomaku. Konkretni `RichAutoA11GCodeGenerator` nije uveden prije implementacije ToolPath naredbi kako djelomičan generator ne bi vraćao program bez rezne putanje.

**Utjecaj na implementaciju:** Budući single-element generator implementirat će `GCodeGenerator`, sastaviti header i footer oko naredbi putanje te primijeniti profilnu Z konvenciju na rezultate `PassDepthCalculatora`. Fizička potvrda naredbi, Z-smjera i work zeroa mora se zasebno zabilježiti tek nakon testa na ciljnom stroju; trenutačni unit testovi dokazuju samo determinističan softverski output.

---

## V1 geometrijska reprezentacija i ToolPath

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** Geometry sloj koristi vlastite nepromjenjive 2D vrijednosne objekte `Point2`, `LineSegment`, `ArcSegment` i `ToolPath`, bez JavaFX tipova i bez G-code teksta. Lokalne konture pravokutnih oblika i trokuta počinju u `(0,0)` i imaju smjer suprotan kazaljci na satu. Lokalni koordinatni prostor kruga koristi donji lijevi origin njegovog bounding prostora: za promjer `d` centar je `(d/2,d/2)`, a kružnica se zapisuje kao dva `COUNTERCLOCKWISE` polukružna `ArcSegment` zapisa od lijeve do desne krajnje točke i natrag. `ToolPath.translated` stvara novu pomaknutu putanju za budući layout. `ToolPathBoundsCalculator` računa `Bounds2` iz stvarnih krajnjih točaka segmenata i kardinalnih ekstrema obuhvaćenih lukova. Single-shape fit prihvaća putanju samo kada je cijeli opseg unutar nenegativnog lokalnog XY prostora te ne prelazi ni dimenzije ploče ni XY radno područje stroja.

**Razlog:** Vlastiti geometrijski tipovi čuvaju geometry sloj neovisnim o JavaFX-u i RichAuto formatu. Dva polukruga imaju različite početne i završne točke pa izbjegavaju dvosmislen full-circle zapis sa `start == end`; eksplicitni centar i smjer daju budućem G-code sloju dovoljno podataka za zasebno mapiranje na I/J i G02/G03. Donji lijevi origin omogućuje da lokalni opseg kruga ostane od 0 do promjera po obje osi, što pojednostavljuje buduće bounds i layout operacije.

**Razmotrene alternative:** JavaFX `Point2D` nije odabran jer bi geometry vezao uz UI framework. Jedan full-circle segment nije odabran zbog dvosmislene jednake početne i završne točke. Mutable geometrijske klase nisu odabrane jer translacija treba sačuvati izvornu putanju. Zasebne generator klase za svaki oblik nisu uvedene jer su četiri algoritma dovoljno kratka i čitljiva kao privatne metode jednog `ToolPathService` razreda.

**Utjecaj na implementaciju:** Bounds i fit koriste već generirane segmente bez ponavljanja formula trokuta ili kruga. Budući layout sloj može premještati cijele putanje, ponovno izračunati njihov opseg i primijeniti zasebna pravila rasporeda, a gcode sloj mora konzumirati segmente bez računanja geometrije. `COUNTERCLOCKWISE` je geometrijska orijentacija, ne fizički potvrđena strategija rezanja na ZK-1325 / RichAuto A11.

---

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

## Lokalna autentikacija, uloge i vlasništvo nad poslovima u V1 — zamijenjeno odlukom 13.1

**Datum:** 2026-08-25
**Status:** ZAMIJENJENO ODLUKOM 13.1 / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

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

## Jedan ručni application composition root

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / TESTIRANO

**Odluka:** `ApplicationCompositionRoot` u paketu `hr.lukabosnjak.app` jedino je mjesto koje ručno stvara i povezuje produkcijske JDBC repositoryje, validatore, geometry/fit komponente, RichAuto generator i profil, application servicee te ovisnosti JavaFX controllera. `Main` zadržava samo JavaFX lifecycle i delegiranje. Projekt ne koristi Spring, DI framework ni modularni service loader.

**Razlog:** Eksplicitno ručno povezivanje ostaje pregledno za studentski projekt, izbjegava duplicirane factory metode i omogućuje integration testu stvaranje svježeg objektnog grafa nad izoliranim `ConnectionProviderom`.

**Razmotrene alternative:** Povezivanje svih ovisnosti izravno u `Main`, globalni singletoni, service locator i DI framework nisu odabrani. Snapshot repositoryji nisu javno izloženi composition rootom jer ostaju transakcijski detalj `JdbcMachiningJobRepositoryja`.

**Utjecaj na implementaciju:** Nove produkcijske ovisnosti moraju se dodavati u composition root tek kada pripadaju odobrenom scopeu. `LayoutService` se ne povezuje dok stvarno ne postoji nakon GATE 1.

---

## Predložak odluke

### <Kratak naziv odluke>

**Datum:** YYYY-MM-DD  
**Status:** PLANIRANO / IMPLEMENTIRANO / TESTIRANO

**Odluka:** <što je potvrđeno>  
**Razlog:** <zašto je odluka donesena>  
**Razmotrene alternative:** <stvarno razmotrene alternative ili „Nisu razmatrane”>  
**Utjecaj na implementaciju:** <koje dijelove projekta ili kasniji opis aplikacije odluka mijenja>

## FXML view i non-modularni JavaFX UI

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO

**Odluka:** Glavna JavaFX forma koristi FXML kao resource na putanji `src/main/resources/hr/lukabosnjak/ui/view/`, dok JavaFX controlleri pripadaju paketu `ui.controller`. Projekt ostaje non-modularan; FXML ne uvodi `module-info.java`. Maven dobiva `javafx-fxml` ovisnost samo zato što se FXML sada stvarno koristi.

**Razlog:** FXML jasno odvaja deklarativni prikaz od UI koordinacije u controlleru, što je održivo i jednostavno za obrazlaganje u studentskom radu. Postojeći Maven projekt već radi bez modula, a za trenutačni opseg nema potrebe uvoditi modularnu konfiguraciju.

**Razmotrene alternative:** Programatski UI ostaje moguća JavaFX alternativa, ali nije odabran za formu s više grupa unosa i dinamičkim poljima. Java klasa u `ui.view` nije uvedena jer view pripada FXML resursu.

**Utjecaj na implementaciju:** `Main` učitava FXML, a controller samo upravlja JavaFX kontrolama i lokalnim porukama. Buduće povezivanje na service sloj mora zadržati SQL, geometriju, layout i G-code izvan controllera.

---

<!-- Nove potvrđene odluke dodaju se iznad odjeljka predloška. -->

## Modalni unos referentnih podataka bez zadanih fizičkih vrijednosti

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

**Odluka:** Vrsta materijala, CNC stroj i alat dodaju se kroz tri zasebna modalna FXML obrasca otvorena iz glavne forme. Nakon spremanja novi zapis odmah se dodaje i odabire u odgovarajućem padajućem izborniku. Alat se uvijek stvara za prethodno odabrani spremljeni stroj. Fizičke vrijednosti stroja i alata nisu seedane; korisnik ih mora unijeti iz potvrđenog izvora.

**Razlog:** Modalni tok uklanja prazne padajuće izbornike bez napuštanja glavnog programa, dok izostanak pretpostavljenog seeda sprječava da nepotvrđeni max feed, RPM, Z hod ili glodalo postanu stvarne postavke aplikacije.

**Razmotrene alternative:** Razmotreni su zasebna navigacijska scena, tabovi i testni/defaultni seed. Nisu odabrani jer je potvrđen modalni tok, a stvarne fizičke vrijednosti još nisu dostavljene.

**Utjecaj na implementaciju:** `ApplicationCompositionRoot` stvara sva četiri UI controllera i zajednički `ReferenceDataManagementService`. Controlleri ne sadrže SQL; management service koristi postojeće repositoryje i novu referentnu validaciju. `Tool.type` ostaje slobodan tekst do potvrđene odluke o stvarnim kategorijama alata.

---
## Konzervativni softverski profil Generate previewa

**Datum:** 2026-08-26
**Status:** IMPLEMENTIRANO / NIJE TESTIRANO NA STROJU

**Odluka:** Generate preview koristi `RichAutoA11Profile` s milimetrima, apsolutnim pozicioniranjem, preciznošću 3 i `MATERIAL_SURFACE_ZERO_NEGATIVE_CUT`. Emitira samo neobavezne naredbe G21, G17, G90 i M30 iz postojećeg generatora; `F`, `S`, `G54`, `M03` i `M05` nisu konfigurirani za emitiranje. Skup fizički potvrđenih mogućnosti je prazan.

**Razlog:** Korisnik je odobrio izričitu softversku Z konvenciju i konzervativan preview bez pretpostavke da ciljna konfiguracija stroja prihvaća neobavezne naredbe.

**Razmotrene alternative:** Puni softverski output s F/S/G54/spindle naredbama te djelomični F/S profil nisu odabrani.

**Utjecaj na implementaciju:** Preview se može deterministički generirati i testirati na razini softvera, ali nije dokaz fizičke kompatibilnosti ili sigurnosti na ZK-1325 / RichAuto A11.

---

## Status aplikacijskog `.nc` izlaza nakon usporedbe s fizički provjerenim programima

**Datum:** 2026-08-27
**Status:** ODLUČENO / PROGRAMERSKI PREGLEDANO / APLIKACIJSKI IZLAZ NIJE FIZIČKI TESTIRAN

**Odluka:** `Dokumentacija/testni_cnc_kodovi/test01.nc` smatra se strukturno ispravnim i prikladnim kandidatom za fizički test na ZK-1325 / RichAuto A11. Kod je uspoređen s referentnim i ručno programiranim `.nc` programima, a programer je potvrdio da je izlaz dobar. Ručno programirani referentni kodovi prethodno su fizički pokrenuti na tom stroju i radili su bez problema.

**Razlog:** Aplikacijski izlaz koristi isti osnovni obrazac naredbi: milimetre, XY ravninu, apsolutne koordinate, G54, safe-Z pokrete, plunge/feed, spindle start/stop i M30. Razlike u koordinatama očekivane su zbog aplikacijske kompenzacije centra alata. Sličnost i programski pregled ne zamjenjuju pokretanje konkretnog aplikacijskog `.nc` izlaza.

**Razmotrene alternative:** Nije odabrana izmjena generatora samo radi byte-for-byte kopiranja reference. Nije odabrano dodavanje `S`, `G40`, `G41`, `G42` ili `D` naredbi bez potvrde konkretnog RichAuto workflowa.

**Utjecaj na implementaciju:** Trenutni generator se ne mijenja. Prije fizičkog testa treba evidentirati konkretan alat/promjer, work zero, orijentaciju osi, Z smjer, stvarne kontrolerske read/ignore postavke i machining vrijednosti. Nakon uspješnog fizičkog pokretanja `test01.nc` bilješka se može dopuniti statusom fizičkog testa i stvarnim rezultatima.

---

## Povijest naloga i upravljanje spremljenim programima

**Datum:** 2026-08-27
**Status:** ODLUČENO / PLANIRANO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

**Odluka:** `ADMIN` i `ENGINEER` u katalogu vide sve aktivne spremljene programe i njihova autora. `ADMIN` smije uređivati i soft-brisati sve programe. `ENGINEER` smije uređivati i soft-brisati vlastite programe te programe korisnika čija je trenutačna rola `OPERATOR`; izraz „programer” ne uvodi novu rolu. Uređivanje mijenja postojeći spremljeni nalog. Soft-delete zapisuje `deleted_at` i `deleted_by_user_id`, skriva zapis iz aplikacije i zadržava ga u bazi. Soft-obrisani program ne može se uređivati, a restore nije dio potvrđenog opsega.

`ADMIN` i `ENGINEER` mogu pregledati povijest svih naloga. Za nalog se prikazuju broj korištenih ploča, ukupna površina obrađenih elemenata, iskorištenje ploča i preostala površina samo kada ti podaci postoje u stvarno implementiranom i spremljenom modelu. Ne uvodi se zaseban statistički modul niti se nedostupne vrijednosti procjenjuju iz nepotpunih podataka.

**Razlog:** Pravila omogućuju administrativnu kontrolu i inženjersko upravljanje operatorskim programima uz sljedivost soft-deletea. Uvjetni prikaz pokazatelja sprječava da single-element model bez layouta bude pogrešno predstavljen kao izvor stvarnih proizvodnih statistika.

**Razmotrene alternative:** Nisu odabrani hard-delete, nova rola `PROGRAMMER`, uređivanje soft-obrisanih zapisa, restore UI ni zaseban statistički modul. Nije odabrano ni izračunavanje broja ploča, iskorištenja ili ostatka samo iz dimenzija jedne ploče i jednog oblika.

**Utjecaj na prethodne odluke:** Ova odluka nadjačava samo dio odluke „V1 RBAC, registracija, korisnički računi i session” prema kojem se spremljeni job nikada ne prepisuje. Ostala pravila te odluke ostaju na snazi, uključujući fiksne role `ADMIN`, `ENGINEER` i `OPERATOR`. Pravo `OPERATOR` korisnika na vlastite spremljene programe izvan upravljačkog kataloga nije ukinuto.

**Utjecaj na buduću implementaciju:** Autorizacija uređivanja i soft-deletea pripada service sloju, SQL i filtriranje soft-obrisanih zapisa persistence sloju, a JavaFX controller samo koordinira katalog. Pokazatelji iskorištenja smiju se prikazati tek kada ih budući layout i spremljeni model stvarno mogu isporučiti.

---

<!-- Nove potvrđene odluke dodaju se iznad odjeljka predloška. -->
