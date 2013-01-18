package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class UpdateStatementNode extends ParseNode {

	public UpdateStatementNode(Token token) {
		super(token);
		assert (token.isLextant(Keyword.UPDATE));
	}

	public UpdateStatementNode(ParseNode node) {
		super(node);
	}

}
