package semanticAnalyzer.rewriters;

import parseTree.ParseNode;

public interface ASTRewriter {
	/** rewrites the tree starting at the given root, giving the new root.
	 * 
	 * @param root	Root of the tree to rewrite.
	 * @return the new root.  This is the old root unless the old root has been rewritten.
	 */
	public ParseNode rewrite(ParseNode root);
	
	/** Reports whether or not the previous call to rewrite() made any changes to the tree.
	 */
	public boolean changesWereMade();
}