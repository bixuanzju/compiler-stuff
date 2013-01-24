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
		assert(token.isLextant(Keyword.UPDATE));
	}

	public UpdateStatementNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getUpdateType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static UpdateStatementNode withChildren(Token token, ParseNode updateName, ParseNode updateValue) {
		UpdateStatementNode node = new UpdateStatementNode(token);
		node.appendChild(updateName);
		node.appendChild(updateValue);
		return node;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
