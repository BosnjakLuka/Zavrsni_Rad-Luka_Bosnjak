# Plan implementacije aplikacije – kontrolirani milestone promptovi za Codex

## Svrha dokumenta

Ovaj dokument je izvedbeni plan za razvoj aplikacije završnog rada **„Razvoj aplikacije za optimiziranje rada na CNC stroju“**. Plan koristi kombinaciju manjih koraka i grupiranih implementacijskih milestoneova. Jednostavne, međusobno povezane promjene mogu se odraditi u jednom Codex zadatku, dok se Plan/Ask Mode zadržava za arhitektonske odluke, višeslojne promjene, bazu podataka, geometriju, G-kod, layout, integraciju i druge rizičnije korake.

Glavni redoslijed više nije jedan linearni niz do završnog fizičkog testa. Razvoj je podijeljen u **dvije implementacijske iteracije** s obveznim prolazom kroz prvi stvarni test između njih.

## ITERACIJA 1 – dovršena single-element aplikacija prije prvog testa na stroju

Koraci **1–11** ostaju povijest već odrađene implementacije. Nakon pregleda stanja aplikacije na početku starog Koraka 12 uvedena je korektivna faza: prije prvog fizičkog testa aplikacija mora biti funkcionalno i vizualno dovršena za sve V1 funkcionalnosti koje **ne ovise o quantity/layout algoritmu**.

1. Kreiranje Java projekta
2. Struktura paketa
3. Domain/model klase
4. Enumovi
5. H2 konfiguracija
6. SQL schema
7. Repository/DAO sloj
8. Service / validacija / Geometry / ToolPath
9. RichAuto A11 G-code generator za **jedan element**
10. JavaFX UI za **jedan element**
11. Integracija prve iteracije – dosadašnje stanje / razvojni kostur
12. Korektivni audit baze, modela i bootstrap/seed podataka
13. Login, registracija, session i uloge korisnika
14. Dovršavanje JavaFX korisničkog sučelja i administracijskih/kataloških ekrana
15. Pojednostavljenje operatorskog workflowa i upravljanje machining postavkama bez obveznog ručnog unosa svih tehničkih parametara
16. Usklađivanje RichAuto A11 generatora s referentnim `.nc` programom i kompenzacijom alata
17. Release-candidate integracija Iteracije 1 – sve osim quantity/layouta mora raditi
18. Prvi kontrolirani test na ZK-1325 / RichAuto A11 za **jedan element**

### GATE 1 – obvezna kontrolna točka

**Ne implementirati korisnički unos količine, algoritam raspoređivanja, izračun kapaciteta ploče, broj potrebnih ploča ni batch G-code prije nego što je dovršena Iteracija 1 i prvi single-element test dovoljno provjeren da možemo nastaviti.**

Prije fizičkog testa očekuje se cijeli aplikacijski tok:

`Login -> UI -> odabir/unos -> validacija -> ToolPath -> kompenzacija/alati -> RichAutoA11GCodeGenerator -> preview -> Save/Reopen -> .nc export`

Fizički test zatim potvrđuje dio:

`generirani .nc -> ZK-1325 / RichAuto A11 -> jedan element`

Količina u Iteraciji 1 ostaje interno **1** i nije korisničko polje.

## ITERACIJA 2 – više jednakih elemenata i raspoređivanje

19. Quantity – aktivacija korisničkog unosa količine
20. Algoritam raspoređivanja više jednakih elemenata
21. Batch G-code za raspoređene elemente
22. Proširenje JavaFX UI-a za quantity/layout rezultate
23. Integracija druge iteracije
24. Batch test na ZK-1325
25. Završni testni izvještaj

Prije toga postoji samo **korak 0 – priprema Codexa**, jer je za ovaj način rada korisno imati trajne projektne upute, project-specific Skill i sustav razvojnih bilješki.

---

# 0. Izvor istine i potvrđene odluke

## 0.1. Potvrđeni opseg aplikacije

U trenutačnom opsegu završnog rada aplikacija treba podržavati:

- unaprijed definirane 2D oblike:
  - kvadrat
  - pravokutnik
  - krug
  - jednakostranični trokut
- unos dimenzija oblika
- unos dimenzija ploče:
  - širina
  - visina
  - debljina
- unos potrebne količine elemenata
- provjeru stane li oblik na ploču
- izračun kapaciteta jedne ploče
- izračun potrebnog broja ploča
- raspoređivanje više jednakih elemenata
- generiranje geometrijske putanje odvojeno od G-koda
- generiranje G-koda
- prikaz G-koda
- spremanje G-koda
- ponovno otvaranje spremljenog naloga/programa
- izvoz programa u `.nc` datoteku

Pseudojezik i AI definiranje vlastitih oblika **nisu dio trenutne implementacije** i ostaju budući razvoj.

## 0.2. Ciljni stroj

Potvrđeno za V1:

- stroj: **ZK-1325**
- kontroler: **RichAuto A11**
- 3 osi
- radna površina: približno 1250 × 2500 mm
- Z-hod: približno 80 mm
- prijenos programa: USB memorija
- izlaz aplikacije: `.nc` datoteka

Prije stvarnog rezanja moraju se na konkretnom kontroleru provjeriti:

- orijentacija X/Y/Z osi
- radni koordinatni sustav / WCS
- početna točka odnosno work zero
- čita li konkretna konfiguracija `F`
- čita li konkretna konfiguracija `S`
- čita li konkretna konfiguracija `G54`
- način upravljanja vretenom
- sigurna Z-visina
- stvarni parametri rezanja

Ništa od toga ne smije se označiti kao TESTIRANO prije fizičke provjere.

## 0.3. Arhitektura

Dogovorena je podjela odgovornosti:

- UI / JavaFX
- Service / poslovna logika
- Validation
- Geometry / ToolPath
- Layout / raspoređivanje
- GCodeGenerator
- Persistence / Repository / JDBC
- H2 baza

JavaFX controlleri ne smiju računati geometriju, slagati SQL niti generirati G-kod.

## 0.4. Potvrđeni entiteti

V1 model uključuje:

- `Role`
- `User` → SQL tablica `APP_USER`
- `MaterialType`
- `MaterialSheet`
- `Shape`
- `CncMachine`
- `Tool`
- `MachiningParameters`
- `MachiningJob`

`Part` se **ne uvodi** kao zaseban entitet.

`GCodeProgram` može postojati kao domenski rezultat generatora, ali za V1 nije nužna zasebna SQL tablica. Najjednostavnija V1 pohrana generiranog koda je `g_code CLOB` unutar `MACHINING_JOB`.

## 0.5. Potvrđene relacije baze

- `ROLE 1:N APP_USER`
- `CNC_MACHINE 1:N TOOL`
- `MATERIAL_TYPE 1:N MATERIAL_SHEET`
- `APP_USER 1:N MACHINING_JOB`
- `CNC_MACHINE 1:N MACHINING_JOB`
- `TOOL 1:N MACHINING_JOB`
- `MATERIAL_SHEET 1:1 MACHINING_JOB` u V1
- `MACHINING_PARAMETERS 1:1 MACHINING_JOB` u V1
- `SHAPE 1:1 MACHINING_JOB` u V1

Za stvarne 1:1 relacije treba `UNIQUE` ograničenje na odgovarajućim FK stupcima u `MACHINING_JOB`.

`TOOL.tool_number` mora biti jedinstven **unutar jednog stroja**, dakle SQL ograničenje treba biti:

`UNIQUE(cnc_machine_id, tool_number)`

## 0.6. Potvrđena odluka o redoslijedu implementacije

FR-12, FR-13, FR-14 i FR-15 ostaju dio planiranog završnog opsega, ali se **ne implementiraju prije prvog fizičkog single-element testa**.

To znači:

- prije prvog testa nema korisničkog unosa `quantity`
- prije prvog testa nema layout/nesting algoritma
- prije prvog testa nema izračuna `capacity per sheet`
- prije prvog testa nema `requiredSheets`
- prije prvog testa nema batch G-code generatora.

U prvoj iteraciji aplikacija radi s jednim elementom (`quantity = 1` kao interno stanje/testna vrijednost gdje je potrebna zbog konačnog modela baze). Tek nakon prolaska kroz GATE 1 aktivira se korisnički unos količine i razvoj raspoređivanja.

Razlog ove odluke je smanjenje tehničkog rizika: prije razvoja složenijeg layout dijela potrebno je potvrditi da osnovni tok `Shape -> ToolPath -> G-code -> .nc -> ZK-1325` radi za jedan element.

## 0.7. Potvrđena package konvencija

Base package projekta je:

`hr.lukabosnjak`

Dogovorena struktura koristi `domain.model`, `domain.enums`, `service.dto`, `persistence.repository`, `persistence.jdbc`, `ui.controller` i `ui.view` kao jasne podjele odgovornosti. `layout` postoji kao paket, ali se funkcionalno aktivira tek nakon GATE 1. FXML datoteke, ako FXML bude odabran, pripadaju `src/main/resources`, a ne Java source paketu samo zato što postoji naziv `ui.view`.

## 0.8. JDK status i aktualne odluke nakon pregleda aplikacije prije prvog testa

JDK je već odabran u stvarnom IntelliJ/Maven projektu. Ovaj plan ne izmišlja broj verzije: ako još nije zapisan u `00_odluke.md`, treba ga samo pročitati iz postojećeg projekta (`pom.xml`, IntelliJ Project SDK ili stvarni build output) i evidentirati.

Od ovog ažuriranja vrijede sljedeće nove potvrđene odluke:

- autentikacija i osnovni RBAC **ulaze u V1 scope**
- uloge su `ADMIN`, `ENGINEER` i `OPERATOR`
- aplikacija mora imati login, registraciju, logout i aktivnu korisničku sesiju prije prvog fizičkog testa
- korisnički model ponašanjem se može ugledati na raniji ASP.NET MVC projekt, ali se **ne kopira ASP.NET Identity tehnologija**; implementira se jednostavno Java/JDBC rješenje
- lozinke se ne pohranjuju kao čisti tekst
- prije prvog fizičkog testa glavni JavaFX ekran više ne smije biti samo „UI kostur“
- glavna forma ne treba prisiljavati operatora da pri svakom poslu ručno upisuje sve tehničke machining parametre ako ih aplikacija može dobiti iz potvrđenog profila/preseta i odabranog alata
- `MachiningParameters` ostaje domenski/persistence snapshot dok se ne dokaže da je neki atribut suvišan; **skrivanje polja iz UI-a nije isto što i brisanje podatka iz modela/baze**
- dostavljeni referentni program za pravokutnik 100 × 200 mm sadrži `F150` za plunge i `F500` za rezanje, stoga se ne smije zaključiti da feed vrijednosti nisu potrebne; cilj je maknuti nepotreban **ručni unos**, a ne naslijepo ukloniti tehničke podatke iz generatora
- stručna povratna informacija za referentni program je da geometrijski smjer izgleda prihvatljivo, ali nedostaje kompenzacija alata
- kao kandidati stvarnih alata spomenuti su promjeri **Ø6 mm i Ø8 mm**; to još nije dovoljno za izmišljanje `TOOL` seed zapisa bez potvrde koji alat, broj alata i ostali stvarni podaci pripadaju stroju
- kompenzacija alata mora se riješiti prije prvog stvarnog reza; RichAuto/standardna G-code terminologija koristi `G41` za lijevu i `G42` za desnu kompenzaciju, uz `G40` za poništavanje, ali konačni način rada na našem A11 mora se provjeriti na konkretnom kontroleru
- prvi fizički test je pomaknut s starog Koraka 12 na novi **Korak 18**.

### Potvrđeni seed/bootstrap podaci za V1

| Tablica | Pravilo |
|---|---|
| `ROLE` | seedati `ADMIN`, `ENGINEER`, `OPERATOR` |
| `APP_USER` | 3–5 development/test korisnika, bez plaintext lozinki |
| `MATERIAL_TYPE` | 5 testnih/stvarnih vrsta materijala |
| `CNC_MACHINE` | jedan stvarni `ZK-1325 / RichAuto A11`, bez izmišljanja nepoznatih specifikacija |
| `TOOL` | seedati tek stvarno potvrđene alate |
| `MATERIAL_SHEET` | ne seedati; nastaje za konkretan posao |
| `SHAPE` | ne seedati; nastaje iz korisničkog unosa |
| `MACHINING_PARAMETERS` | ne seedati nasumične vrijednosti |
| `MACHINING_JOB` | ne seedati u normalnom runtimeu; eventualni demo podaci samo u jasno odvojenom development/test modu |

Seeder mora biti **idempotentan**: ponovno pokretanje aplikacije ne smije duplicirati uloge, stroj, materijale ili testne korisnike.

### Još otvorene odluke koje se ne smiju izmišljati

- točna matrica prava `ADMIN` / `ENGINEER` / `OPERATOR`
- default rola samostalno registriranog korisnika
- konačan password-hashing pristup i parametri na temelju stvarnog JDK-a/dependencyja
- stvarni broj/naziv i svi podaci alata Ø6 mm / Ø8 mm
- koristi li naš konkretni A11 controller-side cutter compensation s očekivanim tool-offset registrom ili ćemo kompenzaciju računati geometrijski u aplikaciji
- točan lead-in/lead-out potreban za aktivaciju/deaktivaciju `G41/G42` na našem kontroleru
- konkretne postavke `F`, `S`, `G54`, `G40` read/ignore na fizičkom RichAuto A11
- precizan smjer osi, work zero i WCS na stroju
- stvarna dubina rezanja, step-down i sigurna Z-visina za prvi materijal/test
- minimalni razmak među komadima i rubni odmak za Iteraciju 2.

