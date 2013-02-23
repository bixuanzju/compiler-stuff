package asmCodeGenerator;

import static asmCodeGenerator.ASMOpcode.*;
import java.util.HashMap;
import java.util.Map;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.UniaryOperatorNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.WhileStatementNode;
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
		code.append(programCode());
		code.add(Halt);

		code.add(Label, RunTime.DIVIDE_BY_ZERO);
		// print error message
		code.add(PushD, RunTime.ERROR_MESSAGE_IF_DIVIDE_BY_ZERO);
		code.add(Printf);
		code.add(Halt);

		code.add(Label, RunTime.GENERAL_RUNTIME_ERROR);
		code.add(Printf);
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
			else if (node.getType() instanceof RangeType) {
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
				code.append(childCode);
			}

			for (ParseNode child : node.getChildren()) {
				if (child.getType() instanceof DeclarationNode) {
					if (child.child(0).getType() instanceof RangeType) {
						ParseNode rangeVariable = child.child(0);
						code.append(removeValueCode(rangeVariable));
						code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
					}
				}
			}

			code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
		}

		public void visitLeave(BoxBodyNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
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
				String startlabel = labeller.newLabel("-range-start-", "");
				String falselabel = labeller
						.newLabelSameNumber("-range-false-end-", "");
				String notfloatlabel = labeller.newLabelSameNumber(
						"-range-notfloat-end-", "");
				String charlabel = labeller.newLabelSameNumber("-range-char-end-", "");
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
				code.add(Duplicate);
				code.add(PushI, 11); // [... ptr ptr 11]
				code.add(Add); // [... ptr ptr+11]
				code.add(LoadC); // [... ptr size]
				code.add(Duplicate); // [... ptr size size]
				code.add(PushI, 8); // [... ptr size size 8]
				code.add(Subtract); // [.. ptr size size-8]
				code.add(JumpNeg, notfloatlabel); // [... ptr size]
				// yeah, it's float type, let's print
				code.add(Pop); // [... ptr]
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
				code.add(PushD, RunTime.CLOSE_SQUARE_STRING);
				code.add(Printf);
				code.add(Return);

				// ok, it's not float, so it is int or char?
				code.add(Label, notfloatlabel);
				// code.add(Duplicate); // [... ptr size size]
				code.add(PushI, 4); // [... ptr size 4]
				code.add(Subtract); // [... ptr size-4]
				code.add(JumpNeg, charlabel); // [... ptr]
				// it's int type, let's print
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

				code.add(PushD, RunTime.CLOSE_SQUARE_STRING);
				code.add(Printf);
				code.add(Return);

				// finally, it is char type, let's print!!
				code.add(Label, charlabel);
				code.add(Duplicate); // [...ptr ptr]
				code.add(PushI, 12); // [... ptr ptr 12]
				code.add(Add); // [... ptr ptr+12]
				code.add(LoadC); // [... ptr char]
				code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
				// code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);

				code.add(Printf); // [... ptr]
				code.add(PushD, RunTime.SPLICE_STRING);
				code.add(Printf); // [... ptr]
				code.add(PushI, 13); // [... ptr 13]
				code.add(Add); // [... ptr+13]
				code.add(LoadC); // [... char]
				code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
				// code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
				code.add(Printf);

				code.add(PushD, RunTime.CLOSE_SQUARE_STRING);
				code.add(Printf);
				code.add(Return);

				code.add(Label, endlabel);
			}
			else {
				String format = printFormat(node.getType());
				code.append(removeValueCode(node));
				convertToStringIfBoolean(node);
				code.add(PushD, format);
				code.add(Printf);
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
				if (node.child(1).getType() instanceof RangeType) {

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

			// code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT);

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

			code.add(Label, endlabel);

		}

		public void visitLeave(IfStatementNode node) {
			newVoidCode(node);
			String startlabel = labeller.newLabel("-if-start-", "");
			String endlabel = labeller.newLabelSameNumber("-if-end-", "");
			String elselabel = labeller.newLabelSameNumber("-if-else-", "");

			code.add(Label, startlabel);
			ASMCodeFragment expr = removeValueCode(node.child(0));
			code.append(expr);
			if (node.hasElse()) {
				code.add(JumpFalse, elselabel);
			}
			else {
				code.add(JumpFalse, endlabel);
			}

			ASMCodeFragment ifBody = removeVoidCode(node.child(1));
			code.append(ifBody);
			code.add(Jump, endlabel);
			if (node.hasElse()) {
				code.add(Label, elselabel);
				ASMCodeFragment elseBody = removeVoidCode(node.child(2));
				code.append(elseBody);
				code.add(Jump, endlabel);
			}
			code.add(Label, endlabel);
		}

		public void visitLeave(BodyNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}

			for (ParseNode child : node.getChildren()) {
				if (child.getType() instanceof DeclarationNode) {
					if (child.child(0).getType() instanceof RangeType) {
						ParseNode rangeVariable = child.child(0);
						code.append(removeValueCode(rangeVariable));
						code.add(Call, ReferenceCounting.REF_COUNTER_PUSH_RECORD);
					}
				}
			}

			code.add(Call, ReferenceCounting.REF_COUNTER_PERFORM_DECREMENTS);
		}

		public void visitLeave(UniaryOperatorNode node) {

			if (node.getToken().isLextant(Punctuator.LOW, Punctuator.HIGH)) {
				newAddressCode(node);
			}
			else {
				newValueCode(node);
			}

			//newValueCode(node);
			ASMCodeFragment value = removeValueCode(node.child(0));
			code.append(value);

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
			else if (node.getToken().isLextant(Punctuator.LOW)) {

				code.add(PushI, 12);
				code.add(Add);

			}
			else if (node.getToken().isLextant(Punctuator.HIGH)) {

				code.add(PushI, 12 + ((RangeType) node.child(0).getType())
						.getChildType().getSize());
				code.add(Add);

			}

		}

		public void visitLeave(UpdateStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			code.add(opcodeForStore(type));
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
			if (type instanceof RangeType) {
				return StoreI;
			}

			assert false : "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}

		// /////////////////////////////////////////////////////////////////////////
		// expressions
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
			code.add(Duplicate);	// [... ptr ptr]
			
			String tempVariable = labeller.newLabel("$temporary-variable-", "");
			declareI(code, tempVariable);
			storeITo(code, tempVariable); 	// [... ptr]
			
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
				code.append(arg1);	// [... ptr low]
				
				if (node.child(0).getType() instanceof RangeType) {
					code.add(Duplicate);	// [... ptr low low]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT);	// [... ptr low]
				}
				
			}
			else {
				code.append(arg1);	// [... ptr low]
			}

			code.add(Exchange); 	// [... low ptr]
			code.add(PushI, 12);
			code.add(Add); // [... low ptr+12]
			
			code.add(Exchange);	// [... ptr+12 low]
			code.add(opcodeForStore(childType));

			loadIFrom(code, tempVariable);	// [... ptr]
			code.add(Duplicate); // [... ptr ptr]
			code.add(PushI, childType.getSize() + 12);
			code.add(Add);	// [... ptr ptr+size+12]
			
			if (arg2.isAddress()) {
				turnAddressIntoValue(arg2, node.child(1));
				code.append(arg2);	// [... ptr ptr+size+12 high]
				
				if (node.child(1).getType() instanceof RangeType) {
					code.add(Duplicate);	// [... ptr ptr+size+12 high high]
					code.add(Call, ReferenceCounting.REF_COUNTER_INCREMENT_REFCOUNT);	// [... ptr ptr+size+12 high]
				}
			}
			else {
				code.append(arg2);	// [... ptr ptr+size+12 high]
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
			newAddressCode(node);
			Binding binding = node.getBinding();

			binding.generateAddress(code);
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
