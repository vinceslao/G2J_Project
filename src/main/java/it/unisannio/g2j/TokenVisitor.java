package it.unisannio.g2j;

import it.unisannio.g2j.G2JParser.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class TokenVisitor extends G2JBaseVisitor<Void> {

    @Override
    public Void visitGrammarFile(G2JParser.GrammarFileContext ctx) {
        System.out.println("Visiting Grammar File");
        return visitChildren(ctx);
    }

    @Override
    public Void visitLexRule(G2JParser.LexRuleContext ctx) {
        System.out.println("Visiting Lexical Rule: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitParseRule(G2JParser.ParseRuleContext ctx) {
        System.out.println("Visiting Parsing Rule: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitElement(G2JParser.ElementContext ctx) {
        System.out.println("Visiting Element: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public Void visitProduction(G2JParser.ProductionContext ctx) {
        System.out.println("Visiting Production: " + ctx.getText());
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

    public static void main(String[] args) throws Exception {
    //    String input = "<Example> ::= \"a\" | \"b\" "; // Test input

        String fileName = "input.txt";
        InputStream input = new FileInputStream(fileName);
    //    CharStream charStream = CharStreams.fromString(input);
    //    G2JLexer lexer = new G2JLexer(charStream);
        G2JLexer lexer = new G2JLexer(CharStreams.fromStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        G2JParser parser = new G2JParser(tokens);

        ParseTree tree = parser.grammarFile();
        TokenVisitor visitor = new TokenVisitor();
        visitor.visit(tree);
    }
}

