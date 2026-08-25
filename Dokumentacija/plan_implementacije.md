# Plan implementacije aplikacije – baby-step promptovi za Codex

## Svrha dokumenta

Ovaj dokument je izvedbeni plan za razvoj aplikacije završnog rada **„Razvoj aplikacije za optimiziranje rada na CNC stroju“**. Plan je složen tako da se aplikacija ne pokuša izgraditi u nekoliko velikih AI promptova, nego kroz male, razumljive i provjerljive korake.

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

## 0.7. Još uvijek otvorene odluke

Ove stavke se ne smiju izmišljati:

- točna instalirana verzija JDK-a
- konačni base package projekta
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

## A. Plan/Ask Mode

Svaki numerirani prompt prvo koristi u **Plan/Ask modu**.

Codex mora prije bilo kakve promjene:

1. pročitati `AGENTS.md`
2. koristiti project-specific Skill ako postoji
3. pregledati trenutačno stanje repozitorija
4. objasniti što je već implementirano
5. predložiti mali plan samo za taj prompt
6. navesti koje će datoteke mijenjati
7. navesti kako će rezultat biti provjeren
8. upozoriti ako prompt ovisi o odluci koja još nije potvrđena
9. **ne pisati kod dok plan ne odobriš**

## B. Universal follow-up nakon odobrenja plana

Nakon što razumiješ Codexov plan, prebaci se u Code Mode i koristi:

> Implementiraj samo prethodno odobreni plan. Ne prelazi na sljedeći korak. Ne dodaj funkcionalnosti koje nisu dio ovog zadatka. Nakon promjena pokreni najrelevantniji build/test, pokaži rezultat, navedi sve promijenjene datoteke i objasni mi što je napravljeno kao studentu koji mora moći obraniti kod pred mentorom. Ako test ne prolazi, prvo objasni uzrok i popravi samo ono što pripada ovom koraku.
>
> Prije završnog odgovora obavezno ažuriraj dokumentacijski dnevnik u `dokumentacija/biljeske/` prema pravilima iz `AGENTS.md` i project-specific Skilla. Bilješka mora zabilježiti što je promijenjeno, zašto, kako je provjereno, što još nije testirano te izdvojiti samo stvarno važne kandidate za isječke koda za završni rad. Ako u ovom koraku nema smislenog isječka koda, napiši da ga nema umjesto da ubacuješ boilerplate. Ažuriraj i `dokumentacija/biljeske/00_indeks.md`.
>
> Na kraju stani.

## C. Pravilo baby-step rada

Ne šalji dva ili tri prompta odjednom. Nakon svakog:

