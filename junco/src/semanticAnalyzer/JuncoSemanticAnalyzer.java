package semanticAnalyzer;

import java.util.Arrays;
import java.util.List;
import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.JuncoLogger;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.FunctionDeclNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.MemberAccessNode;
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.UniaryOperatorNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.ValueBodyNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.rewriters.ASTRewriter;
import semanticAnalyzer.rewriters.RangeOperatorRewritingVisitor;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

public class JuncoSemanticAnalyzer {
	ParseNode ASTree;

	public static ParseNode analyze(ParseNode ASTree) {
		JuncoSemanticAnalyzer analyzer = new JuncoSemanticAnalyzer(ASTree);
		return analyzer.analyze();
	}

	public JuncoSemanticAnalyzer(ParseNode ASTree) {
		this.ASTree = ASTree;
	}

	public ParseNode analyze() {

		ASTree.accept(new FunctionVistor());

		ASTree.accept(new SemanticAnalysisVisitor());

		ASTree.getScope().resetAllocatedSize();

		if (logging.JuncoLogger.hasErrors()) {
			return ASTree;
		}

		ASTree = iterateRewriting(ASTree);
		// System.out.println(ASTree.getScope().getAllocatedSize());
		ASTree.accept(new SemanticAnalysisVisitor());
		// System.out.println(ASTree.getScope().getAllocatedSize());

		return ASTree;
	}

	private ParseNode iterateRewriting(ParseNode inputTree) {
		ASTRewriter rewriter = null;
		ParseNode tree = inputTree;

		do {
			rewriter = new RangeOperatorRewritingVisitor();
			tree = rewriter.rewrite(tree);
		}
		while (rewriter.changesWereMade());

		return tree;
	}

	private static void enterGlobleScope(ParseNode node) {
		Scope scope = Scope.createGlobalScope();
		node.setScope(scope);
	}

