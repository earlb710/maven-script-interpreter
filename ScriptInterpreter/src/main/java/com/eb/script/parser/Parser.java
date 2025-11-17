package com.eb.script.parser;

import com.eb.script.interpreter.Builtins;
import com.eb.script.json.Json;
import com.eb.script.RuntimeContext;
import com.eb.script.token.Category;
import com.eb.script.token.DataType;
import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import com.eb.script.interpreter.expression.Expression;
import com.eb.script.interpreter.statement.Statement;
import com.eb.script.interpreter.expression.LiteralExpression;
import com.eb.script.interpreter.expression.BinaryExpression;
import com.eb.script.interpreter.expression.CallExpression;
import com.eb.script.interpreter.expression.VariableExpression;
import com.eb.script.interpreter.statement.PrintStatement;
import com.eb.script.interpreter.statement.BlockStatement;
import com.eb.script.interpreter.statement.VarStatement;
import com.eb.script.interpreter.expression.UnaryExpression;
import com.eb.script.interpreter.statement.AssignStatement;
import com.eb.script.interpreter.statement.BreakStatement;
import com.eb.script.interpreter.statement.CallStatement;
import com.eb.script.interpreter.statement.ContinueStatement;
import com.eb.script.interpreter.statement.DoWhileStatement;
import com.eb.script.interpreter.statement.IfStatement;
import com.eb.script.interpreter.statement.Parameter;
import com.eb.script.interpreter.statement.ReturnStatement;
import com.eb.script.interpreter.statement.WhileStatement;
import com.eb.script.interpreter.expression.ArrayExpression;
import com.eb.script.interpreter.expression.ArrayLiteralExpression;
import com.eb.script.interpreter.expression.IndexExpression;
import com.eb.script.interpreter.expression.LengthExpression;
import com.eb.script.interpreter.statement.ForEachStatement;
import com.eb.script.interpreter.statement.IndexAssignStatement;
import com.eb.script.interpreter.expression.SqlSelectExpression;
import com.eb.script.interpreter.statement.CursorStatement;
import com.eb.script.interpreter.statement.OpenCursorStatement;
import com.eb.script.interpreter.statement.CloseCursorStatement;
import com.eb.script.interpreter.expression.CursorHasNextExpression;
import com.eb.script.interpreter.expression.CursorNextExpression;
import com.eb.script.interpreter.statement.ConnectStatement;
import com.eb.script.interpreter.statement.UseConnectionStatement;
import com.eb.script.interpreter.statement.CloseConnectionStatement;
import com.eb.script.interpreter.statement.ScreenStatement;
import com.eb.script.interpreter.statement.ScreenShowStatement;
import com.eb.script.interpreter.statement.ScreenHideStatement;
import com.eb.script.interpreter.statement.ImportStatement;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.util.Util;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private static EbsLexer lexer = new EbsLexer();
    private final List<EbsToken> tokens;
    private int current;
    private EbsToken currToken;
    private EbsToken prevToken;
    private final List<Statement> postParseStatments = new ArrayList();
    private final List<Expression> postParseExpressions = new ArrayList();
    private Map<String, BlockStatement> blocks;
    private List<Statement> statements;
    private int loopDepth;
    private final String source;

    public static RuntimeContext parse(Path file) throws IOException, ParseError {
        String script = Files.readString(file, StandardCharsets.UTF_8);

        List<EbsToken> tokens = lexer.tokenize(script);

        Parser parser = new Parser(script, tokens);
        parser.parse();
        RuntimeContext ret = new RuntimeContext(file.getFileName().toString(), file, parser.blocks, statementsToArray(parser.statements));
        return ret;
    }

    public static RuntimeContext parse(String name, String script) throws IOException, ParseError {
        List<EbsToken> tokens = lexer.tokenize(script);

        Parser parser = new Parser(script, tokens);
        parser.parse();
        RuntimeContext ret = new RuntimeContext(name, parser.blocks, statementsToArray(parser.statements));
        return ret;
    }

    public static void parse(RuntimeContext context, String source, List<EbsToken> tokens) throws IOException, ParseError {
        Parser parser = new Parser(source, tokens);
        parser.parse();
        context.blocks = parser.blocks;
        context.statements = statementsToArray(parser.statements);
    }

    private Parser(String source, List<EbsToken> tokens) {
        this.tokens = tokens;
        this.source = source;
    }

    private void parse() throws ParseError {
        current = 0;
        currToken = tokens.get(current);
        blocks = new HashMap();
        
        // Separate imports from other statements
        List<Statement> importStatements = new ArrayList<>();
        List<Statement> otherStatements = new ArrayList<>();
        
        while (!isAtEnd()) {
            Statement s = statement();
            if (s != null) {
                if (s instanceof ImportStatement) {
                    importStatements.add(s);
                } else if (s instanceof BlockStatement bs) {
                    if (bs.name != null) {
                        blocks.put(bs.name, bs);
                    } else {
                        otherStatements.add(s);
                    }
                } else {
                    otherStatements.add(s);
                }
            }
        }
        
        // Place imports first, then other statements
        statements = new ArrayList<>();
        statements.addAll(importStatements);
        statements.addAll(otherStatements);
        
        postParse();
    }

    private void postParse() throws ParseError {
        for (Statement s : postParseStatments) {
            if (s instanceof CallStatement c) {
                BlockStatement b = blocks.get(c.name);
                if (b == null) {
                    if (Builtins.isBuiltin(c.name)) {
                        try {
                            Parameter[] def = Builtins.getSignature(c.name);
                            c.parameters = matchParameters(def, c.parameters);
                            // No paramInit for builtins; Interpreter evaluates directly from c.parameters
                        } catch (ParseError ex) {
                            throw new ParseError(ex.getMessage() + " in call to " + c.name + " on line " + c.getLine());
                        }
                    }
                    // Allow calls to unknown functions - they might be imported at runtime
                    // The interpreter will validate at execution time
                } else {
                    c.setBlockStatement(b);
                }
                try {
                    if (b != null && b.parameters != null) {
                        Parameter[] parameters = matchParameters(b.parameters, c.parameters);
                        c.parameters = parameters;
                        Statement[] paramInit = new Statement[parameters.length];
                        int pidx = 0;
                        for (Parameter p : parameters) {
                            paramInit[pidx] = new VarStatement(c.getLine(), p.name, p.paramType, p.value);
                            pidx++;
                        }
                        c.paramInit = paramInit;
                    }
                } catch (ParseError ex) {
                    throw new ParseError(ex.getMessage() + " in call to " + c.name + " on line " + c.getLine());
                }
            }
        }
        for (Expression e : postParseExpressions) {
            if (e instanceof CallExpression ce) {
                CallStatement call = ce.call;
                BlockStatement b = blocks.get(call.name);
                if (b == null) {
                    // NEW: builtin resolution
                    if (Builtins.isBuiltin(call.name)) {
                        try {
                            Parameter[] def = Builtins.getSignature(call.name);
                            call.parameters = matchParameters(def, call.parameters);
                            // No paramInit for builtins; Interpreter evaluates directly from c.parameters
                            Builtins.BuiltinInfo bi = Builtins.getBuiltinInfo(call.name);
                            // Set expression return type from builtin definition
                            ce.setReturnType(bi.returnType);
                        } catch (ParseError ex) {
                            throw new ParseError(ex.getMessage() + " in call to " + call.name + " on line " + call.getLine());
                        }
                    }
                    // Allow calls to unknown functions - they might be imported at runtime
                    // The interpreter will validate at execution time
                } else {
                    call.setBlockStatement(b);
                    ce.setReturnType(b.returnType);
//                    if (ce.returnType == null) {
//                        throw error("Called block must have a return value to be used in expression : '" + c.name + "'");
//                    }
                }
                try {
                    if (b != null && b.parameters != null) {
                        Parameter[] parameters = matchParameters(b.parameters, call.parameters);
                        call.parameters = parameters;
                        Statement[] paramInit = new Statement[parameters.length];
                        int pidx = 0;
                        for (Parameter p : parameters) {
                            paramInit[pidx] = new VarStatement(call.getLine(), p.name, p.paramType, p.value);
                            pidx++;
                        }
                        call.paramInit = paramInit;
                    }
                } catch (ParseError ex) {
                    throw new ParseError(ex.getMessage() + " in call to " + call.name + " on line " + call.getLine());
                }
            }
        }
    }

    private Parameter[] matchParameters(Parameter[] defParam, Parameter[] valueParam) throws ParseError {
        if (defParam != null && defParam.length != 0) {
            Parameter[] ret = new Parameter[defParam.length];
            if (valueParam == null || defParam.length >= valueParam.length) {
                int pidx = 0;
                if (valueParam != null && valueParam.length > 0) {
                    if (valueParam[0] != null && valueParam[0].name != null) {
                        for (Parameter pval : valueParam) {
                            if (pval != null) {
                                if (pval.name == null) {
                                    throw error("Cannot mix named and sequence parameters.");
                                }
                                boolean found = false;
                                int didx = 0;
                                for (Parameter dval : defParam) {
                                    if (pval.name.equals(dval.name)) {
                                        pval = new Parameter(dval.name, dval.paramType, pval.value);
                                        found = true;
                                        break;
                                    }
                                    didx++;
                                }
                                if (!found) {
                                    throw error("Cannot find parameter '" + pval.name + "'");
                                } else {
                                    ret[didx] = pval;
                                }
                                pidx++;
                            }
                        }
                    } else {
                        for (Parameter pval : valueParam) {
                            Parameter dval = defParam[pidx];
                            if (pval != null) {
                                pval = new Parameter(dval.name, dval.paramType, pval.value);
                            } else {
                                pval = new Parameter(dval.name, dval.paramType, null);
                            }
                            ret[pidx] = pval;
                            pidx++;
                        }
                    }
                }
                StringBuilder error = new StringBuilder();
                pidx = 0;
                for (Parameter pval : ret) {
                    if (pval == null) {
                        Parameter dval = defParam[pidx];
                        if (dval.value != null) {
                            ret[pidx] = defParam[pidx];
                        } else {
                            if (dval.mandatory) {
                                if (!error.isEmpty()) {
                                    error.append(", ");
                                }
                                error.append(dval.name).append(":").append(dval.paramType);
                            }
                        }
                    }
                    pidx++;
                }
                if (!error.isEmpty()) {
                    throw error("Missing parameters : " + error.toString());
                }
            } else {
                throw error("Too many value parameters : " + valueParam.length);
            }
            return ret;
        }
        return null;
    }

    private EbsTokenType getTokenType(String str) {
        str = str.toLowerCase();
        if (EbsTokenType.INTEGER.contains(str)) {
            return EbsTokenType.INTEGER;
        } else if (EbsTokenType.LONG.contains(str)) {
            return EbsTokenType.LONG;
        } else if (EbsTokenType.STRING.contains(str)) {
            return EbsTokenType.STRING;
        } else if (EbsTokenType.FLOAT.contains(str)) {
            return EbsTokenType.FLOAT;
        } else if (EbsTokenType.DOUBLE.contains(str)) {
            return EbsTokenType.DOUBLE;
        } else if (EbsTokenType.DATE.contains(str)) {
            return EbsTokenType.DATE;
        } else if (EbsTokenType.BOOL.contains(str)) {
            return EbsTokenType.BOOL;
        } else if (EbsTokenType.JSON.contains(str)) {
            return EbsTokenType.JSON;
        }
        return null;
    }

    private Statement statement() throws ParseError {
        if (match(EbsTokenType.IMPORT)) {
            return importStatement();
        } else if (match(EbsTokenType.VAR)) {
            return varDeclaration();
        } else if (match(EbsTokenType.IF)) {
            return ifStatement();
        } else if (match(EbsTokenType.WHILE)) {
            return whileStatement();
        } else if (match(EbsTokenType.DO)) {
            return doWhileStatement();
        } else if (match(EbsTokenType.FOREACH)) {
            return foreachStatement();
        } else if (match(EbsTokenType.BREAK)) {
            return breakStatement();
        } else if (match(EbsTokenType.CONTINUE)) {
            return continueStatement();
        } else if (match(EbsTokenType.CONNECT)) {
            return connectStatement();
        } else if (match(EbsTokenType.SCREEN)) {
            return screenStatement();
        } else if (match(EbsTokenType.USE)) {
            return useConnectionStatement();
        } else if (matchAll(EbsTokenType.CLOSE, EbsTokenType.CONNECTION)) {
            return closeConnectionStatement();
        } else if (match(EbsTokenType.CURSOR)) {
            return cursorStatement();
        } else if (match(EbsTokenType.OPEN)) {
            return openCursorStatement();
        } else if (match(EbsTokenType.CLOSE)) {
            return closeCursorStatement();
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.LBRACE)) {
            EbsToken n = peek();
            advance(2);
            return block((String) n.literal);
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.RETURN)) {
            EbsToken n = peek();
            advance();
            DataType type = blockParameterReturn();
            consume(EbsTokenType.LBRACE, "Expected { after return type.");
            BlockStatement bs = block((String) n.literal);
            bs.returnType = type;
            return bs;
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.LPAREN)) {
            EbsToken n = peek();
            advance(2);
            return blockParameters((String) n.literal);
        } else if (match(EbsTokenType.LBRACE)) {
            return block();
        } else if (match(EbsTokenType.CALL)) {
            return callStatment();
        } else if (match(EbsTokenType.RETURN)) {
            return returnStatment();
        } else if (match(EbsTokenType.PRINT)) {
            return printStatement();
        } else if (match(EbsTokenType.COMMENT)) {
            return null;
        }
        return assignmentStatement();
    }

    private Statement importStatement() throws ParseError {
        int line = prevToken.line;
        
        // Expect a string literal for the filename: QUOTE1 STRING QUOTE1 (or QUOTE2)
        EbsTokenType quoteType = null;
        if (match(EbsTokenType.QUOTE1)) {
            quoteType = EbsTokenType.QUOTE1;
        } else if (match(EbsTokenType.QUOTE2)) {
            quoteType = EbsTokenType.QUOTE2;
        } else {
            throw error(peek(), "Expected string literal for import filename.");
        }
        
        EbsToken filenameToken = consume(EbsTokenType.STRING, "Expected string value for import filename.");
        consume(quoteType, "Expected closing quote after import filename.");
        
        if (!(filenameToken.literal instanceof String)) {
            throw error(filenameToken, "Import filename must be a string literal.");
        }
        
        String filename = (String) filenameToken.literal;
        consume(EbsTokenType.SEMICOLON, "Expected ';' after import statement.");
        
        return new ImportStatement(line, filename);
    }

    private Statement varDeclaration() throws ParseError {
        EbsToken name = peek();
        
        // Check if the token is a keyword and reject it with a clear error message
        if (name.type.getCategory() == Category.KEYWORD) {
            throw error(name, "Cannot use keyword '" + name.literal + "' as a variable name.");
        }
        
        name = consume(EbsTokenType.IDENTIFIER, "Expected variable name.");
        DataType elemType = null;
        Expression[] arrayDims = null;

        if (match(EbsTokenType.COLON)) {
            EbsToken t = peek();
            if (t.type.getDataType() != null) {
                elemType = t.type.getDataType();
            } else {
                if (t.type == EbsTokenType.DATATYPE) {
                    elemType = getTokenType((String) t.literal).getDataType();
                } else {
                    throw error(t, "Expected type name after ':'.");
                }
            }
            advance();
            if (check(EbsTokenType.LBRACKET)) {
                arrayDims = parseArrayDimensions();
            }
        }

        Expression varInit = null;

        if (arrayDims != null && arrayDims.length > 0) {
            // Implicit allocation: synthesize an ArrayInitExpression from parsed sizes.
            varInit = new ArrayExpression(name.line, elemType, arrayDims, null);
        }

        EbsToken eq = consumeOptional(EbsTokenType.EQUAL);
        if (eq != null) {
            if (check(EbsTokenType.SELECT)) {
                // Parse SQL SELECT and produce an expression node
                varInit = parseSqlSelectFromSource();
            } else if (elemType == DataType.JSON && (check(EbsTokenType.LBRACE) || check(EbsTokenType.LBRACKET))) {
                varInit = parseJsonLiteralFromSource();
            } else if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
                if (varInit instanceof ArrayExpression) {
                    ArrayLiteralExpression arrayInit = parseArrayLiteral();
                    ((ArrayExpression) varInit).initializer = arrayInit;
                    arrayInit.array = varInit;
                } else {
                    throw error(eq, "Variable not defined as an array, cannot use {} for non-arrays.");
                }
            } else {
                varInit = expression();
            }
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after variable declaration.");
        return new VarStatement(name.line, (String) name.literal, elemType, varInit);

    }

    private LiteralExpression parseJsonLiteralFromSource() throws ParseError {
        return parseJsonLiteralFromSource(false);
    }
    private LiteralExpression parseJsonLiteralFromSource(boolean lowerCaseKey) throws ParseError {
        if (source == null) {
            throw error(currToken, "Parser was not constructed with source; cannot parse JSON literal.");
        }

        // Expect { or [
        boolean startsWithBrace = check(EbsTokenType.LBRACE);
        boolean startsWithBracket = check(EbsTokenType.LBRACKET);
        if (!startsWithBrace && !startsWithBracket) {
            throw error(currToken, "Expected '{' or '[' to start JSON literal.");
        }

        // The very first token marks the start position
        EbsToken first = currToken;
        int depth = 0;

        // consume first and begin tracking depth
        if (startsWithBrace) {
            advance();
            depth = 1;    // consumed '{'
        } else {
            advance();
            depth = 1;    // consumed '['
        }
        EbsToken last = first;

        while (!isAtEnd() && depth > 0) {
            // Track nesting for { }, [ ]
            if (check(EbsTokenType.LBRACE) || check(EbsTokenType.LBRACKET)) {
                depth++;
            } else if (check(EbsTokenType.RBRACE) || check(EbsTokenType.RBRACKET)) {
                depth--;
            }

            last = currToken;
            advance();
        }

        if (depth != 0) {
            throw error(last, "Unterminated JSON literal; missing closing '}' or ']'.");
        }

        // slice from first.start .. last.end (inclusive)
        int from = first.start;
        int toInclusive = last.end;
        String jsonText = source.substring(from, toInclusive + 1);

        Object value = Json.parse(jsonText, lowerCaseKey);   // your minimal JSON parser
        return new com.eb.script.interpreter.expression.LiteralExpression(com.eb.script.token.DataType.JSON, value);
    }

    private Statement call() throws ParseError {
        int line = currToken.line;  // or previous().line if needed
        String qname = parseQualifiedName();
        EbsToken param = consumeOptional(EbsTokenType.LPAREN);
        if (param != null) {
            List<Parameter> parameters = getCallParameters();
            CallStatement c = new CallStatement(line, qname, parameters);
            postParseStatments.add(c);
            return c;
        } else {
            CallStatement c = new CallStatement(line, qname);
            postParseStatments.add(c);
            return c;
        }
    }

    private Statement callStatment() throws ParseError {
        Statement ret = call();
        consume(EbsTokenType.SEMICOLON, "Expected ';' after call.");
        return ret;
    }

    private Statement returnStatment() throws ParseError {
        int line = previous().line;
        Expression value = expression();
        Statement ret = new ReturnStatement(line, value);
        consume(EbsTokenType.SEMICOLON, "Expected ';' after return value.");
        return ret;
    }

    private Parameter parameterValue() throws ParseError {
        if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.EQUAL)) {
            EbsToken name = consume(EbsTokenType.IDENTIFIER, "Expected parameter name.");
            consume(EbsTokenType.EQUAL, "Expected = after parameter name.");
            Expression value = expression();
            return new Parameter((String) name.literal, value);
        } else {
            Expression value = expression();
            return new Parameter(value);
        }
    }

    private List<Parameter> getCallParameters() throws ParseError {
        List<Parameter> parameters = new ArrayList<>();
        while (!check(EbsTokenType.RPAREN) && !isAtEnd()) {
            parameters.add(parameterValue());
            EbsToken comma = consumeOptional(EbsTokenType.COMMA);
            if (comma == null) {
                break;
            }
        }
        consume(EbsTokenType.RPAREN, "Expected ')' after parameters.");
        return parameters;
    }

    private Statement block() throws ParseError {
        List<Statement> s = new ArrayList<>();
        int line = currToken.line;
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            s.add(statement());
        }
        consume(EbsTokenType.RBRACE, "Expected '}' after block.");
        return new BlockStatement(line, s);
    }

    private List<Statement> getBlockStatements() throws ParseError {
        List<Statement> s = new ArrayList<>();
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            s.add(statement());
        }
        consume(EbsTokenType.RBRACE, "Expected '}' after block.");
        return s;
    }

    private Parameter parameterDef() throws ParseError {
        EbsToken name = peek();
        
        // Check if the token is a keyword and reject it with a clear error message
        if (name.type.getCategory() == Category.KEYWORD) {
            throw error(name, "Cannot use keyword '" + name.literal + "' as a parameter name.");
        }
        
        name = consume(EbsTokenType.IDENTIFIER, "Expected parameter name.");
        EbsTokenType type = null;
        if (match(EbsTokenType.COLON)) {
            // Accept either IDENTIFIER or a data type keyword (STRING, INTEGER, etc.)
            EbsToken t = peek();
            if (t.type == EbsTokenType.IDENTIFIER || t.type == EbsTokenType.DATATYPE || 
                t.type.getDataType() != null) {
                advance();
                if (t.type == EbsTokenType.IDENTIFIER || t.type == EbsTokenType.DATATYPE) {
                    type = getTokenType((String) t.literal);
                    if (type == null) {
                        throw error(name, "Invalid type : " + (String) t.literal);
                    }
                } else {
                    // It's already a data type keyword
                    type = t.type;
                }
            } else {
                throw error(t, "Expected type name after ':'.");
            }
        } else {
            throw error(name, "No parameter type supplied for " + (String) name.literal);
        }

        EbsToken init = consumeOptional(EbsTokenType.EQUAL);
        if (init != null) {
            Expression value = expression();
            return new Parameter((String) name.literal, type.getDataType(), value);
        } else {
            return new Parameter((String) name.literal, type.getDataType());
        }
    }

    private List<Parameter> getBlockParameters() throws ParseError {
        List<Parameter> parameters = new ArrayList<>();
        while (!check(EbsTokenType.RPAREN) && !isAtEnd()) {
            parameters.add(parameterDef());
            EbsToken comma = consumeOptional(EbsTokenType.COMMA);
            if (comma == null) {
                break;
            }
        }
        consume(EbsTokenType.RPAREN, "Expected ')' after parameters.");
        return parameters;
    }

    private DataType blockParameterReturn() throws ParseError {
        EbsToken ret = consumeOptional(EbsTokenType.RETURN);
        if (ret != null) {
            // Check if current token is a datatype keyword or identifier
            EbsToken ttype = currToken;
            EbsTokenType tokenType = ttype.type;
            
            // If it's a datatype keyword token, use it directly
            if (tokenType.getDataType() != null) {
                advance();
                return tokenType.getDataType();
            }
            
            // Handle DATATYPE token (datatype keywords like "string", "int", etc.)
            if (tokenType == EbsTokenType.DATATYPE) {
                ttype = advance();
                EbsTokenType type = getTokenType((String) ttype.literal);
                if (type != null) {
                    return type.getDataType();
                }
            }
            
            // Otherwise, expect an identifier and look it up as a type name
            ttype = consume(EbsTokenType.IDENTIFIER, "Expected type after return.");
            EbsTokenType type = getTokenType((String) ttype.literal);
            if (type != null) {
                return type.getDataType();
            }
        }
        return null;
    }

    private Statement blockParameters(String name) throws ParseError {
        int line = currToken.line;
        List<Parameter> parameters = getBlockParameters();
        DataType type = blockParameterReturn();
        if (type != null) {
            consume(EbsTokenType.LBRACE, "Expected '{' after return.");
            List<Statement> s = getBlockStatements();
            return new BlockStatement(line, name, parameters, s, type);
        } else {
            consume(EbsTokenType.LBRACE, "Expected '{' after parameters.");
            List<Statement> s = getBlockStatements();
            return new BlockStatement(line, name, parameters, s);
        }
    }

    private BlockStatement block(String name) throws ParseError {
        int line = currToken.line;
        List<Statement> s = getBlockStatements();
        return new BlockStatement(line, name, s);
    }

    private Statement printStatement() throws ParseError {
        int line = currToken.line;
        Expression value = expression();
        consume(EbsTokenType.SEMICOLON, "Expected ';' after print value.");
        return new PrintStatement(line, value);
    }

    private Statement assignmentStatement() throws ParseError {
        // We need to parse an lvalue:
        // either IDENTIFIER, or IDENTIFIER followed by one or more [ index-list ]
        // Also support DOT for screen variable access (screenName.varName)
        EbsToken name = consume(EbsTokenType.IDENTIFIER, "Expected variable name.");
        String varName = (String) name.literal;
        
        // Check if this is a screen variable access (screenName.varName)
        if (match(EbsTokenType.DOT)) {
            EbsToken fieldName = consume(EbsTokenType.IDENTIFIER, "Expected field name after '.'.");
            // Treat screen.var as a single variable name with DOT
            varName = varName + "." + fieldName.literal;
        }
        
        Expression lvalue = new VariableExpression(name.line, varName);

        // Attach any number of bracketed index-suffixes to the variable
        while (check(EbsTokenType.LBRACKET)) {
            Expression[] indices = parseIndexList();
            lvalue = new IndexExpression(name.line, lvalue, indices);
        }

        consume(EbsTokenType.EQUAL, "Expected '=' after variable name or index.");
        Expression value;
        if (check(EbsTokenType.SELECT)) {
            value = parseSqlSelectFromSource();
        } else {
            value = expression();
        }
        consume(EbsTokenType.SEMICOLON, "Expected ';' after assignment.");

        if (value instanceof ArrayLiteralExpression array) {
            array.array = lvalue;
        }

        if (lvalue instanceof VariableExpression ve) {
            return new AssignStatement(name.line, (String) name.literal, value);
        } else {
            return new IndexAssignStatement(name.line, lvalue, value);
        }
    }