- pregledaj što je Codex napravio
- pokreni aplikaciju/test kada je primjenjivo
- pitaj ako ti neka klasa ili linija nije jasna
- tek tada nastavi na sljedeći prompt

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
dokumentacija/biljeske/03-03-cnc-machine.md
dokumentacija/biljeske/08-05-toolpath-trokut.md
dokumentacija/biljeske/09-03-grid-layout.md
dokumentacija/biljeske/10-04-linearni-gcode.md
```

### Što se zapisuje nakon svakog implementacijskog prompta

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

## Prompt 2.1 – Dizajn paketa bez stvaranja klasa

> PLAN MODE. Na temelju dogovorene arhitekture predloži package strukturu ispod odabranog base packagea.
>
> Želim jasno odvojiti najmanje:
> - `app`
> - `config`
> - `domain`
> - `domain.enums`
> - `validation`
> - `geometry`
> - `layout`
> - `gcode`
> - `service`
> - `persistence.repository`
> - `persistence.jdbc`
> - `ui`
> - `ui.controller`
> - eventualno `exception` samo ako stvarno ima smisla.
>
> Prije bilo kakvog stvaranja direktorija objasni:
> - odgovornost svakog paketa
> - koje pakete smije pozivati UI
> - zašto domain ne smije ovisiti o JavaFX-u ili JDBC-u
> - zašto gcode ne smije spremati u bazu.

## Prompt 2.2 – Kreiranje package kostura

> PLAN MODE. Kreiraj samo prethodno odobrenu package strukturu.
>
> Ako Java ne dopušta prazan package bez datoteke, nemoj stvarati beskorisne placeholder klase samo radi direktorija. Kreiraj samo ono što je potrebno i objasni kako će se paketi pojavljivati kako budemo dodavali klase.
>
> Ne implementiraj još domenske klase.

---

# 3. Domain / model klase

Napomena: zbog zadanog redoslijeda prvo gradimo klase koje ne ovise o još-nepostojećim enumovima. `Shape`, `Tool` i završni `MachiningJob` dovršavaju se nakon koraka s enumovima.

## Prompt 3.1 – `MaterialType`

> PLAN MODE. Implementiraj samo domensku klasu `MaterialType`.
>
> Potvrđeni podaci:
> - `Long materialTypeId`
> - `String name`
> - `String description` – nullable
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
> - `LocalDateTime deletedAt` – nullable
>
> Ne dodaj JPA anotacije jer koristimo JDBC/H2, ne ORM.
>
> Prije implementacije objasni:
> - zašto je ID `Long`
> - zašto je `deletedAt` nullable
> - hoćemo li koristiti konstruktor + gettere/settere ili drugi jednostavan pristup prikladan JDBC mapiranju.
>
> Dodaj samo minimalne unit testove ako postoji stvarna logika za testiranje; nemoj pisati besmislene getter testove.

## Prompt 3.2 – `MaterialSheet`

> PLAN MODE. Implementiraj samo `MaterialSheet`.
>
> Potvrđeni podaci:
> - `Long materialSheetId`
> - veza prema `MaterialType`
> - `double width`
> - `double height`
> - `double thickness`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> Sve tri dimenzije u valjanom stanju moraju biti > 0, ali nemoj još gurati kompletnu UI validaciju u ovu klasu.
>
> Prije implementacije objasni je li za domenski model čišće imati `MaterialType materialType` ili samo `Long materialTypeId`, uzimajući u obzir da persistence radimo ručnim JDBC-om. Odaberi jednostavniju opciju koju možemo jasno obraniti.

## Prompt 3.3 – `CncMachine`

> PLAN MODE. Implementiraj samo `CncMachine`.
>
> Potvrđeni atributi:
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
> Ne hardkodiraj još ZK-1325 vrijednosti unutar klase. One pripadaju podacima baze/configu, ne source kodu.
>
> Ne tvrdi da su radne osi fizičkog stroja već provjerene.

## Prompt 3.4 – `MachiningParameters`

> PLAN MODE. Implementiraj samo `MachiningParameters`.
>
> Potvrđeni atributi:
> - `Long machiningParametersId`
> - `double spindleSpeed`
> - `double feedRate`
> - `double plungeRate`
> - `double cutDepth`
> - `double stepDown`
> - `double safeZ`
>
> Sve vrijednosti za valjan machining job trebaju biti > 0.
>
> Posebno objasni:
> - razliku `cutDepth` i debljine `MaterialSheet`
> - svrhu `stepDown`
> - zašto `safeZ` pripada machining parametrima, a ne `MaterialType`.
>
> Ne generiraj još G-kod.

## Prompt 3.5 – `Role`

> PLAN MODE. Implementiraj samo klasu `Role`.
>
> Potvrđeni atributi:
> - `Long roleId`
> - `String name`
> - `String description` – nullable
>
> Nemoj još uvoditi `RoleType` enum ni prava ADMIN/ENGINEER/CNC_OPERATOR kao implementiranu funkcionalnost, jer autentikacija/RBAC nije potvrđena funkcionalnim zahtjevima.
>
> Klasa treba biti spremna za JDBC mapiranje, ali bez autentikacijske logike.

## Prompt 3.6 – `User`

> PLAN MODE. Implementiraj samo Java domensku klasu `User`. SQL tablica će se kasnije zvati `APP_USER` jer je `USER` H2 keyword.
>
> Potvrđeni atributi:
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
> Ne implementiraj login, password hashing algoritam, session management niti autorizaciju u ovom koraku.
>
> Objasni zašto `passwordHash` nije isto što i lozinka u čistom tekstu.

---

# 4. Enumovi i dovršavanje modela

## Prompt 4.1 – `ShapeType` i `ShapeSubtype`

> PLAN MODE. Implementiraj potvrđene enumove za geometriju.
>
> `ShapeType` mora pokriti V1:
> - SQUARE
> - RECTANGLE
> - CIRCLE
> - TRIANGLE
>
> Za `ShapeSubtype` u V1 imamo potvrđen samo koncept jednakostraničnog trokuta. Ne izmišljaj druge podtipove. Predloži najjednostavniji način da `EQUILATERAL` postoji bez stvaranja bespotrebnih vrijednosti.
>
> Objasni zašto ne koristimo Stringove za sve shape tipove.

## Prompt 4.2 – `Shape`

> PLAN MODE. Implementiraj samo `Shape` entitet.
>
> Potvrđeni atributi:
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
> - TRIANGLE + EQUILATERAL: A = stranica
>
> Nemoj kreirati `Square`, `Rectangle`, `Circle` i `Triangle` kao zasebne persistence entitete. Specifičnu geometrijsku logiku ćemo kasnije odvojiti u geometry generatore.
>
> UI kasnije mora prikazivati semantičke nazive polja, a ne `dimensionA`.

## Prompt 4.3 – `ToolType` decision gate i `Tool`

> PLAN MODE. Prije implementacije `Tool` klase provjeri postoji li u projektu potvrđen popis vrijednosti `ToolType`.
>
> Ako vrijednosti nisu potvrđene:
> - nemoj ih izmišljati
> - pokaži mi koje informacije trebamo od stvarnog ZK-1325 alata
> - predloži možemo li privremeno modelirati `type` kao `String` bez narušavanja arhitekture.
>
> Nakon odluke, `Tool` treba imati:
> - `Long toolId`
> - vezu prema `CncMachine`
> - `int toolNumber`
> - `String name`
> - `type` prema potvrđenoj odluci
> - `double diameter`
> - `double cuttingLength`
> - `int fluteCount`
> - `boolean active`
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> Nemoj hardkodirati stvarne alate koje još nismo izmjerili/potvrdili.

## Prompt 4.4 – `MachiningJob`

> PLAN MODE. Sada kada postoje ostali osnovni modeli, implementiraj `MachiningJob`.
>
> Potvrđeni podaci/veze:
> - `Long machiningJobId`
> - created-by `User`
> - `CncMachine`
> - `Tool`
> - `MaterialSheet`
> - `MachiningParameters`
> - `Shape`
> - `String name`
> - `int quantity` – polje ostaje dio konačnog modela; tijekom Iteracije 1 koristi se vrijednost 1 i još nema korisničkog unosa količine
> - `String gCode` kao V1 representation spremljenog programa; SQL će biti CLOB
> - `LocalDateTime createdAt`
> - `LocalDateTime updatedAt`
>
> `quantity` mora u valjanom nalogu biti > 0. U Iteraciji 1 workflow radi samo s `quantity = 1`; korisnički unos količine uvodi se tek nakon GATE 1.
>
> Nemoj u ovoj klasi implementirati SQL ni generiranje G-koda.
>
> Objasni zašto je `MachiningJob` agregat koji povezuje podatke jednog posla.

---

# 5. H2 konfiguracija

## Prompt 5.1 – Dodavanje H2 dependencyja

> PLAN MODE. Dodaj H2 kao Maven dependency koristeći aktualnu stabilnu verziju provjerenu iz službene H2 dokumentacije/Maven repozitorija.
>
> Nemoj instalirati zaseban ORM.
>
> Objasni:
> - što znači embedded H2
> - zašto je JDBC dovoljan za ovaj projekt
> - gdje će se fizički spremati baza
> - zašto je H2 Console razvojni alat, a ne dio poslovne logike.

## Prompt 5.2 – `DatabaseConfig` / Connection factory

> PLAN MODE. Implementiraj minimalnu centraliziranu konfiguraciju JDBC veze.
>
> Želim:
> - jedno mjesto za JDBC URL
> - user/password konfiguraciju
> - metodu za dobivanje `Connection`
> - try-with-resources u kodu koji koristi Connection
> - bez globalno otvorene connection varijable
> - bez SQL-a u JavaFX controlleru.
>
> Za file-based embedded bazu odaberi jednostavan projektni path i objasni posljedice relativnog patha.
>
> Ne kreiraj još tablice.

## Prompt 5.3 – H2 connection smoke test i Console

> PLAN MODE. Dodaj mali razvojni smoke test koji potvrđuje da aplikacija može otvoriti i zatvoriti H2 JDBC vezu.
>
> Nakon toga predloži najjednostavniji način pokretanja H2 Console za razvoj i pregled podataka.
>
> Console ne smije postati obvezna runtime komponenta aplikacije.
>
> Ne piši još finalni DDL.

---

# 6. SQL schema

Prije DDL-a treba zaključati nekoliko korekcija koje su ranije identificirane.

## Prompt 6.1 – Audit modela prije DDL-a

> PLAN MODE. Ne piši SQL još.
>
> Usporedi postojeće Java modele i konačni ER/relacijski model te provjeri sljedeće:
> - SQL tablica mora biti `APP_USER`, ne `USER`
> - `safe_z` postoji samo u `MACHINING_PARAMETERS`
> - `MACHINING_JOB` ima `shape_id`
> - `MACHINING_JOB` ima `g_code CLOB`
> - `UNIQUE(cnc_machine_id, tool_number)`
> - `MATERIAL_SHEET`, `MACHINING_PARAMETERS` i `SHAPE` su 1:1 prema jobu u V1 i zato odgovarajući FK-ovi u jobu trebaju UNIQUE
> - svi BIGINT PK-ovi imaju jasno definiranu identity strategiju
> - Tool pripada CncMachineu
> - job sadrži i machine i tool te će poslovna logika provjeravati njihovu usklađenost.
>
> Prikaži mi samo audit tablicu: stavka / stanje / potrebna korekcija.

## Prompt 6.2 – User/Role scope gate

> PLAN MODE. Prije konačnog DDL-a analiziraj problem:
> baza sadrži `ROLE`, `APP_USER` i `MACHINING_JOB.created_by_user_id`, ali funkcionalni zahtjevi još ne definiraju login ni RBAC.
>
> Nemoj implementirati autentikaciju.
>
> Prikaži mi dvije minimalne opcije za V1:
> 1. korisnički model ostaje u bazi, ali bez login UI-ja
> 2. autentikacija se službeno dodaje u scope kao novi funkcionalni zahtjev.
>
> Usporedi utjecaj na završni rad i količinu implementacije. Zaustavi se i čekaj moju odluku prije konačnog DDL-a ako odluka još nije evidentirana u repozitoriju.

## Prompt 6.3 – `schema.sql`, prvi dio

> PLAN MODE. Kreiraj prvi dio `schema.sql` samo za stabilne/roditeljske tablice:
> - ROLE
> - APP_USER
> - MATERIAL_TYPE
> - CNC_MACHINE
>
> Koristi H2 syntax kompatibilan s verzijom dependencyja.
>
> PK:
> - `BIGINT GENERATED ... AS IDENTITY` ili odgovarajuću standardnu H2 identity sintaksu koju prvo provjeri u dokumentaciji.
>
> Dodaj:
> - NOT NULL
> - UNIQUE gdje je potvrđeno
> - FK `APP_USER.role_id -> ROLE.role_id`.
>
> Nemoj još kreirati ostale tablice.

## Prompt 6.4 – `schema.sql`, drugi dio

> PLAN MODE. Proširi postojeći `schema.sql` samo s:
> - TOOL
> - MATERIAL_SHEET
> - MACHINING_PARAMETERS
> - SHAPE
>
> Obavezno:
> - Tool FK na CNC_MACHINE
> - `UNIQUE(cnc_machine_id, tool_number)`
> - MaterialSheet FK na MATERIAL_TYPE
> - `deleted_at` samo tamo gdje je model predvidio
> - enum vrijednosti pohranjuj kao VARCHAR, bez H2-specifičnog ENUM tipa, kako Java enum mapping ostane jednostavan.
>
> Ne kreiraj MACHINING_JOB u ovom promptu.

## Prompt 6.5 – `MACHINING_JOB` i schema integration test

> PLAN MODE. Dodaj `MACHINING_JOB` u `schema.sql`.
>
> Atributi:
> - identity PK
> - FK prema APP_USER prema potvrđenoj odluci
> - FK `cnc_machine_id`
> - FK `tool_id`
> - FK `material_sheet_id`
> - FK `machining_parameters_id`
> - FK `shape_id`
> - `name VARCHAR NOT NULL`
> - `quantity INTEGER NOT NULL` – u Iteraciji 1 sprema se vrijednost 1; korisnički unos količine dolazi tek u Iteraciji 2
> - `g_code CLOB`
> - timestamps
>
> Za V1 dodaj UNIQUE na FK-ove koji predstavljaju stvarne 1:1 veze:
> - material_sheet_id
> - machining_parameters_id
> - shape_id
>
> Dodaj CHECK gdje je H2 rješenje jednostavno i jasno, npr. `quantity > 0`, ali nemoj duplirati svu poslovnu logiku u SQL.
>
> Nakon implementacije napravi integration test koji pokrene schema na čistoj in-memory H2 bazi i potvrdi da se sve tablice mogu kreirati.

---

# 7. Repository / DAO sloj

Pravilo za cijeli sloj:

- JDBC
- PreparedStatement
- try-with-resources
- SQL ostaje u persistence sloju
- controller nikada ne dobiva Connection
- iznimke se pretvaraju u jasne persistence iznimke ili propagiraju na kontroliran način

## Prompt 7.1 – Repository konvencije

> PLAN MODE. Prije implementacije DAO-a predloži konvenciju za repository sloj.
>
> Odluči:
> - interface u `persistence.repository`
> - JDBC implementacija u `persistence.jdbc`
> - naming: `MaterialTypeRepository` + `JdbcMaterialTypeRepository`
> - koje osnovne metode stvarno trebamo.
>
> Nemoj uvoditi generički mega-`CrudRepository` ako će više zakomplicirati studentski projekt.
>
> Za V1 tipične metode su `save`, `findById`, `findAll`, a `delete` samo gdje je funkcionalno potreban.

## Prompt 7.2 – `MaterialTypeRepository` i `CncMachineRepository`

> PLAN MODE. Implementiraj repository interface i JDBC implementaciju samo za:
> - MaterialType
> - CncMachine
>
> Koristi PreparedStatement i eksplicitno mapiranje `ResultSet -> domain`.
>
> Dodaj H2 integration testove za:
> - insert
> - findById
> - findAll.
>
> Ne implementiraj Tool niti job u ovom promptu.

## Prompt 7.3 – `ToolRepository`

> PLAN MODE. Implementiraj `ToolRepository` i `JdbcToolRepository`.
>
> Minimalne potrebe:
> - save
> - findById
> - findAllByMachineId
> - po potrebi findByMachineIdAndToolNumber.
>
> Testiraj da DB odbija dupli `tool_number` na istom stroju, ali dopušta isti broj alata na drugom stroju.
>
> Ne implementiraj machining job.

## Prompt 7.4 – Snapshot repositoryji

> PLAN MODE. Implementiraj repository sloj za:
> - MaterialSheet
> - MachiningParameters
> - Shape
>
> Ove zapise u V1 tretiramo kao podatke konkretnog posla/snapshot, ne kao globalne kataloge koje korisnik stalno uređuje.
>
> Za svaki repository napravi samo metode koje su potrebne za save/load job workflow.
>
> Dodaj integration test barem za round-trip spremanje i čitanje svakog tipa.

## Prompt 7.5 – `MachiningJobRepository`

> PLAN MODE. Implementiraj `MachiningJobRepository` i JDBC implementaciju.
>
> Ovo je važan korak. Spremanje kompletnog joba mora biti transakcijsko:
> - shape
> - material sheet
> - machining parameters
> - machining job s `g_code`.
>
> Ako jedan insert ne uspije, ne smije ostati pola spremljenog joba.
>
> Minimalne metode:
> - save
> - findById
> - findAll
> - eventualno update ako je stvarno potreban za quick access.
>
> Prije koda mi objasni gdje počinje i završava transakcija.

---

# 8. Service / poslovna logika + Geometry/ToolPath

Ovaj korak uključuje i Geometry/ToolPath jer je taj sloj potvrđen arhitekturom, a mora postojati prije layouta i G-code generatora.

## Prompt 8.1 – Model validacije

> PLAN MODE. Dizajniraj minimalni validation pristup bez frameworka.
>
> Želim jasne poruke poput:
> - „Duljina stranice mora biti veća od 0 mm.“
> - „Količina mora biti cijeli broj veći od 0.“
>
> Predloži jednostavan `ValidationResult` ili kontrolirane exceptione. Nemoj stvarati kompleksan validation framework.
>
> Validation ne smije biti u JavaFX controlleru.

## Prompt 8.2 – Validatori domenskih podataka

> PLAN MODE. Implementiraj zasebnu validaciju za:
> - Shape
> - MaterialSheet
> - MachiningParameters
> - MachiningJob quantity – model-level pravilo; tijekom Iteracije 1 praktično se provjerava fiksna vrijednost 1, a korisnički unos dolazi tek u Iteraciji 2.
>
> Shape pravila:
> - Square: A > 0
> - Rectangle: A > 0 i B > 0
> - Circle: A(promjer) > 0
> - Triangle/EQUILATERAL: A > 0
>
> Material:
> - width, height, thickness > 0.
>
> Machining:
> - spindleSpeed, feedRate, plungeRate, cutDepth, stepDown, safeZ > 0.
>
> Dodaj unit testove za normalne, granične i nevaljane vrijednosti.

## Prompt 8.3 – Usklađivanje Machine ↔ Tool i machine limits

> PLAN MODE. Implementiraj poslovnu provjeru:
> - odabrani Tool mora pripadati odabranom CncMachine
> - tool mora biti active
> - feedRate ne smije prelaziti machine maxFeedRate
> - spindleSpeed mora biti unutar podržanog raspona stroja.
>
> Ne izmišljaj vrijednosti konkretnog stroja; provjera radi s podacima iz objekta/baze.
>
> Dodaj unit testove.

## Prompt 8.4 – Geometry primitives

> PLAN MODE. Kreiraj minimalni model geometrijske putanje koji nije vezan uz JavaFX i nije G-kod.
>
> Potrebe:
> - vlastita 2D točka, npr. `Point2`
> - `ToolPath`
> - reprezentacija line segmenta
> - reprezentacija arc segmenta ako je potrebna za krug
> - putanja mora se moći translirati za layout offset.
>
> Nemoj koristiti JavaFX `Point2D` u domain/geometry sloju jer geometrija ne treba ovisiti o UI frameworku.
>
> Prije implementacije nacrtaj mi mali primjer: trokut A→B→C→A kao ToolPath.

## Prompt 8.5 – ToolPath generatori za Square/Rectangle/Triangle

> PLAN MODE. Implementiraj ToolPath generiranje samo za:
> - Square
> - Rectangle
> - Equilateral Triangle.
>
> Lokalni koordinatni sustav neka polazi od dogovorene lokalne točke oblika, npr. `(0,0)`.
>
> Za jednakostranični trokut koristi:
> - A(0,0)
> - B(a,0)
> - C(a/2, sqrt(3)/2 * a)
> - povratak na A.
>
> Ne generiraj G01 stringove.
>
> Unit test mora provjeriti numeričke koordinate s tolerancijom.

## Prompt 8.6 – Circle ToolPath i `ToolPathService`

> PLAN MODE. Implementiraj geometrijsku putanju kruga odvojeno od G-koda.
>
> RichAuto obitelj podržava kružnu interpolaciju G02/G03 prema dokumentaciji, ali stvarno ponašanje našeg A11 mora biti testirano. Zato ToolPath treba predstavljati kružnu geometriju bez vezanja uz konačni output format.
>
> Nakon toga implementiraj `ToolPathService` koji na temelju `ShapeType` poziva odgovarajući geometry generator.
>
> Dodaj testove za sve 4 V1 geometrije.
>
> Ne generiraj još RichAuto G-code.

---


# 9. RichAuto A11 G-code generator – ITERACIJA 1

RichAuto dokumentacija za A11/A11plus obitelj navodi standardne naredbe poput G00, G01, G02, G03, G17, G21, G54–G59, G90/G91 te M03/M05/M30. Posebno je važno da kontroler ima postavke kojima se `F`, `S` i `G54` mogu čitati ili ignorirati. Zbog toga generator mora imati konfigurabilan profil i ne smije unaprijed pretpostaviti da ih naš fizički kontroler obrađuje na određeni način.

**U ovoj iteraciji generator radi isključivo za jedan element. Layout i batch generiranje namjerno još ne postoje.**

## Prompt 9.1 – `GCodeGenerator` interface i RichAuto A11 profil

> PLAN MODE. Implementiraj:
> - `GCodeGenerator` interface
> - mali `RichAutoA11Profile` model/config.
>
> Profile treba imati eksplicitne opcije relevantne za naš test, npr.:
> - emit/use feed rate
> - emit/use spindle speed
> - emit/use G54
> - measurement units
> - absolute positioning
> - numeric precision.
>
> Ne hardkodiraj „stroj sigurno čita F/S/G54“. To ostaje konfiguracija koja će se potvrditi na stroju.
>
> Nemoj implementirati layout niti batch G-code.

## Prompt 9.2 – Formatter G-code brojeva i linija

> PLAN MODE. Implementiraj mali formatter za G-code numeričke vrijednosti.
>
> Zahtjevi:
> - decimalna točka ne smije ovisiti o hrvatskom Localeu
> - nema decimalnog zareza
> - nema scientific notation
> - broj decimala je eksplicitno definiran/configurable
> - rezultat je determinističan za testove.
>
> Ne biraj proizvoljno konačnu preciznost fizičkog stroja bez objašnjenja; napravi konfigurabilno i kasnije zaključaj nakon testa.

## Prompt 9.3 – Header/footer bez rezanja

> PLAN MODE. Implementiraj samo generiranje programskog header/footer kostura prema RichAuto profilu.
>
> Kandidati koje dokumentacija podržava uključuju:
> - G21 za mm
> - G17 za XY plane
> - G90 za absolute
> - G54 samo ako profil kaže da ga emitiramo
> - M03 za spindle CW samo kada je stvarno uključeno u profil/workflow
> - M05 za spindle stop
> - M30 za kraj programa.
>
> Prije implementacije pokaži mi točan redoslijed koji predlažeš i objasni svaku naredbu.
>
> Ne generiraj još shape movement.

## Prompt 9.4 – Jedan linearni ToolPath u G-code

> PLAN MODE. Implementiraj pretvaranje line-segment ToolPatha u G-code za **jedan oblik**.
>
> Logika:
> - rapid move do XY starta na safe Z
> - kontrolirani plunge prema prvoj dubini
> - G01 po linearnoj putanji
> - retract na safe Z.
>
> Dubinu ne reži odjednom ako `cutDepth > stepDown`.
>
> Napravi funkciju koja iz `cutDepth` i `stepDown` računa stvarne dubine prolaza tako da zadnji prolaz završi točno na ciljnoj dubini.
>
> Dodaj unit testove samo za generirani tekst, bez stroja.
>
> Ne dodaj quantity/layout logiku.

## Prompt 9.5 – Arc output za krug

> PLAN MODE. Implementiraj RichAuto output za kružne `ArcSegment` naredbe koristeći G02/G03 samo na temelju već postojeće geometrijske reprezentacije.
>
> Ne računaj krug u generatoru.
>
> Ako je puna kružnica sa start=end potencijalno osjetljiva, predloži stabilniju reprezentaciju s dva polukruga i objasni zašto.
>
> Dodaj string-level testove I/J parametara.
>
> Označi ovu podršku kao IMPLEMENTIRANO, ali ne TESTIRANO NA STROJU.

## Prompt 9.6 – `.nc` export service

> PLAN MODE. Implementiraj odvojeni servis za spremanje generiranog `GCodeProgram` sadržaja u `.nc` datoteku.
>
> Zahtjevi:
> - validiraj ekstenziju
> - plain-text format
> - eksplicitni charset prikladan za ASCII G-code
> - nemoj koristiti default system Locale za brojeve
> - ne zapisuj automatski na USB bez korisničkog odabira
> - UI će kasnije odabrati destination.
>
> Dodaj test koji zapisuje privremenu `.nc` datoteku i pročita je natrag.

---

# 10. JavaFX UI – ITERACIJA 1, jedan element

UI prve iteracije služi tome da možemo napraviti cijeli single-element tok i što prije doći do stvarnog testa. **Ne prikazujemo quantity, capacity per sheet ni required sheets.**

## Prompt 10.1 – FXML decision gate

> PLAN MODE. Usporedi dvije opcije za ovaj projekt:
> 1. JavaFX UI programatski u Javi
> 2. FXML + controller.
>
> Kriteriji:
> - studentski projekt
> - održivost
> - odvajanje viewa i controllera
> - koliko je lako objasniti na obrani
> - koliko novih dependency/config koraka uvodi.
>
> Ne mijenjaj kod prije odluke.
>
> Ako odaberemo FXML, tek tada dodaj `javafx.fxml` dependency i potrebnu module/config podršku.

## Prompt 10.2 – Glavni ekran prve iteracije

> PLAN MODE. Implementiraj samo vizualni kostur glavnog ekrana za rad s **jednim elementom**, bez poslovne logike.
>
> Ekran treba imati:
> - odabir ShapeType
> - dinamička polja dimenzija
> - MaterialSheet width/height/thickness
> - odabir machine
> - odabir tool
> - machining parameters
> - Generate
> - G-code preview TextArea
> - Save
> - Export `.nc`
> - Saved Programs navigation.
>
> **Nemoj dodavati quantity, capacity per sheet, required sheets ni layout prikaz.**
>
> Ne spajaj još gumbe na servise.

## Prompt 10.3 – Dinamička shape polja + parsiranje inputa

> PLAN MODE. Implementiraj samo ponašanje forme prema odabranom shapeu:
> - Square → stranica
> - Rectangle → width + height
> - Circle → promjer
> - Equilateral Triangle → stranica.
>
> UI ne smije korisniku pokazivati `dimensionA/B/C`.
>
> Dodaj centralizirano parsiranje numeričkih vrijednosti tako da controller ne duplira isti try/catch za svako polje.
>
> Još nemoj generirati G-code.

## Prompt 10.4 – UI validacija preko service/validation sloja

> PLAN MODE. Spoji formu s postojećim validatorima.
>
> Controller smije:
> - pročitati input
> - napraviti request/domain objekte
> - pozvati validator/service
> - prikazati korisniku poruku.
>
> Controller ne smije:
> - računati trokut
> - raditi SQL
> - slagati G-code string.
>
> U ovoj iteraciji nema quantity/layout validacije na UI-u.
>
> Testiraj nekoliko nevaljanih inputa ručno ili controller testom ako je razumno.

## Prompt 10.5 – Generate workflow za jedan element

> PLAN MODE. Spoji `Generate` gumb s postojećim service slojem.
>
> Nakon klika želim:
> - validaciju
> - provjeru stane li **jedan** oblik na ploču
> - generiranje ToolPatha
> - generiranje G-koda
> - prikaz G-koda u TextArea.
>
> Workflow mora koristiti jedan element. Ako `MachiningJob` već ima `quantity`, za ovu iteraciju koristi vrijednost `1` bez korisničkog polja.
>
> Controller treba ostati tanak.
>
> Nemoj implementirati layout, capacity ni requiredSheets.

## Prompt 10.6 – Save + Saved Programs screen

> PLAN MODE. Implementiraj:
> - spremanje trenutačno generiranog single-element joba preko service/repository sloja
> - ekran/listu spremljenih jobova
> - otvaranje odabranog joba
> - prikaz spremljenog G-koda.
>
> Nemoj raditi SELECT/INSERT u controlleru.
>
> Quick access znači ponovno učitati spremljene parametre u formu, ne samo pokazati tekst G-koda.
>
> Ako se `quantity` sprema u bazu, u ovoj iteraciji vrijednost je 1.

## Prompt 10.7 – Export `.nc` iz UI-a

> PLAN MODE. Spoji Export gumb s postojećim `.nc` export serviceom.
>
> Koristi JavaFX file chooser ili ekvivalent koji odgovara odabranom UI pristupu.
>
> Controller samo:
> - pita korisnika gdje spremiti
> - preda Path i GCodeProgram export serviceu
> - prikaže rezultat/pogrešku.
>
> Ne implementiraj automatsko slanje na stroj niti USB protokol.

---

# 11. Integracija – ITERACIJA 1

## Prompt 11.1 – Composition root / dependency wiring

> PLAN MODE. Pregledaj postojeće slojeve i predloži jedno jasno mjesto gdje se stvaraju i povezuju:
> - DatabaseConfig
> - repository implementacije
> - validators
> - geometry generators
> - GCodeGenerator
> - ProgramGenerationService
> - controller dependencies.
>
> **LayoutService još ne postoji i ne smije se uvoditi prije GATE 1.**
>
> Nemoj uvoditi Spring ili drugi dependency injection framework.
>
> Za studentski projekt želim jednostavno ručno dependency wiring rješenje koje mogu objasniti.

## Prompt 11.2 – End-to-end Iteracija 1

> PLAN MODE. Spoji i provjeri cijeli tok za **jedan oblik bez batch layouta**:
>
> `UI input -> validation -> single-shape fit -> ToolPath -> RichAuto generator -> preview -> .nc export`
>
> Koristi testni primjer iz dokumentacije projekta:
> - ploča 500 × 500 mm
> - jednakostranični trokut
> - stranica 30 mm
> - quantity = 1.
>
> Machining parametre nemoj izmišljati ako još nisu potvrđeni; u automatiziranom testu koristi jasno označene testne vrijednosti koje nisu deklarirane kao stvarni strojni parametri.
>
> Rezultat ovog koraka je softverski test, ne fizički test.

## Prompt 11.3 – Persistence round-trip za single-element job

> PLAN MODE. Napravi end-to-end test:
> - generiraj single-element job
> - spremi ga
> - otvori fresh repository context
> - ponovno učitaj job
> - potvrdi da su shape, material, machining parameters i gCode isti
> - ako model sadrži quantity, potvrdi da je spremljeno `1`.
>
> To je ključni test za FR-9, FR-10 i FR-11.
>
> Nemoj još tvrditi ništa o fizičkom stroju.

---

# 12. PRVI TEST – ZK-1325 / RichAuto A11, jedan element

Ovo je **obvezna kontrolna točka prije quantity/layout implementacije**.

Prvi fizički test ne dokazuje da je cijela aplikacija završena. Njegova je svrha potvrditi da osnovni generacijski lanac može proizvesti program koji se na ciljnom stroju ponaša očekivano za jedan jednostavan element.

## Prompt 12.1 – Pre-machine test checklist

> PLAN MODE. Na temelju implementiranog single-element generatora napravi checklist za provjeru prije fizičkog testa.
>
> Mora uključivati:
> - verziju aplikacije/commit
> - točan model stroja
> - kontroler RichAuto A11
> - work area X/Y/Z
> - orientation osi
> - work coordinate system
> - work zero
> - postavku čitanja F
> - postavku čitanja S
> - postavku čitanja G54
> - tool podatke
> - safe Z
> - cut depth
> - step down
> - feed/plunge/spindle
> - pregled `.nc` datoteke prije učitavanja.
>
> Ne predlaži proizvoljne brzine ili dubine rezanja. Te vrijednosti mora dati stvarni alat/materijal/operator.

## Prompt 12.2 – Static G-code audit za testni trokut

> PLAN MODE. Za generirani testni program:
> - ploča 500 × 500
> - trokut 30 mm
> - quantity = 1
>
> napravi statičku analizu `.nc` sadržaja bez pokretanja stroja.
>
> Provjeri:
> - G21 / mm
> - G90
> - G17
> - G54 samo prema profilu
> - safe Z prije XY premještanja
> - ispravne XYZ granice
> - točan step-down
> - M03/M05 prema profilu
> - M30 na kraju
> - decimalni format.
>
> Ne mijenjaj kod ako nema konkretno pronađenog problema.

## Prompt 12.3 – Kontrolirani prvi test bez obrade materijala

> PLAN MODE. Pripremi plan prvog kontroliranog testa na fizičkom ZK-1325 koji prvenstveno provjerava koordinatni sustav i smjer putanje prije stvarnog rezanja.
>
> Plan mora naglasiti:
> - koristiti postupak koji odobri iskusni operator stroja
> - provjeriti emergency stop i machine state
> - ne koristiti neprovjerene machining parametre
> - prvo potvrditi XY orijentaciju, work zero i safe Z
> - zabilježiti ponašanje F/S/G54 na konkretnom kontroleru.
>
> Ne izmišljaj sigurnosne ili strojne postavke koje ne znamo.

## Prompt 12.4 – Prvi stvarni test jednog trokuta

> PLAN MODE. Nakon što je prethodni kontrolirani test prošao i operator je potvrdio machining parametre, pripremi test-case zapis za stvarni primjer:
> - material sheet 500 × 500 mm
> - equilateral triangle side 30 mm
> - quantity = 1
> - tool: stvarno korišteni tool zapis
> - machining parameters: stvarno potvrđene vrijednosti
> - generated `.nc`.
>
> Test-case mora imati polja:
> - expected path
> - actual result
> - measured dimensions
> - deviations
> - controller behavior
> - pass/fail
> - notes.
>
> Nemoj unaprijed popuniti rezultate.

## GATE 1 – odluka nakon prvog testa

Prije nastavka na Korak 13 mora se pregledati bilješka prvog testa.

Nastavljamo na quantity/layout ako je potvrđeno da:

- `.nc` datoteka se može učitati u ciljnom workflowu
- koordinatna orijentacija je razumljiva i dokumentirana
- work zero / WCS ponašanje je dovoljno jasno za nastavak
- single-element putanja odgovara očekivanom obliku
- safe Z / plunge / step-down ponašanje nema poznatu prepreku za nastavak
- relevantno ponašanje `F`, `S` i `G54` je zabilježeno
- eventualne korekcije generatora nakon testa su implementirane i ponovno provjerene.

**Ako single-element test ne prođe, ne počinjati layout. Prvo popraviti i ponovno testirati osnovni generator.**

---

# 13. Quantity – početak ITERACIJE 2

Tek nakon prolaska kroz GATE 1 aktivira se funkcionalni zahtjev za više jednakih elemenata.

## Prompt 13.1 – Audit postojećeg `quantity` modela

> PLAN MODE. Ne dodaj još UI.
>
> Pregledaj postojeći kod i utvrdi gdje `quantity` već postoji:
> - `MachiningJob`
> - SQL schema
> - repository mapping
> - test fixtures.
>
> Ne dupliciraj postojeće polje.
>
> Objasni što treba promijeniti da `quantity`, koji je u Iteraciji 1 bio praktično fiksiran na 1, postane stvarni korisnički podatak u Iteraciji 2.

## Prompt 13.2 – Poslovna validacija količine

> PLAN MODE. Implementiraj ili aktiviraj poslovnu validaciju quantity vrijednosti:
> - mora biti cijeli broj
> - mora biti > 0
> - ne smije se tretirati kao decimalna vrijednost.
>
> Dodaj unit testove za:
> - 1
> - veću valjanu količinu
> - 0
> - negativnu vrijednost.
>
> Još nemoj implementirati layout.

## Prompt 13.3 – Request/service podrška za quantity

> PLAN MODE. Proširi request/service workflow tako da može prenijeti korisnički zadanu quantity vrijednost kroz poslovni sloj.
>
> U ovom koraku quantity se samo prenosi i validira.
>
> Ne računaj još gdje će elementi biti postavljeni i ne generiraj batch G-code.

---

# 14. Algoritam raspoređivanja više jednakih elemenata

Cilj V1 nije dokaz matematičke optimalnosti. Prvi algoritam mora dati valjan, deterministički i objašnjiv raspored te može birati bolju od nekoliko jednostavnih varijanti.

## Prompt 14.1 – Formalizacija layout ulaza

> PLAN MODE. Prije algoritma definiraj podatke koji layout mora dobiti.
>
> Potvrđeno:
> - sheet width/height
> - shape
> - quantity
>
> Još nije potvrđeno:
> - edge margin
> - part spacing
> - dopuštena rotacija pravokutnika
> - treba li part spacing automatski uključivati promjer alata.
>
> Ne izmišljaj te vrijednosti.
>
> Predloži mali `LayoutSettings` model s eksplicitnim vrijednostima umjesto skrivenih magic defaulta. Objasni koje postavke moraju biti potvrđene prije stvarnog rezanja.

## Prompt 14.2 – Layout output modeli

> PLAN MODE. Implementiraj samo modele rezultata raspoređivanja:
> - `PlacedShape`
> - `SheetLayout`
> - `LayoutResult`
>
> Rezultat treba moći reći:
> - položaj svakog komada
> - eventualnu rotaciju/orijentaciju
> - na kojoj je ploči
> - koliko komada stane na jednu ploču
> - koliko je ploča potrebno za zadanu količinu.
>
> Ne implementiraj algoritam još.

## Prompt 14.3 – Baseline grid za Square/Rectangle

> PLAN MODE. Implementiraj prvi deterministički grid layout za Square i Rectangle.
>
> Pravila:
> - nijedan bounding box ne smije izaći iz ploče
> - poštuj LayoutSettings margin/spacing
> - za rectangle usporedi barem orijentaciju 0° i 90° ako je rotacija dopuštena
> - odaberi varijantu koja smješta više elemenata
> - nemoj rezultat nazivati globalno optimalnim.
>
> Dodaj unit testove za:
> - oblik koji stane
> - oblik koji ne stane
> - količinu 1
> - više redova/stupaca
> - slučaj gdje rotacija daje bolji kapacitet.

## Prompt 14.4 – Baseline layout za Circle

> PLAN MODE. Implementiraj jednostavan, objašnjiv layout za krugove.
>
> Za prvu implementaciju koristi pravilan grid temeljen na promjeru i spacingu.
>
> Ne uvodi hexagonal close packing u ovom koraku.
>
> Cilj je ispravan baseline koji možemo kasnije usporediti s poboljšanjem.
>
> Dodaj test da svi centri/krugovi ostaju unutar granica ploče.

## Prompt 14.5 – Layout za Equilateral Triangle

> PLAN MODE. Implementiraj prvi valjani layout za jednakostranične trokute.
>
> Prvo napravi jednostavnu varijantu koju možemo lako testirati. Zatim, ako je unutar malog scopea, usporedi s alterniranjem orijentacije trokuta koje može poboljšati iskorištenost.
>
> Ne tvrdi matematičku optimalnost.
>
> Unit test mora provjeriti da nijedna točka trokuta ne izlazi iz ploče.

## Prompt 14.6 – Strategy selector + kapacitet + broj ploča

> PLAN MODE. Spoji postojeće shape-specific layout algoritme kroz mali `LayoutService` ili strategy pristup.
>
> `LayoutService` treba:
> - odabrati algoritam prema ShapeType
> - izračunati capacity per sheet
> - izračunati requiredSheets = ceil(quantity / capacity)
> - vratiti placement za traženu količinu
> - jasno odbiti slučaj capacity = 0.
>
> Dodaj unit testove za sva 4 oblika.
>
> Ne generiraj G-code u ovom servisu.

---

# 15. Batch G-code – ITERACIJA 2

## Prompt 15.1 – Translacija ToolPatha na placement

> PLAN MODE. Prije generiranja batch programa implementiraj i testiraj samo translaciju lokalnog `ToolPath` objekta na XY offset jednog `PlacedShape`.
>
> Cilj je ponovno koristiti single-element ToolPath koji je već prošao prvu iteraciju, a ne ponovno računati geometriju u GCodeGeneratoru.
>
> Dodaj unit test s jednostavnim oblikom i poznatim offsetom.

## Prompt 15.2 – G-code za više raspoređenih elemenata

> PLAN MODE. Proširi generator tako da primi `LayoutResult` / placement listu i za svaki element:
> - translira lokalni ToolPath na XY placement
> - primijeni istu machining logiku koja je već korištena za single-element test
> - između elemenata se sigurno vrati na safe Z
> - ne izlazi iz dopuštenih koordinata.
>
> G-code generator ne smije sam računati layout.
>
> Dodaj unit test s dva jednostavna kvadrata na različitim offsetima.
>
> Posebno objasni koji dio koda je ponovno korišten iz TESTIRANE single-element logike, a koji je nov i još NIJE TESTIRAN NA STROJU.

---

# 16. JavaFX UI – proširenje za quantity i layout

## Prompt 16.1 – Dodavanje quantity polja

> PLAN MODE. Proširi postojeći single-element ekran samo s korisničkim unosom `quantity`.
>
> Zahtjevi:
> - cijeli broj > 0
> - jasna poruka za nevaljani unos
> - postojeći single-element workflow za quantity=1 mora i dalje raditi.
>
> Nemoj još prikazivati capacity/requiredSheets dok layout nije spojen.

## Prompt 16.2 – Prikaz layout rezultata

> PLAN MODE. Nakon što `LayoutService` postoji, dodaj prikaz:
> - capacity per sheet
> - required sheets
> - stvarno raspoređena quantity vrijednost.
>
> Ako još nemamo grafički prikaz ploče, nemoj ga uvoditi samo zbog ovog prompta. Tekstualni rezultat je dovoljan za prvi batch workflow.

## Prompt 16.3 – Generate workflow za više elemenata

> PLAN MODE. Proširi postojeći `Generate` workflow:
>
> `input -> validation -> quantity -> layout -> capacity/requiredSheets -> ToolPaths -> batch G-code -> preview`
>
> Controller treba ostati tanak.
>
> Controller ne smije računati layout niti slagati batch G-code.
>
> Single-element quantity=1 mora ostati valjan slučaj.

---

# 17. Integracija – ITERACIJA 2

## Prompt 17.1 – Composition root proširenje

> PLAN MODE. Proširi postojeći dependency wiring samo onim što je sada potrebno za Iteraciju 2:
> - `LayoutSettings`
> - layout strategije
> - `LayoutService`
> - batch generation dependencies.
>
> Ne mijenjaj postojeće single-element komponente ako za to nema stvarnog razloga.

## Prompt 17.2 – End-to-end Iteracija 2

> PLAN MODE. Spoji tok za više jednakih elemenata:
>
> `input quantity -> layout -> capacity -> requiredSheets -> translated ToolPaths -> G-code za placements`
>
> Provjeri da:
> - layout ne prelazi sheet granice
> - generirani XY ne prelazi sheet granice
> - sheet dimenzije ne prelaze machine work area
> - svaki element koristi postojeću machining/step-down logiku.
>
> Dodaj integration test s malim brojem elemenata čiji expected rezultat možemo ručno provjeriti.

## Prompt 17.3 – Persistence round-trip nakon uvođenja quantity

> PLAN MODE. Ponovi persistence round-trip sada s quantity > 1.
>
> Potvrdi da se nakon ponovnog učitavanja čuvaju:
> - shape
> - material
> - machining parameters
> - quantity
> - gCode.
>
> Ako layout placementi nisu predviđeni za trajnu pohranu, nemoj stvarati novu tablicu samo zbog ovog testa. Dokumentiraj da se layout ponovno izračunava iz spremljenih ulaza, ako je to stvarni odabrani dizajn.

---

# 18. Batch test na ZK-1325

## Prompt 18.1 – Statički audit batch programa

> PLAN MODE. Prije fizičkog batch testa napravi statičku analizu generiranog programa za malu quantity vrijednost.
>
> Provjeri:
> - sve placement koordinate
> - sheet granice
> - machine work-area granice
> - safe Z između elemenata
> - step-down za svaki element
> - početak i kraj programa
> - ponašanje profila F/S/G54 prema stvarno potvrđenim postavkama prvog testa.
>
> Ne mijenjaj kod bez konkretno pronađenog problema.

## Prompt 18.2 – Test više jednakih elemenata

> PLAN MODE. Nakon uspješnog single-shape testa i statičkog batch audita pripremi test za više jednakih elemenata.
>
> Cilj:
> - provjeriti placement
> - capacity per sheet
> - required sheets
> - sigurnu tranziciju između elemenata
> - da nijedan XY move ne prelazi definirani sheet/work-area.
>
> Odaberi malu testnu količinu koju možemo vizualno i ručno provjeriti prije većeg batcha.
>
> Ne proglašavaj layout optimalnim; mjeri samo stvarni rezultat implementiranog algoritma.

---

# 19. Finalni test report

## Prompt 19.1 – Finalni status IMPLEMENTIRANO/TESTIRANO

> PLAN MODE. Nakon što unesemo stvarne rezultate svih provedenih testova, sastavi tehnički sažetak implementacije.
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
> - kapacitet / broj ploča
> - batch G-code
> - persistence
> - `.nc` export
> - RichAuto A11 profil
> - batch fizički test.
>
> Ne izmišljaj mjerne rezultate. Koristi samo stvarno zabilježene podatke.

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
│       ├── 03-03-cnc-machine.md
│       ├── ...
│       └── 19-01-finalni-test-report.md
├── src
├── pom.xml
└── AGENTS.md
```

