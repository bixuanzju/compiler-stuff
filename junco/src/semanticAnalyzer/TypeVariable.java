package semanticAnalyzer;

public class TypeVariable implements Type {

//	private String name;
	private Type constraitType;

	// public TypeVariable() {
	// infoString = constrain.infoString();
	// sizeInBytes = constrain.getSize();
	// // resetType();
	// constraitType = constrain;
	// }

	public void resetType() {
		constraitType = new AnyType();
	}

	public void constrain(Type type) {
		if (compatible(type)) {
			constraitType = type;
		}
		else {
			constraitType = new NoneType();
		}
	}

	private Boolean compatible(Type type) {
		if (constraitType instanceof NoneType) {
			return false;
		}
		else if (constraitType instanceof AnyType) {
			return true;
		}
		else if (constraitType.infoString().equals(type.infoString())) {
			return true;
		}

		return false;
	}
	
	public Type getConstraintType() {
		return constraitType;
	}

	@Override
	public int getSize() {
		return constraitType.getSize();
	}

	@Override
	public String infoString() {
		return constraitType.infoString();
	}

	@Override
	public Boolean isComparable() {
		
		return constraitType.isComparable();
	}
	
	

}