---

# Kako koristiti promptove

## A. Plan/Ask Mode nije obvezan za svaki zadatak

Plan/Ask Mode koristi se kada zadatak uključuje barem jedno od sljedećeg:

- arhitektonsku odluku ili promjenu granica između slojeva
- više povezanih paketa/slojeva
- SQL shemu, relacije, transakcije ili migracije
- geometrijski algoritam, G-kod ili layout
- integraciju većeg dijela aplikacije
- otvorenu odluku koja se ne smije pretpostaviti
- fizički *CNC* test ili statički audit programa
- refaktoriranje koje može promijeniti postojeće ponašanje.

Za male, jasno specificirane i niskorizične zadatke dopušten je **DIRECT CODE** bez zasebnog Plan turna. Primjeri su jednostavni enumovi, DTO/model boilerplate, mali formatter ili `.nc` export kada su ulazi i očekivano ponašanje već zaključani.

Kod grupiranog milestonea koristi se jedan Plan/Ask turn za cijelu povezanu cjelinu, zatim jedan Code turn. Nema potrebe raditi zaseban Plan turn za svaku POJO klasu.

### Oznake korištene u ovom dokumentu

- **PLAN → CODE** – prvo pregled plana, zatim implementacija nakon odobrenja
- **DIRECT CODE** – Codex u istom turnu kratko navede plan, implementira i testira; ne čeka dodatno odobrenje
- **ASK / AUDIT** – analiza, odluka, checklist ili audit bez izmjene koda

## B. Pravilo veličine zadatka

Cilj nije imati najmanji mogući broj promptova, nego najmanji broj **dobro ograničenih** promptova.

- grupiraj jednostavne klase koje pripadaju istoj cjelini
- ne spajaj G-kod, bazu i UI samo radi uštede jednog turna
- jedan grouped milestone mora imati jasan cilj, popis datoteka/slojeva i acceptance kriterije
- ako Codex tijekom plana otkrije da zadatak prelazi razumnu veličinu ili sadrži neriješenu odluku, smije predložiti podjelu na dva manja milestonea
- ne ponavljaj cijeli projektni opis u svakom promptu; `AGENTS.md`, Skill, `00_odluke.md` i kod trebaju biti izvor trajnog konteksta
- ako je thread postao vrlo dug i pun zastarjelog konteksta, novi milestone se može otvoriti u novom threadu jer projektni kontekst mora biti u repozitoriju, a ne samo u chatu.

## C. Universal follow-up nakon odobrenja plana

Nakon što razumiješ Codexov plan, prebaci se u Code Mode i koristi:

> Implementiraj samo prethodno odobreni plan. Ne prelazi na sljedeći milestone i ne dodaj funkcionalnosti izvan scopea. Nakon promjena pokreni najrelevantniji build/test, pokaži rezultat, navedi sve promijenjene datoteke i objasni što je napravljeno tako da kod mogu objasniti mentoru.
>
> Ako test ne prolazi, najprije utvrdi uzrok i popravi samo ono što pripada ovom milestoneu. Ne skrivaj neuspješan test prebacivanjem na drugi korak.
>
> Prije završnog odgovora ažuriraj `dokumentacija/biljeske/` prema pravilima iz `AGENTS.md` i project-specific Skilla. Bilješka mora zabilježiti što je IMPLEMENTIRANO, što je stvarno TESTIRANO, što NIJE TESTIRANO, zašto je rješenje odabrano i koje su otvorene odluke. Ažuriraj i `00_indeks.md`; `00_odluke.md` mijenjaj samo ako je donesena stvarna projektna odluka.
>
> Kandidata za isječak koda dodaj samo ako je tehnički vrijedan za završni rad. Ne izdvajaj gettere, settere i boilerplate samo zato da bilješka ima isječak.
>
> Na kraju stani.

Za **DIRECT CODE** prompt nije potreban poseban follow-up. Sam prompt mora sadržavati iste zahtjeve za testiranje i bilješke.

## C.1. Build naredbe na Windowsu

Ne pretpostavljaj da je globalna naredba `mvn` dostupna u PowerShellu. Ako projekt ima Maven Wrapper, preferiraj:

```powershell
.\mvnw.cmd test
.\mvnw.cmd javafx:run
```

Ako Wrapper ne postoji, koristi Maven koji je stvarno konfiguriran u IntelliJ/Codex okruženju. Ne mijenjaj `pom.xml` samo zato što obični PowerShell ne pronalazi globalni `mvn`.

## C.2. Reasoning effort – praktično pravilo

Ne zaključavati plan uz naziv jednog modela jer se Codex modeli mijenjaju. Praktično:

- **medium**: jednostavni domain modeli, enumovi, DTO-i, manji UI boilerplate, jednostavan export
- **high**: SQL, JDBC transakcije, geometrija, G-kod, layout, integracija i ozbiljniji debugging
- **najviši dostupni effort**: samo za posebno težak audit/debugging ili kritičnu G-code/layout odluku kada postoji stvarna korist.

## D. Dokumentacijski dnevnik za završni rad

Uz izvorni kod tijekom cijelog razvoja vodi se **projektna dokumentacija razvoja**. Cilj nije dokumentirati svaku sitnicu, nego sačuvati upravo one informacije koje će kasnije biti potrebne za poglavlja o arhitekturi, bazi podataka, implementaciji, algoritmu raspoređivanja, generiranju G-koda, korisničkom sučelju i testiranju.

U korijenu IntelliJ projekta treba postojati:

```text
dokumentacija/
└── biljeske/
    ├── 00_indeks.md
    ├── 00_predlozak_biljeske.md
    ├── 00_odluke.md
    └── <oznaka-prompta>-<kratki-naziv>.md
```

Primjeri:

```text
dokumentacija/biljeske/01-02-maven-projekt.md
dokumentacija/biljeske/03-01-domain-core.md
dokumentacija/biljeske/08-02-geometry-toolpath.md
dokumentacija/biljeske/14-02-baseline-layout.md
dokumentacija/biljeske/09-03-linearni-gcode.md
```

### Što se zapisuje nakon svakog implementacijskog milestonea

Bilješka mora sadržavati najmanje:

1. **Oznaku i naziv koraka**
2. **Datum**
3. **Status**
   - PLANIRANO
   - IMPLEMENTIRANO
   - TESTIRANO
   - NIJE TESTIRANO
   - BUDUĆI RAZVOJ
4. **Cilj promjene**
5. **Promijenjene datoteke**
6. **Što je stvarno implementirano**
7. **Zašto je odabrano upravo to rješenje**
8. **Povezanost s arhitekturom aplikacije**
9. **Važne odluke i ograničenja**
10. **Način provjere**
    - izvršena naredba/build/test
    - rezultat testa
11. **Otvorena pitanja / što još nije potvrđeno**
12. **Moguće poglavlje završnog rada**
13. **Kandidati za isječke koda**
14. **Kandidati za sliku, UML, ER dijagram ili tablicu**, ako se tijekom koraka pojavi nešto vrijedno prikaza
15. **Git commit/hash**, ako je Git već uveden i commit postoji.

### Pravila za isječke koda

Codex ne smije kopirati velike količine koda samo radi bilješki.

Kandidat za isječak koda treba biti dio koji pomaže objasniti **kako aplikacija radi** ili **zašto je arhitektura napravljena na određeni način**, primjerice:

- konfiguracija JavaFX/Maven projekta ako je relevantna za opis tehnologija
- H2/JDBC konfiguracija
- korištenje `PreparedStatement`
- transakcijsko spremanje `MachiningJob`
- važna validacija
- pretvorba `Shape` podataka u `ToolPath`
- izračun koordinata jednakostraničnog trokuta
- layout algoritam
- usporedba rotacije pravokutnika
- izračun kapaciteta i potrebnog broja ploča
- step-down logika
- RichAuto A11 profil
- formatiranje G-code brojeva
- pretvorba `ToolPath` segmenata u G-kod
- koordiniranje cijelog procesa u `ProgramGenerationService`
- dinamička JavaFX polja za oblik
- `.nc` export
- relevantna obrada pogrešaka.

U pravilu jedan kandidat treba sadržavati **otprilike 5–25 relevantnih linija**. Ako je metoda dulja, izdvojiti samo dio koji objašnjava bitnu ideju.

Bilješka za svaki kandidat treba imati oblik:

````markdown
### Kandidat za isječak koda: <kratak akademski naziv>

**Datoteka:** `src/.../ClassName.java`  
**Klasa/metoda:** `ClassName#methodName`  
**Zašto je važan:** kratko objašnjenje što ovaj kod dokazuje ili objašnjava.  
**Moguće poglavlje:** npr. 9. Implementacija / 10. Algoritam raspoređivanja

```java
// samo relevantni dio stvarnog koda
```

**Ideja opisa u radu:** 2–4 rečenice koje objašnjavaju kod bez marketinških tvrdnji.
````

Ne treba unaprijed numerirati `Isječak koda 1`, `Isječak koda 2`, itd. Konačna numeracija dodjeljuje se tek pri pisanju Word dokumenta, kada bude poznato koji će kandidati stvarno ući u rad.

Ako korak nema vrijedan isječak, bilješka treba sadržavati:

```text
Kandidat za isječak koda: nema – u ovom koraku nema koda dovoljno značajnog za završni rad.
```

### Što se NE zapisuje kao kandidat za završni rad

Ne izdvajati:

- obične gettere i settere
- import liste
- trivijalne konstruktore
- generirani boilerplate
- cijele klase bez potrebe
- ponovljene isječke koji pokazuju istu stvar
- dependency konfiguraciju koja na kraju nije korištena
- kod iz neuspjelih eksperimenata koji nije dio konačne implementacije, osim ako je problem i njegovo rješenje važno za poglavlje o ograničenjima ili razvoju
- tajne, lozinke, tokene, lokalne putanje koje sadrže osobne podatke ili druge osjetljive podatke.

### `00_indeks.md`

`00_indeks.md` služi kao kronološki katalog i treba se ažurirati nakon svakog koraka.

Predloženi stupci:

```text
Prompt | Tema | Status | Glavne datoteke | Testirano | Kandidat za završni rad | Bilješka
```

Time će se na kraju razvoja moći brzo pronaći:

- gdje je određena funkcionalnost implementirana
- kada je uvedena
- kako je testirana
- koji je kod dobar kandidat za prikaz u završnom radu.

### `00_odluke.md`

U ovu datoteku ulaze **samo važne projektne odluke** koje mogu utjecati na tekst završnog rada, primjerice:

- odabrani build alat
- stvarna JDK verzija
- FXML ili programatski JavaFX
- konačni model `ToolType`
- autentikacija/RBAC scope
- način reprezentacije kružnice u `ToolPath`
- način layouta
- način pohrane G-koda
- potvrđene RichAuto A11 postavke
- promjene početnog modela baze.

Svaka odluka treba sadržavati:

```text
Datum
Odluka
Razlog
Alternative koje su razmotrene
Utjecaj na implementaciju
Status: PLANIRANO / IMPLEMENTIRANO / TESTIRANO
```

### Zašto ovo radimo

Primjer završnog rada `Primjer_ZavrsniRadMatijaKisFinal` koristi poseban **Popis isječaka koda**, a zatim u poglavljima o tehnologijama i implementaciji prikazuje odabrane isječke te tekstom objašnjava njihovu svrhu. Naš dnevnik služi kao priprema za isti princip, ali će se u konačni rad prenijeti samo isječci iz **stvarno završene implementacije**.

Bilješke nisu konačni tekst završnog rada. One su tehnički trag razvoja i izvor iz kojeg se kasnije piše akademski oblikovan tekst.

---

# KORAK 0 – Priprema Codexa

## Prompt 0.1 – `AGENTS.md` i trajni projektni kontekst

> PLAN MODE. Pregledaj ovaj repozitorij i pripremi prijedlog za `AGENTS.md` koji će biti trajni izvor projektnih pravila za Codex.
>
> U njega moraju ući samo potvrđene odluke:
> - Java + JavaFX
> - H2 kao baza
> - slojevi UI, service, validation, geometry/toolpath, layout, gcode i persistence
> - ciljni stroj ZK-1325 / RichAuto A11
> - izlaz `.nc`
> - milimetri kao geometrijska jedinica
> - 4 podržana oblika
> - `Part` se ne uvodi
> - pseudojezik i AI integracija su budući razvoj
> - controller ne sadrži SQL, geometriju ni G-code logiku
> - ne tvrditi matematičku optimalnost layout algoritma
> - ne tvrditi RichAuto kompatibilnost kao TESTIRANU prije fizičkog testa
> - ne izmišljati JDK verziju, ToolType vrijednosti, machining parametre ili postavke fizičkog kontrolera.
>
> Dodaj pravilo da se svaki veći zadatak prvo analizira u Plan/Ask modu i da se implementira samo jedna mala cjelina.
>
> U `AGENTS.md` obavezno dodaj i trajno pravilo dokumentiranja:
> - projektna dokumentacija razvoja nalazi se u `dokumentacija/biljeske/`
> - nakon svakog zadatka koji promijeni kod, konfiguraciju, SQL, testove ili UI mora se stvoriti ili ažurirati bilješka za taj prompt
> - nakon važne arhitektonske/tehnološke odluke ažurira se `00_odluke.md`
> - nakon svakog takvog koraka ažurira se `00_indeks.md`
> - bilješka mora razlikovati IMPLEMENTIRANO od TESTIRANO
> - bilješka mora izdvojiti najviše nekoliko smislenih kandidata za isječak koda i ne smije forsirati isječak ako je kod trivijalan
> - isječak mora biti iz stvarnog trenutnog koda i mora sadržavati putanju datoteke, klasu/metodu, razlog važnosti i moguće poglavlje završnog rada
> - ne zapisivati tajne, lozinke, tokene ni osobne podatke.
>
> Nemoj još stvarati aplikacijske klase.

