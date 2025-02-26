/*
    Questa grammatica ha lo scopo di verificare la correttezza della descrizione di grammatiche generiche
*/

grammar G2J;

@header {
package it.unisannio.g2j;
}

// +++++++++++++
// + PARSER
// +++++++++++++

// Regola principale
grammarFile: (lexRule | parseRule)+ EOF ;

// Definizione di una regola lessicale
lexRule: TERM ASSIGN (regex | STRING (PIPE STRING | regex)* );

// Una regola di produzione
parseRule: NON_TERM ASSIGN productionList;

// Lista di produzioni (separate da |)
productionList: production (PIPE production)*;

// Una produzione
production: element+;

// Elemento di una produzione (può essere terminale o non terminale)
element: NON_TERM | TERM | STRING | '(' | ')' | '[' | ']' | PIPE;

// Gestione espressioni regolari
regex      : term ( '|' term )* ; // L'espressione regolare completa

term       : factor+ ; // Uno o più fattori concatenati

factor     : primary ( '*' | '+' | '?' )? ; // Un elemento con quantificatori opzionali

primary    : CHAR                // Un carattere normale
           | ESCAPED_CHAR         // Un carattere speciale con backslash
           | '.'                  // Punto (matcha qualsiasi carattere)
           | CHAR_CLASS        // Una classe di caratteri come [a-zA-Z_]
           | '(' regex ')'    // Un gruppo tra parentesi
           ;


// +++++++++++++
// + LEXER
// +++++++++++++

// Simboli di EBNF
ASSIGN: '::=';
PIPE: '|';

// Identificatori per non terminali e terminali
TERM: [A-Z][A-Z_]* ;
NON_TERM: '<' [A-Z][a-zA-Z]* '>' ;

// Stringhe tra virgolette
STRING: '"' .*? '"';

// Token espressioni regolari
NEGATION  : '^' ; // Per classi negate come [^a-z]
CHAR      : [a-zA-Z0-9] ; // Lettere, numeri e underscore
ESCAPED_CHAR : '\\' [dwsrtfn\\.*+?()|[\]] ; // Escape per caratteri speciali
CHAR_CLASS : '[' NEGATION? (CHAR | ESCAPED_CHAR | '-')+ ']' ; // Classe di caratteri

// Ignora spazi e commenti
WS: [ \t\r\n]+ -> skip;
COMMENT: '/*' .*? '*/' -> skip;