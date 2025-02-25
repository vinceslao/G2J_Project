grammar G2J;

@header {
package it.unisannio.g2j;
}
// Regola principale
grammarFile: (token+ | rule+) ;

token: terminal '::=' REGEX ;

// Una regola di produzione
rule: nonTerminal '::=' productionList;

// Lista di produzioni (separate da |)
productionList: production ('|' production)*;

// Una produzione
production: element+;

// Elemento di una produzione (può essere terminale o non terminale)
element: nonTerminal
       | terminal
       | specialSymbol;

// Un non terminale (es: <Program>)
nonTerminal: '<' NON_TERM '>';

// Un terminale (es: WRITE)
terminal: TERM;

// Simboli speciali (es: ;, (), ecc.)
specialSymbol: '(' | ')' | '[' | ']' | '{' | '}' | '|' | '=' | '+' | '-' | '*' | '/' | ';' | ',' | '.' | ':' | '::=';

// Identificatori e simboli
ID: [a-zA-Z_][a-zA-Z0-9_]*;
TERM: [A-Z][A-Z_]* ;
NON_TERM: [a-z][a-z_]* ;
REGEX: ~[ \t\r\n]+;

// Ignora spazi e commenti
WS: [ \t\r\n]+ -> skip;