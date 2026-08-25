# 4.3 — MachiningJob agregat

**Datum:** 2026-08-25
**Status:** IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO

## Cilj

Dodati završni osnovni domenski agregat koji na jednom mjestu povezuje potvrđene podatke i reference jednog CNC posla, bez persistence ili G-code generatorske logike.

## Promijenjene datoteke

- `src/main/java/hr/lukabosnjak/domain/entities/MachiningJob.java` — dodan je agregat jednog CNC posla.
- `Dokumentacija/biljeske/00_odluke.md` — zabilježena je granica između snapshot podataka i postojećih dijeljenih referenci.
- `Dokumentacija/biljeske/00_indeks.md` — dodan je ovaj razvojni korak.
- `Dokumentacija/biljeske/04-03-machining-job-aggregate.md` — dokumentirani su implementacija i stvarne provjere.

## Stvarna implementacija

`MachiningJob` je obična mutable Java klasa s privatnim poljima, punim konstruktorom te standardnim getterima i setterima. Povezuje `User createdBy`, `CncMachine`, `Tool`, `MaterialSheet`, `MachiningParameters` i `Shape`, uz ID, naziv, količinu, spremljeni G-code tekst te vremenske oznake stvaranja i izmjene.

Klasa ne postavlja `quantity` automatski na 1 i ne validira ga. Ne generira sadržaj `gCode`; polje samo prima i čuva tekst koji će proizvesti budući gcode sloj.

## Razlog odabranog rješenja

`MaterialSheet`, `MachiningParameters` i `Shape` čine snapshot jednog posla jer njihove vrijednosti opisuju konkretno planiranu obradu. Takav zapis kasnije omogućuje da se posao ponovno učita s izvornim dimenzijama, parametrima i oblikom, umjesto da ovisi o naknadnim izmjenama drugih poslova.

`User`, `CncMachine` i `Tool` dijeljeni su master zapisi i očekuje se da već postoje u bazi; isto vrijedi za `MaterialType` na koji pokazuje snapshot ploče. Budući repository zato neće tiho duplicirati te reference pri svakom spremanju posla.

## Arhitektonska povezanost

Agregat pripada `domain.entities` i ovisi samo o drugim domenskim entitetima te `java.time.LocalDateTime`. Ne sadrži JavaFX, JDBC, H2, ORM, SQL, geometriju, layout ni G-code generator. Transakcija spremanja pripada budućem persistence sloju, a pravila workflowa service/validation slojevima.

## Važne odluke i ograničenja

- `MaterialSheet`, `MachiningParameters` i `Shape` budući su 1:1 snapshot zapisi posla.
- `User`, `CncMachine`, `Tool` i povezani `MaterialType` moraju već postojati prije budućeg spremanja posla.
- Buduća validacija mora potvrditi da `Tool.cncMachine` odgovara stroju odabranom u poslu.
- Iteracija 1 koristi samo `quantity = 1`; model ne uvodi quantity UI ni hardkodira tu vrijednost.
- `gCode` je podatkovno polje, a ne generator. SQL, transakcije i round-trip nisu dio ovog milestonea.

## Build i testiranje

### Izvršene naredbe

```text
mvn -Dmaven.repo.local=<privremeni-repozitorij> clean test
javap -classpath target/classes -private hr.lukabosnjak.domain.entities.MachiningJob
rg -n -i "^import\s+(javafx|java.sql|javax.persistence|jakarta.persistence|org.hibernate)|PreparedStatement|GCodeGenerator|generateGCode|\"\s*(SELECT|INSERT|UPDATE|DELETE)\s" src/main/java/hr/lukabosnjak/domain/entities/MachiningJob.java
```

### Stvarni rezultat

Maven je izvan sandbox ograničenja, uz Oracle OpenJDK 26.0.2.1, kompilirao svih dvanaest glavnih izvora s Java releaseom 26 i završio s `BUILD SUCCESS`. Projekt nema testnih klasa. `javap` je potvrdio sva tražena polja, tipove, puni konstruktor i pristupne metode. Statička provjera nije pronašla JavaFX, JDBC, ORM, SQL ni G-code generatorske ovisnosti.

### Što nije testirano

Nisu testirani validation, service workflow, `quantity = 1` pravilo, generiranje G-koda, SQL, transakcijsko spremanje snapshotova, JDBC/H2 round-trip ni fizičko ponašanje na ZK-1325. Nisu dodani getter/setter testovi jer agregat još nema poslovne izračune ili grananja.

## Otvorena pitanja

- Točan transakcijski redoslijed spremanja snapshot zapisa i posla definirat će se uz `MachiningJobRepository`.
- Početna vrijednost i životni ciklus `gCode` polja definirat će se u budućem generation/service workflowu.

## Moguće poglavlje završnog rada

Domenski model CNC posla i granica agregata.

## Kandidati za isječke koda

> Kandidat za isječak koda: nema — agregat trenutačno sadrži samo potvrđene reference, podatkovna polja, puni konstruktor i standardne pristupne metode.

## Kandidat za sliku, dijagram ili tablicu

- `Dokumentacija/Dijagrami/finalna-vezija_uml_dijagrama.drawio.png` — prikazuje sastav `MachiningJob` modela.
- `Dokumentacija/Dijagrami/finalna_relacija_baza.drawio.png` — prikazuje planirane master i snapshot relacije za budući persistence sloj.

## Git commit/hash

Nema — za ovaj korak nije stvoren commit.

## Sigurnost zapisa

Bilješka ne sadrži tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
