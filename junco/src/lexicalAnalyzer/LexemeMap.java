package lexicalAnalyzer;

import java.util.HashMap;
import java.util.Map;


public class LexemeMap<T extends Lextant> {
	private Map<String, T> lexemeToT = new HashMap<String, T>();
	private T nullValue;

	public LexemeMap(T[] values, T nullValue) {
		this.nullValue = nullValue;
		buildMap(values);
	}
	
	public T forLexeme(String lexeme) {
		if(lexemeToT.containsKey(lexeme)) {
			return lexemeToT.get(lexeme);
		}
		return nullValue;
	}
	
	private void buildMap(T[] values) {
		for(T p: values) {
			lexemeToT.put(p.getLexeme(), p);
		}
	}
}
