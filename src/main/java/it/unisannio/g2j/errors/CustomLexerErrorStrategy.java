package it.unisannio.g2j.errors;

import it.unisannio.g2j.G2JLexer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.Pair;

public class CustomLexerErrorStrategy extends DefaultErrorStrategy {

    public static int lexErrorNum = 0;

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        Token offendingToken = e.getOffendingToken();

        if (offendingToken == null) return;

        int line = offendingToken.getLine();
        int charPosition = offendingToken.getCharPositionInLine();

        System.err.println("⚠️ Errore lessicale alla linea " + line + ":" + charPosition);
        System.err.println("    Token errato: '" + offendingToken.getText() + "'");

        lexErrorNum++;

        // Usa il metodo standard di consumo fino a un insieme di token
        IntervalSet followSet = getErrorRecoverySet(recognizer);
        consumeUntil(recognizer, followSet);
    }

    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
        Token currentToken = recognizer.getInputStream().LT(1);

        // Gestione dei non terminali
        if (currentToken.getType() == G2JLexer.NON_TERM) {
            System.err.println("⚠️ Errore lessicale: non terminale senza parentesi angolari");
            return createMissingToken(recognizer, G2JLexer.NON_TERM,
                    "<" + currentToken.getText() + ">", currentToken);
        }

        // Gestione delle stringhe
        if (currentToken.getType() == G2JLexer.STRING) {
            System.err.println("⚠️ Errore lessicale: stringa senza virgolette");
            return createMissingToken(recognizer, G2JLexer.STRING,
                    "\"" + currentToken.getText() + "\"", currentToken);
        }

        // Delega al metodo standard per altri tipi di token
        return super.recoverInline(recognizer);
    }

    private Token createMissingToken(Parser recognizer, int tokenType, String tokenText, Token currentToken) {
        // Simile al metodo getMissingSymbol di DefaultErrorStrategy
        Token lookback = recognizer.getInputStream().LT(-1);
        Token referenceToken = (currentToken.getType() == -1 && lookback != null) ? lookback : currentToken;

        lexErrorNum++;

        return recognizer.getTokenFactory().create(
                new Pair(referenceToken.getTokenSource(), referenceToken.getTokenSource().getInputStream()),
                tokenType,
                tokenText,
                0,
                -1,
                -1,
                referenceToken.getLine(),
                referenceToken.getCharPositionInLine()
        );
    }

    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        // Override per evitare doppia segnalazione
        if (!inErrorRecoveryMode(recognizer)) {
            beginErrorCondition(recognizer);
            System.err.println("Errore di riconoscimento: " + e.getMessage());
            recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
        }
    }

    public static int getLexerErrorNum() {
        return lexErrorNum;
    }
}