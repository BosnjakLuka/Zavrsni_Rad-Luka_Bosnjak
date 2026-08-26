# 13.3 — JavaFX login, registracija i odjava

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE VIZUALNO TESTIRANO

## Cilj

Uvesti početni JavaFX tok za prijavu i javnu registraciju, povezati ga s postojećim `AuthService` i memorijskom sesijom te omogućiti odjavu iz glavnog prikaza.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/ui/controller/ApplicationNavigation.java` — ugovor navigacije između početnih prikaza i glavne forme.
- `src/main/java/hr/lukabosnjak/ui/controller/LoginController.java` — koordinira unos vjerodajnica, prijavu i prikaz pogreške.
- `src/main/java/hr/lukabosnjak/ui/controller/RegistrationController.java` — koordinira registraciju `OPERATOR` računa, potvrdu lozinke i neposrednu prijavu.
- `src/main/java/hr/lukabosnjak/app/Main.java` — upravlja zamjenom scene i prije glavnog prikaza provjerava aktivnu sesiju.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` — ubrizgava auth, session i navigaciju u JavaFX controllere.
- `src/main/java/hr/lukabosnjak/ui/controller/MainFormController.java` — prikazuje aktivnog korisnika i omogućuje odjavu.
- `src/main/resources/hr/lukabosnjak/ui/view/login.fxml` i `registration.fxml` — novi početni obrasci.
- `src/main/resources/hr/lukabosnjak/ui/view/main-form.fxml` — zaglavlje s usernameom, rolom i gumbom za odjavu.
- `src/test/java/hr/lukabosnjak/ui/controller/FxmlControllerContractTest.java` — ugovorna provjera novih FXML resursa.
- `src/test/java/hr/lukabosnjak/app/ApplicationCompositionRootIntegrationTest.java` — provjera sastavljanja svih novih controllera.
- `Dokumentacija/biljeske/00_indeks.md` i ova bilješka — razvojni trag Koraka 13.3.

## Stvarna implementacija

Aplikacija se nakon inicijalizacije baze otvara na login prikazu. Uspješna prijava stvara postojeću memorijsku sesiju i otvara glavnu formu. Neuspjeh ostaje na loginu te prikazuje poruku iz service sloja.

Javna registracija prikuplja username, ime, prezime, lozinku i potvrdu lozinke. Nakon uspješne registracije aktivnog `OPERATOR` računa aplikacija ga odmah prijavljuje i otvara glavnu formu. Lozinke se iz JavaFX polja prenose kao `char[]`, polja se prazne, a privremena polja znakova prepisuju nakon obrade.

Glavna forma prikazuje username i sistemsku rolu aktivnog korisnika. Odjava prazni session i vraća aplikaciju na login. Izravno otvaranje glavne forme bez aktivne sesije preusmjerava na login.

## Razlog odabranog rješenja

Jedinstveni navigator u klasi `Main` odgovara jednostavnoj desktop aplikaciji s jednim glavnim prozorom. Controlleri ostaju tanki: čitaju UI vrijednosti, pozivaju postojeći service i biraju sljedeći prikaz. Pravila registracije, hashiranje i pristup bazi nisu premješteni u JavaFX sloj.

## Arhitektonska povezanost

UI controlleri ovise o `AuthService`, `SessionContext` i malom navigacijskom ugovoru. `ApplicationCompositionRoot` i dalje je jedino mjesto ručnog sastavljanja ovisnosti. SQL ostaje u persistence sloju, a autentikacijska pravila u service sloju.

## Važne odluke i ograničenja

- Novi registrirani račun ima fiksnu početnu rolu `OPERATOR` i odmah se prijavljuje.
- Session traje samo dok radi proces aplikacije; nema trajnog tokena ni opcije „remember me”.
- Korak 13.3 ne implementira upravljanje korisnicima, promjenu lozinke, RBAC ograničenja ni povezivanje autora sa spremanjem naloga; to ostaje za 13.4.
- Postojeći gumbi za referentne podatke još nisu skriveni ili blokirani prema roli.

## Build i testiranje

### Izvršene naredbe

```text
<IntelliJ Maven> -Dtest=FxmlControllerContractTest,ApplicationCompositionRootIntegrationTest,AuthServiceIntegrationTest test
<IntelliJ Maven> test
```

### Stvarni rezultat

Ciljani paket: 6 testova, 0 failures, 0 errors, `BUILD SUCCESS`. Puni suite: 130 testova, 0 failures, 0 errors, `BUILD SUCCESS`.

FXML ugovorni test potvrđuje da oba nova resursa postoje te da sva `fx:id` polja i `onAction` metode postoje u pripadajućim controllerima. Integration test composition roota potvrđuje sastavljanje login, registration, main i modalnih controllera. Postojeći auth integration test potvrđuje service ponašanje koje novi controlleri koriste.

### Što nije testirano

Nije pokrenut ručni ni vizualni JavaFX test. Nisu kroz stvarni prozor provjereni fokus, raspored, promjena veličine, tipkovničke akcije i poruke pogreške. Nije implementiran ni testiran RBAC iz Koraka 13.4. Nije izvršen fizički test na CNC stroju; ovaj UI korak ga ni ne potvrđuje.

## Otvorena pitanja

- Korak 13.3 nema otvorenu funkcionalnu odluku. Prije prijelaza dalje korisnik treba ručno provjeriti login, registraciju i odjavu ili zasebno odobriti Korak 13.4.

## Moguće poglavlje završnog rada

JavaFX korisničko sučelje za autentikaciju i upravljanje memorijskom sesijom.

## Kandidati za isječke koda

### Kandidat: Zaštićena navigacija glavnog prozora

**Datoteka:** `src/main/java/hr/lukabosnjak/app/Main.java`  
**Klasa/metoda:** `Main#showMain`  
**Zašto je važan:** Pokazuje da glavni prikaz nije dostupan bez aktivne sesije i da navigacija ostaje izvan pojedinačnih formi.  
**Moguće poglavlje:** Organizacija JavaFX aplikacije i korisnička sesija

```java
if (!compositionRoot.sessionContext().isAuthenticated()) {
    showLogin();
    return;
}
showView("/hr/lukabosnjak/ui/view/main-form.fxml", 1100, 760, 900, 650);
```

**Ideja opisa u radu:** Jedna navigacijska točka provjerava memorijsku sesiju prije prikaza funkcionalnosti aplikacije.

### Kandidat: Registracija i neposredna prijava

**Datoteka:** `src/main/java/hr/lukabosnjak/ui/controller/RegistrationController.java`  
**Klasa/metoda:** `RegistrationController#handleRegister`  
**Zašto je važan:** Prikazuje UI koordinaciju dvaju service poziva bez SQL-a ili hashiranja u controlleru.  
**Moguće poglavlje:** Registracija korisnika u JavaFX aplikaciji

```java
authService.register(
        usernameInput.getText(), password, firstNameInput.getText(), lastNameInput.getText());
authService.login(usernameInput.getText(), password);
navigation.showMain();
```

**Ideja opisa u radu:** Nakon validirane registracije aktivni `OPERATOR` račun može odmah započeti memorijsku sesiju.

## Kandidat za sliku, dijagram ili tablicu

Nema — vizualni prikaz još nije ručno potvrđen.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