// Parser.java
    private Statement ifStatement() throws ParseError {
        // We arrive here after having matched IF in statement()
        int line = previous().line;

        Expression condition;
        if (match(EbsTokenType.LPAREN)) {
            condition = expression();
            consume(EbsTokenType.RPAREN, "Expected ')' after if condition.");
        } else {
            // No parentheses → require 'then' to disambiguate
            condition = expression();
            consume(EbsTokenType.THEN, "Expected 'then' after if condition.");
        }

        // Parse the 'then' branch: either a block `{ ... }` or a single statement
        Statement thenBranch;
        if (match(EbsTokenType.LBRACE)) {
            thenBranch = (BlockStatement) block();
        } else {
            thenBranch = statement();
        }

        // Optional else
        Statement elseBranch = null;
        if (match(EbsTokenType.ELSE)) {
            if (match(EbsTokenType.IF)) {
                // else if ...  (nest another if)
                elseBranch = ifStatement();
            } else if (match(EbsTokenType.LBRACE)) {
                elseBranch = (BlockStatement) block();
            } else {
                elseBranch = statement();
            }
        }

        return new IfStatement(line, condition, thenBranch, elseBranch);
    }

    private Statement whileStatement() throws ParseError {
        int line = previous().line; // the 'while' token we just matched

        Expression condition;
        if (match(EbsTokenType.LPAREN)) {
            condition = expression();
            consume(EbsTokenType.RPAREN, "Expected ')' after while condition.");
        } else {
            condition = expression();
            // allow optional 'then' (for consistency with if ... then ...)
            consumeOptional(EbsTokenType.THEN);
        }

        // Body must be a block (while {})
        consume(EbsTokenType.LBRACE, "Expected '{' after while condition.");
        loopDepth++;
        BlockStatement body = (BlockStatement) block();
        loopDepth--;

        return new WhileStatement(line, condition, body);
    }

    private Statement doWhileStatement() throws ParseError {
        int line = previous().line; // the 'do' token

        consume(EbsTokenType.LBRACE, "Expected '{' after 'do'.");
        loopDepth++;
        BlockStatement body = (BlockStatement) block();
        loopDepth--;

        consume(EbsTokenType.WHILE, "Expected 'while' after do-block.");
        consume(EbsTokenType.LPAREN, "Expected '(' after 'while'.");
        Expression condition = expression();
        consume(EbsTokenType.RPAREN, "Expected ')' after do-while condition.");
        consume(EbsTokenType.SEMICOLON, "Expected ';' after do-while condition.");

        return new DoWhileStatement(line, condition, body);
    }

    private Statement breakStatement() throws ParseError {
        int line = previous().line; // 'break' token line
        if (loopDepth <= 0) {
            throw error(previous(), "'break' used outside of a loop.");
        }
        consume(EbsTokenType.SEMICOLON, "Expected ';' after break.");
        return new BreakStatement(line);
    }

    private Statement continueStatement() throws ParseError {
        int line = previous().line; // 'continue' token line
        if (loopDepth <= 0) {
            throw error(previous(), "'continue' used outside of a loop.");
        }
        consume(EbsTokenType.SEMICOLON, "Expected ';' after continue.");
        return new ContinueStatement(line);
    }

    private Expression expression() throws ParseError {
        return logicalOr();
    }

    private Expression logicalOr() throws ParseError {
        Expression expr = logicalAnd();
        while (match(EbsTokenType.BOOL_OR)) {
            EbsToken operator = previous();
            Expression right = logicalAnd();
            expr = new BinaryExpression(currToken.line, expr, operator, right);
        }
        return expr;
    }

    private Expression logicalAnd() throws ParseError {
        Expression expr = bool();
        while (match(EbsTokenType.BOOL_AND)) {
            EbsToken operator = previous();
            Expression right = bool();
            expr = new BinaryExpression(currToken.line, expr, operator, right);
        }
        return expr;
    }