## Prompt 0.2 – Project-specific Skill

> PLAN MODE. Koristeći ugrađeni skill-creator ako je dostupan u ovom Codex okruženju, predloži project-specific Skill za ovaj repozitorij, npr. `cnc-final-project-guardrails`.
>
> Skill treba pomagati u ponavljajućim zadacima:
> - provjera arhitektonskih granica
> - provjera da se ne dodaje nedogovoreni scope
> - provjera da se kod može objasniti studentu
> - provjera da JavaFX controller ostaje tanak
> - provjera da JDBC koristi PreparedStatement
> - provjera da geometrija ostaje odvojena od G-koda
> - provjera da layout algoritam ne naziva rezultat matematički optimalnim
> - provjera da RichAuto profil razlikuje IMPLEMENTIRANO od TESTIRANO
> - nakon svake izmjene tražiti build/test i sažetak promijenjenih datoteka
> - automatska provjera je li nakon implementacije ažuriran `dokumentacija/biljeske/`
> - pomoć pri izboru samo smislenih kandidata za isječke koda koji će se kasnije moći objasniti u završnom radu
> - zabrana zapisivanja trivijalnog boilerplatea kao „važnog isječka“
> - ažuriranje `00_indeks.md` i, kada je donesena važna odluka, `00_odluke.md`.
>
> Nemoj mijenjati aplikacijski kod. Prvo mi pokaži sadržaj i strukturu Skilla te objasni gdje će biti spremljen i kada će se automatski koristiti.

## Prompt 0.3 – Kreiranje sustava `dokumentacija/biljeske`

> PLAN MODE. Prije početka aplikacijskog koda pripremi sustav radnih bilješki za završni rad.
>
> U projektu želim:
>
> ```text
> dokumentacija/
> └── biljeske/
>     ├── 00_indeks.md
>     ├── 00_predlozak_biljeske.md
>     └── 00_odluke.md
> ```
>
> `00_predlozak_biljeske.md` mora sadržavati:
> - oznaku/naziv prompta
> - datum
> - status PLANIRANO / IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO / BUDUĆI RAZVOJ
> - cilj
> - popis promijenjenih datoteka
> - opis stvarne implementacije
> - razlog odabranog rješenja
> - arhitektonsku povezanost
> - važne odluke i ograničenja
> - naredbe za build/test i stvarni rezultat
> - otvorena pitanja
> - moguće poglavlje završnog rada
> - kandidata za isječak koda s putanjom, klasom/metodom, kratkim stvarnim kodom i idejom opisa
> - kandidata za sliku/dijagram/tablicu ako postoji
> - commit/hash ako postoji.
>
> `00_indeks.md` neka bude kratka tablica:
> `Prompt | Tema | Status | Glavne datoteke | Testirano | Kandidat za završni rad | Bilješka`.
>
> `00_odluke.md` služi samo za odluke koje mijenjaju način na koji ćemo kasnije opisivati aplikaciju.
>
> Nakon kreiranja dodaj provjeru da se ova pravila nalaze i u `AGENTS.md` te project-specific Skillu. Ako postoje, nemoj ih duplicirati bez potrebe.
>
> Nemoj još stvarati aplikacijske klase.

---

# 1. Kreiranje Java projekta

## Prompt 1.1 – Provjera stvarnog razvojnog okruženja

**Status za trenutačni projekt: povijesni korak – ne ponavljati ako je JDK/build alat već potvrđen i zapisan.**

> PLAN MODE. Ne mijenjaj projekt. U terminalu provjeri stvarno razvojno okruženje:
> - `java -version`
> - `javac -version`
> - postoji li Maven i koja je verzija
> - postoji li Gradle i koja je verzija
> - je li direktorij već Git repozitorij
> - postoji li postojeći `pom.xml`, `build.gradle` ili `build.gradle.kts`.
>
> Rezultate mi objasni i preporuči build alat za JavaFX + H2 + JUnit projekt. Prednost daj jednostavnosti za studentski projekt. Ne pretpostavljaj JDK verziju.

## Prompt 1.2 – Kreiranje minimalnog Maven projekta

> PLAN MODE. Na temelju stvarno pronađenog JDK-a predloži minimalni Maven projekt za ovu desktop Java aplikaciju.
>
> Za sada želim samo:
> - ispravan `pom.xml`
> - Java source/test strukturu
> - JavaFX dependency potrebnu za minimalni prozor
> - JUnit za testove
> - compiler konfiguraciju usklađenu sa stvarnim JDK-om
> - bez H2 dependencyja u ovom koraku
> - bez FXML dependencyja dok FXML stvarno ne odlučimo koristiti
> - bez dodatnih frameworka.
>
> Objasni svaku dependency i plugin stavku prije implementacije.

## Prompt 1.3 – Minimalni JavaFX smoke test

> PLAN MODE. Dodaj samo minimalnu aplikacijsku ulaznu točku koja može otvoriti prazan JavaFX `Stage`.
>
> Cilj ovog koraka nije graditi UI, nego dokazati:
> - Maven compile radi
> - JavaFX se pokreće
> - projekt koristi stvarni JDK iz prethodnog koraka.
>
> Nemoj još dodavati controller, FXML, bazu, servis, model ni G-code.

---

# 2. Struktura paketa

Dogovorena package konvencija za projekt je:

```text
hr.lukabosnjak
├── app
├── config
├── domain
│   ├── entities
│   └── enums
├── validation
├── geometry
├── layout
├── gcode
├── service
│   └── dto
├── persistence
│   ├── repository
│   └── jdbc
└── ui
    ├── controller
    └── view
```

Odgovornosti:

- `app` – ulazna točka i composition root / ručno povezivanje ovisnosti
- `config` – konfiguracija baze i ostala infrastrukturna konfiguracija
- `domain.model` – poslovni/domenski modeli bez JavaFX-a i JDBC-a
- `domain.enums` – potvrđene domenske enum vrijednosti
- `validation` – poslovna validacija ulaza i modela
- `geometry` – geometrija, `ToolPath` i segmenti; bez G-code stringova
- `layout` – raspoređivanje više elemenata; funkcionalno se koristi tek nakon GATE 1
- `gcode` – RichAuto profil, formatter, generator i `.nc` export
- `service` – koordinacija use-caseova između UI-a i poslovnih dijelova
- `service.dto` – request/result modeli samo kada stvarno pojednostavljuju granicu UI → service
- `persistence.repository` – repository ugovori
- `persistence.jdbc` – JDBC/H2 implementacije repositoryja
- `ui.controller` – tanki JavaFX controlleri
- `ui.view` – Java view klase samo ako ih stvarno bude; ako se odabere FXML, FXML datoteke idu u odgovarajući path unutar `src/main/resources`.

`util`, `io`, dodatni `common` paketi i slični catch-all paketi ne uvode se unaprijed. Dodaju se samo ako se tijekom implementacije pojavi jasna odgovornost koja ne pripada postojećim paketima.

## Prompt 2.1 – Audit i zaključavanje package strukture

**Način: ASK / AUDIT**

> Pregledaj trenutno stanje repozitorija i usporedi ga s potvrđenom package strukturom `hr.lukabosnjak` iz plana.
>
> Ne mijenjaj aplikacijsku logiku. Prikaži samo:
> - postojeće pakete
> - odstupanja od dogovorene strukture
> - koje prazne pakete nema smisla stvarati unaprijed
> - postoje li klase koje su već smještene u pogrešan sloj.
>
> Posebno potvrdi da `domain` ne ovisi o JavaFX-u/JDBC-u i da `gcode` neće spremati podatke u bazu.

## Prompt 2.2 – Kreiranje / korekcija package kostura

**Način: DIRECT CODE**

> Uskladi samo package kostur s prethodno potvrđenom strukturom `hr.lukabosnjak`.
>
> Ne stvaraj beskorisne placeholder klase samo radi praznih direktorija. Ne implementiraj domenske klase. Ako je neka postojeća klasa već dio smoke testa, ne premještaj je bez potrebe samo radi estetike.
>
> Nakon promjene pokreni najrelevantniji compile/build i ažuriraj razvojnu bilješku.

---

# 3. Domain / model klase

Ovaj korak je namjerno grupiran. Jednostavne POJO/model klase ne trebaju zaseban Plan turn za svaku klasu.

## Prompt 3.1 – Core domain milestone

**Način: PLAN → CODE**

> Pregledaj postojeći projekt i u jednom milestoneu isplaniraj i implementiraj osnovne domenske modele u `hr.lukabosnjak.domain.model`:
>
> **`MaterialType`**
> - `Long materialTypeId`
> - `String name`
> - `String description` – nullable
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
> - `LocalDateTime deletedAt` – nullable
>
> **`MaterialSheet`**
> - `Long materialSheetId`
> - veza prema `MaterialType`
> - `double width`
> - `double height`
> - `double thickness`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> **`CncMachine`**
> - `Long cncMachineId`
> - `String name`
> - `String manufacturer` – nullable
> - `String model` – nullable
> - `String controller`
> - `double workAreaX`
> - `double workAreaY`
> - `double workAreaZ`
> - `double maxFeedRate`
> - `double minSpindleSpeed`
> - `double maxSpindleSpeed`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> **`MachiningParameters`**
> - `Long machiningParametersId`
> - `double spindleSpeed`
> - `double feedRate`
> - `double plungeRate`
> - `double cutDepth`
> - `double stepDown`
> - `double safeZ`
>
> **`Role`**
> - `Long roleId`
> - `String name`
> - `String description` – nullable
>
> **`User`**
> - `Long userId`
> - veza prema `Role`
> - `String username`
> - `String passwordHash`
> - `String firstName`
> - `String lastName`
> - `boolean active`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> Pravila:
> - bez JPA/Hibernate anotacija
> - bez JavaFX i JDBC ovisnosti u domain sloju
> - koristi jednostavan pristup pogodan ručnom JDBC mapiranju
> - ne hardkodiraj ZK-1325 vrijednosti u `CncMachine`
> - ne implementiraj login, hashing, session ili RBAC
> - `cutDepth` i `MaterialSheet.thickness` nisu isti podatak
> - potpuna validacija ne pripada setterima; dolazi u validation sloju
> - ne piši besmislene getter/setter unit testove.
>
> U Plan dijelu objasni samo zajedničke dizajnerske odluke za svih šest klasa i navedi datoteke koje će nastati. Nemoj raditi šest zasebnih mini-planova.

---

# 4. Enumovi i dovršavanje modela

## Prompt 4.1 – Shape enumovi + `Shape`

**Način: DIRECT CODE**

> Pregledaj trenutni domain model i implementiraj potvrđene geometrijske enumove u `hr.lukabosnjak.domain.enums` te `Shape` u `hr.lukabosnjak.domain.model`.
>
> `ShapeType`:
> - `SQUARE`
> - `RECTANGLE`
> - `CIRCLE`
> - `TRIANGLE`
>
> `ShapeSubtype` u V1 treba sadržavati samo stvarno potvrđeni koncept `EQUILATERAL`. Ne izmišljaj druge podtipove.
>
> `Shape`:
> - `Long shapeId`
> - `ShapeType shapeType`
> - `ShapeSubtype shapeSubtype` – nullable kada nema smisla
> - `Double dimensionA`
> - `Double dimensionB` – nullable
> - `Double dimensionC` – nullable
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
> - `LocalDateTime deletedAt` – nullable
>
> Semantika V1:
> - SQUARE: A = stranica
> - RECTANGLE: A = width, B = height
> - CIRCLE: A = promjer
> - TRIANGLE + EQUILATERAL: A = stranica.
>
> Ne kreiraj zasebne persistence entitete `Square`, `Rectangle`, `Circle` i `Triangle`. Ne generiraj geometriju u `Shape` klasi. UI kasnije mora koristiti semantičke nazive polja, a ne `dimensionA/B/C`.
>
> U istom turnu kratko navedi što ćeš promijeniti, implementiraj, pokreni build/test i ažuriraj bilješku.

## Prompt 4.2 – `ToolType` decision gate + `Tool`

**Način: PLAN → CODE**

> Prije implementacije `Tool` provjeri `00_odluke.md`, postojeći kod i dokumentaciju projekta postoji li potvrđen popis `ToolType` vrijednosti.
>
> Ako nije potvrđen:
> - ne izmišljaj vrijednosti
> - objasni koje informacije trebamo od stvarnog ZK-1325 alata
> - predloži najmanje složenu privremenu reprezentaciju, npr. `String type`, ali je ne implementiraj kao konačnu odluku dok je ne odobrim.
>
> Nakon odluke implementiraj `Tool`:
> - `Long toolId`
> - veza prema `CncMachine`
> - `int toolNumber`
> - `String name`
> - `type` prema potvrđenoj odluci
> - `double diameter`
> - `double cuttingLength`
> - `int fluteCount`
> - `boolean active`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`.
>
> Ne hardkodiraj stvarne alate koje još nismo izmjerili/potvrdili.

## Prompt 4.3 – `MachiningJob` agregat

**Način: PLAN → CODE**

> Implementiraj `MachiningJob` tek nakon što postoje ostali osnovni modeli.
>
> Potvrđeni podaci/veze:
> - `Long machiningJobId`
> - `User createdBy`
> - `CncMachine cncMachine`
> - `Tool tool`
> - `MaterialSheet materialSheet`
> - `MachiningParameters machiningParameters`
> - `Shape shape`
> - `String name`
> - `int quantity`
> - `String gCode`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`.
>
> `quantity` ostaje dio konačnog modela, ali u Iteraciji 1 workflow koristi samo vrijednost `1`. Korisnički unos quantity ne uvodi se prije GATE 1.
>
> Ne implementiraj SQL ni generiranje G-koda u ovoj klasi. U planu posebno objasni zašto `MachiningJob` povezuje snapshot podatke jednog posla i koje reference očekujemo da već postoje u bazi.

