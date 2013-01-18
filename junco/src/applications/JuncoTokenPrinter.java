package applications;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import tokens.Token;
import tokens.Tokens;

import lexicalAnalyzer.JuncoScanner;
import lexicalAnalyzer.Scanner;

public class JuncoTokenPrinter extends JuncoApplication {
	/** Prints tokens from a Junco file.
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		checkArguments(args, "JuncoTokenPrinter");
		
		Tokens.setPrintLevel(Tokens.Level.TYPE_AND_VALUE);
		scanFile(args[0], System.out);
	}
	
	/** prints the Junco tokens in the file specified by filename
	 * to the given PrintStream.
	 * @param filename the name of the file to be listed.
	 * @param out the PrintStream to list to.
	 * @throws FileNotFoundException 
	 */
	public static void scanFile(String filename, PrintStream out) throws FileNotFoundException {
		Scanner scanner     = JuncoScanner.make(filename);
		
		while(scanner.hasNext()) {
			printNextToken(out, scanner);
		}
		printNextToken(out, scanner);		// prints NullToken
	}

	private static void printNextToken(PrintStream out, Scanner scanner) {
		Token token = scanner.next();
		out.println(token.toString());
	}
}
