# Plan implementacije aplikacije – kontrolirani milestone promptovi za Codex

## Svrha dokumenta

Ovaj dokument je izvedbeni plan za razvoj aplikacije završnog rada **„Razvoj aplikacije za optimiziranje rada na CNC stroju“**. Plan koristi kombinaciju manjih koraka i grupiranih implementacijskih milestoneova. Jednostavne, međusobno povezane promjene mogu se odraditi u jednom Codex zadatku, dok se Plan/Ask Mode zadržava za arhitektonske odluke, višeslojne promjene, bazu podataka, geometriju, G-kod, layout, integraciju i druge rizičnije korake.

Glavni redoslijed više nije jedan linearni niz do završnog fizičkog testa. Razvoj je podijeljen u **dvije implementacijske iteracije** s obveznim prolazom kroz prvi stvarni test između njih.

## ITERACIJA 1 – dokaz da osnovni generator radi za jedan element

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
11. Integracija prve iteracije
12. Prvi test na ZK-1325 / RichAuto A11 za **jedan element**

### GATE 1 – obvezna kontrolna točka

**Ne implementirati korisnički unos količine, algoritam raspoređivanja, izračun kapaciteta ploče, broj potrebnih ploča ni batch G-code prije nego što je prvi single-element workflow dovoljno provjeren da možemo nastaviti.**

Prvi cilj je potvrditi cijeli lanac:

`Shape -> ToolPath -> RichAutoA11GCodeGenerator -> .nc -> ZK-1325 -> jedan element`

Testni primjer ostaje:

- ploča 500 × 500 mm
- jednakostranični trokut
- stranica 30 mm
- količina u prvoj iteraciji: **1**

## ITERACIJA 2 – više jednakih elemenata i raspoređivanje

13. Aktivacija korisničkog unosa količine
14. Algoritam raspoređivanja više jednakih elemenata
15. Batch G-code za raspoređene elemente
16. Proširenje JavaFX UI-a za quantity/layout rezultate
17. Integracija druge iteracije
18. Batch test na ZK-1325
19. Završni testni izvještaj

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

## 0.8. JDK status i još uvijek otvorene odluke

JDK je već odabran u stvarnom IntelliJ/Maven projektu. Ovaj plan ne izmišlja broj verzije: ako još nije zapisan u `00_odluke.md`, treba ga samo pročitati iz postojećeg projekta (`pom.xml`, IntelliJ Project SDK ili stvarni build output) i evidentirati. Ne treba ponovno trošiti poseban Codex milestone na provjeru razvojnog okruženja ako je to već napravljeno.

Sljedeće stavke se još uvijek ne smiju izmišljati:

- hoće li UI koristiti FXML ili će biti građen programatski
- konkretne vrijednosti `ToolType` enuma
- treba li `RoleType` enum i ide li autentikacija/RBAC u završni scope
- stvarne vrijednosti alata na ZK-1325
- stvarni machining parametri za materijale
- konkretne postavke `F`, `S`, `G54` na fizičkom RichAuto A11
- precizan smjer osi i work zero na stroju
- minimalni razmak među komadima i rubni odmak koji će se koristiti pri stvarnom rezanju

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

> Pregledaj postojeći projekt i u jednom milestoneu isplaniraj i implementiraj osnovne domenske modele u `hr.lukabosnjak.domain.entities`:
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

> Pregledaj trenutni domain model i implementiraj potvrđene geometrijske enumove u `hr.lukabosnjak.domain.enums` te `Shape` u `hr.lukabosnjak.domain.entities`.
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

# 12. PRVI TEST – ZK-1325 / RichAuto A11, jedan element

Ovo je obvezna kontrolna točka prije quantity/layout implementacije.

## Prompt 12.1 – Pre-machine checklist

**Način: ASK / AUDIT**

