# 6.1 — Pre-DDL audit i User/Role runtime gate

**Datum:** 2026-08-25  
**Status:** ANALIZIRANO / ODLUČENO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Prije izrade DDL-a usporediti trenutačne Java modele, relacijski/UML prikaz i funkcionalne zahtjeve te zatvoriti postojeći gap između `ROLE`/`APP_USER`/`created_by_user_id` strukture i nepostojanja potvrđenog runtime autentikacijskog toka.

## Promijenjene datoteke

- `Dokumentacija/biljeske/00_odluke.md` — dodana potvrđena odluka o lokalnoj autentikaciji, ulogama i pristupu poslovima.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran Prompt 6.1.
- `Dokumentacija/biljeske/06-01-pre-ddl-audit-user-role-gate.md` — zapisan audit, odluka i ograničenja sljedećeg milestonea.

## Stvarna implementacija

Provedena je dokumentacijska i statička usporedba bez promjene Java koda, dijagrama ili baze. Potvrđeno je da V1 treba lokalnu registraciju i prijavu, uloge `USER` i `ADMIN`, first-run stvaranje administratora bez hardkodirane lozinke te pristup u kojem korisnik upravlja svojim poslovima, a administrator vidi sve poslove. Odluka je unesena u `00_odluke.md`. Autentikacija, autorizacija, seed/bootstrap kod i DDL još nisu implementirani.

### Usporedba modela i zahtjeva

| Pravilo | Java modeli | Relacijski/UML prikaz | Zaključak prije DDL-a |
|---|---|---|---|
| Naziv korisničke tablice | Klasa se zove `User` | Prikazana je `APP_USER` | U bazi koristiti isključivo `APP_USER`, ne `USER`. |
| Položaj `safe_z` | `safeZ` postoji samo u `MachiningParameters` | Relacijski dijagram pogrešno prikazuje i `MATERIAL_TYPE.safe_z`; UML duplicira neke tablice | `safe_z` smije postojati samo u `MACHINING_PARAMETERS`; dijagrame ispraviti prije konačne uporabe. |
| Veza joba i oblika | `MachiningJob.shape` postoji | `MACHINING_JOB.shape_id` postoji | Veza je potvrđena i obvezna u V1. |
| G-kod u Iteraciji 1 | `MachiningJob.gCode` postoji | `g_code` nedostaje na oba dijagrama | Budući `MACHINING_JOB` mora imati `g_code CLOB`. |
| Jedinstvenost broja alata | `Tool` sadrži stroj i `toolNumber` | Veza stroja i alata postoji, ali složena jedinstvenost nije prikazana | Buduća shema mora osigurati `UNIQUE(cnc_machine_id, tool_number)`. |
| Snapshot veze | Job sadrži `MaterialSheet`, `MachiningParameters` i `Shape` | Relacijski dijagram ih opisuje kao 1:1 u V1 | Buduća shema mora jedinstvenim FK-ovima spriječiti dijeljenje snapshot zapisa između jobova. |
| Strategija identiteta | ID polja koriste nullable `Long` | PK-ovi su označeni kao `BIGINT`, bez identity oznake | Svi PK-ovi trebaju biti bazom generirani BIGINT identity. |
| Tool pripada stroju | `Tool.cncMachine` postoji | `TOOL.cnc_machine_id` postoji | Modeli su usklađeni. |
| Job sadrži stroj i alat | `MachiningJob.cncMachine` i `MachiningJob.tool` postoje | Oba FK-a postoje | Service/validation sloj mora provjeriti da alat pripada odabranom stroju. |
| Autor spremljenog posla | `MachiningJob.createdBy` postoji | `created_by_user_id` pokazuje na `APP_USER` | Runtime izvor je prijavljeni korisnik; pravila pristupa određena su novom odlukom. |

## Razlog odabranog rješenja

Jedan tehnički bootstrap korisnik bez prijave bio bi manji zahvat, ali ne bi zadovoljio potvrđenu potrebu za klasičnom registracijom i prijavom. Odabrani lokalni pristup daje smisao autorstvu i vlasništvu poslova bez uvođenja OAutha ili vanjskih servisa. First-run setup izbjegava hardkodiranu administratorsku tajnu.

## Arhitektonska povezanost

Budući UI samo prikuplja podatke i koordinira tok. Hashiranje i provjera lozinki, current-user/session stanje i autorizacijska pravila pripadaju service/validation dijelu, dok JDBC i spremanje korisnika pripadaju persistence sloju. `created_by_user_id` popunjava se identitetom iz prijavljene sesije, a ne proizvoljnim UI unosom.

## Važne odluke i ograničenja

- V1 ima lokalnu registraciju i prijavu bez OAutha.
- Javna registracija stvara samo `USER`; prvi `ADMIN` nastaje kroz first-run setup.
- Lozinka se ne pohranjuje niti zapisuje kao čisti tekst.
- `USER` upravlja svojim poslovima, a `ADMIN` vidi sve poslove i upravlja osnovnim referentnim podacima.
- DDL, SQL, auth kod, seed i UI nisu dio ovog audita.
- Dijagrami trenutačno nisu konačni izvor sheme zbog zabilježenih nesklada.

## Build i testiranje

### Izvršene naredbe

```text
rg -n -C 4 "H2|JDBC|baza|Console|milestone|Milestone" Dokumentacija/plan_implementacije.md
rg -n -C 5 "MachiningJob|APP_USER|ROLE|login|RBAC|snapshot|H2|identity|Tool" Dokumentacija/biljeske/00_odluke.md
```

Uz navedene tekstualne provjere ručno su pregledani svi trenutačni modeli u `domain.entities`, sadržaj funkcionalnih zahtjeva iz `Zavrsni_rad_natuknice_v2.docx` te postojeće PNG verzije relacijskog i UML dijagrama.

### Stvarni rezultat

Svih devet zadanih pravila provjereno je prema stvarnim modelima i dokumentima. Pronađeni su nedostajući `g_code`, pogrešno prikazani dodatni `safe_z`, neoznačena identity strategija i složena jedinstvenost te duplicirani elementi UML prikaza. User/Role runtime gap zatvoren je potvrđenom projektnom odlukom, ali funkcionalnost nije implementirana.

### Što nije testirano

Nisu pokrenuti build ni runtime test jer kod nije promijenjen. Nisu testirani registracija, prijava, first-run administrator, autorizacija, DDL ni persistence ponašanje. Nije provedeno fizičko testiranje na CNC stroju i ovaj audit ne donosi tvrdnju o RichAuto kompatibilnosti.

## Otvorena pitanja

- Parametri i format sigurnog hashiranja lozinki trebaju se zaključati u zasebnom auth implementacijskom milestoneu prije pisanja te funkcionalnosti.
- Točan opseg administratorskog upravljanja pojedinim referentnim podacima treba vezati uz stvarne V1 UI tokove, bez unaprijed stvorenih CRUD ekrana.
- Postojeće dijagrame treba ispraviti prije proglašenja konačnim ER/UML prikazom.

## Moguće poglavlje završnog rada

Analiza podatkovnog modela te autentikacija, autorizacija i vlasništvo nad spremljenim CNC poslovima.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — u ovom koraku nije mijenjan ni implementiran programski kod.

## Kandidat za sliku, dijagram ili tablicu

Tablica usklađenosti u ovoj bilješci koristan je kandidat za objašnjenje prijelaza iz domenskog u relacijski model. Postojeći relacijski i UML PNG dijagrami nisu kandidati za konačni rad dok se ne isprave navedene pogreške.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
