package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class AntlrVisitor extends G2JBaseVisitor<Void> {

    private StringBuilder g4FileContent = new StringBuilder();

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
        g4FileContent.append("grammar GrammarOut;\n\n");
        return visitChildren(ctx);
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        g4FileContent.append(terminal).append(" : ");
        for (G2JParser.RegexContext regex : ctx.regex()) {
            visit(regex);
        }
        g4FileContent.append(";\n");
        return null;
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        // Trasforma il primo carattere del non terminale in minuscolo
        String nonTerminal = ctx.NON_TERM().getText().replace("<", "").replace(">", "");
        nonTerminal = Character.toLowerCase(nonTerminal.charAt(0)) + nonTerminal.substring(1);
        g4FileContent.append(nonTerminal).append(" : ");
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
        for (G2JParser.ElementContext element : ctx.element()) {
            visit(element);
        }
        return null;
    }

    @Override
    public Void visitElement(G2JParser.ElementContext ctx) {
        if (ctx.NON_TERM() != null) {
            // Trasforma il primo carattere del non terminale in minuscolo
            String nonTerminal = ctx.NON_TERM().getText().replace("<", "").replace(">", "");
            nonTerminal = Character.toLowerCase(nonTerminal.charAt(0)) + nonTerminal.substring(1);
            g4FileContent.append(nonTerminal).append(" ");
        } else if (ctx.TERM() != null) {
            String terminal = ctx.TERM().getText();
            g4FileContent.append(terminal).append(" ");
        } else if (ctx.STRING() != null) {
            g4FileContent.append(ctx.STRING().getText()).append(" ");
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
            g4FileContent.append(ctx.STRING().getText());
        }
        return null;
    }

    public void writeOutputToFile(String fileName) {
        try (OutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(g4FileContent.toString().getBytes());
            System.out.println("File " + fileName + " generato con successo.");
        } catch (Exception e) {
            System.err.println("Errore durante la scrittura del file " + fileName + ": " + e.getMessage());
        }
    }
}