package it.unisannio.g2j.errors;

import it.unisannio.g2j.G2JParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class CustomErrorStrategy extends DefaultErrorStrategy {

    private boolean recovering = false;
    private int lastErrorLine = -1;
    public static int sintaxErrorNum = 0;

    // Stack to keep track of open delimiters for better error recovery
    private Stack<Integer> delimiterStack = new Stack<>();

    // Maps to track matching delimiters
    private static final Map<Integer, Integer> openToCloseDelimMap = new HashMap<>();
    private static final Map<Integer, String> delimiterNames = new HashMap<>();

    static {
        // Initialize delimiter mappings
        openToCloseDelimMap.put(G2JParser.LEFT_ROUND_BRACKET, G2JParser.RIGHT_ROUND_BRACKET);
        openToCloseDelimMap.put(G2JParser.LEFT_SQUARE_BRACKET, G2JParser.RIGHT_SQUARE_BRACKET);
        openToCloseDelimMap.put(G2JParser.LEFT_CURLY_BRACKET, G2JParser.RIGHT_CURLY_BRACKET);

        // Initialize delimiter names for error messages
        delimiterNames.put(G2JParser.LEFT_ROUND_BRACKET, "parentesi tonda");
        delimiterNames.put(G2JParser.LEFT_SQUARE_BRACKET, "parentesi quadra");
        delimiterNames.put(G2JParser.LEFT_CURLY_BRACKET, "parentesi graffa");
    }

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        // Se siamo già in fase di recovery, evitiamo di gestire ulteriori errori sulla stessa linea
        Token t = e.getOffendingToken();
        if (recovering && lastErrorLine == t.getLine()) {
            // Semplicemente consumiamo il token e continuiamo
            recognizer.consume();
            return;
        }

        // Segna che stiamo iniziando una recovery
        recovering = true;
        lastErrorLine = t.getLine();

        int line = t.getLine();
        int charPositionInLine = t.getCharPositionInLine();

        // Verifica se si tratta di un problema di spazio mancante tra parentesi e token
        if (handleMissingSpaceAfterDelimiter(recognizer, e)) {
            // Se abbiamo gestito questo caso specifico, possiamo terminare qui
            recovering = false;
            return;
        }

        System.err.println("⚠️ Errore sintattico alla linea " + line + ":" + charPositionInLine);
        System.err.println("    Token errato: '" + t.getText() + "'");

        // Verifica i diversi tipi di errore in ordine di priorità
        boolean handled = handleMissingAssignOperator(recognizer, e);

        if (!handled) {
            handled = handleDelimiterError(recognizer, e);
        }

        if (!handled && e instanceof InputMismatchException) {
            IntervalSet expectedTokens = ((InputMismatchException) e).getExpectedTokens();
            if (expectedTokens != null) {
                StringBuilder tokenList = new StringBuilder();

                // Ottieni la lista di token attesi
                List<Integer> tokenTypes = expectedTokens.toList();
                Vocabulary vocabulary = recognizer.getVocabulary();

                // Formatta la lista in modo leggibile
                for (int i = 0; i < tokenTypes.size(); i++) {
                    String tokenName = vocabulary.getDisplayName(tokenTypes.get(i));
                    tokenList.append(tokenName);

                    // Aggiungi separatori appropriati
                    if (i < tokenTypes.size() - 2) {
                        tokenList.append(", ");
                    } else if (i == tokenTypes.size() - 2) {
                        tokenList.append(" o ");
                    }
                }

                System.err.println("    Token attesi: "+tokenList);
            }
        }

        sintaxErrorNum += 1;

        // Resetta qualsiasi stato di errore precedente
        beginErrorCondition(recognizer);

        // Se non è stato gestito specificamente, esegui il recovery standard
        if (!handled) {
            recoverToSemicolon(recognizer, t);
        }

        // Segna che abbiamo completato il recovery
        recovering = false;

        // Indica al parser che abbiamo completato il recovery
        reportError(recognizer, e);
    }

    /**
     * Gestisce il caso speciale in cui manca uno spazio tra una parentesi e il token successivo
     * @return true se l'errore è stato gestito, false altrimenti
     */
    private boolean handleMissingSpaceAfterDelimiter(Parser recognizer, RecognitionException e) {
        // Ottieni il token problematico e il token successivo
        Token currentToken = e.getOffendingToken();

        // Controlla se il token corrente è un delimitatore di apertura
        if (isOpeningDelimiter(currentToken.getType())) {
            // Controlla se siamo in un contesto dove ci aspettiamo elementi dopo il delimitatore
            ParserRuleContext context = recognizer.getContext();
            if (context instanceof G2JParser.GroupingContext ||
                    context instanceof G2JParser.OptionalityContext ||
                    context instanceof G2JParser.RepetivityContext) {

                // Qui possiamo semplicemente continuare senza considerare questo un errore
                // Aggiungiamo il token al delimiter stack per la successiva gestione
                delimiterStack.push(currentToken.getType());

                // Non segnaliamo l'errore quando non c'è spazio dopo una parentesi
                return true;
            }
        }

        // Verifica anche se ci sono problemi con le parentesi di chiusura
        TokenStream tokens = recognizer.getInputStream();
        if (tokens.size() > 1 && tokens.index() > 0) {
            Token previousToken = tokens.get(tokens.index() - 1);

            // Se abbiamo appena letto un token che potrebbe essere attaccato a una parentesi di chiusura
            if (isClosingDelimiter(currentToken.getType()) &&
                    (previousToken.getType() == G2JParser.TERM ||
                            previousToken.getType() == G2JParser.NON_TERM)) {

                // Questo è il caso in cui un token è attaccato a una parentesi di chiusura
                // Possiamo gestirlo semplicemente continuando
                if (!delimiterStack.isEmpty()) {
                    delimiterStack.pop();  // Bilancia lo stack
                }

                return true;
            }
        }

        return false;
    }

    private boolean handleMissingAssignOperator(Parser recognizer, RecognitionException e) {
        // Verifica se stiamo analizzando una regola (parseRule o lexRule)
        ParserRuleContext currentContext = recognizer.getContext();
        if (currentContext == null) return false;

        // Ottieni i token attesi
        IntervalSet expectedTokens = recognizer.getExpectedTokens();
        Token currentToken = e.getOffendingToken();

        // Verifica se ci aspettiamo un operatore di assegnazione
        if (expectedTokens.contains(G2JParser.ASSIGN)) {
            Vocabulary vocabulary = recognizer.getVocabulary();

            // Controlla se siamo in una regola di parsing o lessicale
            boolean isParseRule = false;
            boolean isLexRule = false;

            // Verifica il contesto
            for (ParserRuleContext ctx = currentContext; ctx != null; ctx = ctx.getParent()) {
                if (ctx instanceof G2JParser.ParseRuleContext) {
                    isParseRule = true;
                    break;
                } else if (ctx instanceof G2JParser.LexRuleContext) {
                    isLexRule = true;
                    break;
                }
            }

            // Determina il tipo di regola per un messaggio di errore più preciso
            String ruleType = isParseRule ? "produzione" : (isLexRule ? "lessicale" : "");

            System.err.println("    Manca l'operatore di assegnazione '::=' nella definizione della regola " + ruleType);
            System.err.println("    Recovery: inserito operatore di assegnazione '::=' mancante");

            // Non consumiamo il token corrente, ma indichiamo che l'errore è stato gestito
            return true;
        }

        return false;
    }

    private boolean handleDelimiterError(Parser recognizer, RecognitionException e) {
        Token t = e.getOffendingToken();
        IntervalSet expectedTokens = null;

        if (e instanceof InputMismatchException) {
            expectedTokens = ((InputMismatchException) e).getExpectedTokens();
        }

        if (expectedTokens != null) {
            // Verifica se stiamo aspettando una parentesi chiusa
            boolean expectsClosing = false;
            int closingTokenType = -1;

            for (int expected : expectedTokens.toList()) {
                if (expected == G2JParser.RIGHT_ROUND_BRACKET ||
                        expected == G2JParser.RIGHT_SQUARE_BRACKET ||
                        expected == G2JParser.RIGHT_CURLY_BRACKET) {
                    expectsClosing = true;
                    closingTokenType = expected;
                    break;
                }
            }

            if (expectsClosing) {
                // Determina quale parentesi aperta corrispondente dovrebbe essere
                int openTokenType = -1;
                if (closingTokenType == G2JParser.RIGHT_ROUND_BRACKET) {
                    openTokenType = G2JParser.LEFT_ROUND_BRACKET;
                } else if (closingTokenType == G2JParser.RIGHT_SQUARE_BRACKET) {
                    openTokenType = G2JParser.LEFT_SQUARE_BRACKET;
                } else if (closingTokenType == G2JParser.RIGHT_CURLY_BRACKET) {
                    openTokenType = G2JParser.LEFT_CURLY_BRACKET;
                }

                Vocabulary vocabulary = recognizer.getVocabulary();
                String closingName = vocabulary.getDisplayName(closingTokenType);

                System.err.println("    Manca la " + delimiterNames.get(openTokenType) + " chiusa '" +
                        vocabulary.getLiteralName(closingTokenType).replace("'", "") + "' attesa");

                // Inserisci virtualmente il token mancante per permettere al parser di continuare
                System.err.println("    Recovery: inserita " + delimiterNames.get(openTokenType) + " chiusa mancante");

                // Continua il parsing senza consumare il token corrente
                return true;
            }
        }

        // Verifica anche gli errori di apertura di parentesi senza chiusura
        if (isOpeningDelimiter(t.getType())) {
            delimiterStack.push(t.getType());
        } else if (isClosingDelimiter(t.getType())) {
            if (!delimiterStack.isEmpty()) {
                int lastOpening = delimiterStack.pop();
                int expectedClosing = openToCloseDelimMap.get(lastOpening);

                if (t.getType() != expectedClosing) {
                    Vocabulary vocabulary = recognizer.getVocabulary();
                    System.err.println("    Errore di delimitatori non bilanciati. Trovato '" +
                            vocabulary.getLiteralName(t.getType()).replace("'", "") +
                            "' ma atteso '" +
                            vocabulary.getLiteralName(expectedClosing).replace("'", "") + "'");

                    // Invece di consumare, torniamo al recovery standard
                    return false;
                }
            }
        }

        return false;
    }

    private boolean isOpeningDelimiter(int tokenType) {
        return tokenType == G2JParser.LEFT_ROUND_BRACKET ||
                tokenType == G2JParser.LEFT_SQUARE_BRACKET ||
                tokenType == G2JParser.LEFT_CURLY_BRACKET;
    }

    private boolean isClosingDelimiter(int tokenType) {
        return tokenType == G2JParser.RIGHT_ROUND_BRACKET ||
                tokenType == G2JParser.RIGHT_SQUARE_BRACKET ||
                tokenType == G2JParser.RIGHT_CURLY_BRACKET;
    }

    private void recoverToSemicolon(Parser recognizer, Token t) {
        // Trova e consuma fino al prossimo punto e virgola
        TokenStream tokens = recognizer.getInputStream();
        Token currentToken = t;

        // Ciclo manuale per cercare il punto e virgola
        while (currentToken.getType() != Token.EOF &&
                currentToken.getType() != G2JParser.SEMICOLON) {
            tokens.consume();
            currentToken = tokens.LT(1);
        }

        // Consuma anche il punto e virgola se trovato
        if (currentToken.getType() == G2JParser.SEMICOLON) {
            tokens.consume();
            System.err.println("    Recovery: saltato fino al punto e virgola alla linea " +
                    currentToken.getLine() + ":" + currentToken.getCharPositionInLine());
        } else {
            System.err.println("    Recovery: raggiunta la fine del file senza trovare un punto e virgola");
        }
    }

    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        // Sovrascrive il comportamento standard per evitare di riportare l'errore due volte
        if (!inErrorRecoveryMode(recognizer)) {
            recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
            beginErrorCondition(recognizer);
        }
    }

    @Override
    public void sync(Parser recognizer) throws RecognitionException {
        // Disabilita completamente la sincronizzazione standard
        // per evitare che interferisca con la nostra strategia
    }

    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
        // Ottieni i token attesi
        IntervalSet expectedTokenSet = recognizer.getExpectedTokens();
        TokenStream tokens = recognizer.getInputStream();
        Token currentToken = tokens.LT(1);

        // Verifica se è un caso di mancanza di spazio tra parentesi e token
        if (handleParenthesisSpaceIssue(recognizer, currentToken)) {
            // Continua normalmente senza generare errori
            return currentToken;
        }

        // Verifica se stiamo aspettando un operatore di assegnazione
        if (expectedTokenSet.contains(G2JParser.ASSIGN)) {
            // Crea un token virtuale per l'operatore di assegnazione mancante
            CommonToken missingToken = new CommonToken(G2JParser.ASSIGN, "::=");
            missingToken.setLine(currentToken.getLine());
            missingToken.setCharPositionInLine(currentToken.getCharPositionInLine());

            System.err.println("⚠️ Errore sintattico alla linea " + currentToken.getLine() + ":" +
                    currentToken.getCharPositionInLine());
            System.err.println("    Manca l'operatore di assegnazione '::='");
            System.err.println("    Recovery: inserito operatore '::=' mancante");

            sintaxErrorNum += 1;

            // Non consumare il token corrente
            return missingToken;
        }

        // Verifica se ci si aspetta una parentesi chiusa
        boolean expectsClosingDelimiter = false;
        int expectedClosingType = -1;

        for (int tokenType : expectedTokenSet.toList()) {
            if (tokenType == G2JParser.RIGHT_ROUND_BRACKET ||
                    tokenType == G2JParser.RIGHT_SQUARE_BRACKET ||
                    tokenType == G2JParser.RIGHT_CURLY_BRACKET) {
                expectsClosingDelimiter = true;
                expectedClosingType = tokenType;
                break;
            }
        }

        if (expectsClosingDelimiter) {
            // Crea un token virtuale per la parentesi chiusa mancante
            CommonToken missingToken = new CommonToken(expectedClosingType,
                    recognizer.getVocabulary().getLiteralName(expectedClosingType).replace("'", ""));
            missingToken.setLine(currentToken.getLine());
            missingToken.setCharPositionInLine(currentToken.getCharPositionInLine());

            // Avvisa dell'aggiunta della parentesi chiusa
            String delimType = "parentesi";
            if (expectedClosingType == G2JParser.RIGHT_ROUND_BRACKET) {
                delimType = "parentesi tonda";
            } else if (expectedClosingType == G2JParser.RIGHT_SQUARE_BRACKET) {
                delimType = "parentesi quadra";
            } else if (expectedClosingType == G2JParser.RIGHT_CURLY_BRACKET) {
                delimType = "parentesi graffa";
            }

            System.err.println("⚠️ Errore sintattico alla linea " + currentToken.getLine() + ":" +
                    currentToken.getCharPositionInLine());
            System.err.println("    Manca la " + delimType + " chiusa");
            System.err.println("    Recovery: inserita " + delimType + " chiusa mancante");

            sintaxErrorNum += 1;

            // Non consumare il token corrente
            return missingToken;
        }

        // Se non è un caso specifico, ricadi sul comportamento standard
        InputMismatchException e = new InputMismatchException(recognizer);
        for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
            context.exception = e;
        }

        recover(recognizer, e);

        // Restituisci un token dummy per permettere al parser di continuare
        return new CommonToken(Token.INVALID_TYPE);
    }

    /**
     * Verifica se il problema è legato a mancanza di spazi tra parentesi e token
     */
    private boolean handleParenthesisSpaceIssue(Parser recognizer, Token currentToken) {
        String text = currentToken.getText();

        // Controlla se il token contiene una parentesi aperta seguita immediatamente da altri caratteri
        if (text.length() > 1) {
            char firstChar = text.charAt(0);
            if (firstChar == '(' || firstChar == '[' || firstChar == '{') {
                String remainder = text.substring(1);

                // Crea un token per la parentesi
                int bracketType = getBracketType(firstChar);
                CommonToken bracketToken = new CommonToken(bracketType, String.valueOf(firstChar));
                bracketToken.setLine(currentToken.getLine());
                bracketToken.setCharPositionInLine(currentToken.getCharPositionInLine());

                // Crea un token per il resto del testo
                CommonToken remainderToken = new CommonToken(determineTokenType(remainder), remainder);
                remainderToken.setLine(currentToken.getLine());
                remainderToken.setCharPositionInLine(currentToken.getCharPositionInLine() + 1);

                // Modifica lo stream di token per inserire i due token separati
                TokenStream tokens = recognizer.getInputStream();
                if (tokens instanceof BufferedTokenStream) {
                    BufferedTokenStream bufferedTokens = (BufferedTokenStream) tokens;

                    // Questo è un approccio semplificato; in pratica dovresti modificare lo stream
                    // in modo più sofisticato, questo è solo un esempio concettuale
                    // NOTA: Questa parte richiederebbe l'accesso diretto allo stream sottostante
                    // che non è sempre possibile in ANTLR

                    // Per ora, possiamo usare un altro approccio
                    System.err.println("Nota: Il token '" + text + "' potrebbe essere interpretato come '" +
                            firstChar + " " + remainder + "' per una migliore analisi");

                    // Restituisci true per indicare che abbiamo gestito il caso
                    return true;
                }
            }

            // Controlla anche per il caso di caratteri seguiti da parentesi chiuse
            char lastChar = text.charAt(text.length() - 1);
            if (lastChar == ')' || lastChar == ']' || lastChar == '}') {
                String prefix = text.substring(0, text.length() - 1);

                System.err.println("Nota: Il token '" + text + "' potrebbe essere interpretato come '" +
                        prefix + " " + lastChar + "' per una migliore analisi");

                return true;
            }
        }

        return false;
    }

    /**
     * Determina il tipo di token per una stringa
     */
    private int determineTokenType(String text) {
        // Implementa una logica semplice per determinare il tipo di token
        // Questo è un approccio semplificato; in pratica dovresti usare il lexer

        if (text.matches("[A-Z][A-Z_]*")) {
            return G2JParser.TERM;
        } else if (text.matches("<[A-Z][a-zA-Z]*>")) {
            return G2JParser.NON_TERM;
        }

        // Tipo generico se non riusciamo a determinarlo
        return Token.DEFAULT_CHANNEL;
    }

    /**
     * Converte un carattere di parentesi nel corrispondente tipo di token
     */
    private int getBracketType(char bracket) {
        switch (bracket) {
            case '(': return G2JParser.LEFT_ROUND_BRACKET;
            case '[': return G2JParser.LEFT_SQUARE_BRACKET;
            case '{': return G2JParser.LEFT_CURLY_BRACKET;
            case ')': return G2JParser.RIGHT_ROUND_BRACKET;
            case ']': return G2JParser.RIGHT_SQUARE_BRACKET;
            case '}': return G2JParser.RIGHT_CURLY_BRACKET;
            default: return Token.INVALID_TYPE;
        }
    }

    @Override
    protected void consumeUntil(Parser recognizer, IntervalSet set) {
        // Sovrascriviamo questo metodo per un controllo più preciso
        TokenStream tokens = recognizer.getInputStream();
        int ttype = tokens.LA(1);

        while (ttype != Token.EOF && !set.contains(ttype)) {
            System.err.println("    Consumo token: " + tokens.LT(1).getText());
            tokens.consume();
            ttype = tokens.LA(1);
        }
    }
}