> Na temelju stvarno implementiranog single-element generatora napravi checklist prije fizičkog testa.
>
> Mora uključiti:
> - verziju aplikacije/commit
> - točan stroj i kontroler
> - work area X/Y/Z
> - orientation osi
> - WCS/work zero
> - F/S/G54 read/ignore postavke
> - spindle ponašanje
> - tool podatke
> - safe Z
> - cut depth / step down
> - feed/plunge/spindle
> - pregled `.nc` datoteke.
>
> Ne predlaži proizvoljne machining vrijednosti.

## Prompt 12.2 – Statički audit stvarnog `.nc` testa

**Način: ASK / AUDIT**

> Uzmi stvarno generiranu `.nc` datoteku za testni trokut i napravi statički audit bez pokretanja stroja.
>
> Provjeri:
> - mm / G21 prema profilu
> - G90/G17 prema profilu
> - G54 samo ako je uključeno
> - Z-konvenciju prema potvrđenom planu testa
> - safe Z prije XY premještanja
> - step-down
> - koordinate i granice
> - F/S prema profilu
> - M03/M05 prema profilu
> - M30
> - decimalni format.
>
> Ne mijenjaj kod ako nema konkretno pronađenog problema.

## Prompt 12.3 – Kontrolirani fizički test + test-case predložak

**Način: ASK / AUDIT**

> Nakon statičkog audita pripremi kontrolirani testni protokol koji mora odobriti iskusni operator konkretnog stroja.
>
> Prvo potvrditi XY orijentaciju, work zero, Z-smjer/safe položaj i ponašanje F/S/G54 prije stvarne obrade materijala. Ne izmišljaj strojne/sigurnosne postavke.
>
> Nakon što operator potvrdi machining parametre, pripremi test-case za:
> - material sheet 500 × 500 mm
> - equilateral triangle side 30 mm
> - quantity = 1
> - stvarni tool
> - stvarno potvrđene machining parametre
> - generated `.nc`.
>
> Test-case mora imati prazna polja:
> - expected path
> - actual result
> - measured dimensions
> - deviations
> - controller behavior
> - pass/fail
> - notes.
>
> Ne popunjavaj rezultate unaprijed.

## GATE 1 – odluka nakon prvog testa

Ne počinjati Iteraciju 2 dok nije dokumentirano:

- `.nc` se može učitati u ciljnom workflowu
- koordinatna orijentacija i work zero dovoljno su jasni
- Z-smjer/safe ponašanje je zabilježeno
- single-element putanja odgovara očekivanom obliku
- step-down nema poznatu prepreku
- ponašanje `F`, `S`, `G54` i spindle naredbi je zabilježeno
- eventualne korekcije su implementirane i ponovno softverski provjerene.

Ako test ne prođe, popravlja se Iteracija 1. Layout se ne koristi kao zaobilazno rješenje.

---

# 13. Quantity – početak ITERACIJE 2

## Prompt 13.1 – Aktivacija quantity kroz model/service workflow

**Način: PLAN → CODE**

> Pregledaj gdje `quantity` već postoji (`MachiningJob`, SQL, repository, test fixtures) i aktiviraj ga kao stvarni korisnički poslovni podatak bez dupliciranja polja.
>
> U ovom milestoneu:
> - quantity mora biti `int`
> - > 0
> - dodaj/aktiviraj validation testove za 1, veću vrijednost, 0 i negativnu vrijednost
> - prenesi quantity kroz service DTO/request sloj ako postoji
> - persistence mora round-tripati quantity > 1.
>
> Još ne implementiraj layout ni batch G-code i ne dodaj UI ako bi to zahtijevalo lažni layout rezultat.

---

# 14. Algoritam raspoređivanja više jednakih elemenata

Cilj V1 je deterministički, valjan i objašnjiv raspored. Ne tvrdi se matematička optimalnost.

## Prompt 14.1 – Layout contract: settings + result modeli

**Način: PLAN → CODE**

