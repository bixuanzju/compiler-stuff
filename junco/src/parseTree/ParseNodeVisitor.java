package parseTree;

import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.FunctionDeclNode;
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

// Visitor pattern with pre- and post-order visits
public interface ParseNodeVisitor {

	// non-leaf nodes: visitEnter and visitLeave
	void visitEnter(BinaryOperatorNode node);

	void visitLeave(BinaryOperatorNode node);

	void visitEnter(BoxBodyNode node);

	void visitLeave(BoxBodyNode node);

	void visitEnter(DeclarationNode node);

	void visitLeave(DeclarationNode node);

	void visitEnter(UpdateStatementNode node);

	void visitLeave(UpdateStatementNode node);

	void visitEnter(IfStatementNode node);

	void visitLeave(IfStatementNode node);

	void visitEnter(WhileStatementNode node);

	void visitLeave(WhileStatementNode node);
	
	void visitEnter(ReturnStatementNode node);
	
	void visitLeave(ReturnStatementNode node);

	void visitEnter(BodyNode node);

	void visitLeave(BodyNode node);

	void visitEnter(ParseNode node);

	void visitLeave(ParseNode node);

	void visitEnter(PrintStatementNode node);

	void visitLeave(PrintStatementNode node);

	void visitEnter(ProgramNode node);

	void visitLeave(ProgramNode node);
	
	void visitEnter(ValueBodyNode node);
	
	void visitLeave(ValueBodyNode node);

	void visitEnter(UniaryOperatorNode node);

	void visitLeave(UniaryOperatorNode node);
	
	void visitEnter(FunctionDeclNode node);

	void visitLeave(FunctionDeclNode node);
	
	void visitEnter(ParameterListNode node);

	void visitLeave(ParameterListNode node);
	

	// leaf nodes: visitLeaf only
	void visit(BooleanConstantNode node);

	void visit(ErrorNode node);

	void visit(IdentifierNode node);

	void visit(IntNumberNode node);

	void visit(FloatNumberNode node);

	void visit(CharacterNode node);

	public static class Default implements ParseNodeVisitor {
		public void defaultVisit(ParseNode node) {
		}

		public void defaultVisitEnter(ParseNode node) {
			defaultVisit(node);
		}

		public void defaultVisitLeave(ParseNode node) {
			defaultVisit(node);
		}

		public void defaultVisitForLeaf(ParseNode node) {
			defaultVisit(node);
		}

		public void visitEnter(BinaryOperatorNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(BinaryOperatorNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(BoxBodyNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(BoxBodyNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(BodyNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(BodyNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(DeclarationNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(DeclarationNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(UpdateStatementNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(UpdateStatementNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(ParseNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(ParseNode node) {
			defaultVisitLeave(node);
		}
		
		public void visitEnter(ValueBodyNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(ValueBodyNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(PrintStatementNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(PrintStatementNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(IfStatementNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(IfStatementNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(WhileStatementNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(WhileStatementNode node) {
			defaultVisitLeave(node);
		}

			
		public void visitEnter(ReturnStatementNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(ReturnStatementNode node) {
			defaultVisitLeave(node);		
		}
		
		public void visitEnter(ProgramNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(ProgramNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(FunctionDeclNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(FunctionDeclNode node) {
			defaultVisitLeave(node);
		}
		
		public void visitEnter(ParameterListNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(ParameterListNode node) {
			defaultVisitLeave(node);
		}

		public void visitEnter(UniaryOperatorNode node) {
			defaultVisitEnter(node);
		}

		public void visitLeave(UniaryOperatorNode node) {
			defaultVisitLeave(node);
		}

		public void visit(BooleanConstantNode node) {
			defaultVisitForLeaf(node);
		}

		public void visit(ErrorNode node) {
			defaultVisitForLeaf(node);
		}

		public void visit(IdentifierNode node) {
			defaultVisitForLeaf(node);
		}

		public void visit(IntNumberNode node) {
			defaultVisitForLeaf(node);
		}

		public void visit(FloatNumberNode node) {
			defaultVisitForLeaf(node);
		}

		public void visit(CharacterNode node) {
			defaultVisitForLeaf(node);
		}
		
		
	}

}
