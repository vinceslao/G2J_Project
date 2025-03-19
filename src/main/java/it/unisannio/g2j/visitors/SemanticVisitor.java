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

    // Variabili per le ottimizzazioni
    private Map<String, List<List<String>>> optimizedProductions = new HashMap<>();
    private Set<String> newNonTerminals = new HashSet<>();
    private boolean grammarModified = false;

    // Lista di token e regole per ricostruire l'output
    private List<String> lexRulesList = new ArrayList<>();
    private Map<String, String> lexRulesMap = new HashMap<>();

    // Lista ordinata utilizzata per mantenere l'ordine della definizione dei non terminali per l'input ottimizzato.
    private List<String> orderedNonTerminals = new ArrayList<>();
    private Set<String> optimizedNonTerminals = new HashSet<>();

    private int numRecorsionSymbols = 0;


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
        System.out.println("\nVisiting Rule");
        return visitChildren(ctx);
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        String nonTerminal = ctx.NON_TERM().getText();
        definedNonTerminals.add(nonTerminal);
        orderedNonTerminals.add(nonTerminal); // Aggiungi il non terminale alla lista ordinata
        optimizedNonTerminals.add(nonTerminal); // Aggiungiamo il non terminale anche alla lista degli ottimizzati.
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
            visitGrouping(ctx.grouping(), elements);
        } else if (ctx.optionality() != null) {
            visitOptionality(ctx.optionality(), elements);
        } else if (ctx.repetivity() != null) {
            visitRepetivity(ctx.repetivity(), elements);
        }
    }

    private void visitGrouping(G2JParser.GroupingContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_ROUND_BRACKET().getText()); // Apre il raggruppamento
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add(ctx.RIGHT_ROUND_BRACKET().getText()); // Chiude il raggruppamento
    }

    private void visitOptionality(G2JParser.OptionalityContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_SQUARE_BRACKET().getText()); // Apre l'opzionalità
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add(ctx.RIGHT_SQUARE_BRACKET().getText()); // Chiude l'opzionalità
    }

    private void visitRepetivity(G2JParser.RepetivityContext ctx, List<String> elements) {
        elements.add(ctx.LEFT_CURLY_BRACKET().getText()); // Apre la ripetizione
        visitProduction(ctx.production(), elements); // Visita la produzione interna
        elements.add(ctx.RIGHT_CURLY_BRACKET().getText()); // Chiude la ripetizione
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        String terminal = ctx.TERM().getText();
        definedTerminals.add(terminal);

        // Salva la definizione completa della regola lessicale
        String regexDef = ctx.getText().substring(terminal.length() + "::=".length());
        lexRulesMap.put(terminal, regexDef);
        lexRulesList.add(terminal);

        System.out.println("Visiting Lexical Rule: " + terminal);
        return visitChildren(ctx);
    }


    // ==================== Metodi di Analisi Semantica ====================

    public void checkSemantics() {
        try {
            checkNotUsedNonTerminals();
            checkNotUsedTerminals();
            checkUnreachableProductions();
            checkUnreachableTokens();
        } catch(SemanticException e) {
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
                throw new SemanticException("Errore semantico: Terminale non definito - " + terminal);
            }
        }
    }

    /**
     * 3. Verifica produzioni non raggiungibili
     */
    private void checkUnreachableProductions() {
        Set<String> reachable = new HashSet<>();
        reachable.add("<Program>");

        boolean changed;
        do {
            changed = false;
            Set<String> reachableCopy = new HashSet<>(reachable); // Copia temporanea
            for (String nonTerminal : reachableCopy) {
                List<List<String>> nonTermProds = productions.get(nonTerminal);
                if (nonTermProds != null) {
                    for (List<String> production : nonTermProds) {
                        for (String symbol : production) {
                            if (definedNonTerminals.contains(symbol) && !reachable.contains(symbol)) {
                                reachable.add(symbol); // Modifica il Set `reachable`
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);

        for (String nonTerminal : definedNonTerminals) {
            if (!reachable.contains(nonTerminal)) {
                throw new SemanticException("⚠️ Errore semantico: Produzione non raggiungibile - " + nonTerminal);
            }
        }
    }

    /**
     * 4. Verifica produzioni non raggiungibili
     */
    private void checkUnreachableTokens() {
        // Ottieni tutti i terminali definiti
        Set<String> definedTokens = new HashSet<>(definedTerminals);

        // Ottieni tutti i terminali utilizzati nelle produzioni
        Set<String> usedTokens = new HashSet<>(usedTerminals);

        // Verifica se ci sono terminali definiti ma non utilizzati
        for (String token : definedTokens) {
            if (!usedTokens.contains(token)) {
                throw new SemanticException("⚠️ Errore semantico: Token non raggiungibile - " + token);
            }
        }
    }

    // ============================== OTTIMIZZAZIONI =================================

    /**
     * 1. Eliminazione della ricorsione sinistra
     */
    private void eliminateLeftRecursion() {
        for (String nonTerminal : new HashSet<>(definedNonTerminals)) { // Usa una copia per evitare ConcurrentModificationException
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
                numRecorsionSymbols++;
                return true;
            }
        }
        return false;
    }

    private void applyLeftRecursionElimination(String nonTerminal) {
        // Trova tutte le produzioni con ricorsione sinistra
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
            // Stampa la regola originale
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

            // Crea un nuovo non terminale per gestire la ricorsione
            String newNonTerminal = nonTerminal.replace(">", "Tail>");
            newNonTerminals.add(newNonTerminal);

            // Aggiorna la produzione originale
            List<List<String>> newProductions = new ArrayList<>();

            if (!nonLeftRecursiveProductions.isEmpty()) {
                for (List<String> alpha : nonLeftRecursiveProductions) {
                    List<String> newProduction = new ArrayList<>(alpha);
                    // Aggiungi il non terminale opzionale
                    newProduction.add("[" +newNonTerminal+ "]");
                    newProductions.add(newProduction);
                }
            } else {
                // Se non ci sono produzioni non ricorsive, usa una produzione vuota
                List<String> emptyProduction = new ArrayList<>();
                emptyProduction.add("["+newNonTerminal+"]");
                newProductions.add(emptyProduction);
            }

            optimizedProductions.put(nonTerminal, newProductions);

            // Crea le produzioni per il nuovo non terminale
            List<List<String>> tailProductions = new ArrayList<>();

            for (List<String> beta : leftRecursiveProductions) {
                List<String> newProduction = new ArrayList<>(beta.subList(1, beta.size()));
                // Aggiungi il non terminale opzionale
                newProduction.add("["+newNonTerminal+"]");
                tailProductions.add(newProduction);
            }

            optimizedProductions.put(newNonTerminal, tailProductions);

            System.out.println("Regola ottimizzata:");
            System.out.println(nonTerminal + " ::= " + formatOptimizedProduction(newProductions) + " ;");
            System.out.println(newNonTerminal + " ::= " + formatOptimizedProduction(tailProductions) + " ;");

            optimizedNonTerminals.add(newNonTerminal);
        }
    }


    /**
     * 2. Fattorizzazione a prefisso comune
     */
    private void factorizeCommonPrefixes() {
        boolean factorizationApplied;

        do {
            factorizationApplied = false;

            for (String nonTerminal : new HashSet<>(definedNonTerminals)) {
                List<List<String>> productionsForNT = optimizedProductions.get(nonTerminal);
                if (productionsForNT != null && productionsForNT.size() > 1) {
                    // Trova il prefisso comune più lungo tra tutte le produzioni
                    List<String> commonPrefix = findLongestCommonPrefix(productionsForNT);

                    if (!commonPrefix.isEmpty()) {
                        System.out.println("\n⚠️ Prefisso comune rilevato per: " + nonTerminal);
                        System.out.println("Regola originale:");
                        System.out.print(nonTerminal + " ::= ");
                        System.out.println(formatOptimizedProduction(productionsForNT) + " ;");

                        // Applica la fattorizzazione
                        applyFactorization(nonTerminal, commonPrefix, productionsForNT);
                        factorizationApplied = true;
                        grammarModified = true;
                        break;  // Ricomincia dall'inizio dopo aver applicato una fattorizzazione
                    }
                }
            }
        } while (factorizationApplied);  // Continua finché vengono applicate fattorizzazioni
    }

    /**
     * Applica la fattorizzazione del prefisso comune
     */
    private void applyFactorization(String nonTerminal, List<String> commonPrefix, List<List<String>> productionsForNT) {
        // Crea un nuovo non terminale per gestire i suffissi
        String newNonTerminal = nonTerminal.replace(">", "Suffix>");

        // Assicurati che il nome sia unico
        int counter = 1;
        String originalName = newNonTerminal;
        while (definedNonTerminals.contains(newNonTerminal)) {
            newNonTerminal = originalName.replace(">", counter + ">");
            counter++;
        }

        newNonTerminals.add(newNonTerminal);

        // Crea le nuove produzioni
        List<List<String>> newProductions = new ArrayList<>();
        List<String> newProduction = new ArrayList<>(commonPrefix);
        newProduction.add("["+newNonTerminal+"]");
        newProductions.add(newProduction);

        // Crea le produzioni per il nuovo non terminale (suffissi)
        List<List<String>> suffixProductions = new ArrayList<>();

        for (List<String> production : productionsForNT) {
            if (production.size() > commonPrefix.size()) {
                List<String> suffix = new ArrayList<>(production.subList(commonPrefix.size(), production.size()));
                suffixProductions.add(suffix);
            }
        }

        // Aggiorna le produzioni ottimizzate
        optimizedProductions.put(nonTerminal, newProductions);
        optimizedProductions.put(newNonTerminal, suffixProductions);

        System.out.println("Regola fattorizzata:");
        System.out.println(nonTerminal + " ::= " + formatOptimizedProduction(newProductions) + " ;");
        System.out.println(newNonTerminal + " ::= " + formatOptimizedProduction(suffixProductions) + " ;");

        optimizedNonTerminals.add(newNonTerminal);
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



    // ============================== COSTRUZIONE FILE DI INPUT OTTIMIZZATO ===================================


    /*
     * In questo metodo si gestisce la generazione del file di input ottimizzato.
     */
    public void optimizeInput(){
        // Inizializza la mappa di produzioni ottimizzate con le produzioni originali
        optimizedProductions = new HashMap<>(productions);

        // Applica le ottimizzazioni. In questi metodi, si fa in modo che le ottimizzazioni vengano aggiunte alla mappa.
        eliminateLeftRecursion();
        factorizeCommonPrefixes();

        // Se sono state apportate modifiche, genera il file ottimizzato
        if (grammarModified) {
            generateOptimizedGrammarFile();
        }
    }


    /**
     * Formatta una lista di produzioni in una stringa per la visualizzazione
     */
    private String formatOptimizedProduction(List<List<String>> productions) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < productions.size(); i++) {
            List<String> production = productions.get(i);

            // Se la produzione è vuota, non aggiungere nulla
            if (production.isEmpty()) {
                continue; // Salta le produzioni vuote
            }

            // Aggiungi gli elementi della produzione
            for (String s : production) {
                sb.append(s);
                sb.append(" ");
            }

            // Aggiungi il simbolo "|" solo se ci sono altre produzioni non vuote
            if (i < productions.size() - 1 && !productions.get(i + 1).isEmpty()) {
                sb.append(" | ");
            }
        }

        return sb.toString();
    }

    private void generateOptimizedGrammarFile() {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter("output/optimized_input.txt"))) {
            // Write lexical rules first
            for (String terminal : lexRulesList) {
                String ruleDef = lexRulesMap.get(terminal);
                writer.println(terminal + "::=" + ruleDef+";");
            }

            writer.println(); // Add a blank line between lexical and parsing rules

            // Write parsing rules in the original order
            for (String nonTerminal : orderedNonTerminals) {
                // Skip new non-terminals created during optimization
                if (newNonTerminals.contains(nonTerminal)) {
                    continue;
                }

                List<List<String>> prods = optimizedProductions.get(nonTerminal);
                if (prods != null && !prods.isEmpty()) {
                    writer.print(nonTerminal + " ::= ");
                    if (nonTerminal.equals("<Program>")) {
                        // Aggiungi manualmente EOF alla regola <Program>
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
        } catch (java.io.IOException e) {
            System.err.println("❌ Errore durante la scrittura del file di grammatica ottimizzato: " + e.getMessage());
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

     * Calcola la complessità di McCabe per un insieme di produzioni.
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
            assert productionList != null;
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

        return complexity;
    }

    public void calcMetrics() {
        // Calcola le metriche per l'input originale

        System.out.println("\n📐CALCOLO DELLE METRICHE SULL'INPUT ORIGINALE");
        System.out.println("Numero dei simboli non terminali: " + definedNonTerminals.size());
        System.out.println("Numero dei simboli terminali: " + definedTerminals.size());

        // Calcola il numero totale di produzioni per l'input originale
        double totalOriginalProductions = 0;
        int totalOriginalUnitProductions = 0; // Contatore per produzioni unitarie
        int originalRHSMax = 0; // Contatore per RHS max
        int sumProductionLength = 0;
        double RHSMean=0;
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

        // Calcola la quantità di simboli terminali e non terminali media delle produzioni sul
        // lato destro,
        RHSMean = (sumProductionLength+1) / totalOriginalProductions;

        double alt = totalOriginalProductions / definedNonTerminals.size(); // media del numero di alternative per regola

        System.out.println("Numero di regole di produzione: " + totalOriginalProductions);
        System.out.println("Numero di produzioni unitarie: " + totalOriginalUnitProductions);
        System.out.println("RHS max: " + originalRHSMax);
        System.out.println("RHS mean: " + RHSMean);
        System.out.println("ALT " + alt);
        System.out.println("Numero di simboli ricorsivi rilevati: " + numRecorsionSymbols);

        int originalComplexity = calculateMcCabeForProductions(productions);
        System.out.println("Complessità di McCabe della grammatica originale: " + originalComplexity);


        // Calcola le metriche per l'input ottimizzato
        System.out.println("\n📐CALCOLO DELLE METRICHE SULL'INPUT OTTIMIZZATO");
        System.out.println("Numero dei simboli non terminali: " + optimizedNonTerminals.size());
        System.out.println("Numero dei simboli terminali: " + definedTerminals.size()); // I terminali non cambiano

        // Calcola il numero totale di produzioni per l'input ottimizzato
        double totalOptimizedProductions = 0;
        int totalOptimizedUnitProductions = 0; // Contatore per produzioni unitarie
        int optimizedRHSMax = 0; // Contatore per RHS max
        int sumProductionLengthOpt = 0;
        double OptimizedRHSMean = 0;

        for (List<List<String>> productionList : optimizedProductions.values()) {
            totalOptimizedProductions += productionList.size();
            for (List<String> production : productionList) {
                int actualElements = 0;
                for (String element : production) {
                    if (!delimiters.contains(element)) {
                        actualElements++;
                    }
                }
                if (production.size() == 1) { // Produzione unitaria
                    totalOptimizedUnitProductions++;
                }
                if (production.size() > optimizedRHSMax) { // Aggiorna RHS max
                    optimizedRHSMax = actualElements;
                }

                sumProductionLengthOpt += actualElements;
            }
        }

        OptimizedRHSMean = (sumProductionLengthOpt+1) / totalOptimizedProductions;

        double alt_opt = totalOptimizedProductions / optimizedNonTerminals.size();


        System.out.println("Numero di regole di produzione: " + totalOptimizedProductions);
        System.out.println("Numero di produzioni unitarie: " + totalOptimizedUnitProductions);
        System.out.println("RHS max: " + optimizedRHSMax);
        System.out.println("RHS mean: " + OptimizedRHSMean);
        System.out.println("ALT: " + alt_opt);

        int optimizedComplexity = calculateMcCabeForProductions(optimizedProductions);
        System.out.println("Complessità di McCabe della grammatica ottimizzata: " + optimizedComplexity);
    }

}