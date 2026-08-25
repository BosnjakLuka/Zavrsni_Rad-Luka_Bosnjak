# H2 Console za razvoj

H2 Console je pomoćni razvojni alat za ručni pregled lokalne baze. Nije runtime ni poslovna komponenta aplikacije i aplikacija ne ovisi o pokretanju Consolea.

## Pokretanje u IntelliJ IDEA-i

1. Kreiraj privremenu **Application** run konfiguraciju.
2. Kao main klasu postavi `org.h2.tools.Console`.
3. Za classpath odaberi modul projekta `cnc-optimizer` kako bi H2 Maven dependency bio dostupan.
4. Kao working directory postavi korijenski direktorij projekta.
5. Pokreni konfiguraciju i u Consoleu se spoji s:
   - JDBC URL: `jdbc:h2:file:./data/cnc-optimizer`
   - User Name: `sa`
   - Password: ostavi prazno

Relativni dio `./data/cnc-optimizer` računa se od working directoryja Java procesa. Ako se aplikacija ili Console pokrenu iz drugog working directoryja, isti tekst URL-a pokazivat će na drugu fizičku datoteku baze. Zato obje konfiguracije trebaju koristiti isti working directory.

Prije otvaranja iste file-based baze u Consoleu zatvori aplikaciju i sve njezine JDBC veze. Produkcijski kod koji pozove `DatabaseConfig.getConnection()` treba dobivenu vezu zatvoriti pomoću try-with-resources.

Driver Class:
org.h2.Driver

Ispravan apsolutni JDBC URL za H2 Console:
jdbc:h2:file:C:/Users/lukab/Documents/Projekt/Zavrsni_Rad-Luka_Bosnjak/data/cnc-optimizer

User Name:
sa

Password:
prazno

## Provjera da je otvorena prava baza

Aplikacijska baza zove se `cnc-optimizer`. Naziv `cnc_optimizer` s donjom crtom nije ista baza i H2 ce za takav URL otvoriti ili stvoriti drugu datoteku baze.

Ne koristiti:
`jdbc:h2:C:/Users/lukab/Documents/Projekt/Zavrsni_Rad-Luka_Bosnjak/data/cnc_optimizer`

Ako Console nakon spajanja prikazuje samo `INFORMATION_SCHEMA` i `Users`, najvjerojatnije je otvorena pogresna fizicka baza ili aplikacija nije inicijalizirala shemu u toj bazi.

Nakon spajanja pokreni:

```sql
SHOW TABLES;
```

U ispravnoj aplikacijskoj bazi trebaju se vidjeti tablice poput `ROLE`, `APP_USER`, `MATERIAL_TYPE`, `CNC_MACHINE`, `TOOL`, `SHAPE` i `MACHINING_JOB`.
