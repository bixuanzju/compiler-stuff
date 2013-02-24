package semanticAnalyzer.rewriters;

import static semanticAnalyzer.PrimitiveType.BOOLEAN;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;

public class RangeInRewriter extends NodeRewriterImp {
	private ParseNode left;
	private ParseNode right;
	private RangeType rangeType;
	private Type type;

	@Override
	public ParseNode rewriteNode(ParseNode node) {
		// TODO Auto-generated method stub

		setLocation(node.getToken().getLocation());

		left = node.child(0);
		right = node.child(1);
		rangeType = (RangeType) right.getType();

		BinaryOperatorNode operatorNode = (BinaryOperatorNode) node;
		Lextant operator = operatorNode.getOperator();

		if (operator == Keyword.IN) {
			return rewriteIn(node);
		}

		return node;
	}

	private ParseNode rewriteIn(ParseNode node) {
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, type),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						and(lessEquals(identifier(leftVariable, type),
								highEnd(identifier(rightVariable, rangeType))),
								lessEquals(lowEnd(identifier(rightVariable, rangeType)),
										identifier(leftVariable, type))),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode()))),

				identifier(resultVariable, BOOLEAN));

	}

}