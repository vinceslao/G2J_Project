package it.unisannio.g2j;

import it.unisannio.g2j.errors.CustomErrorStrategy;
import it.unisannio.g2j.errors.CollectingErrorListener;
import it.unisannio.g2j.visitors.AntlrVisitor;
import it.unisannio.g2j.visitors.JavaCCVisitor;
import it.unisannio.g2j.visitors.SemanticVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        // ============= ANALISI LESSICALE, SINTATTICA E SEMANTICA DEL FILE DI INPUT =================

        String fileName = "src/main/resources/Tiny_Example_Input.txt";
    //    String fileName = "src/main/resources/C_Example_Input.txt";
    //    String fileName = "src/main/resources/Python_Example_Input.txt";
    //    String fileName = "src/main/resources/Java_Example_Input.txt";
    //    String fileName = "src/main/resources/SQL_Example_Input.txt";
        InputStream input = new FileInputStream(fileName);

        // Crea il listener
        CollectingErrorListener errorListener = new CollectingErrorListener();

        // Lexer
        G2JLexer lexer = new G2JLexer(CharStreams.fromStream(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        // Parser
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        G2JParser parser = new G2JParser(tokens);

        ANTLRErrorStrategy errorStrategy = new CustomErrorStrategy();
        parser.setErrorHandler(errorStrategy);

        parser.setBuildParseTree(true);

        System.out.println("Inizio parsing con recovery attivato...");

        // Esegui il parsing
        ParseTree tree = parser.grammarFile();

        if (errorListener.hasErrors() || CustomErrorStrategy.sintaxErrorNum > 0 ) {
            System.err.println("Sono stati rilevati errori durante la fase di analisi:");
            for (String err : errorListener.getErrors()) {
                System.err.println(err);
            }
            return;
        } else {
            System.out.println("Parsing completato senza errori sintattici.");
        }

        // Analisi semantica e ottimizzazione dell'input
        SemanticVisitor semanticVisitor = new SemanticVisitor();
        semanticVisitor.visit(tree);
        semanticVisitor.checkSemantics();

        semanticVisitor.optimizeInput();


        // ============= GENERAZIONE DEI FILE DI OUTPUT DALL'INPUT OTTIMIZZATO =================

        String optimized_fileName = "output/optimized_input.txt";
        InputStream optimized_input = new FileInputStream(optimized_fileName);
        G2JLexer lexer2 = new G2JLexer(CharStreams.fromStream(optimized_input));
        CommonTokenStream tokens2 = new CommonTokenStream(lexer2);
        G2JParser parser2 = new G2JParser(tokens2);

        ParseTree tree2 = parser2.grammarFile();

        // Generazione dei file di specifica per JavaCC e ANTLR
        JavaCCVisitor javaCCVisitor = new JavaCCVisitor();
        javaCCVisitor.visit(tree2);
        javaCCVisitor.writeOutputToFile("output/GrammarOut.jj");

        AntlrVisitor antlrVisitor = new AntlrVisitor();
        antlrVisitor.visit(tree2);
        antlrVisitor.writeOutputToFile("output/GrammarOut.g4");


        semanticVisitor.calcMetrics();

    }
}