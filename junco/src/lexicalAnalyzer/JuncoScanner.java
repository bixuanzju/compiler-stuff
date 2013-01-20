package lexicalAnalyzer;

import logging.JuncoLogger;
import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.NumberToken;
import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class JuncoScanner extends ScannerImp implements Scanner {
	public static JuncoScanner make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new JuncoScanner(charStream);
	}

	public JuncoScanner(PushbackCharStream input) {
		super(input);
	}

	// ////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch

	// TODO add float number analysis
	@Override
	protected void findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();
		
		// deal with comment
		while (ch.getCharacter() == '*' && input.peek().getCharacter() == '*') {
			// eat the second '*'
			ch = input.next();
			do {
				ch = input.next();
			}
			while ((ch.getCharacter() != '*' || input.peek().getCharacter() != '*') && ch.getCharacter() != '\n');
			if (ch.getCharacter() == '*') {
				// eat the second '*'
				ch = input.next();
			}
			ch = nextNonWhitespaceChar();
		}
		
		if ((ch.getCharacter() == '-' && input.peek().isDigit()) || ch.isDigit()) {
			scanNumber(ch);
		}
		else if (isPunctuatorStart(ch)) {
			nextToken = PunctuatorScanner.scan(ch, input);
		}
		else if (ch.isLetter() || ch.getCharacter() == '_' || ch.getCharacter() == '#') {
			scanIdentifier(ch);
		}

		else if (isEndOfInput(ch)) {
			nextToken = NullToken.make(ch.getLocation());
		}
		else {
			lexicalError(ch);
			findNextToken();
		}
		
	}

	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while (ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}

	// ////////////////////////////////////////////////////////////////////////////
	// Integer lexical analysis

	// TODO modify int number to accommodate negative and float type
	private void scanNumber(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentDigits(buffer);

		nextToken = NumberToken.make(firstChar.getLocation(), buffer.toString());
	}

	private void appendSubsequentDigits(StringBuffer buffer) {
		LocatedChar c = input.next();
		while (c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	

	// ////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis
	
	// TODO modify identifier analysis
	private void scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentChar(buffer);

		String lexeme = buffer.toString();
		if (Keyword.isAKeyword(lexeme)) {
			nextToken = LextantToken.make(firstChar.getLocation(), lexeme,
					Keyword.forLexeme(lexeme));
		}
		else {
			nextToken = IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}

	private void appendSubsequentChar(StringBuffer buffer) {
		LocatedChar c = input.next();
		while (c.isLetter() || c.getCharacter() == '_' || c.getCharacter() == '#' || c.getCharacter() == '-' || c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}

	// ////////////////////////////////////////////////////////////////////////////
	// Punctuator lexical analysis
	// old method left in to show a simple scanning method.
	// current method is the algorithm object PunctuatorScanner.java
	@SuppressWarnings("unused")
	private void oldScanPunctuator(LocatedChar ch) {
		// all operators in Junco-0 are single-character.
		TextLocation location = ch.getLocation();

		switch (ch.getCharacter()) {
		case '*':
			nextToken = LextantToken.make(location, "*", Punctuator.MULTIPLY);
			break;
		case '+':
			nextToken = LextantToken.make(location, "+", Punctuator.ADD);
			break;
		case '>':
			nextToken = LextantToken.make(location, ">", Punctuator.GREATER);
			break;
		case '=':
			nextToken = LextantToken.make(location, "=", Punctuator.ASSIGN);
			break;
		case ',':
			nextToken = LextantToken.make(location, ",", Punctuator.SPLICE);
			break;
		case ';':
			nextToken = LextantToken.make(location, ";", Punctuator.TERMINATOR);
			break;
		default:
			throw new IllegalArgumentException("bad LocatedChar " + ch
					+ "in scanOperator");
		}
	}

	// ////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Junco scanning.

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}

	// ////////////////////////////////////////////////////////////////////////////
	// Error-reporting

	private void lexicalError(LocatedChar ch) {
		JuncoLogger log = JuncoLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character " + ch);
	}

}
