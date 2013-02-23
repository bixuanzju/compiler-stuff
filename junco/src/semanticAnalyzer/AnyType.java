package semanticAnalyzer;

public class AnyType implements Type {

	@Override
	public int getSize() {

		return 0;
	}

	@Override
	public String infoString() {

		return "Any type";
	}

}
