grammar GrammarOut;

ID : [a-zA-Z][a-zA-Z0-9]*;
NUMBER : [0-9]+;
SEMICOLON : ';' ;
ASSIGNMENT : ':=' ;
MUL_DIV : '*'  | '/' ;
LEF_PAR : '(' ;
RIGHT_PAR : ')' ;
WRITE : 'WRITE' ;
READ : 'READ' ;
IF : 'IF' ;
THEN : 'THEN' ;
ELSE : 'ELSE' ;
END : 'END' ;
REPEAT : 'REPEAT' ;
UNTIL : 'UNTIL' ;
COMPARISON : '=='  | '!='  | '<'  | '<='  | '>'  | '>=' ;
program : statements EOF ;
statements : statement SEMICOLON statements statement ;
statement : callRead callWrite assignment ifThen repeatUntil ;
callWrite : WRITE (ID NUMBER );
callRead : READ (ID );
assignment : ID ASSIGNMENT expression ;
expression : expression SUM_DIF mulDivExpr mulDivExpr ;
mulDivExpr : mulDivExpr MUL_DIV factor factor ;
factor : ID NUMBER (LEF_PAR expression RIGHT_PAR );
ifThen : IF condition THEN statements (ELSE statements )?END ;
condition : expression COMPARISON expression ;
repeatUntil : REPEAT statements UNTIL condition ;
