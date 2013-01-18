package symbolTable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import logging.JuncoLogger;

import tokens.Token;

public class SymbolTable {
	private Map<String, Binding> table;
	
	public SymbolTable() {
		table = new HashMap<String, Binding>();
	}
	
	
	////////////////////////////////////////////////////////////////
	// installation and lookup of identifiers

	public Binding install(String identifier, Binding binding) {
		table.put(identifier, binding);
		return binding;
	}
	public Binding lookup(String identifier) {
		Binding binding = table.get(identifier);
		if(binding == null) {
			return Binding.nullInstance();
		}
		return binding;
	}
	
	///////////////////////////////////////////////////////////////////////
	// Map delegates	
	
	public boolean containsKey(String identifier) {
		return table.containsKey(identifier);
	}
	public Set<String> keySet() {
		return table.keySet();
	}
	public Collection<Binding> values() {
		return table.values();
	}
	
	///////////////////////////////////////////////////////////////////////
	//error reporting

	public void errorIfAlreadyDefined(Token token) {
		if(containsKey(token.getLexeme())) {		
			multipleDefinitionError(token);
		}
	}
	protected static void multipleDefinitionError(Token token) {
		JuncoLogger log = JuncoLogger.getLogger("complier.symbolTable");
		log.severe("variable \"" + token.getLexeme() + 
				          "\" multiply defined at " + token.getLocation());
	}

	///////////////////////////////////////////////////////////////////////
	// toString

	public String toString() {
		String result = "    symbol table: \n";
		for(Entry<String, Binding> entry: table.entrySet()) {
			result += "        " + entry + "\n";
		}
		return result;
	}
}
