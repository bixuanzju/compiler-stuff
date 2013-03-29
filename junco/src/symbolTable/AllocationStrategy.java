package symbolTable;


// how a scope allocates offsets to its constituent memory blocks.
public interface AllocationStrategy {
	public String getBaseAddress();
	public MemoryLocation allocate(int sizeInBytes);
	public void saveState();
	public void restoreState();
	public int getMaxAllocatedSize();
	public void resetOffset();
}
