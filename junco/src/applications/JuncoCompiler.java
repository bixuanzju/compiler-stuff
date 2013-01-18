package applications;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import asmCodeGenerator.ASMCodeFragment;
import asmCodeGenerator.ASMCodeGenerator;

import lexicalAnalyzer.JuncoScanner;
import lexicalAnalyzer.Scanner;
import parseTree.ParseNode;
import parser.JuncoParser;
import semanticAnalyzer.JuncoSemanticAnalyzer;
import tokens.Tokens;

public class JuncoCompiler extends JuncoApplication {
	/** Compiles a Junco file.
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		checkArguments(args, "JuncoCompiler");
		
		Tokens.setPrintLevel(Tokens.Level.FULL);
		compile(args[0]);
	}
	
	/** analyzes a file specified by filename.
	 * @param filename the name of the file to be analyzed.
	 * @throws FileNotFoundException 
	 */
	public static void compile(String filename) throws FileNotFoundException {
		Scanner scanner         = JuncoScanner.make(filename);
		ParseNode syntaxTree    = JuncoParser.parse(scanner);
		ParseNode decoratedTree = JuncoSemanticAnalyzer.analyze(syntaxTree);

		generateCodeIfNoErrors(filename, decoratedTree);
	}

	private static void generateCodeIfNoErrors(String filename, ParseNode decoratedTree)
			throws FileNotFoundException {
		String outfile = outputFilename(filename);
		
		if(thereAreErrors()) {
			stopProcessing(outfile);
		} 
		else {
			generateAndPrintCode(outfile, decoratedTree);
		}
	}

	// stopProcessing -- inform user and clean up.
	private static void stopProcessing(String outfile) {
		informUserNoCodeGenerated();
		removeOldASMFile(outfile);
	}
	private static void informUserNoCodeGenerated() {
		System.err.println("program has errors.  no executable created.");
	}
	private static void removeOldASMFile(String filename) {
		File file = new File(filename);
		if(file.exists()) {
			file.delete();
		}
	}
	
	// normal code generation.
	private static void generateAndPrintCode(String outfile, ParseNode decoratedTree) 
			throws FileNotFoundException {
		ASMCodeFragment code = ASMCodeGenerator.generate(decoratedTree);
		printCodeToFile(outfile, code);
	}
	private static void printCodeToFile(String filename, ASMCodeFragment code)
			throws FileNotFoundException {
		File file = new File(filename);
		PrintStream out = new PrintStream(file);
		out.print(code);
		out.close();
	}

	private static boolean thereAreErrors() {
		return logging.JuncoLogger.hasErrors();
	}
}
