# Trajna projektna pravila za Codex

Ova datoteka sadrži potvrđene projektne odluke i obvezna pravila rada. Ne proširuj opseg projekta pretpostavkama izvan ovih pravila i trenutačno odobrenog zadatka.

## Potvrđeni tehnološki kontekst

- Aplikacija se razvija u Javi uz JavaFX.
- Za bazu podataka koristi se H2.
- Sve geometrijske veličine izražavaju se u milimetrima.
- Izlazni CNC program sprema se kao `.nc` datoteka.
- Ciljni stroj i kontroler su ZK-1325 / RichAuto A11.
- Podržani oblici su kvadrat, pravokutnik, krug i jednakostranični trokut.
- Ne uvodi entitet ni klasu `Part`.
- Pseudojezik i AI integracija pripadaju budućem razvoju. Ne implementiraj ih bez nove potvrđene odluke.

## Arhitektonske granice

- Odgovornosti razdvoji na slojeve UI, service, validation, geometry/toolpath, layout, gcode i persistence.
- JavaFX controller smije koordinirati UI tok, ali ne smije sadržavati SQL, izračun geometrije, layout algoritam ni logiku generiranja G-koda.
- SQL i drugi detalji pristupa bazi pripadaju persistence sloju.
- Geometrija i ToolPath moraju ostati odvojeni od tekstualnog G-koda.
- Layout sloj računa raspored, a gcode sloj ne smije sam računati layout.
- Ne uvodi dodatne slojeve, tehnologije ili obrasce bez potrebe potvrđene konkretnim zadatkom.

## Ograničenja tvrdnji i nepotvrđene vrijednosti

- Layout opisuj kao rezultat implementiranog algoritma. Ne tvrdi da je matematički ili globalno optimalan.
- RichAuto A11 podršku uvijek razlikuj kao IMPLEMENTIRANU od fizički TESTIRANE.
- Ne tvrdi da je RichAuto kompatibilnost TESTIRANA prije stvarnog fizičkog testa na ciljnom stroju.
- Ne izmišljaj niti zaključavaj JDK verziju, vrijednosti `ToolType`, machining parametre, orijentaciju osi, work zero ili druge postavke fizičkog kontrolera.
- Vrijednost koja se trenutačno nalazi u konfiguraciji nije automatski potvrđena trajna projektna odluka.

## Obvezni način rada

- Svaki veći zadatak prvo analiziraj u Plan/Ask modu.
- Prije plana pregledaj trenutačno stanje repozitorija i utvrdi što je stvarno implementirano.
- Plan ograniči na jednu malu, jasno provjerljivu cjelinu.
- Nakon odobrenja implementiraj samo tu cjelinu. Ne prelazi na sljedeću funkcionalnost i ne širi opseg bez zasebnog zadatka.
- Nakon promjene pokreni najrelevantniju dostupnu provjeru.
- U izvještaju jasno navedi što je implementirano, što je testirano, kako je testirano i što još nije testirano.

## Dokumentiranje razvoja

- Projektna dokumentacija razvoja nalazi se u `Dokumentacija/biljeske/`.
- Nakon svakog zadatka koji promijeni kod, konfiguraciju, SQL, testove ili UI stvori ili ažuriraj bilješku povezanu s tim promptom.
- Nakon važne arhitektonske ili tehnološke odluke ažuriraj `Dokumentacija/biljeske/00_odluke.md`.
- Nakon svakog dokumentiranog koraka ažuriraj `Dokumentacija/biljeske/00_indeks.md`.
- Ako potrebna dokumentacijska mapa, indeks ili datoteka odluka još ne postoji, prijavi to kao preduvjet umjesto da prešutno preskočiš dokumentiranje.
- Svaku bilješku izradi prema `Dokumentacija/biljeske/00_predlozak_biljeske.md` i sačuvaj sve njegove obvezne odjeljke.
- Bilješka mora jasno razlikovati IMPLEMENTIRANO od TESTIRANO. Navedi izvršenu provjeru i njezin stvarni rezultat; ne prikazuj neizvršeni test kao uspješan.
- Izdvoji najviše nekoliko smislenih kandidata za isječak koda. Ne forsiraj isječak kada je kod trivijalan; tada izričito navedi da smislen kandidat ne postoji.
- Svaki kandidat za isječak mora biti iz stvarnog trenutačnog koda i mora sadržavati putanju datoteke, klasu i metodu, razlog važnosti te moguće poglavlje završnog rada.
- Kandidata za sliku, dijagram ili tablicu te Git commit/hash navedi samo kada stvarno postoje.
- Ne zapisuj tajne, lozinke, tokene, osobne podatke ni osobne lokalne putanje.
