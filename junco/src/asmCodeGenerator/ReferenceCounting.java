package asmCodeGenerator;

import static asmCodeGenerator.ASMHelper.*;
import static asmCodeGenerator.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.ASMOpcode.*;
import asmCodeGenerator.ASMCodeFragment;

public class ReferenceCounting {
	public  static final String REF_COUNTER_INITIALIZE             = "-ref-counter-initialize";
	private static final String RCTR_END_INITIALIZATION            = "-rctr-end-initialization";
	private static final String RCTR_TO_DECREMENT_STACK            = "$rctr-to-decrement-stack";
	private static final String RCTR_DECREMENT_STACK_INDEX         = "$rctr-to-decrement-stack-index";
	private static final String RCTR_DECREMENT_STACK_SIZE  		   = "$rctr-to-decrement-stack-size";

	public  static final String REF_COUNTER_PUSH_RECORD            = "-ref-counter-push-record";
	private static final String RCTR_PUSH_RETURN_ADDRESS           = "$rctr-push-return-address";
	private static final String RCTR_PUSH_RECORD_STACK_OKAY 	   = "-rctr-push-record-stack-okay";

	private static final String RCTR_IS_DECREMENT_STACK_EMPTY      = "-rctr-is-decrement-stack-empty";
	private static final String RCTR_STACK_EMPTY_TRUE              = "-rctr-stack-empty-true";
	private static final String RCTR_STACK_EMPTY_JOIN              = "-rctr-stack-empy-join";

	public  static final String REF_COUNTER_INCREMENT_REFCOUNT     = "-ref-counter-increment-refcount";
	private static final String RCTR_PERFORM_DECREMENTS_LOOP 	   = "-rctr-perform-decrements-loop";
	private static final String RCTR_PERFORM_DECREMENTS_DONE	   = "-rctr-perform-decrements-done";
	public  static final String REF_COUNTER_PERFORM_DECREMENTS     = "-ref-counter-perform-decrements";
	private static final String RCTR_PROCESS_ONE_DONE              = "-rctr-process-one-done";
	private static final String RCTR_PROCESS_ONE_POPDONE           = "-rctr-process-one-popdone";
	private static final String RCTR_RELEASE_NEXT_1                = "-rctr-release-next-1";

	private static final String REF_COUNTER_STACK_SIZE_EXCEEDED_MESSAGE = "$errors-decrement-stack-size-exceeded";
	private static final String REF_COUNTER_STACK_SIZE_EXCEEDED_ERROR   = "-ref-counter-stack_size_exceeded_error";

	// to-decrement stack information.  We never grow the stack, but give an error if it overflows.
	// a more-robust implementation would grow the stack on overflow.  We keep the simple implementation
	// because stack overflow in testing may indicate an error.
	private static final int initialStackSize = 200;
	private static final int ASMPointerSize = 4;
	
	// general record information:
	@SuppressWarnings("unused") 
	private static final int RECORD_REFERENCE_COUNT_OFFSET 		 = 0;	// NOTE: always assumed to be zero...we don't add this offset in the code.
	private static final int RECORD_TYPECODE_OFFSET 			 = 4;
	
	// range record information:
	@SuppressWarnings("unused")
	private static final int RANGE_WITHOUT_CHILDREN_TYPECODE = 2;
	private static final int RANGE_WITH_CHILDREN_TYPECODE 	 = 3;
	
	private static final int OFFSET_FOR_RANGE_CHILD_1 = 12;		
	private static final int OFFSET_FOR_RANGE_CHILD_2 = OFFSET_FOR_RANGE_CHILD_1 + ASMPointerSize;
	
