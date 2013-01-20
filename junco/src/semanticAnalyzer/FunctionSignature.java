package semanticAnalyzer;

import parseTree.ParseNode;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

class FunctionSignature {
	private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private Type resultType;
	private Type[] paramTypes;
	Object whichVariant;

	public FunctionSignature(Object whichVariant, Type ...types) {
		assert(types.length >= 1);
		storeParamTypes(types);
		resultType = types[types.length-1];
		this.whichVariant = whichVariant;
	}
	private void storeParamTypes(Type[] types) {
		paramTypes = new Type[types.length-1];
		for(int i=0; i<types.length-1; i++) {
			paramTypes[i] = types[i];
		}
	}

	public Type resultType() {
		return resultType;
	}
	public boolean accepts(Type ...types) {
		if(types.length != paramTypes.length) {
			return false;
		}
		
		for(int i=0; i<paramTypes.length; i++) {
			if(!assignableTo(paramTypes[i], types[i])) {
				return false;
			}
		}
		return true;
	}

	private boolean assignableTo(Type variableType, Type valueType) {
		if(valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
			return true;
		}	
		return variableType.equals(valueType);
	}
	
	// signature definitions for integer add, multiply, and greater-than.
	private static FunctionSignature addSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature minusSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature multiplySignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature divideSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature greaterSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature greatereqSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lessSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lesseqSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature2 = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature2 = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, PrimitiveType.ERROR) {
		public boolean accepts(Type ...types) {
			return false;
		}
	};
	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant, ParseNode node) {
		assert(lextant instanceof Punctuator);	
		Punctuator punctuator = (Punctuator)lextant;
		
		switch(punctuator) {
		case ADD:		return addSignature;
		case MINUS:	return minusSignature;
		case MULTIPLY:	return multiplySignature;
		case DIVIDE:		return divideSignature;
		case GREATER:	return greaterSignature;
		case GREATEREQ:	return greatereqSignature;
		case LESS:	return lessSignature;
		case LESSEQ:	return lesseqSignature;
		case EQUAL:	
			if (node.getType() == PrimitiveType.INTEGER)
			return equalSignature1;
			else if (node.getType() == PrimitiveType.BOOLEAN)
				return equalSignature2;
		case UNEQUAL:	
			if (node.getType() == PrimitiveType.INTEGER)
				return unequalSignature1;
			else if (node.getType() == PrimitiveType.BOOLEAN)
				return unequalSignature2;

		default:
			return neverMatchedSignature;
		}
	}

}