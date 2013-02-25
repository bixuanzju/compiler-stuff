package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import static semanticAnalyzer.PrimitiveType.BOOLEAN;
import semanticAnalyzer.RangeType;

public class CompareRangeNodeRewriter extends NodeRewriterImp {
	private ParseNode left;
	private ParseNode right;
	private RangeType rangeType;

	@Override
	public ParseNode rewriteNode(ParseNode node) {
		setLocation(node.getToken().getLocation());

		left = node.child(0);
		right = node.child(1);
		rangeType = (RangeType) left.getType();

		BinaryOperatorNode operatorNode = (BinaryOperatorNode) node;
		Lextant operator = operatorNode.getOperator();

		if (operator == Punctuator.LESSEQ) {
			return rewriteLessEquals(node);
		}
		else if (operator == Punctuator.LESS) {
			return rewriteLess(node);
		}
		else if (operator == Punctuator.GREATEREQ) {
			return rewriteGreaterEquals(node);
		}
		else if (operator == Punctuator.GREATER) {
			return rewriteGreater(node);
		}
		else if (operator == Punctuator.EQUAL) {
			return rewriteEquals(node);
		}
		else if (operator == Punctuator.UNEQUAL) {
			return rewriteNotEquals(node);
		}

		return node;
	}

	private ParseNode rewriteLessEquals(ParseNode node) {
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();
		
		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						isEmpty(identifier(leftVariable, rangeType)),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(ifStatement(
								isEmpty(identifier(rightVariable, rangeType)),
								bodyNode(update(identifier(resultVariable, BOOLEAN),
										falseNode())),
								bodyNode(update(
										identifier(resultVariable, BOOLEAN),
										and(lessEquals(lowEnd(identifier(leftVariable, rangeType)),
												lowEnd(identifier(rightVariable, rangeType))),
												lessEquals(
														highEnd(identifier(leftVariable, rangeType)),
														highEnd(identifier(rightVariable, rangeType))))))))),

				identifier(resultVariable, BOOLEAN));
	}

	private ParseNode rewriteLess(ParseNode node) {
		// for you to implement.
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						isEmpty(identifier(leftVariable, rangeType)),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(ifStatement(
								isEmpty(identifier(rightVariable, rangeType)),
								bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
								bodyNode(update(
										identifier(resultVariable, BOOLEAN),
										and(and(
												lessEquals(lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType))),
												lessEquals(
														highEnd(identifier(leftVariable, rangeType)),
														highEnd(identifier(rightVariable, rangeType)))),
												or(less(lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType))),
														less(highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType)))))))))),

				identifier(resultVariable, BOOLEAN));
	}

	private ParseNode rewriteGreater(ParseNode node) {
		// for you to implement.
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						isEmpty(identifier(rightVariable, rangeType)),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(ifStatement(
								isEmpty(identifier(leftVariable, rangeType)),
								bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
								bodyNode(update(
										identifier(resultVariable, BOOLEAN),
										and(and(
												lessEquals(
														lowEnd(identifier(rightVariable, rangeType)),
														lowEnd(identifier(leftVariable, rangeType))),
												lessEquals(
														highEnd(identifier(rightVariable, rangeType)),
														highEnd(identifier(leftVariable, rangeType)))),
												or(less(lowEnd(identifier(rightVariable, rangeType)),
														lowEnd(identifier(leftVariable, rangeType))),
														less(highEnd(identifier(rightVariable, rangeType)),
																highEnd(identifier(leftVariable, rangeType)))))))))),

				identifier(resultVariable, BOOLEAN));
	}

	private ParseNode rewriteGreaterEquals(ParseNode node) {
		// for you to implement.
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						isEmpty(identifier(rightVariable, rangeType)),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(ifStatement(
								isEmpty(identifier(leftVariable, rangeType)),
								bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
								bodyNode(update(
										identifier(resultVariable, BOOLEAN),
										and(lessEquals(
												lowEnd(identifier(rightVariable, rangeType)),
												lowEnd(identifier(leftVariable, rangeType))),
												lessEquals(
														highEnd(identifier(rightVariable, rangeType)),
														highEnd(identifier(leftVariable, rangeType))))))))),

				identifier(resultVariable, BOOLEAN));
	}

	private ParseNode rewriteEquals(ParseNode node) {
		// for you to implement.
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						and(isEmpty(identifier(leftVariable, rangeType)),
								isEmpty(identifier(rightVariable, rangeType))),
						bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
						bodyNode(ifStatement(
								and(isEmpty(identifier(leftVariable, rangeType)),
										negate(isEmpty(identifier(rightVariable, rangeType)))),
								bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
								bodyNode(ifStatement(
										and(isEmpty(identifier(rightVariable, rangeType)),
												negate(isEmpty(identifier(leftVariable, rangeType)))),
										bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
										bodyNode(ifStatement(
												and(equalEquals(
														lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType))),
														equalEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType)))),
												bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
												bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode()))))))))),

				identifier(resultVariable, BOOLEAN));
	}

	private ParseNode rewriteNotEquals(ParseNode node) {
		// for you to implement.
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, falseNode(), BOOLEAN),

				ifStatement(
						and(isEmpty(identifier(leftVariable, rangeType)),
								isEmpty(identifier(rightVariable, rangeType))),
						bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode())),
						bodyNode(ifStatement(
								and(isEmpty(identifier(leftVariable, rangeType)),
										negate(isEmpty(identifier(rightVariable, rangeType)))),
								bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
								bodyNode(ifStatement(
										and(isEmpty(identifier(rightVariable, rangeType)),
												negate(isEmpty(identifier(leftVariable, rangeType)))),
										bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
										bodyNode(ifStatement(
												or(negate(equalEquals(
														lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType)))),
														negate(equalEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType))))),
												bodyNode(update(identifier(resultVariable, BOOLEAN), trueNode())),
												bodyNode(update(identifier(resultVariable, BOOLEAN), falseNode()))))))))),

				identifier(resultVariable, BOOLEAN));
	}

}
