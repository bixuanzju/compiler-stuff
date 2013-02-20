package asmCodeGenerator;
import static asmCodeGenerator.ASMOpcode.*;
import static asmCodeGenerator.ASMCodeFragment.CodeType.*;
public class RunTime {
	public static final String EAT_LOCATION_ZERO    = "$eat-location-zero";		// helps us distinguish null pointers from real ones.
	public static final String INTEGER_PRINT_FORMAT = "$print-format-integer";
	public static final String FLOAT_PRINT_FORMAT = "$print-format-float";
	public static final String CHARACTER_PRINT_FORMAT = "$print-format-character";
	public static final String BOOLEAN_PRINT_FORMAT = "$print-format-boolean";
	public static final String NEWLINE_PRINT_FORMAT = "$print-format-newline";
	public static final String BOOLEAN_TRUE_STRING  = "$boolean-true-string";
	public static final String BOOLEAN_FALSE_STRING = "$boolean-false-string";
	public static final String PRINT_SPACER_STRING  = "$print-spacer-string";
	public static final String GLOBAL_MEMORY_BLOCK  = "$global-memory-block";
	public static final String USABLE_MEMORY_START  = "$usable-memory-start";
	public static final String MAIN_PROGRAM_LABEL   = "$$main";
	public static final String DIVIDE_BY_ZERO   = "$$divede-by-zero";
	public static final String ERROR_MESSAGE_IF_DIVIDE_BY_ZERO   = "$print-error-message";
	public static final String GENERAL_RUNTIME_ERROR  = "$$general-runtime-error";
	public static final String RUNTIME_ERROR_MESSAGE  = "$print-runtime-error-message";
	

	private ASMCodeFragment environmentASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(jumpToMain());
		result.append(stringsForPrintf());
		result.add(DLabel, USABLE_MEMORY_START);
		return result;
	}
	
	private ASMCodeFragment jumpToMain() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Jump, MAIN_PROGRAM_LABEL);
		return frag;
	}

	private ASMCodeFragment stringsForPrintf() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, EAT_LOCATION_ZERO);
		frag.add(DataZ, 8);
		frag.add(DLabel, INTEGER_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, FLOAT_PRINT_FORMAT);
		frag.add(DataS, "%g");
		frag.add(DLabel, CHARACTER_PRINT_FORMAT);
		frag.add(DataS, "%c");
		frag.add(DLabel, BOOLEAN_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, NEWLINE_PRINT_FORMAT);
		frag.add(DataS, "\n");
		frag.add(DLabel, BOOLEAN_TRUE_STRING);
		frag.add(DataS, "true");
		frag.add(DLabel, BOOLEAN_FALSE_STRING);
		frag.add(DataS, "false");
		frag.add(DLabel, PRINT_SPACER_STRING);
		frag.add(DataS, " ");
		frag.add(DLabel, ERROR_MESSAGE_IF_DIVIDE_BY_ZERO);
		frag.add(DataS, "Damn error! you must divided by zero!!!\n");
		
		return frag;
	}
	
	public static ASMCodeFragment getEnvironment() {
		RunTime rt = new RunTime();
		return rt.environmentASM();
	}
}
