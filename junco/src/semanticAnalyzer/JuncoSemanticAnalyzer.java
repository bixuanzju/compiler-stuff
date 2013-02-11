package semanticAnalyzer;

import java.util.Arrays;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.JuncoLogger;

import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BooleanNotNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CastingNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.WhileStatementNode;
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
		ParseNodeVisitor visitor = new SemanticAnalysisVisitor();
		ASTree.accept(visitor);
		return ASTree;
	}
	
	
	private class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
		@Override
		public void visitLeave(ParseNode node) {
			throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
		}
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		@Override
		public void visitEnter(ProgramNode node) {
			Scopes.enterStaticScope(node);				//TODO that's where scope is
		}
		public void visitLeave(ProgramNode node) {
			Scopes.leaveScope();
		}
		public void visitEnter(BoxBodyNode node) {
		}
		public void visitLeave(BoxBodyNode node) {
		}

		///////////////////////////////////////////////////////////////////////////
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
		}
		
		public void visitLeave(IfStatementNode node) {
		}
		

		///////////////////////////////////////////////////////////////////////////
		// expressions
		@Override
		public void visitLeave(BinaryOperatorNode node) {
			assert node.nChildren() == 2;
			ParseNode left  = node.child(0);
			ParseNode right = node.child(1);
			
			Lextant operator = operatorFor(node);
			FunctionSignature signature = FunctionSignature.signatureOf(operator, left);
			
			if(signature.accepts(left.getType(), right.getType())) {
				node.setType(signature.resultType());
			}
			else {
				typeCheckError(node, left.getType(), right.getType());
				node.setType(PrimitiveType.ERROR);
			}
		}
		private Lextant operatorFor(BinaryOperatorNode node) {
			LextantToken token = (LextantToken) node.getToken();
			return token.getLextant();
		}
		
		public void visitLeave(BooleanNotNode node) {
			ParseNode child = node.child(0);
			if (child.getType() != PrimitiveType.BOOLEAN) {
				logError("must be boolean type");
			}
			node.setType(PrimitiveType.BOOLEAN);
		}

		public void visitLeave(CastingNode node) {
			ParseNode child = node.child(0);
			
			if (node.getToken().isLextant(Punctuator.CASTTOBOOL)) {
				logError("cannot convert to boolean type");
				node.setType(PrimitiveType.BOOLEAN);
			}
			else if (node.getToken().isLextant(Punctuator.CASTTOCHAR)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.FLOATNUM
						|| child.getType() == PrimitiveType.CHARACTER) {
					logError("cannot convert to character type");
				}
				node.setType(PrimitiveType.CHARACTER);

			}
			else if (node.getToken().isLextant(Punctuator.CASTTOINT)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.INTEGER) {
					logError("cannot convert to integer type");
				}
				node.setType(PrimitiveType.INTEGER);
			}
			else if (node.getToken().isLextant(Punctuator.CASTTOFLAOT)) {
				if (child.getType() == PrimitiveType.BOOLEAN
						|| child.getType() == PrimitiveType.CHARACTER
						|| child.getType() == PrimitiveType.FLOATNUM) {
					logError("cannot convert to floating type");
				}
				node.setType(PrimitiveType.FLOATNUM);
			}
		}

		///////////////////////////////////////////////////////////////////////////
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
			IdentifierNode target = (IdentifierNode) node.child(0);
			ParseNode updateValue = node.child(1);

			// if (!isBeingDeclared(target)) {
			// Binding binding = target.findVariableBinding();
			// target.setType(binding.getType());
			// node.setType(binding.getType());

			node.setType(target.getType());
			if (target.getType() != updateValue.getType())
				typeCheckError(node, updateValue.getType());

			if (!findDeclaredNode(target.findScopeNode(), target).getParent()
					.getToken().isLextant(Keyword.INIT))
				logError("immutable identifier " + target.getToken().getLexeme()
						+ " cannot change value " + " at "
						+ target.getToken().getLocation());

			// }

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
		///////////////////////////////////////////////////////////////////////////
		// IdentifierNodes, with helper methods
		@Override
		public void visit(IdentifierNode node) {
			if(!isBeingDeclared(node)) {		
				Binding binding = node.findVariableBinding();
				node.setType(binding.getType());
				node.setBinding(binding);
			}
			// else parent DeclarationNode does the processing.
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
		
		///////////////////////////////////////////////////////////////////////////
		// error logging/printing

		private void typeCheckError(ParseNode node, Type ...operandTypes) {
			Token token = node.getToken();
			if (token.isLextant(Keyword.UPDATE))
				logError("identifier " + node.child(0).getToken().getLexeme()
						+ " cannot be assigned " + Arrays.toString(operandTypes) + " value "
						+ " at " + token.getLocation());
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
