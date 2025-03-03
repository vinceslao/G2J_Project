package it.unisannio.g2j;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String fileName = "input.txt";
        InputStream input = new FileInputStream(fileName);
        G2JLexer lexer = new G2JLexer(CharStreams.fromStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        G2JParser parser = new G2JParser(tokens);

        ParseTree tree = parser.grammarFile();

        // Genera il file .jj (JavaCC)
        JavaCCVisitor javaCCVisitor = new JavaCCVisitor();
        javaCCVisitor.visit(tree);
        javaCCVisitor.checkSemantics();
        javaCCVisitor.writeOutputToFile("output.jj");

        // Genera il file .g4 (ANTLR)
        AntlrVisitor antlrVisitor = new AntlrVisitor();
        antlrVisitor.visit(tree);
        antlrVisitor.writeOutputToFile("output.g4");
    }
}