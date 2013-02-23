package semanticAnalyzer;

import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

class FunctionSignature {
	// private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private TypeVariable resultType;
	private TypeVariable[] paramTypes;
	Object whichVariant;

	public FunctionSignature(Object whichVariant, TypeVariable... types) {
		assert (types.length >= 1);
		storeParamTypes(types);
		resultType = types[types.length - 1];
		this.whichVariant = whichVariant;
	}

	private void storeParamTypes(TypeVariable[] types) {
		paramTypes = new TypeVariable[types.length - 1];
		for (int i = 0; i < types.length - 1; i++) {
			paramTypes[i] = types[i];
		}
	}

	public TypeVariable resultType() {
		return resultType;
	}

	public boolean accepts(Lextant lextant, Type... types) {
		if (types.length != paramTypes.length) {
			return false;
		}

		for (int i = 0; i < paramTypes.length; i++) {
			paramTypes[i].constrain(types[i]);
		}

		if (paramTypes[0].getConstraintType() instanceof NoneType) {
			return false;
		}

		if ((Punctuator) lextant == Punctuator.ADD
				|| (Punctuator) lextant == Punctuator.MINUS
				|| (Punctuator) lextant == Punctuator.MULTIPLY
				|| (Punctuator) lextant == Punctuator.DIVIDE) {
			if (resultType.getConstraintType() != PrimitiveType.INTEGER
					&& resultType.getConstraintType() != PrimitiveType.FLOATNUM) {
				return false;
			}
		}
		
		if ((Punctuator) lextant == Punctuator.GREATEREQ
				|| (Punctuator) lextant == Punctuator.GREATER
				|| (Punctuator) lextant == Punctuator.LESSEQ
				|| (Punctuator) lextant == Punctuator.LESS) {
			if (paramTypes[0].getConstraintType() == PrimitiveType.BOOLEAN) {
				return false;
			}
		}
		
//		if ((Punctuator) lextant == Punctuator.EQUAL
//				|| (Punctuator) lextant == Punctuator.UNEQUAL) {
//			if (resultType.getConstraintType() != PrimitiveType.INTEGER
//					&& resultType.getConstraintType() != PrimitiveType.FLOATNUM) {
//				return false;
//			}
//		}
		return true;
	}

	public static FunctionSignature signatureOf(Lextant lextant) {
		assert (lextant instanceof Punctuator);
		Punctuator punctuator = (Punctuator) lextant;

		TypeVariable typeVariable = new TypeVariable();
		TypeVariable resultType = new TypeVariable();
		typeVariable.resetType();
		resultType.resetType();

		switch (punctuator) {
		case ADD:
			return new FunctionSignature(1, typeVariable, typeVariable, typeVariable);
		case MINUS:
			return new FunctionSignature(1, typeVariable, typeVariable, typeVariable);
		case MULTIPLY:
			return new FunctionSignature(1, typeVariable, typeVariable, typeVariable);
		case DIVIDE:
			return new FunctionSignature(1, typeVariable, typeVariable, typeVariable);
		case GREATEREQ:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case GREATER:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case LESSEQ:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case LESS:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case EQUAL:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case UNEQUAL:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case AND:
			resultType.constrain(PrimitiveType.BOOLEAN);
			typeVariable.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		case OR:
			resultType.constrain(PrimitiveType.BOOLEAN);
			typeVariable.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(1, typeVariable, typeVariable, resultType);
		default:
			resultType.constrain(PrimitiveType.ERROR);
			return new FunctionSignature(1, resultType);
		}

	}

	// TODO Type variable
	public Boolean RangeSignature(Type type1, Type type2) {
		return true;
	}

}