package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class JavaCCVisitor extends G2JBaseVisitor<Void> {

    private Set<String> definedNonTerminals = new HashSet<>();
    private Set<String> definedTerminals = new HashSet<>();
    private Set<String> usedNonTerminals = new HashSet<>();
    private Set<String> usedTerminals = new HashSet<>();
    private StringBuilder jjFileContent = new StringBuilder();

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
        jjFileContent.append("options {\n");
        jjFileContent.append("  STATIC = false;\n");
        jjFileContent.append("}\n\n");
        jjFileContent.append("PARSER_BEGIN(GrammarOut)\n");
        jjFileContent.append("public class GrammarOut {\n");
        jjFileContent.append("  public static void main(String[] args) throws ParseException {\n");
        jjFileContent.append("    Grammar parser = new Grammar(System.in);\n");
        jjFileContent.append("    parser.Program();\n");
        jjFileContent.append("  }\n");
        jjFileContent.append("}\n");
        jjFileContent.append("PARSER_END(GrammarOut)\n\n");
        return visitChildren(ctx);
    }

    @Override
    public Void visitRules(G2JParser.RulesContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Void visitRule(G2JParser.RuleContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        String nonTerminal = ctx.NON_TERM().getText();
        definedNonTerminals.add(nonTerminal);
        jjFileContent.append("void ").append(nonTerminal.replace("<", "").replace(">", "")).append("() :\n");
        jjFileContent.append("{\n");
        jjFileContent.append("}\n");
        jjFileContent.append("{\n");
        visit(ctx.productionList());
        jjFileContent.append("}\n\n");
        return null;
    }

    @Override
    public Void visitProductionList(G2JParser.ProductionListContext ctx) {
        for (int i = 0; i < ctx.production().size(); i++) {
            if (i > 0) {
                jjFileContent.append(" |\n");
            }
            visit(ctx.production(i));
        }
        return null;
    }

    @Override
    public Void visitProduction(G2JParser.ProductionContext ctx) {
        for (G2JParser.ElementContext element : ctx.element()) {
            visit(element);
        }
        return null;
    }

    @Override
    public Void visitElement(G2JParser.ElementContext ctx) {
        if (ctx.NON_TERM() != null) {
            String nonTerminal = ctx.NON_TERM().getText();
            usedNonTerminals.add(nonTerminal);
            jjFileContent.append(" ").append(nonTerminal.replace("<", "").replace(">", "")).append("()");
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            String terminal = ctx.TERM().getText();
            usedTerminals.add(terminal);
            jjFileContent.append(" <").append(terminal).append(">");
        } else if (ctx.grouping() != null) {
            visit(ctx.grouping());
        } else if (ctx.optionality() != null) {
            visit(ctx.optionality());
        } else if (ctx.repetivity() != null) {
            visit(ctx.repetivity());
        }
        return null;
    }

    @Override
    public Void visitGrouping(G2JParser.GroupingContext ctx) {
        jjFileContent.append(" (");
        visit(ctx.production());
        jjFileContent.append(")");
        return null;
    }

    @Override
    public Void visitOptionality(G2JParser.OptionalityContext ctx) {
        jjFileContent.append(" [");
        visit(ctx.production());
        jjFileContent.append("]");
        return null;
    }

    @Override
    public Void visitRepetivity(G2JParser.RepetivityContext ctx) {
        jjFileContent.append(" {");
        visit(ctx.production());
        jjFileContent.append("}");
        return null;
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        definedTerminals.add(terminal);
        jjFileContent.append("TOKEN : {\n");
        jjFileContent.append("  <").append(terminal).append(" : ");
        for (G2JParser.RegexContext regex : ctx.regex()) {
            visit(regex);
        }
        jjFileContent.append(">\n");
        jjFileContent.append("}\n\n");
        return null;
    }

    @Override
    public Void visitRegex(G2JParser.RegexContext ctx) {
        for (int i = 0; i < ctx.term().size(); i++) {
            if (i > 0) {
                jjFileContent.append(" | ");
            }
            visit(ctx.term(i));
        }
        return null;
    }

    @Override
    public Void visitTerm(G2JParser.TermContext ctx) {
        for (G2JParser.FactorContext factor : ctx.factor()) {
            visit(factor);
        }
        return null;
    }

    /**
     * Converte una classe di caratteri (es. [a-zA-Z]) nel formato ["a"-"z", "A"-"Z"].
     */
    private String convertCharClass(String charClass) {

        // Prendiamo in considerazione la sottostringa senza le parentesi
        charClass = charClass.substring(1, charClass.length() - 1);

        // Usiamo un StringBuilder per costruire il risultato
        StringBuilder result = new StringBuilder("[");
        int i = 0;
        while (i < charClass.length()) {
            if (i > 0) {
                result.append(", ");
            }
            // Se c'è un intervallo (es. a-z)
            if (i + 2 < charClass.length() && charClass.charAt(i + 1) == '-') {
                result.append("\"").append(charClass.charAt(i)).append("\"")
                        .append("-")
                        .append("\"").append(charClass.charAt(i + 2)).append("\"");
                i += 3; // Saltiamo i 3 caratteri (a-z)
            } else {
                // Singolo carattere
                result.append("\"").append(charClass.charAt(i)).append("\"");
                i += 1;
            }
        }
        result.append("]");
        return result.toString();
    }

    @Override
    public Void visitFactor(G2JParser.FactorContext ctx) {
        // Se ci sono quantificatori (*, +, ?), racchiudiamo l'espressione tra parentesi tonde
        if (ctx.KLEENE_CLOSURE() != null || ctx.POSITIVE_CLOSURE() != null || ctx.OPTIONALITY() != null) {
            jjFileContent.append("(");
            visit(ctx.primary());
            jjFileContent.append(")");
        } else {
            visit(ctx.primary());
        }

        // Aggiungiamo il quantificatore se presente
        if (ctx.KLEENE_CLOSURE() != null) {
            jjFileContent.append("*");
        } else if (ctx.POSITIVE_CLOSURE() != null) {
            jjFileContent.append("+");
        } else if (ctx.OPTIONALITY() != null) {
            jjFileContent.append("?");
        }
        return null;
    }

    @Override
    public Void visitPrimary(G2JParser.PrimaryContext ctx) {
        if (ctx.CHAR() != null) {
            jjFileContent.append("\"").append(ctx.CHAR().getText()).append("\"");

        } else if (ctx.ESCAPED_CHAR() != null) {
            jjFileContent.append(ctx.ESCAPED_CHAR().getText());

        } else if (ctx.DOT() != null) {
            jjFileContent.append(".");

        } else if (ctx.CHAR_CLASS() != null) {
            // La classe di caratteri (CHAR_CLASSES) viene trasformata nel formato ["a"-"z", "A"-"Z"]
            String charClass = ctx.CHAR_CLASS().getText();
            jjFileContent.append(convertCharClass(charClass));

        } else if (ctx.LEFT_ROUND_BRACKET() != null) {
            jjFileContent.append("(");
            visit(ctx.regex());
            jjFileContent.append(")");

        } else if (ctx.STRING() != null) {
            jjFileContent.append(ctx.STRING().getText());
        }
        return null;
    }

    public void writeOutputToFile(String fileName) {
        try (OutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(jjFileContent.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("File " + fileName + " generato con successo.");
        } catch (Exception e) {
            System.err.println("Errore durante la scrittura del file " + fileName + ": " + e.getMessage());
        }
    }
}