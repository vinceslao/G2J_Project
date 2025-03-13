package it.unisannio.g2j.errors;

import it.unisannio.g2j.G2JParser;
import org.antlr.v4.runtime.*;

public class CustomErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        // Stampa un messaggio di errore
        System.err.println("⚠️ Errore sintattico alla linea " + line + ":" + charPositionInLine + " - " + msg);

        // Recupera il parser e imposta lo stato di recovery
        Parser parser = (Parser) recognizer;
        parser.getContext(); // Ottieni il contesto corrente

        // Skippa fino al punto e virgola (fine della regola corrente)
        TokenStream tokens = parser.getTokenStream();
        tokens.consume(); // Consuma il token corrente (quello che ha causato l'errore)
        while (tokens.LA(1) != Token.EOF && !(tokens.LA(1) == G2JParser.SEMICOLON)) {
            tokens.consume(); // Continua a consumare token fino al prossimo punto e virgola
        }
    }
}