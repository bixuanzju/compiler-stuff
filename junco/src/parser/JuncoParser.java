package parser;

import java.util.Arrays;
import logging.JuncoLogger;
import parseTree.*;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BodyNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.BoxBodyNode;
import parseTree.nodeTypes.BreakStatementNode;
import parseTree.nodeTypes.CallStatementNode;
import parseTree.nodeTypes.CharacterNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.ExpressionListNode;
import parseTree.nodeTypes.FloatNumberNode;
import parseTree.nodeTypes.FunctionDeclNode;
import parseTree.nodeTypes.FunctionInvocationNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntNumberNode;
import parseTree.nodeTypes.MemberAccessNode;
import parseTree.nodeTypes.ParameterListNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.ReturnStatementNode;
import parseTree.nodeTypes.UniaryOperatorNode;
import parseTree.nodeTypes.UpdateStatementNode;
import parseTree.nodeTypes.ValueBodyNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.BoxType;
import semanticAnalyzer.PrimitiveType;
import semanticAnalyzer.RangeType;
import semanticAnalyzer.Type;
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

	// //////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> box

	private ParseNode parseProgram() {
		if (!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);

		while (nowReading.isLextant(Keyword.BOX)) {
			ParseNode box = parseBox();
			program.appendChild(box);
		}

		if (!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}

		return program;
	}

	private boolean startsProgram(Token token) {
		return startsBox(token);
	}

	// /////////////////////////////////////////////////////////
	// boxes

	// box -> BOX MAIN boxBody
	private ParseNode parseBox() {
		if (!startsBox(nowReading)) {
			return syntaxErrorNode("box");
		}

		expect(Keyword.BOX);
		// expect(Keyword.MAIN);
		ParseNode box = parseBoxBody();

		return box;
	}

	private boolean startsBox(Token token) {
		return token.isLextant(Keyword.BOX);
	}

	// boxBody -> { statement* }
	private ParseNode parseBoxBody() {
		if (!startsBoxBody(nowReading)) {
			return syntaxErrorNode("boxBody");
		}
		ParseNode box = new BoxBodyNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_BRACE);

		while (startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			box.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return box;
	}

	private boolean startsBoxBody(Token token) {
		return token instanceof IdentifierToken;
	}

	// /////////////////////////////////////////////////////////
	// statements

	// statement-> declaration | printStmt | updateStmt
	private ParseNode parseStatement() {
		if (!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if (startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if (startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		if (startsUpdateStatement(nowReading)) {
			return parseUpdateStatement();
		}
		if (startsIfStatement(nowReading)) {
			return parseIfStatement();
		}
		if (startsWhileStatement(nowReading)) {
			return parseWhileStatement();
		}
		if (startsBody(nowReading)) {
			return parseBody();
		}
		if (startsReturnStatement(nowReading)) {
			return parseReturnStatement();
		}
		if (startsBreakStatement(nowReading)) {
			return parseBreakStatement();
		}
		if (startsFunctionDecl(nowReading)) {
			return parseFunctionDecl();
		}
		if (startsCallStatement(nowReading)) {
			return parseCallStatement();
		}
		assert false : "bad token " + nowReading + " in parseStatement()";
		return null;
	}

	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) || startsDeclaration(token)
				|| startsUpdateStatement(token) || startsIfStatement(token)
				|| startsWhileStatement(token) || startsBody(token)
				|| startsReturnStatement(token) || startsFunctionDecl(token)
				|| startsCallStatement(token) || startsBreakStatement(token);
	}

	private boolean startsCallStatement(Token token) {
		return token.isLextant(Keyword.CALL);
	}

	private ParseNode parseCallStatement() {
		if (!startsCallStatement(nowReading)) {
			syntaxErrorNode("call statement");
		}

		CallStatementNode result = new CallStatementNode(nowReading);

		readToken();

		result.appendChild(parseMemberAccess());

		expect(Punctuator.TERMINATOR);

		return result;
	}

	private ParseNode parseIfStatement() {
		if (!startsIfStatement(nowReading)) {
			syntaxErrorNode("if statement");
		}

		IfStatementNode result = new IfStatementNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_BRACKET);
		ParseNode whileExpression = parseExpression();
		result.appendChild(whileExpression);
		expect(Punctuator.CLOSE_BRACKET);

		ParseNode body = parseBody();
		result.appendChild(body);

		if (startsElseBody(nowReading)) {
			expect(Keyword.ELSE);
			// result.setElse(true);
			body = parseBody();
			result.appendChild(body);
		}

		return result;
	}

	private boolean startsElseBody(Token token) {
		return token.isLextant(Keyword.ELSE);
	}

	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}

	private ParseNode parseWhileStatement() {
		if (!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("while statement");
		}
		WhileStatementNode result = new WhileStatementNode(nowReading);
		readToken();
		expect(Punctuator.OPEN_BRACKET);
		ParseNode whileExpression = parseExpression();
		result.appendChild(whileExpression);
		expect(Punctuator.CLOSE_BRACKET);

		ParseNode body = parseBody();
		result.appendChild(body);
		return result;

	}

	private ParseNode parseFunctionDecl() {
		if (!startsFunctionDecl(nowReading)) {
			return syntaxErrorNode("function declaration");
		}

		FunctionDeclNode result = new FunctionDeclNode(nowReading);

		expect(Keyword.FUNC);

		ParseNode identifier = parseIdentifier();
		result.appendChild(identifier);

		ParseNode parameterList = parseParameterList();
		result.appendChild(parameterList);

		expect(Punctuator.COLON);

		Type typeSpecification = parseTypeSpec();
		if (typeSpecification == null) {
			syntaxError(previouslyRead, "unkown type");
		}
		else {
			result.setType(typeSpecification);
		}

		result.appendChild(parseValueBodyNode());

		return result;
	}

	private boolean startsFunctionDecl(Token token) {
		return token.isLextant(Keyword.FUNC);
	}

	private Type parseTypeSpec() {

		ParseNode result;
		if (nowReading.isLextant(Keyword.BOX)) {
			result = new IdentifierNode(IdentifierToken.make(
					nowReading.getLocation(), "x"));
			readToken();
		}
		else {
			result = parseIdentifier();
		}

		String id = result.getToken().getLexeme().toLowerCase();

		if (id.equals("i") || id.equals("int") || id.equals("integer")) {
			return PrimitiveType.INTEGER;
		}
		else if (id.equals("b") || id.equals("bool") || id.equals("boolean")) {
			return PrimitiveType.BOOLEAN;
		}
		else if (id.equals("c") || id.equals("char") || id.equals("character")) {
			return PrimitiveType.CHARACTER;
		}
		else if (id.equals("f") || id.equals("float") || id.equals("floating")) {
			return PrimitiveType.FLOATNUM;
		}
		else if (id.equals("r") || id.equals("range")) {
			expect(Punctuator.COLON);
			return new RangeType(parseTypeSpec());
		}
		else if (id.equals("x") || id.equals("box")) {
			expect(Punctuator.COLON);
			ParseNode boxType = parseIdentifier();
			return new BoxType(boxType.getToken().getLexeme(), 0);
		}
		return null;
	}

	private ParseNode parseParameterList() {
		if (!startsParameterList(nowReading)) {
			return syntaxErrorNode("parameter list");
		}

		ParseNode result = new ParameterListNode(nowReading);
		expect(Punctuator.OPEN_BRACKET);

		if (nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
			expect(Punctuator.CLOSE_BRACKET);
			return result;
		}

		ParseNode parameter = parseIdentifier();
		expect(Punctuator.COLON);
		Type typeSpecification = parseTypeSpec();
		if (typeSpecification == null) {
			syntaxError(previouslyRead, "unkown type");
		}
		else {
			parameter.setType(typeSpecification);
		}
		result.appendChild(parameter);

		while (nowReading.isLextant(Punctuator.SPLICE)) {
			expect(Punctuator.SPLICE);

			parameter = parseIdentifier();
			expect(Punctuator.COLON);
			typeSpecification = parseTypeSpec();
			if (typeSpecification == null) {
				syntaxError(previouslyRead, "unkown type");
			}
			else {
				parameter.setType(typeSpecification);
			}
			result.appendChild(parameter);
		}

		expect(Punctuator.CLOSE_BRACKET);

		return result;

	}

	private boolean startsParameterList(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET);
	}

	private ParseNode parseBody() {
		if (!startsBody(nowReading)) {
			syntaxErrorNode("body node");
		}
		ParseNode body = new BodyNode(nowReading);

		expect(Punctuator.OPEN_BRACE);
		while (startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			body.appendChild(statement);
		}

		expect(Punctuator.CLOSE_BRACE);
		return body;
	}

	private ParseNode parseValueBodyNode() {
		if (!startsValueBodyNode(nowReading)) {
			syntaxErrorNode("body node");
		}
		ParseNode body = new ValueBodyNode(nowReading);

		expect(Punctuator.BODY_OPEN);
		while (startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			body.appendChild(statement);
		}

		expect(Punctuator.BODY_CLOSE);
		return body;
	}

	private boolean startsValueBodyNode(Token token) {
		return token.isLextant(Punctuator.BODY_OPEN);
	}

	private boolean startsBody(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}

	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}

	// printStmt -> (PRINT | PRUNT) expressionList $?;
	private ParseNode parsePrintStatement() {
		if (!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print or prunt");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);

		readToken();
		result = (PrintStatementNode) parseExpressionList(result);

		if (nowReading.isLextant(Punctuator.PRINT_NEWLINE)) {
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
	private ParseNode parseExpressionList(ParseNode parent) {
		if (!startsExpressionList(nowReading)) {
			parent.appendChild(syntaxErrorNode("expressionList"));
			return parent;
		}

		if (startsExpression(nowReading)) {
			ParseNode firstExpression = parseExpression();
			parent.appendChild(firstExpression);

			while (nowReading.isLextant(Punctuator.SPLICE)) {
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
		if (!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();

		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);

		return DeclarationNode.withChildren(declarationToken, identifier,
				initializer);
	}

	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST, Keyword.INIT);
	}

	private ParseNode parseReturnStatement() {
		if (!startsReturnStatement(nowReading)) {
			return syntaxErrorNode("return");
		}

		Token returnToken = nowReading;
		readToken();

		ParseNode expression = parseExpression();
		expect(Punctuator.TERMINATOR);

		return ReturnStatementNode.withChildren(returnToken, expression);
	}
	

	private boolean startsReturnStatement(Token token) {
		return token.isLextant(Keyword.RETURN);
	}
	
	private ParseNode parseBreakStatement() {
		if (!startsBreakStatement(nowReading)) {
			return syntaxErrorNode("break");
		}
		
		Token returnToken = nowReading;
		readToken();
		
		expect(Punctuator.TERMINATOR);
		return new BreakStatementNode(returnToken);
	
	}
	
	private boolean startsBreakStatement(Token token) {
		return token.isLextant(Keyword.BREAK);
	}


	// updateStmt -> UPDATE identifier <- expression ;
	private ParseNode parseUpdateStatement() {
		if (!startsUpdateStatement(nowReading)) {
			return syntaxErrorNode("updateStatement");
		}
		Token updateToken = nowReading;
		readToken();

		ParseNode target = parseExpression();

		if (!(target instanceof IdentifierNode || target instanceof MemberAccessNode)) {
			syntaxError(updateToken, "can't be targeted");
		}

		expect(Punctuator.ASSIGN);
		ParseNode updateValue = parseExpression();
		expect(Punctuator.TERMINATOR);

		return UpdateStatementNode.withChildren(updateToken, target, updateValue);
	}

	private boolean startsUpdateStatement(Token token) {
		return token.isLextant(Keyword.UPDATE);
	}

	// /////////////////////////////////////////////////////////
	// expressions
	// expr -> expr1
	// expr1 -> expr2 [> expr2]?
	// expr2 -> expr3 [+ expr3]* (left-assoc)
	// expr3 -> expr4 [MULT expr4]* (left-assoc)
	// expr4 -> literal
	// literal -> intNumber | identifier | booleanConstant | characterConstant

	// expr -> expr1
	private ParseNode parseExpression() {
		if (!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseExpressionOr();
	}

	private boolean startsExpression(Token token) {
		return startsExpression5(token);
	}

	private ParseNode parseExpressionOr() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<Or>");
		}

		ParseNode left = parseExpressionAnd();
		while (nowReading.isLextant(Punctuator.OR)) {
			Token BooleanToken = nowReading;
			readToken();
			ParseNode right = parseExpressionAnd();

			left = BinaryOperatorNode.withChildren(BooleanToken, left, right);
		}

		return left;

	}

	private ParseNode parseExpressionAnd() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<And>");
		}

		ParseNode left = parseExpression1();
		while (nowReading.isLextant(Punctuator.AND)) {
			Token BooleanToken = nowReading;
			readToken();
			ParseNode right = parseExpression1();

			left = BinaryOperatorNode.withChildren(BooleanToken, left, right);
		}

		return left;

	}

	// expr1 -> expr2 [> expr2]?
	private ParseNode parseExpression1() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<1>");
		}

		ParseNode left = parseExpressionSpan();
		while (nowReading.isLextant(Punctuator.GREATER, Punctuator.GREATEREQ,
				Punctuator.LESS, Punctuator.LESSEQ, Punctuator.EQUAL,
				Punctuator.UNEQUAL, Keyword.IN)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpressionSpan();

			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}

		return left;

	}

	// parse range span
	private ParseNode parseExpressionSpan() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<span>");
		}

		ParseNode left = parseExpressionInter();
		while (nowReading.isLextant(Punctuator.SPAN)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpressionInter();

			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}

		return left;

	}

	// parse range intersection
	private ParseNode parseExpressionInter() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<intersection>");
		}

		ParseNode left = parseExpression2();
		while (nowReading.isLextant(Punctuator.INTERSECTION)) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseExpression2();

			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}

		return left;

	}

	// private boolean startsExpression1(Token token) {
	// return startsExpression2(token);
	// }

	// expr2 -> expr3 [+ expr3]* (left-assoc)
	private ParseNode parseExpression2() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<2>");
		}

		ParseNode left = parseExpression3();
		while (nowReading.isLextant(Punctuator.ADD, Punctuator.MINUS)) {
			Token operatorToken = nowReading;
			readToken();
			ParseNode right = parseExpression3();

			left = BinaryOperatorNode.withChildren(operatorToken, left, right);
		}

		return left;
	}

	// private boolean startsExpression2(Token token) {
	// return startsExpression3(token);
	// }

	// expr3 -> expr4 [MULT expr4]* (left-assoc)
	private ParseNode parseExpression3() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<3>");
		}

		ParseNode left = parseExpression4();
		while (nowReading.isLextant(Punctuator.MULTIPLY, Punctuator.DIVIDE)) {
			Token operatorToken = nowReading;
			readToken();
			ParseNode right = parseExpression4();

			left = BinaryOperatorNode.withChildren(operatorToken, left, right);
		}

		return left;
	}

	// cast expression
	private ParseNode parseExpression4() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<case>");
		}

		ParseNode left = parseExpressionNot();

		if (nowReading.isLextant(Punctuator.COLON)) {

			expect(Punctuator.COLON);

			ParseNode typeIdentifier = parseIdentifier();

			String id = typeIdentifier.getToken().getLexeme().toLowerCase();

			if (id.equals("i") || id.equals("int") || id.equals("integer")) {
				left = UniaryOperatorNode
						.withChildren(left, LextantToken.make(typeIdentifier.getToken()
								.getLocation(), "i", Punctuator.CASTTOINT));
			}
			else if (id.equals("b") || id.equals("bool") || id.equals("boolean")) {
				left = UniaryOperatorNode.withChildren(left, LextantToken
						.make(typeIdentifier.getToken().getLocation(), "b",
								Punctuator.CASTTOBOOL));
			}
			else if (id.equals("f") || id.equals("float") || id.equals("floating")) {
				left = UniaryOperatorNode.withChildren(left, LextantToken.make(
						typeIdentifier.getToken().getLocation(), "f",
						Punctuator.CASTTOFLAOT));
			}
			else if (id.equals("c") || id.equals("char") || id.equals("character")) {
				left = UniaryOperatorNode.withChildren(left, LextantToken
						.make(typeIdentifier.getToken().getLocation(), "c",
								Punctuator.CASTTOCHAR));
			}
			else {
				syntaxError(typeIdentifier.getToken(), "unknown type");
			}

		}

		return left;
	}

	// parse boolean not expression
	private ParseNode parseExpressionNot() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<Not>");
		}

		Token BoolenaNotToken = nowReading;
		ParseNode left = null;

		if (nowReading.isLextant(Punctuator.NOT)) {
			readToken();
			left = parseMemberAccess();
			left = UniaryOperatorNode.withChildren(left, BoolenaNotToken);
		}
		else
			left = parseMemberAccess();

		return left;
	}

	// parse member access expression
	private ParseNode parseMemberAccess() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<menber>");
		}

		ParseNode left = parseBoxCreation();

		while (nowReading.isLextant(Punctuator.DOT)) {
			Token operatorToken = nowReading;
			readToken();

			ParseNode right = parseExpression5();

			left = MemberAccessNode.withChildren(operatorToken, left, right);
		}

		return left;
	}

	private ParseNode parseBoxCreation() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<box creation>");
		}
		Token token = nowReading;
		ParseNode right = null;

		if (nowReading.isLextant(Punctuator.AT)) {
			readToken();
			right = parseExpression5();
			right = UniaryOperatorNode.withChildren(right, token);
		}
		else {
			right = parseExpression5();
		}

		return right;
	}

	// expr4 -> literal
	private ParseNode parseExpression5() {
		if (!startsExpression5(nowReading)) {
			return syntaxErrorNode("expression<5>");
		}
		if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
			readToken();
			ParseNode node = parseExpression();
			expect(Punctuator.CLOSE_BRACKET);
			return node;
		}
		else if (nowReading.isLextant(Punctuator.OPEN_SQUARE)) {
			Token token = nowReading;
			readToken();
			ParseNode left = parseExpression();
			expect(Punctuator.SPLICE);
			ParseNode right = parseExpression();
			expect(Punctuator.CLOSE_SQUARE);
			return BinaryOperatorNode.withChildren(token, left, right);
		}
		else if (nowReading.isLextant(Punctuator.BODY_OPEN)) {
			return parseValueBodyNode();
		}
		else if (nowReading instanceof IdentifierToken) {
			ParseNode id = parseIdentifier();

			if (nowReading.isLextant(Punctuator.OPEN_BRACKET)) {
				ParseNode functionInvocation = new FunctionInvocationNode(nowReading);

				ParseNode expressionList = new ExpressionListNode(nowReading);

				expect(Punctuator.OPEN_BRACKET);
				if (!nowReading.isLextant(Punctuator.CLOSE_BRACKET)) {
					expressionList = parseExpressionList(expressionList);
				}
				expect(Punctuator.CLOSE_BRACKET);

				functionInvocation.appendChild(id);
				functionInvocation.appendChild(expressionList);

				return functionInvocation;
			}

			return id;
		}
		else {
			return parseLiteral();
		}
	}

	private boolean startsExpression5(Token token) {
		return startsLiteralOrBracket(token);
	}

	// literal -> number | identifier | booleanConstant
	private ParseNode parseLiteral() {
		if (!startsLiteralOrBracket(nowReading)) {
			return syntaxErrorNode("literal");
		}

		if (startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if (startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if (startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if (startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}
		if (startsThisKeyword(nowReading)) {
			return parseThisKeyWord();
		}

		assert false : "bad token " + nowReading + " in parseLiteral()";
		return null;
	}

	private boolean startsLiteralOrBracket(Token token) {
		return startsIntNumber(token)
				|| startsFloatNumber(token)
				|| startsIdentifier(token)
				|| startsBooleanConstant(token)
				|| startsCharacterConstant(token)
				|| (token.isLextant(Punctuator.OPEN_BRACKET, Punctuator.NOT,
						Punctuator.OPEN_SQUARE, Punctuator.BODY_OPEN, Punctuator.AT))
				|| startsThisKeyword(token);
	}

	private ParseNode parseThisKeyWord() {
		if (!startsThisKeyword(nowReading)) {
			return syntaxErrorNode("this keyword");
		}
		readToken();
		return new IdentifierNode(IdentifierToken.make(
				previouslyRead.getLocation(), Keyword.THIS.getLexeme()));
	}

	// this keyword
	private boolean startsThisKeyword(Token token) {
		return token.isLextant(Keyword.THIS);
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if (!startsIntNumber(nowReading)) {
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
		if (!startsFloatNumber(nowReading)) {
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
		if (!startsCharacterConstant(nowReading)) {
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
		if (!startsIdentifier(nowReading)) {
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
		if (!startsBooleanConstant(nowReading)) {
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
	// otherwise, give a syntax error and read next token (to avoid endless
	// looping).
	private void expect(Lextant... lextants) {
		if (!nowReading.isLextant(lextants)) {
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
