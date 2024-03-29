package semanticAnalyzer;

public class BoxType implements Type {

	private int sizeInBytes = 4;
	private int scopeSize;
	private int boxIdentifier;	
	private String boxName;
	private int hasPrint = 0;
	
	public BoxType(String boxName, int identifier) {
		this.boxName = boxName;
		boxIdentifier = identifier;
	}

	public int getSize() {
		return sizeInBytes;
	}

	public String infoString() {
		return "x:" + boxName;
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
	
	public String getBoxName() {
		return boxName;
	}
	
	public void setFlag(int size) {
		hasPrint = size;
	}
	
	public int getFlag() {
		return hasPrint;
	}
}