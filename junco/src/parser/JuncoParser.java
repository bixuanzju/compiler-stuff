package parser;

import java.util.Arrays;
import logging.JuncoLogger;

import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.CastingNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.UpdateStatementNode;
import tokens.*;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;


public class JuncoParser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		JuncoParser parser = new JuncoParser(scanner);
		return parser.parse();
	}
	public JuncoParser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> box
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		ParseNode box = parseBox();
		program.appendChild(box);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return startsBox(token);
	}
	
	
	///////////////////////////////////////////////////////////
	// boxes

	// box -> BOX MAIN boxBody
	private ParseNode parseBox() {
		if(!startsBox(nowReading)) {
			return syntaxErrorNode("box");
		}
		
		expect(Keyword.BOX);
		expect(Keyword.MAIN);
		ParseNode box = parseBoxBody();
		
		return box;
	}
	private boolean startsBox(Token token) {
		return token.isLextant(Keyword.BOX);
	}
	
	// boxBody -> { statement* }
	private ParseNode parseBoxBody() {
		if(!startsBoxBody(nowReading)) {
			return syntaxErrorNode("boxBody");
		}
		ParseNode box = new BoxBodyNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			box.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return box;
	}
	private boolean startsBoxBody(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	
	///////////////////////////////////////////////////////////
	// statements
	
	// statement-> declaration | printStmt | updateStmt
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		if(startsUpdateStatement(nowReading)) {
			return parseUpdateStatement();
		}
		assert false : "bad token " + nowReading + " in parseStatement()";
		return null;
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) ||
			   startsDeclaration(token) ||
			   startsUpdateStatement(token);
	}
	   
	// printStmt -> (PRINT | PRUNT) expressionList $?;
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print or prunt");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parseExpressionList(result);

		if(nowReading.isLextant(Punctuator.PRINT_NEWLINE)) {
			readToken();
			result.setHasNewline(true);
		}
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT, Keyword.PRUNT);
	}	
	
	// This adds the expressions found to the children of the given parent
	// expressionList -> (expr (, expr)*)?
	private PrintStatementNode parseExpressionList(PrintStatementNode parent) {
		if(!startsExpressionList(nowReading)) {
			parent.appendChild(syntaxErrorNode("expressionList"));
			return parent;
		}
		
		if(startsExpression(nowReading)) {
			ParseNode firstExpression = parseExpression();
			parent.appendChild(firstExpression);
			
			while(nowReading.isLextant(Punctuator.SPLICE)) {
				readToken();
				ParseNode expression = parseExpression();
				parent.appendChild(expression);
			}
		}
		return parent;
	}
	private boolean startsExpressionList(Token token) {
		return true;
	}
	
	// declaration -> CONST identifier <- expression ;
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return DeclarationNode.withChildren(declarationToken, identifier, initializer);
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST, Keyword.INIT);
	}

	// updateStmt -> UPDATE identifier <- expression ;
	private ParseNode parseUpdateStatement() {
		if(!startsUpdateStatement(nowReading)) {
			return syntaxErrorNode("updateStatement");
		}
		Token updateToken = nowReading;
		readToken();
		
		ParseNode target = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode updateValue = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return UpdateStatementNode.withChildren(updateToken, target, updateValue);
	}
	private boolean startsUpdateStatement(Token token) {
		return token.isLextant(Keyword.UPDATE);
	}
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr  -> expr1
	// expr1 -> expr2 [> expr2]?
	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	// expr3 -> expr4 [MULT expr4]*  (left-assoc)
	// expr4 -> literal
	// literal -> intNumber | identifier | booleanConstant | characterConstant

	// expr  -> expr1
	private ParseNode parseExpression() {		
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseExpression1();
	}
	private boolean startsExpression(Token token) {
		return startsExpression1(token);
	}

	// expr1 -> expr2 [> expr2]?
	private ParseNode parseExpression1() {
		if(!startsExpression1(nowReading)) {
			return syntaxErrorNode("expression<1>");
		}
		
		ParseNode left = parseExpression2();
		if(nowReading.isLextant(Punctuator.GREATER, Punctuator.GREATEREQ, Punctuator.LESS, Punctuator.LESSEQ,
				Punctuator.EQUAL, Punctuator.UNEQUAL)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpression2();
			
			return BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		
		return left;

	}
	private boolean startsExpression1(Token token) {
		return startsExpression2(token);
	}

	// expr2 -> expr3 [+ expr3]*  (left-assoc)
	private ParseNode parseExpression2() {
		if(!startsExpression2(nowReading)) {
			return syntaxErrorNode("expression<2>");
		}
		
		ParseNode left = parseExpression3();
		while (nowReading.isLextant(Punctuator.ADD)) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseExpression3();

			left = BinaryOperatorNode.withChildren(additiveToken, left, right);
		}

		while (nowReading.isLextant(Punctuator.MINUS)) {
			Token minusToken = nowReading;
			readToken();
			ParseNode right = parseExpression3();

			left = BinaryOperatorNode.withChildren(minusToken, left, right);
		}

		return left;
	}
	private boolean startsExpression2(Token token) {
		return startsExpression3(token);
	}	

	// expr3 -> expr4 [MULT expr4]*  (left-assoc)
	private ParseNode parseExpression3() {
		if(!startsExpression3(nowReading)) {
			return syntaxErrorNode("expression<3>");
		}
		
		ParseNode left = parseExpression4();
		while(nowReading.isLextant(Punctuator.MULTIPLY)) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseExpression4();
			
			left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
		}
		while(nowReading.isLextant(Punctuator.DIVIDE)) {
			Token divideToken = nowReading;
			readToken();
			ParseNode right = parseExpression4();
			
			left = BinaryOperatorNode.withChildren(divideToken, left, right);
		}
		return left;
	}
	private boolean startsExpression3(Token token) {
		return startsExpression4(token);
	}
	
	
	private ParseNode parseExpression4() {
		if (!startsExpression4(nowReading)) {
			return syntaxErrorNode("expression<4>");
		}

		ParseNode left = parseExpression5();
	
		if (nowReading.isLextant(Punctuator.CASTTOBOOL, Punctuator.CASTTOCHAR,
				Punctuator.CASTTOFLAOT, Punctuator.CASTTOINT)) {
			// syntaxError(nowReading, " expecting type indicator");
			
			Token typeIndicatorToken = nowReading;
			left = CastingNode.withChildren(left, typeIndicatorToken);
			readToken();
		}
		return left;
	}
	
	private boolean startsExpression4(Token token) {
		return startsExpression5(token);
	}
	
	// expr4 -> literal
	private ParseNode parseExpression5() {
		if(!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<4>");
		}
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			readToken();
			ParseNode node = parseExpression1();
			expect(Punctuator.CLOSE_BRACKET);
			return node;
		}
		else
			return parseLiteral();
	}
	private boolean startsExpression5(Token token) {
		return startsLiteralOrBracket(token);
	}
	
	// literal -> number | identifier | booleanConstant
	private ParseNode parseLiteral() {
		if(!startsLiteralOrBracket(nowReading)) {
			return syntaxErrorNode("literal");
		}
		
		if(startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if(startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if(startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}
		assert false : "bad token " + nowReading + " in parseLiteral()";
		return null;
	}
	private boolean startsLiteralOrBracket(Token token) {
		return startsIntNumber(token) || startsFloatNumber(token) ||  startsIdentifier(token) || 
				startsBooleanConstant(token) || startsCharacterConstant(token) || (token.isLextant(Punctuator.OPEN_BRACKET));
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if(!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntNumberNode(previouslyRead);
	}
	private boolean startsIntNumber(Token token) {
		return token instanceof NumberToken;
	}
	
	// floating number (terminal)
	private ParseNode parseFloatNumber() {
		if(!startsFloatNumber(nowReading)) {
			return syntaxErrorNode("floating number constant");
		}
		readToken();
		return new FloatNumberNode(previouslyRead);
	}
	private boolean startsFloatNumber(Token token) {
		return token instanceof FloatingToken;
	}

	// character (terminal)
	private ParseNode parseCharacterConstant() {
		if(!startsCharacterConstant(nowReading)) {
			return syntaxErrorNode("character constant");
		}
		readToken();
		return new CharacterNode(previouslyRead);
	}
	private boolean startsCharacterConstant(Token token) {
		return token instanceof CharacterToken;
	}
	
	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		return new IdentifierNode(previouslyRead);
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}

	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}	
	
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		JuncoLogger log = JuncoLogger.getLogger("compiler.JuncoParser");
		log.severe("syntax error: " + message);
	}	
}