Naziv pojedine bilješke treba sadržavati oznaku prompta kako bi se kasnije mogla izravno povezati s fazom implementacije.

---

# Predložena konačna package struktura

Ovo je planirana struktura; stvarni base package upisuje se tek nakon kreiranja projekta.

```text
<base-package>
├── app
├── config
├── domain
│   └── enums
├── validation
├── geometry
├── layout
├── gcode
├── service
├── persistence
│   ├── repository
│   └── jdbc
├── ui
│   └── controller
└── exception        # samo ako se pokaže korisnim
```

---

# Planirane ključne klase

## Domain

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

## Geometry

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

## Layout – ITERACIJA 2, tek nakon GATE 1

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

## G-code

Single-element generator pripada Iteraciji 1; batch proširenje tek Iteraciji 2.

```text
GCodeProgram
GCodeGenerator
RichAutoA11Profile
RichAutoA11GCodeGenerator
GCodeFormatter
NcFileExportService
```

## Service

Moguće klase, samo ako odgovaraju stvarno implementiranom toku:

```text
ProgramGenerationService
JobPersistenceService
ToolPathService
LayoutService
```

Ne stvarati jedan `ApplicationService` koji radi sve.

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

`GCODE_PROGRAM` se u V1 ne uvodi kao zasebna SQL tablica dok postoji samo jedan spremljeni output po jobu. Ako se kasnije uvede verzioniranje, regeneriranje ili više izlaznih programa po jobu, tada se može izdvojiti.

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
- repository integration testovi prolaze
- single-element `MachiningJob` se može save/loadati
- persistence nije kriterij kojim se proglašava RichAuto kompatibilnost.

