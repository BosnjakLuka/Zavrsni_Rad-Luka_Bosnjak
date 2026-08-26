# 13.1 — Role/RBAC decision gate

**Datum:** 2026-08-26  
**Status:** ODLUČENO / NIJE IMPLEMENTIRANO / NIJE TESTIRANO

## Cilj

Zaključati minimalnu V1 matricu prava, registracijska pravila, session ponašanje i granice upravljanja korisnicima prije implementacije autentikacije.

## Promijenjene datoteke

- `Dokumentacija/biljeske/00_odluke.md` — zapisana nova odluka koja zamjenjuje staru `USER`/`ADMIN` odluku.
- `Dokumentacija/biljeske/00_indeks.md` — evidentiran prompt 13.1.
- `Dokumentacija/biljeske/13-01-rbac-decision-gate.md` — ova bilješka.

## Stvarna implementacija

U ovom promptu nije mijenjan produkcijski kod. Potvrđene su tri fiksne role i sljedeća matrica:

| Funkcionalnost | ADMIN | ENGINEER | OPERATOR |
|---|---|---|---|
| Upravljanje korisnicima i dodjela rola | Da | Ne | Ne |
| Upravljanje materijalima, strojevima i alatima | Da | Da | Ne |
| Generiranje i spremanje novog programa | Da | Da | Da |
| Pregled, reopen i export svih programa | Da | Da | Ne |
| Pregled, reopen i export vlastitih programa | Da | Da | Da |
| Promjena vlastite lozinke uz staru lozinku | Da | Da | Da |

Javna registracija stvara odmah aktivan `OPERATOR` račun. Role nisu korisnički CRUD podaci, nego fiksni sistemski skup. Session nije trajan. Spremljeni jobovi ne prepisuju se; reopen i izmjena završavaju novim job snapshotom.

## Razlog odabranog rješenja

Matrica je dovoljno mala za lokalnu JavaFX aplikaciju i izravno odgovara stvarnim funkcionalnostima. Centralizirana pravila mogu se testirati bez dupliciranih provjera po controllerima.

## Arhitektonska povezanost

Odluka buduću autentikaciju i autorizaciju smješta u service sloj, user SQL u persistence sloj, a prikaz i navigaciju u JavaFX UI. Ne uvodi se novi framework ni dodatna arhitektonska razina.

## Važne odluke i ograničenja

- Admin ne smije deaktivirati vlastiti aktivni session račun ni ukloniti posljednjeg aktivnog administratora.
- Korisnici se deaktiviraju, ne brišu fizički.
- Korisničko ime je jedinstveno i nepromjenjivo.
- Development računi smiju biti testni, ali plaintext lozinke ne ulaze u repozitorij, bazu ili bilješke.
- Točan PBKDF2 format i radni parametri pripadaju implementaciji 13.2.

## Build i testiranje

### Izvršene naredbe

```text
Nije pokrenut build — prompt 13.1 mijenja samo potvrđene razvojne odluke i dokumentaciju.
```

### Stvarni rezultat

Statički je provjerena usklađenost odluke s aktualnim Korakom 13 u `plan_implementacije(5).md`, postojećim `User`/`Role` modelom i pravilima projekta.

### Što nije testirano

Autentikacija, session, RBAC enforcement, JavaFX ekrani i upravljanje korisnicima još nisu implementirani ni testirani.

## Otvorena pitanja

- Za 13.2 treba odabrati i zapisati točan PBKDF2 broj iteracija, veličinu salta, veličinu izvedenog ključa i format spremljenog hasha.

## Moguće poglavlje završnog rada

Autentikacija i autorizacija lokalne desktop aplikacije.

## Kandidati za isječke koda

Kandidat za isječak koda: nema — u ovom koraku nije mijenjan produkcijski kod.

## Kandidat za sliku, dijagram ili tablicu

RBAC matrica iz ove bilješke može se koristiti kao tablica u poglavlju o autentikaciji i autorizaciji.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
