package it.unisannio.g2j;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        try {
            String fileName = "input.txt";
            InputStream input = new FileInputStream(fileName);

            G2JLexer lexer = new G2JLexer(CharStreams.fromStream(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            G2JParser parser = new G2JParser(tokens);
            ParseTree tree = parser.grammarFile();

            System.out.println("Albero Sintattico:");
            System.out.println(tree.toStringTree(parser));


            // Dalla grammatica a JavaCC

            /*
            TokenVisitor visitor = new TokenVisitor();
            visitor.visit(tree);

            String outputFileName = "output.jj";
            FileWriter writer = new FileWriter(outputFileName);
            writer.write(visitor.getOutput());
            writer.close();
            System.out.println("File .jj generato con successo: " + outputFileName); */

        }catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }
}