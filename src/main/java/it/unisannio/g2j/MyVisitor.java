package it.unisannio.g2j;

import it.unisannio.g2j.G2JParser.*;

public class MyVisitor extends G2JBaseVisitor<String>{

    private StringBuilder output = new StringBuilder();

    @Override public String visitGrammarFile(G2JParser.GrammarFileContext ctx) {

        // Intestazione del file .jj
        output.append("PARSER_BEGIN(MyParser)\n\n");
        output.append("public class MyParser {\n");
        output.append("}\n\n");
        output.append("PARSER_END(MyParser)\n\n");
        output.append("SKIP : { \" \" | \"\\t\" | \"\\n\" | \"\\r\" }\n\n");

        // Visita tutte le regole
        for (RuleContext rule : ctx.rule_()) {
            visit(rule);
        }
        return output.toString();
    }

    @Override public String visitRule(G2JParser.RuleContext ctx) {

        // Nome del non-terminal
        String nonTerminal = visit(ctx.nonTerminal());

        // Produzioni corrispondenti
        String productions = visit(ctx.productionList());

        // Aggiungi la regola al file .jj
        output.append(nonTerminal).append(" : ").append(productions).append(" ;\n");

        return null;
    }

    @Override public String visitProductionList(G2JParser.ProductionListContext ctx) {
        // Combina tutte le produzioni con "|"
        StringBuilder productions = new StringBuilder();
        for (ProductionContext production : ctx.production()) {
            if (!productions.isEmpty()) {
                productions.append(" | ");
            }
            productions.append(visit(production));
        }
        return productions.toString();
    }

    @Override public String visitProduction(G2JParser.ProductionContext ctx) {
        // Combina tutti gli elementi della produzione
        StringBuilder elements = new StringBuilder();
        for (ElementContext element : ctx.element()) {
            if (!elements.isEmpty()) {
                elements.append(" ");
            }
            elements.append(visit(element));
        }
        return elements.toString();
    }

    @Override public String visitElement(G2JParser.ElementContext ctx) {
        if (ctx.nonTerminal() != null) {
            return visit(ctx.nonTerminal());
        } else if (ctx.terminal() != null) {
            return "\"" + ctx.terminal().getText() + "\"";
        } else if (ctx.specialSymbol() != null) {
            return ctx.specialSymbol().getText();
        }
        return "";
    }

    @Override public String visitNonTerminal(G2JParser.NonTerminalContext ctx) {
        return ctx.getText();
    }

    @Override public String visitTerminal(G2JParser.TerminalContext ctx) {
        return "\"" + ctx.getText() + "\"";
    }

    @Override public String visitSpecialSymbol(G2JParser.SpecialSymbolContext ctx) {
        return ctx.getText();
    }

    public String getOutput() {
        return output.toString();
    }
}
