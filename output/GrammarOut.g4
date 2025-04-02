grammar GrammarOut;

SEMICOLON : ';' ;
SELECT : 'SELECT' ;
FROM : 'FROM' ;
WHERE : 'WHERE' ;
AND : 'AND' ;
OR : 'OR' ;
INSERT : 'INSERT' ;
INTO : 'INTO' ;
VALUES : 'VALUES' ;
UPDATE : 'UPDATE' ;
SET : 'SET' ;
DELETE : 'DELETE' ;
CREATE : 'CREATE' ;
TABLE : 'TABLE' ;
PRIMARY : 'PRIMARY' ;
KEY : 'KEY' ;
NOT : 'NOT' ;
NULL : 'NULL' ;
UNIQUE : 'UNIQUE' ;
INT : 'INT' ;
VARCHAR : 'VARCHAR' ;
DATE : 'DATE' ;
BOOLEAN : 'BOOLEAN' ;
EQUALS : '=' ;
NOT_EQUALS : '<>' ;
LESS_THAN : '<' ;
GREATER_THAN : '>' ;
LESS_EQUAL : '<=' ;
GREATER_EQUAL : '>=' ;
COMMA : ',' ;
LEFT_PAREN : '(' ;
RIGHT_PAREN : ')' ;
DOT : '.' ;
STAR : '*' ;
IDENTIFIER : [a-zA-Z][a-zA-Z0-9_]*;
NUMBER_LITERAL : [0-9]+('.' [0-9]+)?;
COMPARISON :  |  |  |  |  | ;
LOGIC_OP :  | ;
program : statement SEMICOLON (programTail )?EOF ;
statement : select_Statement  | insert_Statement  | update_Statement  | delete_Statement  | create_Statement ;
select_Statement : SELECT column_List FROM table_List (select_StatementSuffix )?;
insert_Statement : INSERT INTO IDENTIFIER LEFT_PAREN column_List RIGHT_PAREN (insert_StatementSuffix )?;
update_Statement : UPDATE IDENTIFIER SET assignment_List (update_StatementSuffix )?;
delete_Statement : DELETE FROM IDENTIFIER (delete_StatementSuffix )?;
create_Statement : CREATE TABLE IDENTIFIER LEFT_PAREN column_Definition_List RIGHT_PAREN ;
column_List : STAR  | IDENTIFIER  | IDENTIFIER DOT IDENTIFIER  | IDENTIFIER COMMA column_List  | IDENTIFIER DOT IDENTIFIER COMMA column_List ;
table_List : IDENTIFIER (table_ListSuffix )?;
condition : expression (conditionTail )?;
expression : term (expressionTail )?;
term : IDENTIFIER  | NUMBER_LITERAL  | NULL ;
value_List : term (value_ListTail )?;
assignment_List : assignment (assignment_ListTail )?;
assignment : IDENTIFIER EQUALS term ;
column_Definition_List : column_Definition (column_Definition_ListTail )?;
column_Definition : IDENTIFIER data_Type (column_DefinitionSuffix )?;
data_Type : INT  | VARCHAR LEFT_PAREN NUMBER_LITERAL RIGHT_PAREN  | DATE  | BOOLEAN ;
constraint : NOT NULL  | PRIMARY KEY  | UNIQUE ;
select_StatementSuffix : WHERE condition ;
delete_StatementSuffix : WHERE condition ;
update_StatementSuffix : WHERE condition ;
table_ListSuffix : COMMA table_List ;
programTail : statement SEMICOLON (programTail )?;
conditionTail : LOGIC_OP expression (conditionTail )?;
expressionTail : COMPARISON term (expressionTail )?;
value_ListTail : COMMA term (value_ListTail )?;
insert_StatementSuffix : VALUES LEFT_PAREN value_List RIGHT_PAREN  | SELECT column_List FROM table_List ;
assignment_ListTail : COMMA assignment (assignment_ListTail )?;
column_DefinitionSuffix : constraint ;
column_Definition_ListTail : COMMA column_Definition (column_Definition_ListTail )?;
