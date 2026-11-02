# JDER

**Java Diagrammi E/R** — Un editor moderno e multipiattaforma per diagrammi Entità-Relazione, Schemi Logici, diagrammi Use Case, delle Classi, di Sequenza e Logici

[![Website](https://img.shields.io/badge/Website-jder.it-blue?style=for-the-badge)](https://jder.it/)
[![GitHub Release](https://img.shields.io/github/v/release/LoryPelli/JDER?style=for-the-badge)](https://github.com/LoryPelli/JDER/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-orange?style=for-the-badge&logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform)

[🌐 Sito Web](https://jder.it/) • [⬇️ Download](https://jder.it/download) • [🐛 Segnala un problema](https://github.com/LoryPelli/JDER/issues)

---

## Panoramica

JDER è una riscrittura completa in **Kotlin** e **Jetpack Compose** del software originale creato da [Alessandro Ballini](https://ballini.it/Software/ProgER) e successivamente mantenuto da [Gianvito Pio](https://gianvitopio.wordpress.com/jder). Porta con sé un'interfaccia Material 3 moderna e pulita, nuovi tipi di diagramma e un'esperienza utente notevolmente migliorata.

---

## Funzionalità

### 🗂️ Tipi di Diagramma

#### Diagramma Entità-Relazione (E/R)

- Crea e gestisci **Entità** con pieno supporto agli attributi
  - Attributi normali, chiave (chiave primaria), multivalore, derivati e composti
  - Supporto alle entità deboli
- Crea **Relazioni** tra entità con notazione di cardinalità completa
  - Cardinalità supportate: `1`, `(0,1)`, `(1,1)`, `N`, `(0,N)`, `(1,N)`
  - Conversione in entità associativa (relazioni N-a-N)
- Aggiungi **Note testuali** in qualsiasi punto della lavagna
- Pannello Proprietà per modificare elementi e connessioni
- Menu contestuale per azioni rapide su qualsiasi elemento

#### Schema Logico (Relazionale)

- Il passo successivo all'E/R nella progettazione di una base di dati: crea **Tabelle** con le loro **Colonne**
  - Chiave primaria anche composta (colonne sottolineate e segnate `PK`), colonne opzionali che ammettono `NULL` (segnate con `*`) e a valore unico (`UQ`)
  - Tipo facoltativo per ogni colonna (es. `INT`, `VARCHAR(50)`)
- Crea **Chiavi Esterne** con lo strumento dedicato (clic sulla tabella che riferisce e poi su quella riferita) o dalla finestra della colonna
  - Le nuove colonne riprendono la chiave primaria della tabella riferita e possono far parte della chiave primaria (relazioni N-a-N, entità deboli)
  - Le frecce collegano la colonna alla colonna riferita e vengono instradate evitando le altre tabelle
- Eliminando una tabella o una colonna riferita vengono rimosse anche le chiavi esterne che la usavano
- Aggiungi **Note testuali** come negli altri diagrammi

#### Diagramma delle Classi

- Crea **Classi**, **Interfacce**, **Enum** e **Classi Astratte** (con etichette stereotipo UML)
- Aggiungi **Attributi** e **Metodi** a ogni classe con:
  - Modificatori di visibilità: Pubblico (`+`), Privato (`-`), Protetto (`#`), Package (`~`)
  - Flag per membri statici e astratti
  - Annotazione del tipo per ogni membro
- Disegna **Relazioni** tra classi:
  - Associazione, Aggregazione, Composizione, Ereditarietà, Realizzazione, Dipendenza
  - Etichette di molteplicità sorgente e destinazione su ogni relazione
  - Etichetta della relazione opzionale
- Aggiungi **documentazione** inline a qualsiasi classe
- Aggiungi **Note testuali** condivise con il componente note degli altri diagrammi

#### Diagramma dei Casi d'Uso

- Crea **Attori** (con rappresentazione a omino stilizzato)
- Crea **Casi d'Uso** (con rappresentazione a ellisse)
- Disegna **Relazioni** tra elementi:
  - Associazione, Include (`<<include>>`), Extend (`<<extend>>`), Generalizzazione
  - Frecce correttamente renderizzate con direzionalità appropriata
- Definisci **Confini di Sistema** per raggruppare i casi d'uso
- Aggiungi **Note testuali** condivise con il componente note E/R

#### Diagramma di Sequenza

- Crea **Linee di Vita** per attori, oggetti e gli stereotipi di analisi **Boundary**, **Control** ed **Entity**
- Crea **Messaggi** trascinando da una linea di vita all'altra (oppure con lo strumento Messaggio) e spostali in verticale trascinandoli
  - Sincroni, asincroni, di risposta, di creazione (`«create»`: l'oggetto compare all'altezza del messaggio) e di distruzione (`«destroy»`: la linea di vita termina con una `X`)
  - Messaggi a sé stessi (con lo strumento Messaggio clicca due volte la stessa linea di vita) e inversione della direzione dal pannello Proprietà
- **Barre di attivazione** disegnate automaticamente: un messaggio sincrono attiva il destinatario fino alla risposta corrispondente
- **Frammenti combinati** `alt`, `opt`, `loop`, `par`, `break`, `critical` e `ref`, con condizione e operandi aggiuntivi (es. `[else]`) trascinabili
- Aggiungi **Note testuali** come negli altri diagrammi

#### Diagramma Logico

- Inserisci **Porte Logiche** con i simboli standard: `AND`, `OR`, `NOT`, `NAND`, `NOR`, `XOR`, `XNOR`
  - Da 2 a 8 ingressi per le porte a più ingressi, etichetta opzionale (es. `U1`)
- Aggiungi **Ingressi** (interruttori 0/1, cliccabili) e **Uscite** (indicatori luminosi)
- Collega gli elementi **trascinando da un pin all'altro** (oppure con lo strumento Collegamento)
  - I collegamenti vengono instradati automaticamente evitando gli altri componenti
- **Simulazione in tempo reale**: i fili si colorano in base al valore (1, 0 o indefinito)
  - Gestisce anche circuiti con retroazione (es. latch SR) e segnala quelli che oscillano
- **Tabella di verità** generata automaticamente (copiabile negli appunti)
- **Espressione booleana** di ogni uscita e porta nel pannello Proprietà
- Aggiungi **Note testuali** come negli altri diagrammi

---

### 🎨 Interfaccia & UX

- Sistema di design **Material 3** in tutta l'applicazione
- **Modalità Chiara & Scura** con transizione animata fluida
- **20 palette di colori Material ufficiali** selezionabili a runtime:
  - Rosso, Rosa, Viola, Viola Scuro, Indaco, Blu, Blu Chiaro, Ciano, Verde Acqua, Verde, Verde Chiaro, Lime, Giallo, Ambra, Arancione, Arancione Scuro, Marrone, Grigio, Grigio Blu, Bianco & Nero
- **Scelta del tipo di diagramma** all'avvio, con l'anteprima di ogni tipo in versione chiara o scura secondo il tema (clic sull'anteprima oppure `Ctrl` + `1` … `6`)
- **Navigazione a schede** tra i sei tipi di diagramma (le schede con modifiche non salvate sono segnate con `•`)
- **Pannello Proprietà** — barra laterale contestuale per modificare gli elementi selezionati
- **Menu Contestuale** — clic destro su qualsiasi elemento della lavagna per azioni rapide
- **Barra dei menu** — File, Modifica, Vista, Strumenti, Elemento e Aiuto con le scorciatoie, gli strumenti del diagramma attivo, le azioni sull'elemento selezionato, il tema, le palette e il riquadro «Informazioni su JDER»: su macOS è la barra di sistema (con anche il menu Finestra), su Windows e Linux sta in cima alla finestra e segue il tema dell'app
- **Menu del Dock** (macOS) — clic destro sull'icona di JDER nel Dock per creare subito un nuovo diagramma E/R, schema logico o diagramma delle classi, dei casi d'uso, di sequenza o logico
- **Suggerimenti** sui pulsanti della barra degli strumenti e scala corretta sugli schermi ad alta densità (Retina/HiDPI)
- **Messaggi colorati** in basso nella finestra: verdi per le conferme, gialli per gli avvisi e rossi per gli errori; si chiudono da soli (rispettivamente dopo 4, 8 e 12 secondi) o con ✕ e, avviando JDER da terminale, avvisi ed errori vengono scritti anche lì

---

### 🖱️ Controlli della Lavagna

| Azione                | Controllo                                              |
| --------------------- | ------------------------------------------------------ |
| Zoom Avanti           | `Ctrl` + `+` o scroll mouse su (verso il cursore)      |
| Zoom Indietro         | `Ctrl` + `-` o scroll mouse giù                        |
| Scorrimento           | Trascinamento su un'area vuota o con il tasto centrale |
| Seleziona e Sposta    | Clic sinistro + trascinamento                          |
| Modifica elemento     | Doppio clic sull'elemento                              |
| Apri menu contestuale | Clic destro sull'elemento (`Ctrl` + clic su macOS)     |
| Collega porte logiche | Trascinamento da un pin a un altro                     |
| Crea un messaggio     | Trascinamento da una linea di vita a un'altra          |
| Reimposta Zoom        | `Ctrl` + `0` o Menu Vista → Reimposta Zoom             |

---

### ⌨️ Scorciatoie da Tastiera

Su macOS si può usare `Cmd` al posto di `Ctrl`.

| Scorciatoia                     | Azione                                                             |
| ------------------------------- | ------------------------------------------------------------------ |
| `Ctrl + Z`                      | Annulla                                                            |
| `Ctrl + Y` / `Ctrl + Shift + Z` | Ripeti                                                             |
| `Ctrl + S`                      | Salva                                                              |
| `Ctrl + Shift + S`              | Salva con nome                                                     |
| `Ctrl + O`                      | Apri                                                               |
| `Ctrl + N`                      | Nuovo Diagramma                                                    |
| `Ctrl + E`                      | Esporta come PNG                                                   |
| `Ctrl + +`                      | Zoom Avanti                                                        |
| `Ctrl + -`                      | Zoom Indietro                                                      |
| `Ctrl + 0`                      | Reimposta Zoom                                                     |
| `Ctrl + 1` … `Ctrl + 6`         | Diagramma E/R, Schema Logico, Classi, Casi d'Uso, Sequenza, Logico |
| `Delete` / `Backspace`          | Elimina elemento selezionato                                       |
| `Esc`                           | Deseleziona e torna allo strumento Seleziona                       |
| `F11`                           | Schermo intero (Windows e Linux)                                   |
| `Ctrl + Cmd + F`                | Schermo intero (macOS)                                             |
| `Cmd + M`                       | Riduci a icona (macOS)                                             |

---

### 💾 Salvataggio & Esportazione

- Salva i diagrammi in formato **JSON** (`.json`)
- **Salvataggio automatico**: dopo il primo salvataggio si può attivare **File → Salvataggio Automatico** e ogni modifica viene salvata da sola nello stesso file
- Apri e ricarica diagrammi salvati in precedenza (un file di un altro tipo di diagramma viene riconosciuto e non sovrascritto per errore)
- Esporta qualsiasi diagramma come **immagine PNG** ad alta risoluzione, identica a quanto mostrato sulla lavagna e ritagliata sul contenuto
- **Esportazione automatica**: dopo la prima esportazione si può attivare **File → Esportazione Automatica** e l'immagine PNG viene aggiornata a ogni modifica (comodo per tenerla sempre allineata in documenti e presentazioni)
- Finestra di dialogo per modifiche non salvate alla chiusura o all'apertura di un nuovo diagramma
- Cronologia annulla/ripeti per ogni diagramma (fino a 50 stati)

---

## Stack Tecnologico

| Tecnologia                                                                   | Utilizzo                     |
| ---------------------------------------------------------------------------- | ---------------------------- |
| [Kotlin](https://kotlinlang.org/)                                            | Linguaggio principale        |
| [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) | Framework UI                 |
| [Material 3](https://m3.material.io/)                                        | Sistema di design            |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)     | Salvataggio/caricamento JSON |
| [Gradle](https://gradle.org/)                                                | Sistema di build             |
| [Cloudflare Workers](https://workers.cloudflare.com/)                        | Hosting del sito web         |
| [Skia](https://skia.org/)                                                    | Rendering/esportazione PNG   |
| Java 11                                                                      | Target JVM                   |

---

## Requisiti

- **Java 11** o versione superiore
- Windows, macOS (Intel o Apple Silicon), o Linux (x64 o ARM)

---

## Per Iniziare

### Download

Scarica l'ultimo `JDER.jar` dalla [pagina delle Release](https://github.com/LoryPelli/JDER/releases/latest) oppure direttamente:

```
https://jder.it/download
```

Poi eseguilo con:

```bash
java -jar JDER.jar
```

### Riga di comando

`JDER.jar` accetta anche alcune opzioni:

| Opzione            | Descrizione                                                                                         |
| ------------------ | --------------------------------------------------------------------------------------------------- |
| `--json <file>`    | Apre il diagramma salvato nel file JSON, direttamente nella scheda del suo tipo                    |
| `--png <file>`     | Esporta il diagramma nel PNG indicato e lo aggiorna automaticamente a ogni modifica                |
| `--file <file>`    | Alias di `--json`, mantenuto per compatibilità con l'integrazione Obsidian precedente               |
| `--preview <file>` | Alias di `--png`, mantenuto per compatibilità con l'integrazione Obsidian precedente               |
| `-h`, `--help`     | Mostra l'elenco delle opzioni ed esce                                                               |

Se manca, al nome del file viene aggiunta l'estensione (`.json` o `.png`), quindi `--json diagramma` apre `diagramma.json`.

Per esempio, per modificare un diagramma tenendo sempre aggiornata la sua immagine:

```bash
java -jar JDER.jar --json diagramma.json --png diagramma.png
```

Con `--png` l'esportazione automatica parte già attiva e si può disattivare dal menu **File**. Un file JSON esistente invece non viene salvato da solo: si salva come al solito oppure attivando **File → Salvataggio Automatico**. Per un nuovo JSON il salvataggio automatico parte già attivo dopo la scelta del tipo di diagramma.

Se il file JSON non esiste ancora, o non si riconosce che tipo di diagramma contiene, JDER mostra all'avvio la scelta del tipo di diagramma e apre il file nella scheda scelta. Anche `--png` si può usare da solo: in questo caso si sceglie il tipo all'avvio e l'immagine segue il nuovo diagramma.

Se un'opzione non è valida, JDER la ignora e lo segnala all'apertura con un avviso (scritto anche nel terminale). Se invece il file JSON esiste ma non si può aprire, l'avviso ne indica il motivo e JDER parte dalla scelta del tipo di diagramma senza usare né il file né l'immagine indicata con `--png`, così da non sovrascriverli.

Il controllo automatico degli aggiornamenti è stato rimosso in questa fork: l'applicazione non effettua richieste di rete all'avvio.

### Diagrammi di esempio

La cartella [`examples/`](examples/) contiene un diagramma per ogni tipo (E/R, schema logico, classi, casi d'uso, sequenza e logico), apribili da JDER con **File → Apri Diagramma**. Gli screenshot del sito, in versione chiara e scura, sono generati da questi file e la build li include anche in `JDER.jar`, come anteprime nella scelta del tipo di diagramma.

### Compilare dal sorgente

```bash
git clone https://github.com/LoryPelli/JDER.git
cd JDER
./gradlew run
```

Per creare un JAR standalone:

```bash
./gradlew jar
```

Il file di output si troverà in `build/libs/JDER.jar`.

### Avvio da Obsidian

Per aprire il JSON e aggiornare automaticamente il PNG del diagramma:

```bash
java -jar jder.jar --json "ERModel/json/customer.json" --png "ERModel/png/customer.png"
```

I percorsi con spazi vanno racchiusi tra virgolette. Le cartelle di output mancanti vengono create quando si salva o si esporta. Per un JSON nuovo, scegli **Diagramma E/R** nella schermata iniziale; dopo la prima modifica il JSON viene salvato automaticamente e il PNG viene aggiornato a ogni modifica.

Per ottenere il JAR standalone usato dal plugin, eseguire `./gradlew jar` dalla cartella del progetto e copiare `build/libs/JDER.jar` nella cartella `ERModel/` del vault.

---

## Struttura del Progetto

```
src/
└── main/
    ├── kotlin/com/jder/
    │   ├── Main.kt                    # Punto di ingresso dell'app
    │   ├── CommandLine.kt             # Opzioni da riga di comando (--json, --png)
    │   ├── data/
    │   │   ├── DiagramFileRepository.kt       # Persistenza JSON generica con riconoscimento del tipo
    │   │   ├── ERDiagramRepository.kt         # Persistenza JSON E/R
    │   │   ├── RelationalDiagramRepository.kt # Persistenza JSON Schema Logico
    │   │   ├── UseCaseDiagramRepository.kt    # Persistenza JSON Use Case
    │   │   ├── SequenceDiagramRepository.kt   # Persistenza JSON Diagramma di Sequenza
    │   │   ├── ClassDiagramRepository.kt      # Persistenza JSON Diagramma Classi
    │   │   ├── LogicDiagramRepository.kt      # Persistenza JSON Diagramma Logico
    │   │   ├── ImageExporter.kt               # Esportazione PNG via Skia
    │   │   ├── JsonConfig.kt                  # Configurazione kotlinx.serialization
    │   ├── domain/model/
    │   │   ├── DiagramEditorState.kt          # Stato comune: file, modifiche, annulla/ripeti, zoom
    │   │   ├── UndoHistory.kt                 # Cronologia annulla/ripeti
    │   │   ├── DiagramType.kt                 # Tipi di diagramma (schede e file)
    │   │   ├── ERModels.kt                    # Data class Entity, Relationship, Attribute, Note
    │   │   ├── RelationalModels.kt            # Data class RelationalTable, RelationalColumn, chiavi esterne
    │   │   ├── UseCaseModels.kt               # Data class Actor, UseCase, SystemBoundary
    │   │   ├── SequenceModels.kt              # Data class SequenceLifeline, SequenceMessage, SequenceFragment
    │   │   ├── ClassDiagramModels.kt          # Data class ClassEntity, ClassMember, ClassRelation
    │   │   ├── LogicModels.kt                 # Data class LogicComponent, LogicWire, LogicDiagram
    │   │   ├── LogicSimulator.kt              # Simulazione, tabella di verità, espressioni booleane
    │   │   ├── ERDiagramState.kt              # Stato E/R
    │   │   ├── RelationalDiagramState.kt      # Stato Schema Logico
    │   │   ├── UseCaseDiagramState.kt         # Stato Use Case
    │   │   ├── SequenceDiagramState.kt        # Stato Diagramma di Sequenza
    │   │   ├── ClassDiagramState.kt           # Stato Diagramma Classi
    │   │   ├── LogicDiagramState.kt           # Stato Diagramma Logico
    │   │   └── *ToolMode.kt                   # Modalità strumento di ogni diagramma
    │   └── ui/
    │       ├── components/            # Lavagne, disegno, gesti, barre strumenti, pannelli
    │       ├── dialogs/               # Finestre di dialogo per proprietà e creazione
    │       ├── screens/               # AppScreen, DiagramEditorScaffold e una schermata per diagramma
    │       ├── theme/                 # Tema, Palette Colori, StatoTema
    │       └── utils/                 # Esportazione PNG e utilità di piattaforma
    └── resources/
        └── jder_icon.png
```

---

## Credits

JDER is a complete rewrite inspired by the work of:

- **Alessandro Ballini** — [Original JDER](https://ballini.it/Software/ProgER)
- **Gianvito Pio** — [Maintained version](https://gianvitopio.wordpress.com/jder)

---

## License

This project is licensed under the **MIT License** — see [LICENSE.md](LICENSE.md) for details.

---

Made with ❤️ by [LoryPelli](https://github.com/LoryPelli) • [jder.it](https://jder.it/)
