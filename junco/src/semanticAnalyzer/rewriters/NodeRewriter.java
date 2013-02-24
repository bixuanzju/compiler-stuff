package semanticAnalyzer.rewriters;

import parseTree.ParseNode;

public interface NodeRewriter {
	/** returns the root of the rewritten node.
	 */
	public ParseNode rewriteNode(ParseNode node);
}