---

# 5. H2 konfiguracija

## Prompt 5.1 – H2 dependency + `DatabaseConfig` + smoke test

**Način: PLAN → CODE**

> U jednom milestoneu uvedi minimalnu H2/JDBC infrastrukturu.
>
> Napravi:
> - H2 Maven dependency koristeći aktualnu stabilnu verziju provjerenu iz službenog H2 izvora
> - `DatabaseConfig` ili ekvivalent u `hr.lukabosnjak.config`
> - centralno mjesto za JDBC URL, user i password
> - metodu/factory za dobivanje novog `Connection`
> - razvojni smoke test koji otvara i zatvara vezu
> - kratku uputu za pokretanje H2 Consolea kao development alata.
>
> Pravila:
> - embedded H2 + JDBC, bez ORM-a
> - bez globalno otvorenog `Connection`
> - kod koji dobije connection kasnije koristi try-with-resources
> - file-based baza za aplikaciju, in-memory H2 za integration testove
> - relativni path mora biti objašnjen jer ovisi o working directoryju
> - Console nije runtime poslovna komponenta
> - ne kreiraj još tablice i ne dodaj SQL u JavaFX controller.
>
> Za build/test prvo provjeri postoji li Maven Wrapper. Ako postoji, na Windowsu koristi `./mvnw.cmd`/`.\\mvnw.cmd`; ne pretpostavljaj da globalni `mvn` postoji.

---

# 6. SQL schema i inicijalizacija baze

## Prompt 6.1 – Pre-DDL audit + User/Role runtime gate

**Način: ASK / AUDIT**

> Ne piši SQL. Usporedi trenutne Java modele, ER/relacijski model i funkcionalne zahtjeve.
>
> Obavezno provjeri:
> - tablica `APP_USER`, ne `USER`
> - `safe_z` postoji samo u `MACHINING_PARAMETERS`
> - `MACHINING_JOB` ima `shape_id`
> - `MACHINING_JOB` ima `g_code CLOB` u Iteraciji 1
> - `UNIQUE(cnc_machine_id, tool_number)`
> - `MATERIAL_SHEET`, `MACHINING_PARAMETERS` i `SHAPE` su snapshot 1:1 prema jobu u V1
> - identity strategiju BIGINT PK-ova
> - Tool pripada CncMachineu
> - job sadrži i machine i tool; poslovna logika provjerava usklađenost.
>
> Posebno riješi postojeći gap: baza sadrži `ROLE`, `APP_USER` i `created_by_user_id`, ali login/RBAC nije potvrđen funkcionalnim zahtjevima. Prikaži minimalne opcije:
> 1. `ROLE`/`APP_USER` ostaju u V1 bez login UI-a, uz jasno definiran bootstrap/seed lokalnog korisnika koji omogućuje stvarno spremanje `MachiningJob` zapisa
> 2. autentikacija/RBAC službeno se dodaje u scope kao novi funkcionalni zahtjev.
>
> Usporedi utjecaj na kod i završni rad. Zaustavi se prije DDL-a dok odluka nije unesena u `00_odluke.md`.

## Prompt 6.2 – Kompletni `schema.sql` + schema integration test

**Način: PLAN → CODE**

> Nakon zaključene odluke iz 6.1 kreiraj kompletni V1 `schema.sql` u jednom milestoneu za:
> - ROLE
> - APP_USER
> - MATERIAL_TYPE
> - CNC_MACHINE
> - TOOL
> - MATERIAL_SHEET
> - MACHINING_PARAMETERS
> - SHAPE
> - MACHINING_JOB.
>
> Zahtjevi:
> - H2 sintaksa usklađena sa stvarnom dependency verzijom
> - BIGINT identity PK-ovi
> - NOT NULL i UNIQUE samo gdje su opravdani
> - svi potvrđeni FK-ovi
> - `UNIQUE(cnc_machine_id, tool_number)`
> - enum vrijednosti kao `VARCHAR`
> - `MACHINING_JOB.g_code CLOB`
> - UNIQUE na `material_sheet_id`, `machining_parameters_id`, `shape_id` za V1 snapshot 1:1 veze
> - `quantity INTEGER NOT NULL` + jednostavan `CHECK (quantity > 0)`
> - nemoj duplicirati svu poslovnu validaciju u SQL.
>
> Ako odluka iz 6.1 zahtijeva bootstrap user/role, dodaj samo minimalni seed mehanizam koji je odobren; ne izmišljaj autentikaciju.
>
> Dodaj integration test koji pokrene schema na čistoj in-memory H2 bazi i potvrdi da se sve tablice kreiraju.

## Prompt 6.3 – `DatabaseInitializer` / first-run schema initialization

**Način: PLAN → CODE**

> Aplikacija mora moći otvoriti praznu file-based H2 bazu na novom računalu bez ručnog kreiranja tablica u Consoleu.
>
> Predloži i implementiraj najmanje složen, eksplicitan način da se `schema.sql` izvrši pri inicijalizaciji aplikacije, npr. mali `DatabaseInitializer` u config/persistence infrastrukturi.
>
> Ne skrivaj cijelu inicijalizaciju u teško objašnjivom JDBC URL magic stringu ako jednostavan Java initializer daje čitljivije rješenje.
>
> Dodaj test koji otvara novu privremenu file-based H2 bazu, pokreće initializer i potvrđuje da očekivane tablice postoje. Initializer ne smije brisati postojeće podatke pri običnom pokretanju aplikacije.

---

# 7. Repository / DAO sloj

Za cijeli sloj vrijedi:

- JDBC
- `PreparedStatement`
- try-with-resources
- SQL ostaje u persistence sloju
- controller nikada ne dobiva `Connection`
- nema generičkog mega-`CrudRepository` samo radi patterna
- testovi koriste H2 i stvarnu schema definiciju.

## Prompt 7.1 – Simple repositories milestone

**Način: PLAN → CODE**

> U jednom milestoneu definiraj repository konvenciju i implementiraj jednostavne repositoryje koji ne spremaju cijeli `MachiningJob` agregat.
>
> Struktura:
> - interface u `hr.lukabosnjak.persistence.repository`
> - JDBC implementacija u `hr.lukabosnjak.persistence.jdbc`
> - naming `XRepository` + `JdbcXRepository`.
>
> Obuhvati:
> - `MaterialType`
> - `CncMachine`
> - `Tool`
> - `MaterialSheet`
> - `MachiningParameters`
> - `Shape`
> - `Role`/`User` samo u minimalnom opsegu koji stvarno zahtijeva odluka iz 6.1.
>
> Nemoj svakom repositoryju automatski dodati puni CRUD. Dodaj samo `save`, `findById`, `findAll` ili specifične upite koji su potrebni stvarnom workflowu. Za `Tool` trebamo barem `findAllByMachineId` i po potrebi `findByMachineIdAndToolNumber`.
>
> Dodaj integration testove koji pokrivaju stvarno mapiranje `ResultSet -> domain`, round-trip spremanje/čitanje i DB constraint da isti `tool_number` nije dopušten dvaput na istom stroju, ali jest na drugom stroju.

## Prompt 7.2 – `MachiningJobRepository` + transakcijski round-trip

**Način: PLAN → CODE**

> Implementiraj `MachiningJobRepository` i JDBC implementaciju.
>
> Spremanje jednog joba mora biti transakcijsko za snapshot podatke koje job posjeduje:
> - `Shape`
> - `MaterialSheet`
> - `MachiningParameters`
> - `MachiningJob` s `g_code`.
>
> Reference poput `CncMachine`, `Tool`, `MaterialType` i `User` moraju već postojati prema stvarnom odabranom workflowu; nemoj ih tiho duplicirati pri svakom spremanju joba.
>
> Prije koda objasni:
> - gdje transakcija počinje i završava
> - što se rollbacka ako insert ne uspije
> - kako se ponovno učitava cijeli job bez SQL-a u service/controller sloju.
>
> Minimalno:
> - `save`
> - `findById`
> - `findAll`
> - `update` samo ako quick-access workflow stvarno zahtijeva izmjenu postojećeg zapisa.
>
> Dodaj integration test koji potvrđuje rollback i fresh-context round-trip za single-element job s `quantity = 1`.

---

# 8. Validation + Geometry / ToolPath + single-shape fit

## Prompt 8.1 – Validation milestone

**Način: PLAN → CODE**

> Dizajniraj i implementiraj minimalan validation pristup bez frameworka.
>
> Validiraj:
> - `Shape`
> - `MaterialSheet`
> - `MachiningParameters`
> - `MachiningJob.quantity`
> - `Tool` pripada odabranom `CncMachine`
> - tool je active
> - `feedRate <= machine.maxFeedRate`
> - `spindleSpeed` unutar machine min/max raspona.
>
> Pravila shapea:
> - Square: A > 0
> - Rectangle: A > 0 i B > 0
> - Circle: A(promjer) > 0
> - Triangle/EQUILATERAL: A > 0.
>
> Material: width, height, thickness > 0.
>
> Machining parametri: spindleSpeed, feedRate, plungeRate, cutDepth, stepDown i safeZ > 0 kao pozitivne domenske veličine. Nemoj iz toga zaključiti fizički znak Z koordinate na stroju.
>
> Poruke trebaju biti konkretne, npr. „Duljina stranice mora biti veća od 0 mm.“
>
> Dodaj unit testove za valjane, granične i nevaljane vrijednosti. U Iteraciji 1 quantity validator postoji, ali workflow mu predaje samo `1`.

## Prompt 8.2 – Geometry + ToolPath milestone za sva 4 oblika

**Način: PLAN → CODE**

> Implementiraj controller-agnostic geometry sloj u `hr.lukabosnjak.geometry`.
>
> Potrebe:
> - vlastita 2D točka, npr. `Point2`
> - `ToolPath`
> - `PathSegment`
> - `LineSegment`
> - `ArcSegment` ili druga jasna geometrijska reprezentacija kružnog luka
> - translacija ToolPatha za budući layout
> - `ToolPathService`
> - shape-specific generatori samo ako stvarno poboljšavaju čitljivost.
>
> Ne koristiti JavaFX `Point2D`. Geometrija ne proizvodi `G00/G01/G02/G03` stringove.
>
> Lokalni shape koordinatni sustav u V1 polazi od `(0,0)`.
>
> Square/Rectangle: zatvorena linearna putanja.
>
> Equilateral Triangle:
> - A(0,0)
> - B(a,0)
> - C(a/2, sqrt(3)/2 * a)
> - povratak na A.
>
> Circle: geometrijski model mora ostati neovisan o RichAuto output formatu. Ako je za pouzdan prikaz kružnice bolje koristiti dva luka/polukruga umjesto jednog full-circle segmenta, objasni odluku.
>
> Dodaj numeričke unit testove s tolerancijom za sva 4 V1 oblika.

## Prompt 8.3 – Bounds + single-shape fit

**Način: DIRECT CODE**

> Implementiraj malu, odvojenu provjeru granica geometrijske putanje za Iteraciju 1.
>
> Cilj:
> - iz `ToolPath` izračunati XY bounds
> - provjeriti stane li jedan oblik u `MaterialSheet`
> - provjeriti da dimenzije/planirani XY opseg ne prelaze radno područje odabranog stroja
> - vratiti jasnu poslovnu grešku ako shape ne stane.
>
> Ne dupliciraj formule trokuta/kruga u validatoru ako ih već možemo dobiti iz stvarnog ToolPatha/bounds kalkulatora.
>
> Ne uvodi margin/spacing ni layout logiku; to dolazi tek u Iteraciji 2.

---

# 9. RichAuto A11 G-code generator – ITERACIJA 1

RichAuto dokumentacija potvrđuje relevantne G/M naredbe i postavke čitanja `F`, `S` i `G54`, ali konkretni ZK-1325 mora se fizički provjeriti. Generator zato mora biti konfigurabilan i testovi teksta nisu dokaz kompatibilnosti sa strojem.

## Prompt 9.1 – G-code ugovori + RichAuto profil + formatter

**Način: PLAN → CODE**

> U jednom milestoneu implementiraj:
> - `GCodeProgram`
> - `GCodeGenerator` interface
> - `RichAutoA11Profile`
> - `GCodeFormatter`.
>
> Profil treba eksplicitno razlikovati što generator emitira od onoga što je fizički potvrđeno na kontroleru. Potrebne su konfigurabilne odluke poput:
> - emit feed rate (`F`)
> - emit spindle speed (`S`)
> - emit `G54`
> - emit spindle commands
> - units
> - absolute positioning
> - numeric precision
> - eksplicitna konvencija pretvaranja pozitivnih domenskih dubina/safeZ vrijednosti u Z koordinate G-koda.
>
> Ne hardkodiraj tvrdnju da naš A11 čita `F`, `S` ili `G54`. Z-smjer/work zero također nije TESTIRAN prije fizičke provjere.
>
> Formatter:
> - decimalna točka neovisna o hrvatskom Localeu
> - bez scientific notation
> - determinističan output
> - konfigurabilan broj decimala.

