package it.unisannio.g2j.symbols;

import java.util.*;

/**
 * Symbol table for tracking and managing grammar symbols
 */
public class SymbolTable {
    // Map to store symbol information
    private final Map<String, Symbol> symbols = new HashMap<>();

    // Ordered lists to maintain symbol declaration order
    private final List<String> orderedTerminals = new ArrayList<>();
    private final List<String> orderedNonTerminals = new ArrayList<>();

    /**
     * Adds a terminal symbol to the table
     * @param name Terminal symbol name
     * @param definition Terminal's regex definition
     * @return True if the symbol was added, false if it already existed
     */
    public boolean addTerminal(String name, String definition) {
        if (symbols.containsKey(name)) {
            return false;
        }

        Symbol symbol = new Symbol(name, SymbolType.TERMINAL);
        symbol.setDefinition(definition);
        symbols.put(name, symbol);
        orderedTerminals.add(name);
        return true;
    }

    /**
     * Adds a non-terminal symbol to the table
     * @param name Non-terminal symbol name
     * @return True if the symbol was added, false if it already existed
     */
    public boolean addNonTerminal(String name) {
        if (symbols.containsKey(name)) {
            return false;
        }

        Symbol symbol = new Symbol(name, SymbolType.NON_TERMINAL);
        symbols.put(name, symbol);
        orderedNonTerminals.add(name);
        return true;
    }

    /**
     * Adds a production rule to a non-terminal
     * @param nonTerminal Name of the non-terminal
     * @param production List of symbols in the production
     * @return True if the production was added successfully
     */
    public boolean addProduction(String nonTerminal, List<String> production) {
        Symbol symbol = symbols.get(nonTerminal);
        if (symbol == null || symbol.getType() != SymbolType.NON_TERMINAL) {
            return false;
        }

        if (symbol.getProductions() == null) {
            symbol.setProductions(new ArrayList<>());
        }

        symbol.getProductions().add(production);

        // Mark symbols as used when they appear in productions
        for (String element : production) {
            if (isNonTerminal(element)) {
                markAsUsed(element);
            } else if (isTerminal(element)) {
                markAsUsed(element);
            }
            // Skip delimiters and other structural elements like (), [], {}
        }

        return true;
    }

    /**
     * Marks a symbol as used in the grammar
     * @param name Symbol name
     */
    public void markAsUsed(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            symbol.setUsed(true);
        }
    }

    /**
     * Gets all defined terminals
     * @return Set of terminal symbol names
     */
    public Set<String> getDefinedTerminals() {
        Set<String> result = new HashSet<>();
        for (Symbol symbol : symbols.values()) {
            if (symbol.getType() == SymbolType.TERMINAL) {
                result.add(symbol.getName());
            }
        }
        return result;
    }

    /**
     * Gets all defined non-terminals
     * @return Set of non-terminal symbol names
     */
    public Set<String> getDefinedNonTerminals() {
        Set<String> result = new HashSet<>();
        for (Symbol symbol : symbols.values()) {
            if (symbol.getType() == SymbolType.NON_TERMINAL) {
                result.add(symbol.getName());
            }
        }
        return result;
    }

    /**
     * Gets all used terminals
     * @return Set of used terminal symbol names
     */
    public Set<String> getUsedTerminals() {
        Set<String> result = new HashSet<>();
        for (Symbol symbol : symbols.values()) {
            if (symbol.getType() == SymbolType.TERMINAL && symbol.isUsed()) {
                result.add(symbol.getName());
            }
        }
        return result;
    }

    /**
     * Gets all used non-terminals
     * @return Set of used non-terminal symbol names
     */
    public Set<String> getUsedNonTerminals() {
        Set<String> result = new HashSet<>();
        for (Symbol symbol : symbols.values()) {
            if (symbol.getType() == SymbolType.NON_TERMINAL && symbol.isUsed()) {
                result.add(symbol.getName());
            }
        }
        return result;
    }

    /**
     * Gets the terminal definition
     * @param name Terminal name
     * @return Definition or null if not found
     */
    public String getTerminalDefinition(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null && symbol.getType() == SymbolType.TERMINAL) {
            return symbol.getDefinition();
        }
        return null;
    }

    /**
     * Gets all productions for a non-terminal
     * @param nonTerminal Non-terminal name
     * @return List of productions or empty list if not found
     */
    public List<List<String>> getProductions(String nonTerminal) {
        Symbol symbol = symbols.get(nonTerminal);
        if (symbol != null && symbol.getType() == SymbolType.NON_TERMINAL && symbol.getProductions() != null) {
            return symbol.getProductions();
        }
        return new ArrayList<>();
    }

    /**
     * Gets all productions in a map format
     * @return Map of non-terminals to their productions
     */
    public Map<String, List<List<String>>> getAllProductions() {
        Map<String, List<List<String>>> result = new HashMap<>();
        for (Symbol symbol : symbols.values()) {
            if (symbol.getType() == SymbolType.NON_TERMINAL && symbol.getProductions() != null) {
                result.put(symbol.getName(), symbol.getProductions());
            }
        }
        return result;
    }

    /**
     * Checks if a symbol is a terminal
     * @param name Symbol name
     * @return True if the symbol is a terminal
     */
    public boolean isTerminal(String name) {
        Symbol symbol = symbols.get(name);
        return symbol != null && symbol.getType() == SymbolType.TERMINAL;
    }

    /**
     * Checks if a symbol is a non-terminal
     * @param name Symbol name
     * @return True if the symbol is a non-terminal
     */
    public boolean isNonTerminal(String name) {
        Symbol symbol = symbols.get(name);
        return symbol != null && symbol.getType() == SymbolType.NON_TERMINAL;
    }

    /**
     * Gets ordered list of terminal symbols in declaration order
     * @return List of terminal names
     */
    public List<String> getOrderedTerminals() {
        return new ArrayList<>(orderedTerminals);
    }

    /**
     * Gets ordered list of non-terminal symbols in declaration order
     * @return List of non-terminal names
     */
    public List<String> getOrderedNonTerminals() {
        return new ArrayList<>(orderedNonTerminals);
    }

    /**
     * Checks if a symbol exists in the table
     * @param name Symbol name
     * @return True if the symbol exists
     */
    public boolean containsSymbol(String name) {
        return symbols.containsKey(name);
    }

    /**
     * Stampa la tabella dei simboli in un formato leggibile.
     */
    public void printSymbolTable() {
        System.out.println("\n=== TABELLA DEI SIMBOLI ===");

        // Stampa i terminali
        System.out.println("\n--- TERMINALI ---");
        for (String terminal : orderedTerminals) {
            Symbol symbol = symbols.get(terminal);
            System.out.println("\nTerminale: " + terminal);
            System.out.println("  Definizione: " + symbol.getDefinition());
        }

        // Stampa i non-terminali
        System.out.println("\n--- NON TERMINALI ---");
        for (String nonTerminal : orderedNonTerminals) {
            Symbol symbol = symbols.get(nonTerminal);
            System.out.println("\nNon Terminale: " + nonTerminal);

            // Stampa le produzioni associate al non-terminale
            List<List<String>> productions = symbol.getProductions();
            if (productions != null && !productions.isEmpty()) {
                System.out.println("  Produzioni:");
                for (List<String> production : productions) {
                    System.out.println("    " + String.join(" ", production));
                }
            } else {
                System.out.println("  Nessuna produzione associata.");
            }
        }

        System.out.println("\n=== FINE TABELLA DEI SIMBOLI ===\n");
    }
}