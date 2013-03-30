package semanticAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class FunctionType implements Type {

//	private String name;
	private List<Type> functionType;
	
	public FunctionType() {
		functionType = new ArrayList<Type>();
	}
	
	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public String infoString() {
		
		return "function signature";
	}

	@Override
	public boolean isComparable() {
		return false;
	}
	
	
	public void appendType(Type type) {
		functionType.add(type);
	}

	public List<Type> getList() {
		return functionType;
	}

	public Type getReturnType() {
		return functionType.get(functionType.size() - 1);
	}

	
	

}
