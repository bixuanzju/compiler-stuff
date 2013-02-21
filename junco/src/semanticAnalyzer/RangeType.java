package semanticAnalyzer;

public class RangeType implements Type {
	
	private int sizeInBytes = 4;
	public Type childType;
	
	public RangeType(Type childType) {
		this.childType = childType;
	}
//	private RangeType(int size, String infoString) {
//		this.sizeInBytes = size;
//		this.infoString = infoString;
//	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return "Range:"+ childType.infoString();
	}
}