## Prompt 9.2 – Header/footer + pass-depth logika

**Način: PLAN → CODE**

> Prvo pokaži predloženi programski redoslijed i značenje svake naredbe koju namjeravaš emitirati.
>
> Kandidati koje dokumentacija podržava uključuju `G21`, `G17`, `G90`, `G54` prema profilu, `M03`, `M05`, `M30`, ali ne emitiraj naredbu samo zato što postoji u manualu.
>
> U istom milestoneu implementiraj čistu logiku izračuna step-down prolaza iz pozitivnih veličina `cutDepth` i `stepDown` tako da završni prolaz dosegne točnu ciljnu dubinu i ne napravi dodatni prolaz.
>
> Znak/koordinata fizičkog Z pomaka mora dolaziti iz eksplicitne profile/work-coordinate konvencije, ne iz skrivene pretpostavke u `MachiningParameters`.
>
> Dodaj unit testove header/footer varijanti i pass-depth izračuna.

## Prompt 9.3 – Linearni ToolPath → single-element G-code

**Način: PLAN → CODE**

> Implementiraj pretvaranje line-segment ToolPatha u G-code za samo jedan oblik.
>
> Tok mora osigurati:
> - siguran Z položaj prije XY repositioninga prema definiranoj profile konvenciji
> - rapid do početnog XY
> - kontrolirani plunge
> - G01 rezanje po već postojećem ToolPathu
> - step-down prolaze
> - retract na safe Z između prolaza / na kraju.
>
> Generator ne računa geometriju shapea i ne provjerava SQL/persistence.
>
> Dodaj string-level unit testove s testnim profile vrijednostima koje su jasno označene kao softverske testne vrijednosti, ne stvarni machining parametri ZK-1325.

## Prompt 9.4 – Arc output za Circle

**Način: PLAN → CODE**

> Implementiraj RichAuto G02/G03 output iz postojeće `ArcSegment` geometrije.
>
> Ne računaj krug u GCodeGeneratoru.
>
> Prije koda potvrdi način reprezentacije I/J centra prema našoj geometriji i profilnoj konvenciji. Ako full-circle start=end može biti osjetljiv, koristi dvije jasne polukružnice ako je to stabilnije i lakše testirati.
>
> Dodaj string-level testove I/J vrijednosti i smjera luka.
>
> Status nakon ovoga: IMPLEMENTIRANO i SOFTVERSKI TESTIRANO; NIJE TESTIRANO NA STROJU.

## Prompt 9.5 – `.nc` export service

**Način: DIRECT CODE**

> Implementiraj odvojeni `.nc` export service.
>
> Zahtjevi:
> - validiraj `.nc` ekstenziju
> - plain text
> - eksplicitni charset prikladan ASCII G-kodu
> - ne koristi default Locale za brojeve
> - ne zapisuj automatski na USB
> - test zapisuje privremenu `.nc` datoteku i čita je natrag.
>
> UI će kasnije odabrati destination path.

---

# 10. JavaFX UI – ITERACIJA 1, jedan element

## Prompt 10.1 – FXML decision gate

**Način: ASK / AUDIT**

> Usporedi programatski JavaFX UI i FXML + controller za stvarni trenutni projekt.
>
> Kriteriji:
> - studentski projekt
> - održivost
> - odvajanje view/controller
> - jednostavnost obrane
> - postojeći package kostur
> - količina dodatne konfiguracije.
>
> Ne mijenjaj kod prije odluke. Ako se odabere FXML, `javafx.fxml` dodaj tek tada. FXML datoteke idu u `src/main/resources/hr/lukabosnjak/ui/view/` ili drugi jasno dokumentiran resource path; ne trebamo Java `ui.view` klasu samo zato što postoji taj naziv.
>
> Ne uvoditi `module-info.java` samo radi FXML-a ako je projekt već svjesno non-modularan i nema stvarne potrebe za modulima.

## Prompt 10.2 – Single-element forma + dinamička shape polja

**Način: PLAN → CODE**

> Implementiraj vizualni kostur i ponašanje glavne forme bez spajanja na G-code/persistence.
>
> Forma:
> - ShapeType
> - dinamička shape polja
> - MaterialSheet width/height/thickness
> - machine
> - tool
> - machining parameters
> - Generate
> - G-code preview
> - Save
> - Export `.nc`
> - Saved Programs navigation.
>
> Dinamička polja:
> - Square → stranica
> - Rectangle → width + height
> - Circle → promjer
> - Equilateral Triangle → stranica.
>
> UI ne prikazuje `dimensionA/B/C`.
>
> Dodaj centralizirano parsiranje numeričkih inputa da controller ne ponavlja isti try/catch.
>
> **Ne dodavati quantity, capacity, requiredSheets ni layout prikaz.**

## Prompt 10.3 – Generate workflow za jedan element

**Način: PLAN → CODE**

> Spoji `Generate` s postojećim service/validation/geometry/gcode slojevima.
>
> Tok:
> `UI input -> DTO/request po potrebi -> validation -> single-shape ToolPath -> bounds/fit -> GCodeGenerator -> preview`.
>
> Ako `MachiningJob` ima quantity, u Iteraciji 1 koristi vrijednost `1` bez UI polja.
>
> Controller smije čitati input, pozvati service i prikazati rezultat/pogrešku. Ne računa trokut, ne radi SQL i ne slaže G-code string.
>
> Ako još ne postoji `ProgramGenerationService`, uvedi ga samo ako stvarno koordinira ovaj use-case; ne stvaraj mega-service.

## Prompt 10.4 – Save / reopen / export workflow

**Način: PLAN → CODE**

> Implementiraj u jednom milestoneu:
> - spremanje generiranog single-element joba preko service/repository sloja
> - pregled spremljenih jobova
> - otvaranje joba
> - quick access: ponovno učitavanje spremljenih parametara u formu
> - prikaz spremljenog G-koda
> - Export gumb preko postojećeg `.nc` export servicea i JavaFX file choosera.
>
> Controller ne sadrži SELECT/INSERT niti file-writing logiku.
>
> Quantity u ovoj iteraciji ostaje `1`.

---

# 11. Integracija – ITERACIJA 1

## Prompt 11.1 – Composition root + end-to-end milestone

**Način: PLAN → CODE**

> Pregledaj sve postojeće slojeve i uvedi jedno jasno composition-root mjesto u `hr.lukabosnjak.app` gdje se ručno povezuju ovisnosti bez Springa/DI frameworka.
>
> Poveži samo komponente koje već postoje:
> - database config/initializer
> - repository implementacije
> - validators
> - geometry generators / ToolPathService
> - single-shape fit/bounds
> - RichAuto generator/profile
> - application/service sloj
> - controller dependencies.
>
> `LayoutService` još ne postoji.
>
> Dodaj/proširi integration testove za tok:
> `input -> validation -> ToolPath -> fit -> G-code -> preview/result -> persistence -> reload`.
>
> Testni primjer:
> - ploča 500 × 500 mm
> - jednakostranični trokut
> - stranica 30 mm
> - quantity = 1
> - testni machining parametri jasno označeni kao softverske vrijednosti.
>
> Fresh repository context mora ponovno učitati shape, material, machining parameters, quantity=1 i gCode.
>
> Ovo je softverski end-to-end test, ne fizička potvrda RichAuto kompatibilnosti.

---

# 12. Korektivni audit baze, modela i bootstrap podataka – PRIJE prvog testa

**Kontekst:** korisnik je došao do starog Koraka 12, ali pregled stvarnog UI-a i baze pokazao je da je aplikacija još razvojni kostur. Ovaj korak ne vraća projekt na početak: koristi postojeći kod iz Koraka 1–11 i ispravlja samo ono što je potrebno da Iteracija 1 postane cjelovita aplikacija.

## Prompt 12.1 – Audit trenutačnog stanja i gap analiza

**Način: ASK / AUDIT**

> Pregledaj stvarni repozitorij nakon završenog starog Koraka 11. Ne mijenjaj kod.
>
> Usporedi stvarno IMPLEMENTIRANO stanje s novim acceptance kriterijem Iteracije 1: prije prvog fizičkog testa sve V1 funkcionalnosti osim quantity/layout/batch dijela moraju biti funkcionalne.
>
> Posebno provjeri:
> - pokreće li se file-based H2 baza i inicijalizira li schema automatski
> - koje tablice imaju podatke
> - postoje li `ROLE`, `APP_USER`, `MATERIAL_TYPE`, `CNC_MACHINE` i `TOOL` repositoryji i metode potrebne za UI
> - rade li Save / Saved Programs / reopen / export stvarno ili su samo UI gumbi
> - je li trenutačni glavni ekran još označen kao „UI kostur“
> - jesu li `MachiningParameters` obvezna ručna UI polja samo zato što su bila u ranijem planu
> - gdje su u kodu hardkodirane ili testne machine/profile vrijednosti
> - što u koracima 1–11 treba minimalno refaktorirati zbog novih odluka o loginu, seedu i operatorskom workflowu.
>
> Rezultat mora biti tablica: `Funkcionalnost | IMPLEMENTIRANO | TESTIRANO | Nedostaje | Predloženi novi korak`.
>
> Ne implementiraj quantity/layout/batch.

## Prompt 12.2 – Usklađivanje modela stroja s poznatim/nepoznatim podacima

**Način: PLAN → CODE**

> Pregledaj `CncMachine`, `schema.sql`, repository mapping i validaciju.
>
> Želimo moći spremiti naš stvarni stroj bez izmišljanja tehničkih specifikacija:
> - name/model: `ZK-1325`
> - controller: `RichAuto A11`
> - workAreaX: `1250 mm`
> - workAreaY: `2500 mm`
> - workAreaZ: približno `80 mm` je informacija iz razgovora, ali je nemoj koristiti kao strogi safety limit ako u projektu nije potvrđena mjerenjem/dokumentacijom
> - manufacturer: nepoznat
> - maxFeedRate: nepoznat
> - minSpindleSpeed: nepoznat
> - maxSpindleSpeed: nepoznat.
>
> Ako su nepoznati podaci danas primitive `double` + SQL `NOT NULL`, predloži minimalnu migraciju na nullable reprezentaciju (`Double` / SQL `NULL`) samo za atribute koji stvarno mogu biti nepoznati. Nemoj unositi `0` kao lažnu zamjenu za „nepoznato“.
>
> Ažuriraj repository mapping i validaciju tako da nepoznata tehnička granica ne blokira aplikaciju, ali potvrđena granica i dalje bude provjerena kada postoji.
>
> Dodaj integration testove i dokumentiraj promjenu modela.

## Prompt 12.3 – Idempotentni V1 bootstrap / seed

**Način: PLAN → CODE**

> Implementiraj jasan i idempotentan development/bootstrap seeder koji se izvršava nakon `DatabaseInitializer`a.
>
> Seed pravila su zaključana:
> - `ROLE`: `ADMIN`, `ENGINEER`, `OPERATOR`
> - `MATERIAL_TYPE`: ukupno 5 seed zapisa; stvarne nazive koristi samo ako su potvrđeni, inače koristi jasno označene development vrijednosti (`TEST_MATERIAL_*`) umjesto izmišljanja da su to stvarni materijali
> - `CNC_MACHINE`: točno jedan potvrđeni `ZK-1325 / RichAuto A11` s poznatim vrijednostima i `NULL` za stvarno nepoznate
> - `TOOL`: trenutno **ne izmišljati** zapise; dodati tek stvarno potvrđeni alat Ø6 ili Ø8 mm kada znamo koji je stvarni alat i potrebne atribute
> - `MATERIAL_SHEET`, `SHAPE`, `MACHINING_PARAMETERS`, `MACHINING_JOB`: ne seedati u normalnoj bazi; to su podaci konkretnih poslova/snapshotovi.
>
> `APP_USER` seed pripada Koraku 13 nakon implementacije stvarnog password hashera. Nemoj privremeno spremiti čiste lozinke samo da bi baza bila popunjena.
>
> Seed mora biti siguran za višestruko pokretanje: drugi start aplikacije ne smije napraviti duplikate.
>
> Dodaj test koji pokrene ovaj dio seedera dvaput i potvrdi isti broj bootstrap zapisa.

## Prompt 12.4 – Database/UI lookup smoke test

**Način: DIRECT CODE**

> Dodaj ili proširi integration test kojim se potvrđuje da nakon inicijalizacije i seeda aplikacijski service/repository sloj može dohvatiti:
> - tri role
> - pet material type zapisa
> - jedan ZK-1325 stroj.
>
> Development korisnike ovdje još ne očekuj; oni se seedaju u Koraku 13 nakon što postoji stvarni password hasher.
>
> Ne očekuj Tool zapis dok stvarni alat nije potvrđen.
>
> Ažuriraj bilješke i stani.

---

# 13. Login, registracija, session i uloge korisnika – ITERACIJA 1

Ovaj dio sada je **potvrđeni V1 scope**. Ponašanjem se može ugledati na raniji ASP.NET MVC projekt: korisnici se prijavljuju, postoje role, postoji bootstrap korisnika/uloga i autorizacija određenih funkcionalnosti. Ne kopira se ASP.NET Identity framework; radi se čitljivo Java/JavaFX/JDBC rješenje primjereno studentskom desktop projektu.

## Prompt 13.1 – Role/RBAC decision gate

