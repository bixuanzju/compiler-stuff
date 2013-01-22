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
	private static FunctionSignature addSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature addSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM);
	private static FunctionSignature minusSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature minusSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM);
	private static FunctionSignature multiplySignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature multiplySignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM);
	private static FunctionSignature divideSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER);
	private static FunctionSignature divideSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM);
	private static FunctionSignature greaterSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature greaterSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature greaterSignature3 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature greatereqSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature greatereqSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature greatereqSignature3 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lessSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lessSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature lessSignature3 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lesseqSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature lesseqSignature2 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature lesseqSignature3 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature2 = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature3 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature equalSignature4 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature1 = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature2 = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature3 = new FunctionSignature(1, PrimitiveType.FLOATNUM, PrimitiveType.FLOATNUM, PrimitiveType.BOOLEAN);
	private static FunctionSignature unequalSignature4 = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN);
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, PrimitiveType.ERROR) {
		public boolean accepts(Type ...types) {
			return false;
		}
	};
	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant, ParseNode node) {
		assert(lextant instanceof Punctuator);	
		Punctuator punctuator = (Punctuator)lextant;
		
		switch (punctuator) {
		case ADD:
			if (node.getType() == PrimitiveType.INTEGER)
				return addSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return addSignature2;
			else return neverMatchedSignature;
		case MINUS:
			if (node.getType() == PrimitiveType.INTEGER)
				return minusSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return minusSignature2;
			else return neverMatchedSignature;
		case MULTIPLY:
			if (node.getType() == PrimitiveType.INTEGER)
				return multiplySignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return multiplySignature2;
			else return neverMatchedSignature;
		case DIVIDE:
			if (node.getType() == PrimitiveType.INTEGER)
				return divideSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return divideSignature2;
			else return neverMatchedSignature;
		case GREATER:
			if (node.getType() == PrimitiveType.INTEGER)
				return greaterSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return greaterSignature2;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return greaterSignature3;
			else return neverMatchedSignature;
		case GREATEREQ:
			if (node.getType() == PrimitiveType.INTEGER)
				return greatereqSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return greatereqSignature2;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return greatereqSignature3;
			else return neverMatchedSignature;
		case LESS:
			if (node.getType() == PrimitiveType.INTEGER)
				return lessSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return lessSignature2;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return lessSignature3;
			else return neverMatchedSignature;
		case LESSEQ:
			if (node.getType() == PrimitiveType.INTEGER)
				return lesseqSignature1;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return lesseqSignature2;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return lesseqSignature3;
			else return neverMatchedSignature;
		case EQUAL:
			if (node.getType() == PrimitiveType.INTEGER)
				return equalSignature1;
			else if (node.getType() == PrimitiveType.BOOLEAN)
				return equalSignature2;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return equalSignature3;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return equalSignature4;
			else return neverMatchedSignature;
		case UNEQUAL:
			if (node.getType() == PrimitiveType.INTEGER)
				return unequalSignature1;
			else if (node.getType() == PrimitiveType.BOOLEAN)
				return unequalSignature2;
			else if (node.getType() == PrimitiveType.FLOATNUM)
				return unequalSignature3;
			else if (node.getType() == PrimitiveType.CHARACTER)
				return unequalSignature4;
			else return neverMatchedSignature;

		default:
			return neverMatchedSignature;
		}
	}

}