# 13.2 — Password hashing, AuthService i SessionContext

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Implementirati lokalnu authentication jezgru, sigurno spremanje lozinki, memorijsku sesiju te idempotentni bootstrap po jednog development korisnika za svaku potvrđenu rolu.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/PasswordHasher.java` — PBKDF2 hashiranje i verifikacija.
- `src/main/java/hr/lukabosnjak/service/AuthService.java` — registracija, login, logout i normalizacija usernamea.
- `src/main/java/hr/lukabosnjak/service/SessionContext.java` — memorijski current-user session.
- `src/main/java/hr/lukabosnjak/service/AuthenticationException.java` i `RegistrationException.java` — jasne service pogreške.
- `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java` — tri idempotentna development korisnika s PBKDF2 zapisima.
- `src/main/java/hr/lukabosnjak/persistence/repository/UserRepository.java` i `persistence/jdbc/JdbcUserRepository.java` — dohvat svih korisnika potreban provjeri seeda.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` — povezuje hasher, auth service i session.
- `src/test/java/hr/lukabosnjak/service/PasswordHasherTest.java`, `AuthServiceIntegrationTest.java` i prošireni bootstrap/repository testovi — softverska provjera.
- `Dokumentacija/biljeske/00_odluke.md`, `00_indeks.md` i ova bilješka — odluka i razvojni trag.

## Stvarna implementacija

`PasswordHasher` stvara zaseban 16-byte salt za svaki zapis, izvodi 256-bitni ključ s 600.000 PBKDF2-HMAC-SHA256 iteracija i sprema algoritam, broj iteracija, salt i hash u jednom verzioniranom zapisu. Malformed i nepodržani zapisi odbijaju se bez autentikacije.

`AuthService` normalizira username, registrira odmah aktivnog `OPERATOR` korisnika, provjerava jedinstvenost usernamea, zahtijeva ime/prezime i registracijsku lozinku duljine 8–128 znakova. Login odbija nepostojećeg korisnika, pogrešnu lozinku i inactive račun. Uspješan login postavlja `SessionContext`, a logout ga prazni.

Bootstrap idempotentno dodaje račune `admin`, `engineer` i `operator` s odgovarajućim rolama. Source i baza sadrže samo unaprijed generirane PBKDF2 zapise, ne plaintext lozinke. Ponovno pokretanje ne mijenja ni duplicira postojeći username.

## Razlog odabranog rješenja

Ugrađeni JDK API izbjegava novu dependency biblioteku i ostaje lako objašnjiv u studentskom projektu. Auth pravila su u service sloju, a SQL ostaje u JDBC repositoryju. Versionirani hash format omogućuje kasniju promjenu work factora bez promjene SQL sheme.

## Arhitektonska povezanost

Domenski `User` i `Role` ostaju bez JavaFX/JDBC ovisnosti. `AuthService` koordinira repository i hasher, `SessionContext` čuva samo runtime stanje, JDBC repository izvršava SQL, a composition root povezuje konkretne implementacije.

## Važne odluke i ograničenja

- Ovaj korak ne implementira JavaFX login/registration ekrane; oni pripadaju 13.3.
- Ne implementira promjenu role, aktivaciju/deaktivaciju, password reset ni RBAC enforcement; oni pripadaju 13.4.
- Development računi nisu produkcijska sigurnosna preporuka.
- Nema `remember me` tokena ni trajnog sessiona.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> -Dtest=PasswordHasherTest,AuthServiceIntegrationTest,V1BootstrapServiceIntegrationTest,ApplicationCompositionRootIntegrationTest,JdbcRepositoriesIntegrationTest test
<IntelliJ Maven> -Dmaven.repo.local=<lokalni-maven-repozitorij> test
```

### Stvarni rezultat

Ciljani paket: 14 testova, 0 failures, 0 errors, `BUILD SUCCESS`. Puni suite: 130 testova, 0 failures, 0 errors, `BUILD SUCCESS`.

Testovi potvrđuju različite saltove za istu lozinku, dobru i pogrešnu lozinku, malformed zapis, registraciju, normalizaciju usernamea, dupli username, default `OPERATOR`, inactive račun, login/logout session te dvostruki bootstrap bez duplikata korisnika.

### Što nije testirano

JavaFX login i registration tok nije implementiran ni vizualno testiran. Nije proveden ručni login nad produkcijskom file-based bazom. Administratorska pravila i RBAC još nisu implementirani.

## Otvorena pitanja

- Nema otvorene odluke unutar 13.2. Sljedeća zasebna cjelina je JavaFX login/registration tok iz 13.3.

## Moguće poglavlje završnog rada

Sigurno lokalno spremanje lozinki i upravljanje korisničkom sesijom.

## Kandidati za isječke koda

### Kandidat: Verz­ionirani PBKDF2 zapis i verifikacija

**Datoteka:** `src/main/java/hr/lukabosnjak/service/PasswordHasher.java`  
**Klasa/metoda:** `PasswordHasher#hash`, `PasswordHasher#verify`  
**Zašto je važan:** Prikazuje salt, work factor, verzioniranje i constant-time provjeru bez vanjskog auth frameworka.  
**Moguće poglavlje:** Autentikacija i sigurnost lozinki

```java
return ALGORITHM_ID + "$" + ITERATIONS + "$"
        + Base64.getEncoder().encodeToString(salt) + "$"
        + Base64.getEncoder().encodeToString(derived);
```

**Ideja opisa u radu:** Spremljeni zapis nosi sve javne parametre potrebne za kasniju provjeru i buduće podizanje radnog faktora.

### Kandidat: Service koordinacija prijave

**Datoteka:** `src/main/java/hr/lukabosnjak/service/AuthService.java`  
**Klasa/metoda:** `AuthService#login`  
**Zašto je važan:** Pokazuje odvajanje persistence dohvata, statusa računa, verifikacije lozinke i session stanja.  
**Moguće poglavlje:** Aplikacijski service sloj

```java
if (!passwordHasher.verify(password, user.getPasswordHash())) {
    throw invalidCredentials(null);
}
sessionContext.login(user);
```

**Ideja opisa u radu:** Controller kasnije samo prosljeđuje vjerodajnice i reagira na rezultat, bez SQL-a i hashinga.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
