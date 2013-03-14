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
		int sum = 0;
		for (Type child : functionType) {
			sum+=child.getSize();
		}
		return sum;
	}

	@Override
	public String infoString() {
		
		return "function signature";
	}

	@Override
	public Boolean isComparable() {
		return false;
	}
	
	
	public void appendType(Type type) {
		functionType.add(type);
	}

	public List<Type> getList() {
		return functionType;
	}


	
	

}
