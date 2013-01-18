package applications;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import parseTree.ParseNode;
import parseTree.ParseTreePrinter;
import parser.JuncoParser;

import lexicalAnalyzer.JuncoScanner;
import lexicalAnalyzer.Scanner;
import tokens.Tokens;

public class JuncoAbstractSyntaxTree extends JuncoApplication {
	/** Prints abstract syntax tree of a Junco file.
	 *  Prints errors if syntax incorrect.
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		checkArguments(args, "JuncoAbstractSyntaxTree");
		
		ParseTreePrinter.setPrintLevel(ParseTreePrinter.Level.FULL);
		Tokens.setPrintLevel(Tokens.Level.TYPE_VALUE_SEQ);
		parseFileToAST(args[0], System.out);
	}
	
	/** analyzes a file specified by filename.
	 * @param filename the name of the file to be analyzed.
	 * @throws FileNotFoundException 
	 */
	public static void parseFileToAST(String filename, PrintStream out) throws FileNotFoundException {
		Scanner scanner     = JuncoScanner.make(filename);
		ParseNode syntaxTree    = JuncoParser.parse(scanner);
		
		out.print(syntaxTree);
	}
}
