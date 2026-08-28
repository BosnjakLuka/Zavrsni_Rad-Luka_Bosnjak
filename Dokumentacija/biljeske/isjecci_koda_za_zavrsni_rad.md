# Isječci koda za završni rad — dokumentacijski audit

**Datum:** 2026-08-28
**Status:** IMPLEMENTIRANO / DOKUMENTACIJSKI PREGLEDANO / TESTOVI NISU PONOVNO IZVRŠENI U OVOM AUDITU

Ovaj dokument izdvaja mali broj akademski korisnih isječaka iz aktualnog produkcijskog koda. Pregledano je 26 izričito zadanih kandidata. Pomoćne datoteke, H2 shema, produkcijski composition root, glavni UI tok i postojeći testovi korišteni su za provjeru konteksta i statusa, ali nisu pribrojeni kandidatima. Testni kod nije korišten kao glavni isječak implementacije.

| # | Predloženi naslov | Poglavlje | Datoteka / metoda | Prioritet |
|---:|---|---|---|---|
| 1 | Sigurna pohrana i provjera lozinke PBKDF2 algoritmom | 6.1 Autentikacija, korisnička sesija i uloge | `PasswordHasher.java` — `hash`, `verify` | OPCIONALNO uključiti |
| 2 | Poslovna pravila odabira alata i CNC stroja | 6.2 Upravljanje referentnim podacima / 6.4 Validacija podataka | `ReferenceDataManagementService#createTool`; `MachiningJobValidator#validate`, `belongsTo` | OPCIONALNO uključiti |
| 3 | Orkestracija generiranja programa za jedan element | 6.3 Priprema proizvodnog naloga | `ProgramGenerationService#generate` | OBAVEZNO uključiti |
| 4 | Modeliranje kružnice dvama lučnim segmentima | 6.5 Izrada geometrijske putanje | `ToolPathService#generateCircle` | OBAVEZNO uključiti |
| 5 | Geometrijska kompenzacija polumjera alata | 6.6 Kompenzacija alata i provjera granica | `ToolPathCompensationService#compensate`, `compensateLines`, `compensateArcs` | OBAVEZNO uključiti |
| 6 | Transakcijsko spremanje snapshot podataka naloga | 6.7 Spremanje i ponovno otvaranje naloga | `JdbcMachiningJobRepository#save` | OBAVEZNO uključiti |
| 7 | Konfiguracija referentnog RichAuto A11 profila | 7.1 RichAuto A11 profil | `RichAutoA11Profile#referenceProgramProfile` | OBAVEZNO uključiti |
| 8 | Pretvorba linijskih i lučnih segmenata u G-kod | 7.2 Pretvorba ToolPath segmenata u G-kod | `RichAutoA11GCodeGenerator#cuttingMove`, `linearMove`, `arcMove` | OBAVEZNO uključiti |
| 9 | Izračun više dubinskih prolaza | 7.3 Više dubinskih prolaza | `PassDepthCalculator#calculate` | OBAVEZNO uključiti |
| 10 | Siguran izvoz CNC programa u `.nc` datoteku | 7.5 Formatiranje i `.nc` export | `NcExportService#export`, `requireNcExtension`, `encodeAscii` | OBAVEZNO uključiti |

## 1. Sigurna pohrana i provjera lozinke PBKDF2 algoritmom

1. **Predloženi broj i naslov:** Isječak 6.1 — Sigurna pohrana i provjera lozinke PBKDF2 algoritmom.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.1 Autentikacija, korisnička sesija i uloge.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/service/PasswordHasher.java`.
4. **Klasa i metode:** `PasswordHasher#hash`, `PasswordHasher#verify`.
5. **Stvarni isječak koda:**