	private static final boolean DEBUGGING = false;
	private static Labeller labeller = new Labeller();
	
	
	// this code should reside on the executable pathway before the application,
	// after initialization of the memory manager.
	public static ASMCodeFragment codeForInitialization() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Label, REF_COUNTER_INITIALIZE);

		declareI(frag, RCTR_TO_DECREMENT_STACK);		// freshly-allocated memory for stack.
		frag.add(PushI, initialStackSize *4);
		frag.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
		storeITo(frag, RCTR_TO_DECREMENT_STACK);
		
		declareI(frag, RCTR_DECREMENT_STACK_INDEX);		// initialized to zero.

		declareI(frag, RCTR_DECREMENT_STACK_SIZE);
		frag.add(PushI, initialStackSize);
		storeITo(frag, RCTR_DECREMENT_STACK_SIZE);
		
		
		frag.add(Jump, RCTR_END_INITIALIZATION);
		
		frag.append(subroutinePushOntoToDecrementStack());
		frag.append(subroutineIsStackEmpty());
		frag.append(subroutineIncrementReferences());
		frag.append(subroutineDecrementAllReferences());
		
		frag.add(Label, RCTR_END_INITIALIZATION);
		return frag;
	}

	private static ASMCodeFragment subroutinePushOntoToDecrementStack() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Label, REF_COUNTER_PUSH_RECORD);		    // [... recordPtr (return)]

		declareI(frag, RCTR_PUSH_RETURN_ADDRESS);
		storeITo(frag, RCTR_PUSH_RETURN_ADDRESS); 			// [... recordPtr]
		if(DEBUGGING) {
			printRecordPtrAndRefcount(frag, "push: ");
		}
		
		// check if overflowing
		loadIFrom(frag, RCTR_DECREMENT_STACK_INDEX);		// [... recordPtr index]
		loadIFrom(frag, RCTR_DECREMENT_STACK_SIZE);			// [... recordPtr index size]
		frag.add(Subtract);									// [... recordPtr index-size]
		frag.add(JumpNeg, RCTR_PUSH_RECORD_STACK_OKAY);
		decrementStackSizeExceededError(frag);
		
		// store the recordPtr on the stack.
		frag.add(Label, RCTR_PUSH_RECORD_STACK_OKAY);
		generateTopOfStackPointer(frag);					// [... recordPtr addrInStack]							
		frag.add(Exchange);									// [... addrInStack recordPtr]
		frag.add(StoreI);									// [...]

		incrementInteger(frag, RCTR_DECREMENT_STACK_INDEX);		

		loadIFrom(frag, RCTR_PUSH_RETURN_ADDRESS);			// [... (return)]
		frag.add(Return);

		return frag;
	}

	private static void generateTopOfStackPointer(ASMCodeFragment frag) {
		loadIFrom(frag, RCTR_TO_DECREMENT_STACK);	            // [... recordPtr base]
		loadIFrom(frag, RCTR_DECREMENT_STACK_INDEX);		    // [... recordPtr base index]
		frag.add(PushI, ASMPointerSize);						// [... recordPtr base index PtrSize]
		frag.add(Multiply);										// [... recordPtr base offset]
		frag.add(Add);											// [... recordPtr addrInStack]
	}
	private static void decrementStackSizeExceededError(ASMCodeFragment frag) {
		
		frag.add(DLabel, REF_COUNTER_STACK_SIZE_EXCEEDED_MESSAGE);
		frag.add(DataS, "size of reference-counting decrement stack exceeded");

		frag.add(Label, REF_COUNTER_STACK_SIZE_EXCEEDED_ERROR);
		frag.add(PushD, REF_COUNTER_STACK_SIZE_EXCEEDED_MESSAGE);

		frag.add(DLabel, RunTime.RUNTIME_ERROR_MESSAGE);
		frag.add(DataS, "Runtime error: ");

		frag.add(PushD, RunTime.RUNTIME_ERROR_MESSAGE);

		frag.add(Jump, RunTime.GENERAL_RUNTIME_ERROR);
		// repair the above to print  "Runtime error:" + REF_COUNTER_STACK_SIZE_EXCEEDED_MESSAGE,  and then halt.			
	}
	
	
	// this puts a 0/1 boolean on the stack, but it would be simpler
	// to just put the decrement stack index on the stack,
	// if the caller promises to play nice with it.
	private static ASMCodeFragment subroutineIsStackEmpty() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Label, RCTR_IS_DECREMENT_STACK_EMPTY);		// [... (return)]
		loadIFrom(frag, RCTR_DECREMENT_STACK_INDEX);		// [... (return) index]
		frag.add(JumpFalse, RCTR_STACK_EMPTY_TRUE);	        // [... (return)]
		frag.add(PushI, 0);									// [... (return) 0]
		frag.add(Jump, RCTR_STACK_EMPTY_JOIN);
		frag.add(Label, RCTR_STACK_EMPTY_TRUE);
		frag.add(PushI, 1);									// [... (return) 1]
		frag.add(Label, RCTR_STACK_EMPTY_JOIN);			    // [... (return) value]
