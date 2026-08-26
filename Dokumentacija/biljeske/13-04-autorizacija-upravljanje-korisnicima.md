# 13.4 — Autorizacija i upravljanje korisnicima

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Uvesti minimalnu autorizaciju prema matrici 13.1 i administratorski pregled
korisnika sa sigurnim promjenama statusa i role.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/AuthorizationService.java` i
  `AuthorizationException.java` — centralna provjera prava.
- `src/main/java/hr/lukabosnjak/service/UserManagementService.java` i
  `UserManagementException.java` — administratorska orkestracija.
- `src/main/java/hr/lukabosnjak/persistence/repository/UserRepository.java` i
  `JdbcUserRepository.java` — prepared-statement izmjene statusa i role.
- `src/main/java/hr/lukabosnjak/ui/controller/UserManagementController.java` i
  `user-management.fxml` — pregled i akcije nad korisnicima.
- `MainFormController.java`, `Main.java`, `ApplicationNavigation.java`,
  `ApplicationCompositionRoot.java` i `main-form.fxml` — role gateovi i wiring.
- `AuthorizationServiceTest.java`, `FxmlControllerContractTest.java` i
  `ApplicationCompositionRootIntegrationTest.java` — ciljane provjere.
- `00_indeks.md` i ova bilješka — razvojni trag.

## Stvarna implementacija

`AuthorizationService` centralno provjerava `MANAGE_USERS`,
`MANAGE_REFERENCE_DATA` i `GENERATE_PROGRAM`. Samo `ADMIN` može upravljati
korisnicima, dok `ADMIN` i `ENGINEER` mogu upravljati referentnim podacima.

Administratorski ekran prikazuje korisnike, omogućuje aktivaciju/deaktivaciju i
promjenu jedne od postojećih sistemskih rola. Deaktivacija vlastitog računa i
deaktivacija ili degradacija posljednjeg aktivnog administratora odbijaju se.
Kreiranje korisnika od strane administratora nije uvedeno jer postojeća javna
registracija već pokriva potvrđeni minimalni tok.

Gumbi za referentne podatke su onemogućeni za `OPERATOR`, a handleri dodatno
provjeravaju pravo prije otvaranja dijaloga. Direktna navigacija na
administratorski prikaz vraća neovlaštenog korisnika na glavni ekran.

## Razlog odabranog rješenja

Jedan mali service za prava izbjegava duplicirane role provjere u controllerima,
a postojeći composition root i slojevi ostaju nepromijenjeni u odgovornostima.
SQL vrijednosti se prenose kroz `PreparedStatement`.

## Arhitektonska povezanost

Controlleri koordiniraju JavaFX prikaz i pozivaju service sloj. User service
provjerava sesiju i poslovna ograničenja, dok JDBC repository izvršava SQL.
Domenski modeli ne ovise o JavaFX-u ni JDBC-u.

## Važne odluke i ograničenja

- Role ostaju fiksne: `ADMIN`, `ENGINEER`, `OPERATOR`.
- Korisnici se deaktiviraju, ne brišu fizički.
- Nije uveden složeni permission framework ni kreiranje novih rola.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit run: AuthorizationServiceTest
IDE JUnit run: FxmlControllerContractTest
mvn -q "-Dtest=AuthorizationServiceTest,FxmlControllerContractTest,ApplicationCompositionRootIntegrationTest" test
```

### Stvarni rezultat

IDE build je uspješan bez problema. `AuthorizationServiceTest` je uspješno
izvršio 2 testa, a `FxmlControllerContractTest` 1 test. Maven naredba nije
izvršena jer `mvn` nije dostupan u terminalskom okruženju.

### Što nije testirano

Nije pokrenut puni Maven suite ni integracijski test JDBC izmjena. JavaFX ekran
nije vizualno testiran. Nije proveden fizički test na CNC stroju.

## Otvorena pitanja

- Za potpunu runtime provjeru potrebno je pokrenuti postojeće JDBC integracijske
  testove u Maven/IntelliJ okruženju s dostupnim Mavenom.

## Moguće poglavlje završnog rada

Autorizacija i upravljanje korisnicima u servisnom sloju desktop aplikacije.

## Kandidati za isječke koda

### Kandidat: Centralna provjera prava

**Datoteka:** `src/main/java/hr/lukabosnjak/service/AuthorizationService.java`  
**Klasa/metoda:** `AuthorizationService#require`  
**Zašto je važan:** Prikazuje jedinstveno odlučivanje o pravima prema ulozi.  
**Moguće poglavlje:** Autorizacija i RBAC

```java
case MANAGE_USERS -> "ADMIN".equals(role);
case MANAGE_REFERENCE_DATA -> "ADMIN".equals(role) || "ENGINEER".equals(role);
```

**Ideja opisa u radu:** Role pravila nalaze se na jednom mjestu, a controlleri ih
ne dupliciraju.

### Kandidat: Zaštita posljednjeg administratora

**Datoteka:** `src/main/java/hr/lukabosnjak/service/UserManagementService.java`  
**Klasa/metoda:** `UserManagementService#setActive`, `changeRole`  
**Zašto je važan:** Prikazuje poslovno ograničenje nad administratorskim računima.  
**Moguće poglavlje:** Upravljanje korisnicima i integritet sustava

```java
if (!active && target.isActive() && "ADMIN".equals(target.getRole().getName())
        && countActiveAdmins() <= 1) {
    throw new AuthorizationException("Posljednji aktivni administrator ne može biti deaktiviran.");
}
```

**Ideja opisa u radu:** Administratorski service štiti minimalni operativni
integritet neovisno o UI kontroli.

## Kandidat za sliku, dijagram ili tablicu

Nema.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
