package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.UniaryOperatorNode;
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
		// else span, intersect, in ?
	}

	public void visitLeave(UniaryOperatorNode node) {
		if (isIsEmptyMemberOperator(node)) { // range.isEmpty
			assert node.nChildren() == 1;

			NodeRewriter rewriter = new IsEmptyNodeRewriter();
			registerReplacement(node, rewriter.rewriteNode(node));
		}
	}

	private boolean isRangeOperation(BinaryOperatorNode node) {
		ParseNode child = node.child(0);
		Type type = child.getType();
		return type instanceof RangeType;
	}

	private boolean isIsEmptyMemberOperator(UniaryOperatorNode node) {
		// TODO You must implement this!
		if (node.getToken().isLextant(Punctuator.EMPTY)) {
			return true;
		}
		return false;
	}

	private boolean isComparisonOperator(BinaryOperatorNode node) {
		// TODO You must implement this!
		if (node.getToken().isLextant(Punctuator.GREATER, Punctuator.GREATEREQ,
				Punctuator.LESS, Punctuator.LESSEQ, Punctuator.EQUAL,
				Punctuator.UNEQUAL)) {
			return true;
		}
		return false;
	}

}