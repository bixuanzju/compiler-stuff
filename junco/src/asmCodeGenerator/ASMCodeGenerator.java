package asmCodeGenerator;

import static asmCodeGenerator.ASMOpcode.*;
import java.util.HashMap;
import java.util.Map;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CastingNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.UpdateStatementNode;
import semanticAnalyzer.PrimitiveType;
import semanticAnalyzer.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.ASMCodeFragment.CodeType.*;

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

		code.append(RunTime.getEnvironment());
		code.append(globalVariableBlockASM());
		code.append(programASM());
		// code.append( MemoryManager.codeForAfterApplication() );

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
		code.add(Jump, RunTime.NOT_DIVIDE_BY_ZERO);
		
		code.add(Label, RunTime.DIVIDE_BY_ZERO);
		// print error message
		code.add(PushD, RunTime.ERROR_MESSAGE_IF_DIVIDE_BY_ZERO);
		code.add(Printf);
		
		code.add(Label, RunTime.NOT_DIVIDE_BY_ZERO);
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
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}

		// //////////////////////////////////////////////////////////////////
		// ensures all types of ParseNode in given AST have at least a visitLeave
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
			String format = printFormat(node.getType());

			code.append(removeValueCode(node));
			convertToStringIfBoolean(node);
			code.add(PushD, format);
			code.add(Printf);
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
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			code.add(opcodeForStore(type));
		}
		
		public void visitLeave(CastingNode node) {
			newVoidCode(node);
			ASMCodeFragment value = null;
			if (node.child(0) instanceof IdentifierNode) {
				value = removeAddressCode(node.child(0));
			}
			else value = removeValueCode(node.child(0));
			

			code.append(value);
			if (node.getToken().isLextant(Punctuator.CASTTOFLAOT)) {
				code.add(ConvertF);
			}
			else if (node.getToken().isLextant(Punctuator.CASTTOINT)) {
				code.add(ConvertI);
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
			else {
				visitNormalBinaryOperatorNode(node);
			}
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
			if (node.getType() == PrimitiveType.INTEGER)
				code.add(JumpFalse, RunTime.DIVIDE_BY_ZERO);
			else code.add(JumpFZero, RunTime.DIVIDE_BY_ZERO);
			
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
