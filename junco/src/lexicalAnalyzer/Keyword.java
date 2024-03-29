package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;


public enum Keyword implements Lextant {
	CONST("const"),
	PRINT("print"),
	PRUNT("prunt"),
	TRUE("true"),
	FALSE("false"),
	BOX("box"),
	CALL("call"),
	NULL_KEYWORD(""),
	INIT("init"),
	IF("if"),
	ELSE("else"),
	WHILE("while"),
	IN("in"),
	RETURN("return"),
	FUNC("func"),
	THIS("this"),
	BREAK("break"),
	CONTINUE("continue"),
	UPDATE("update");
	private String lexeme;
	private Token prototype;
	
	
	private Keyword(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}
	public String getLexeme() {
		return lexeme;
	}
	public Token prototype() {
		return prototype;
	}
	
	public static Keyword forLexeme(String lexeme) {
		for(Keyword keyword: values()) {
			if(keyword.lexeme.equals(lexeme)) {
				return keyword;
			}
		}
		return NULL_KEYWORD;
	}
	public static boolean isAKeyword(String lexeme) {
		return forLexeme(lexeme) != NULL_KEYWORD;
	}
	
	/*   the following can replace the implementation of forLexeme above. It is faster but less clear. 
	private static LexemeMap<Keyword> lexemeToKeyword = new LexemeMap<Keyword>(values(), NULL_KEYWORD);
	public static Keyword forLexeme(String lexeme) {
		return lexemeToKeyword.forLexeme(lexeme);
	}
	*/
}