```java
public String hash(char[] password) {
    requirePassword(password);
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);
    byte[] derived = derive(password, salt, ITERATIONS, KEY_BITS);
    try {
        return ALGORITHM_ID + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    } finally {
        Arrays.fill(derived, (byte) 0);
    }
}

public boolean verify(char[] password, String encodedHash) {
    if (password == null || password.length == 0 || encodedHash == null) {
        return false;
    }
    try {
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !ALGORITHM_ID.equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        if (iterations <= 0 || iterations > MAX_STORED_ITERATIONS) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        if (salt.length < SALT_BYTES || expected.length == 0) {
            return false;
        }
        byte[] actual = derive(password, salt, iterations, expected.length * Byte.SIZE);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(actual, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            Arrays.fill(salt, (byte) 0);
        }
    } catch (IllegalArgumentException exception) {
        return false;
    }
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANO je generiranje nasumičnog salta, izvedba ključa PBKDF2 algoritmom, samodostatan verzionirani zapis i provjera pomoću `MessageDigest.isEqual`. Privremeni bajtovni spremnici brišu se nakon uporabe.
7. **Akademska vrijednost:** Isječak pokazuje konkretnu sigurnosnu odluku i razliku između pohrane izvedenog ključa i pohrane izvorne lozinke. Povezuje kriptografski mehanizam sa service slojem autentikacije.
8. **Što se smije tvrditi:** Smije se tvrditi da su hashiranje i provjera IMPLEMENTIRANI te da postoje testovi `PasswordHasherTest` za različite saltove, ispravnu lozinku i nevaljane zapise. Prema razvojnoj bilješci 13.2 ti su testovi prethodno TESTIRANI.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da je rješenje prošlo neovisni sigurnosni audit, penetracijsko testiranje ili da je otporno na sve napade. Testovi nisu ponovno izvršeni u ovom dokumentacijskom auditu.
10. **Preporuka:** OPCIONALNO uključiti — tehnički je vrijedan, ali nije središnji CNC algoritam rada.

## 2. Poslovna pravila odabira alata i CNC stroja

1. **Predloženi broj i naslov:** Isječak 6.2 — Poslovna pravila odabira alata i CNC stroja.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.2 Upravljanje referentnim podacima i 6.4 Validacija podataka.
3. **Izvorne datoteke:** `src/main/java/hr/lukabosnjak/service/ReferenceDataManagementService.java` i `src/main/java/hr/lukabosnjak/validation/MachiningJobValidator.java`.
4. **Klase i metode:** `ReferenceDataManagementService#createTool`; `MachiningJobValidator#validate`, `MachiningJobValidator#belongsTo`.
5. **Stvarni isječak koda:**

```java
public Tool createTool(Tool tool) {
    requireCatalogAccess();
    Objects.requireNonNull(tool, "tool");
    normalize(tool);
    validator.validate(tool);
    try {
        if (toolRepository.findByMachineIdAndToolNumber(
                tool.getCncMachine().getCncMachineId(), tool.getToolNumber()).isPresent()) {
            throw new ValidationException("Odabrani CNC stroj već ima alat s tim brojem.");
        }
        return toolRepository.save(tool);
    } catch (SQLException exception) {
        throw new ReferenceDataAccessException("Spremanje alata nije uspjelo.", exception);
    }
}
```

```java
Tool tool = job.getTool();
if (tool == null) {
    throw new ValidationException("Alat mora biti odabran.");
}
if (!belongsTo(tool, machine)) {
    throw new ValidationException("Odabrani alat ne pripada odabranom CNC stroju.");
}
if (!tool.isActive()) {
    throw new ValidationException("Odabrani alat nije aktivan.");
}
...
private boolean belongsTo(Tool tool, CncMachine selectedMachine) {
    CncMachine toolMachine = tool.getCncMachine();
    if (toolMachine == selectedMachine) {
        return true;
    }
    return toolMachine != null
            && toolMachine.getCncMachineId() != null
            && selectedMachine.getCncMachineId() != null
            && toolMachine.getCncMachineId().equals(selectedMachine.getCncMachineId());
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANA su dva povezana poslovna pravila: broj alata jedinstven je u kontekstu odabranog stroja pri stvaranju, a generiranje prihvaća samo aktivan alat povezan s odabranim strojem. H2 shema dodatno definira `UNIQUE (cnc_machine_id, tool_number)` i time štiti jedinstvenost i pri drugim upisima.
7. **Akademska vrijednost:** Isječak jasno razlikuje katalog referentnih podataka od validacije proizvodnog naloga te pokazuje da poslovno značenje broja alata ovisi o stroju, a ne o cijeloj aplikaciji.
8. **Što se smije tvrditi:** Smije se tvrditi da su pravila IMPLEMENTIRANA. Postoje `ReferenceDataManagementServiceTest` i `ProgramGenerationServiceTest` za duplikat broja alata odnosno nepripadanje stroju; ranije bilješke ih navode kao TESTIRANE.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da `createTool` sam štiti sve update putove. Pri ažuriranju konačnu zaštitu jedinstvenosti daje H2 ograničenje. Ne smije se tvrditi da tip ili broj alata predstavljaju fizički potvrđenu konfiguraciju ciljnog stroja.
10. **Preporuka:** OPCIONALNO uključiti — vrijedan je kao primjer poslovne validacije, ali se može izostaviti ako je opseg poglavlja 6 ograničen.

## 3. Orkestracija generiranja programa za jedan element

1. **Predloženi broj i naslov:** Isječak 6.3 — Orkestracija generiranja programa za jedan element.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.3 Priprema proizvodnog naloga.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/service/ProgramGenerationService.java`.
4. **Klasa i metoda:** `ProgramGenerationService#generate`.
5. **Stvarni isječak koda:**