// private Expression bool() {
//     Expression expr = addition();
//     if (match(Token.Type.BOOL_EQ, Token.Type.BOOL_NEQ, Token.Type.BOOL_GT, Token.Type.BOOL_LT, Token.Type.BOOL_GT_EQ, Token.Type.BOOL_LT_EQ)) {
//         Token operator = previous();
//         Expression right = addition();
//         expr = new BinaryExpression(currToken.line, expr, operator, right);
//     }
//     return expr;
// }
// (chained comparisons)
    private Expression bool() throws ParseError {
        Expression first = addition();

        java.util.List<EbsToken> ops = new java.util.ArrayList<>();
        java.util.List<Expression> operands = new java.util.ArrayList<>();
        operands.add(first);

        while (match(EbsTokenType.BOOL_EQ, EbsTokenType.BOOL_NEQ,
                EbsTokenType.BOOL_GT, EbsTokenType.BOOL_LT,
                EbsTokenType.BOOL_GT_EQ, EbsTokenType.BOOL_LT_EQ)) {
            EbsToken op = previous();
            Expression rhs = addition();
            ops.add(op);
            operands.add(rhs);
        }

        if (ops.isEmpty()) {
            return first;
        }
        if (ops.size() == 1) {
            // keep AST as simple binary when there is just one comparator
            return new BinaryExpression(currToken.line, operands.get(0), ops.get(0), operands.get(1));
        }
        // For 2+ comparators, use the chain node to avoid re-evaluating middle operands
        return new com.eb.script.interpreter.expression.ChainComparisonExpression(
                currToken.line,
                operands.toArray(Expression[]::new),
                ops.toArray(EbsToken[]::new)
        );
    }

    private Expression addition() throws ParseError {
        Expression left = multiplication();

        // fold all '+' left-associatively
        while (match(EbsTokenType.PLUS, EbsTokenType.MINUS)) {
            EbsToken op = previous();
            Expression right = multiplication();
            left = new BinaryExpression(currToken.line, left, op, right);
        }

        return left;
    }

    private Expression multiplication() throws ParseError {
        Expression expr = unary();

        while (match(EbsTokenType.STAR, EbsTokenType.SLASH)) {
            EbsToken operator = previous();
            Expression right = unary();
            expr = new BinaryExpression(currToken.line, expr, operator, right);
        }

        return expr;
    }

    private Expression unary() throws ParseError {
        // Include BANG only if your lexer supports it
        if (match(EbsTokenType.MINUS, EbsTokenType.PLUS, EbsTokenType.BOOL_BANG)) {
            EbsToken operator = previous();
            Expression right = unary();
            return new UnaryExpression(currToken.line, operator, right);
        }
        return exponentiation();
    }

    private Expression exponentiation() throws ParseError {
        Expression left = postfix();
        if (match(EbsTokenType.CARET)) {
            EbsToken op = previous();

            Expression right;
            // NEW: permit a leading unary sign/operator on the exponent
            if (match(EbsTokenType.MINUS, EbsTokenType.PLUS, EbsTokenType.BOOL_BANG)) {
                EbsToken u = previous();
                // Keep right-associativity: the unary applies to the whole right exponent chain
                Expression rhs = exponentiation();
                right = new UnaryExpression(currToken.line, u, rhs);
            } else {
                right = exponentiation();
            }

            return new BinaryExpression(currToken.line, left, op, right);
        }
        return left;
    }

    private Expression postfix() throws ParseError {
        Expression base = primary();
        // keep applying indexing as long as we see '['
        while (check(EbsTokenType.LBRACKET)) {
            int line = currToken.line; // 'currToken' is at '['
            Expression[] indices = parseIndexList();
            base = new IndexExpression(line, base, indices);
        }

        while (match(EbsTokenType.DOT)) {
            int line = currToken.line;
            
            // Accept either IDENTIFIER, LENGTH, or SIZE after DOT
            EbsToken prop;
            String name;
            if (check(EbsTokenType.LENGTH)) {
                prop = advance();
                name = "length";
            } else if (check(EbsTokenType.SIZE)) {
                prop = advance();
                name = "size";
            } else {
                prop = consume(EbsTokenType.IDENTIFIER, "Expected property name after '.'.");
                name = (String) prop.literal;
            }

            // Optional empty parentheses for method-style calls
            boolean callEmpty = false;
            EbsToken lp = consumeOptional(EbsTokenType.LPAREN);
            if (lp != null) {
                consume(EbsTokenType.RPAREN, "Expected ')' after method call.");
                callEmpty = true;
            }

            if (("length".equals(name) || "size".equals(name)) && !callEmpty) {
                base = new LengthExpression(line, base);
            } else if ("hasNext".equals(name) && callEmpty) {
                base = new CursorHasNextExpression(line, base);
            } else if ("next".equals(name) && callEmpty) {
                base = new CursorNextExpression(line, base);
            } else {
                // You may decide to allow other dot-names or reject them here.
                // For now, reject unknown member form to avoid silent acceptance.
                throw error(prop, "Unknown member '" + name + "'" + (callEmpty ? "()" : ""));
            }
        }

        return base;
    }

    private Expression primary() throws ParseError {
        EbsTokenType type = peek().type;
        Expression expr = null;
        if (match(EbsTokenType.INTEGER, EbsTokenType.LONG, EbsTokenType.FLOAT, EbsTokenType.DOUBLE, EbsTokenType.DATE, EbsTokenType.BOOL_TRUE, EbsTokenType.BOOL_FALSE, EbsTokenType.NULL)) {
            expr = new LiteralExpression(type, previous().literal);
        } else if (match(EbsTokenType.IDENTIFIER)) {
            EbsToken p = previous();
            expr = new VariableExpression(p.line, (String) p.literal);
        } else if (match(EbsTokenType.LPAREN)) {
            expr = expression();
            consume(EbsTokenType.RPAREN, "Expected ')' after expression.");
            return expr;
        } else if (match(EbsTokenType.CALL)) {
            CallStatement c = (CallStatement) call();
            expr = new CallExpression(c);
            postParseExpressions.add(expr);
            return expr;
        } else if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
            expr = parseArrayLiteral();
            return expr;
        } else if (match(EbsTokenType.QUOTE1)) {
            return primaryString(EbsTokenType.QUOTE1);
        } else if (match(EbsTokenType.QUOTE2)) {
            return primaryString(EbsTokenType.QUOTE2);
        }
        if (expr == null) {
            throw error(peek(), "Expected expression.");
        } else {
            return expr;
        }
    }

    private Expression primaryString(EbsTokenType quote) throws ParseError {
        EbsTokenType type = peek().type;
        Expression expr = null;
        if (match(EbsTokenType.STRING)) {
            expr = new LiteralExpression(type, previous().literal);
            consume(quote, "Expected termination quote after string value.");
        }
        return expr;
    }

