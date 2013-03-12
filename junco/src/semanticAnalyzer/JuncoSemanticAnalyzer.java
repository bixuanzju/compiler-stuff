package semanticAnalyzer;

import java.util.Arrays;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.JuncoLogger;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.FunctionDeclNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
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

		if (logging.JuncoLogger.hasErrors()) {
			return ASTree;
		}

		ASTree = iterateRewriting(ASTree);
		ASTree.accept(new SemanticAnalysisVisitor());

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

	private class FunctionVistor extends ParseNodeVisitor.Default {

		public void visitEnter(ProgramNode node) {
			// Scopes.enterStaticScope(node);
			enterGlobleScope(node);
		}

		public void visitEnter(FunctionDeclNode node) {

			IdentifierNode name = (IdentifierNode) node.child(0);
			ParameterListNode parameterList = (ParameterListNode) node.child(1);
			// binding function name
			Scope scope = node.getLocalScope();
			Binding binding = scope.createBinding(name, node.getType());
			name.setBinding(binding);

			enterParameterScope(node);

			for (ParseNode child : parameterList.getChildren()) {
				addBinding((IdentifierNode) child, child.getType());
			}

			int sizeOfParameters = node.getScope().getAllocatedSize();

			for (ParseNode child : parameterList.getChildren()) {
				((IdentifierNode) child).getBinding().getMemoryLocation()
						.resetOffset(sizeOfParameters);
			}

			//
			// for (ParseNode child : parameterList.getChildren()) {
			// System.out.println(((IdentifierNode)child).getBinding().getMemoryLocation().getOffset());
			// }
			//

		}

		private void addBinding(IdentifierNode identifierNode, Type type) {
			Scope scope = identifierNode.getLocalScope();
			Binding binding = scope.createBinding(identifierNode, type);
			identifierNode.setBinding(binding);
		}

	}

	private class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
		@Override
		public void visitLeave(ParseNode node) {
			throw new RuntimeException(
					"Node class unimplemented in SemanticAnalysisVisitor: "
							+ node.getClass());
		}

		// /////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		@Override
		public void visitLeave(ProgramNode node) {
			// Scopes.leaveScope();
		}

		public void visitLeave(BoxBodyNode node) {
			// Scopes.leaveScope();
		}

		public void visitEnter(BoxBodyNode node) {
			// Scopes.enterStaticScope(node);
			enterSubscope(node);
		}

		public void visitLeave(BodyNode node) {
			// Scopes.leaveScope();
		}

		public void visitEnter(BodyNode node) {
			// Scopes.enterStaticScope(node);
			enterSubscope(node);
		}

		public void visitLeave(FunctionDeclNode node) {
			
			
		}

		public void visitLeave(FunctionInvocationNode node) {
			node.setType(node.child(0).getType());
		}

		public void visitEnter(ValueBodyNode node) {
			// Scopes.enterStaticScope(node);
			if (node.getParent() instanceof FunctionDeclNode) {
				enterProcedureScope(node);
			}
			else {
				enterSubscope(node);
			}

		}

		public void visitLeave(ValueBodyNode node) {

			TypeVariable returnType = new TypeVariable();
			returnType.resetType();

			for (ParseNode child : node.getChildren()) {
				if (child instanceof ReturnStatementNode) {
					returnType.constrain(child.getType());
				}
				else if ((child instanceof IfStatementNode)
						|| (child instanceof WhileStatementNode)) {
					checkReturn(returnType, child);

				}
			}

			if (returnType.getConstraintType() instanceof NoneType) {
				logError("All return statements must return the same type");
			}
			else if (returnType.getConstraintType() instanceof AnyType) {
				logError("Value body must have at least one return statement");
			}

			node.setType(node.child(node.nChildren() - 1).getType());
			// Scopes.leaveScope();

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
		@Override
		public void visitLeave(PrintStatementNode node) {
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

		public void visitLeave(WhileStatementNode node) {
			Token token = node.getToken();
			if (node.child(0).getType() != PrimitiveType.BOOLEAN) {
				logError("not boolean expression at " + token.getLocation());
			}
		}

		public void visitLeave(IfStatementNode node) {
			Token token = node.getToken();
			if (node.child(0).getType() != PrimitiveType.BOOLEAN) {
				logError("not boolean expression at " + token.getLocation());
			}
		}

		public void visitLeave(ReturnStatementNode node) {
			node.setType(node.child(0).getType());

		}

		// /////////////////////////////////////////////////////////////////////////
		// expressions
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
			if (!isBeingDeclared(node) && !isParameter(node)) {
				Binding binding = node.findVariableBinding();
				node.setType(binding.getType());
				node.setBinding(binding);
				// System.out.println(node.getBinding().getMemoryLocation().getOffset());
			}
			// else parent DeclarationNode does the processing.
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
