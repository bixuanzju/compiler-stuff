package semanticAnalyzer;

public class BoxType implements Type {

	private int sizeInBytes = 4;
	private int scopeSize;
	private int boxIdentifier;	
	private String boxName;

	public BoxType(String boxName, int identifier) {
		this.boxName = boxName;
		boxIdentifier = identifier;
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
	
	public void setScopeSize(int size) {
		scopeSize = size;
	}
	
	public int getScopeSize() {
		return scopeSize;
	}
	
	public int getBoxIdentifier() {
		return boxIdentifier;
	}
}