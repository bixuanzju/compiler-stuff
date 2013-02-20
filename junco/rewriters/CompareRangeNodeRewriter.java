package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.ParseNode;
import parseTree.nodeTypes.OperatorNode;
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
		rangeType = (RangeType)left.getType();
		
		OperatorNode operatorNode = (OperatorNode)node;
		Lextant operator = operatorNode.getOperator();
		
		if(operator == Punctuator.LESS_EQUALS) {
			return rewriteLessEquals(node);
		}
		else if(operator == Punctuator.LESS) {
			return rewriteLess(node);
		}
		else if(operator == Punctuator.GREATER_EQUALS) {
			return rewriteGreaterEquals(node);
		}
		else if(operator == Punctuator.GREATER) {
			return rewriteGreater(node);
		}
		else if(operator == Punctuator.EQUALS) {
			return rewriteEquals(node);
		}
		else if(operator == Punctuator.NOT_EQUALS) {
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
						update(identifier(resultVariable, BOOLEAN), trueNode()),
						ifStatement(
							isEmpty(identifier(rightVariable, rangeType)),
							update(identifier(resultVariable, BOOLEAN), falseNode()),
							update(identifier(resultVariable, BOOLEAN), and(
								lessEquals(
									lowEnd(identifier(leftVariable, rangeType)),
									lowEnd(identifier(rightVariable, rangeType))
								),
								lessEquals(
									highEnd(identifier(leftVariable, rangeType)),
									highEnd(identifier(rightVariable, rangeType))
								)
							))
						)
					),
					
					identifier(resultVariable, BOOLEAN)
			   );
	}
	private ParseNode rewriteLess(ParseNode node) {
		// for you to implement.
	}
	private ParseNode rewriteGreaterEquals(ParseNode node) {
		// for you to implement.
	}
	private ParseNode rewriteGreater(ParseNode node) {
		// for you to implement.
	}
	private ParseNode rewriteEquals(ParseNode node) {
		// for you to implement.
	}
	private ParseNode rewriteNotEquals(ParseNode node) {
		// for you to implement.
	}

}
