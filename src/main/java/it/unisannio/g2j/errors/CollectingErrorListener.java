package it.unisannio.g2j.errors;

import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class CollectingErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        errors.add("Errore alla linea " + line + ":" + charPositionInLine + " - " + msg);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }
}
