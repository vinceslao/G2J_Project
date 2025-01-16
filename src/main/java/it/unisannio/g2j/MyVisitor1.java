package it.unisannio.g2j;

import it.unisannio.g2j.G2JParser.*;

import java.util.HashSet;
import java.util.Set;

public class MyVisitor1 extends G2JBaseVisitor<String> {
    private StringBuilder output = new StringBuilder();
    private Set<String> terminals = new HashSet<>();
    private Set<String> nonTerminals = new HashSet<>();
    private Set<String> specialSymbols = new HashSet<>();


    @Override
    public String visitGrammarFile(GrammarFileContext ctx) {
        // Aggiunta delle opzioni
        output.append("options {\n");
        output.append("    Static = false;\n");
        output.append("}\n\n");

        // Definizione della sezione PARSER_BEGIN
        output.append("PARSER_BEGIN(gParser)\n\n");
        output.append("public class gParser {\n");
        output.append("    public static void main(String[] args) throws ParseException, FileNotFoundException {\n");
        output.append("        InputStream s;\n");
        output.append("        if(args.length > 0){\n");
        output.append("            String fileName = args[0];\n");
        output.append("            s = new FileInputStream(args[0]);\n");
        output.append("        } else {\n");
        output.append("            System.out.println(args.length);\n");
        output.append("            s = System.in;\n");
        output.append("        }\n\n");
        output.append("        gParserTokenManager tokenmanager = new gParserTokenManager(new SimpleCharStream(s));\n");
        output.append("        Token token;\n\n");
        output.append("        while ((token = tokenmanager.getNextToken()).kind != EOF) {\n");
        output.append("            System.out.println(\"lessema: \" + token.image + \" --> Token associato: \" + tokenImage[token.kind]);\n");
        output.append("        }\n\n");
        output.append("}\n\n");
        output.append("PARSER_END(gParser)\n\n");

        // Aggiunta delle regole principali
        for (RuleContext rule : ctx.rule_()) {
            visit(rule);
        }

        generateTokens();

        // Generazione delle regole di produzione
        generateProductions(ctx);

        return output.toString();
    }


    @Override
    public String visitRule(RuleContext ctx) {
        // Aggiungi il non terminale alla lista
        nonTerminals.add(ctx.nonTerminal().getText());

        // Visita la lista di produzioni
        visit(ctx.productionList());
        return null;
    }

    @Override
    public String visitProductionList(ProductionListContext ctx) {
        // Visita ogni produzione
        for (ProductionContext production : ctx.production()) {
            visit(production);
        }
        return null;
    }

    @Override
    public String visitProduction(ProductionContext ctx) {
        // Visita ogni elemento della produzione
        for (ElementContext element : ctx.element()) {
            visit(element);
        }
        return null;
    }

    @Override
    public String visitElement(ElementContext ctx) {
        if (ctx.nonTerminal() != null) {
            visit(ctx.nonTerminal());
        } else if (ctx.terminal() != null) {
            visit(ctx.terminal());
        } else if (ctx.specialSymbol() != null) {
            visit(ctx.specialSymbol());
        }
        return null;
    }

    @Override
    public String visitNonTerminal(NonTerminalContext ctx) {
        // I non-terminali sono già tra "<>", quindi li aggiungiamo alla lista
        nonTerminals.add(ctx.getText());
        return null;
    }

    @Override
    public String visitTerminal(TerminalContext ctx) {
        // I terminali sono identificatori e vengono aggiunti alla lista dei terminali
        terminals.add(ctx.getText());
        return null;
    }

    @Override
    public String visitSpecialSymbol(SpecialSymbolContext ctx) {
        // I simboli speciali vengono aggiunti alla lista dei simboli speciali
        specialSymbols.add(ctx.getText());
        return null;
    }

    private void generateTokens() {
        output.append("TOKEN : {\n");

        // Terminali
        for (String terminal : terminals) {
            output.append("    <" + terminal.toUpperCase() + ": \"" + terminal + "\">\n");
        }

        // Simboli speciali
        for (String symbol : specialSymbols) {
            String escapedSymbol = symbol.replace("\\", "\\\\").replace("\"", "\\\"");
            output.append("    <" + symbol.toUpperCase() + ": \"" + escapedSymbol + "\">\n");
        }

        output.append("}\n\n");

        // Aggiunta di token compositi (per lettere, numeri, ecc.)
        output.append("TOKEN: {\n");
        output.append("    <#LETTER: ([\"A\"-\"Z\",\"a\"-\"z\"]+)>\n");
        output.append("    | <#NUMBA: ([\"0\"-\"9\"]+)>\n");
        output.append("}\n\n");

        // Regole SKIP
        output.append("SKIP : {\n");
        output.append("    < SKIPPED : [ \" \", \"\\t\", \"\\n\", \"\\r\"] >\n");
        output.append("    | <COMMENT: \"{\" (~[\"}\"])* \"}\">\n");
        output.append("}\n");
    }

    private void generateProductions(GrammarFileContext ctx) {
        output.append("\n\n// Regole di produzione");

        for (RuleContext rule : ctx.rule_()) {
            String ruleName = rule.nonTerminal().getText().replace("<", "").replace(">", "");
            output.append("\nvoid ").append(ruleName).append("() :\n");
            output.append("{}\n{");

            String productions = visit(rule.productionList());
            output.append(productions);

            output.append("}\n\n");
        }
    }

    public String getOutput() {
        return output.toString();
    }
}