package semanticAnalyzer.rewriters;

import java.util.HashMap;
import java.util.Map;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;

public class ASTRewritingVisitor extends ParseNodeVisitor.Default implements ASTRewriter {
	private Map<ParseNode, ParseNode> replacements = new HashMap<ParseNode, ParseNode>();

	/////////////////////////////////////////////////////////////////////////
	// for subclasses to use.
	protected void registerReplacement(ParseNode node, ParseNode replacement) {
		replacements.put(node, replacement);
	}
	
	
	/////////////////////////////////////////////////////////////////////////
	// Rewriter implementation
	@Override
	public ParseNode rewrite(ParseNode root) {
		root.accept(this);
		
		for(ParseNode old: replacements.keySet()) {
			assert(old != root);
			replace(old);
		}
		return root;
	}
	private void replace(ParseNode old) {
		ParseNode replacement = replacements.get(old);
		ParseNode parent = old.getParent();
		parent.replaceChild(old, replacement);
	}
	
	@Override
	public boolean changesWereMade() {
		return !replacements.isEmpty();
	}
}