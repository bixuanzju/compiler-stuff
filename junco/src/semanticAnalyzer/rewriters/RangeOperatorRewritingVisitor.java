package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.MemberAccessNode;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;

public class RangeOperatorRewritingVisitor extends ASTRewritingVisitor {

	// /////////////////////////////////////////////////////////////////////////
	// I use a class called "OperatorNode". You might have "BinaryOperatorNode"
	// and/or "UnaryOperatorNode" instead.
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		if (isComparisonOperator(node) && isRangeOperation(node)) {
			assert node.nChildren() == 2;

			NodeRewriter rewriter = new CompareRangeNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
		else if (isSetRangeOperator(node)) {
			assert node.nChildren() == 2;
	
			NodeRewriter rewriter = new SetRangeNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
		
		else if (isInOperator(node)) {
			assert node.nChildren() == 2;
			
			NodeRewriter rewriter = new RangeInRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}

	}
	
	public void visitLeave(MemberAccessNode node) {
		if (isIsEmptyMemberOperator(node)) { // range.isEmpty
			assert node.nChildren() == 2;

			NodeRewriter rewriter = new IsEmptyNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
	}
	
	private boolean isRangeOperation(BinaryOperatorNode node) {
		ParseNode child = node.child(0);
		Type type = child.getType();
		return type instanceof RangeType;
	}

	private boolean isIsEmptyMemberOperator(MemberAccessNode node) {
		if (node.child(1).getToken().getLexeme().equals("isEmpty")) {
			return true;
		}
		return false;
	}
	
	private boolean isSetRangeOperator(BinaryOperatorNode node) {
		if (node.getToken().isLextant(Punctuator.SPAN, Punctuator.INTERSECTION)) {
			return true;
		}
		
		return false;
	}
	
	private boolean isInOperator(BinaryOperatorNode node) {
		if (node.getToken().isLextant(Keyword.IN)) {
			return true;
		}
		
		return false;
	}
	

	private boolean isComparisonOperator(BinaryOperatorNode node) {

		if (node.getToken().isLextant(Punctuator.GREATER, Punctuator.GREATEREQ,
				Punctuator.LESS, Punctuator.LESSEQ, Punctuator.EQUAL,
				Punctuator.UNEQUAL)) {
			return true;
		}
		return false;
	}

}