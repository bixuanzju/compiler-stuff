package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.OperatorNode;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;

public class RangeOperatorRewritingVisitor extends ASTRewritingVisitor {	
	
	///////////////////////////////////////////////////////////////////////////
	// I use a class called "OperatorNode".  You might have "BinaryOperatorNode" 
	// and/or "UnaryOperatorNode" instead.
	@Override
	public void visitLeave(OperatorNode node) {
		if(isIsEmptyMemberOperator(node)) {			// range.isEmpty
			assert node.nChildren() == 1;
			
			NodeRewriter rewriter = new IsEmptyNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
		else if(isComparisonOperator(node) && isRangeOperation(node)) {
			assert node.nChildren() == 2;
			
			NodeRewriter rewriter = new CompareRangeNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
		// else span, intersect, in ?
	}
	private boolean isRangeOperation(OperatorNode node) {
		ParseNode child = node.child(0);
		Type type = child.getType();
		return type instanceof RangeType;
	}
	private boolean isIsEmptyMemberOperator(OperatorNode node) {
		// TODO You must implement this!
		return false;
	}
	private boolean isComparisonOperator(OperatorNode node) {
		// TODO You must implement this!
		return false;
	}
	
}