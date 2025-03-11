# G2J - Compilatore per Grammatiche EBNF

G2J è un compilatore che prende in input la definizione di grammatiche scritte in EBNF (Extended Backus-Naur Form) e genera file in formato `.jj` (JavaCC) e `.g4` (ANTLR). Inoltre, esegue l'analisi semantica sulla grammatica fornita, verificando la correttezza e suggerendo ottimizzazioni.

## Funzionalità

- **Analisi Semantica**: Verifica la correttezza della grammatica, inclusa la presenza di ricorsione sinistra, produzioni non raggiungibili, prefissi comuni e altro.
- **Generazione di File JavaCC**: Produce un file `.jj` compatibile con JavaCC a partire dalla grammatica EBNF.
- **Generazione di File ANTLR**: Produce un file `.g4` compatibile con ANTLR a partire dalla grammatica EBNF.
- **Ottimizzazioni**: Suggerisce ottimizzazioni come l'eliminazione della ricorsione sinistra e la fattorizzazione dei prefissi comuni.

## Requisiti

- Java Development Kit (JDK) 8 o superiore
- ANTLR 4 (opzionale, se si desidera eseguire i file `.g4` generati)
- JavaCC (opzionale, se si desidera eseguire i file `.jj` generati)

## Configurazione

1. Clona il repository:

   ```bash
   git clone https://github.com/vinceslao/G2J_Project.git
   cd G2J_Project
   ```

2. Compila il progetto:

   ```bash
   ./mvnw clean install
   ```

## Utilizzo

### Input

Il file di input deve contenere la definizione della grammatica in formato EBNF. Un esempio di file di input è fornito in `src/main/resources/input.txt`.

### Esecuzione

Per eseguire il compilatore e generare i file `.jj` e `.g4`, esegui il seguente comando:

```bash
java -cp target/g2j-1.0-SNAPSHOT.jar it.unisannio.g2j.Main
```

Questo comando legge il file di input (`src/main/resources/input.txt`), esegue l'analisi semantica e genera i file `GrammarOut.jj` (JavaCC) e `GrammarOut.g4` (ANTLR) nella directory principale del progetto.

### Output

- **GrammarOut.jj**: File JavaCC generato a partire dalla grammatica EBNF.
- **GrammarOut.g4**: File ANTLR generato a partire dalla grammatica EBNF.

## Esempio di Input EBNF

```ebnf
/* Token */
ID            ::= [a-zA-Z][a-zA-Z0-9]* ;
NUMBER        ::= [0-9]+ ;
SEMICOLON     ::= ";" ;
ASSIGNMENT    ::= ":=" ;
SUM_DIF       ::= "+" | "-" ;
MUL_DIV       ::= "*" | "/" ;
LEF_PAR       ::= "(" ;
RIGHT_PAR     ::= ")" ;
WRITE         ::= "WRITE" ;
READ          ::= "READ" ;
IF            ::= "IF" ;
THEN          ::= "THEN" ;
ELSE          ::= "ELSE" ;
REPEAT        ::= "REPEAT" ;
UNTIL         ::= "UNTIL" ;
COMPARISON    ::= "==" | "!=" | "<" | "<=" | ">" | ">=" ;

/* Regole di produzione */
<Program>::= <Statements> EOF ;
<Statements>::= <Statement> SEMICOLON <Statements> | <Statement> ;
<Statement>::= <CallRead> | <CallWrite> | <Assignment> | <IfThen> | <RepeatUntil> ;
<CallWrite>::= WRITE LEF_PAR ID RIGHT_PAR;
<CallRead>::= READ LEF_PAR ID RIGHT_PAR ;
<Assignment>::= ID ASSIGNMENT <Expression> ;
<Expression>::= <Expression> SUM_DIF <MulDivExpr> | <MulDivExpr> ;
<MulDivExpr>::= <MulDivExpr> MUL_DIV <Factor> | <Factor> ;
<Factor>::= ID | NUMBER | (LEF_PAR <Expression> RIGHT_PAR) ;
<IfThen>::= IF <Condition> THEN <Statements> | IF <Condition> THEN <Statements> ELSE <Statements> ;
<Condition>::= <Expression> COMPARISON <Expression> ;
<RepeatUntil>::= REPEAT <Statements> UNTIL <Condition> ;
```

## Contributi

Se desideri contribuire al progetto, segui questi passaggi:

1. Fork del repository.
2. Crea un nuovo branch (`git checkout -b feature/nuova-funzionalita`).
3. Fai commit delle tue modifiche (`git commit -am 'Aggiungi una nuova funzionalità'`).
4. Push del branch (`git push origin feature/nuova-funzionalita`).
5. Apri una Pull Request.

## Autori

- [Concia Gianvincenzo](https://github.com/vinceslao)
- [Orlando Vittoria](https://github.com/v-orlando)
- [Russo Francesco](https://github.com/Frusso3)