**Način: ASK / AUDIT**

> Potvrđene role su:
> - `ADMIN`
> - `ENGINEER`
> - `OPERATOR`.
>
> Pregledaj postojeći UI i funkcionalnosti te predloži minimalnu matricu prava bez enterprise-kompleksnosti.
>
> Obavezno riješi:
> - koja rola je default za samostalnu registraciju
> - tko smije upravljati korisnicima i mijenjati role
> - tko smije dodavati/uređivati `CNC_MACHINE`, `TOOL`, `MATERIAL_TYPE`
> - tko smije generirati, spremati, otvarati i exportati programe.
>
> Sigurnosno pravilo: korisnik se pri registraciji ne smije sam proglasiti `ADMIN`om.
>
> Ne mijenjaj kod dok matricu ne odobrim i dok odluka nije zapisana u `00_odluke.md`.

## Prompt 13.2 – Password hashing + AuthService + SessionContext

**Način: PLAN → CODE**

> Implementiraj jednostavan lokalni authentication sloj bez Spring Securityja i bez ASP.NET Identityja.
>
> Potrebe:
> - `PasswordHasher` ili ekvivalent s modernim salted password-hashing pristupom; ne koristiti plain SHA-256 i ne spremati plaintext
> - stvarni algoritam/parametre odabrati prema dostupnom JDK-u/dependencyjima i zabilježiti odluku
> - `AuthService.login(username, password)`
> - `AuthService.register(...)` prema potvrđenom modelu `User`
> - `SessionContext` ili sličan mali objekt koji drži samo trenutno prijavljenog korisnika
> - logout
> - odbijanje inactive korisnika
> - korisničko ime mora biti jedinstveno prema stvarnoj schemi.
>
> Ne dodaj email samo zato što ga je imao ASP.NET projekt ako email nije dio našeg potvrđenog `User` modela.
>
> Nakon što hasher postoji, proširi idempotentni seeder s **3–5 development/test korisnika** i dodijeli im role prema potvrđenoj matrici. Seed lozinke moraju proći kroz isti hasher kao i registrirani korisnici; baza nikada ne sadrži plaintext lozinke.
>
> Dodaj unit/integration testove za dobru i lošu lozinku, nepostojećeg korisnika, inactive korisnika, registraciju, dupli username i dvostruko pokretanje user seeda bez duplikata.

## Prompt 13.3 – Login / Registration JavaFX ekrani

**Način: PLAN → CODE**

> Implementiraj početni login ekran i registration ekran u istom UI pristupu koji stvarni projekt već koristi.
>
> Login:
> - username
> - password
> - Login
> - navigacija na Registration.
>
> Registration koristi samo potvrđena polja `User` modela i potvrđenu default rolu iz 13.1. Lozinka i potvrda lozinke su UI inputi; u bazu ide samo hash.
>
> Nakon uspješnog logina otvara se glavni aplikacijski ekran. Glavni ekran mora prikazati barem identitet/username prijavljenog korisnika i omogućiti Logout.
>
> Controller ne radi SQL niti hashing.

## Prompt 13.4 – Autorizacija i upravljanje korisnicima

**Način: PLAN → CODE**

> Implementiraj minimalnu autorizaciju prema odobrenoj role matrici iz 13.1.
>
> Za administratorski dio implementiraj funkcionalnosti koje su stvarno potrebne:
> - pregled korisnika
> - aktivacija/deaktivacija
> - promjena role ako je odobrena matricom
> - po potrebi kreiranje korisnika od strane admina.
>
> Ne radi složeni permission framework. Jedna centralizirana provjera role/prava je bolja od dupliciranih `if (role...)` provjera po svim controllerima.
>
> Testiraj da neovlaštena rola ne može otvoriti/izvršiti administratorsku akciju.

---

# 14. Dovršavanje JavaFX korisničkog sučelja – ITERACIJA 1

Cilj ovog koraka je da prije strojnog testa aplikacija **više ne izgleda i ne ponaša se kao „Single-element program — UI kostur“**. Ne uvodi se quantity/layout.

## Prompt 14.1 – UI/UX audit trenutnog glavnog ekrana

**Način: ASK / AUDIT**

> Pregledaj stvarni screenshot i trenutačni JavaFX kod.
>
> Trenutačni ekran ima sve elemente u jednom velikom obrascu i ručno pokazuje šest `Machining parameters` polja. Napravi prijedlog završnog desktop rasporeda za Iteraciju 1.
>
> Ciljevi:
> - jasno hrvatsko korisničko sučelje
> - ukloniti tekstove tipa „UI kostur“
> - vizualno odvojiti osnovni posao od administracije/kataloga
> - ostaviti dovoljno prostora za G-code preview, ali Saved Programs ne mora trajno zauzimati donju polovicu glavnog ekrana ako je zaseban ekran pregledniji
> - machine/tool/material odabiri dolaze iz baze
> - `Dodaj...` akcije vode na stvarne forme samo ako ih rola smije koristiti
> - bez quantity/layout elemenata do GATE 1.
>
> Ne mijenjaj kod u ovom promptu. Pokaži predloženu navigaciju i ekrane.

## Prompt 14.2 – Application shell + navigacija

**Način: PLAN → CODE**

> Implementiraj odobreni application shell/navigaciju.
>
> Minimalno:
> - glavni generator ekran
> - Saved Programs ekran
> - User/Admin ekran prema roli
> - Catalog/Settings ekran ili dijalozi za `MaterialType`, `CncMachine`, `Tool` prema role matrici
> - prikaz prijavljenog korisnika/role i Logout.
>
> Nemoj raditi SPA/router framework; koristi jednostavan JavaFX način prikladan postojećem projektu.

## Prompt 14.3 – Kataloški CRUD koji stvarno treba UI-u

**Način: PLAN → CODE**

> Dovrši stvarne forme i service/repository tokove za kataloge koje korisnik treba prije generiranja:
> - `MaterialType`
> - `CncMachine`
> - `Tool`.
>
> Nemoj forsirati puni CRUD ako nije potreban. Minimalno omogući pregled/dohvat i dodavanje/uređivanje onih zapisa za koje je to odobreno role matricom.
>
> `Tool` se ne smije stvarati s lažnim podacima. Za stvarni Ø6/Ø8 alat dopusti unos tek kada korisnik ima potvrđene vrijednosti.
>
> UI controller ne smije pisati SQL.

## Prompt 14.4 – Saved Programs / quick access finalizacija

**Način: PLAN → CODE**

> Dovrši `Saved Programs` kao stvarnu funkcionalnost:
> - lista spremljenih jobova
> - naziv, datum, korisnik i osnovni podaci potrebni za prepoznavanje
> - odabir programa
> - prikaz spremljenog G-koda
> - ponovno učitavanje parametara u generator formu (quick access)
> - export odabranog `.nc` programa.
>
> Test mora dokazati fresh-context round-trip preko H2 baze.

## Prompt 14.5 – Završno oblikovanje Iteracije 1

**Način: DIRECT CODE**

> Uskladi spacing, veličine kontrola, poruke, disable/enable stanja i stilove da ekran bude uredan desktop alat, a ne razvojni kostur.
>
> Ne uvodi dekorativne animacije niti kompleksan CSS samo radi izgleda. Prioritet su čitljivost, konzistentnost i jasan glavni workflow.
>
> Provjeri ručno minimalno: login -> glavni ekran -> izbor baze -> generate -> preview -> save -> saved programs -> reopen -> export -> logout.

---

# 15. Pojednostavljenje operatorskog workflowa i machining postavki

## Važna nova činjenica iz referentnog `.nc` programa

Dostavljeni referentni program za pravokutnik 100 × 200 mm je:

```gcode
G90 G54
M03
G00 Z5.000
G00 X0.000 Y0.000
G01 Z-1.000 F150.000
G01 X100.000 Y0.000 F500.000
G01 X100.000 Y200.000
G01 X0.000 Y200.000
G01 X0.000 Y0.000
G00 Z5.000
M05
M30
```

Iz ovoga **ne slijedi** da brzine nisu potrebne: program sadrži `F150` i `F500`. Ono što želimo promijeniti jest da operator ne mora nužno svaki put ručno upisivati tehničke vrijednosti u glavnu formu.

RichAuto A11 dokumentacija pokazuje da kontroler ima vlastite processing postavke te G-code atribute kojima se `F`, `S`, `G54` i `G40` mogu čitati ili ignorirati. Zato UI i generator moraju biti projektirani oko stvarnog profila kontrolera, a ne oko šest obveznih praznih TextFieldova.

## Prompt 15.1 – Audit što zaista mora biti korisnički input

**Način: ASK / AUDIT**

> Pregledaj `MachiningParameters`, `RichAutoA11Profile`, trenutačni UI, service sloj i dostavljeni referentni `.nc`.
>
> Za svaki podatak klasificiraj:
> 1. mora ga korisnik zadavati za svaki posao
> 2. treba dolaziti iz odabranog Tool/Machine/profile/preseta
> 3. controller može ignorirati/odrediti sam
> 4. još nije potvrđeno na našem stroju.
>
> Analiziraj najmanje:
> - spindleSpeed
> - feedRate
> - plungeRate
> - cutDepth
> - stepDown
> - safeZ
> - machine
> - tool/diameter
> - material type/sheet dimensions.
>
> Nemoj brisati `MachiningParameters` iz modela samo zato što ga želimo maknuti iz glavnog UI-a.

## Prompt 15.2 – Process/profile settings bez ručnog unosa svih parametara

**Način: PLAN → CODE**

> Na temelju odobrene analize iz 15.1 implementiraj najmanje složen način da glavni operatorski ekran ne traži svih šest machining vrijednosti pri svakom poslu.
>
> Preferiraj postojeći `RichAutoA11Profile` i mali process/default settings objekt/preset ako je dovoljan. Ne stvaraj veliki novi subsystem.
>
> Za prvi test vrijednosti smiju biti:
> - potvrđene iz stvarnog workflowa/operatora, ili
> - jasno označene kao test/reference vrijednosti koje još nisu fizički potvrđene.
>
> Aplikacija mora i dalje moći spremiti korištene vrijednosti u `MachiningParameters` snapshot kako bi se kasnije moglo objasniti s kojim je postavkama job generiran.
>
> Main UI treba prikazati samo ono što je korisniku stvarno potrebno. Eventualni „Advanced/Technical settings“ ekran može postojati ako je potreban, ali ne smije biti obvezan dio svakog Generate toka.

## Prompt 15.3 – Stvarni tool unos i veza s promjerom

**Način: PLAN → CODE**

> U projektnoj dokumentaciji postoji nova terenska informacija da se na stroju koriste/razmatraju glodala Ø6 mm ili Ø8 mm.
>
> Nemoj automatski seedati oba kao činjenicu. Omogući da se stvarni `Tool` zapis unese kroz odobreni katalog kada znamo:
> - tool number/oznaku ako postoji
> - naziv
> - diameter: 6 ili 8 mm prema stvarno odabranom alatu
> - ostale atribute samo ako su stvarno poznati ili model dopušta da budu nepoznati.
>
> Promjer mora biti dostupan generatoru/kompenzacijskom sloju; korisnik ga ne treba ponovno tipkati ako je već pohranjen uz alat.

---

# 16. RichAuto A11 generator – referentni `.nc` + kompenzacija alata

**Status izvora:** dostavljeni `.nc` je referentni program koji je osoba upoznata s tim strojem ocijenila kao prihvatljiv za pravokutnik 200 × 100 mm, uz napomenu da nedostaje kompenzacija alata. To je vrijedan real-world reference, ali još nije dokaz fizičkog testa naše aplikacije.

## Prompt 16.1 – Referentni `.nc` kao regression fixture

**Način: PLAN → CODE**

> U `src/test/resources` ili `dokumentacija/reference/` spremi referentni program 100 × 200 mm kao testni artefakt, uz bilješku o porijeklu i statusu: stručni pregled/referenca, **NIJE TESTIRANO kao output naše aplikacije na stroju**.
>
> Usporedi postojeći `RichAutoA11GCodeGenerator` s referencom:
> - `G90 G54`
> - `M03`
> - safe Z 5.000
> - XY start 0,0
> - plunge Z-1.000 uz `F150`
> - rezanje uz `F500`
> - retract
> - `M05`
> - `M30`.
>
> Nemoj slijepo mijenjati generator da byte-for-byte kopira referencu ako postoje opravdane profile opcije. Zapiši svaku razliku i odluči je li:
> - namjerna i konfigurabilna
> - nepotrebna
> - potencijalna nekompatibilnost za prvi test.
>
> Dodaj regression test za naš generator koristeći iste geometrijske dimenzije i eksplicitne reference postavke.

## Prompt 16.2 – Cutter compensation decision gate: G41/G42 ili geometrijski offset

**Način: ASK / AUDIT**

> Nova potvrđena potreba: generirana putanja mora uzeti u obzir promjer alata. Terenska povratna informacija izričito navodi lijevu/desnu kompenzaciju `G41` / `G42` i alat Ø6 ili Ø8 mm.
>
> RichAuto dokumentacija potvrđuje G-code postavku `Read G40`, a standardna G-code semantika je:
> - `G40` cancel cutter compensation
> - `G41` cutter compensation left
> - `G42` cutter compensation right.
>
> Ali nemoj pretpostaviti da RichAuto A11 koristi isti `D`/tool-offset workflow kao Haas/LinuxCNC bez provjere konkretnog kontrolera.
>
> Usporedi dvije V1 opcije:
> 1. controller-side compensation (`G41/G42/G40`) uz potvrđen način zadavanja radijusa/offseta i odgovarajući lead-in/lead-out
> 2. aplikacija unaprijed računa offsetirani ToolPath prema radijusu alata, a G-code ostaje bez controller compensation naredbi.
>
> Usporedi:
> - složenost za sva 4 shapea
> - pouzdanost na našem A11
> - potrebu za D/tool-table podacima
> - lead-in/lead-out
> - unutarnji/vanjski rez
> - utjecaj na layout Iteraciju 2.
>
> Zaustavi se dok ne odobrim strategiju i dok se odluka ne upiše u `00_odluke.md`.

