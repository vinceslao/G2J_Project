package it.unisannio.g2j.visitors;

import it.unisannio.g2j.G2JBaseVisitor;
import it.unisannio.g2j.G2JParser;
import it.unisannio.g2j.exceptions.SemanticException;
import it.unisannio.g2j.symbols.SymbolTable;

import java.io.IOException;
import java.util.*;

public class SemanticVisitor extends G2JBaseVisitor<Void> {

    // Symbol table for managing symbols
    private final SymbolTable symbolTable = new SymbolTable();

    // Maps and sets for optimization
    private Map<String, List<List<String>>> optimizedProductions = new HashMap<>();
    private Set<String> newNonTerminals = new HashSet<>();
    private boolean grammarModified = false;

    // Keep track of recursion symbols
    private int numRecursionSymbols = 0;

    // Set of symbols that should be ignored for symbol usage tracking (delimiters, etc.)
    private final Set<String> ignoredSymbols = new HashSet<>(
            Arrays.asList("(", ")", "[", "]", "{", "}")
    );

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
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
        symbolTable.addNonTerminal(nonTerminal);
        symbolTable.markAsDefined(nonTerminal);
        visitProductionList(ctx.productionList(), nonTerminal);
        return visitChildren(ctx);
    }

    private void visitProductionList(G2JParser.ProductionListContext ctx, String currentNonTerminal) {
        for (G2JParser.ProductionContext production : ctx.production()) {
            List<String> elements = new ArrayList<>();
            visitProduction(production, elements); // Collect elements from the production
            symbolTable.addProduction(currentNonTerminal, elements);
        }
    }

    private void visitProduction(G2JParser.ProductionContext ctx, List<String> elements) {
        for (G2JParser.ElementContext element : ctx.element()) {
            visitElement(element, elements);
        }
    }

    private void visitElement(G2JParser.ElementContext ctx, List<String> elements) {
        if (ctx.NON_TERM() != null) {
            String nonTerm = ctx.NON_TERM().getText();
            elements.add(nonTerm);
            symbolTable.markAsUsed(nonTerm);
        } else if (ctx.TERM() != null && !Objects.equals(ctx.TERM().getText(), "EOF")) {
            String term = ctx.TERM().getText();
            elements.add(term);
            symbolTable.markAsUsedTerminal(term);
        } else if (ctx.grouping() != null) {
            visitGrouping(ctx.grouping(), elements);
        } else if (ctx.optionality() != null) {
            visitOptionality(ctx.optionality(), elements);
        } else if (ctx.repetivity() != null) {
            visitRepetivity(ctx.repetivity(), elements);
        } else if (ctx.rep_opt() != null) {
            visitRep_opt(ctx.rep_opt(), elements);
        }
    }

    private void visitGrouping(G2JParser.GroupingContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_ROUND_BRACKET().getText()); // Add opening bracket
        visitProduction(ctx.production(), elements);      // Visit internal production
        elements.add(ctx.RIGHT_ROUND_BRACKET().getText()); // Add closing bracket
    }

    private void visitOptionality(G2JParser.OptionalityContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_SQUARE_BRACKET().getText()); // Add opening bracket
        visitProduction(ctx.production(), elements);      // Visit internal production
        elements.add(ctx.RIGHT_SQUARE_BRACKET().getText()); // Add closing bracket
    }

    private void visitRepetivity(G2JParser.RepetivityContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_CURLY_BRACKET().getText()); // Add opening bracket
        visitProduction(ctx.production(), elements);      // Visit internal production
        elements.add(ctx.RIGHT_CURLY_BRACKET().getText()); // Add closing bracket
    }

    private void visitRep_opt(G2JParser.Rep_optContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_CURLY_BRACKET().getText());
        elements.add(ctx.LEFT_SQUARE_BRACKET().getText());
        visitProduction(ctx.production(), elements);
        elements.add(ctx.RIGHT_SQUARE_BRACKET().getText());
        elements.add(ctx.RIGHT_CURLY_BRACKET().getText());
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        String regexDef = ctx.getText().substring(terminal.length() + "::=".length());
        symbolTable.addTerminal(terminal, regexDef);
        symbolTable.markAsDefined(terminal);
        return visitChildren(ctx);
    }

    // ==================== Semantic Analysis Methods ====================

    public void checkSemantics() {
        try {
            symbolTable.markAsUsed("<Program>");

            checkNotDefinedNonTerminals();
            checkNotDefinedTerminals();
            checkNotUsedNonTerminals();
            checkNotUsedTerminals();

            symbolTable.printSymbolTable();
        } catch(SemanticException e) {
            System.err.println("❌ Catturata eccezione di tipo SemanticException 😡");
            System.err.println(e.getMessage());
       //     System.exit(1);
        }
    }

    /**
     * 1. Verifica che tutti i non terminali usati siano definiti.
     */
    private void checkNotDefinedNonTerminals() {
        Set<String> usedNonTerminals = symbolTable.getUsedNonTerminals();
        Set<String> definedNonTerminals = symbolTable.getDefinedNonTerminals();
        ArrayList<String> errors = new ArrayList<>();

        for (String nonTerminal : usedNonTerminals) {
            if (!definedNonTerminals.contains(nonTerminal)) {
                errors.add(nonTerminal);
            }
        }
        if(!errors.isEmpty()) {
            throw new SemanticException("Errore semantico: Non terminale usato ma NON DEFINITO - " + errors);
        }
    }

    /**
     * 2. Verifica che tutti i terminali usati siano definiti.
     */
    private void checkNotDefinedTerminals() {
        Set<String> usedTerminals = symbolTable.getUsedTerminals();
        Set<String> definedTerminals = symbolTable.getDefinedTerminals();
        ArrayList<String> errors = new ArrayList<>();

        for (String terminal : usedTerminals) {
            if (!definedTerminals.contains(terminal)) {
                errors.add(terminal);
            }
        }
        if(!errors.isEmpty()) {
            throw new SemanticException("Errore semantico: Terminale usato ma NON DEFINITO - " + errors);
        }
    }

    /**
     * 3. Verifica che tutti i non terminali definiti siano usati.
     */
    private void checkNotUsedNonTerminals(){
        Set<String> usedNonTerminals = symbolTable.getUsedNonTerminals();
        Set<String> definedNonTerminals = symbolTable.getDefinedNonTerminals();
        ArrayList<String> errors = new ArrayList<>();

        for (String nonTerminal : definedNonTerminals) {
            if (!usedNonTerminals.contains(nonTerminal)) {
                errors.add(nonTerminal);
            }
        }
        if(!errors.isEmpty()) {
            throw new SemanticException("Errore semantico: Non terminale definito ma NON USATO - " + errors);
        }
    }

    /**
     * 4. Verifica che tutti i terminali definiti siano usati.
     */
    private void checkNotUsedTerminals(){
        Set<String> usedTerminals = symbolTable.getUsedTerminals();
        Set<String> definedTerminals = symbolTable.getDefinedTerminals();
        ArrayList<String> errors = new ArrayList<>();


        for (String terminal : definedTerminals) {
            if (!usedTerminals.contains(terminal)) {
                errors.add(terminal);
            }
        }

        if(!errors.isEmpty()){
            throw new SemanticException("Errore semantico: Terminale definito ma NON USATO - " + errors);
        }
    }

    // ============================== OTTIMIZZAZIONI =================================

    /**
     * 1. Eliminazione della ricorsione a sinistra
     */
    private void eliminateLeftRecursion() {
        // Initialize optimized productions with original ones if not already done
        if (optimizedProductions.isEmpty()) {
            optimizedProductions = new HashMap<>(symbolTable.getAllProductions());
        }

        for (String nonTerminal : new HashSet<>(symbolTable.getDefinedNonTerminals())) {
            if (hasLeftRecursion(nonTerminal)) {
                System.out.println("\n\n⚠️ Ricorsione sinistra rilevata per - " + nonTerminal);
                applyLeftRecursionElimination(nonTerminal);
                grammarModified = true;
            }
        }
    }

    private boolean hasLeftRecursion(String nonTerminal) {
        List<List<String>> prods = optimizedProductions.get(nonTerminal);
        if (prods == null) return false;

        for (List<String> production : prods) {
            if (!production.isEmpty() && production.get(0).equals(nonTerminal)) {
                numRecursionSymbols++;
                return true;
            }
        }
        return false;
    }

    private void applyLeftRecursionElimination(String nonTerminal) {
        // Find all productions with left recursion
        List<List<String>> leftRecursiveProductions = new ArrayList<>();
        List<List<String>> nonLeftRecursiveProductions = new ArrayList<>();

        for (List<String> production : optimizedProductions.get(nonTerminal)) {
            if (!production.isEmpty() && production.get(0).equals(nonTerminal)) {
                leftRecursiveProductions.add(production);
            } else {
                nonLeftRecursiveProductions.add(production);
            }
        }

        if (!leftRecursiveProductions.isEmpty()) {
            // Print original rule
            System.out.println("\nRegola che contiene la ricorsione a sinistra:");
            System.out.print(nonTerminal + " ::= ");
            for (int i = 0; i < optimizedProductions.get(nonTerminal).size(); i++) {
                List<String> production = optimizedProductions.get(nonTerminal).get(i);
                System.out.print(String.join(" ", production));
                if (i < optimizedProductions.get(nonTerminal).size() - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println(" ;");

            // Create a new non-terminal to handle recursion
            String newNonTerminal = nonTerminal.replace(">", "Tail>");
            newNonTerminals.add(newNonTerminal);

            // Update original production
            List<List<String>> newProductions = new ArrayList<>();

            if (!nonLeftRecursiveProductions.isEmpty()) {
                for (List<String> alpha : nonLeftRecursiveProductions) {
                    List<String> newProduction = new ArrayList<>(alpha);
                    newProduction.add("[" + newNonTerminal + "]");
                    newProductions.add(newProduction);
                }
            } else {
                // If there are no non-recursive productions, use an empty one
                List<String> emptyProduction = new ArrayList<>();
                emptyProduction.add("[" + newNonTerminal + "]");
                newProductions.add(emptyProduction);
            }

            optimizedProductions.put(nonTerminal, newProductions);

            // Create productions for the new non-terminal
            List<List<String>> tailProductions = new ArrayList<>();

            for (List<String> beta : leftRecursiveProductions) {
                List<String> newProduction = new ArrayList<>(beta.subList(1, beta.size()));
                newProduction.add("[" + newNonTerminal + "]");
                tailProductions.add(newProduction);
            }

            optimizedProductions.put(newNonTerminal, tailProductions);

            System.out.println("Regola ottimizzata:");
            System.out.println(nonTerminal + " ::= " + formatOptimizedProduction(newProductions) + " ;");
            System.out.println(newNonTerminal + " ::= " + formatOptimizedProduction(tailProductions) + " ;");
        }
    }

    /**
     * 2. Fattorizzazione dei prefissi comuni.
     */
    private void factorizeCommonPrefixes() {
        boolean factorizationApplied;

        do {
            factorizationApplied = false;

            for (String nonTerminal : new HashSet<>(symbolTable.getDefinedNonTerminals())) {
                List<List<String>> productionsForNT = optimizedProductions.get(nonTerminal);
                if (productionsForNT != null && productionsForNT.size() > 1) {
                    // Find the longest common prefix among all productions
                    List<String> commonPrefix = findLongestCommonPrefix(productionsForNT);

                    if (!commonPrefix.isEmpty()) {
                        System.out.println("\n⚠️ Prefisso comune rilevato per: " + nonTerminal);
                        System.out.println("Regola originale:");
                        System.out.print(nonTerminal + " ::= ");
                        System.out.println(formatOptimizedProduction(productionsForNT) + " ;");

                        // Apply factorization
                        applyFactorization(nonTerminal, commonPrefix, productionsForNT);
                        factorizationApplied = true;
                        grammarModified = true;
                        break;  // Start over after applying a factorization
                    }
                }
            }
        } while (factorizationApplied);  // Continue until no more factorizations are applied
    }

    private void applyFactorization(String nonTerminal, List<String> commonPrefix, List<List<String>> productionsForNT) {
        // Create a new non-terminal to handle suffixes
        String newNonTerminal = nonTerminal.replace(">", "Suffix>");

        // Ensure unique name
        int counter = 1;
        String originalName = newNonTerminal;
        while (symbolTable.containsSymbol(newNonTerminal) || newNonTerminals.contains(newNonTerminal)) {
            newNonTerminal = originalName.replace(">", counter + ">");
            counter++;
        }

        newNonTerminals.add(newNonTerminal);

        // Create new productions
        List<List<String>> newProductions = new ArrayList<>();
        List<String> newProduction = new ArrayList<>(commonPrefix);
        newProduction.add("[" + newNonTerminal + "]");
        newProductions.add(newProduction);

        // Create productions for the new non-terminal (suffixes)
        List<List<String>> suffixProductions = new ArrayList<>();

        for (List<String> production : productionsForNT) {
            if (production.size() > commonPrefix.size()) {
                List<String> suffix = new ArrayList<>(production.subList(commonPrefix.size(), production.size()));
                suffixProductions.add(suffix);
            }
        }

        // Update optimized productions
        optimizedProductions.put(nonTerminal, newProductions);
        optimizedProductions.put(newNonTerminal, suffixProductions);

        System.out.println("Regola fattorizzata:");
        System.out.println(nonTerminal + " ::= " + formatOptimizedProduction(newProductions) + " ;");
        System.out.println(newNonTerminal + " ::= " + formatOptimizedProduction(suffixProductions) + " ;");
    }

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

    // ============================== GENERAZIONE INPUT OTTIMIZZATO ===================================

    public void optimizeInput() {
        // Initialize optimized productions with original ones
        optimizedProductions = new HashMap<>(symbolTable.getAllProductions());

        // Apply optimizations
        eliminateLeftRecursion();
        factorizeCommonPrefixes();

        // Generate optimized file if modifications were made
        if (grammarModified) {
            generateOptimizedGrammarFile();
        }
    }

    /**
     * Formatta una lista di produzioni come stringa.
     */
    private String formatOptimizedProduction(List<List<String>> productions) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < productions.size(); i++) {
            List<String> production = productions.get(i);

            // Skip empty productions
            if (production.isEmpty()) {
                continue;
            }

            // Add production elements
            for (String s : production) {
                sb.append(s);
                sb.append(" ");
            }

            // Add "|" separator if there are more non-empty productions
            if (i < productions.size() - 1 && !productions.get(i + 1).isEmpty()) {
                sb.append(" | ");
            }
        }

        return sb.toString();
    }

    private void generateOptimizedGrammarFile() {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter("output/optimized_input.txt"))) {
            // Write lexical rules first
            for (String terminal : symbolTable.getOrderedTerminals()) {
                String ruleDef = symbolTable.getTerminalDefinition(terminal);
                writer.println(terminal + "::=" + ruleDef + ";");
            }

            writer.println(); // Add blank line between lexical and parsing rules

            // Write parsing rules in original order
            for (String nonTerminal : symbolTable.getOrderedNonTerminals()) {
                // Skip new non-terminals created during optimization
                if (newNonTerminals.contains(nonTerminal)) {
                    continue;
                }

                List<List<String>> prods = optimizedProductions.get(nonTerminal);
                if (prods != null && !prods.isEmpty()) {
                    writer.print(nonTerminal + " ::= ");
                    if (nonTerminal.equals("<Program>")) {
                        // Manually add EOF to <Program> rule
                        writer.print(formatOptimizedProduction(prods) + " EOF");
                    } else {
                        writer.print(formatOptimizedProduction(prods));
                    }
                    writer.println(" ;");
                }
            }

            // Write new non-terminals created during optimization
            for (String newNonTerminal : newNonTerminals) {
                List<List<String>> prods = optimizedProductions.get(newNonTerminal);
                if (prods != null && !prods.isEmpty()) {
                    writer.print(newNonTerminal + " ::= ");
                    writer.print(formatOptimizedProduction(prods));
                    writer.println(" ;");
                }
            }

            System.out.println("\n✅ Grammatica ottimizzata salvata nel file: " + "output/optimized_input.txt");
        } catch (IOException e) {
            System.err.println("Errore durante la scrittura del file di grammatica ottimizzata: " + e.getMessage());
        }
    }

    // ============================== CALCOLO DELLE METRICHE DI VALUTAZIONE ===================================

    /**
     * Calcola la complessità di McCabe della grammatica.
     * La complessità aumenta di 1 per ogni:
     * - Scelta (operatore |)
     * - Ripetizione (parentesi graffe {})
     * - Opzionalità (parentesi quadre [])
     * - Raggruppamento (parentesi tonde ())
     *
     * @param productionsMap Le produzioni da analizzare
     * @return La complessità di McCabe
     */
    private int calculateMcCabeForProductions(Map<String, List<List<String>>> productionsMap) {
        int complexity = 1; // Valore base

        // Conta le scelte (|) nelle produzioni
        for (String nonTerminal : productionsMap.keySet()) {
            List<List<String>> productionList = productionsMap.get(nonTerminal);
            if (productionList != null && productionList.size() > 1) {
                // Ogni produzione alternativa (oltre la prima) aumenta la complessità di 1
                complexity += productionList.size() - 1;
            }

            // Analizza ogni produzione per elementi di complessità
            if (productionList != null) {
                for (List<String> production : productionList) {
                    // Conta le strutture di controllo all'interno di ogni produzione
                    int openRoundBrackets = 0;
                    int openSquareBrackets = 0;
                    int openCurlyBrackets = 0;

                    for (String element : production) {
                        // Conta i raggruppamenti (parentesi tonde)
                        if (element.equals("(")) {
                            openRoundBrackets++;
                        } else if (element.equals(")")) {
                            if (openRoundBrackets > 0) {
                                complexity++; // Aumenta la complessità per ogni coppia di parentesi tonde
                                openRoundBrackets--;
                            }
                        }

                        // Conta le opzionalità (parentesi quadre)
                        else if (element.equals("[")) {
                            openSquareBrackets++;
                        } else if (element.equals("]")) {
                            if (openSquareBrackets > 0) {
                                complexity++; // Aumenta la complessità per ogni coppia di parentesi quadre
                                openSquareBrackets--;
                            }
                        }

                        // Conta le ripetizioni (parentesi graffe)
                        else if (element.equals("{")) {
                            openCurlyBrackets++;
                        } else if (element.equals("}")) {
                            if (openCurlyBrackets > 0) {
                                complexity++; // Aumenta la complessità per ogni coppia di parentesi graffe
                                openCurlyBrackets--;
                            }
                        }

                        // Controlla anche per le parentesi nel testo (come nelle regole ottimizzate)
                        else if (element.startsWith("[") && element.endsWith("]")) {
                            complexity++; // Opzionalità incorporata
                        }
                    }
                }
            }
        }

        return complexity;
    }

    /**
     * Calcola le metriche per l'input originale e ottimizzato
     */
    public void calcMetrics() {
        // Calcola le metriche per l'input originale
        Set<String> definedNonTerminals = symbolTable.getDefinedNonTerminals();
        Set<String> definedTerminals = symbolTable.getDefinedTerminals();
        Map<String, List<List<String>>> productions = symbolTable.getAllProductions();

        System.out.println("\n📐CALCOLO DELLE METRICHE SULL'INPUT ORIGINALE");
        System.out.println("Numero dei simboli non terminali: " + definedNonTerminals.size());
        System.out.println("Numero dei simboli terminali: " + definedTerminals.size());

        // Calcola il numero totale di produzioni per l'input originale
        double totalOriginalProductions = 0;
        int totalOriginalUnitProductions = 0; // Contatore per produzioni unitarie
        int originalRHSMax = 0; // Contatore per RHS max
        int sumProductionLength = 0;
        Set<String> delimiters = new HashSet<>(Arrays.asList("(", ")", "[", "]", "{", "}"));

        for (List<List<String>> productionList : productions.values()) {
            totalOriginalProductions += productionList.size();
            for (List<String> production : productionList) {
                // Count actual elements (excluding delimiters)
                int actualElements = 0;
                for (String element : production) {
                    if (!delimiters.contains(element)) {
                        actualElements++;
                    }
                }

                if (actualElements == 1) {
                    totalOriginalUnitProductions++;
                }
                if (actualElements > originalRHSMax) {
                    originalRHSMax = actualElements;
                }
                sumProductionLength += actualElements;
            }
        }

        // Calcola la quantità di simboli terminali e non terminali media delle produzioni sul lato destro
        double RHSMean = (sumProductionLength+1) / totalOriginalProductions;

        double alt = totalOriginalProductions / definedNonTerminals.size(); // media del numero di alternative per regola

        System.out.println("Numero di regole di produzione: " + totalOriginalProductions);
        System.out.println("Numero di produzioni unitarie: " + (totalOriginalUnitProductions-1));
        System.out.println("RHS max: " + originalRHSMax);
        System.out.println("RHS mean: " + RHSMean);
        System.out.println("ALT: " + alt);
        System.out.println("Numero di simboli ricorsivi rilevati: " + numRecursionSymbols);

        int originalComplexity = calculateMcCabeForProductions(productions);
        System.out.println("Complessità di McCabe della grammatica originale: " + originalComplexity);

        // Calcola le metriche per l'input ottimizzato
        System.out.println("\n📐CALCOLO DELLE METRICHE SULL'INPUT OTTIMIZZATO");

        // Include both original non-terminals and newly created ones
        Set<String> optimizedNonTerminals = new HashSet<>(definedNonTerminals);
        optimizedNonTerminals.addAll(newNonTerminals);

        System.out.println("Numero dei simboli non terminali: " + optimizedNonTerminals.size());
        System.out.println("Numero dei simboli terminali: " + definedTerminals.size()); // I terminali non cambiano

        // Calcola il numero totale di produzioni per l'input ottimizzato
        double totalOptimizedProductions = 0;
        int totalOptimizedUnitProductions = 0; // Contatore per produzioni unitarie
        int optimizedRHSMax = 0; // Contatore per RHS max
        int sumProductionLengthOpt = 0;

        for (List<List<String>> productionList : optimizedProductions.values()) {
            totalOptimizedProductions += productionList.size();
            for (List<String> production : productionList) {
                int actualElements = 0;
                for (String element : production) {
                    if (!delimiters.contains(element)) {
                        actualElements++;
                    }
                }
                if (actualElements == 1) { // Produzione unitaria
                    totalOptimizedUnitProductions++;
                }
                if (actualElements > optimizedRHSMax) { // Aggiorna RHS max
                    optimizedRHSMax = actualElements;
                }

                sumProductionLengthOpt += actualElements;
            }
        }

        double OptimizedRHSMean = (double) (sumProductionLengthOpt + 1) / totalOptimizedProductions;

        double alt_opt = totalOptimizedProductions / optimizedNonTerminals.size();

        System.out.println("Numero di regole di produzione: " + totalOptimizedProductions);
        System.out.println("Numero di produzioni unitarie: " + (totalOptimizedUnitProductions-1));
        System.out.println("RHS max: " + optimizedRHSMax);
        System.out.println("RHS mean: " + OptimizedRHSMean);
        System.out.println("ALT: " + alt_opt);

        int optimizedComplexity = calculateMcCabeForProductions(optimizedProductions);
        System.out.println("Complessità di McCabe della grammatica ottimizzata: " + optimizedComplexity);
    }
}