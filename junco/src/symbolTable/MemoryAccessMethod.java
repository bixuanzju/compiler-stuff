package symbolTable;

import asmCodeGenerator.ASMCodeFragment;
import static asmCodeGenerator.ASMOpcode.*;

public enum MemoryAccessMethod {
	NULL_ACCESS () {
		public void generateAddress(ASMCodeFragment code, String baseAddress, int offset, String comment) {
			code.add(PushI, 0, comment);
		}
	},		
	
	// base is the label of the start of the memory block.
	DIRECT_ACCESS_BASE() {
		protected void generateBaseAddress(ASMCodeFragment code, String baseAddress) {
			code.add(PushD, baseAddress);
		}
	},
	
	// base is the label of the memory holding a pointer to the start of the memory block.
	INDIRECT_ACCESS_BASE() {
		protected void generateBaseAddress(ASMCodeFragment code, String baseAddress) {
			code.add(PushD, baseAddress);
			code.add(LoadI);
		}
	};	
	

	public void generateAddress(ASMCodeFragment code, String baseAddress, int offset, String comment) {
		generateBaseAddress(code, baseAddress);
		code.add(PushI, offset);
		code.add(Add, "", comment);
	}	
	public void generateAddress(ASMCodeFragment code, String baseAddress, int offset) {
		generateAddress(code, baseAddress, offset, "");
	}	
	protected void generateBaseAddress(ASMCodeFragment code, String baseAddress) {}

}