//		if(DEBUGGING) {
//			ASMCodeGenerator.ptop(frag, " stack_is_empty: %d\n");
//		}
		frag.add(Exchange);									// [... value (return)]
		frag.add(Return);

		return frag;
	}

	private static ASMCodeFragment subroutineIncrementReferences() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Label, REF_COUNTER_INCREMENT_REFCOUNT);	// [... recordAddr (return)]
		frag.add(Exchange);									// [... (return) recordAddr]
		
		if(DEBUGGING) {
			frag.add(Duplicate);										// [... (return) recordAddr recordAddr]
			addToRefCount(frag, 1);										// [... (return) recordAddr]
			printRecordPtrAndRefcount(frag, "increment refcount:  ");	// [... (return) recordAddr]
			frag.add(Pop);												// [... (return)]
		}
		else {
			addToRefCount(frag, 1);							// [... (return)]
		}
		
		frag.add(Return);									// [...]

		return frag;
	}

	private static ASMCodeFragment subroutineDecrementAllReferences() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		// while (!stackEmpty())
		//     processOneDecrement();
		frag.add(Label, REF_COUNTER_PERFORM_DECREMENTS);	// [... (return)]
		if(DEBUGGING) {
		    pstring(frag, "decrement-all-references\n");
		}
		frag.add(Label, RCTR_PERFORM_DECREMENTS_LOOP);
		frag.add(Call, RCTR_IS_DECREMENT_STACK_EMPTY);			// [... (return) stackEmpty? ]
		frag.add(JumpTrue, RCTR_PERFORM_DECREMENTS_DONE);	// [... (return)]

		processOneDecrement(frag);

		frag.add(Jump, RCTR_PERFORM_DECREMENTS_LOOP);
		frag.add(Label, RCTR_PERFORM_DECREMENTS_DONE);

		frag.add(Return);									// [...]
		return frag;
	}

	private static void processOneDecrement(ASMCodeFragment frag) {
		// take element off stack
		decrementInteger(frag, RCTR_DECREMENT_STACK_INDEX);	// this moves index to the last element in the stack.
		generateTopOfStackPointer(frag);					// [... topPtr]
		frag.add(LoadI);									// [... recordPtr]
		
		
		// subtract one from its references
		frag.add(Duplicate);								// [... recordPtr recordPtr]
		
		if(DEBUGGING) {
			addToRefCount(frag, -1);										// [... recordPtr]
			printRecordPtrAndRefcount(frag, "process one decrement:  ");	// [... recordPtr]
		}
		else {
			addToRefCount(frag, -1);						// [... recordPtr]
		}
		
		
		// if(refCount now 0)
		//    releaseChildrenAndDeallocate(element)
		frag.add(Duplicate);								// [... recordPtr recordPtr]
		frag.add(LoadI);									// [... recordPtr refCount]
		frag.add(JumpPos, RCTR_PROCESS_ONE_POPDONE);		// [... recordPtr]
		releaseChildrenAndDeallocate(frag);
		frag.add(Jump, RCTR_PROCESS_ONE_DONE);
		
		frag.add(Label, RCTR_PROCESS_ONE_POPDONE);			// [... recordPtr]
		frag.add(Pop);										// [... ]
		frag.add(Label, RCTR_PROCESS_ONE_DONE);
	}

	// [... ptr] -> [... ptr]  (does not disturb stack)
	private static void printRecordPtrAndRefcount(ASMCodeFragment frag, String s) {
																// [... ptr]
		ptop(frag, s + "record ptr %d ");		// [... ptr]
		frag.add(Duplicate);									// [... ptr ptr]
		frag.add(PushI, 4);										// [... ptr ptr 4]
		frag.add(Add);											// [... ptr ptr+4]
		frag.add(LoadI);										// [... ptr typecode]
		ptop(frag,  " typecode %d ");			// [... ptr typecode]
		frag.add(Pop);											// [... ptr ]
		frag.add(Duplicate);									// [... ptr ptr ]
		frag.add(LoadI);										// [... ptr refCount]
		ptop(frag, "refCount now %d\n");		// [... ptr refCount]
		frag.add(Pop);											// [... ptr]
	}

	// [... recordPtr] -> [...]
	private static void releaseChildrenAndDeallocate(ASMCodeFragment frag) {
		frag.add(Duplicate);								// [... recordPtr recordPtr]
		if(DEBUGGING) {
			ptop(frag, "Release record %d\n");
		}
		typecodeOfRecord(frag);								// [... recordPtr typecode]

		frag.add(PushI, RANGE_WITH_CHILDREN_TYPECODE);		// [... recordPtr typecode testTypecode]
		frag.add(Subtract);									// [... recordPtr !typecodesEqual?]
		frag.add(JumpTrue, RCTR_RELEASE_NEXT_1);			// [... recordPtr]


		// if we are here, then this is a range with children.  So, add children to stack to process.
		if(DEBUGGING) {
			ptop(frag, "     releasing children of %d\n");
		}
		pushChildRecord(frag, OFFSET_FOR_RANGE_CHILD_1);	// [... recordPtr]
		pushChildRecord(frag, OFFSET_FOR_RANGE_CHILD_2);	// [... recordPtr]
		
		frag.add(Label, RCTR_RELEASE_NEXT_1);				// [... recordPtr]
		// add code for compound types other than range here.
		
		// now we can deallocate!
		if(DEBUGGING) {
			ptop(frag, "     deallocating %d\n");
		}
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);    // [...]
	}

	/** Given a record pointer, add the parameter "amount" to its reference count. <br />
	 * [... recordPtr] -> [...]
	 */
	private static void addToRefCount(ASMCodeFragment frag, int amount) {
		frag.add(Duplicate);						        // [... recordAddr recordAddr]
		frag.add(LoadI);									// [... recordAddr numRefs]
		frag.add(PushI, amount);							// [... recordAddr numRefs amount]
		frag.add(Add);										// [... recordAddr newNumRefs]
		frag.add(StoreI);									// [... ]
	}	
	/** Given a record pointer, push the child whose offset is the parameter
	 * onto the to-decrement stack.  <br />
	 * [... recordPtr] -> [... recordPtr]
	 */
	private static void pushChildRecord(ASMCodeFragment frag, int offset) {
		frag.add(Duplicate);							// [... recordPtr recordPtr]
		readIOffset(frag, offset);						// [... recordPtr childRecordPtr]
		frag.add(Call, REF_COUNTER_PUSH_RECORD);		// [... recordPtr]
	}
	/** given a record pointer, get its typecode.  <br />
	 * [... recordPtr] -> [... typecode]
	 */
	private static void typecodeOfRecord(ASMCodeFragment frag) {
		frag.add(PushI, RECORD_TYPECODE_OFFSET);			// [... recordPtr offset]
		frag.add(Add);										// [... typecodePtr]
		frag.add(LoadI);									// [... typecode]
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	// debugging - print top of stack [... formatString] -> [...]
	// (does not eat top of stack)
	public static void ptop(ASMCodeFragment code, String format) {
		code.add(Duplicate);
		String stringLabel = labeller.newLabel("ptop-", "");
		code.add(DLabel, stringLabel);
		code.add(DataS, format);
		code.add(PushD, stringLabel);
		code.add(Printf);
	}
	
	public static void pstring(ASMCodeFragment code, String string) {
		String label = labeller.newLabel("pstring", "");
		code.add(DLabel, label);
		code.add(DataS, string);
		code.add(PushD, label);
		code.add(Printf);
	}
}