## Prompt 16.3 – Implementacija odabrane kompenzacijske strategije

**Način: PLAN → CODE**

> Implementiraj samo strategiju odobrenu u 16.2.
>
> Obavezni zahtjevi bez obzira na strategiju:
> - koristi stvarni `Tool.diameter`
> - radius = diameter / 2
> - jasno razlikuj programiranu konturu komada od putanje centra alata
> - unutarnja/vanjska strana mora biti eksplicitna; ne pogađaj `G41/G42` samo iz naziva shapea
> - smjer putanje mora biti determinističan i dokumentiran
> - dodaj testove za Ø6 i Ø8 mm na jednostavnom pravokutniku, ali te promjere označi kao tehničke testne slučajeve dok konkretni tool zapis nije fizički potvrđen
> - kompenzaciju resetirati/ugasiti na dokumentiran način prije završetka programa
> - ne dopustiti da kompenzirana putanja izađe iz sheet/machine granica.
>
> Za controller-side varijantu obavezno testiraj lead-in/lead-out strukturu i ne emitiraj neprovjeren `D` format.

## Prompt 16.4 – Finalno pravilo za F/S/G54 i UI

**Način: PLAN → CODE**

> Uskladi generator i `RichAutoA11Profile` s onime što trenutno znamo:
> - referentni program koristi `G54`
> - referentni program koristi `F150` i `F500`
> - referentni program ne sadrži `S`
> - RichAuto A11 može imati F/S/G54 read/ignore konfiguraciju.
>
> Cilj nije ukloniti F/feed iz koda, nego ukloniti nepotreban ručni unos iz glavnog UI-a.
>
> Generator mora moći reproducirati potvrđeni reference-profile oblik outputa, a profile opcije moraju ostati dovoljno jasne da nakon fizičkog testa zabilježimo točno ponašanje konkretnog A11.
>
> `MachiningParameters` snapshot mora odražavati vrijednosti s kojima je program stvarno generiran, čak i kada ih korisnik nije ručno upisao.

## Prompt 16.5 – Software audit sva 4 single-element shapea

**Način: PLAN → CODE**

> Nakon kompenzacije i profile promjena provjeri sva četiri V1 oblika:
> - square
> - rectangle
> - circle
> - equilateral triangle.
>
> Za svaki testiraj:
> - validaciju
> - originalni ToolPath
> - kompenzirani path/kompenzacijske naredbe prema odabranoj strategiji
> - granice ploče i machine XY
> - Z/pass logiku
> - deterministic `.nc` output.
>
> To je SOFTVERSKO TESTIRANJE, ne fizička potvrda stroja.

---

# 17. Release-candidate integracija – ITERACIJA 1

Ovo je zadnji softverski korak prije prvog fizičkog testa. Acceptance kriterij nije „generator se kompilira“, nego da je aplikacija praktično dovršena za **single-element workflow**.

## Prompt 17.1 – Full end-to-end Iteracija 1

**Način: PLAN → CODE**

> Provjeri i popravi samo integracijske nedostatke cijelog toka:
>
> `start -> DB init/seed -> login/register -> session/role -> main UI -> load machine/material/tool -> shape input -> validation -> generation -> compensation -> preview -> save -> Saved Programs -> reopen -> export .nc -> logout`.
>
> Quantity je interno 1. Nema layouta, capacityja ni requiredSheets.
>
> Obavezno provjeri da nakon restarta aplikacije:
> - bootstrap podaci nisu duplicirani
> - korisnik se može ponovno prijaviti
> - spremljeni program ostaje dostupan
> - quick access ponovno učitava job
> - `.nc` se može ponovno exportati.

## Prompt 17.2 – UI release-candidate checklist

**Način: ASK / AUDIT**

> Pregledaj aplikaciju kao krajnji korisnik i napravi PASS/FAIL checklist:
> - nema placeholder/„UI kostur“ tekstova
> - hrvatski nazivi i poruke su konzistentni
> - obavezna polja su jasna
> - stroj/material/tool ComboBoxovi rade iz baze
> - neovlaštene admin akcije nisu dostupne
> - raw machining polja nisu obvezna na glavnom ekranu osim ako je prethodna odluka dokazala da neko polje mora ostati
> - Generate, Save, Saved Programs, Reopen, Export i Logout rade
> - greške se prikazuju razumljivo bez rušenja aplikacije
> - quantity/layout funkcionalnosti nisu prerano prikazane.
>
> Ako nešto pada, vrati se samo na odgovarajući korak i popravi prije strojnog testa.

## Prompt 17.3 – Referentni pravokutnik 100 × 200 mm: static release audit

**Način: ASK / AUDIT**

> Generiraj pravokutnik 100 × 200 mm s točno odabranim testnim Tool/profile postavkama i usporedi rezultat s dostavljenim referentnim `.nc` programom.
>
> Posebno provjeri:
> - geometrijske dimenzije
> - početak/kraj programa
> - G90/G54
> - M03/M05/M30
> - safe Z i Z dubinu
> - F vrijednosti prema profilu
> - cutter compensation prema odluci iz 16.2
> - granice XY nakon kompenzacije.
>
> Razlike dokumentiraj. Ne označavaj fizički TESTIRANO.

---

# 18. PRVI TEST – ZK-1325 / RichAuto A11, jedan element

**Ovo je stari Korak 12 pomaknut tek nakon dovršetka baze, korisnika, UI-a, operatorskog workflowa i generatora.**

## Prompt 18.1 – Pre-machine checklist

**Način: ASK / AUDIT**

> Na temelju stvarno implementiranog release-candidate generatora napravi checklist prije fizičkog testa.
>
> Mora uključiti:
> - verziju aplikacije/commit
> - točan stroj `ZK-1325`
> - kontroler `RichAuto A11`
> - potvrđeno radno područje
> - orientation X/Y/Z osi
> - WCS/work zero
> - stvarne F/S/G54/G40 read/ignore postavke kontrolera
> - spindle ponašanje
> - stvarni Tool zapis i diameter
> - odabranu cutter-compensation strategiju
> - lead-in/lead-out ako se koristi G41/G42
> - safe Z
> - cut depth / step-down
> - feed/plunge/spindle vrijednosti koje će se stvarno koristiti
> - pregled `.nc` datoteke.
>
> Ne predlaži proizvoljne machining vrijednosti. Sve fizičke parametre prije rezanja mora potvrditi operator konkretnog stroja.

## Prompt 18.2 – Statički audit stvarnog `.nc` testa

**Način: ASK / AUDIT**

> Auditiraj **točno onu `.nc` datoteku koja će biti prenesena na stroj**.
>
> Provjeri:
> - koordinatni mod / G90
> - G54 samo prema potvrđenoj konfiguraciji
> - spindle start/stop
> - safe Z prije XY premještanja
> - Z target i step-down
> - feed/plunge ponašanje
> - cutter compensation / geometrijski offset
> - lead-in/lead-out i G40 ako je primjenjivo
> - sve XY granice nakon kompenzacije
> - decimalni format
> - završetak programa.
>
> Ne mijenjaj kod ako nema konkretno pronađenog problema.

## Prompt 18.3 – Kontrolirani test bez rezanja / dry-run prema operatoru

**Način: ASK / AUDIT**

> Pripremi testni protokol koji prvo provjerava koordinatni sustav i putanju bez stvarnog reza, prema proceduri koju odobri iskusni operator.
>
> Potvrditi:
> - emergency stop / machine state
> - XY orijentaciju
> - work zero
> - smjer Z i safe Z
> - ponašanje programa s G54
> - ponašanje F/S prema stvarnoj konfiguraciji
> - aktivaciju/deaktivaciju cutter compensation ako se koristi controller-side G41/G42
> - da očekivana putanja ne izlazi iz materijala/stola.
>
> Ne izmišljaj strojne sigurnosne korake; operator ima zadnju riječ za fizičku proceduru.

## Prompt 18.4 – Prvi stvarni single-element test

**Način: ASK / AUDIT**

> Tek nakon uspješnog 18.3 pripremi test-case za prvi stvarni rez.
>
> Preporučeni prvi geometrijski test može ostati:
> - ploča 500 × 500 mm
> - jednakostranični trokut 30 mm
> - quantity = 1,
>
> ali ako operator za prvo rezanje preferira jednostavniji pravokutnik 100 × 200 mm zbog postojećeg referentnog programa, to zabilježi kao opravdanu promjenu test-casea; ne mijenjaj unaprijed rezultat.
>
> Test-case mora zapisati:
> - stvarni material
> - stvarni tool i diameter
> - korištene machining/profile vrijednosti
> - compensation mode
> - expected path
> - actual result
> - izmjerene dimenzije
> - deviations
> - controller behavior
> - pass/fail
> - notes.
>
> Rezultate ostavi praznima dok se test stvarno ne izvrši.

## GATE 1 – odluka nakon prvog testa

Iteracija 2 smije početi tek kada je dokumentirano da:

- `.nc` se može učitati na ciljni ZK-1325 / RichAuto A11
- work zero/WCS i smjer osi su razumljivi
- single-element putanja prati očekivanu geometriju
- kompenzacija alata daje očekivanu dimenziju ili je identificiran i popravljen problem
- safe Z / Z dubina / step-down nemaju poznatu prepreku
- relevantno F/S/G54/G40 ponašanje je zabilježeno
- stvarni Tool zapis odgovara korištenom alatu
- eventualne korekcije generatora nakon testa ponovno prolaze software regression testove.

**Ako test ne prođe, ne počinjati quantity/layout. Popraviti osnovni generator i ponoviti Gate 1.**

---

# 19. Quantity – početak ITERACIJE 2

Tek nakon prolaska kroz GATE 1 aktivira se korisnički unos količine.

## Prompt 19.1 – Audit postojećeg `quantity` modela

**Način: ASK / AUDIT**

> Pregledaj gdje `quantity` već postoji u `MachiningJob`, SQL schemi, repository mappingu i testovima. Ne dupliciraj polje.
>
> Objasni što treba promijeniti da vrijednost koja je u Iteraciji 1 bila interno 1 postane korisnički input.

## Prompt 19.2 – Quantity validacija + UI

**Način: PLAN → CODE**

> Aktiviraj korisnički unos quantity:
> - cijeli broj
> - > 0
> - jasna validacijska poruka.
>
> Dodaj polje u postojeći dovršeni UI bez narušavanja single-element workflowa. `quantity = 1` mora ostati regresijski valjan slučaj.
>
> Još ne implementiraj layout.

---

# 20. Algoritam raspoređivanja više jednakih elemenata

Cilj nije dokazati matematičku optimalnost nego dati valjan, deterministički i objašnjiv raspored s boljom iskorištenošću materijala.

## Prompt 20.1 – Layout settings decision gate

**Način: ASK / AUDIT**

> Prije algoritma zaključi:
> - edge margin
> - part spacing
> - dopuštenu rotaciju pravokutnika
> - kako stvarni tool diameter/kompenzacija utječe na minimalni razmak.
>
> Nemoj koristiti skrivene magic default vrijednosti. Predloži mali `LayoutSettings` model i zaustavi se dok odluke nisu potvrđene.

## Prompt 20.2 – Layout modeli

**Način: DIRECT CODE**

> Implementiraj samo rezultatne modele potrebne layoutu, npr. `PlacedShape`, `SheetLayout`, `LayoutResult`, bez algoritma.
>
> Rezultat mora sadržavati placement, rotaciju/orijentaciju, indeks ploče, capacity per sheet i required sheets.

## Prompt 20.3 – Square / Rectangle / Circle baseline

**Način: PLAN → CODE**

> Implementiraj deterministički baseline:
> - Square/Rectangle grid uz margin/spacing
> - za Rectangle usporedi 0°/90° kada je rotacija dopuštena
> - Circle grid temeljen na stvarnom potrebnom footprintu i spacingu
> - cijeli komad/kompenzirana putanja mora ostati unutar ploče.
>
> Ne nazivati rezultat globalno optimalnim.

## Prompt 20.4 – Triangle layout + capacity/requiredSheets

**Način: PLAN → CODE**

> Implementiraj valjan layout jednakostraničnih trokuta, zatim `LayoutService` koji bira strategiju i računa:
> - placements
> - capacity per sheet
> - `requiredSheets = ceil(quantity / capacity)`
> - više `SheetLayout` rezultata kada je potrebno.
>
> Testiraj da nijedna stvarna putanja nakon tool-kompenzacije ne izlazi iz sheet granica.

---

# 21. Batch G-code – ITERACIJA 2

## Prompt 21.1 – Multi-sheet persistence decision gate

**Način: ASK / AUDIT**

> Sada stvarna potreba može biti `requiredSheets > 1`.
>
> Analiziraj treba li:
> 1. `GCODE_PROGRAM` 1:N prema jobu, jedan program po `SheetLayout`, ili
> 2. determinističko regeneriranje programa uz spremanje layouta, ako je jednostavnije i pouzdano.
>
> Ne spremaj više fizički odvojenih programa u jedan nejasan CLOB samo radi izbjegavanja schema promjene.
>
> Odluku zapiši u `00_odluke.md` prije implementacije.