```java
public GCodeProgram generate(ProgramGenerationRequest request) {
    Objects.requireNonNull(request, "request");

    MachiningJob job = new MachiningJob(
            null, null, request.machine(), request.tool(), request.materialSheet(),
            request.machiningParameters(), request.shape(), "Preview", SINGLE_ELEMENT_QUANTITY,
            null, null, null);
    machiningJobValidator.validate(job);

    ToolPath programmedContour = toolPathService.generate(request.shape());
    ToolPath cutterCenterPath = toolPathCompensationService.compensate(
            programmedContour, request.tool(), request.cutSide());
    singleShapeFitValidator.validate(cutterCenterPath, request.materialSheet(), request.machine());
    return gCodeGenerator.generate(cutterCenterPath, request.machiningParameters());
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRAN je stvarni redoslijed `validacija → ToolPath → geometrijska kompenzacija → fit provjera kompenzirane putanje → GCodeGenerator`. Privremeni nalog ima internu količinu 1.
7. **Akademska vrijednost:** Ovo je najsažetiji dokaz slojevite arhitekture i granica odgovornosti. Service koordinira postupak, dok pojedinačni algoritmi ostaju u validation, geometry i gcode slojevima.
8. **Što se smije tvrditi:** Smije se tvrditi da generator prima već kompenziranu i provjerenu putanju centra alata te da je single-element tok IMPLEMENTIRAN. `ProgramGenerationServiceTest` i integracijski test četiriju oblika postoje i u bilješkama su označeni kao TESTIRANI.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da metoda računa layout, optimalan raspored, kapacitet ploče, broj potrebnih ploča ili batch program. Quantity/layout funkcionalnosti su BUDUĆI RAZVOJ.
10. **Preporuka:** OBAVEZNO uključiti.

## 4. Modeliranje kružnice dvama lučnim segmentima

1. **Predloženi broj i naslov:** Isječak 6.4 — Modeliranje kružnice dvama lučnim segmentima.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.5 Izrada geometrijske putanje.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathService.java`.
4. **Klasa i metoda:** `ToolPathService#generateCircle`.
5. **Stvarni isječak koda:**

```java
private ToolPath generateCircle(double diameter) {
    double radius = diameter / 2;
    Point2 center = new Point2(radius, radius);
    Point2 left = new Point2(0, radius);
    Point2 right = new Point2(diameter, radius);

    return new ToolPath(List.of(
            new ArcSegment(left, right, center, ArcDirection.COUNTERCLOCKWISE),
            new ArcSegment(right, left, center, ArcDirection.COUNTERCLOCKWISE)));
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANA je kružna kontura kao dvije povezane polukružnice s eksplicitnim centrom i smjerom. Lokalni bounding prostor kružnice proteže se od 0 do promjera po objema osima.
7. **Akademska vrijednost:** Primjer objašnjava kako se domenska dimenzija pretvara u geometrijsku reprezentaciju neovisnu o tekstualnom G-kodu i zašto se izbjegava full-circle segment s jednakim početkom i krajem.
8. **Što se smije tvrditi:** Smije se tvrditi da su dva `ArcSegment` zapisa IMPLEMENTIRANA te da `ToolPathServiceTest` provjerava zatvorenost i očekivane polukružnice. Softversko generiranje svih četiriju oblika zabilježeno je kao TESTIRANO.
9. **Što se ne smije tvrditi:** `COUNTERCLOCKWISE` je geometrijski smjer, a ne dokaz univerzalno ispravne fizičke strategije rezanja. Fizički test pravokutnog `test01.nc` ne potvrđuje kružne `G03` naredbe.
10. **Preporuka:** OBAVEZNO uključiti.

## 5. Geometrijska kompenzacija polumjera alata

1. **Predloženi broj i naslov:** Isječak 6.5 — Geometrijska kompenzacija polumjera alata.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.6 Kompenzacija alata i provjera granica.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/geometry/ToolPathCompensationService.java`.
4. **Klasa i metode:** `ToolPathCompensationService#compensate`, `compensateLines`, `compensateArcs`.
5. **Stvarni isječak koda:**

