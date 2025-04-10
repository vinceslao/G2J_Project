package it.unisannio.g2j.errors;

import it.unisannio.g2j.G2JParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategia di gestione degli errori personalizzata per il parser G2J.
 * Estende la DefaultErrorStrategy di ANTLR4 e aggiunge logica di recovery
 * per casi specifici (es. mancanza di `;`, `::=`, parentesi, ecc).
 */
public class CustomErrorStrategy extends DefaultErrorStrategy {

    public static int sintaxErrorNum = 0;

    // Mappature parentesi aperte → chiuse
    private static final Map<Integer, Integer> openToCloseDelimMap = new HashMap<>();
    private static final Map<Integer, String> delimiterNames = new HashMap<>();

    static {
        openToCloseDelimMap.put(G2JParser.LEFT_ROUND_BRACKET, G2JParser.RIGHT_ROUND_BRACKET);
        openToCloseDelimMap.put(G2JParser.LEFT_SQUARE_BRACKET, G2JParser.RIGHT_SQUARE_BRACKET);
        openToCloseDelimMap.put(G2JParser.LEFT_CURLY_BRACKET, G2JParser.RIGHT_CURLY_BRACKET);

        delimiterNames.put(G2JParser.LEFT_ROUND_BRACKET, "parentesi tonda");
        delimiterNames.put(G2JParser.LEFT_SQUARE_BRACKET, "parentesi quadra");
        delimiterNames.put(G2JParser.LEFT_CURLY_BRACKET, "parentesi graffa");
    }

    // Disabilita il sync automatico (default di ANTLR)
    @Override
    public void sync(Parser recognizer) {
        // Nessuna sincronizzazione automatica
    }

    // Evita doppia segnalazione errore
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        if (!inErrorRecoveryMode(recognizer)) {
            recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
            beginErrorCondition(recognizer);
        }
    }

    /**
     * Recovery inline personalizzato: intercetta errori specifici come
     * mancanza di ::=, ; o parentesi chiuse.
     */
    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
        IntervalSet expectedTokenSet = recognizer.getExpectedTokens();
        TokenStream tokens = recognizer.getInputStream();
        Token currentToken = tokens.LT(1);

        // Gestione caso: token unito a parentesi senza spazio (es. (A)
        if (handleParenthesisSpaceIssue(recognizer, currentToken)) {
            return currentToken;
        }

        // Gestione caso: manca l'operatore ::= (ASSIGN)
        if (expectedTokenSet.contains(G2JParser.ASSIGN)) {
            return injectMissingToken(
                    currentToken,
                    G2JParser.ASSIGN,
                    "::=",
                    "Manca l'operatore di assegnazione '::='",
                    "inserito operatore '::=' mancante"
            );
        }

        // Gestione caso: manca il punto e virgola ';'
        if (expectedTokenSet.contains(G2JParser.SEMICOLON)) {
            System.err.println("⚠️ Errore sintattico alla linea " + currentToken.getLine() + ":" +
                    currentToken.getCharPositionInLine());
            System.err.println("    Manca il punto e virgola ';' alla fine della regola");
            System.err.println("    Recovery: salto fino al prossimo ';'");

            sintaxErrorNum++;

            // Consuma fino a SEMICOLON o EOF
            while (currentToken.getType() != G2JParser.SEMICOLON &&
                    currentToken.getType() != Token.EOF) {
                recognizer.consume();
                currentToken = tokens.LT(1);
            }

            // Consuma anche il SEMICOLON se lo trova
            if (currentToken.getType() == G2JParser.SEMICOLON) {
                recognizer.consume();
            }

            // Restituisci un token fittizio per continuare
            return new CommonToken(Token.INVALID_TYPE);
        }

        // Gestione caso: parentesi chiusa mancante
        for (int tokenType : expectedTokenSet.toList()) {
            if (isClosingBracket(tokenType)) {
                String delimName = getDelimiterName(tokenType);
                return injectMissingToken(
                        currentToken,
                        tokenType,
                        recognizer.getVocabulary().getLiteralName(tokenType).replace("'", ""),
                        "Manca la " + delimName + " chiusa",
                        "inserita " + delimName + " chiusa mancante"
                );
            }
        }

        // Altri casi generici → fallback su recover()
        InputMismatchException e = new InputMismatchException(recognizer);
        for (ParserRuleContext ctx = recognizer.getContext(); ctx != null; ctx = ctx.getParent()) {
            ctx.exception = e;
        }

        recover(recognizer, e);
        return new CommonToken(Token.INVALID_TYPE);
    }

    /**
     * Override di consumeUntil per loggare i token consumati durante la recovery.
     */
    @Override
    protected void consumeUntil(Parser recognizer, IntervalSet set) {
        TokenStream tokens = recognizer.getInputStream();
        int ttype = tokens.LA(1);

        while (ttype != Token.EOF && !set.contains(ttype)) {
            System.err.println("    Consumo token: " + tokens.LT(1).getText());
            tokens.consume();
            ttype = tokens.LA(1);
        }
    }

    // --- 🔧 Metodi di supporto ---

    /**
     * Crea un token virtuale per gestire errori specifici (es. ::=, parentesi).
     */
    private Token injectMissingToken(Token currentToken, int tokenType, String text, String errorMsg, String recoveryMsg) {
        CommonToken missingToken = new CommonToken(tokenType, text);
        missingToken.setLine(currentToken.getLine());
        missingToken.setCharPositionInLine(currentToken.getCharPositionInLine());

        System.err.println("⚠️ Errore sintattico alla linea " + currentToken.getLine() + ":" +
                currentToken.getCharPositionInLine());
        System.err.println("    " + errorMsg);
        System.err.println("    Recovery: " + recoveryMsg);

        sintaxErrorNum++;
        return missingToken;
    }

    /**
     * Rileva se un token contiene una parentesi attaccata al contenuto (es: "(A")
     */
    private boolean handleParenthesisSpaceIssue(Parser recognizer, Token token) {
        String text = token.getText();

        if (text.length() > 1) {
            char first = text.charAt(0);
            char last = text.charAt(text.length() - 1);

            if (first == '(' || first == '[' || first == '{') {
                System.err.println("Nota: Il token '" + text + "' potrebbe essere interpretato come '" +
                        first + " " + text.substring(1) + "'");
                return true;
            }

            if (last == ')' || last == ']' || last == '}') {
                System.err.println("Nota: Il token '" + text + "' potrebbe essere interpretato come '" +
                        text.substring(0, text.length() - 1) + " " + last + "'");
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se un tipo token rappresenta una parentesi chiusa.
     */
    private boolean isClosingBracket(int tokenType) {
        return tokenType == G2JParser.RIGHT_ROUND_BRACKET ||
                tokenType == G2JParser.RIGHT_SQUARE_BRACKET ||
                tokenType == G2JParser.RIGHT_CURLY_BRACKET;
    }

    private String getDelimiterName(int tokenType) {
        return delimiterNames.getOrDefault(tokenType, "parentesi");
    }
}
