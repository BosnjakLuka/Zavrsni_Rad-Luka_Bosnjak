# 18.1 — Povijest naloga i upravljanje spremljenim programima

**Datum:** 2026-08-27
**Status:** ODLUČENO / PLANIRANO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Uskladiti funkcionalne zahtjeve i potvrđene odluke s budućim pregledom povijesti naloga, uvjetnim prikazom stvarno dostupnih podataka o iskorištenju te role-ovisnim uređivanjem i soft-brisanjem spremljenih programa.

## Promijenjene datoteke

- `Dokumentacija/zahtjevi/funkcionalni_zahtjevi.md` — dodani planirani zahtjevi `FZ-30` i `FZ-31`.
- `Dokumentacija/biljeske/00_odluke.md` — zapisana odluka o povijesti, RBAC pravilima i soft-deleteu.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran prompt 18.1.
- `Dokumentacija/biljeske/18-01-povijest-naloga-i-upravljanje-programima.md` — ova bilješka.

## Stvarna implementacija

U ovom koraku nije mijenjan produkcijski kod, SQL, FXML ni testovi. Trenutačni `MachiningJob` sadrži autora, jednu ploču, jedan oblik i količinu, ali generatorski tok koristi jedan element i nema layout model. `SavedJobService#loadAll` vraća sve spremljene programe bez role filtra, a persistence ugovor nema update ni soft-delete operacije. Trenutačna H2 tablica `MACHINING_JOB` nema `deleted_at` ni `deleted_by_user_id`.

Dokumentacija sada planira da `ADMIN` i `ENGINEER` vide sve aktivne programe i autora. `ADMIN` smije uređivati i soft-brisati sve, dok `ENGINEER` smije uređivati i soft-brisati vlastite programe i programe korisnika čija je trenutačna rola `OPERATOR`. Soft-delete mora sačuvati vrijeme i korisnika brisanja u bazi te ukloniti zapis iz prikaza aplikacije.

## Razlog odabranog rješenja

Nova pravila jasno odvajaju postojeće ponašanje od budućeg opsega. Uvjetni prikaz broja ploča, površine, iskorištenja i ostatka sprječava uvođenje izmišljenih statistika prije stvarnog quantity/layout modela, dok soft-delete čuva trag zapisa u bazi.

## Arhitektonska povezanost

Buduća provjera prava pripada service sloju, a spremanje podataka o brisanju i filtriranje obrisanih zapisa persistence sloju. Katalog u JavaFX UI-ju smije koordinirati prikaz i akcije, ali neće sadržavati SQL, autorizacijska pravila ni izračune iskorištenja. Ne uvodi se dodatni statistički sloj.

## Važne odluke i ograničenja

- „Programer” znači postojeća rola `OPERATOR`; ne uvodi se `PROGRAMMER`.
- Uređivanje mijenja postojeći spremljeni nalog i time nadjačava samo prethodnu odluku o obveznom stvaranju novog snapshota.
- Soft-delete koristi `deleted_at` i `deleted_by_user_id`; zapis nije vidljiv u aplikaciji, ali ostaje u bazi.
- Soft-obrisani zapis ne može se uređivati; vraćanje obrisanog zapisa nije potvrđeno.
- Pravo `OPERATOR` korisnika na vlastite programe izvan upravljačkog kataloga nije ukinuto.
- Pokazatelji broja ploča, ukupne površine, iskorištenja i ostatka prikazuju se samo ako postoje u stvarnom spremljenom modelu.

## Build i testiranje

### Izvršene naredbe

```text
git diff --check
rg -n "FZ-30|FZ-31|deleted_at|deleted_by_user_id|OPERATOR|NIJE IMPLEMENTIRANO" Dokumentacija/zahtjevi/funkcionalni_zahtjevi.md Dokumentacija/biljeske/00_odluke.md Dokumentacija/biljeske/00_indeks.md Dokumentacija/biljeske/18-01-povijest-naloga-i-upravljanje-programima.md
```

### Stvarni rezultat

Statičke provjere dokumentacije uspješno su izvršene: diff nema whitespace pogrešaka, a ciljani pojmovi postoje u zahtjevima, odluci, indeksu i ovoj bilješci. Build i automatizirani testovi nisu pokrenuti jer nisu mijenjani produkcijski kod, SQL, konfiguracija, testovi ni UI.

### Što nije testirano

Nisu implementirani ni testirani katalog programa, role-ovisno uređivanje, soft-delete, skrivanje obrisanih zapisa, zapis korisnika brisanja ni pokazatelji iskorištenja. Nije proveden JavaFX vizualni test ni test na ciljnom stroju.

## Otvorena pitanja

- Točna polja koja će biti dopušteno uređivati u postojećem nalogu treba potvrditi u zasebnom implementacijskom koraku.
- Način spremanja layout rezultata i jedinice prikaza pokazatelja treba odrediti tek kada se implementira stvarni quantity/layout model.

## Moguće poglavlje završnog rada

Autorizacija, sljedivost i upravljanje spremljenim CNC programima.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — u ovom koraku nije mijenjan produkcijski kod, a trenutačni kod još ne implementira dokumentirana pravila.

## Kandidat za sliku, dijagram ili tablicu

RBAC matrica prava nad spremljenim programima može se izvesti iz ove potvrđene odluke nakon implementacije pripadajućeg UI-ja.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