```java
public ToolPath compensate(ToolPath programmedContour, Tool tool, CutSide cutSide) {
    Objects.requireNonNull(programmedContour, "programmedContour");
    Objects.requireNonNull(tool, "tool");
    Objects.requireNonNull(cutSide, "cutSide");

    double radius = tool.getDiameter() / 2.0;
    if (!Double.isFinite(radius) || radius <= 0.0) {
        throw new IllegalArgumentException("Tool diameter must be positive and finite");
    }

    if (programmedContour.segments().stream().allMatch(LineSegment.class::isInstance)) {
        return compensateLines(programmedContour, radius, cutSide);
    }
    if (programmedContour.segments().stream().allMatch(ArcSegment.class::isInstance)) {
        return compensateArcs(programmedContour, radius, cutSide);
    }
    throw new IllegalArgumentException("Mixed line and arc contours are not supported");
}

...
boolean counterClockwise = signedArea > 0.0;
boolean leftSide = cutSide == CutSide.INSIDE ? counterClockwise : !counterClockwise;
List<OffsetLine> offsetLines = lines.stream()
        .map(line -> offsetLine(line, leftSide ? radius : -radius))
        .toList();
...
boolean inward = (direction == ArcDirection.COUNTERCLOCKWISE) == (cutSide == CutSide.INSIDE);
double compensatedRadius = originalRadius + (inward ? -radius : radius);
```

6. **Što isječak pokazuje:** IMPLEMENTIRANA je aplikacijska kompenzacija iz nominalne konture u putanju centra alata. Servis podržava `CutSide.INSIDE` i `CutSide.OUTSIDE` za homogene linijske i kružne konture. Produkcijski `MainFormController#readGenerationRequest` trenutačno uvijek prosljeđuje `CutSide.INSIDE`.
7. **Akademska vrijednost:** Isječak prikazuje geometrijski offset prije tekstualnog generatora, korištenje orijentacije konture za odabir strane linijskog offseta i radijalnu promjenu kružne konture.
8. **Što se smije tvrditi:** Smije se tvrditi da su `INSIDE` i `OUTSIDE` IMPLEMENTIRANI. `ToolPathCompensationServiceTest` sadrži softverske testove obiju strana za pravokutnik, a integracijski tok četiriju oblika testira `INSIDE`; prema bilješkama ti su testovi TESTIRANI.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da UI omogućuje odabir `OUTSIDE`, da su podržane miješane line/arc konture ili proizvoljne samopresijecajuće konture. Generator ne emitira `G41`, `G42`, `G40` ni `D`; fizički test jednog unutarnje kompenziranog pravokutnika ne potvrđuje vanjsku kompenzaciju.
10. **Preporuka:** OBAVEZNO uključiti.

## 6. Transakcijsko spremanje snapshot podataka naloga

1. **Predloženi broj i naslov:** Isječak 6.6 — Transakcijsko spremanje snapshot podataka naloga.
2. **Poglavlje/podpoglavlje:** 6. Implementacija aplikacije — 6.7 Spremanje i ponovno otvaranje naloga.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/persistence/jdbc/JdbcMachiningJobRepository.java`.
4. **Klasa i metoda:** `JdbcMachiningJobRepository#save`.
5. **Stvarni isječak koda:**

```java
@Override
public MachiningJob save(MachiningJob job) throws SQLException {
    validateNewJob(job);

    try (Connection connection = connectionProvider.getConnection()) {
        connection.setAutoCommit(false);
        try {
            long materialSheetId = materialSheetRepository.insertOnOpenConnection(
                    connection, job.getMaterialSheet());
            long machiningParametersId = machiningParametersRepository.insertOnOpenConnection(
                    connection, job.getMachiningParameters());
            long shapeId = shapeRepository.insertOnOpenConnection(connection, job.getShape());
            long jobId = insertJob(connection, job, materialSheetId, machiningParametersId, shapeId);
            MachiningJob savedJob = findById(connection, jobId)
                    .orElseThrow(() -> new SQLException("Saved machining job was not found"));
            connection.commit();
            return savedJob;
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        }
    }
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANO je spremanje snapshot zapisa ploče, parametara i oblika te završnog naloga unutar jedne JDBC transakcije. Agregat se ponovno učitava na istoj vezi prije `commit` operacije, a pogreška uzrokuje `rollback`.
7. **Akademska vrijednost:** Isječak pokazuje praktičnu primjenu atomske transakcije i razlog zašto snapshot zapisi ne smiju ostati djelomično spremljeni.
8. **Što se smije tvrditi:** Smije se tvrditi da su `setAutoCommit(false)`, snapshot inserti, `commit` i `rollback` IMPLEMENTIRANI. `JdbcMachiningJobRepositoryIntegrationTest` sadrži round-trip i rollback scenarij te je prema razvojnoj dokumentaciji TESTIRAN.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da repository sprema layout, batch podatke ili više elemenata. Ponovno otvaranje rekonstruira spremljeni snapshot; ne dokazuje postojanje update ili soft-delete toka.
10. **Preporuka:** OBAVEZNO uključiti.

## 7. Konfiguracija referentnog RichAuto A11 profila

1. **Predloženi broj i naslov:** Isječak 7.1 — Konfiguracija referentnog RichAuto A11 profila.
2. **Poglavlje/podpoglavlje:** 7. Generiranje G-koda — 7.1 RichAuto A11 profil.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11Profile.java`.
4. **Klasa i metoda:** `RichAutoA11Profile#referenceProgramProfile`.
5. **Stvarni isječak koda:**

