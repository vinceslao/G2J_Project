package it.unisannio.g2j.symbols;

import java.util.List;

/**
 * Represents a grammar symbol (terminal or non-terminal)
 */
public class Symbol {
    private final String name;
    private final SymbolType type;
    private String definition;
    private List<List<String>> productions;
    private boolean used;
    private boolean defined;

    /**
     * Creates a new symbol
     * @param name Symbol name
     * @param type Symbol type (TERMINAL or NON_TERMINAL)
     */
    public Symbol(String name, SymbolType type) {
        this.name = name;
        this.type = type;
        this.used = false;
        this.defined = false;
    }

    /**
     * Gets the symbol name
     * @return Symbol name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the symbol type
     * @return Symbol type
     */
    public SymbolType getType() {
        return type;
    }

    /**
     * Gets the symbol definition (for terminals)
     * @return Definition string
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * Sets the symbol definition (for terminals)
     * @param definition Definition string
     */
    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * Gets the symbol's productions (for non-terminals)
     * @return List of productions
     */
    public List<List<String>> getProductions() {
        return productions;
    }

    /**
     * Sets the symbol's productions (for non-terminals)
     * @param productions List of productions
     */
    public void setProductions(List<List<String>> productions) {
        this.productions = productions;
    }

    /**
     * Checks if the symbol is used in the grammar
     * @return True if the symbol is used
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * Sets the symbol's usage status
     * @param used True if the symbol is used
     */
    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isDefined() {
        return defined;
    }

    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    @Override
    public String toString() {
        return "\tname='" + name + "',\n" +
                "\ttype=" + type + ",\n" +
                "\tdefinition='" + definition + "',\n" +
                "\tproductions=" + productions + ",\n" +
                "\tused=" + used + ",\n" +
                "\tdefined=" + defined + "\n";
    }

}
