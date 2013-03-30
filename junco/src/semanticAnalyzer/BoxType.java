package semanticAnalyzer;

public class BoxType implements Type {

	private int sizeInBytes = 4;
	private String boxName;

	public BoxType(String boxName) {
		this.boxName = boxName;
	}

	public int getSize() {
		return sizeInBytes;
	}

	public String infoString() {
		return "box:" + boxName;
	}

	@Override
	public boolean isComparable() {
		return false;
	}
}