> Definiraj ulaze i rezultate layout sloja prije algoritma.
>
> Input:
> - sheet width/height
> - Shape
> - quantity
> - eksplicitni `LayoutSettings`.
>
> `LayoutSettings` treba imati vrijednosti poput edge margin, part spacing i policy dopuštene rotacije, ali bez skrivenih magic defaulta. Ne pretpostavljaj da spacing automatski uključuje promjer alata dok to nije odlučeno.
>
> Output modeli:
> - `PlacedShape`
> - `SheetLayout`
> - `LayoutResult`.
>
> Rezultat mora moći izraziti:
> - položaj/orijentaciju svakog komada
> - sheet index
> - capacity per sheet
> - required sheets
> - placements za traženu quantity vrijednost.
>
> Ne implementiraj algoritam raspoređivanja u ovom promptu.

## Prompt 14.2 – Baseline layout: Square / Rectangle / Circle

**Način: PLAN → CODE**

> Implementiraj jednostavne determinističke baseline strategije:
>
> Square/Rectangle:
> - grid
> - poštuj margin/spacing
> - nijedan bounding box ne izlazi iz ploče
> - za rectangle usporedi 0° i 90° samo ako `LayoutSettings` dopušta rotaciju
> - odaberi bolju od tih jednostavnih varijanti; ne nazivati globalno optimalnom.
>
> Circle:
> - pravilan grid temeljen na promjeru + spacingu
> - bez hexagonal close packing u baselineu
> - cijeli krug mora ostati unutar ploče.
>
> Dodaj unit testove za shape koji stane/ne stane, quantity=1, više redova/stupaca i rectangle slučaj gdje rotacija daje veći kapacitet.

## Prompt 14.3 – Triangle layout + `LayoutService` + capacity/requiredSheets

**Način: PLAN → CODE**

> Implementiraj prvi valjani layout za jednakostranične trokute. Kreni od jednostavne lako provjerljive varijante; alterniranje orijentacije dodaj samo ako ostaje mali i testabilan scope.
>
> Zatim uvedi `LayoutService`/strategy selector koji:
> - bira algoritam prema ShapeType
> - računa capacity per sheet
> - odbija capacity=0
> - računa `requiredSheets = ceil(quantity / capacity)`
> - vraća placements raspoređene po `SheetLayout` objektima.
>
> Unit test mora provjeriti da nijedna točka trokuta ni placement bilo kojeg shapea ne izlazi iz dopuštene ploče.

---

# 15. Batch G-code – ITERACIJA 2

## Prompt 15.1 – Multi-sheet output / persistence decision gate

**Način: ASK / AUDIT**

> Prije batch G-koda analiziraj slučaj `requiredSheets > 1`.
>
> Iteracija 1 sprema jedan `g_code CLOB` u `MACHINING_JOB`, što je dovoljno dok postoji jedan machine program po jobu. Nakon layouta jedan job može zahtijevati više `SheetLayout` programa, a zadnja ploča može imati drukčiji broj placementa.
>
> Ne guraj više odvojenih machine programa u jedan `.nc` string bez jasnog razloga.
>
> Predloži minimalne V1 opcije, npr.:
> 1. jedan `GCodeProgram` po `SheetLayout` i uvođenje `GCODE_PROGRAM` 1:N tablice tek sada kada je potreba stvarno nastala
> 2. druga jednostavna reprezentacija samo ako čuva jasnu vezu job → sheet → machine program i omogućuje ispravan save/reopen/export.
>
> Usporedi utjecaj na postojeći `MachiningJob.gCode`, SQL schema, repository i završni rad. Zaustavi se dok se odluka ne zapiše u `00_odluke.md`.

## Prompt 15.2 – ToolPath translation + batch generator

**Način: PLAN → CODE**

