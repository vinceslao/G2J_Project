package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;
import it.unisannio.g2j.symbols.SymbolTable;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AntlrVisitor extends G2JBaseVisitor<Void> {

    private SymbolTable symbolTable = new SymbolTable();
    private StringBuilder g4FileContent = new StringBuilder();

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
        g4FileContent.append("grammar GrammarOut;\n\n");

        // First pass to build the symbol table
        visitChildren(ctx);

        // Optional: print symbol table for debugging
        // symbolTable.printSymbolTable();

        return null;
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();

        // Build regex definition for the symbol table
        StringBuilder regexDef = new StringBuilder();
        for (G2JParser.RegexContext regex : ctx.regex()) {
            // Store original position to restore after
            int originalLength = g4FileContent.length();

            // Use visitor to build the regex
            visit(regex);

            // Extract the regex from the g4FileContent
            String regexContent = g4FileContent.substring(originalLength);
            regexDef.append(regexContent);

            // Restore g4FileContent
            g4FileContent.setLength(originalLength);
        }

        // Add terminal to symbol table with its definition
        symbolTable.addTerminal(terminal, regexDef.toString());

        // Now build the actual token definition in the G4 file
        g4FileContent.append(terminal).append(" : ");

        // Re-visit regex nodes to add their content to g4FileContent
        for (G2JParser.RegexContext regex : ctx.regex()) {
            visit(regex);
        }

        g4FileContent.append(";\n");
        return null;
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        // Trasforma il primo carattere del non terminale in minuscolo
        String nonTerminal = ctx.NON_TERM().getText();
        String formattedNonTerminal = nonTerminal.replace("<", "").replace(">", "");
        formattedNonTerminal = Character.toLowerCase(formattedNonTerminal.charAt(0)) + formattedNonTerminal.substring(1);

        // Add to symbol table with original name
        symbolTable.addNonTerminal(nonTerminal);

        g4FileContent.append(formattedNonTerminal).append(" : ");
        visit(ctx.productionList());
        g4FileContent.append(";\n");
        return null;
    }

    @Override
    public Void visitProductionList(G2JParser.ProductionListContext ctx) {
        for (int i = 0; i < ctx.production().size(); i++) {
            if (i > 0) {
                g4FileContent.append(" | ");
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
            } else if (element.TERM() != null) {
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
            // Trasforma il primo carattere del non terminale in minuscolo
            String nonTerminal = ctx.NON_TERM().getText();
            symbolTable.markAsUsed(nonTerminal);

            String formattedNonTerminal = nonTerminal.replace("<", "").replace(">", "");
            formattedNonTerminal = Character.toLowerCase(formattedNonTerminal.charAt(0)) + formattedNonTerminal.substring(1);

            g4FileContent.append(formattedNonTerminal).append(" ");
        } else if (ctx.TERM() != null) {
            String terminal = ctx.TERM().getText();
            symbolTable.markAsUsed(terminal);
            g4FileContent.append(terminal).append(" ");
        } else if (ctx.grouping() != null || ctx.optionality() != null || ctx.repetivity() != null) {
            visitChildren(ctx);
        }
        return null;
    }

    @Override
    public Void visitGrouping(G2JParser.GroupingContext ctx) {
        g4FileContent.append("(");
        visit(ctx.production());
        g4FileContent.append(")");
        return null;
    }

    @Override
    public Void visitOptionality(G2JParser.OptionalityContext ctx) {
        g4FileContent.append("(");
        visit(ctx.production());
        g4FileContent.append(")?");
        return null;
    }

    @Override
    public Void visitRepetivity(G2JParser.RepetivityContext ctx) {
        g4FileContent.append("(");
        visit(ctx.production());
        g4FileContent.append(")*");
        return null;
    }

    @Override
    public Void visitRegex(G2JParser.RegexContext ctx) {
        for (int i = 0; i < ctx.term().size(); i++) {
            if (i > 0) {
                g4FileContent.append(" | ");
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

    @Override
    public Void visitFactor(G2JParser.FactorContext ctx) {
        visit(ctx.primary());
        if (ctx.KLEENE_CLOSURE() != null) {
            g4FileContent.append("*");
        } else if (ctx.POSITIVE_CLOSURE() != null) {
            g4FileContent.append("+");
        } else if (ctx.OPTIONALITY() != null) {
            g4FileContent.append("?");
        }
        return null;
    }

    @Override
    public Void visitPrimary(G2JParser.PrimaryContext ctx) {
        if (ctx.CHAR() != null) {
            g4FileContent.append(ctx.CHAR().getText());
        } else if (ctx.ESCAPED_CHAR() != null) {
            g4FileContent.append(ctx.ESCAPED_CHAR().getText());
        } else if (ctx.DOT() != null) {
            g4FileContent.append(".");
        } else if (ctx.CHAR_CLASS() != null) {
            g4FileContent.append(ctx.CHAR_CLASS().getText());
        } else if (ctx.LEFT_ROUND_BRACKET() != null) {
            g4FileContent.append("(");
            visit(ctx.regex());
            g4FileContent.append(")");
        } else if (ctx.STRING() != null) {
            String literal = ctx.STRING().getText().replace("\"", "'"); // Sostituisci " con '
            g4FileContent.append(literal).append(" ");
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
            outputStream.write(g4FileContent.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ File " + fileName + " generato con successo.");
        } catch (Exception e) {
            System.err.println("Errore durante la scrittura del file " + fileName + ": " + e.getMessage());
        }
    }
}