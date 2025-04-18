package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;
import it.unisannio.g2j.symbols.SymbolTable;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaCCVisitor extends G2JBaseVisitor<Void> {

    private SymbolTable symbolTable = new SymbolTable();
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

        // First pass to build the symbol table
        visitChildren(ctx);

        // symbolTable.printSymbolTable();

        return null;
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        String nonTerminal = ctx.NON_TERM().getText();
        symbolTable.addNonTerminal(nonTerminal);

        jjFileContent.append("void ").append(nonTerminal.replace("<", "").replace(">", "")).append("() :\n");
        jjFileContent.append("{\n");
        jjFileContent.append("}\n");
        jjFileContent.append("{\n");

        visit(ctx.productionList());

        if (nonTerminal.equals("<Program>")) {
            jjFileContent.append(" <EOF>");
        }

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
        // For tracking symbols in this production to add to the symbol table later
        List<String> productionSymbols = new ArrayList<>();

        for (G2JParser.ElementContext element : ctx.element()) {
            if (element.NON_TERM() != null) {
                productionSymbols.add(element.NON_TERM().getText());
            } else if (element.TERM() != null && !Objects.equals(element.TERM().getText(), "EOF")) {
                productionSymbols.add(element.TERM().getText());
            }

            visit(element);
        }

        // If we're in a parse rule context, add this production to the current non-terminal
        if (ctx.getParent() instanceof G2JParser.ProductionListContext &&
                ctx.getParent().getParent() instanceof G2JParser.ParseRuleContext) {
            String nonTerminal = ((G2JParser.ParseRuleContext) ctx.getParent().getParent()).NON_TERM().getText();
            symbolTable.addProduction(nonTerminal, productionSymbols);
        }

        return null;
    }

    @Override
    public Void visitElement(G2JParser.ElementContext ctx) {
        if (ctx.NON_TERM() != null) {
            String nonTerminal = ctx.NON_TERM().getText();
            symbolTable.markAsUsed(nonTerminal);
            jjFileContent.append(" ").append(nonTerminal.replace("<", "").replace(">", "")).append("()");
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            String terminal = ctx.TERM().getText();
            symbolTable.markAsUsed(terminal);
            jjFileContent.append(" <").append(terminal).append(">");
        } else if (ctx.grouping() != null) {
            visit(ctx.grouping());
        } else if (ctx.optionality() != null) {
            visit(ctx.optionality());
        } else if (ctx.repetivity() != null) {
            visit(ctx.repetivity());
        } else if (ctx.rep_opt() != null) {
            visit(ctx.rep_opt());
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
        jjFileContent.append(" (");
        visit(ctx.production());
        jjFileContent.append(")+ ");
        return null;
    }

    public Void visitRep_opt(G2JParser.Rep_optContext ctx) {

        jjFileContent.append("(");
        visit(ctx.production());
        jjFileContent.append(")*");

        return null;
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();

        // Build regex definition for the symbol table
        StringBuilder regexDef = new StringBuilder();
        for (G2JParser.RegexContext regex : ctx.regex()) {
            // Store original position to restore after
            int originalLength = jjFileContent.length();

            // Use visitor to build the regex
            visit(regex);

            // Extract the regex from the jjFileContent
            String regexContent = jjFileContent.substring(originalLength);
            regexDef.append(regexContent);

            // Restore jjFileContent
            jjFileContent.setLength(originalLength);
        }

        // Add terminal to symbol table with its definition
        symbolTable.addTerminal(terminal, regexDef.toString());

        // Now build the actual token definition in the JJ file
        jjFileContent.append("TOKEN : {\n");
        jjFileContent.append("  <").append(terminal).append(" : ");

        // Re-visit regex nodes to add their content to jjFileContent
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

    /**
     * Gets the symbol table built during traversal
     * @return Symbol table containing grammar symbols
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void writeOutputToFile(String fileName) {
        try (OutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(jjFileContent.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ File " + fileName + " generato con successo.");
        } catch (Exception e) {
            System.err.println("Errore durante la scrittura del file " + fileName + ": " + e.getMessage());
        }
    }

}