> Nakon odluke iz 15.1 proširi generator za jedan `SheetLayout` koristeći postojeću single-element logiku.
>
> Za svaki `PlacedShape`:
> - transliraj lokalni ToolPath na XY placement
> - ponovno koristi već provjerenu step-down/machining logiku
> - sigurno se vrati na safe Z između elemenata prema potvrđenoj profile konvenciji
> - ne računaj layout u GCodeGeneratoru.
>
> Dodaj test s dva jednostavna shapea na poznatim offsetima i test da generirane XY koordinate ostaju unutar sheet granica.
>
> Ako odluka 15.1 uvodi više GCodeProgram zapisa ili novu tablicu, u ovom milestoneu implementiraj samo potrebnu minimalnu schema/repository evoluciju i integration test; ne ostavljaj persistence model u nekonzistentnom stanju.
>
> Jasno dokumentiraj koji dio generatora je ponovno korišten iz single-element workflowa, a koji je novi i još NIJE TESTIRAN NA STROJU.

---

# 16. JavaFX UI – quantity i layout proširenje

## Prompt 16.1 – Batch UI milestone

**Način: PLAN → CODE**

> Proširi postojeći UI tek nakon što quantity/layout/batch service sloj stvarno postoji.
>
> Dodaj:
> - quantity input, cijeli broj > 0
> - capacity per sheet
> - required sheets
> - broj stvarno raspoređenih elemenata
> - ako postoji više SheetLayout/GCodeProgram rezultata, jasan odabir/pregled pojedine ploče/programa prema odluci iz 15.1.
>
> Workflow:
> `input -> validation -> quantity -> layout -> capacity/requiredSheets -> ToolPaths -> batch G-code -> preview`.
>
> Controller ne računa layout niti generira batch stringove.
>
> Quantity=1 mora ostati regresijski valjan slučaj.
>
> Grafički prikaz ploče nije obvezan za V1; nemoj ga uvoditi samo zato što bi izgledao atraktivno ako tekstualni rezultat zadovoljava funkcionalni zahtjev.

---

# 17. Integracija – ITERACIJA 2

## Prompt 17.1 – Batch end-to-end milestone

**Način: PLAN → CODE**

> Proširi composition root samo novim Iteracija 2 dependencyjima i provjeri cijeli tok:
>
> `input quantity -> validation -> layout -> capacity/requiredSheets -> translated ToolPaths -> G-code program(i) -> persistence -> reload`.
>
> Provjeri:
> - placements unutar sheet granica
> - sheet dimenzije unutar machine XY work area
> - svaki element koristi postojeću machining/step-down logiku
> - safe Z tranzicije između elemenata
> - quantity > 1 round-trip
> - prema odluci iz 15.1 svi machine programi/sheetovi se ponovno učitavaju ili deterministički regeneriraju na dokumentiran način.
>
> Dodaj integration test s malom quantity vrijednošću čiji expected rezultat možemo ručno provjeriti.

---

# 18. Batch test na ZK-1325

## Prompt 18.1 – Statički audit batch programa

**Način: ASK / AUDIT**

> Prije fizičkog batch testa auditiraj stvarno generirani program ili programe za malu quantity vrijednost.
>
> Provjeri:
> - placement koordinate
> - sheet granice
> - machine XY work area
> - safe Z između elemenata
> - step-down za svaki element
> - početak/kraj svakog programa
> - F/S/G54/Z profile ponašanje prema stvarno potvrđenim postavkama prvog testa
> - multi-sheet mapping ako postoji.
>
> Ne mijenjaj kod bez konkretnog pronađenog problema.

## Prompt 18.2 – Kontrolirani batch test

**Način: ASK / AUDIT**

> Nakon uspješnog single-element testa i statičkog batch audita pripremi test male količine koja se može vizualno i ručno provjeriti.
>
> Cilj:
> - placement
> - capacity per sheet
> - required sheets
> - sigurna tranzicija između elemenata
> - granice XY
> - ponašanje više programa/sheetova ako je primjenjivo.
>
> Test mora koristiti stvarno potvrđene alate i machining parametre te proceduru koju odobri operator. Ne proglašavaj layout optimalnim; mjeri samo rezultat implementiranog algoritma.

---

# 19. Finalni test report

## Prompt 19.1 – Finalni status IMPLEMENTIRANO / TESTIRANO

