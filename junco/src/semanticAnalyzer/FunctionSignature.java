package semanticAnalyzer;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;

class FunctionSignature {
	// private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private TypeVariable resultType;
	private TypeVariable[] paramTypes;

	// Object whichVariant;

	public FunctionSignature(TypeVariable... types) {
		assert (types.length >= 1);
		storeParamTypes(types);
		resultType = types[types.length - 1];
		// this.whichVariant = whichVariant;
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
		
		if (lextant == Keyword.IN) {
			paramTypes[0].constrain(types[0]);
			if (!(types[1] instanceof RangeType)) {
				return false;
			}
			paramTypes[1].constrain(((RangeType)types[1]).getChildType());
			
			if (paramTypes[0].isComparable()) {
				return true;
			}
			else {
				return false;
			}
		}
		

		for (int i = 0; i < paramTypes.length; i++) {
			paramTypes[i].constrain(types[i]);
		}

		if (paramTypes[0].getConstraintType() instanceof NoneType) {
			return false;
		}

		switch ((Punctuator) lextant) {
		case ADD:
		case MINUS:
		case MULTIPLY:
		case DIVIDE:
			if (resultType.getConstraintType() != PrimitiveType.INTEGER
					&& resultType.getConstraintType() != PrimitiveType.FLOATNUM) {
				return false;
			}
		
		else return true;
		case EQUAL:
		case UNEQUAL:
		case GREATEREQ:
		case GREATER:
		case LESSEQ:
		case LESS:
			if (getPrimitiveType(paramTypes[0].getConstraintType()) == PrimitiveType.BOOLEAN) {
				return false;
			}
			else return true;
		case AND:
		case OR:
			return true;
		case SPAN:
		case INTERSECTION:
				if (resultType.getConstraintType() instanceof RangeType) {
				return true;
			}
			else return false;
			
		default:
			return false;
		}

	}

	public static FunctionSignature signatureOf(Lextant lextant) {
		//assert (lextant instanceof Punctuator);
		//Punctuator punctuator = (Punctuator) lextant;

		TypeVariable typeVariable = new TypeVariable();
		TypeVariable resultType = new TypeVariable();
		typeVariable.resetType();
		resultType.resetType();

		if (lextant == Keyword.IN) {
			
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(typeVariable, typeVariable, resultType);
		}

		switch ((Punctuator) lextant) {
		case ADD:
		case MINUS:
		case MULTIPLY:
		case DIVIDE:
		case SPAN:
		case INTERSECTION:
			return new FunctionSignature(typeVariable, typeVariable, typeVariable);
		case GREATEREQ:
		case GREATER:
		case LESSEQ:
		case LESS:
		case EQUAL:
		case UNEQUAL:
			resultType.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(typeVariable, typeVariable, resultType);
		case AND:
		case OR:
			resultType.constrain(PrimitiveType.BOOLEAN);
			typeVariable.constrain(PrimitiveType.BOOLEAN);
			return new FunctionSignature(typeVariable, typeVariable, resultType);
		default:
			resultType.constrain(PrimitiveType.ERROR);
			return new FunctionSignature(resultType);
		}

	}

	public Boolean RangeSignature(Type type1, Type type2) {
		return true;
	}
	
	private Type getPrimitiveType(Type type) {
		if (type instanceof RangeType) {
			return getPrimitiveType(((RangeType) type).getChildType());
		}
		else {
			return type;
		}
	}

}