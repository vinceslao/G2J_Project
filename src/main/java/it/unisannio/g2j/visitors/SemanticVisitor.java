package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;
import it.unisannio.g2j.exceptions.SemanticException;

import java.util.*;

public class SemanticVisitor extends G2JBaseVisitor<Void> {

    private Set<String> definedNonTerminals = new HashSet<>();
    private Set<String> definedTerminals = new HashSet<>();
    private Set<String> usedNonTerminals = new HashSet<>();
    private Set<String> usedTerminals = new HashSet<>();
    private Map<String, List<List<String>>> productions = new HashMap<>();

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
        productions.put(nonTerminal, new ArrayList<>()); // Inizializza la lista di produzioni per questo non terminale
        System.out.println("Visiting Parsing Rule: " + nonTerminal);
        visitProductionList(ctx.productionList(), nonTerminal); // Passa il non terminale corrente
        return null;
    }

    private Void visitProductionList(G2JParser.ProductionListContext ctx, String currentNonTerminal) {
        for (G2JParser.ProductionContext production : ctx.production()) {
            List<String> elements = new ArrayList<>();
            visitProduction(production, elements); // Visita la produzione e raccoglie gli elementi
            productions.get(currentNonTerminal).add(elements); // Aggiunge la produzione al non terminale corrente
        }
        return null;
    }

    private void visitProduction(G2JParser.ProductionContext ctx, List<String> elements) {
        for (G2JParser.ElementContext element : ctx.element()) {
            visitElement(element, elements); // Visita ogni elemento della produzione
        }
    }

    private void visitElement(G2JParser.ElementContext ctx, List<String> elements) {
        if (ctx.NON_TERM() != null) {
            String nonTerminal = ctx.NON_TERM().getText();
            usedNonTerminals.add(nonTerminal);
            elements.add(nonTerminal); // Aggiunge il non terminale alla produzione
            System.out.println("Visiting Non-Terminal: " + nonTerminal);
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            String terminal = ctx.TERM().getText();
            usedTerminals.add(terminal);
            elements.add(terminal); // Aggiunge il terminale alla produzione
            System.out.println("Visiting Terminal: " + terminal);
        } else if (ctx.grouping() != null) {
            visitGrouping(ctx.grouping(), elements); // Gestisce i raggruppamenti
        } else if (ctx.optionality() != null) {
            visitOptionality(ctx.optionality(), elements); // Gestisce le opzionalità
        } else if (ctx.repetivity() != null) {
            visitRepetivity(ctx.repetivity(), elements); // Gestisce le ripetizioni
        }
    }

    private void visitGrouping(G2JParser.GroupingContext ctx, List<String> elements) {
        elements.add("("); // Apre il raggruppamento
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add(")"); // Chiude il raggruppamento
    }

    private void visitOptionality(G2JParser.OptionalityContext ctx, List<String> elements) {
        elements.add("["); // Apre l'opzionalità
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add("]"); // Chiude l'opzionalità
    }

    private void visitRepetivity(G2JParser.RepetivityContext ctx, List<String> elements) {
        elements.add("{"); // Apre la ripetizione
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add("}"); // Chiude la ripetizione
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        definedTerminals.add(terminal);
        System.out.println("Visiting Lexical Rule: " + terminal);
        return visitChildren(ctx);
    }


    // ==================== Metodi di Analisi Semantica ====================

    public void checkSemantics() {
        try {
            checkNotUsedNonTerminals();
            checkNotUsedTerminals();
            checkLeftRecursion();
            checkUnreachableProductions();
        }catch(SemanticException e) {
            System.err.println("❌ Catturata eccezione di tipo SemanticException 😡");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     *  1. Verifica che tutti i non terminali usati siano definiti
     */
    private void checkNotUsedNonTerminals() {
        for (String nonTerminal : usedNonTerminals) {
            if (!definedNonTerminals.contains(nonTerminal)) {
                throw new SemanticException("Errore semantico: Non terminale non definito - " + nonTerminal);
            }
        }
    }

    /**
     * 2. Verifica che tutti i terminali usati siano definiti
     */
    private void checkNotUsedTerminals() {
        for (String terminal : usedTerminals) {
            if (!definedTerminals.contains(terminal)) {
                throw new SemanticException("Errore semantico: Terminal non definito - " + terminal);
            }
        }
    }

    /**
     * 3. Verifica ricorsione sinistra
     */
    private void checkLeftRecursion() {
        for (String nonTerminal : definedNonTerminals) {
            if (isLeftRecursive(nonTerminal)) {
                throw new SemanticException("Errore semantico: Ricorsione sinistra rilevata per - " + nonTerminal);
            }
        }
    }

    private boolean isLeftRecursive(String nonTerminal) {
        for (List<String> production : productions.get(nonTerminal)) {
            if (!production.isEmpty() && production.get(0).equals(nonTerminal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 4. Verifica produzioni non raggiungibili
     */
    private void checkUnreachableProductions() {
        Set<String> reachable = new HashSet<>();
        reachable.add("<Program>");

        boolean changed;
        do {
            changed = false;
            Set<String> reachableCopy = new HashSet<>(reachable); // Copia temporanea
            for (String nonTerminal : reachableCopy) {
                for (List<String> production : productions.get(nonTerminal)) {
                    for (String symbol : production) {
                        if (definedNonTerminals.contains(symbol) && !reachable.contains(symbol)) {
                            reachable.add(symbol); // Modifica il Set `reachable`
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        for (String nonTerminal : definedNonTerminals) {
            if (!reachable.contains(nonTerminal)) {
                throw new SemanticException("Errore semantico: Produzione non raggiungibile - " + nonTerminal);
            }
        }
    }
}