**Način: ASK / REPORT**

> Nakon što unesemo stvarne rezultate testova, sastavi tehnički sažetak implementacije koristeći samo kod i zabilježene rezultate.
>
> Za svaku funkcionalnost označi:
> - IMPLEMENTIRANO
> - TESTIRANO
> - NIJE TESTIRANO
> - BUDUĆI RAZVOJ.
>
> Posebno razdvoji:
> - geometriju
> - single-element G-code
> - single-element fizički test
> - quantity
> - layout
> - capacity / required sheets
> - batch G-code
> - persistence
> - multi-sheet program persistence ako postoji
> - `.nc` export
> - RichAuto A11 profil
> - batch fizički test.
>
> Ne izmišljaj mjerne rezultate.

---

# Dokumentacijska struktura projekta

Ova struktura nije dio Java package arhitekture, nego prati razvoj radi završnog rada:

```text
<project-root>
├── dokumentacija
│   └── biljeske
│       ├── 00_indeks.md
│       ├── 00_predlozak_biljeske.md
│       ├── 00_odluke.md
│       ├── 01-02-maven-projekt.md
│       ├── 03-01-domain-core.md
│       ├── ...
│       └── 19-01-finalni-test-report.md
├── src
├── pom.xml
└── AGENTS.md
```

Naziv pojedine bilješke treba sadržavati oznaku prompta kako bi se kasnije mogla izravno povezati s fazom implementacije.

---

# Potvrđena konačna package struktura

```text
hr.lukabosnjak
├── app
├── config
├── domain
│   ├── model
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

Napomena: `ui.view` kao Java package koristi se samo ako stvarno postoje Java view klase. Ako se odabere FXML, datoteke pripadaju `src/main/resources/hr/lukabosnjak/ui/view/` ili drugom dokumentiranom resource pathu. `layout` se funkcionalno koristi tek nakon GATE 1.

---

# Planirane ključne klase

## `domain.model`

### `MaterialType`

```text
Long materialTypeId
String name
String description
LocalDateTime createdAt
LocalDateTime updatedAt
LocalDateTime deletedAt
```

### `MaterialSheet`

```text
Long materialSheetId
MaterialType / materialType reference
double width
double height
double thickness
LocalDateTime createdAt
LocalDateTime updatedAt
```

### `CncMachine`

```text
Long cncMachineId
String name
String manufacturer
String model
String controller
double workAreaX
double workAreaY
double workAreaZ
double maxFeedRate
double minSpindleSpeed
double maxSpindleSpeed
LocalDateTime createdAt
LocalDateTime updatedAt
```

### `Tool`

```text
Long toolId
CncMachine / cncMachine reference
int toolNumber
String name
ToolType ili String – zaključati tek nakon potvrde vrijednosti
double diameter
double cuttingLength
int fluteCount
boolean active
LocalDateTime createdAt
LocalDateTime updatedAt
```

### `MachiningParameters`

```text
Long machiningParametersId
double spindleSpeed
double feedRate
double plungeRate
double cutDepth
double stepDown
double safeZ
```

### `Shape`

```text
Long shapeId
ShapeType shapeType
ShapeSubtype shapeSubtype
Double dimensionA
Double dimensionB
Double dimensionC
LocalDateTime createdAt
LocalDateTime updatedAt
LocalDateTime deletedAt
```

### `Role`

```text
Long roleId
String name
String description
```

### `User`

```text
Long userId
Role / role reference
String username
String passwordHash
String firstName
String lastName
boolean active
LocalDateTime createdAt
LocalDateTime updatedAt
```

### `MachiningJob`

```text
Long machiningJobId
User createdBy
CncMachine cncMachine
Tool tool
MaterialSheet materialSheet
MachiningParameters machiningParameters
Shape shape
String name
int quantity
String gCode
LocalDateTime createdAt
LocalDateTime updatedAt
```

## `geometry`

Planirane support klase, ako se kroz implementaciju potvrdi da su dovoljne:

```text
Point2
ToolPath
PathSegment
LineSegment
ArcSegment
ToolPathService
SquareToolPathGenerator
RectangleToolPathGenerator
TriangleToolPathGenerator
CircleToolPathGenerator
```

## `layout` – ITERACIJA 2, tek nakon GATE 1

```text
LayoutSettings
PlacedShape
SheetLayout
LayoutResult
LayoutService
SquareRectangleGridLayoutStrategy
CircleGridLayoutStrategy
TriangleLayoutStrategy
```

Nazivi strategy klasa se mogu prilagoditi nakon što vidimo koliko će algoritam stvarno biti generičan. Ne stvarati interface/klasu samo radi patterna ako ne donosi čitljivost.

## `gcode`

Single-element generator pripada Iteraciji 1; batch proširenje tek Iteraciji 2.

```text
GCodeProgram
GCodeGenerator
RichAutoA11Profile
RichAutoA11GCodeGenerator
GCodeFormatter
NcFileExportService
```

## `service`

Moguće klase, samo ako odgovaraju stvarno implementiranom toku:

```text
ProgramGenerationService
JobPersistenceService
ToolPathService
LayoutService
```

Ne stvarati jedan `ApplicationService` koji radi sve. `service.dto` koristiti samo za request/result modele koji stvarno pojednostavljuju UI → service granicu; ne duplicirati domain modele bez potrebe.

---

# SQL V1 – ciljana struktura

Tablice:

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

Ključne korekcije:

```text
APP_USER umjesto USER

