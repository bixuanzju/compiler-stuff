package semanticAnalyzer;

public class NoneType implements Type{

	@Override
	public int getSize() {
	
		return 0;
	}

	@Override
	public String infoString() {
	
		return "None type";
	}

	@Override
	public Boolean isComparable() {
	
		return false;
	}

}
