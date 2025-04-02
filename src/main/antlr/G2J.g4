grammar G2J;

@header {
package it.unisannio.g2j;
}

// +++++++++++++
// + PARSER
// +++++++++++++

// Regola principale
grammarFile: rules EOF ;

rules: rule+;

rule: (lexRule | parseRule) SEMICOLON ;

// Una regola di produzione
parseRule: NON_TERM ASSIGN productionList;

// Lista di produzioni (separate da |)
productionList: production (PIPE production)*;

// Una produzione
production: element+ ;

// Elemento di una produzione
element: NON_TERM | TERM |  rep_opt | grouping | optionality | repetivity;

grouping: LEFT_ROUND_BRACKET production RIGHT_ROUND_BRACKET ;

optionality: LEFT_SQUARE_BRACKET production RIGHT_SQUARE_BRACKET;

repetivity: LEFT_CURLY_BRACKET production RIGHT_CURLY_BRACKET;

rep_opt: LEFT_CURLY_BRACKET LEFT_SQUARE_BRACKET production RIGHT_SQUARE_BRACKET RIGHT_CURLY_BRACKET;

// Definizione di una regola lessicale
lexRule: TERM ASSIGN regex+;

// Gestione espressioni regolari che definiscono i token
regex      : term ( PIPE term )* ; // L'espressione regolare completa

term       : factor+ ; // Uno o più fattori concatenati

factor     : primary ( KLEENE_CLOSURE | POSITIVE_CLOSURE | OPTIONALITY )? ; // Un elemento con quantificatori opzionali

primary    : CHAR                // Un carattere normale
           | ESCAPED_CHAR         // Un carattere speciale con backslash
           | DOT                  // Punto (matcha qualsiasi carattere)
           | CHAR_CLASS        // Una classe di caratteri come [a-zA-Z_]
           | LEFT_ROUND_BRACKET regex RIGHT_ROUND_BRACKET    // Un gruppo tra parentesi
           | STRING
           | TERM
           ;


// +++++++++++++
// + LEXER
// +++++++++++++

// Simboli
SEMICOLON: ';';
ASSIGN: '::=';
PIPE: '|';
LEFT_ROUND_BRACKET: '(';
RIGHT_ROUND_BRACKET: ')';
LEFT_SQUARE_BRACKET: '[';
RIGHT_SQUARE_BRACKET: ']';
LEFT_CURLY_BRACKET: '{';
RIGHT_CURLY_BRACKET: '}';

// Identificatori per non terminali e terminali
TERM: [A-Z][A-Z_]* ;
NON_TERM: '<' [A-Z][a-zA-Z_]* '>' ;

// Stringhe tra virgolette
STRING: '"' .*? '"';

// Token espressioni regolari
NEGATION  : '^' ; // Per classi negate come [^a-z]
CHAR      : [a-zA-Z0-9_] ; // Lettere, numeri e underscore
ESCAPED_CHAR : '\\' [dwsrtfn\\.*+?()|[\]] ; // Escape per caratteri speciali
CHAR_CLASS : '[' NEGATION? (CHAR | ESCAPED_CHAR | '-')+ ']' ; // Classe di caratteri
DOT: '.';
KLEENE_CLOSURE: '*';
POSITIVE_CLOSURE: '+';
OPTIONALITY: '?';

// Ignora spazi e commenti
WS: [ \t\r\n]+ -> skip;
COMMENT: '/*' .*? '*/' -> skip;