```java
public static RichAutoA11Profile referenceProgramProfile() {
    return new RichAutoA11Profile(
            true,
            false,
            true,
            true,
            Units.MILLIMETERS,
            PositioningMode.ABSOLUTE,
            3,
            ZCoordinateConvention.MATERIAL_SURFACE_ZERO_NEGATIVE_CUT,
            ArcCenterMode.RELATIVE_TO_ARC_START,
            Set.of());
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANI reference profil emitira feed, `G54` i spindle naredbe, ne emitira `S`, koristi milimetre, apsolutno pozicioniranje, tri decimale, negativni rezni Z i I/J relativne početku luka. Prazan `Set.of()` znači da profil u kodu ne označava nijednu mogućnost kao fizički potvrđenu.
7. **Akademska vrijednost:** Centralizirana konfiguracija odvaja format izlaza od algoritma generiranja i, barem na razini modela, odvaja emitiranje naredbe od evidencije fizičke potvrde.
8. **Što se smije tvrditi:** Smije se tvrditi da je ovaj profil IMPLEMENTIRAN i da ga produkcijski `ApplicationCompositionRoot` predaje generatoru. `RichAutoA11ProfileTest` i generator testovi postoje i u bilješkama su označeni kao TESTIRANI. Bilješka 18.A navodi da je konkretni aplikacijski pravokutni `test01.nc` fizički TESTIRAN na ZK-1325 / RichAuto A11 prema potvrdi operatora.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da prazan `physicallyConfirmedCapabilities` odražava bilješku 18.A niti da jedan fizički test potvrđuje sve naredbe, lukove, Z-konvencije, alate, work zero postavke ili sve RichAuto A11 konfiguracije.
10. **Preporuka:** OBAVEZNO uključiti.

## 8. Pretvorba linijskih i lučnih segmenata u G-kod

1. **Predloženi broj i naslov:** Isječak 7.2 — Pretvorba linijskih i lučnih segmenata u G-kod.
2. **Poglavlje/podpoglavlje:** 7. Generiranje G-koda — 7.2 Pretvorba ToolPath segmenata u G-kod.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/gcode/RichAutoA11GCodeGenerator.java`.
4. **Klasa i metode:** `RichAutoA11GCodeGenerator#cuttingMove`, `linearMove`, `arcMove`.
5. **Stvarni isječak koda:**

```java
private String cuttingMove(PathSegment segment) {
    return switch (segment) {
        case LineSegment line -> linearMove(line);
        case ArcSegment arc -> arcMove(arc);
    };
}

private String linearMove(LineSegment line) {
    return "G01 X" + formatter.format(line.end().x())
            + " Y" + formatter.format(line.end().y());
}

private String arcMove(ArcSegment arc) {
    double centerOffsetI = switch (profile.arcCenterMode()) {
        case RELATIVE_TO_ARC_START -> arc.center().x() - arc.start().x();
    };
    double centerOffsetJ = switch (profile.arcCenterMode()) {
        case RELATIVE_TO_ARC_START -> arc.center().y() - arc.start().y();
    };

    String command = switch (arc.direction()) {
        case CLOCKWISE -> "G02";
        case COUNTERCLOCKWISE -> "G03";
    };
    return command
            + " X" + formatter.format(arc.end().x())
            + " Y" + formatter.format(arc.end().y())
            + " I" + formatter.format(centerOffsetI)
            + " J" + formatter.format(centerOffsetJ);
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRANO je tipizirano mapiranje `LineSegment → G01` i `ArcSegment → G02/G03`. Završne X/Y koordinate dolaze iz segmenta, a I/J se računaju kao relativni pomak centra od početka luka.
7. **Akademska vrijednost:** Ovo je ključna granica između geometrijskog modela i tekstualnog CNC jezika. Isječak jasno pokazuje da generator ne rekonstruira oblik, nego prevodi već pripremljene segmente.
8. **Što se smije tvrditi:** Smije se tvrditi da su linearne i lučne naredbe IMPLEMENTIRANE. `RichAutoA11GCodeGeneratorTest` sadrži provjere `G01`, oba smjera luka i relativne I/J vrijednosti te je prema bilješkama softverski TESTIRAN.
9. **Što se ne smije tvrditi:** Ne smije se tvrditi da su lučne naredbe fizički TESTIRANE na temelju pravokutnog `test01.nc`. Generator ne emitira `G41/G42` i ne računa kompenzaciju ili layout.
10. **Preporuka:** OBAVEZNO uključiti.

## 9. Izračun više dubinskih prolaza

1. **Predloženi broj i naslov:** Isječak 7.3 — Izračun više dubinskih prolaza sa završetkom na ciljnoj dubini.
2. **Poglavlje/podpoglavlje:** 7. Generiranje G-koda — 7.3 Više dubinskih prolaza.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/gcode/PassDepthCalculator.java`.
4. **Klasa i metoda:** `PassDepthCalculator#calculate`.
5. **Stvarni isječak koda:**