	private static void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}

	private static void enterProcedureScope(ParseNode node) {
		Scope scope = Scope.createProcedureScope();
		node.setScope(scope);
	}

	private static void enterParameterScope(ParseNode node) {
		Scope scope = Scope.createParameterScope();
		node.setScope(scope);
	}

	private static void enterBoxBodyScope(ParseNode node) {
		Scope scope = Scope.createBoxBodyScope();
		node.setScope(scope);
	}

	private class FunctionVistor extends ParseNodeVisitor.Default {
		private int boxTypeIdentifier = 128;

		public void visitEnter(ProgramNode node) {
			// Scopes.enterStaticScope(node);
			enterGlobleScope(node);
		}

		public void visitEnter(BoxBodyNode node) {
			IdentifierNode boxName = new IdentifierNode(IdentifierToken.make(node
					.getToken().getLocation(), node.getToken().getLexeme()));
			Scope scope = node.getParent().getScope();
			BoxType type = new BoxType(node.getToken().getLexeme(),
					boxTypeIdentifier++);
			scope.createBinding(boxName, type);
			node.setType(type);
			node.setBoxName(node.getToken().getLexeme());
		}

		public void visitEnter(FunctionDeclNode node) {
			node.setBoxName(node.getParent().returnBoxName());

			IdentifierNode name = (IdentifierNode) node.child(0);
			name.getToken().setLexeme(node.returnBoxName());

			ParameterListNode parameterList = (ParameterListNode) node.child(1);

			FunctionType funcType = new FunctionType();

			for (ParseNode child : parameterList.getChildren()) {
				funcType.appendType(child.getType());
			}

			funcType.appendType(node.getType());

			addBinding(name, funcType);

			enterParameterScope(node);

			for (ParseNode child : parameterList.getChildren()) {
				addBinding((IdentifierNode) child, child.getType());
			}

			int sizeOfParameters = node.getScope().getAllocatedSize() + 4;

			for (ParseNode child : parameterList.getChildren()) {
				((IdentifierNode) child).getBinding().getMemoryLocation()
						.resetOffset(sizeOfParameters);
			}

		}

		private void addBinding(IdentifierNode identifierNode, Type type) {
			Scope scope = identifierNode.getLocalScope();
			Binding binding = scope.createBinding(identifierNode, type);
			identifierNode.setBinding(binding);
		}

	}

	private class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {

		private Labeller labeller = new Labeller();

		@Override
		public void visitLeave(ParseNode node) {
			throw new RuntimeException(
					"Node class unimplemented in SemanticAnalysisVisitor: "
							+ node.getClass());
		}

		// /////////////////////////////////////////////////////////////////////////
		// constructs larger than statements

		public void visitEnter(BoxBodyNode node) {
			if (node.getToken().getLexeme().equals("main")) {
				enterSubscope(node);
				IdentifierNode thisPtr = new IdentifierNode(IdentifierToken.make(node
						.getToken().getLocation(), Keyword.THIS.getLexeme()));
				Scope scope = node.getScope();
				scope.createBinding(thisPtr, node.getType());
			}
			else {
				enterBoxBodyScope(node);
				IdentifierNode thisPtr = new IdentifierNode(IdentifierToken.make(node
						.getToken().getLocation(), Keyword.THIS.getLexeme()));
				Scope scope = node.getScope();
				scope.createBinding(thisPtr, node.getType());
			}

		}

		public void visitLeave(BoxBodyNode node) {
			BoxType type = (BoxType) node.getType();
			type.setScopeSize(node.getScope().getAllocatedSize());
		}

		public void visitLeave(ProgramNode node) {
			if (!node.getScope().getSymbolTable().containsKey("main")) {
				logError("no main box detected");
			}

		}

		public void visitEnter(BodyNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
			enterSubscope(node);
		}

		public void visitLeave(FunctionDeclNode node) {
			if (!(node.getParent() instanceof BoxBodyNode)) {
				logError("no function declaration allowed at "
						+ node.getToken().getLocation());
			}

			TypeVariable returnType = ((ValueBodyNode) node.child(2)).getReturnType();

			returnType.constrain(node.getType());

			if (returnType.getConstraintType() instanceof NoneType) {
				logError("function signature doesn't match return type at"
						+ node.getToken().getLocation());
			}

		}

		public void visitEnter(CallStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(CallStatementNode node) {
			ParseNode child = node.child(0);
			
			if (child instanceof MemberAccessNode) {
				if (!(child.child(0).getType() instanceof BoxType)) {
					logError("need function invocation at " + node.getToken().getLocation());
				}
			}
			else if (!(child instanceof FunctionInvocationNode)) {
				logError("need function invocation at " + node.getToken().getLocation());
			}
		}

		public void visitEnter(FunctionInvocationNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			ParseNode child = node.child(0);

			if (node.getParent() instanceof MemberAccessNode) {
				if (node.getParent().child(0).getType() instanceof BoxType) {
					BoxType type = (BoxType) node.getParent().child(0).getType();
					child.getToken().setLexeme(type.getBoxName());
				}
			}
			else {
				child.getToken().setLexeme(node.returnBoxName());
			}
		}

		public void visitLeave(FunctionInvocationNode node) {

			if (node.child(0).getType() instanceof FunctionType) {
				List<Type> typeList = ((FunctionType) node.child(0).getType())
						.getList();

				Type returnType = ((FunctionType) node.child(0).getType())
						.getReturnType();
				node.setType(returnType);

				List<ParseNode> parameterList = node.child(1).getChildren();
				if (parameterList.size() == typeList.size()) {
					for (int i = 0; i < parameterList.size() - 1; i++) {
						if (!parameterList.get(i).getType().infoString()
								.equals(typeList.get(i).infoString())) {
							logError("parameter doesn't match function declaration at "
									+ node.getToken().getLocation());
							node.setType(PrimitiveType.ERROR);
							break;
						}

					}
				}
				else {
					logError("parameter size doesn't match at "
							+ node.getToken().getLocation());
					node.setType(PrimitiveType.ERROR);
				}
			}
			else {
				logError("no valid function name at " + node.getToken().getLocation());
				node.setType(PrimitiveType.ERROR);
			}
		}

		public void visitEnter(ValueBodyNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			String returnlabel = labeller.newLabel("value-body-start", "");
			node.setReturnLabel(returnlabel);

			if (node.getParent() instanceof FunctionDeclNode) {
				enterProcedureScope(node);
			}
			else {
				enterSubscope(node);
			}

		}

		public void visitLeave(ValueBodyNode node) {

			node.getReturnType().resetType();

			for (ParseNode child : node.getChildren()) {
				if (child instanceof ReturnStatementNode) {
					node.getReturnType().constrain(child.getType());
				}
				else if ((child instanceof IfStatementNode)
						|| (child instanceof WhileStatementNode)) {
					checkReturn(node.getReturnType(), child);

				}
			}

			if (node.getReturnType().getConstraintType() instanceof NoneType) {
				logError("All return statements must return the same type");
			}
			else if (node.getReturnType().getConstraintType() instanceof AnyType) {
				logError("Value body must have at least one return statement");
			}
			else if (!(node.child(node.nChildren() - 1) instanceof ReturnStatementNode)) {
				logError("The last statement of value body must be return statememt");
			}

			node.setType(node.child(node.nChildren() - 1).getType());

		}

		private void checkReturn(TypeVariable type, ParseNode node) {

			ParseNode thenBody = node.child(1);

			for (ParseNode child : thenBody.getChildren()) {
				if (child instanceof ReturnStatementNode) {
					type.constrain(child.getType());
				}
				else if (child instanceof IfStatementNode) {
					checkReturn(type, child);
				}
			}

			if (node.nChildren() == 3) {
				// ParseNode thenBody = node.child(0);
				ParseNode elseBody = node.child(2);

				for (ParseNode child : elseBody.getChildren()) {
					if (child instanceof ReturnStatementNode) {
						type.constrain(child.getType());
					}
					else if (child instanceof IfStatementNode) {
						checkReturn(type, child);
					}
				}

			}
		}

		// /////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitEnter(PrintStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitEnter(DeclarationNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		@Override
		public void visitLeave(DeclarationNode node) {
			IdentifierNode identifier = (IdentifierNode) node.child(0);
			ParseNode initializer = node.child(1);

			Type declarationType = initializer.getType();
			node.setType(declarationType);

			identifier.setType(declarationType);
			addBinding(identifier, declarationType);
		}

		public void visitEnter(WhileStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(WhileStatementNode node) {
			Token token = node.getToken();
			if (node.child(0).getType() != PrimitiveType.BOOLEAN) {
				logError("not boolean expression at " + token.getLocation());
			}
		}

		public void visitEnter(IfStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(IfStatementNode node) {
			Token token = node.getToken();
			if (node.child(0).getType() != PrimitiveType.BOOLEAN) {
				logError("not boolean expression at " + token.getLocation());
			}
		}

		public void visitEnter(ReturnStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(ReturnStatementNode node) {

			if (node.getReturnLabel() == null) {
				logError("return statement must be in the value body at "
						+ node.getToken().getLocation());
			}
			node.setType(node.child(0).getType());

		}

		// /////////////////////////////////////////////////////////////////////////
		// expressions

		public void visitEnter(BinaryOperatorNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		@Override
		public void visitLeave(BinaryOperatorNode node) {
			assert node.nChildren() == 2;
			ParseNode left = node.child(0);
			ParseNode right = node.child(1);
			Token token = node.getToken();

			Lextant operator = operatorFor(node);

			if (operator == Punctuator.OPEN_SQUARE) {
				if (left.getType().infoString().equals(right.getType().infoString())) {
					node.setType(new RangeType(left.getType()));
				}
				else {
					logError("the low end and the high end must have the same type, at "
							+ token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
			}
			else {

				FunctionSignature signature = FunctionSignature.signatureOf(operator);

				if (signature.accepts(operator, left.getType(), right.getType())) {
					node.setType(signature.resultType().getConstraintType());
				}
				else {
					typeCheckError(node, left.getType(), right.getType());
					node.setType(PrimitiveType.ERROR);
				}

			}
		}

		private Lextant operatorFor(BinaryOperatorNode node) {
			LextantToken token = (LextantToken) node.getToken();
			return token.getLextant();
		}

		public void visitEnter(MemberAccessNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(MemberAccessNode node) {
			ParseNode left = node.child(0);
			ParseNode right = node.child(1);

			if (left.getType() instanceof BoxType) {
				if (!(right instanceof FunctionInvocationNode)) {
					logError("not function invocation at "
							+ node.getToken().getLocation());
				}
				else {
					node.setType(right.getType());
				}
			}
			else if (left.getType() instanceof RangeType) {

			}
			else {
				logError("member access not appled at " + node.getToken().getLocation());
			}
		}

		public void visitEnter(UniaryOperatorNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		public void visitLeave(UniaryOperatorNode node) {
			ParseNode child = node.child(0);
			Token token = node.getToken();

			if (token.isLextant(Punctuator.NOT)) {
				if (child.getType() != PrimitiveType.BOOLEAN) {
					logError("must be boolean at " + token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else
					node.setType(PrimitiveType.BOOLEAN);
			}
			else if (token.isLextant(Punctuator.CASTTOBOOL)) {
				logError("cannot convert to boolean at " + token.getLocation());
				node.setType(PrimitiveType.BOOLEAN);
			}
			else if (token.isLextant(Punctuator.CASTTOCHAR)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.FLOATNUM
						|| child.getType() == PrimitiveType.CHARACTER) {
					logError("cannot convert to character at " + token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else
					node.setType(PrimitiveType.CHARACTER);
			}
			else if (token.isLextant(Punctuator.CASTTOFLAOT)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.CHARACTER
						|| child.getType() == PrimitiveType.FLOATNUM) {
					logError("cannot convert to floating at " + token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else
					node.setType(PrimitiveType.FLOATNUM);
			}
			else if (token.isLextant(Punctuator.CASTTOINT)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.INTEGER) {
					logError("cannot convert to integer type at " + token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else
					node.setType(PrimitiveType.INTEGER);
			}
			else if (token.isLextant(Punctuator.LOW, Punctuator.HIGH,
					Punctuator.EMPTY)) {
				if (!(child.getType() instanceof RangeType)) {
					logError("must be range type at " + token.getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else if (token.isLextant(Punctuator.EMPTY)) {
					node.setType(PrimitiveType.BOOLEAN);
				}
				else {
					node.setType(((RangeType) child.getType()).getChildType());
				}

			}
			else if (token.isLextant(Punctuator.AT)) {
				if (!(child instanceof IdentifierNode)) {
					logError("@boxNameIdentifier at " + node.getToken().getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else if (child.getToken().getLexeme().equals("main")) {
					logError("main box cannot be initialized at "
							+ node.getToken().getLocation());
					node.setType(PrimitiveType.ERROR);
				}
				else {
					IdentifierNode box = (IdentifierNode) child;
					Binding binding = box.findGlobalBinding();
					box.setType(binding.getType());
					box.setBinding(binding);
					node.setType(child.getType());
				}

			}
		}

		// /////////////////////////////////////////////////////////////////////////
		// simple leaf nodes
		@Override
		public void visit(BooleanConstantNode node) {
			node.setType(PrimitiveType.BOOLEAN);
		}

		@Override
		public void visit(FloatNumberNode node) {
			node.setType(PrimitiveType.FLOATNUM);
		}

		@Override
		public void visit(CharacterNode node) {
			node.setType(PrimitiveType.CHARACTER);
		}

		@Override
		public void visit(ErrorNode node) {
			node.setType(PrimitiveType.ERROR);
		}

		@Override
		public void visit(IntNumberNode node) {
			node.setType(PrimitiveType.INTEGER);
		}

		public void visitEnter(UpdateStatementNode node) {
			node.setBoxName(node.getParent().returnBoxName());
			node.setReturnLabel(node.getParent().getReturnLabel());
		}

		@Override
		public void visitLeave(UpdateStatementNode node) {
			ParseNode target = node.child(0);
			ParseNode updateValue = node.child(1);

			if (!target.getType().infoString()
					.equals(updateValue.getType().infoString())) {
				if (updateValue.getType() instanceof RangeType) {
					logError("identifier " + node.child(0).getToken().getLexeme()
							+ " cannot be assigned " + updateValue.getType().infoString()
							+ " value " + " at " + node.getToken().getLocation());
				}
				else
					typeCheckError(node, updateValue.getType());
				node.setType(PrimitiveType.ERROR);
			}
			else if (target instanceof IdentifierNode
					&& !findDeclaredNode(((IdentifierNode) target).findScopeNode(),
							target).getParent().getToken().isLextant(Keyword.INIT)) {
				logError("immutable identifier " + target.getToken().getLexeme()
						+ " cannot change value " + " at "
						+ target.getToken().getLocation());
				node.setType(PrimitiveType.ERROR);
			}
			else
				node.setType(target.getType());

		}

		private ParseNode findDeclaredNode(ParseNode node, ParseNode target) {
			ParseNode want = null;
			if (node.getChildren() != null)
				for (ParseNode iterator : node.getChildren())
					if (iterator.getToken().getLexeme() != target.getToken().getLexeme()) {
						want = findDeclaredNode(iterator, target);
						if (want != null)
							break;
					}
					else {
						want = iterator;
						break;
					}

			return want;

		}

		// /////////////////////////////////////////////////////////////////////////
		// IdentifierNodes, with helper methods
		@Override
		public void visit(IdentifierNode node) {
			if (!isBeingDeclared(node) && !isParameter(node) && !isBoxname(node)) {
				Binding binding = node.findVariableBinding();
				node.setType(binding.getType());
				node.setBinding(binding);
			}
		}

		private boolean isBoxname(ParseNode node) {
			ParseNode parent = node.getParent();
			return parent.getToken().isLextant(Punctuator.AT);
		}

		private boolean isParameter(ParseNode node) {
			ParseNode parent = node.getParent();
			return (parent instanceof ParameterListNode);
		}

		private boolean isBeingDeclared(IdentifierNode node) {
			ParseNode parent = node.getParent();
			return (parent instanceof DeclarationNode) && (node == parent.child(0));
		}

		private void addBinding(IdentifierNode identifierNode, Type type) {
			Scope scope = identifierNode.getLocalScope();
			Binding binding = scope.createBinding(identifierNode, type);
			identifierNode.setBinding(binding);
		}

		// /////////////////////////////////////////////////////////////////////////
		// error logging/printing

		private void typeCheckError(ParseNode node, Type... operandTypes) {
			Token token = node.getToken();
			if (token.isLextant(Keyword.UPDATE))
				logError("identifier " + node.child(0).getToken().getLexeme()
						+ " cannot be assigned " + Arrays.toString(operandTypes)
						+ " value " + " at " + token.getLocation());
			else
				logError("operator " + token.getLexeme() + " not defined for types "
						+ Arrays.toString(operandTypes) + " at " + token.getLocation());
		}

		private void logError(String message) {
			JuncoLogger log = JuncoLogger.getLogger("compiler.semanticAnalyzer");
			log.severe(message);
		}
	}
}
