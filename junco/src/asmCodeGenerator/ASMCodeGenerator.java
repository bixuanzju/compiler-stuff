package asmCodeGenerator;

import static asmCodeGenerator.ASMOpcode.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.FunctionDeclNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.MemberAccessNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.UniaryOperatorNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.ValueBodyNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.BoxType;
import semanticAnalyzer.PrimitiveType;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.ASMHelper.declareI;
import static asmCodeGenerator.ASMHelper.loadIFrom;
import static asmCodeGenerator.ASMHelper.storeITo;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}

	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}

	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.append(MemoryManager.codeForInitialization());
		code.append(ReferenceCounting.codeForInitialization());

		code.append(RunTime.getEnvironment());
		code.append(globalVariableBlockASM());
		code.append(programASM());
		code.append(MemoryManager.codeForAfterApplication());

		return code;
	}

	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();

		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}

	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.add(Label, RunTime.MAIN_PROGRAM_LABEL);

		// allocate memory for frame pointer and stack pointer
		code.add(Memtop);
		declareI(code, RunTime.GLOBAL_FRAME_POINTER);
		storeITo(code, RunTime.GLOBAL_FRAME_POINTER);
		code.add(Memtop);
		declareI(code, RunTime.GLOBAL_STACK_POINTER);
		storeITo(code, RunTime.GLOBAL_STACK_POINTER);

		code.add(Jump, RunTime.BOX_MAIN_LABEL);
		code.append(programCode());
		code.add(Label, RunTime.BOX_MAIN_END_LABEL);
		code.add(Halt);

		code.add(Label, RunTime.DIVIDE_BY_ZERO);
		// print error message
		code.add(PushD, RunTime.ERROR_MESSAGE_IF_DIVIDE_BY_ZERO);
		code.add(Printf);
		code.add(Halt);

		code.add(Label, RunTime.GENERAL_RUNTIME_ERROR);
		code.add(Printf);
		code.add(Halt);

		return code;
	}

	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}

	private class CodeVisitor extends ParseNodeVisitor.Default {
		private Labeller labeller = new Labeller();
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;

		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}

		// //////////////////////////////////////////////////////////////////
		// Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}

		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}

		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

		// //////////////////////////////////////////////////////////////////
		// Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}

		public ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}

		private ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}

		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}

		private ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}

		// //////////////////////////////////////////////////////////////////
		// convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();

			if (code.isAddress()) {
				turnAddressIntoValue(code, node);
			}
		}

		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if (node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}
			else if (node.getType() == PrimitiveType.FLOATNUM) {
				code.add(LoadF);
			}
			else if (node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}
			else if (node.getType() == PrimitiveType.CHARACTER) {
				code.add(LoadC);
			}
			else if ((node.getType() instanceof RangeType)
					|| (node.getType() instanceof BoxType)) {
				code.add(LoadI);
			}
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}

		// //////////////////////////////////////////////////////////////////
		// ensures all types of ParseNode in given AST have at least a
		// visitLeave
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}

		// /////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				if (child.getToken().getLexeme().equals("main")) {
					code.add(Label, RunTime.BOX_MAIN_LABEL);
					code.append(childCode);
					code.add(Jump, RunTime.BOX_MAIN_END_LABEL);
				}
				else {
					code.add(Label, child.getToken().getLexeme());
					code.append(childCode);
					code.add(Return);
				}

			}

		}

		public void visitLeave(BoxBodyNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}

			if (node.getToken().getLexeme().equals("main")) {
				cleanReference(node.getScope());
				code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
			}
		}

		private void cleanReference(Scope scope) {
			Collection<Binding> allNames = scope.getSymbolTable().values();

			for (Binding binding : allNames) {
				if ((binding.getType() instanceof RangeType)
						|| (binding.getType() instanceof BoxType)) {
					binding.generateAddress(code);
					code.add(LoadI);
					code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
				}
			}
		}

		public void visitLeave(ValueBodyNode node) {
			newValueCode(node);

			for (int i = 0; i < node.nChildren() - 1; i++) {
				ParseNode child = node.child(i);

				if (child instanceof ReturnStatementNode) {
					code.append(removeValueCode(child));
				}
				else {
					code.append(removeVoidCode(child));
				}
			}

			code.append(removeValueCode(node.child(node.nChildren() - 1)));

			code.add(Label, node.getReturnLabel());

			cleanReference(node.getScope());
			// code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
		}

		public void visitLeave(BodyNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				if (child instanceof ReturnStatementNode) {
					code.append(removeValueCode(child));
				}
				else {
					code.append(removeVoidCode(child));
				}

			}

			cleanReference(node.getScope());
			code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
		}

		// /////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);

			for (ParseNode child : node.getChildren()) {
				appendPrintCode(child);
				appendSpacerCode(node);
			}
			appendNewlineCode(node);

			// code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
		}

		private void appendNewlineCode(PrintStatementNode node) {
			if (node.hasNewline()) {
				code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
				code.add(Printf);
			}
		}

		private void appendSpacerCode(PrintStatementNode node) {
			if (node.hasSpaces()) {
				code.add(PushD, RunTime.PRINT_SPACER_STRING);
				code.add(Printf);
			}
		}

		private void appendPrintCode(ParseNode node) {

			if (node.getType() instanceof RangeType) {
				RangeType type = (RangeType) node.getType();

				String startlabel = labeller.newLabel("-range-start-", "");
				String falselabel = labeller
						.newLabelSameNumber("-range-false-end-", "");
				String endlabel = labeller.newLabelSameNumber("-range-end-", "");

				code.append(removeValueCode(node));

				code.add(Call, startlabel);
				code.add(Jump, endlabel);

				code.add(Label, startlabel);
				code.add(Exchange); // [... pc ptr]
				code.add(PushD, RunTime.OPEN_SQUARE_STRING);
				code.add(Printf);

				// I need to know the type identifier
				code.add(Duplicate);
				code.add(PushI, 4); // [... ptr ptr 4]
				code.add(Add); // [... ptr ptr+4]
				code.add(LoadI); // [... ptr id]
				code.add(PushI, 2); // [... ptr id 2]
				code.add(Subtract); // [... ptr id-2]
				code.add(JumpFalse, falselabel); // [... ptr]
				// god, it's range type, how can I print
				code.add(Duplicate); // [... ptr ptr]
				code.add(PushI, 12); // [... ptr ptr 12]
				code.add(Add); // [... ptr ptr+12]
				code.add(LoadI); // [... ptr lptr]
				code.add(Exchange); // [... lptr ptr]
				code.add(PushI, 16);
				code.add(Add);
				code.add(LoadI); // [... lptr hptr]
				code.add(Exchange); // [... hptr lptr]
				// what to do?
				code.add(Call, startlabel);
				code.add(PushD, RunTime.SPLICE_STRING);
				code.add(Printf); // [... htpr]
				code.add(Call, startlabel);
				code.add(PushD, RunTime.CLOSE_SQUARE_STRING);
				code.add(Printf);
				code.add(Return);

				// ok, it's primitive type, now I need to know what type size actually
				code.add(Label, falselabel);

				if (getPrimitiveType(type) == PrimitiveType.FLOATNUM) {
					code.add(Duplicate); // [...ptr ptr]
					code.add(PushI, 12); // [... ptr ptr 12]
					code.add(Add); // [... ptr ptr+12]
					code.add(LoadF); // [... ptr float]
					code.add(PushD, RunTime.FLOAT_PRINT_FORMAT);
					code.add(Printf); // [... ptr]
					code.add(PushD, RunTime.SPLICE_STRING);
					code.add(Printf); // [... ptr]
					code.add(PushI, 20); // [... ptr 20]
					code.add(Add); // [... ptr+20]
					code.add(LoadF); // [... float]
					code.add(PushD, RunTime.FLOAT_PRINT_FORMAT);
					code.add(Printf);

				}
				else if (getPrimitiveType(type) == PrimitiveType.INTEGER) {
					code.add(Duplicate); // [...ptr ptr]
					code.add(PushI, 12); // [... ptr ptr 12]
					code.add(Add); // [... ptr ptr+12]
					code.add(LoadI); // [... ptr int]
					code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
					code.add(Printf); // [... ptr]
					code.add(PushD, RunTime.SPLICE_STRING);
					code.add(Printf); // [... ptr]
					code.add(PushI, 16); // [... ptr 16]
					code.add(Add); // [... ptr+16]
					code.add(LoadI); // [... int]
					code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
					code.add(Printf);

				}
				else if (getPrimitiveType(type) == PrimitiveType.BOOLEAN) {
					code.add(Duplicate); // [...ptr ptr]
					code.add(PushI, 12); // [... ptr ptr 12]
					code.add(Add); // [... ptr ptr+12]
					code.add(LoadC); // [... ptr char]
					convertToBoolean();
					code.add(Printf);

					code.add(PushD, RunTime.SPLICE_STRING);
					code.add(Printf); // [... ptr]
					code.add(PushI, 13); // [... ptr 13]
					code.add(Add); // [... ptr+13]
					code.add(LoadC); // [... char]
					convertToBoolean();
					code.add(Printf);

				}
				else if (getPrimitiveType(type) == PrimitiveType.CHARACTER) {
					code.add(Duplicate); // [...ptr ptr]
					code.add(PushI, 12); // [... ptr ptr 12]
					code.add(Add); // [... ptr ptr+12]
					code.add(LoadC); // [... ptr char]
					code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
					code.add(Printf); // [... ptr]

					code.add(PushD, RunTime.SPLICE_STRING);
					code.add(Printf); // [... ptr]
					code.add(PushI, 13); // [... ptr 13]
					code.add(Add); // [... ptr+13]
					code.add(LoadC); // [... char]
					code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
					// code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
					code.add(Printf);

				}

				code.add(PushD, RunTime.CLOSE_SQUARE_STRING);
				code.add(Printf);
				code.add(Return);

				code.add(Label, endlabel);

			}
			else if (node.getType() instanceof BoxType) {
				BoxType type = (BoxType) node.getType();
				if (type.getFlag() > 0) {

					loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
					code.add(PushI, 4);
					code.add(Subtract); // [... sp-4]
					code.add(Duplicate);
					code.append(removeValueCode(node)); // [... sp-4, sp-4, val]
					code.add(StoreI); // [... sp-4]
					storeITo(code, RunTime.GLOBAL_STACK_POINTER);

					code.add(Call, "$" + type.getBoxName() + "printx");

					// we back, stack is [...]
					// load the return value

					loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
					code.add(Duplicate); // [... sp, sp]
					int returnTypeSize = type.getFlag();
					switch (returnTypeSize) {
					case 1:
						code.add(LoadC); // [... sp, val]
						code.add(Exchange); // [... val, sp]
						code.add(PushI, 1);
						code.add(Add); // [... val, sp+1]
						storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
						break;
					case 4:
						code.add(LoadI);
						code.add(Exchange); // [... val, sp]
						code.add(PushI, 4);
						code.add(Add); // [... val, sp+4]
						storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
						break;
					case 8:
						code.add(LoadF);
						code.add(Exchange); // [... val, sp]
						code.add(PushI, 8);
						code.add(Add); // [... val, sp+8]
						storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
						break;
					default:
						break;
					}
					// get rid of return value
					code.add(Pop);
				}
				else {
					String label = labeller.newLabel("print-box-", "");
					code.add(DLabel, label);
					code.add(DataS, type.getBoxName());
					code.add(PushD, label);
					code.add(Printf);
				}

			}
			else {
				String format = printFormat(node.getType());
				code.append(removeValueCode(node));
				convertToStringIfBoolean(node);
				code.add(PushD, format);
				code.add(Printf);
			}
		}

		private Type getPrimitiveType(Type type) {
			if (type instanceof RangeType) {
				return getPrimitiveType(((RangeType) type).getChildType());
			}
			else {
				return type;
			}
		}

		private void convertToStringIfBoolean(ParseNode node) {
			if (node.getType() != PrimitiveType.BOOLEAN) {
				return;
			}

			String trueLabel = labeller.newLabel("-print-boolean-true", "");
			String endLabel = labeller.newLabelSameNumber("-print-boolean-join", "");

			code.add(JumpTrue, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
			code.add(Jump, endLabel);
			code.add(Label, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
			code.add(Label, endLabel);
		}

		private void convertToBoolean() {

			String trueLabel = labeller.newLabel("-print-boolean-true", "");
			String endLabel = labeller.newLabelSameNumber("-print-boolean-join", "");

			code.add(JumpTrue, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
			code.add(Jump, endLabel);
			code.add(Label, trueLabel);
			code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
			code.add(Label, endLabel);
		}

		private String printFormat(Type type) {
			assert type instanceof PrimitiveType;

			switch ((PrimitiveType) type) {
			case INTEGER:
				return RunTime.INTEGER_PRINT_FORMAT;
			case BOOLEAN:
				return RunTime.BOOLEAN_PRINT_FORMAT;
			case FLOATNUM:
				return RunTime.FLOAT_PRINT_FORMAT;
			case CHARACTER:
				return RunTime.CHARACTER_PRINT_FORMAT;
			default:
				assert false : "Type " + type
						+ " unimplemented in ASMCodeGenerator.printFormat()";
				return "";
			}
		}

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);

			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = getAndRemoveCode(node.child(1));

			if (rvalue.isAddress()) {
				turnAddressIntoValue(rvalue, node.child(1));
				code.append(rvalue); // [... ptr]
				if ((node.child(1).getType() instanceof RangeType)
						|| (node.child(1).getType() instanceof BoxType)) {
					code.add(Duplicate); // [... ptr ptr]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT);

				}

				code.append(lvalue);
				code.add(Exchange);
				Type type = node.getType();
				code.add(opcodeForStore(type));

			}
			else {

				code.append(lvalue);
				code.append(rvalue);
				Type type = node.getType();
				code.add(opcodeForStore(type));
			}

			// code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);

		}

		public void visitLeave(WhileStatementNode node) {
			newVoidCode(node);

			String startlabel = labeller.newLabel("-while-start-", "");
			String endlabel = labeller.newLabelSameNumber("-while-end-", "");

			code.add(Label, startlabel);
			ASMCodeFragment expr = removeValueCode(node.child(0));
			code.append(expr);
			code.add(JumpFalse, endlabel);
			ASMCodeFragment body = removeVoidCode(node.child(1));
			code.append(body);
			code.add(Jump, startlabel);
			
			code.add(Label, node.returnLoopLabel());
			code.add(Label, endlabel);

		}
		
		public void visitLeave(BreakStatementNode node) {
			newVoidCode(node);
			
			code.add(Jump, node.returnLoopLabel());
		}

		public void visitLeave(IfStatementNode node) {
			newVoidCode(node);

			String startlabel = labeller.newLabel("-if-start-", "");

			String endlabel = labeller.newLabelSameNumber("-if-end-", "");
			String elselabel = labeller.newLabelSameNumber("-if-else-", "");

			code.add(Label, startlabel);
			if (node.nChildren() == 2) {
				ASMCodeFragment expr = removeValueCode(node.child(0));
				code.append(expr);
				code.add(JumpFalse, endlabel);
				ASMCodeFragment ifBody = removeVoidCode(node.child(1));
				code.append(ifBody);
				code.add(Jump, endlabel);
			}
			else {
				ASMCodeFragment expr = removeValueCode(node.child(0));
				code.append(expr);
				code.add(JumpFalse, elselabel);
				ASMCodeFragment ifBody = removeVoidCode(node.child(1));
				code.append(ifBody);
				code.add(Jump, endlabel);
				code.add(Label, elselabel);
				ASMCodeFragment elseBody = removeVoidCode(node.child(2));
				code.append(elseBody);
				code.add(Jump, endlabel);

			}

			code.add(Label, endlabel);

		}

		public void visitLeave(ReturnStatementNode node) {
			newValueCode(node);

			ASMCodeFragment expr = removeValueCode(node.child(0));
			code.append(expr);
			// if (node.child(0).getType() instanceof RangeType) {
			// code.add(Duplicate);
			// code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
			// }
			code.add(Jump, node.getReturnLabel());

		}

		public void visitLeave(MemberAccessNode node) {
			ParseNode left = node.child(0);
			ParseNode right = node.child(1);

			if (right.getToken().getLexeme().equals("low")
					|| right.getToken().getLexeme().equals("high")) {

				newAddressCode(node);

				ASMCodeFragment value = getAndRemoveCode(left);

				if (!value.isAddress()) {
					code.append(value); // [... val]
					code.add(Duplicate); // [... val val]
					code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
				}
				else {
					turnAddressIntoValue(value, node.child(0));
					code.append(value); // [... val]
				}

				if (right.getToken().getLexeme().equals("low")) {

					code.add(PushI, 12);
					code.add(Add);

				}
				else {

					code.add(PushI, 12 + ((RangeType) left.getType()).getChildType()
							.getSize());
					code.add(Add);

				}
			}
			else {
				newValueCode(node);
				code.append(removeValueCode(right));
			}
		}

		public void visitLeave(UniaryOperatorNode node) {

			newValueCode(node);

			if (!(node.getToken().isLextant(Punctuator.AT))) {

				ASMCodeFragment value = removeValueCode(node.child(0));
				code.append(value); // [... val]

				if (node.getToken().isLextant(Punctuator.NOT)) {

					String startLabel = labeller.newLabel("-not-arg1-", "");
					String notendLabel = labeller.newLabelSameNumber("-not-end-", "");
					code.add(Label, startLabel);

					code.add(BNegate);
					code.add(Duplicate);
					code.add(JumpFalse, notendLabel);
					code.add(Pop);
					code.add(PushI, 1);
					code.add(Label, notendLabel);
				}
				else if (node.getToken().isLextant(Punctuator.CASTTOFLAOT)) {

					code.add(ConvertF);
				}
				else if (node.getToken().isLextant(Punctuator.CASTTOINT)) {

					if (node.child(0).getType() == PrimitiveType.FLOATNUM)
						code.add(ConvertI);

				}
				else if (node.getToken().isLextant(Punctuator.CASTTOCHAR)) {

					code.add(PushI, 127);
					code.add(BTAnd);
				}
			}
			else {
				ParseNode child = node.child(0);
				BoxType type = (BoxType) child.getType();
				code.add(PushI, 12 + type.getScopeSize());
				code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
				code.add(Duplicate); // [... ptr, ptr]

				// store ptr frame
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr, ptr, sp]
				code.add(PushI, 4);
				code.add(Subtract); // [... ptr, ptr, sp-4]
				code.add(Exchange); // [... ptr, sp-4, ptr]
				code.add(StoreI); // [... ptr]

				// update sp
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr, sp]
				code.add(PushI, 4);
				code.add(Subtract); // [... ptr, sp-4]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr]

				// store pre fp
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr, sp]
				code.add(PushI, 4);
				code.add(Subtract); // [... ptr, sp-4]
				loadIFrom(code, RunTime.GLOBAL_FRAME_POINTER); // [... ptr, sp-4, fp]
				code.add(StoreI); // [... ptr]

				// set fp to equal to sp
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER);
				storeITo(code, RunTime.GLOBAL_FRAME_POINTER);

				// update sp
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr, sp]
				code.add(PushI, 4);
				code.add(Subtract); // [... ptr, sp-4]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... ptr]

				code.add(Duplicate); // [... ptr, ptr]

				// for reference count
				code.add(PushI, 1);
				code.add(StoreI); // [... ptr]

				// for type identifier
				code.add(Duplicate); // [... ptr, ptr]
				code.add(PushI, 4);
				code.add(Add); // [... ptr, ptr+4]
				code.add(PushI, type.getBoxIdentifier());
				code.add(StoreI); // [... ptr]

				// set this pointer
				code.add(Duplicate); // [... ptr, ptr]
				code.add(Duplicate); // [... ptr, ptr, ptr]
				code.add(PushI, 12);
				code.add(Add); // [... ptr, ptr, ptr+12]
				code.add(Exchange); // [... ptr, ptr+12, ptr]
				code.add(StoreI); // [... ptr]

				code.add(Call, child.getToken().getLexeme());

				// restore fp
				loadIFrom(code, RunTime.GLOBAL_FRAME_POINTER);
				code.add(PushI, 4);
				code.add(Subtract);
				code.add(LoadI);
				storeITo(code, RunTime.GLOBAL_FRAME_POINTER);

				// restore sp
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER);
				code.add(PushI, 8);
				code.add(Add);
				storeITo(code, RunTime.GLOBAL_STACK_POINTER);

			}

		}

		public void visitLeave(UpdateStatementNode node) {
			newVoidCode(node);

			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = getAndRemoveCode(node.child(1));

			code.append(lvalue); // [... addr]

			// reference counting
			if ((node.child(0).getType() instanceof RangeType)
					|| (node.child(0).getType() instanceof BoxType)) {
				code.add(Duplicate); // [... addr addr]
				code.add(LoadI); // [... addr lptr]
				code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD); // [... addr]

				if (rvalue.isAddress()) {
					turnAddressIntoValue(rvalue, node.child(1));
					code.append(rvalue); // [... addr rprt]
					code.add(Duplicate); // [... addr rptr rptr]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT); // [...addr
																																						// rptr]
				}
				else {
					code.append(rvalue); // [... addr rptr]
				}
			}
			else {
				if (rvalue.isAddress()) {
					turnAddressIntoValue(rvalue, node.child(1));
				}
				code.append(rvalue); // [... addr rval]
			}

			Type type = node.getType();
			code.add(opcodeForStore(type));

			code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);

		}

		private ASMOpcode opcodeForStore(Type type) {
			if (type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if (type == PrimitiveType.FLOATNUM) {
				return StoreF;
			}
			if (type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			if (type == PrimitiveType.CHARACTER) {
				return StoreC;
			}
			if ((type instanceof RangeType) || (type instanceof BoxType)) {
				return StoreI;
			}

			assert false : "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}

		public void visitLeave(FunctionDeclNode node) {
			newVoidCode(node);

			String endlabel = labeller.newLabel("function-decl", "");

			// don't run the code until invoation enter in
			code.add(Jump, endlabel);

			ParseNode id = node.child(0);
			// ParseNode parameterList = node.child(1);
			ParseNode valueBody = node.child(2);

			code.add(Label, id.getToken().getLexeme());

			// enter handshaking

			// store previous fp
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [ ... pc, sp]
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Subtract); // [... pc, sp, sp-4]
			loadIFrom(code, RunTime.GLOBAL_FRAME_POINTER); // [... pc, sp, sp-4, fp]
			code.add(StoreI); // [... pc, sp]

			// store return address
			code.add(PushI, 8);
			code.add(Subtract); // [... pc, sp-8]
			code.add(Exchange); // [... sp-8, pc]
			code.add(StoreI); // [...]

			// set fp to be equal to sp
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
			storeITo(code, RunTime.GLOBAL_FRAME_POINTER); // [...]

			// set sp to the bottom of the frame
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
			code.add(PushI, 8);
			code.add(PushI, valueBody.getScope().getAllocatedSize());
			code.add(Add);
			code.add(Subtract); // [... sp-size]
			storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [...]

			// enter handshaking over

			// user code
			code.append(removeValueCode(valueBody));

			// exit handshaking

			// now the stack is like [... val], push return address onto stack
			loadIFrom(code, RunTime.GLOBAL_FRAME_POINTER);
			code.add(PushI, 8);
			code.add(Subtract); // [... val, fp-8]
			code.add(LoadI); // [... val, pc]
			// set fp to previous
			loadIFrom(code, RunTime.GLOBAL_FRAME_POINTER); // [... val, pc, fp]
			code.add(PushI, 4);
			code.add(Subtract); // [... val, pc, fp-4]
			code.add(LoadI); // [... val, pc, previous fp]
			storeITo(code, RunTime.GLOBAL_FRAME_POINTER); // [... val, pc]

			code.add(Exchange); // [... pc, val]
			// increase sp
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... pc, val, sp]
			code.add(PushI, 8);
			code.add(PushI, valueBody.getScope().getAllocatedSize());
			code.add(Add);
			code.add(PushI, node.getScope().getAllocatedSize() + 4); // lambda lifting
			code.add(Add);
			code.add(Add); // [... pc, val, sp+size]
			storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... pc, val]

			// store return val to frame
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... pc, val, sp]
			code.add(PushI, node.getType().getSize());
			code.add(Subtract); // [... pc, val, sp-size]
			code.add(Duplicate); // [... pc, val, sp-size, sp-size]
			// set sp to new val
			storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... pc, val, sp-size]
			code.add(Exchange); // [... pc, sp-size, val]
			switch (node.getType().getSize()) {
			case 1:
				code.add(StoreC);
				// code.add(PushI, 1);
				break;
			case 4:
				code.add(StoreI);
				// code.add(PushI, 4);
				break;
			case 8:
				code.add(StoreF);
				// code.add(PushI, 8);
				break;
			default:
				break;
			} // [... pc]

			code.add(Return);

			code.add(Label, endlabel);

		}

		public void visitLeave(CallStatementNode node) {
			newVoidCode(node);

			code.append(removeValueCode(node.child(0)));

			code.add(Pop);
		}

		// /////////////////////////////////////////////////////////////////////////
		// expressions

		public void visitLeave(FunctionInvocationNode node) {
			newValueCode(node);

			ParseNode exprList = node.child(1);
			ParseNode id = node.child(0);

			code.append(removeVoidCode(exprList));

			// lambda lifting
			if (node.getParent() instanceof MemberAccessNode) {
				ParseNode left = node.getParent().child(0);
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
				code.add(PushI, 4);
				code.add(Subtract); // [... sp-4]
				code.add(Duplicate); // [... sp-4, sp-4]
				code.append(removeValueCode(left)); // [... sp-4, sp-4, val]
				code.add(StoreI); // [... sp-4]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [...]
			}

			code.add(Call, id.getToken().getLexeme());

			// we back, stack is [...]
			// load the return val
			loadIFrom(code, RunTime.GLOBAL_STACK_POINTER); // [... sp]
			code.add(Duplicate); // [... sp, sp]
			int returnTypeSize = node.getType().getSize();
			switch (returnTypeSize) {
			case 1:
				code.add(LoadC); // [... sp, val]
				code.add(Exchange); // [... val, sp]
				code.add(PushI, 1);
				code.add(Add); // [... val, sp+1]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
				break;
			case 4:
				code.add(LoadI);
				code.add(Exchange); // [... val, sp]
				code.add(PushI, 4);
				code.add(Add); // [... val, sp+4]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
				break;
			case 8:
				code.add(LoadF);
				code.add(Exchange); // [... val, sp]
				code.add(PushI, 8);
				code.add(Add); // [... val, sp+8]
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [... val]
				break;
			default:
				break;
			}

			// if (node.getType() instanceof RangeType) {
			// code.add(Duplicate);
			// code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
			// }

		}

		public void visitLeave(ExpressionListNode node) {

			newVoidCode(node);

			for (ParseNode child : node.getChildren()) {

				// store arguement vlaue to location pointed by stack pointer
				loadIFrom(code, RunTime.GLOBAL_STACK_POINTER);
				code.add(PushI, child.getType().getSize());
				code.add(Subtract); // [... sp-size]
				code.add(Duplicate); // [... new-sp, new-sp]
				code.append(removeValueCode(child)); // [... new-sp, new-sp, val]

				switch (child.getType().getSize()) {
				case 1:
					code.add(StoreC);
					break;
				case 4:
					code.add(StoreI);
					break;
				case 8:
					code.add(StoreF);
					break;
				default:
					break;
				} // [... new-sp]

				// store new sp to memory
				storeITo(code, RunTime.GLOBAL_STACK_POINTER); // [...]

			}

		}

		public void visitLeave(BinaryOperatorNode node) {

			Lextant operator = node.getOperator();

			if ((operator == Punctuator.GREATER)
					|| (operator == Punctuator.GREATEREQ)
					|| (operator == Punctuator.LESS) || (operator == Punctuator.LESSEQ)
					|| (operator == Punctuator.EQUAL) || (operator == Punctuator.UNEQUAL)) {
				visitComparisonOperatorNode(node, operator);
			}
			else if ((operator == Punctuator.AND) || (operator == Punctuator.OR)) {
				visitBooleanOperator(node, operator);
			}
			else if (operator == Punctuator.OPEN_SQUARE) {
				visitRangeOpertator(node);
			}
			else {
				visitNormalBinaryOperatorNode(node);
			}
		}

		public void visitRangeOpertator(BinaryOperatorNode node) {
			newValueCode(node);

			ASMCodeFragment arg1 = getAndRemoveCode(node.child(0));
			ASMCodeFragment arg2 = getAndRemoveCode(node.child(1));
			Type childType = node.child(0).getType();

			code.add(PushI, 12 + 2 * childType.getSize());
			code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(Duplicate); // [... ptr ptr]

			String tempVariable = labeller.newLabel("$temporary-variable-", "");
			declareI(code, tempVariable);
			storeITo(code, tempVariable); // [... ptr]

			// for reference count
			code.add(Duplicate);
			code.add(PushI, 1);
			code.add(StoreI);
			// for type identifier
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Add); // [... ptr ptr+4]
			if (childType instanceof PrimitiveType) {
				code.add(PushI, 2);
			}
			else {
				code.add(PushI, 3);
			}
			code.add(StoreI);
			// for subtype size
			code.add(Duplicate); // [... ptr ptr]
			code.add(PushI, 11); // [... ptr ptr 11]
			code.add(Add); // [... ptr ptr+11]
			code.add(PushI, childType.getSize());
			code.add(StoreC); // [... ptr]

			// check reference counting
			if (arg1.isAddress()) {
				turnAddressIntoValue(arg1, node.child(0));
				code.append(arg1); // [... ptr low]

				if (node.child(0).getType() instanceof RangeType) {
					code.add(Duplicate); // [... ptr low low]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT); // [...
																																						// ptr
																																						// low]

				}

			}
			else {
				code.append(arg1); // [... ptr low]
			}

			code.add(Exchange); // [... low ptr]
			code.add(PushI, 12);
			code.add(Add); // [... low ptr+12]

			code.add(Exchange); // [... ptr+12 low]
			code.add(opcodeForStore(childType));

			loadIFrom(code, tempVariable); // [... ptr]
			code.add(Duplicate); // [... ptr ptr]
			code.add(PushI, childType.getSize() + 12);
			code.add(Add); // [... ptr ptr+size+12]

			if (arg2.isAddress()) {
				turnAddressIntoValue(arg2, node.child(1));
				code.append(arg2); // [... ptr ptr+size+12 high]

				if (node.child(1).getType() instanceof RangeType) {
					code.add(Duplicate); // [... ptr ptr+size+12 high high]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT); // [...
																																						// ptr
																																						// ptr+size+12
																																						// high]
				}
			}
			else {
				code.append(arg2); // [... ptr ptr+size+12 high]
			}

			code.add(opcodeForStore(childType));

		}

		private void visitBooleanOperator(BinaryOperatorNode node, Lextant operator) {
			newValueCode(node);

			String startLabel = labeller.newLabel("-boolean-arg1-", "");
			String arg2Label = labeller.newLabelSameNumber("-boolean-arg2-", "");
			String endLabel = labeller.newLabel("-boolean-end-", "");

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Duplicate);

			if ((Punctuator) operator == Punctuator.AND) {
				code.add(JumpFalse, endLabel);

			}
			else {
				code.add(JumpTrue, endLabel);
			}
			code.add(Pop);
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, endLabel);

		}

		private void visitComparisonOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));

			String startLabel = labeller.newLabel("-compare-arg1-", "");
			String arg2Label = labeller.newLabelSameNumber("-compare-arg2-", "");
			String subLabel = labeller.newLabelSameNumber("-compare-sub-", "");
			String trueLabel = labeller.newLabelSameNumber("-compare-true-", "");
			String falseLabel = labeller.newLabelSameNumber("-compare-false-", "");
			String joinLabel = labeller.newLabelSameNumber("-compare-join-", "");

			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);

			if (node.child(0).getType() == PrimitiveType.FLOATNUM) {
				code.add(FSubtract);

				switch ((Punctuator) operator) {
				case GREATER:
					code.add(JumpFPos, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case GREATEREQ:
					code.add(JumpFNeg, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case LESS:
					code.add(JumpFNeg, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case LESSEQ:
					code.add(JumpFPos, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case EQUAL:
					code.add(JumpFZero, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case UNEQUAL:
					code.add(JumpFZero, falseLabel);
					code.add(Jump, trueLabel);
					break;
				default:
					break;
				}
			}
			else {
				code.add(Subtract);

				switch ((Punctuator) operator) {
				case GREATER:
					code.add(JumpPos, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case GREATEREQ:
					code.add(JumpNeg, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case LESS:
					code.add(JumpNeg, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case LESSEQ:
					code.add(JumpPos, falseLabel);
					code.add(Jump, trueLabel);
					break;
				case EQUAL:
					code.add(JumpFalse, trueLabel);
					code.add(Jump, falseLabel);
					break;
				case UNEQUAL:
					code.add(JumpTrue, trueLabel);
					code.add(Jump, falseLabel);
					break;
				default:
					break;
				}
			}

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}

		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));

			code.append(arg1);
			code.append(arg2);

			code.add(Duplicate);
			if (node.getOperator() == Punctuator.DIVIDE) {
				if (node.getType() == PrimitiveType.INTEGER)
					code.add(JumpFalse, RunTime.DIVIDE_BY_ZERO);
				else
					code.add(JumpFZero, RunTime.DIVIDE_BY_ZERO);
			}
			else {
				code.add(Pop);
			}

			ASMOpcode opcode = opcodeForOperator(node.getOperator(), node.getType());
			code.add(opcode); // type-dependent!

		}

		private ASMOpcode opcodeForOperator(Lextant lextant, Type type) {
			assert (lextant instanceof Punctuator);
			Punctuator punctuator = (Punctuator) lextant;
			switch (punctuator) {
			case ADD:
				if (type == PrimitiveType.INTEGER)
					return Add; // type-dependent!
				else if (type == PrimitiveType.FLOATNUM)
					return FAdd;
			case MINUS:
				if (type == PrimitiveType.INTEGER)
					return Subtract; // type-dependent!
				else if (type == PrimitiveType.FLOATNUM)
					return FSubtract;
			case MULTIPLY:
				if (type == PrimitiveType.INTEGER)
					return Multiply; // type-dependent!
				else if (type == PrimitiveType.FLOATNUM)
					return FMultiply;
			case DIVIDE:
				if (type == PrimitiveType.INTEGER)
					return Divide; // type-dependent!
				else if (type == PrimitiveType.FLOATNUM)
					return FDivide;

			default:
				assert false : "unimplemented operator in opcodeForOperator";
			}
			return null;
		}

		// /////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}

		public void visit(CharacterNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue());
		}

		public void visit(IdentifierNode node) {
			if (!(node.getToken().getLexeme().equals("low"))
					&& !(node.getToken().getLexeme().equals("high"))) {
				newAddressCode(node);
				Binding binding = node.getBinding();

				binding.generateAddress(code);
			}
		}

		public void visit(IntNumberNode node) {
			newValueCode(node);

			code.add(PushI, node.getValue());
		}

		public void visit(FloatNumberNode node) {
			newValueCode(node);

			code.add(PushF, node.getValue());
		}
	}
}
