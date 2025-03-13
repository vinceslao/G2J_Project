package it.unisannio.g2j;

import it.unisannio.g2j.errors.CustomErrorListener;
import it.unisannio.g2j.visitors.AntlrVisitor;
import it.unisannio.g2j.visitors.JavaCCVisitor;
import it.unisannio.g2j.visitors.SemanticVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {

        // ============= ANALISI LESSICALE, SINTATTICA E SEMANTICA DEL FILE DI INPUT =================

        String fileName = "src/main/resources/input.txt";
        InputStream input = new FileInputStream(fileName);
        G2JLexer lexer = new G2JLexer(CharStreams.fromStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        G2JParser parser = new G2JParser(tokens);

        // Rimuovi gli error listener di default e aggiungi il CustomErrorListener
        parser.removeErrorListeners();
        parser.addErrorListener(new CustomErrorListener());

        // Esegui il parsing
        ParseTree tree = parser.grammarFile();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Numero errori sintattici: "+parser.getNumberOfSyntaxErrors());
            System.err.println("Rilevato un errore sintattico, quindi analisi interrotta.");
            return;
        }

        // Analisi semantica e ottimizzazione dell'input
        SemanticVisitor semanticVisitor = new SemanticVisitor();
        semanticVisitor.visit(tree);
        semanticVisitor.checkSemantics();


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
    }
}