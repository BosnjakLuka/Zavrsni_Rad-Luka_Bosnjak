# 15.4 — Softverski testni alati Ø6 i Ø8

**Datum:** 2026-08-26  
**Status:** IMPLEMENTIRANO / SOFTVERSKI TESTIRANO / NIJE FIZIČKI TESTIRANO

## Cilj

Omogućiti pokretanje Generate toka u razvojnoj bazi kada stvarni alat još nije
potvrđen, bez predstavljanja testnih promjera kao stvarnih aktivnih alata.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java` — idempotentni
  seed razvojnih alata.
- `src/main/java/hr/lukabosnjak/app/ApplicationCompositionRoot.java` —
  prosljeđuje `ToolRepository` bootstrap servisu.
- `src/test/java/hr/lukabosnjak/service/V1BootstrapServiceIntegrationTest.java` —
  provjera promjera, oznaka i učitavanja alata.
- `src/test/java/hr/lukabosnjak/persistence/jdbc/JdbcRepositoriesIntegrationTest.java`
  — usklađen bootstrap poziv.
- `Dokumentacija/biljeske/00_indeks.md` i `00_odluke.md` — dokumentacija.

## Stvarna implementacija

Bootstrap za stroj `ZK-1325 / RichAuto A11` sada idempotentno osigurava dva
aktivna razvojna zapisa:

| Oznaka | Promjer | Tool number |
|---|---:|---:|
| `TESTNI ALAT Ø6 mm (SOFTVERSKI)` | 6 mm | 9006 |
| `TESTNI ALAT Ø8 mm (SOFTVERSKI)` | 8 mm | 9008 |

Brojevi 9006 i 9008 su razvojne oznake, ne potvrđeni brojevi alata na stroju.
`type`, `cuttingLength` i `fluteCount` ostaju nepoznati (`NULL`). Promjer je
dostupan katalogu, generation requestu i kompenzacijskom sloju.

Ponovno pokretanje bootstrap procesa ne duplicira zapise jer provjerava
`machine_id` i razvojni `tool_number`.

## Razlog odabranog rješenja

Korisnik može odmah testirati Generate i kompenzaciju s oba tehnička slučaja,
ali naziv i dokumentacija jasno sprječavaju zaključak da su Ø6 ili Ø8 stvarno
potvrđeni alati na ZK-1325.

## Arhitektonska povezanost

Seed ostaje u service bootstrap sloju, a JDBC upis u `JdbcToolRepository`.
`ReferenceDataService` ih učitava za UI. Nisu uvedeni SQL upisi u controller,
novi entiteti ni promjene generatora.

## Važne odluke i ograničenja

- Ovo nisu fizički potvrđeni alati.
- Nisu dodani nepoznati tehnički atributi.
- Promjer je stvarna vrijednost softverskog testnog zapisa i koristi se u
  aplikacijskoj kompenzaciji.
- Kada se potvrdi stvarni alat, treba ga unijeti kroz katalog s pravim brojem
  i nazivom; testni zapisi mogu ostati samo za razvoj ili se deaktivirati.

## Build i testiranje

### Izvršene naredbe

```text
IDE build_project (rebuild=false)
IDE JUnit: hr.lukabosnjak.service.V1BootstrapServiceIntegrationTest
```

### Stvarni rezultat

IDE build je uspješan. Sva 3 testa `V1BootstrapServiceIntegrationTest` su
uspješno završila, uključujući provjeru da se promjeri Ø6 i Ø8 učitavaju
idempotentno.

## Što nije testirano

Maven nije dostupan. Nije proveden fizički test alata, promjera ni G-codea na
ZK-1325 / RichAuto A11.

## Otvorena pitanja

- Potvrditi stvarni broj, naziv i promjer alata na stroju.
- Nakon potvrde odlučiti ostaju li razvojni zapisi aktivni ili se deaktiviraju.

## Moguće poglavlje završnog rada

Bootstrap referentnih podataka i validacija alata.

## Kandidati za isječke koda

### Kandidat: Idempotentno dodavanje razvojnih alata

**Datoteka:** `src/main/java/hr/lukabosnjak/service/V1BootstrapService.java`  
**Klasa/metoda:** `V1BootstrapService#ensureSoftwareTestTools`  
**Zašto je važan:** Pokazuje kako se razvojni podaci dodaju bez dupliciranja i
  bez izmišljanja nepoznatih atributa alata.  
**Moguće poglavlje:** Persistence i inicijalizacija podataka

```java
for (double diameter : SOFTWARE_TEST_TOOL_DIAMETERS) {
    int toolNumber = diameter == 6.0 ? 9006 : 9008;
    if (toolRepository.findByMachineIdAndToolNumber(machine.getCncMachineId(), toolNumber).isEmpty()) {
        toolRepository.save(new Tool(
                null, machine, toolNumber,
                "TESTNI ALAT Ø" + (int) diameter + " mm (SOFTVERSKI)",
                null, diameter, null, null, true, null, null));
    }
}
```

## Kandidat za sliku, dijagram ili tablicu

Tablica razvojnih testnih alata i njihovog statusa potvrde.

## Git commit/hash

Nema.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne
putanje.