```java
public static List<Double> calculate(double cutDepth, double stepDown) {
    requirePositiveFinite(cutDepth, "Cut depth");
    requirePositiveFinite(stepDown, "Step down");

    BigDecimal target = BigDecimal.valueOf(cutDepth);
    BigDecimal step = BigDecimal.valueOf(stepDown);
    BigDecimal[] division = target.divideAndRemainder(step);
    BigInteger passCount = division[0].toBigIntegerExact();
    if (division[1].signum() != 0) {
        passCount = passCount.add(BigInteger.ONE);
    }
    if (passCount.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
        throw new IllegalArgumentException("Requested pass count exceeds the supported list size");
    }

    int count = passCount.intValueExact();
    List<Double> depths = new ArrayList<>(count);
    for (int pass = 1; pass < count; pass++) {
        depths.add(step.multiply(BigDecimal.valueOf(pass)).doubleValue());
    }
    depths.add(cutDepth);
    return List.copyOf(depths);
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRAN je izračun broja kumulativnih prolaza bez binarne floating-point odluke o ostatku. Posljednja vrijednost uvijek se izravno postavlja na `cutDepth`.
7. **Akademska vrijednost:** Isječak pokazuje izbor `BigDecimal` aritmetike radi izbjegavanja dodatnog prolaza kod decimalnih vrijednosti te zaštitu od neprihvatljivo velikog broja prolaza.
8. **Što se smije tvrditi:** Smije se tvrditi da svaki rezultat završava točno zadanom domenskom dubinom i da su nevaljane vrijednosti odbijene. `PassDepthCalculatorTest` sadrži testove djeljivih i nedjeljivih dubina, decimalnog step-downa i nevaljanih vrijednosti; prema bilješkama je TESTIRAN.
9. **Što se ne smije tvrditi:** Ova metoda ne bira tehnološki optimalan step-down, ne provjerava mogućnosti konkretnog alata ili materijala i ne potvrđuje fizičku sigurnost machining parametara.
10. **Preporuka:** OBAVEZNO uključiti.

## 10. Siguran izvoz CNC programa u `.nc` datoteku

1. **Predloženi broj i naslov:** Isječak 7.4 — Siguran izvoz CNC programa u `.nc` datoteku.
2. **Poglavlje/podpoglavlje:** 7. Generiranje G-koda — 7.5 Formatiranje i `.nc` export.
3. **Izvorna datoteka:** `src/main/java/hr/lukabosnjak/gcode/NcExportService.java`.
4. **Klasa i metode:** `NcExportService#export`, `requireNcExtension`, `encodeAscii`.
5. **Stvarni isječak koda:**

```java
public void export(GCodeProgram program, Path destination) throws IOException {
    Objects.requireNonNull(program, "program");
    Objects.requireNonNull(destination, "destination");
    requireNcExtension(destination);

    byte[] content = encodeAscii(program.text());
    Files.write(
            destination,
            content,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE);
}

private void requireNcExtension(Path destination) {
    Path fileNamePath = destination.getFileName();
    if (fileNamePath == null) {
        throw new IllegalArgumentException("Destination must have an .nc file name");
    }

    String fileName = fileNamePath.toString();
    int extensionStart = fileName.length() - 3;
    if (extensionStart < 0 || !fileName.regionMatches(true, extensionStart, ".nc", 0, 3)) {
        throw new IllegalArgumentException("Destination file must use the .nc extension");
    }
}

private byte[] encodeAscii(String text) {
    try {
        ByteBuffer encoded = StandardCharsets.US_ASCII.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    } catch (CharacterCodingException exception) {
        throw new IllegalArgumentException("G-code program must contain only US-ASCII characters", exception);
    }
}
```