## Milestone D – geometrija i validacija za jedan element rade

Završeno kada:

- sva 4 oblika daju očekivani ToolPath
- nevaljani unosi se odbijaju
- geometrija ne proizvodi G-code tekst
- jedan oblik se može provjeriti prema dimenzijama ploče.

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
- batch G-code se generira
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

1. OpenJFX službene upute podržavaju Maven workflow za JavaFX, uključujući automatsko dohvaćanje platformskih JavaFX dependencyja.
2. H2 službena dokumentacija potvrđuje embedded JDBC način rada i H2 Console.
3. H2 `USER` je keyword, zato SQL tablica ostaje `APP_USER`.
4. H2 podržava CLOB i standardna SQL ograničenja potrebna ovom modelu.
5. RichAuto A11 dokumentacija pokazuje da se postavke za `F`, `S` i `G54` mogu konfigurirati kao read/ignore, zato generator mora biti profiliran, a ne hardkodiran.
6. RichAuto A11/A11plus dokumentacija navodi G00/G01/G02/G03, G17, G21, G54–G59, G90/G91 i tipične M03/M05/M30 naredbe, ali stvarni output za konkretni ZK-1325 mora se potvrditi fizičkim testom.
7. OpenAI Codex best practices preporučuju prvo planiranje u Ask/Plan načinu, dobro scoped zadatke, persistent project context preko `AGENTS.md` i iterativnu implementaciju umjesto jednog velikog zadatka.
8. OpenAI smjernice za rad s Codexom navode `AGENTS.md` kao mehanizam za trajne projektne upute, a smjernice za AI-native engineering posebno preporučuju uključivanje pravila za dokumentaciju u `AGENTS.md`, automatsko generiranje dokumentacije gdje ima smisla te ljudski pregled važnih dokumenata. Zato se bilješke generiraju automatski, ali ih student mora pregledati prije korištenja u završnom radu.
9. Aktualne OpenAI preporuke za Codex naglašavaju Ask/Plan pristup, dobro ograničene zadatke i iterativni rad. Ovaj plan zato uvodi GATE 1: prvo se završava i provjerava mali end-to-end single-element tok, a tek zatim se otvara složeniji quantity/layout/batch dio.

---

# Što NE raditi

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
