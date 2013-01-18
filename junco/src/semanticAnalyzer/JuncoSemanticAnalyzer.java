package semanticAnalyzer;

import java.util.Arrays;

import lexicalAnalyzer.Lextant;
import logging.JuncoLogger;

import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
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
			Scopes.enterStaticScope(node);
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

		///////////////////////////////////////////////////////////////////////////
		// expressions
		@Override
		public void visitLeave(BinaryOperatorNode node) {
			assert node.nChildren() == 2;
			ParseNode left  = node.child(0);
			ParseNode right = node.child(1);
			
			Lextant operator = operatorFor(node);
			FunctionSignature signature = FunctionSignature.signatureOf(operator);
			
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


		///////////////////////////////////////////////////////////////////////////
		// simple leaf nodes
		@Override
		public void visit(BooleanConstantNode node) {
			node.setType(PrimitiveType.BOOLEAN);
		}
		@Override
		public void visit(ErrorNode node) {
			node.setType(PrimitiveType.ERROR);
		}
		@Override
		public void visit(IntNumberNode node) {
			node.setType(PrimitiveType.INTEGER);
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
			
			logError("operator " + token.getLexeme() + " not defined for types " 
					 + Arrays.toString(operandTypes)  + " at " + token.getLocation());	
		}
		private void logError(String message) {
			JuncoLogger log = JuncoLogger.getLogger("compiler.semanticAnalyzer");
			log.severe(message);
		}
	}
}
