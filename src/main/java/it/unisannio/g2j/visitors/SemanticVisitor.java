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

    private void visitProductionList(G2JParser.ProductionListContext ctx, String currentNonTerminal) {
        for (G2JParser.ProductionContext production : ctx.production()) {
            List<String> elements = new ArrayList<>();
            visitProduction(production, elements); // Visita la produzione e raccoglie gli elementi
            productions.get(currentNonTerminal).add(elements); // Aggiunge la produzione al non terminale corrente
        }
    }

    private void visitProduction(G2JParser.ProductionContext ctx, List<String> elements) {
        System.out.println("Visiting production: " + ctx.getText());
        for (G2JParser.ElementContext element : ctx.element()) {
            visitElement(element, elements);
        }
        System.out.println("Elements: " + elements);
    }

    private void visitElement(G2JParser.ElementContext ctx, List<String> elements) {
        if (ctx.NON_TERM() != null) {
            elements.add(ctx.NON_TERM().getText());
            usedNonTerminals.add(ctx.NON_TERM().getText());
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            elements.add(ctx.TERM().getText());
            usedTerminals.add(ctx.TERM().getText());
        } else if (ctx.grouping() != null) {
            elements.add("(");
            visitProduction(ctx.grouping().production(), elements);
            elements.add(")");
        } else if (ctx.optionality() != null) {
            elements.add("[");
            visitProduction(ctx.optionality().production(), elements);
            elements.add("]");
        } else if (ctx.repetivity() != null) {
            elements.add("{");
            visitProduction(ctx.repetivity().production(), elements);
            elements.add("}");
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
            checkCommonPrefixes();
        }catch(SemanticException e) {
            System.err.println("❌ Catturata eccezione di tipo SemanticException 😡");
            System.err.println(e.getMessage());
        //    System.exit(1);
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
                throw new SemanticException("Errore semantico: Terminale non definito - " + terminal);
            }
        }
    }

    /**
     * 3. Verifica ricorsione sinistra
     */
    private void checkLeftRecursion() {
        for (String nonTerminal : definedNonTerminals) {
            if (isLeftRecursive(nonTerminal)) {
                System.out.println("\n\n⚠️ Attenzione: Ricorsione sinistra rilevata per - " + nonTerminal);
                suggestLeftRecursionElimination(nonTerminal);
            //    throw new SemanticException("Errore semantico: Ricorsione sinistra rilevata per - " + nonTerminal);
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
        //        throw new SemanticException("Errore semantico: Produzione non raggiungibile - " + nonTerminal);
            }
        }
    }

    /**
     * 5. Verifica prefissi comuni e suggerisce la fattorizzazione
     */
    private void checkCommonPrefixes() {
        for (String nonTerminal : definedNonTerminals) {
            List<List<String>> productionsForNT = productions.get(nonTerminal);
            if (productionsForNT.size() > 1) {
                // Trova il prefisso comune più lungo tra tutte le produzioni
                List<String> commonPrefix = findLongestCommonPrefix(productionsForNT);

                if (!commonPrefix.isEmpty()) {
                    System.out.println("\n⚠️ Prefisso comune rilevato per: " + nonTerminal);
                    System.out.println("Regola originale:");
                    System.out.print(nonTerminal + " ::= ");
                    for (int i = 0; i < productionsForNT.size(); i++) {
                        System.out.print(String.join(" ", productionsForNT.get(i)));
                        if (i < productionsForNT.size() - 1) {
                            System.out.print(" | ");
                        }
                    }
                    System.out.println(" ;");

                    // Suggerimento per la fattorizzazione
                    suggestFactorization(nonTerminal, commonPrefix, productionsForNT);
                }
            }
        }
    }

    /**
     * Trova il prefisso comune più lungo tra una lista di produzioni
     */
    private List<String> findLongestCommonPrefix(List<List<String>> productions) {
        if (productions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> commonPrefix = new ArrayList<>(productions.get(0));
        for (List<String> production : productions) {
            commonPrefix = getCommonPrefix(commonPrefix, production);
            if (commonPrefix.isEmpty()) {
                break;
            }
        }
        return commonPrefix;
    }

    /**
     * Trova il prefisso comune tra due liste
     */
    private List<String> getCommonPrefix(List<String> list1, List<String> list2) {
        List<String> prefix = new ArrayList<>();
        int minLength = Math.min(list1.size(), list2.size());
        for (int i = 0; i < minLength; i++) {
            if (list1.get(i).equals(list2.get(i))) {
                prefix.add(list1.get(i));
            } else {
                break;
            }
        }
        return prefix;
    }


    // ==================== OTTIMIZZAZIONI ====================


    /**
     * 1. Eliminazione della ricorsione a sinistra
     */
    private void suggestLeftRecursionElimination(String nonTerminal) {
        // Trova tutte le produzioni con ricorsione sinistra
        List<List<String>> leftRecursiveProductions = new ArrayList<>();
        List<List<String>> nonLeftRecursiveProductions = new ArrayList<>();

        for (List<String> production : productions.get(nonTerminal)) {
            if (!production.isEmpty() && production.get(0).equals(nonTerminal)) {
                leftRecursiveProductions.add(production);
            } else {
                nonLeftRecursiveProductions.add(production);
            }
        }

        if (!leftRecursiveProductions.isEmpty()) {
            // Stampa la regola originale
            System.out.println("\nRegola che contiene la ricorsione a sinistra:");
            System.out.print(nonTerminal + " ::= ");
            for (int i = 0; i < productions.get(nonTerminal).size(); i++) {
                List<String> production = productions.get(nonTerminal).get(i);
                System.out.print(String.join(" ", production));
                if (i < productions.get(nonTerminal).size() - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println(" ;");

            // Suggerimento per eliminare la ricorsione sinistra
            System.out.println("\nSuggerimento per eliminare la ricorsione sinistra:");

            // Crea un nuovo non terminale per gestire la ricorsione
            String newNonTerminal = nonTerminal.replace(">", "Tail>"); // Esempio: <Expression> -> <ExpressionTail>

            // Se ci sono produzioni non ricorsive a sinistra, usale
            if (!nonLeftRecursiveProductions.isEmpty()) {
                System.out.println(nonTerminal + " ::= " + String.join(" ", nonLeftRecursiveProductions.get(0)) + " " + newNonTerminal + " ;");
            } else {
                // Se non ci sono produzioni non ricorsive, usa una produzione vuota
                System.out.println(nonTerminal + " ::= " + newNonTerminal + " ;");
            }

            // Aggiungi le produzioni per il nuovo non terminale
            System.out.print(newNonTerminal + " ::= ");
            for (int i = 0; i < leftRecursiveProductions.size(); i++) {
                List<String> production = leftRecursiveProductions.get(i);
                // Rimuovi il primo elemento (il non terminale ricorsivo)
                List<String> newProduction = new ArrayList<>(production.subList(1, production.size()));
                System.out.print(String.join(" ", newProduction) + " " + newNonTerminal);
                if (i < leftRecursiveProductions.size() - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println(" | ε ;");
            System.out.println("\n");
        }
    }

    /**
     * 2. Fattorizzazione a prefisso comune
     */
    private void suggestFactorization(String nonTerminal, List<String> commonPrefix, List<List<String>> productionsForNT) {
        System.out.println("\nSuggerimento per la fattorizzazione:");

        // Crea un nuovo non terminale per gestire la parte rimanente
        String newNonTerminal = nonTerminal.replace(">", "Suffix>"); // Esempio: <Expression> -> <ExpressionSuffix>

        // Stampa la nuova produzione fattorizzata
        System.out.print(nonTerminal + " ::= ");
        System.out.print(String.join(" ", commonPrefix) + " " + newNonTerminal + " ;\n");

        // Stampa le produzioni per il nuovo non terminale
        System.out.print(newNonTerminal + " ::= ");
        for (int i = 0; i < productionsForNT.size(); i++) {
            List<String> production = productionsForNT.get(i);
            if (production.size() > commonPrefix.size()) {
                List<String> suffix = production.subList(commonPrefix.size(), production.size());
                System.out.print(String.join(" ", suffix));
            } else {
                System.out.print("ε"); // Se non c'è suffisso, usa la stringa vuota
            }
            if (i < productionsForNT.size() - 1) {
                System.out.print(" | ");
            }
        }
        System.out.println(" ;\n");
    }

}