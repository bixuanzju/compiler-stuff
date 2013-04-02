package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Punctuator implements Lextant {
	ADD("+"),
	MINUS("-"),
	MULTIPLY("*"),
	DIVIDE("/"),
	ASSIGN("<-"),
	GREATER(">"),
	GREATEREQ(">="),
	LESS("<"),
	LESSEQ("<="),
	EQUAL("=="),
	UNEQUAL("<>"),
	AND("&&"),
	OR("||"),
	NOT("!"),
	SPLICE(","),
	TERMINATOR(";"), 
	PRINT_NEWLINE("$"),
	OPEN_BRACE("{"),
	CLOSE_BRACE("}"),
	BODY_OPEN("{["),
	BODY_CLOSE("]}"),
	OPEN_BRACKET("("),
	CLOSE_BRACKET(")"),
	OPEN_SQUARE("["),
	CLOSE_SQUARE("]"),
	COLON(":"),
	CASTTOINT("::i"),
	CASTTOFLAOT("::f"),
	CASTTOCHAR("::c"),
	CASTTOBOOL("::b"),
	DOT("."),
	LOW(".low"),
	HIGH(".high"),
	EMPTY(".isEmpty"),
	SPAN("|"),
	AT("@"),
	INTERSECTION("&"),
	NULL_PUNCTUATOR("");
	

	private String lexeme;
	private Token prototype;
	
	private Punctuator(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	
	public static Punctuator forLexeme(String lexeme) {
		for(Punctuator punctuator: values()) {
			if(punctuator.lexeme.equals(lexeme)) {
				return punctuator;
			}
		}
		return NULL_PUNCTUATOR;
	}
	
/*
	//   the following can replace the implementation of forLexeme above. It is faster but less clear. 
	private static LexemeMap<Punctuator> lexemeToPunctuator = new LexemeMap<Punctuator>(values(), NULL_PUNCTUATOR);
	public static Punctuator forLexeme(String lexeme) {
		   return lexemeToPunctuator.forLexeme(lexeme);
	}
*/
	
}