// Parse [ dim1, dim2, ... ]
    private Expression[] parseArrayDimensions() throws ParseError {
        List<Expression> dims = new ArrayList<>();
        consume(EbsTokenType.LBRACKET, "Expected '[' after type name for array dimensions.");
        if (!check(EbsTokenType.RBRACKET)) {
            if (check(EbsTokenType.STAR)) {
                dims.add(null);
                advance();
            } else {
                do {
                    dims.add(expression());
                } while (match(EbsTokenType.COMMA));
            }
        } else {
            dims.add(null);
        }
        consume(EbsTokenType.RBRACKET, "Expected ']' after array dimensions.");
        return dims.toArray(Expression[]::new);
    }
    // Parse { val1, val2, ... } with nesting: elements may themselves be array literals

    private ArrayLiteralExpression parseArrayLiteral() throws ParseError {
        int line = currToken.line;
        EbsToken bstart = consumeOptional(EbsTokenType.LBRACKET);
        EbsTokenType bend = EbsTokenType.RBRACKET;
        if (bstart == null) {
            consume(EbsTokenType.LBRACE, "Expected '{' or '[' to start array literal.");
            bend = EbsTokenType.RBRACE;
        }
        List<Expression> elements = new ArrayList();
        int idx = 0;
        if (!check(bend)) {
            do {
                if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
                    elements.add(parseArrayLiteral());
                } else {
                    elements.add(expression());
                }
                idx++;
            } while (match(EbsTokenType.COMMA));
        }
        consume(bend, "Expected '}' to end array literal.");
        return new ArrayLiteralExpression(line, null, elements.toArray(Expression[]::new));
    }

    private Expression[] parseIndexList() throws ParseError {
        java.util.List<Expression> indices = new java.util.ArrayList<>();
        consume(EbsTokenType.LBRACKET, "Expected '[' to start index list.");
        if (!check(EbsTokenType.RBRACKET)) {
            indices.add(expression());
            while (match(EbsTokenType.COMMA)) {
                indices.add(expression());
            }
        }
        consume(EbsTokenType.RBRACKET, "Expected ']' after index list.");
        Expression[] ret = new Expression[indices.size()];
        int idx = 0;
        for (Expression e : indices) {
            ret[idx] = e;
            idx++;
        }
        return ret;
    }

    private Statement foreachStatement() throws ParseError {
        int line = previous().line; // 'foreach'

        String varName;
        Expression collection;

        if (match(EbsTokenType.LPAREN)) {
            EbsToken var = peek();
            
            // Check if the token is a keyword and reject it with a clear error message
            if (var.type.getCategory() == Category.KEYWORD) {
                throw error(var, "Cannot use keyword '" + var.literal + "' as a loop variable name.");
            }
            
            var = consume(EbsTokenType.IDENTIFIER, "Expected loop variable name.");
            varName = (String) var.literal;
            consume(EbsTokenType.IN, "Expected 'in' after loop variable.");
            collection = expression();
            consume(EbsTokenType.RPAREN, "Expected ')' after foreach header.");
        } else {
            EbsToken var = peek();
            
            // Check if the token is a keyword and reject it with a clear error message
            if (var.type.getCategory() == Category.KEYWORD) {
                throw error(var, "Cannot use keyword '" + var.literal + "' as a loop variable name.");
            }
            
            var = consume(EbsTokenType.IDENTIFIER, "Expected loop variable name.");
            varName = (String) var.literal;
            consume(EbsTokenType.IN, "Expected 'in' after loop variable.");
            collection = expression();
        }

        consume(EbsTokenType.LBRACE, "Expected '{' to start foreach body.");
        loopDepth++;
        BlockStatement body = (BlockStatement) block();
        loopDepth--;

        return new ForEachStatement(line, varName, collection, body);
    }

    private Statement cursorStatement() throws ParseError {
        int line = previous().line; // 'cursor'
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected cursor name after 'cursor'.");
        String cursorName = (String) nameTok.literal;

        consume(EbsTokenType.EQUAL, "Expected '=' after cursor name.");

        // SELECT ... (slice text; do not consume trailing ';' here)
        SqlSelectExpression select = parseSqlSelectFromSource();
        consume(EbsTokenType.SEMICOLON, "Expected ';' after cursor SELECT.");

        return new CursorStatement(line, cursorName, select);
    }

    private Statement openCursorStatement() throws ParseError {
        int line = previous().line; // 'open'
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected cursor name after 'open'.");
        String cursorName = (String) nameTok.literal;

        consume(EbsTokenType.LPAREN, "Expected '(' after cursor name.");
        java.util.List<com.eb.script.interpreter.statement.Parameter> params = getCallParameters(); // reuses existing logic
        consume(EbsTokenType.SEMICOLON, "Expected ';' after open cursor.");

        return new OpenCursorStatement(line, cursorName, params);
    }

    private Statement closeCursorStatement() throws ParseError {
        int line = previous().line; // 'close'
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected cursor name after 'close'.");
        String cursorName = (String) nameTok.literal;
        consume(EbsTokenType.SEMICOLON, "Expected ';' after close cursor.");
        return new CloseCursorStatement(line, cursorName);
    }

    private Statement connectStatement() throws ParseError {
        int line = previous().line; // the 'connect' token

        // 1) Require connection name
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected connection name after 'connect'.");
        String connName = (String) nameTok.literal;

        // 2) Require '='
        consume(EbsTokenType.EQUAL, "Expected '=' after connection name.");

        // 3) Parse the spec:
        //    - STRING literal (e.g., DSN/JDBC URL)
        //    - JSON literal ({...} or [...]) captured from source
        //    - IDENTIFIER (reference to a var holding string/json)
        Expression spec;
        if (check(EbsTokenType.STRING)) {
            // Your existing expression() will read the string literal
            spec = expression();
        } else if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
            spec = parseJsonLiteralFromSource();
        } else if (check(EbsTokenType.IDENTIFIER)) {
            EbsToken id = consume(EbsTokenType.IDENTIFIER, "Expected connection string, json or variable.");
            spec = new VariableExpression(id.line, (String) id.literal);
        } else {
            throw error(peek(), "Expected connection string, json or variable after '='.");
        }

        // 4) ';'
        consume(EbsTokenType.SEMICOLON, "Expected ';' after connect statement.");

        return new ConnectStatement(line, connName, spec);
    }

    private Statement useConnectionStatement() throws ParseError {
        int line = previous().line;  // 'use'
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected connection name after 'use'.");
        String connName = (String) nameTok.literal;

        consume(EbsTokenType.LBRACE, "Expected '{' after connection name.");

        java.util.ArrayList<Statement> body = new java.util.ArrayList<>();
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            Statement s = statement();
            if (s != null) {
                body.add(s);
            }
        }
        consume(EbsTokenType.RBRACE, "Expected '}' to end 'use' block.");

        return new UseConnectionStatement(line, connName, body);
    }

    private Statement closeConnectionStatement() throws ParseError {
        int line = previous().line; // 'connection' token line (after 'close')
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected connection name after 'close connection'.");
        consume(EbsTokenType.SEMICOLON, "Expected ';' after close connection.");
        return new CloseConnectionStatement(line, (String) nameTok.literal);
    }

    private Statement screenStatement() throws ParseError {
        int line = previous().line; // the 'screen' token

        // 1) Require screen name
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected screen name after 'screen'.");
        String screenName = (String) nameTok.literal;

        // 2) Check what follows: '=' (definition), 'show', or 'hide'
        if (match(EbsTokenType.SHOW)) {
            // screen <name> show;
            consume(EbsTokenType.SEMICOLON, "Expected ';' after 'screen <name> show'.");
            return new ScreenShowStatement(line, screenName);
        } else if (match(EbsTokenType.HIDE)) {
            // screen <name> hide;
            consume(EbsTokenType.SEMICOLON, "Expected ';' after 'screen <name> hide'.");
            return new ScreenHideStatement(line, screenName);
        } else if (match(EbsTokenType.EQUAL)) {
            // screen <name> = {...};
            // 3) Parse the spec:
            //    - JSON literal ({...}) captured from source
            //    - IDENTIFIER (reference to a var holding json)
            Expression spec;
            if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
                spec = parseJsonLiteralFromSource(true);
            } else if (check(EbsTokenType.IDENTIFIER)) {
                EbsToken id = consume(EbsTokenType.IDENTIFIER, "Expected screen configuration json or variable.");
                spec = new VariableExpression(id.line, (String) id.literal);
            } else {
                throw error(peek(), "Expected screen configuration json or variable after '='.");
            }

            // 4) ';'
            consume(EbsTokenType.SEMICOLON, "Expected ';' after screen statement.");

            return new ScreenStatement(line, screenName, spec);
        } else {
            throw error(peek(), "Expected '=', 'show', or 'hide' after screen name.");
        }
    }

    private SqlSelectExpression parseSqlSelectFromSource() throws ParseError {
        int line = currToken.line;
        if (source == null) {
            throw error(currToken, "Parser was not constructed with source; cannot parse SQL literal.");
        }

        // Require and consume the SELECT keyword
        EbsToken first = consume(EbsTokenType.SELECT, "Expected 'select'.");
        EbsToken last = first;

        // Walk tokens until we reach the terminator ';' (but do NOT consume it here)
        while (!isAtEnd() && !check(EbsTokenType.SEMICOLON)) {
            last = currToken;
            advance();
        }

        // Reconstruct exact text from the beginning of 'select' to the char before ';'
        int from = first.start;
        int toInclusive = last.end;
        String sqlText = source.substring(from, Math.min(toInclusive + 1, source.length()));

        return new SqlSelectExpression(line, sqlText);
    }

    /**
     * identifier ('.' identifier)* → combined like "ns.name"
     */
    private String parseQualifiedName() throws ParseError {
        EbsToken first = consumeOptional(EbsTokenType.BUILTIN);
        StringBuilder sb = new StringBuilder();
        if (first == null) {
            first = consume(EbsTokenType.IDENTIFIER, "Expected identifier.");
            sb = sb.append((String) first.literal);
            while (match(EbsTokenType.DOT)) {
                EbsToken seg = consume(EbsTokenType.IDENTIFIER, "Expected identifier after '.'.");
                sb.append('.').append((String) seg.literal);
            }
        } else {
            sb = sb.append((String) first.literal);
        }

        return sb.toString();
    }

    private boolean match(EbsTokenType... types) {
        for (EbsTokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean matchAll(EbsTokenType... types) {
        if (check(types[0])) {
            for (int idx = 1; idx < types.length; idx++) {
                if (current + idx < tokens.size()) {
                    EbsTokenType type = types[idx];
                    EbsToken t = tokens.get(current + idx);
                    if (t.type != type) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean check(EbsTokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return currToken.type == type;
    }

    private boolean checkAny(EbsTokenType... types) {
        if (isAtEnd()) {
            return false;
        }
        for (EbsTokenType t : types) {
            if (currToken.type == t) {
                return true;
            }
        }
        return false;
    }

    private EbsToken advance() {
        if (!isAtEnd()) {
            current++;
            prevToken = currToken;
            currToken = tokens.get(current);
        }
        return prevToken;
    }

    private EbsToken advance(int count) {
        if (count > 1 && tokens.size() >= current + count) {
            current = current + count - 1;
            currToken = tokens.get(current);
            prevToken = tokens.get(current - 1);
        }
        return advance();
    }

    private boolean isAtEnd() {
        return currToken.type == EbsTokenType.EOF;
    }

    private EbsToken peek() {
        return currToken;
        //return tokens.get(current );
    }

    private EbsToken previous() {
        return prevToken;
        //return tokens.get(current - 1);
    }

    private EbsToken consume(EbsTokenType type, String message) throws ParseError {
        if (check(type)) {
            return advance();
        }
        throw error(currToken, message);
    }

    private EbsToken consumeOptional(EbsTokenType type) {
        if (check(type)) {
            return advance();
        }
        return null;
    }

    private static Statement[] statementsToArray(List<Statement> list) {
        if (list != null) {
            return list.toArray(Statement[]::new);
        }
        return null;
    }

    private ParseError error(EbsToken token, String message) {
        if (token.type == EbsTokenType.EOF) {
            message = "[line " + token.line + "] Parse error, end of file reached : " + message;
        } else {
            message = "[line " + token.line + "] Parse error at " + token.type.name() + " (" + token.literal + "): " + message;
        }
        return new ParseError(message);
    }

    private ParseError error(EbsToken token, Exception ex) {
        String message = Util.formatExceptionWith2Origin(ex);
        if (token.type == EbsTokenType.EOF) {
            message = "[line " + token.line + "] Parse error end of file reached : " + message;
        } else {
            message = "[line " + token.line + "] Parse error at " + token.type.name() + " (" + token.literal + "): " + message;
        }
        return new ParseError(message);
    }

    private ParseError error(String message) {
        message = "Parse error : " + message;
        return new ParseError(message);
    }

}
