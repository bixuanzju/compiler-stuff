package semanticAnalyzer.rewriters;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import inputHandler.TextLocation;
import parseTree.ParseNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.UniaryOperatorNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.ValueBodyNode;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.Token;

abstract public class NodeRewriterImp implements NodeRewriter {
	private TextLocation rewriteLocation;
	protected void setLocation(TextLocation location) {
		rewriteLocation = location;
	}
	
	private static int nextVariableNumber = 0;
	protected String freshVariableName() {
		return "rewriter*" + nextVariableNumber++;
	}
	
	// valueBodyNode is a new node type that isn't in the spec (yet).
	// it makes rewriting parts of expressions easier.
	protected ParseNode valueBodyNode(ParseNode ...children) {
		Token valueBodyToken = lextantToken(Punctuator.NULL_PUNCTUATOR);
		ParseNode node = new ValueBodyNode(valueBodyToken);
		for(ParseNode child: children) {
			node.appendChild(child);
		}
		return node;
	}	
	protected ParseNode bodyNode(ParseNode ...children) {
		Token bodyToken = lextantToken(Punctuator.NULL_PUNCTUATOR);
		ParseNode node = new BodyNode(bodyToken);
		for(ParseNode child: children) {
			node.appendChild(child);
		}
		return node;
	}
	protected ParseNode ifStatement(ParseNode ...children) {
		assert children.length == 2 || children.length == 3;
		
		ParseNode result = new IfStatementNode(lextantToken(Keyword.IF));
		for (ParseNode child : children) {
			result.appendChild(child);
		}
		
		return result;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	// declarations	and update
	protected ParseNode declareConst(String variableName, ParseNode initializer, Type type) {
		return declare(Keyword.CONST, variableName, initializer, type);
	}	
	protected ParseNode declareInit(String variableName, ParseNode initializer, Type type) {
		return declare(Keyword.INIT, variableName, initializer, type);
	}
	private ParseNode declare(Keyword declarationType, String variableName, ParseNode initializer, Type type) {
		Token declarationToken = lextantToken(declarationType);
		IdentifierNode identifierNode = identifier(variableName, type);
		
		return DeclarationNode.withChildren(declarationToken, identifierNode, initializer);
	}
	protected ParseNode update(ParseNode updated, ParseNode updater) {
		Token declarationToken = lextantToken(Keyword.UPDATE);
		
		return UpdateStatementNode.withChildren(declarationToken, updated, updater);
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	// low end, high end, identifier	
	protected ParseNode lowEnd(ParseNode child) {
		Token operatorToken = lextantToken(Punctuator.LOW);	//TODO use binary node
		ParseNode result = UniaryOperatorNode.withChildren(child, operatorToken);
		
		RangeType childType = (RangeType)child.getType();
		result.setType(childType.getChildType());
		return result;
	}
	protected ParseNode highEnd(ParseNode child) {
		Token operatorToken = lextantToken(Punctuator.HIGH);
		ParseNode result = UniaryOperatorNode.withChildren(child, operatorToken);
		
		RangeType childType = (RangeType)child.getType();
		result.setType(childType.getChildType());
		return result;
	}
	protected IdentifierNode identifier(String variableName, Type type) {
		IdentifierNode identifierNode = new IdentifierNode(identifierToken(variableName));
		identifierNode.setType(type);
		return identifierNode;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	// simple nodes at rewrite location	
	protected ParseNode lessEquals(ParseNode arg1, ParseNode arg2) {
		
		return binaryOperatorNode(Punctuator.LESSEQ, arg1, arg2);
	}	
	protected ParseNode less(ParseNode arg1, ParseNode arg2) {
		return binaryOperatorNode(Punctuator.LESS, arg1, arg2);
	}	
	protected ParseNode equalEquals(ParseNode arg1, ParseNode arg2) {
		
		return binaryOperatorNode(Punctuator.EQUAL, arg1, arg2);
	}		
	protected ParseNode isEmpty(ParseNode arg1) {
		return uniaryOperatorNode(Punctuator.EMPTY, arg1);
	}	
	protected ParseNode and(ParseNode arg1, ParseNode arg2) {
		return binaryOperatorNode(Punctuator.AND, arg1, arg2);
	}	
	protected ParseNode or(ParseNode arg1, ParseNode arg2) {
		return binaryOperatorNode(Punctuator.OR, arg1, arg2);
	}
	protected ParseNode negate(ParseNode child) {
		return uniaryOperatorNode(Punctuator.NOT, child);
	}
	private ParseNode binaryOperatorNode(Lextant lextant, ParseNode arg1, ParseNode arg2) {
		Token operatorToken = lextantToken(lextant);
		return BinaryOperatorNode.withChildren(operatorToken, arg1, arg2);
	}
	private ParseNode uniaryOperatorNode(Lextant lextant, ParseNode arg) {
		Token operatorToken = lextantToken(lextant);
		return UniaryOperatorNode.withChildren(arg, operatorToken);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	// booleans
	protected BooleanConstantNode falseNode() {
		LextantToken token = lextantToken(Keyword.FALSE);
		return new BooleanConstantNode(token);
	}
	protected BooleanConstantNode trueNode() {
		LextantToken token = lextantToken(Keyword.TRUE);
		return new BooleanConstantNode(token);
	}

	
	////////////////////////////////////////////////////////////////////////////////////////
	// tokens at rewrite location
	protected IdentifierToken identifierToken(String variableName) {
		return IdentifierToken.make(rewriteLocation, variableName);
	}
	protected LextantToken lextantToken(Lextant lextant) {
		return LextantToken.make(rewriteLocation, "(synthetic) " + lextant.getLexeme(), lextant);
	}

	
}
