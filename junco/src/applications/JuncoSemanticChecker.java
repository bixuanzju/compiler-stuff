package applications;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import lexicalAnalyzer.JuncoScanner;
import lexicalAnalyzer.Scanner;
import parseTree.ParseNode;
import parseTree.ParseTreePrinter;
import parser.JuncoParser;
import semanticAnalyzer.JuncoSemanticAnalyzer;
import tokens.Tokens;

public class JuncoSemanticChecker extends JuncoApplication {
	/** Checks semantics of a Junco file.
	 *  Prints filename and "done" if syntax is correct; prints errors if not.
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		checkArguments(args, "JuncoSemanticChecker");

		ParseTreePrinter.setPrintLevel(ParseTreePrinter.Level.FULL);
		Tokens.setPrintLevel(Tokens.Level.FULL);
		checkFileSemantics(args[0], System.out);
	}
	
	/** analyzes a file specified by filename and prints
	 *  a decorated syntax tree for the file.
	 *  
	 * @param filename the name of the file to be analyzed.
	 * @param out the PrintStream to print the decorated tree to.
	 * @throws FileNotFoundException 
	 */
	public static void checkFileSemantics(String filename, PrintStream out) throws FileNotFoundException {
		Scanner scanner         = JuncoScanner.make(filename);
		ParseNode syntaxTree    = JuncoParser.parse(scanner);
		ParseNode decoratedTree = JuncoSemanticAnalyzer.analyze(syntaxTree);
		
		out.print(decoratedTree);
	}
}