MACHINING_JOB.g_code CLOB

UNIQUE(TOOL.cnc_machine_id, TOOL.tool_number)

UNIQUE(MACHINING_JOB.material_sheet_id)
UNIQUE(MACHINING_JOB.machining_parameters_id)
UNIQUE(MACHINING_JOB.shape_id)
```

`GCODE_PROGRAM` se u Iteraciji 1 ne uvodi kao zasebna SQL tablica jer postoji jedan spremljeni output po jobu. U Iteraciji 2, nakon što layout može vratiti `requiredSheets > 1`, obvezan je decision gate 15.1. Ako jedan job stvarno treba više zasebnih machine programa, tada se model i schema smiju proširiti s `GCODE_PROGRAM` 1:N umjesto spremanja više programa u jedan nejasan CLOB.

---

# Redoslijed milestoneova

Za **svaki milestone** vrijedi dodatni kriterij: odgovarajuće bilješke u `dokumentacija/biljeske/` moraju biti ažurirane i moraju odgovarati stvarnom kodu/testovima.

## Milestone A – projekt se pokreće

Završeno kada:

- Maven build prolazi
- JavaFX minimalni prozor se otvara
- stvarna JDK verzija je zapisana.

## Milestone B – domain model postoji

Završeno kada:

- potvrđene domain klase postoje
- enumovi postoje samo za potvrđene vrijednosti
- nema JavaFX/JDBC ovisnosti u domainu
- `quantity` može postojati u konačnom modelu, ali korisnički workflow prve iteracije koristi samo vrijednost 1.

## Milestone C – H2 persistence radi

Završeno kada:

- schema se kreira na praznoj H2 bazi
- first-run initializer može inicijalizirati novu file-based bazu
- repository integration testovi prolaze
- single-element `MachiningJob` se može save/loadati
- persistence nije kriterij kojim se proglašava RichAuto kompatibilnost.

## Milestone D – geometrija i validacija za jedan element rade

Završeno kada:

- sva 4 oblika daju očekivani ToolPath
- nevaljani unosi se odbijaju
- geometrija ne proizvodi G-code tekst
- jedan oblik se može provjeriti prema dimenzijama ploče
- ToolPath bounds i machine XY work-area provjera postoje bez layout logike.

## Milestone E – single-element G-code radi softverski

Završeno kada:

- generator radi za jedan oblik
- step-down radi
- `.nc` export radi
- postoje string/unit/integration testovi
- quantity/layout/batch još nisu implementirani u korisničkom workflowu
- status je IMPLEMENTIRANO, ali još ne TESTIRANO NA STROJU.

## Milestone F – single-element UI i integracija rade

Završeno kada:

- korisnik može odabrati jedan oblik i njegove dimenzije
- može unijeti materijal i machining parametre
- može generirati i pregledati G-code
- može spremiti/otvoriti program prema stvarno implementiranom persistence workflowu
- može izvesti `.nc`
- nema quantity/layout UI-a
- controller nema poslovnu logiku.

## Milestone G – GATE 1: prvi stvarni test

Završeno kada:

- postavke konkretnog RichAuto A11 relevantne za generator su zabilježene
- single-element kontrolirani test je stvarno proveden
- stvarni test jednog trokuta je proveden ako su uvjeti sigurni i operator ga odobri
- rezultat je zapisan bez izmišljanja
- eventualne korekcije generatora su ponovno softverski provjerene
- donesena je odluka smije li projekt prijeći na quantity/layout.

**Bez ovog milestonea ne počinje Iteracija 2.**

## Milestone H – quantity i layout rade softverski

Završeno kada:

- korisnik može zadati quantity > 0
- capacity per sheet radi
- required sheets radi
- placements su unutar ploče
- algoritam je deterministički i testiran
- nema tvrdnje matematičke optimalnosti.

## Milestone I – batch G-code i prošireni UI rade

Završeno kada:

- već provjereni single-element ToolPath/generation pristup koristi se za više placementa
- batch G-code se generira po SheetLayoutu prema odluci iz 15.1
- multi-sheet output/persistence ima eksplicitno dokumentiran model ako `requiredSheets > 1`
- safe Z se koristi između elemenata
- UI prikazuje quantity, capacity i requiredSheets
- integration test za više elemenata prolazi
- batch dio je IMPLEMENTIRAN, ali fizički TESTIRAN tek nakon stvarnog batch testa.

## Milestone J – batch test i završni izvještaj

Završeno kada:

- batch program prođe statički audit
- fizički batch test je proveden ako je sigurno i potrebno
- stvarni rezultati su zabilježeni
- konačni izvještaj jasno razlikuje IMPLEMENTIRANO, TESTIRANO, NIJE TESTIRANO i BUDUĆI RAZVOJ.

---

# Web-provjerene tehničke napomene korištene za ovaj plan

1. OpenAI Codex best practices preporučuju **Ask/Plan prvenstveno za velike promjene**, a ne kao obvezan dodatni turn za svaku malu izmjenu. Dobro ograničen zadatak tipično treba imati jasan issue-like scope, očekivane datoteke i provjeru rezultata.
2. OpenAI navodi da Codex usage nije fiksan „po promptu“: ovisi o veličini i složenosti zadatka, modelu, kontekstu, mjestu izvršavanja i alatima. Zato grupiranje šest jednostavnih POJO klasa može smanjiti nepotrebni overhead, ali jedan nekontrolirano velik zadatak može potrošiti više od nekoliko malih.
3. OpenAI preporučuje `AGENTS.md` za trajni repo kontekst. Novije agent-first smjernice dodatno upozoravaju da golemi instruction fileovi troše kontekst; bolje je dati agentu mapu i indeksirane projektne dokumente nego kopirati cijeli projektni opis u svaki prompt.
4. OpenJFX službene Maven upute potvrđuju da Maven može dohvatiti JavaFX module i platformske native dependencyje te da `javafx.fxml` treba dodati tek ako aplikacija stvarno koristi FXML. Non-modularni JavaFX projekt ne mora imati `module-info.java` samo zato što koristi Maven.
5. H2 službena dokumentacija potvrđuje embedded JDBC način rada, file-based i in-memory baze, transakcije i H2 Console. Relativni file path računa se od current working directoryja, zato path mora biti svjesno odabran i dokumentiran.
6. H2 `USER` je keyword, zato SQL tablica ostaje `APP_USER`.
7. RichAuto A11 dokumentacija pokazuje da se `F`, `S` i `G54` mogu postaviti na read/ignore. Službeni RichAuto materijali za A1X obitelj navode G00/G01/G02/G03, G17, G21, G54–G59, G90/G91 te M03/M05/M30, ali konkretno ponašanje našeg ZK-1325/A11 mora biti potvrđeno fizičkim testom.
8. Z-smjer, work zero, safe Z i stvarni machining parametri ne smiju se zaključiti samo iz općeg G-code primjera. Interni model može koristiti pozitivne veličine, dok generator mora imati eksplicitnu konvenciju pretvorbe u strojne koordinate koja se potvrđuje prije rezanja.
9. Iteracija 2 uvodi novi arhitektonski rizik: `requiredSheets > 1` može značiti više zasebnih machine programa. Zato se `GCODE_PROGRAM` ne uvodi prerano, ali se obvezno ponovno razmatra u decision gateu 15.1 kada potreba postane stvarna.

Korišteni službeni/primarni izvori za provjeru plana:

- OpenAI – How OpenAI uses Codex: https://openai.com/business/guides-and-resources/how-openai-uses-codex/
- OpenAI Help – Using Codex with your ChatGPT plan: https://help.openai.com/en/articles/11369540/
- OpenAI – Harness engineering: https://openai.com/index/harness-engineering/
- OpenJFX Maven documentation: https://openjfx.io/openjfx-docs/maven
- OpenJFX Getting Started: https://openjfx.io/openjfx-docs/
- H2 Features: https://h2database.github.io/html/features.html
- H2 Quickstart: https://h2database.github.io/html/quickstart.html
- H2 Keywords: https://h2database.github.io/html/advanced.html#keywords
- RichAuto official manual/download material: https://www.richnc.com.cn/download/file/135

---

# Što NE raditi

- Ne koristiti zaseban Plan turn za svaki getter/setter/POJO samo zato što dokument ima broj prompta.
- Ne spajati previše nepovezanih slojeva u jedan golemi Codex zadatak samo radi uštede usagea.
- Ne pretpostavljati da broj promptova linearno odgovara Codex potrošnji; složenost i kontekst su važni.
- Ne pretpostavljati da globalni `mvn` mora postojati ako projekt radi preko IntelliJ Mavena ili Maven Wrappera.
- Ne tražiti od Codexa: „napravi cijelu aplikaciju“.
- Ne generirati UI prije poslovne logike.
- Ne pisati SQL u controlleru.
- Ne raditi geometriju unutar GCodeGeneratora.
- Ne računati nesting u GCodeGeneratoru.
- Ne stavljati H2 Connection u domain klase.
- Ne uvoditi Spring/Hibernate samo zato što postoje.
- Ne uvoditi `Part`.
- Ne implementirati pseudojezik.
- Ne implementirati AI shape generator.
- Ne izmišljati ToolType vrijednosti.
- Ne izmišljati machining parametre.
- Ne hardkodirati RichAuto F/S/G54 ponašanje prije provjere.
- Ne tvrditi da je layout matematički optimalan.
- Ne označiti RichAuto kompatibilnost kao TESTIRANU prije stvarnog testa.
- Ne implementirati korisnički quantity, layout, capacity, requiredSheets ni batch G-code prije prolaska kroz GATE 1.
- Ako prvi single-element test otkrije problem u osnovnom generatoru, ne zaobilaziti ga prelaskom na Iteraciju 2.
- Ne preskakati ažuriranje `dokumentacija/biljeske/` nakon implementacijskih promjena.
- Ne puniti bilješke trivijalnim getterima/setterima i boilerplateom samo da bi postojao isječak koda.
- Ne koristiti bilješke kao dokaz TESTIRANOG stanja ako test nije stvarno izvršen.
- Ne prelaziti na sljedeći prompt dok prethodni korak nije razumljiv, provjeren i dokumentiran.