6. **Što isječak pokazuje:** IMPLEMENTIRAN je izvoz samo u `.nc`, strogo US-ASCII kodiranje i stvaranje nove datoteke opcijom `CREATE_NEW` bez nenamjernog prepisivanja postojećeg odredišta.
7. **Akademska vrijednost:** Isječak pokazuje da export nije samo poziv za zapis teksta, nego eksplicitna granica formata, kodiranja i zaštite korisničke datoteke.
8. **Što se smije tvrditi:** Smije se tvrditi da su `.nc`, US-ASCII i zabrana prepisivanja IMPLEMENTIRANI. `NcExportServiceTest` sadrži round-trip, uppercase ekstenziju, nevaljanu ekstenziju, ne-ASCII sadržaj i postojeće odredište; prema ranijoj dokumentaciji ti su scenariji TESTIRANI.
9. **Što se ne smije tvrditi:** `.nc` ekstenzija i ASCII kodiranje sami ne dokazuju kompatibilnost sa svakim CNC kontrolerom. Testovi nisu ponovno izvršeni u ovom auditu.
10. **Preporuka:** OBAVEZNO uključiti.

## Tehnička ograničenja generatora

- **IMPLEMENTIRANO:** produkcijski tok generira jedan element; `ProgramGenerationRequest` nema quantity, a servis i UI koriste količinu `1`.
- **BUDUĆI RAZVOJ:** quantity unos, layout, kapacitet ploče, broj potrebnih ploča i batch G-kod nisu prisutni u produkcijskom kodu.
- **IMPLEMENTIRANO:** generator zahtijeva povezanu i zatvorenu putanju.
- **IMPLEMENTIRANO:** kompenzacija prihvaća homogene linijske konture ili kružne konture sastavljene od lukova; miješane line/arc konture nisu podržane.
- **IMPLEMENTIRANO:** `ToolPathCompensationService` podržava `INSIDE` i `OUTSIDE`, ali glavni UI/generatorski tok trenutačno uvijek koristi `INSIDE`.
- **IMPLEMENTIRANO:** kompenzacija se računa geometrijski prije generatora. Generator ne emitira `G41`, `G42`, `G40` ni `D` naredbe.
- **IMPLEMENTIRANO:** reference profil ne emitira `S`; emitira feed riječi, `G54` i spindle start/stop naredbe.
- **TESTIRANO:** prema bilješci 18.A konkretni pravokutni `test01.nc` fizički je izvršen na ZK-1325 / RichAuto A11 bez problema prema potvrdi operatora.
- **NIJE TESTIRANO:** iz tog testa nisu potvrđeni svi oblici, `G02/G03`, `OUTSIDE`, sve vrijednosti alata i parametara, orijentacija osi, work zero, sve profilne naredbe ni sve konfiguracije RichAuto A11 kontrolera.
- **NIJE IMPLEMENTIRANO:** generator ne odabire alat, ne određuje tehnološki optimalne machining parametre i ne provjerava fizičku konfiguraciju stroja.

## Preporučeni konačni izbor

Preporučenih 10 isječaka treba u radu poredati ovim redoslijedom:

1. Sigurna pohrana i provjera lozinke PBKDF2 algoritmom — OPCIONALNO uključiti.
2. Poslovna pravila odabira alata i CNC stroja — OPCIONALNO uključiti.
3. Orkestracija generiranja programa za jedan element — OBAVEZNO uključiti.
4. Modeliranje kružnice dvama lučnim segmentima — OBAVEZNO uključiti.
5. Geometrijska kompenzacija polumjera alata — OBAVEZNO uključiti.
6. Transakcijsko spremanje snapshot podataka naloga — OBAVEZNO uključiti.
7. Konfiguracija referentnog RichAuto A11 profila — OBAVEZNO uključiti.
8. Pretvorba linijskih i lučnih segmenata u G-kod — OBAVEZNO uključiti.
9. Izračun više dubinskih prolaza — OBAVEZNO uključiti.
10. Siguran izvoz CNC programa u `.nc` datoteku — OBAVEZNO uključiti.

Ako završni rad mora biti dodatno skraćen, prvo treba izostaviti prva dva OPCIONALNA isječka, a zadržati preostalih osam.

## Kandidati koje ne preporučujem

