package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import semanticAnalyzer.PrimitiveType;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;

public class SetRangeNodeRewriter extends NodeRewriterImp {
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

		if (operator == Punctuator.SPAN) {
			if (getPrimitiveType(rangeType) == PrimitiveType.BOOLEAN) {
				String leftVariable = freshVariableName();
				String rightVariable = freshVariableName();
				String resultVariable = freshVariableName();

				return valueBodyNode(
						declareConst(leftVariable, left, rangeType),
						declareConst(rightVariable, right, rangeType),
						declareInit(resultVariable, identifier(leftVariable, rangeType), rangeType),

						update(
								identifier(resultVariable, rangeType),
								rangeNode(lowEnd(identifier(leftVariable, rangeType)),
										highEnd(identifier(leftVariable, rangeType)))),
										
						returnStatement(identifier(resultVariable, rangeType)));
			}

			return rewriteSpan(node);
		}

		else if (operator == Punctuator.INTERSECTION) {
			if (getPrimitiveType(rangeType) == PrimitiveType.BOOLEAN) {
				String leftVariable = freshVariableName();
				String rightVariable = freshVariableName();
				String resultVariable = freshVariableName();

				return valueBodyNode(

						declareConst(leftVariable, left, rangeType),
						declareConst(rightVariable, right, rangeType),
						declareInit(resultVariable, identifier(leftVariable, rangeType), rangeType),
						
						update(
								identifier(resultVariable, rangeType),
								rangeNode(lowEnd(identifier(rightVariable, rangeType)),
										highEnd(identifier(rightVariable, rangeType)))),
										
						returnStatement(identifier(resultVariable, rangeType)));

			}

			return rewriteIntersection(node);
		}

		return node;
	}

	private ParseNode rewriteSpan(ParseNode node) {
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, identifier(leftVariable, rangeType), rangeType),

				ifStatement(
						isEmpty(identifier(rightVariable, rangeType)),
						bodyNode(update(
								identifier(resultVariable, rangeType),
								rangeNode(lowEnd(identifier(leftVariable, rangeType)),
										highEnd(identifier(leftVariable, rangeType))))),
						bodyNode(ifStatement(
								isEmpty(identifier(leftVariable, rangeType)),
								bodyNode(update(
										identifier(resultVariable, rangeType),
										rangeNode(lowEnd(identifier(rightVariable, rangeType)),
												highEnd(identifier(rightVariable, rangeType))))),
								bodyNode(ifStatement(
										and(lessEquals(lowEnd(identifier(leftVariable, rangeType)),
												lowEnd(identifier(rightVariable, rangeType))),
												less(highEnd(identifier(rightVariable, rangeType)),
														highEnd(identifier(leftVariable, rangeType)))),
										bodyNode(update(
												identifier(resultVariable, rangeType),
												rangeNode(lowEnd(identifier(leftVariable, rangeType)),
														highEnd(identifier(leftVariable, rangeType))))),
										bodyNode(ifStatement(
												and(lessEquals(
														lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType))),
														lessEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType)))),
												bodyNode(update(
														identifier(resultVariable, rangeType),
														rangeNode(
																lowEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType))))),
												bodyNode(ifStatement(
														and(lessEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType))),
																less(
																		lowEnd(identifier(rightVariable, rangeType)),
																		lowEnd(identifier(leftVariable, rangeType)))),
														bodyNode(update(
																identifier(resultVariable, rangeType),
																rangeNode(
																		lowEnd(identifier(rightVariable, rangeType)),
																		highEnd(identifier(rightVariable, rangeType))))),
														bodyNode(update(
																identifier(resultVariable, rangeType),
																rangeNode(
																		lowEnd(identifier(rightVariable, rangeType)),
																		highEnd(identifier(leftVariable, rangeType)))))))))))))),
				returnStatement(identifier(resultVariable, rangeType)));

	}

	private ParseNode rewriteIntersection(ParseNode node) {
		String leftVariable = freshVariableName();
		String rightVariable = freshVariableName();
		String resultVariable = freshVariableName();

		return valueBodyNode(
				declareConst(leftVariable, left, rangeType),
				declareConst(rightVariable, right, rangeType),
				declareInit(resultVariable, identifier(leftVariable, rangeType), rangeType),

				ifStatement(
						isEmpty(identifier(rightVariable, rangeType)),
						bodyNode(update(
								identifier(resultVariable, rangeType),
								rangeNode(lowEnd(identifier(rightVariable, rangeType)),
										highEnd(identifier(rightVariable, rangeType))))),
						bodyNode(ifStatement(
								isEmpty(identifier(leftVariable, rangeType)),
								bodyNode(update(
										identifier(resultVariable, rangeType),
										rangeNode(lowEnd(identifier(leftVariable, rangeType)),
												highEnd(identifier(leftVariable, rangeType))))),
								bodyNode(ifStatement(
										and(lessEquals(lowEnd(identifier(leftVariable, rangeType)),
												lowEnd(identifier(rightVariable, rangeType))),
												less(highEnd(identifier(rightVariable, rangeType)),
														highEnd(identifier(leftVariable, rangeType)))),
										bodyNode(update(
												identifier(resultVariable, rangeType),
												rangeNode(lowEnd(identifier(rightVariable, rangeType)),
														highEnd(identifier(rightVariable, rangeType))))),
										bodyNode(ifStatement(
												and(lessEquals(
														lowEnd(identifier(leftVariable, rangeType)),
														lowEnd(identifier(rightVariable, rangeType))),
														lessEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType)))),
												bodyNode(update(
														identifier(resultVariable, rangeType),
														rangeNode(
																lowEnd(identifier(rightVariable, rangeType)),
																highEnd(identifier(leftVariable, rangeType))))),
												bodyNode(ifStatement(
														and(lessEquals(
																highEnd(identifier(leftVariable, rangeType)),
																highEnd(identifier(rightVariable, rangeType))),
																less(
																		lowEnd(identifier(rightVariable, rangeType)),
																		lowEnd(identifier(leftVariable, rangeType)))),
														bodyNode(update(
																identifier(resultVariable, rangeType),
																rangeNode(
																		lowEnd(identifier(leftVariable, rangeType)),
																		highEnd(identifier(leftVariable, rangeType))))),
														bodyNode(update(
																identifier(resultVariable, rangeType),
																rangeNode(
																		lowEnd(identifier(leftVariable, rangeType)),
																		highEnd(identifier(rightVariable, rangeType)))))))))))))),
				returnStatement(identifier(resultVariable, rangeType)));

	}
	
	private Type getPrimitiveType(Type type) {
		if (type instanceof RangeType) {
			return getPrimitiveType(((RangeType) type).getChildType());
		}
		else {
			return type;
		}
	}

}