# Indeks razvojnih bilješki

Kronološki katalog dokumentiranih razvojnih koraka. Nakon svakog dokumentiranog koraka dodaje se ili ažurira odgovarajući redak.

| Prompt | Tema | Status | Glavne datoteke | Testirano | Kandidat za završni rad | Bilješka |
|---|---|---|---|---|---|---|
| 1.2 | Minimalni Maven projekt | IMPLEMENTIRANO / TESTIRANO | `pom.xml`, `AGENTS.md` | `mvn test` — BUILD SUCCESS; nema testnih klasa | Tablica tehnologija; nema isječka koda | [01-02-minimalni-maven-projekt.md](01-02-minimalni-maven-projekt.md) |
| 1.3 | Minimalni JavaFX smoke test | IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO | `Main.java` | Compile i JavaFX runtime testirani; Stage nije vizualno potvrđen | `Main#main`, `Main#start`; snimka Stagea nakon potvrde | [01-03-minimalni-javafx-smoke-test.md](01-03-minimalni-javafx-smoke-test.md) |
| 1.4 | Osnovna package struktura | IMPLEMENTIRANO / TESTIRANO | `Main.java`, `pom.xml` | `mvn test` — rezultat zabilježen u bilješci | Nema isječka; arhitektonski dijagram | [01-04-osnovna-package-struktura.md](01-04-osnovna-package-struktura.md) |
| 1.5 | Domenska klasa MaterialType | IMPLEMENTIRANO / TESTIRANO | `MaterialType.java` | `mvn clean test` — BUILD SUCCESS; nema unit testova | Nema smislenog isječka | [01-05-material-type.md](01-05-material-type.md) |
| 1.6 | Slojevita package konvencija | IMPLEMENTIRANO / TESTIRANO | `MaterialType.java` | `mvn clean test` — BUILD SUCCESS; nema testnih klasa | Nema isječka ni postojećeg dijagrama | [01-06-slojevita-package-konvencija.md](01-06-slojevita-package-konvencija.md) |
| 1.7 | Domain entities package | IMPLEMENTIRANO / TESTIRANO | `MaterialType.java` | `mvn clean test` — BUILD SUCCESS; nema testnih klasa | Nema smislenog isječka | [01-07-domain-entities-package.md](01-07-domain-entities-package.md) |
| 3.2 | Domenska klasa MaterialSheet | IMPLEMENTIRANO / TESTIRANO / ZAMIJENJENO 3.1 | `MaterialSheet.java`, `MaterialSheetTest.java` | `mvn clean test` — 8 testova prošlo; BUILD SUCCESS | Povijesna domenska invarijanta dimenzija | [03-02-material-sheet.md](03-02-material-sheet.md) |
| 3.1 | Osnovni domenski modeli | IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO | šest klasa u `domain.entities` | `mvn clean test` — BUILD SUCCESS; nema unit testova | Nema smislenog isječka; UML domenskog modela | [03-01-domain-core.md](03-01-domain-core.md) |
| 4.1 | Shape enumovi i entitet | IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO | `ShapeType.java`, `ShapeSubtype.java`, `Shape.java` | `mvn clean test` — BUILD SUCCESS; nema unit testova | UML domenskog modela; nema smislenog isječka | [04-01-shape-enums-entity.md](04-01-shape-enums-entity.md) |
| 4.2 | Privremeni tip alata i Tool | IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO | `Tool.java` | `mvn clean test` — BUILD SUCCESS; nema unit testova | UML domenskog modela; nema smislenog isječka | [04-02-tool-type-decision-tool.md](04-02-tool-type-decision-tool.md) |
| 4.3 | MachiningJob agregat | IMPLEMENTIRANO / TESTIRANO / NIJE TESTIRANO | `MachiningJob.java` | `mvn clean test` — BUILD SUCCESS; nema unit testova | UML/relacijski dijagram; nema smislenog isječka | [04-03-machining-job-aggregate.md](04-03-machining-job-aggregate.md) |