- `AuthService#login` — korektno koordinira dohvat korisnika, aktivni status, provjeru lozinke i session, ali bi uz `PasswordHasher` ponavljao isto područje bez dodatne CNC vrijednosti.
- `SessionContext` — trivijalni in-memory spremnik trenutačnog korisnika.
- `AuthorizationService#require` — čitljiva RBAC matrica, ali jednostavan switch s manjom tehničkom dubinom od odabranih algoritama.
- `ProgramGenerationRequest` — koristan kao dokaz single-element ulaza, ali record bez logike nije vrijedan samostalnog isječka.
- `NumericInputParser` — kvalitetna UI pomoćna klasa, ali parsiranje zareza/točke i konačnih brojeva slabije predstavlja glavne tehničke odluke.
- `ShapeValidator`, `MaterialSheetValidator` i `MachiningParametersValidator` — važni su za pouzdanost, ali se uglavnom sastoje od ponavljajućih provjera pozitivnih konačnih vrijednosti.
- `SingleShapeFitValidator` — važan je u toku, ali je njegova uloga već jasno vidljiva u `ProgramGenerationService#generate`; zaseban isječak bi povećao broj bez proporcionalne akademske vrijednosti.
- `ToolPath`, `PathSegment` i `LineSegment` — dobri su za UML ili arhitektonski opis, ali pojedinačno su pretežno kratki vrijednosni tipovi.
- `ArcSegment` — važan model, ali njegova se uporaba jasnije vidi u odabranom stvaranju kružnice i G-code mapiranju.
- `ToolPathBoundsCalculator` — tehnički korektno računa kardinalne ekstreme luka, no kompenzacija i segmentno mapiranje važniji su za zadana poglavlja.
- `CutSide` — enum s dvije vrijednosti nije samostalno akademski vrijedan; njegovo značenje prikazano je u kompenzacijskom isječku.
- `SavedJobService` — pretežno delegira repositoryju i rekonstruira `GCodeProgram`; transakcijski JDBC isječak bolje pokazuje persistence odluku.
- `RichAutoA11ProgramEnvelope` — zaglavlje i završetak važni su, ali bi zaseban isječak duplicirao profil i glavni generator. U tekstu rada dovoljno je navesti da reference profil daje `G21`, `G17`, `G90`, `G54`, `M03`, `M05` i `M30`.
- `GCodeFormatter` — determinističko formatiranje je korisno, ali je kratka pomoćna odgovornost i nije jači kandidat od izvoza ili izračuna prolaza.
- `ProgramExportService` — čista service-granica koja delegira `NcExportService`; nema dovoljno vlastite logike.

## Nesukladnosti dokumentacije i aktualnog koda

1. `Dokumentacija/Zavrsni_rad_natuknice_v2.docx` i `Dokumentacija/Tema završnog rada-v2.docx` opisuju quantity, raspoređivanje, broj ploča i batch G-kod kao cilj ili buduću iteraciju. Aktualni produkcijski kod ostaje stabilna single-element implementacija. Te se funkcionalnosti smiju označiti samo kao **BUDUĆI RAZVOJ**, ne kao IMPLEMENTIRANO.
2. Stariji odjeljak u `Dokumentacija/biljeske/00_odluke.md` opisuje generator koji prihvaća samo `LineSegment` i navodi da krug još nije podržan. Aktualni kod i kasniji odjeljci iste datoteke implementiraju `ArcSegment`, `G02/G03` i krug. Stariji tekst je povijesno stanje, ali nije izričito označen kao zastario.
3. `Dokumentacija/zavrsni_rad/04_arhitektura_aplikacije.md` zadržava formulaciju da fizička provjera nije provedena „u ovom auditu”, dok novija bilješka 18.A navodi uspješan fizički test konkretnog aplikacijskog `test01.nc`. Aktualni kod profila unatoč toj bilješci i dalje vraća prazan `physicallyConfirmedCapabilities`.
4. Fizička potvrda iz bilješke 18.A odnosi se na konkretni pravokutni program. Ne predstavlja dokaz za kružne lukove, `OUTSIDE`, sve naredbe profila ili univerzalnu RichAuto A11 kompatibilnost.
5. `ReferenceDataManagementService#createTool` izravno provjerava jedinstvenost `tool_number` pri stvaranju. `updateTool` ne ponavlja tu service-provjeru, ali H2 ograničenje `UNIQUE (cnc_machine_id, tool_number)` i dalje štiti bazu. Dokumentacija ne smije svim write putovima pripisati istu prijateljsku service-poruku.

## Provjera ovog audita

Produkcijski kod, testni izvori i postojeća dokumentacija statički su pregledani. Maven naredba `mvn -q test` nije izvršena jer naredba `mvn` nije dostupna u trenutačnom okruženju. Zbog toga ovaj dokument ne predstavlja novi testni dokaz: oznaka TESTIRANO odnosi se isključivo na postojeće testove i stvarne rezultate prethodno zabilježene u razvojnim bilješkama. U ovom zadatku nisu mijenjani produkcijski kod, testovi, SQL, FXML ni ponašanje aplikacije.
