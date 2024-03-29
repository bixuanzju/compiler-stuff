package semanticAnalyzer.rewriters;

import parseTree.ParseNode;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;

public class IsEmptyNodeRewriter extends NodeRewriterImp {

	@Override
	public ParseNode rewriteNode(ParseNode node) {
		setLocation(node.getToken().getLocation());

		ParseNode child = node.child(0);
		RangeType rangeType = (RangeType) child.getType();
		Type rangeSubtype = rangeType.getChildType();

		if (!rangeSubtype.isComparable()) {
			return trueNode();
		}
		else {
			String variableName = freshVariableName();
			return valueBodyNode(
					declareConst(variableName, child, rangeType),
					returnStatement(negate(lessEquals(lowEnd(identifier(variableName, rangeType)),
							highEnd(identifier(variableName, rangeType))))));
		}
	}
}
