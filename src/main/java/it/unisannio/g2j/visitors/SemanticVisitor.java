package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SemanticVisitor extends G2JBaseVisitor<Void> {

    private Set<String> definedNonTerminals = new HashSet<>();
    private Set<String> definedTerminals = new HashSet<>();
    private Set<String> usedNonTerminals = new HashSet<>();
    private Set<String> usedTerminals = new HashSet<>();

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
        System.out.println("Visiting Grammar File");
        return visitChildren(ctx);
    }

    @Override
    public Void visitRules(G2JParser.RulesContext ctx) {
        System.out.println("Visiting Rules");
        return visitChildren(ctx);
    }

    @Override
    public Void visitRule(G2JParser.RuleContext ctx) {
        System.out.println("Visiting Rule");
        return visitChildren(ctx);
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        String nonTerminal = ctx.NON_TERM().getText();
        definedNonTerminals.add(nonTerminal);
        System.out.println("Visiting Parsing Rule: " + nonTerminal);
        return visitChildren(ctx);
    }

    @Override
    public Void visitProductionList(G2JParser.ProductionListContext ctx) {
        System.out.println("Visiting Production List");
        return visitChildren(ctx);
    }

    @Override
    public Void visitProduction(G2JParser.ProductionContext ctx) {
        System.out.println("Visiting Production: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitElement(G2JParser.ElementContext ctx) {
        if (ctx.NON_TERM() != null) {
            String nonTerminal = ctx.NON_TERM().getText();
            usedNonTerminals.add(nonTerminal);
            System.out.println("Visiting Non-Terminal: " + nonTerminal);
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            String terminal = ctx.TERM().getText();
            usedTerminals.add(terminal);
            System.out.println("Visiting Terminal: " + terminal);
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitGrouping(G2JParser.GroupingContext ctx) {
        System.out.println("Visiting Grouping");
        return visitChildren(ctx);
    }

    @Override
    public Void visitOptionality(G2JParser.OptionalityContext ctx) {
        System.out.println("Visiting Optionality");
        return visitChildren(ctx);
    }

    @Override
    public Void visitRepetivity(G2JParser.RepetivityContext ctx) {
        System.out.println("Visiting Repetivity");
        return visitChildren(ctx);
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        definedTerminals.add(terminal);
        System.out.println("Visiting Lexical Rule: " + terminal);
        return visitChildren(ctx);
    }

    @Override
    public Void visitTerm(G2JParser.TermContext ctx) {
        System.out.println("Visiting Term: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitFactor(G2JParser.FactorContext ctx) {
        System.out.println("Visiting Factor: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitPrimary(G2JParser.PrimaryContext ctx) {
        System.out.println("Visiting Primary: " + ctx.getText());
        return visitChildren(ctx);
    }

    public void checkSemantics() {
        // Verifica che tutti i non terminali usati siano definiti
        for (String nonTerminal : usedNonTerminals) {
            if (!definedNonTerminals.contains(nonTerminal)) {
                System.err.println("Errore semantico: Non terminale non definito - " + nonTerminal);
            }
        }

        // Verifica che tutti i terminali usati siano definiti
        for (String terminal : usedTerminals) {
            if (!definedTerminals.contains(terminal)) {
                System.err.println("Errore semantico: Terminale non definito - " + terminal);
            }
        }

        // Verifica che non ci siano cicli nelle produzioni (questa è una versione semplificata)
        // Una versione più completa richiederebbe un'analisi più approfondita delle dipendenze
        for (String nonTerminal : definedNonTerminals) {
            if (usedNonTerminals.contains(nonTerminal)) {
                System.err.println("Avviso: Possibile ciclo nelle produzioni per - " + nonTerminal);
            }
        }
    }
}