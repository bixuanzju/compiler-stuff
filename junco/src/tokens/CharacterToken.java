package tokens;

import inputHandler.TextLocation;

public class CharacterToken extends TokenImp {
	
	protected String value;
	
	protected CharacterToken(TextLocation location, String lexeme) {
		super(location, lexeme.intern());
	}
	
	protected void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	
	public static CharacterToken make(TextLocation location, String lexeme) {
		CharacterToken result = new CharacterToken(location, lexeme);
		return result;
	}


	@Override
	protected String rawString() {
		return "character, " + getLexeme();
	}
}