## Prompt 21.2 – Batch generator

**Način: PLAN → CODE**

> Reuseaj **strojno potvrđenu single-element logiku** iz Iteracije 1.
>
> Za svaki `PlacedShape`:
> - transliraj ToolPath
> - primijeni istu compensation strategiju
> - koristi potvrđeni RichAuto profil
> - osiguraj safe Z između elemenata
> - provjeri sheet/machine granice.
>
> Ne dupliciraj geometry algoritme u batch generatoru.

## Prompt 21.3 – Multi-sheet output + export/persistence

**Način: PLAN → CODE**

> Implementiraj odluku iz 21.1 tako da svaki sheet ima jasan machine program i da Save/Reopen/Export zna kojem sheetu program pripada.
>
> Dodaj integration test za najmanje dva sheeta ako ga deterministički testni slučaj može proizvesti.

---

# 22. JavaFX UI – quantity i layout proširenje

## Prompt 22.1 – Batch UI milestone

**Način: PLAN → CODE**

> Proširi već dovršeni UI:
> - quantity input
> - capacity per sheet
> - required sheets
> - broj raspoređenih elemenata
> - odabir/pregled pojedinog sheeta/programa kada ih je više.
>
> Controller ne računa layout ni batch G-code.
>
> Grafički prikaz ploče nije obvezan za V1 ako tekstualni rezultat jasno zadovoljava zahtjev.

---

# 23. Integracija – ITERACIJA 2

## Prompt 23.1 – Batch end-to-end

**Način: PLAN → CODE**

> Provjeri cijeli tok:
>
> `login -> input quantity -> validation -> layout -> capacity/requiredSheets -> compensated ToolPaths -> G-code program(i) -> preview -> persistence -> reload -> export`.
>
> Test mora uključiti:
> - quantity 1 kao regression
> - malu quantity > 1
> - placements unutar sheet granica
> - machine work-area granice
> - multi-sheet slučaj ako postoji
> - isti tool/profile/compensation model potvrđen u Gate 1.

---

# 24. Batch test na ZK-1325

## Prompt 24.1 – Statički audit batch programa

**Način: ASK / AUDIT**

> Prije stroja auditiraj stvarno generirani batch program/program(e):
> - placement koordinate
> - tool compensation
> - sheet/machine granice
> - safe Z između elemenata
> - Z/pass logiku
> - potvrđeni F/S/G54/G40 profil
> - početak/kraj svakog programa
> - mapping program -> sheet.

## Prompt 24.2 – Kontrolirani batch test

**Način: ASK / AUDIT**

> Nakon uspješnog static audita koristi malu quantity vrijednost koju operator može lako vizualno provjeriti.
>
> Mjeri stvarni rezultat implementiranog layouta; ne proglašavaj ga matematički optimalnim.
>
> Zabilježi placement, capacity, required sheets, dimenzijske rezultate i ponašanje prijelaza između elemenata.

---

# 25. Finalni test report

## Prompt 25.1 – Finalni status IMPLEMENTIRANO / TESTIRANO

**Način: ASK / REPORT**

> Na temelju stvarnog koda i stvarno zabilježenih testova sastavi tehnički sažetak.
>
> Za svaku funkcionalnost označi:
> - IMPLEMENTIRANO
> - TESTIRANO
> - NIJE TESTIRANO
> - BUDUĆI RAZVOJ.
>
> Posebno razdvoji:
> - autentikaciju/role
> - H2/seed
> - single-element UI
> - geometriju
> - tool compensation
> - single-element G-code
> - fizički single-element test
> - quantity
> - layout
> - capacity/requiredSheets
> - batch G-code
> - persistence/multi-sheet
> - `.nc` export
> - batch fizički test.
>
> Ne izmišljaj mjerne rezultate.

---

# Dokumentacijska struktura projekta

```text
<project-root>
├── dokumentacija
│   ├── reference
│   │   └── pravokutnik_100x200_RichAuto_A11_primjer.nc
│   └── biljeske
│       ├── 00_indeks.md
│       ├── 00_predlozak_biljeske.md
│       ├── 00_odluke.md
│       ├── ...
│       └── 25-01-finalni-test-report.md
├── src
├── pom.xml
└── AGENTS.md
```

Referentna `.nc` datoteka u repozitoriju mora biti označena kao **referenca dobivena iz stvarnog radnog konteksta**, a ne kao dokaz da ju je naša aplikacija generirala ili da je output naše aplikacije već fizički testiran.

---

# Potvrđena package struktura – ažurirana za autentikaciju

Postojeća slojevita struktura ostaje. Nije potrebno rušiti paketnu arhitekturu samo zbog logina. Po potrebi se uvode male, jasno imenovane klase unutar postojećih slojeva:

```text
hr.lukabosnjak
├── app
├── config
├── domain
│   ├── model
│   └── enums
├── validation
├── geometry
├── layout                 # aktivan tek nakon GATE 1
├── gcode
├── service
│   └── dto
├── persistence
│   ├── repository
│   └── jdbc
└── ui
    ├── controller
    └── view
```

Moguće nove klase samo ako odgovaraju implementaciji:

```text
service/AuthService
service/SessionContext
service/PasswordHasher (ili zaseban auth podpaket samo ako stvarno povećava čitljivost)
ui/controller/LoginController
ui/controller/RegisterController
ui/controller/UserManagementController
```

Ne uvoditi Spring/Spring Security samo radi lokalnog desktop logina.

---

# Ažurirane ključne domenske napomene

## `CncMachine`

Poznate tehničke granice ostaju vrijednosti; nepoznate granice smiju biti nullable. `0` ne znači „nepoznato“.

## `Tool`

`diameter` je od sada izravno važan za korektnu kompenzaciju putanje. Terenska informacija Ø6/Ø8 mm nije dovoljan razlog za izmišljanje dva runtime zapisa bez potvrde stvarnog toola.

## `MachiningParameters`

Ostaje snapshot korištenih postavki čak i ako više nije skup obveznih TextFieldova na glavnom ekranu.

## `User` / `Role`

Role su potvrđene: `ADMIN`, `ENGINEER`, `OPERATOR`. Login/registration/session/RBAC su V1 funkcionalnosti. Lozinka se ne sprema kao plaintext.

---

# SQL V1 – ciljana korekcija

Tablice ostaju:

```text
ROLE
APP_USER
MATERIAL_TYPE
CNC_MACHINE
TOOL
MATERIAL_SHEET
MACHINING_PARAMETERS
SHAPE
MACHINING_JOB
```

Uz postojeća pravila obavezno:

```text
ROLE.name UNIQUE
APP_USER.username UNIQUE
TOOL: UNIQUE(cnc_machine_id, tool_number)
```

Bootstrap podaci ne smiju se duplicirati pri restartu.

`GCODE_PROGRAM` se ne uvodi u Iteraciji 1. Ponovno se razmatra u Iteraciji 2 tek ako `requiredSheets > 1` stvarno zahtijeva više fizičkih `.nc` programa.

---

# Ažurirani milestoneovi

## Milestone F1 – korektivna baza + autentikacija

Završeno kada:
- H2 first-run radi
- seed je idempotentan
- postoje tri role, dev users, 5 material types i ZK-1325
- login/register/logout/session rade
- role pravila su centralizirana i testirana.

## Milestone F2 – dovršeni single-element UI

Završeno kada:
- UI više nije kostur
- katalog/stroj/material/tool odabiri rade iz baze
- Saved Programs i quick access rade
- korisnik ne mora ručno unositi tehničke parametre koji mogu doći iz profila/preseta
- nema quantity/layout UI-a.

## Milestone F3 – generator spreman za Gate 1

Završeno kada:
- referentni 100 × 200 `.nc` je regression fixture
- razlike generator/reference su objašnjene
- tool compensation strategija je zaključana i implementirana
- tool diameter ulazi u stvarni izračun
- sva 4 shapea prolaze software testove
- status je IMPLEMENTIRANO/SOFTVERSKI TESTIRANO, ali još NIJE FIZIČKI TESTIRANO.

## Milestone G – GATE 1 fizički single-element test

Završeno kada:
- stvarni A11 profil je zabilježen
- tool/compensation je potvrđen
- kontrolirani test i stvarni rez su provedeni kada operator odobri
- rezultat je dokumentiran
- eventualne korekcije su ponovno regresijski testirane.

## Milestone H – quantity + layout

Završeno kada:
- quantity > 0 radi
- placements, capacity i requiredSheets rade
- tool compensation/spacing je uzet u obzir
- nema tvrdnje globalne optimalnosti.

## Milestone I – batch workflow

Završeno kada:
- batch G-code reusea potvrđeni single-element generator
- multi-sheet output/persistence je jasan
- UI prikazuje quantity/layout rezultate
- integration testovi prolaze.

## Milestone J – batch fizički test + finalni report

Završeno kada su stvarni rezultati uneseni i jasno odvojeni od samo implementiranih/softverski testiranih funkcionalnosti.

---

# Web-provjerene tehničke napomene za ovo ažuriranje

1. RichAuto A11 manual opisuje `Auto Pro Setup` s `Work Speed`, `Safe Height`, `Auto Scale`, `Fall Scale` i posebnim `G Code Setup` postavkama. U `G Code Setup` moguće je konfigurirati čitanje/ignoriranje `F`, `S`, `G54` i `G40`. To podržava odluku da glavni UI ne mora izlagati svaki tehnički parametar kao obvezni input, dok generator/profil i dalje mora znati stvarno korištene vrijednosti.
2. Referentni `.nc` koji je korisnik dostavio ipak sadrži feed vrijednosti: `F150` za plunge i `F500` za XY rezanje. Stoga se u ovom planu **ne uklanja feed logika**, nego se odvaja od obveznog ručnog unosa.
3. Standardna G-code semantika je `G41 = left cutter compensation`, `G42 = right cutter compensation`, `G40 = cancel`. Strana je relativna na smjer kretanja alata. Za ispravnu kompenzaciju potreban je radijus/promjer alata i odgovarajuća aktivacija/deaktivacija kompenzacije.
4. Dokumentacija drugog kontrolera (npr. LinuxCNC/Haas) koristi `D`/tool-offset tablice uz G41/G42, ali to se **ne smije automatski preslikati na RichAuto A11** bez provjere konkretnog controller workflowa.
5. RichAuto A11 manual navodi da je origin X/Y/Z u programu origin obratka, pa WCS/work-zero ostaje obvezna točka fizičkog Gate 1 testa.
6. Raniji ASP.NET MVC projekt koristi identity + role seed pristup. Ovdje se preuzima samo funkcionalna ideja: login, role, seed i autorizacija; Java desktop aplikacija dobiva vlastiti jednostavan JDBC/auth sloj.
7. Za lozinke koristiti moderni salted password-hashing/KDF, ne plaintext i ne brzi opći hash bez work factora.

Primarni/relevantni izvori koje treba sačuvati u projektnoj bilješci:

- RichAuto A11 User Manual, posebno Auto Pro Setup / G Code Setup i workpiece origin poglavlja
- službeni RichAuto A1X/A11 materijali kada su dostupni
- LinuxCNC/Haas dokumentacija samo za opću semantiku G41/G42/G40, ne kao dokaz RichAuto-specifičnog `D` ponašanja
- OWASP Password Storage Cheat Sheet za password hashing odluku
- korisnikov raniji ASP.NET MVC projekt samo kao UX/role funkcionalna referenca.

---

# Što NE raditi – ažurirano

- Ne ići na fizički stroj odmah nakon starog Koraka 11; prvo završiti nove Korake 12–17.
- Ne smatrati UI „gotovim“ samo zato što se kontrole prikazuju.
- Ne ostavljati login/users/roles kao mrtve tablice ako su sada potvrđeni V1 scope.
- Ne spremati lozinke u čistom tekstu.
- Ne dopustiti self-registration korisniku da sam odabere ADMIN rolu.
- Ne seedati 5 redaka u svaku tablicu samo radi popunjenosti.
- Ne izmišljati strojne limite ni Tool atribute.
- Ne unositi `0` kao lažnu vrijednost za „unknown“ machine spec.
- Ne izbaciti feed/spindle/depth model samo zato što ih ne želimo vidjeti na glavnom ekranu.
- Ne tvrditi da referentni program nema brzine: sadrži `F150` i `F500`.
- Ne hardkodirati da A11 sigurno čita/ignorira F, S, G54 ili G40 prije provjere konfiguracije.
- Ne dodati G41/G42 bez rješavanja tool radiusa, smjera putanje, strane reza i lead-in/lead-out ponašanja.
- Ne pretpostaviti `D` sintaksu ili tool-offset tablicu iz Haas/LinuxCNC dokumentacije kao RichAuto činjenicu.
- Ne nazvati Ø6 ili Ø8 „stvarnim aktivnim alatom“ dok nije potvrđeno koji se alat koristi u testu.
- Ne implementirati quantity/layout/capacity/requiredSheets prije GATE 1.
- Ne prebaciti machining/profile logiku u JavaFX controller.
- Ne raditi SQL u controlleru.
- Ne raditi geometriju ili layout unutar GCodeGeneratora.
- Ne tvrditi matematičku optimalnost layouta.
- Ne označiti RichAuto kompatibilnost kao TESTIRANU prije stvarnog testa.
- Ne preskakati `dokumentacija/biljeske/`, `00_indeks.md` i relevantne odluke u `00_odluke.md`.
