package com.eb.script.parser;

import com.eb.script.interpreter.builtins.Builtins;
import com.eb.script.json.Json;
import com.eb.script.RuntimeContext;
import com.eb.script.token.BitmapType;
import com.eb.script.token.IntmapType;
import com.eb.script.token.Category;
import com.eb.script.token.DataType;
import com.eb.script.token.RecordType;
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
import com.eb.script.interpreter.expression.PropertyExpression;
import com.eb.script.interpreter.expression.CastExpression;
import com.eb.script.interpreter.expression.QueueExpression;
import com.eb.script.interpreter.statement.ForEachStatement;
import com.eb.script.interpreter.statement.ForStatement;
import com.eb.script.interpreter.statement.IndexAssignStatement;
import com.eb.script.interpreter.statement.TypedefStatement;
import com.eb.script.interpreter.TypeRegistry;
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
import com.eb.script.interpreter.statement.ScreenCloseStatement;
import com.eb.script.interpreter.statement.ScreenSubmitStatement;
import com.eb.script.interpreter.statement.ImportStatement;
import com.eb.script.interpreter.statement.TryStatement;
import com.eb.script.interpreter.statement.RaiseStatement;
import com.eb.script.interpreter.statement.ExceptionHandler;
import com.eb.script.interpreter.ErrorType;
import com.eb.script.token.ebs.EbsTokenType;
import com.eb.util.Util;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        // Check if this is a packaged .ebsp file
        if (file.toString().toLowerCase().endsWith(".ebsp")) {
            try {
                return com.eb.script.package_tool.RuntimeContextSerializer.deserialize(file);
            } catch (ClassNotFoundException e) {
                throw new ParseError("Error loading packaged script: " + e.getMessage());
            }
        }
        
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
        blocks = new ConcurrentHashMap<>();
        
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
                            // For dynamic builtins (like custom.*), def may be null
                            // In that case, only positional parameters are allowed (no named parameters)
                            if (def != null) {
                                c.parameters = matchParameters(def, c.parameters);
                            }
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
                    if (b != null && b.parameters != null && b.parameters.length > 0) {
                        Parameter[] parameters = matchParameters(b.parameters, c.parameters);
                        if (parameters != null) {
                            c.parameters = parameters;
                            Statement[] paramInit = new Statement[parameters.length];
                            int pidx = 0;
                            for (Parameter p : parameters) {
                                paramInit[pidx] = new VarStatement(c.getLine(), p.name, p.paramType, p.value);
                                pidx++;
                            }
                            c.paramInit = paramInit;
                        }
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
                            // For dynamic builtins (like custom.*), def may be null
                            // In that case, only positional parameters are allowed
                            if (def != null) {
                                call.parameters = matchParameters(def, call.parameters);
                            }
                            // No paramInit for builtins; Interpreter evaluates directly from c.parameters
                            Builtins.BuiltinInfo bi = Builtins.getBuiltinInfo(call.name);
                            // Set expression return type from builtin definition (if available)
                            if (bi != null) {
                                ce.setReturnType(bi.returnType);
                            }
                            // For custom.* functions, returnType remains null/ANY
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
                    if (b != null && b.parameters != null && b.parameters.length > 0) {
                        Parameter[] parameters = matchParameters(b.parameters, call.parameters);
                        if (parameters != null) {
                            call.parameters = parameters;
                            Statement[] paramInit = new Statement[parameters.length];
                            int pidx = 0;
                            for (Parameter p : parameters) {
                                paramInit[pidx] = new VarStatement(call.getLine(), p.name, p.paramType, p.value);
                                pidx++;
                            }
                            call.paramInit = paramInit;
                        }
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

    /**
     * Convert a type name string to its corresponding EbsTokenType.
     * This helper method maps type name strings to token types, supporting
     * common type name variants (e.g., "int" and "integer", "bool" and "boolean").
     * Order doesn't matter as each type has distinct non-overlapping string patterns.
     */
    private EbsTokenType getTokenType(String str) {
        str = str.toLowerCase();
        if (EbsTokenType.BYTE.contains(str)) {
            return EbsTokenType.BYTE;
        } else if (EbsTokenType.INTEGER.contains(str)) {
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
        } else if (EbsTokenType.ARRAY.contains(str)) {
            return EbsTokenType.ARRAY;
        } else if (EbsTokenType.QUEUE.contains(str)) {
            return EbsTokenType.QUEUE;
        } else if (EbsTokenType.RECORD.contains(str)) {
            return EbsTokenType.RECORD;
        } else if (EbsTokenType.MAP.contains(str)) {
            return EbsTokenType.MAP;
        } else if (EbsTokenType.BITMAP.contains(str)) {
            return EbsTokenType.BITMAP;
        } else if (EbsTokenType.INTMAP.contains(str)) {
            return EbsTokenType.INTMAP;
        } else if (EbsTokenType.IMAGE.contains(str)) {
            return EbsTokenType.IMAGE;
        } else if (EbsTokenType.VECTOR_IMAGE.contains(str)) {
            return EbsTokenType.VECTOR_IMAGE;
        } else if (EbsTokenType.CANVAS.contains(str)) {
            return EbsTokenType.CANVAS;
        }
        return null;
    }

    private Statement statement() throws ParseError {
        if (match(EbsTokenType.IMPORT)) {
            return importStatement();
        } else if (match(EbsTokenType.VAR)) {
            return varDeclaration(false);
        } else if (match(EbsTokenType.CONST)) {
            return varDeclaration(true);
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.TYPEOF)) {
            // Type alias definition: typename typeof type_definition
            return typedefStatement();
        } else if (match(EbsTokenType.IF)) {
            return ifStatement();
        } else if (match(EbsTokenType.WHILE)) {
            return whileStatement();
        } else if (match(EbsTokenType.DO)) {
            return doWhileStatement();
        } else if (match(EbsTokenType.FOREACH)) {
            return foreachStatement();
        } else if (match(EbsTokenType.FOR)) {
            return forStatement();
        } else if (match(EbsTokenType.BREAK)) {
            return breakStatement();
        } else if (match(EbsTokenType.CONTINUE)) {
            return continueStatement();
        } else if (match(EbsTokenType.TRY)) {
            return tryStatement();
        } else if (matchAll(EbsTokenType.RAISE, EbsTokenType.EXCEPTION)) {
            return raiseStatement();
        } else if (match(EbsTokenType.CONNECT)) {
            return connectStatement();
        } else if (checkNewScreen()) {
            // new screen <name> = {...}; syntax for replacing existing screen
            advance(); // consume 'new' identifier
            advance(); // consume 'screen' keyword
            return screenStatement(true);
        } else if (match(EbsTokenType.SCREEN)) {
            return screenStatement(false);
        } else if (match(EbsTokenType.SHOW)) {
            return showScreenStatement();
        } else if (match(EbsTokenType.HIDE)) {
            return hideScreenStatement();
        } else if (matchAll(EbsTokenType.SUBMIT, EbsTokenType.SCREEN)) {
            return submitScreenStatement();
        } else if (match(EbsTokenType.USE)) {
            return useConnectionStatement();
        } else if (matchAll(EbsTokenType.CLOSE, EbsTokenType.CONNECTION)) {
            return closeConnectionStatement();
        } else if (matchAll(EbsTokenType.CLOSE, EbsTokenType.SCREEN)) {
            return closeScreenStatement();
        } else if (match(EbsTokenType.CURSOR)) {
            return cursorStatement();
        } else if (match(EbsTokenType.OPEN)) {
            return openCursorStatement();
        } else if (match(EbsTokenType.CLOSE)) {
            return closeCursorStatement();
        } else if (match(EbsTokenType.FUNCTION)) {
            // Optional 'function' keyword before function definitions
            return functionDeclaration();
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.LBRACE)) {
            EbsToken n = peek();
            advance(2);
            return block((String) n.literal);
        } else if (matchAll(EbsTokenType.IDENTIFIER, EbsTokenType.RETURN)) {
            EbsToken n = peek();
            advance();
            ReturnTypeInfo typeInfo = blockParameterReturn();
            consume(EbsTokenType.LBRACE, "Expected { after return type.");
            BlockStatement bs = block((String) n.literal);
            if (typeInfo != null) {
                bs.setReturnType(typeInfo.dataType, typeInfo.recordType, typeInfo.bitmapType, typeInfo.intmapType);
            }
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
        } else if (match(EbsTokenType.SET)) {
            // 'set' keyword is optional syntactic sugar for screen variable assignment
            // Just consume it and parse the assignment statement
            return assignmentStatement();
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

    private Statement varDeclaration(boolean isConst) throws ParseError {
        EbsToken name = peek();
        
        // Check if the token is a keyword and reject it with a clear error message
        if (name.type.getCategory() == Category.KEYWORD) {
            throw error(name, "Cannot use keyword '" + name.literal + "' as a variable name.");
        }
        
        name = consume(EbsTokenType.IDENTIFIER, "Expected variable name.");
        DataType elemType = null;
        RecordType recordType = null;
        BitmapType bitmapType = null;
        IntmapType intmapType = null;
        Expression[] arrayDims = null;
        boolean consumedBraces = false; // Track if we consumed braces for record/bitmap/intmap fields
        boolean isQueueType = false; // Track if this is a queue type declaration
        boolean isSortedMap = false; // Track if this is a sorted map

        if (match(EbsTokenType.COLON)) {
            EbsToken t = peek();
            boolean handledScreenType = false; // Track if we handled screen.xxx syntax
            
            // Check for "sorted map" type modifier
            if (t.type == EbsTokenType.SORTED) {
                advance(); // consume 'sorted'
                EbsToken mapToken = peek();
                if (mapToken.type == EbsTokenType.MAP || 
                    (mapToken.literal instanceof String && "map".equals(((String)mapToken.literal).toLowerCase()))) {
                    isSortedMap = true;
                    elemType = DataType.MAP;
                    advance(); // consume 'map'
                } else {
                    throw error(mapToken, "Expected 'map' after 'sorted' type modifier.");
                }
            }
            // Special handling for "screen.xxx" type annotations
            // Since "screen" is a keyword, we need to check for SCREEN token followed by DOT
            else if (t.type == EbsTokenType.SCREEN && peekNext() != null && peekNext().type == EbsTokenType.DOT) {
                // Consume SCREEN token
                advance();
                // Consume DOT token
                consume(EbsTokenType.DOT, "Expected '.' after 'screen' in type annotation.");
                // Get the component type name (can be IDENTIFIER or DATATYPE like "canvas")
                EbsToken componentTypeToken = peek();
                if (componentTypeToken.type == EbsTokenType.IDENTIFIER || componentTypeToken.type == EbsTokenType.DATATYPE) {
                    advance(); // consume the component type token
                    String componentTypeName = (String) componentTypeToken.literal;
                    
                    // Treat screen component types as STRING since they hold the component's value
                    elemType = DataType.STRING;
                    handledScreenType = true; // Mark that we've handled all tokens
                } else {
                    throw error(componentTypeToken, "Expected component type name after 'screen.'.");
                }
            }
            // Check if this is a bitmap type definition
            // Check both token type and literal value since lexer might categorize it differently
            else if (t.type == EbsTokenType.BITMAP || 
                (t.literal instanceof String && "bitmap".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.BITMAP;
                advance(); // consume 'bitmap'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'bitmap' keyword.");
                
                // Parse bitmap fields
                bitmapType = parseBitmapFields();
                
                consume(EbsTokenType.RBRACE, "Expected '}' after bitmap field definitions.");
                consumedBraces = true;
            // Check if this is an intmap type definition
            // Check both token type and literal value since lexer might categorize it differently
            } else if (t.type == EbsTokenType.INTMAP || 
                (t.literal instanceof String && "intmap".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.INTMAP;
                advance(); // consume 'intmap'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'intmap' keyword.");
                
                // Parse intmap fields
                intmapType = parseIntmapFields();
                
                consume(EbsTokenType.RBRACE, "Expected '}' after intmap field definitions.");
                consumedBraces = true;
            // Check if this is a record type definition
            // Check both token type and literal value since lexer might categorize it differently
            } else if (t.type == EbsTokenType.RECORD || 
                (t.literal instanceof String && "record".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.RECORD;
                advance(); // consume 'record'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'record' keyword.");
                
                // Parse record fields
                recordType = parseRecordFields();
                
                consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
                consumedBraces = true;
            } else if (t.type.getDataType() != null) {
                elemType = t.type.getDataType();
            } else {
                if (t.type == EbsTokenType.DATATYPE || t.type == EbsTokenType.IDENTIFIER) {
                    String typeName = (String) t.literal;
                    
                    // Check if it's a qualified type name like "array.int" or "queue.string"
                    if (typeName.startsWith("array.")) {
                        String subType = typeName.substring(6); // Remove "array." prefix
                        switch (subType.toLowerCase()) {
                            case "any" -> elemType = DataType.ARRAY;
                            case "string" -> elemType = DataType.STRING;
                            case "byte" -> elemType = DataType.BYTE;
                            case "bitmap" -> elemType = DataType.BITMAP;
                            case "intmap" -> elemType = DataType.INTMAP;
                            case "int", "integer" -> elemType = DataType.INTEGER;
                            case "long" -> elemType = DataType.LONG;
                            case "float" -> elemType = DataType.FLOAT;
                            case "double", "number" -> elemType = DataType.DOUBLE;
                            case "bool", "boolean" -> elemType = DataType.BOOL;
                            case "date" -> elemType = DataType.DATE;
                            case "image" -> elemType = DataType.IMAGE;
                            case "record" -> {
                                elemType = DataType.RECORD;
                                // For array.record, field definitions come after array dimensions
                                // So don't parse them here, just note we have a record type
                            }
                            default -> throw error(t, "Unknown array element type: " + subType);
                        }
                    } else if (typeName.startsWith("queue.")) {
                        String subType = typeName.substring(6); // Remove "queue." prefix
                        isQueueType = true; // Mark as queue type
                        // For queue types, set elemType based on the subtype
                        // The QUEUE dataType will be set separately
                        switch (subType.toLowerCase()) {
                            case "any" -> elemType = DataType.QUEUE;
                            case "string" -> elemType = DataType.STRING;
                            case "byte" -> elemType = DataType.BYTE;
                            case "int", "integer" -> elemType = DataType.INTEGER;
                            case "long" -> elemType = DataType.LONG;
                            case "float" -> elemType = DataType.FLOAT;
                            case "double", "number" -> elemType = DataType.DOUBLE;
                            case "bool", "boolean" -> elemType = DataType.BOOL;
                            case "date" -> elemType = DataType.DATE;
                            case "image" -> elemType = DataType.IMAGE;
                            default -> throw error(t, "Unknown queue element type: " + subType);
                        }
                    } else if (typeName.toLowerCase().startsWith("screen.")) {
                        // Screen component type: screen.textArea, screen.button, etc.
                        // For now, we'll treat these as STRING type since they hold the component's value
                        // The actual component type information will be stored separately during screen creation
                        elemType = DataType.STRING;
                    } else {
                        // Check if it's a type alias
                        TypeRegistry.TypeAlias alias = TypeRegistry.getTypeAlias(typeName);
                        if (alias != null) {
                            // Use the aliased type
                            elemType = alias.dataType;
                            recordType = alias.recordType;
                            
                            // For array type aliases, we'll handle them specially below
                            // Just set a flag to indicate this is from an alias
                            boolean isAliasedArray = alias.isArray;
                            Integer aliasedArraySize = alias.arraySize;
                            
                            // Advance the token
                            advance();
                            
                            // If this is an array alias, we need to create the array expression
                            if (isAliasedArray) {
                                // Build array dimensions expression
                                if (aliasedArraySize != null) {
                                    Expression dimExpr = new LiteralExpression(DataType.INTEGER, aliasedArraySize);
                                    arrayDims = new Expression[]{dimExpr};
                                    
                                    // Create ArrayExpression with dimensions
                                    ArrayExpression varInit = new ArrayExpression(name.line, elemType, arrayDims, null);
                                    
                                    // Check for assignment
                                    if (match(EbsTokenType.EQUAL)) {
                                        Expression actualInit = expression();
                                        
                                        if (actualInit instanceof ArrayLiteralExpression) {
                                            varInit.initializer = (ArrayLiteralExpression) actualInit;
                                            ((ArrayLiteralExpression) actualInit).array = varInit;
                                        } else if (actualInit instanceof ArrayExpression) {
                                            varInit = (ArrayExpression) actualInit;
                                        }
                                    }
                                    
                                    consume(EbsTokenType.SEMICOLON, "Expected ';' after variable declaration.");
                                    
                                    // Return appropriate VarStatement
                                    if (recordType != null) {
                                        return new VarStatement(name.line, (String) name.literal, elemType, recordType, varInit, isConst);
                                    } else {
                                        return new VarStatement(name.line, (String) name.literal, elemType, varInit, isConst);
                                    }
                                } else {
                                    // Dynamic array - no dimensions, just handle assignment
                                    Expression varInit = null;
                                    if (match(EbsTokenType.EQUAL)) {
                                        varInit = expression();
                                    }
                                    
                                    consume(EbsTokenType.SEMICOLON, "Expected ';' after variable declaration.");
                                    
                                    // Return appropriate VarStatement
                                    if (recordType != null) {
                                        return new VarStatement(name.line, (String) name.literal, elemType, recordType, varInit, isConst);
                                    } else {
                                        return new VarStatement(name.line, (String) name.literal, elemType, varInit, isConst);
                                    }
                                }
                            }
                            // For non-array aliases, we've set the type info
                            // Don't advance again - already advanced at line 501
                        } else {
                            EbsTokenType tokenType = getTokenType(typeName);
                            if (tokenType != null && tokenType.getDataType() != null) {
                                elemType = tokenType.getDataType();
                            } else {
                                throw error(t, "Expected type name after ':'.");
                            }
                        }
                    }
                } else {
                    throw error(t, "Expected type name after ':'.");
                }
            }
            // Advance token for non-record types and non-aliases (record already consumed all its tokens)
            // Also don't advance if we already advanced for array.record case or type alias
            // Check if we haven't already processed this as a type alias
            boolean isAlias = (t.type == EbsTokenType.IDENTIFIER || t.type == EbsTokenType.DATATYPE) &&
                             t.literal instanceof String &&
                             TypeRegistry.hasTypeAlias((String) t.literal);
            
            if (t.type != EbsTokenType.RECORD && 
                t.type != EbsTokenType.BITMAP &&
                t.type != EbsTokenType.INTMAP &&
                t.type != EbsTokenType.SORTED && // Don't advance if we handled sorted map
                !(t.literal instanceof String && "record".equals(((String)t.literal).toLowerCase())) &&
                !(t.literal instanceof String && "bitmap".equals(((String)t.literal).toLowerCase())) &&
                !(t.literal instanceof String && "intmap".equals(((String)t.literal).toLowerCase())) &&
                !(t.literal instanceof String && ((String)t.literal).toLowerCase().startsWith("array.record")) &&
                !(t.literal instanceof String && ((String)t.literal).toLowerCase().startsWith("queue.")) &&
                !handledScreenType && // Don't advance if we already handled screen.xxx
                !isAlias) {
                advance();
            }
            
            // Check for array.type syntax when type is not already qualified
            // This handles cases where lexer separates array and type (array . int)
            if (elemType == DataType.ARRAY && match(EbsTokenType.DOT)) {
                EbsToken subType = peek();
                String subTypeName = null;
                
                // Handle both keyword types (int, string, etc.) and identifiers
                if (subType.type.getDataType() != null) {
                    elemType = subType.type.getDataType();
                    advance();
                } else if (subType.type == EbsTokenType.IDENTIFIER || subType.type == EbsTokenType.DATATYPE) {
                    subTypeName = (String) subType.literal;
                    advance();
                    
                    // Map common type names
                    switch (subTypeName.toLowerCase()) {
                        case "any" -> elemType = DataType.ARRAY;  // array.any is same as array
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "bitmap" -> elemType = DataType.BITMAP;
                        case "intmap" -> elemType = DataType.INTMAP;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        case "record" -> {
                            elemType = DataType.RECORD;
                            // For array.record, field definitions come after array dimensions
                            // So don't parse them here, just note we have a record type
                        }
                        default -> throw error(subType, "Unknown array element type: " + subTypeName);
                    }
                } else {
                    throw error(subType, "Expected type name after 'array.'");
                }
            }
            
            // Check for queue.type syntax when type is not already qualified
            // This handles cases where lexer separates queue and type (queue . string)
            if (elemType == DataType.QUEUE && match(EbsTokenType.DOT)) {
                isQueueType = true; // Mark as queue type
                EbsToken subType = peek();
                String subTypeName = null;
                
                // Handle both keyword types (int, string, etc.) and identifiers
                if (subType.type.getDataType() != null) {
                    elemType = subType.type.getDataType();
                    advance();
                } else if (subType.type == EbsTokenType.IDENTIFIER || subType.type == EbsTokenType.DATATYPE) {
                    subTypeName = (String) subType.literal;
                    advance();
                    
                    // Map common type names for queue element type
                    switch (subTypeName.toLowerCase()) {
                        case "any" -> elemType = DataType.QUEUE;  // queue.any is generic queue
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        default -> throw error(subType, "Unknown queue element type: " + subTypeName);
                    }
                } else {
                    throw error(subType, "Expected type name after 'queue.'");
                }
            }
            
            // Check for array dimensions after type or after record field definitions
            if (check(EbsTokenType.LBRACKET)) {
                arrayDims = parseArrayDimensions();
            }
            
            // For array.record with inline field definitions, parse them after dimensions (if any)
            // Syntax: array.record{...} or array.record[size]{...}
            // Only do this if we haven't already consumed braces (standalone record case)
            if (elemType == DataType.RECORD && !consumedBraces && check(EbsTokenType.LBRACE)) {
                consume(EbsTokenType.LBRACE, "Expected '{' for record field definitions.");
                recordType = parseRecordFields();
                consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
            }
        }


        Expression varInit = null;

        if (isQueueType) {
            // For queue types, create a QueueExpression
            varInit = new QueueExpression(name.line, elemType);
        } else if (arrayDims != null && arrayDims.length > 0) {
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
            } else if (elemType == DataType.MAP && check(EbsTokenType.LBRACE)) {
                // For map types, parse JSON object literal (maps only accept objects, not arrays)
                varInit = parseJsonLiteralFromSource();
            } else if (elemType == DataType.RECORD && check(EbsTokenType.LBRACE)) {
                // For record types, parse JSON object literal
                varInit = parseJsonLiteralFromSource();
            } else if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
                // Allow array literals in these cases:
                // 1. Variable was declared with array dimensions (varInit is ArrayExpression)
                // 2. No type specified (type inference from literal)
                // 3. Array type specified but no dimensions
                if (varInit instanceof ArrayExpression) {
                    ArrayLiteralExpression arrayInit = parseArrayLiteral();
                    ((ArrayExpression) varInit).initializer = arrayInit;
                    arrayInit.array = varInit;
                } else if (elemType == null || elemType == DataType.ARRAY) {
                    // Type inference or generic array - just parse the literal
                    varInit = parseArrayLiteral();
                } else {
                    throw error(eq, "Variable not defined as an array, cannot use {} for non-arrays.");
                }
            } else {
                varInit = expression();
            }
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after variable declaration.");
        
        // Return appropriate VarStatement based on whether it's a record, bitmap, or intmap type
        if (intmapType != null) {
            return new VarStatement(name.line, (String) name.literal, elemType, intmapType, varInit, isConst);
        } else if (bitmapType != null) {
            return new VarStatement(name.line, (String) name.literal, elemType, bitmapType, varInit, isConst);
        } else if (recordType != null) {
            return new VarStatement(name.line, (String) name.literal, elemType, recordType, varInit, isConst);
        } else {
            return new VarStatement(name.line, (String) name.literal, elemType, varInit, isConst, isSortedMap);
        }

    }

    /**
     * Parse typedef statement: typename typeof type_definition
     * Examples:
     *   atype typeof array.record{id: int, name: string}
     *   personType typeof record{name: string, age: int}
     *   intArray typeof array.int[10]
     */
    private Statement typedefStatement() throws ParseError {
        // Get the type name - matchAll only checks, doesn't consume
        EbsToken typeName = peek();
        advance(); // consume the type name
        
        // Consume 'typeof' keyword
        consume(EbsTokenType.TYPEOF, "Expected 'typeof' in type definition.");
        
        // Parse the type definition (similar to varDeclaration logic)
        DataType elemType = null;
        RecordType recordType = null;
        BitmapType bitmapType = null;
        IntmapType intmapType = null;
        boolean isArray = false;
        Integer arraySize = null;
        Expression arrayInit = null;
        
        // Check for array.type syntax
        if (check(EbsTokenType.ARRAY) || check(EbsTokenType.IDENTIFIER) || check(EbsTokenType.DATATYPE) || 
            check(EbsTokenType.RECORD) || check(EbsTokenType.BITMAP) || check(EbsTokenType.INTMAP)) {
            EbsToken typeToken = peek();
            
            // Check if it's "array" keyword or identifier starting with "array"
            boolean isArrayToken = typeToken.type == EbsTokenType.ARRAY ||
                                  (typeToken.literal instanceof String && ((String) typeToken.literal).toLowerCase().startsWith("array"));
            
            if (isArrayToken) {
                isArray = true;
                advance(); // consume 'array'
                
                // Expect dot
                if (check(EbsTokenType.DOT)) {
                    advance();
                } else {
                    throw error(typeToken, "Expected '.' after 'array' in type definition.");
                }
                
                // Parse element type
                EbsToken elemTypeToken = peek();
                
                // Check if element type is 'record'
                if (elemTypeToken.type == EbsTokenType.RECORD || 
                    (elemTypeToken.literal instanceof String && "record".equals(((String)elemTypeToken.literal).toLowerCase()))) {
                    elemType = DataType.RECORD;
                    advance(); // consume 'record'
                    
                    // Check for field definitions
                    if (check(EbsTokenType.LBRACKET)) {
                        // Has dimensions: array.record[size] or array.record[*]
                        advance(); // consume '['
                        if (check(EbsTokenType.STAR)) {
                            advance(); // dynamic array
                            arraySize = null;
                        } else {
                            EbsToken sizeToken = consume(EbsTokenType.INTEGER, "Expected array size or '*'.");
                            arraySize = (Integer) sizeToken.literal;
                        }
                        consume(EbsTokenType.RBRACKET, "Expected ']' after array dimension.");
                    }
                    
                    // Parse record fields if present
                    if (check(EbsTokenType.LBRACE)) {
                        advance(); // consume '{'
                        recordType = parseRecordFields();
                        consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
                    }
                } else {
                    // Regular type
                    if (elemTypeToken.type.getDataType() != null) {
                        elemType = elemTypeToken.type.getDataType();
                        advance();
                    } else if (elemTypeToken.type == EbsTokenType.IDENTIFIER || elemTypeToken.type == EbsTokenType.DATATYPE) {
                        String typeLiteral = (String) elemTypeToken.literal;
                        EbsTokenType tokenType = getTokenType(typeLiteral);
                        if (tokenType != null && tokenType.getDataType() != null) {
                            elemType = tokenType.getDataType();
                            advance();
                        } else {
                            throw error(elemTypeToken, "Unknown type '" + typeLiteral + "' in type definition.");
                        }
                    }
                    
                    // Parse array dimensions if present
                    if (check(EbsTokenType.LBRACKET)) {
                        advance(); // consume '['
                        if (check(EbsTokenType.STAR)) {
                            advance(); // dynamic array
                            arraySize = null;
                        } else {
                            EbsToken sizeToken = consume(EbsTokenType.INTEGER, "Expected array size or '*'.");
                            arraySize = (Integer) sizeToken.literal;
                        }
                        consume(EbsTokenType.RBRACKET, "Expected ']' after array dimension.");
                    }
                }
            } else if (typeToken.type == EbsTokenType.RECORD || 
                      (typeToken.literal instanceof String && "record".equals(((String)typeToken.literal).toLowerCase()))) {
                // Standalone record type
                elemType = DataType.RECORD;
                advance(); // consume 'record'
                
                // Parse record fields
                if (check(EbsTokenType.LBRACE)) {
                    advance(); // consume '{'
                    recordType = parseRecordFields();
                    consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
                }
            } else if (typeToken.type == EbsTokenType.BITMAP || 
                      (typeToken.literal instanceof String && "bitmap".equals(((String)typeToken.literal).toLowerCase()))) {
                // Standalone bitmap type
                elemType = DataType.BITMAP;
                advance(); // consume 'bitmap'
                
                // Parse bitmap fields
                if (check(EbsTokenType.LBRACE)) {
                    advance(); // consume '{'
                    bitmapType = parseBitmapFields();
                    consume(EbsTokenType.RBRACE, "Expected '}' after bitmap field definitions.");
                }
            } else if (typeToken.type == EbsTokenType.INTMAP || 
                      (typeToken.literal instanceof String && "intmap".equals(((String)typeToken.literal).toLowerCase()))) {
                // Standalone intmap type
                elemType = DataType.INTMAP;
                advance(); // consume 'intmap'
                
                // Parse intmap fields
                if (check(EbsTokenType.LBRACE)) {
                    advance(); // consume '{'
                    intmapType = parseIntmapFields();
                    consume(EbsTokenType.RBRACE, "Expected '}' after intmap field definitions.");
                }
            } else {
                // Simple type
                if (typeToken.type.getDataType() != null) {
                    elemType = typeToken.type.getDataType();
                    advance();
                } else {
                    throw error(typeToken, "Expected type name in type definition.");
                }
            }
        }
        
        consume(EbsTokenType.SEMICOLON, "Expected ';' after type definition.");
        
        // Register the type alias immediately during parsing (not during execution)
        // This allows subsequent variable declarations to use the type alias
        TypeRegistry.TypeAlias alias = new TypeRegistry.TypeAlias(
            (String) typeName.literal,
            elemType,
            recordType,
            bitmapType,
            intmapType,
            isArray,
            arraySize
        );
        TypeRegistry.registerTypeAlias(alias);
        
        // Create TypedefStatement
        if (isArray) {
            return new TypedefStatement(typeName.line, (String) typeName.literal, elemType, recordType, arraySize);
        } else if (intmapType != null) {
            return new TypedefStatement(typeName.line, (String) typeName.literal, intmapType);
        } else if (bitmapType != null) {
            return new TypedefStatement(typeName.line, (String) typeName.literal, bitmapType);
        } else if (recordType != null) {
            return new TypedefStatement(typeName.line, (String) typeName.literal, recordType);
        } else {
            return new TypedefStatement(typeName.line, (String) typeName.literal, elemType);
        }
    }

    /**
     * Parse record field definitions inside { }
     * Expected format: fieldName: type, fieldName: type, ...
     * Supports nested records: fieldName: record { ... }
     */
    private RecordType parseRecordFields() throws ParseError {
        RecordType recordType = new RecordType();
        
        // Parse field definitions until we hit the closing brace
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            // Parse field name
            EbsToken fieldName = consume(EbsTokenType.IDENTIFIER, "Expected field name in record definition.");
            
            // Expect colon
            consume(EbsTokenType.COLON, "Expected ':' after field name in record definition.");
            
            // Parse field type
            EbsToken fieldTypeToken = peek();
            DataType fieldType = null;
            RecordType nestedRecordType = null;
            
            // Check if this is a nested record type
            if (fieldTypeToken.type == EbsTokenType.RECORD || 
                (fieldTypeToken.literal instanceof String && "record".equals(((String)fieldTypeToken.literal).toLowerCase()))) {
                // This is a nested record - parse it recursively
                advance(); // consume 'record'
                consume(EbsTokenType.LBRACE, "Expected '{' after 'record' keyword in nested record definition.");
                nestedRecordType = parseRecordFields(); // Recursive call
                consume(EbsTokenType.RBRACE, "Expected '}' after nested record field definitions.");
                fieldType = DataType.RECORD;
            } else if (fieldTypeToken.type.getDataType() != null) {
                fieldType = fieldTypeToken.type.getDataType();
                advance();
            } else if (fieldTypeToken.type == EbsTokenType.IDENTIFIER || fieldTypeToken.type == EbsTokenType.DATATYPE) {
                String typeName = (String) fieldTypeToken.literal;
                EbsTokenType tokenType = getTokenType(typeName);
                if (tokenType != null && tokenType.getDataType() != null) {
                    fieldType = tokenType.getDataType();
                    advance();
                } else {
                    // Check if this is a user-defined type alias (typedef)
                    TypeRegistry.TypeAlias typeAlias = TypeRegistry.getTypeAlias(typeName);
                    if (typeAlias != null) {
                        // This is a type alias - use its type based on what it represents
                        if (typeAlias.recordType != null) {
                            nestedRecordType = typeAlias.recordType;
                            fieldType = DataType.RECORD;
                        } else if (typeAlias.bitmapType != null) {
                            // Bitmap types are stored as BYTE in records
                            fieldType = DataType.BYTE;
                        } else if (typeAlias.dataType != null) {
                            fieldType = typeAlias.dataType;
                        } else {
                            // Safety check: TypeAlias constructor guarantees at least one type is non-null,
                            // but this guards against potential TypeRegistry corruption or concurrent modification
                            throw error(fieldTypeToken, "Malformed type alias '" + typeName + "' - missing type definition.");
                        }
                        advance();
                    } else {
                        throw error(fieldTypeToken, "Unknown type '" + typeName + "' in record field definition.");
                    }
                }
            } else {
                throw error(fieldTypeToken, "Expected type name after ':' in record field definition.");
            }
            
            // Add field to record type
            if (nestedRecordType != null) {
                recordType.addNestedRecord((String) fieldName.literal, nestedRecordType);
            } else {
                recordType.addField((String) fieldName.literal, fieldType);
            }
            
            // Check for comma (more fields) or closing brace (end of record)
            if (check(EbsTokenType.COMMA)) {
                advance(); // consume comma
            } else if (!check(EbsTokenType.RBRACE)) {
                throw error(peek(), "Expected ',' or '}' in record definition.");
            }
        }
        
        return recordType;
    }

    /**
     * Parse bitmap field definitions inside { }
     * Expected format: fieldName: startBit-endBit, fieldName: bit, ...
     * Examples:
     *   status: 0-1    (2 bits at positions 0-1, values 0-3)
     *   enabled: 2     (1 bit at position 2, values 0-1)
     *   priority: 3-5  (3 bits at positions 3-5, values 0-7)
     *   reserved: 6-7  (2 bits at positions 6-7, values 0-3)
     */
    private BitmapType parseBitmapFields() throws ParseError {
        BitmapType bitmapType = new BitmapType();
        
        // Parse field definitions until we hit the closing brace
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            // Parse field name
            EbsToken fieldName = consume(EbsTokenType.IDENTIFIER, "Expected field name in bitmap definition.");
            
            // Expect colon
            consume(EbsTokenType.COLON, "Expected ':' after field name in bitmap definition.");
            
            // Parse bit position or range
            EbsToken startBitToken = consume(EbsTokenType.INTEGER, "Expected bit position (0-7) after ':' in bitmap definition.");
            int startBit = (Integer) startBitToken.literal;
            
            if (startBit < 0 || startBit > 7) {
                throw error(startBitToken, "Bit position must be 0-7, got: " + startBit);
            }
            
            int endBit = startBit; // Default: single bit
            
            // Check for range: startBit-endBit
            if (match(EbsTokenType.MINUS)) {
                EbsToken endBitToken = consume(EbsTokenType.INTEGER, "Expected end bit position (0-7) after '-' in bitmap definition.");
                endBit = (Integer) endBitToken.literal;
                
                if (endBit < 0 || endBit > 7) {
                    throw error(endBitToken, "End bit position must be 0-7, got: " + endBit);
                }
                
                if (startBit > endBit) {
                    throw error(endBitToken, "Start bit (" + startBit + ") must be <= end bit (" + endBit + ")");
                }
            }
            
            // Add field to bitmap type
            try {
                bitmapType.addField((String) fieldName.literal, startBit, endBit);
            } catch (IllegalArgumentException e) {
                throw error(fieldName, e.getMessage());
            }
            
            // Check for comma (more fields) or closing brace (end of bitmap)
            if (check(EbsTokenType.COMMA)) {
                advance(); // consume comma
            } else if (!check(EbsTokenType.RBRACE)) {
                throw error(peek(), "Expected ',' or '}' in bitmap definition.");
            }
        }
        
        return bitmapType;
    }

    /**
     * Parse intmap field definitions inside { }
     * Expected format: fieldName: startBit-endBit, fieldName: bit, ...
     * Examples:
     *   status: 0-1      (2 bits at positions 0-1, values 0-3)
     *   enabled: 2       (1 bit at position 2, values 0-1)
     *   priority: 3-7    (5 bits at positions 3-7, values 0-31)
     *   flags: 8-15      (8 bits at positions 8-15, values 0-255)
     *   reserved: 16-31  (16 bits at positions 16-31, values 0-65535)
     */
    private IntmapType parseIntmapFields() throws ParseError {
        IntmapType intmapType = new IntmapType();
        
        // Parse field definitions until we hit the closing brace
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            // Parse field name
            EbsToken fieldName = consume(EbsTokenType.IDENTIFIER, "Expected field name in intmap definition.");
            
            // Expect colon
            consume(EbsTokenType.COLON, "Expected ':' after field name in intmap definition.");
            
            // Parse bit position or range
            EbsToken startBitToken = consume(EbsTokenType.INTEGER, "Expected bit position (0-31) after ':' in intmap definition.");
            int startBit = (Integer) startBitToken.literal;
            
            if (startBit < 0 || startBit > 31) {
                throw error(startBitToken, "Bit position must be 0-31, got: " + startBit);
            }
            
            int endBit = startBit; // Default: single bit
            
            // Check for range: startBit-endBit
            if (match(EbsTokenType.MINUS)) {
                EbsToken endBitToken = consume(EbsTokenType.INTEGER, "Expected end bit position (0-31) after '-' in intmap definition.");
                endBit = (Integer) endBitToken.literal;
                
                if (endBit < 0 || endBit > 31) {
                    throw error(endBitToken, "End bit position must be 0-31, got: " + endBit);
                }
                
                if (startBit > endBit) {
                    throw error(endBitToken, "Start bit (" + startBit + ") must be <= end bit (" + endBit + ")");
                }
            }
            
            // Add field to intmap type
            try {
                intmapType.addField((String) fieldName.literal, startBit, endBit);
            } catch (IllegalArgumentException e) {
                throw error(fieldName, e.getMessage());
            }
            
            // Check for comma (more fields) or closing brace (end of intmap)
            if (check(EbsTokenType.COMMA)) {
                advance(); // consume comma
            } else if (!check(EbsTokenType.RBRACE)) {
                throw error(peek(), "Expected ',' or '}' in intmap definition.");
            }
        }
        
        return intmapType;
    }

    /**
     * Process builtin calls (#builtin.method(...)) in JSON text by evaluating them
     * and replacing with their result values.
     */
    private String processBuiltinCallsInJson(String jsonText) throws ParseError {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        
        while (pos < jsonText.length()) {
            // Look for # character that starts a builtin call
            int hashPos = jsonText.indexOf('#', pos);
            if (hashPos == -1) {
                // No more builtin calls
                result.append(jsonText.substring(pos));
                break;
            }
            
            // Append everything before the #
            result.append(jsonText.substring(pos, hashPos));
            
            // Try to parse the builtin call starting at hashPos
            try {
                // Extract the builtin call expression
                int callEnd = findBuiltinCallEnd(jsonText, hashPos);
                if (callEnd == -1) {
                    // Not a valid builtin call, just append the # and continue
                    result.append('#');
                    pos = hashPos + 1;
                    continue;
                }
                
                String builtinCall = jsonText.substring(hashPos, callEnd);
                
                // Parse and evaluate the builtin call
                // Remove the leading # to get the actual call syntax
                String callText = builtinCall.substring(1); // Remove #
                
                // Tokenize just this expression
                List<EbsToken> callTokens = lexer.tokenize(callText);
                
                // Create a temporary parser for this expression
                Parser tempParser = new Parser(callText, callTokens);
                // Initialize the parser state
                tempParser.current = 0;
                tempParser.currToken = callTokens.get(0);
                
                // Parse the call as an expression
                // The call syntax is: qualifiedName(params)
                // tempParser is positioned at the start
                Expression expr = tempParser.parseBuiltinCallExpression();
                
                // Evaluate parameters
                CallExpression callExpr = (CallExpression) expr;
                CallStatement callStmt = callExpr.call;
                String functionName = callStmt.name.toLowerCase();
                
                // Evaluate parameter expressions to get actual values
                List<Object> argValues = new ArrayList<>();
                for (Parameter param : callStmt.parameters) {
                    // For literal values, extract directly
                    if (param.value instanceof LiteralExpression litExpr) {
                        argValues.add(litExpr.value);
                    } else {
                        // For more complex expressions, we'd need an interpreter
                        // For now, throw an error
                        throw new ParseError("Complex expressions in JSON builtin calls are not yet supported: " + param.value);
                    }
                }
                
                // Call the builtin function directly
                com.eb.script.interpreter.Environment env = new com.eb.script.interpreter.Environment();
                Object value = Builtins.callBuiltin(env, functionName, argValues.toArray());
                
                // Convert value to JSON representation
                String jsonValue = valueToJson(value);
                result.append(jsonValue);
                
                pos = callEnd;
            } catch (Exception e) {
                // If we can't parse/evaluate, treat # as literal
                result.append('#');
                pos = hashPos + 1;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Find the end position of a builtin call starting at hashPos.
     * Returns -1 if not a valid builtin call pattern.
     */
    private int findBuiltinCallEnd(String text, int hashPos) {
        int pos = hashPos + 1; // Skip #
        
        // Must have identifier after #
        if (pos >= text.length() || !Character.isJavaIdentifierStart(text.charAt(pos))) {
            return -1;
        }
        
        // Read first identifier
        while (pos < text.length() && Character.isJavaIdentifierPart(text.charAt(pos))) {
            pos++;
        }
        
        // Expect dot for qualified name
        if (pos >= text.length() || text.charAt(pos) != '.') {
            return -1;
        }
        pos++; // Skip dot
        
        // Must have second identifier
        if (pos >= text.length() || !Character.isJavaIdentifierStart(text.charAt(pos))) {
            return -1;
        }
        
        // Read second identifier
        while (pos < text.length() && Character.isJavaIdentifierPart(text.charAt(pos))) {
            pos++;
        }
        
        // Expect opening paren
        if (pos >= text.length() || text.charAt(pos) != '(') {
            return -1;
        }
        pos++; // Skip (
        
        // Find matching closing paren, tracking nested parens
        int depth = 1;
        boolean inString = false;
        char stringChar = 0;
        
        while (pos < text.length() && depth > 0) {
            char c = text.charAt(pos);
            
            if (inString) {
                if (c == '\\') {
                    pos++; // Skip escape sequence
                } else if (c == stringChar) {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = c;
                } else if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
            }
            pos++;
        }
        
        if (depth != 0) {
            return -1; // Unmatched parens
        }
        
        return pos;
    }
    
    /**
     * Convert a value to its JSON representation.
     */
    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            // Escape and quote string
            return Json.compactJson(value);
        } else if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        } else if (value instanceof Map || value instanceof List || value instanceof com.eb.script.arrays.ArrayDef) {
            // Use Json.compactJson for complex types
            return Json.compactJson(value);
        } else {
            // Default: convert to string and escape
            return Json.compactJson(value.toString());
        }
    }

    /**
     * Helper method to detect if we're looking at a JSON object literal.
     * JSON objects have the pattern: { "key": value, ... } or { key: value, ... }
     * whereas array literals have: { value, value, ... }
     * This method looks ahead to see if there's a COLON at depth 1 before a COMMA or closing brace.
     */
    private boolean looksLikeJsonObject() {
        if (!check(EbsTokenType.LBRACE)) {
            return false;
        }
        
        // Save current position
        int savedCurrent = current;
        EbsToken savedCurrToken = currToken;
        EbsToken savedPrevToken = prevToken;
        
        try {
            // Skip opening brace
            advance();
            int depth = 1;
            
            // Check for empty braces {} immediately after opening brace
            if (check(EbsTokenType.RBRACE)) {
                // Empty braces {} should be treated as an empty JSON object
                return true;
            }
            
            // Look ahead to find a colon at depth 1
            while (!isAtEnd() && depth > 0) {
                if (check(EbsTokenType.LBRACE) || check(EbsTokenType.LBRACKET)) {
                    depth++;
                    advance();
                } else if (check(EbsTokenType.RBRACE) || check(EbsTokenType.RBRACKET)) {
                    depth--;
                    if (depth == 0) {
                        // Reached closing brace without finding colon at depth 1
                        return false;
                    }
                    advance();
                } else if (check(EbsTokenType.COLON) && depth == 1) {
                    // Found colon at depth 1 - this is a JSON object
                    return true;
                } else {
                    advance();
                }
            }
            
            return false;
        } finally {
            // Restore position
            current = savedCurrent;
            currToken = savedCurrToken;
            prevToken = savedPrevToken;
        }
    }
    
    /**
     * Helper method to detect if we're looking at a JSON array (array containing JSON objects).
     * JSON arrays have the pattern: [ {...}, {...}, ... ]
     * This method only checks if the first element after '[' is a JSON object.
     * It's designed to handle the common case where JSON arrays contain homogeneous object elements.
     * Arrays like [1, 2, 3] or mixed arrays like [{}, 1, 2] are treated as regular array literals.
     */
    private boolean looksLikeJsonArray() {
        if (!check(EbsTokenType.LBRACKET)) {
            return false;
        }
        
        // Save current position
        int savedCurrent = current;
        EbsToken savedCurrToken = currToken;
        EbsToken savedPrevToken = prevToken;
        
        try {
            // Skip opening bracket
            advance();
            
            // Check if the first element is a JSON object
            if (check(EbsTokenType.LBRACE)) {
                return looksLikeJsonObject();
            }
            
            return false;
        } finally {
            // Restore position to before the opening bracket
            current = savedCurrent;
            currToken = savedCurrToken;
            prevToken = savedPrevToken;
        }
    }

    /**
     * Parse a record literal with unquoted keys: TypeName { field: value, field: value }
     * This provides a cleaner, more readable syntax for record initialization.
     * Example: ChessPiece { piece: "R", color: "B", pos: posType { x: 0, y: 0 } }
     */
    private LiteralExpression parseRecordLiteral() throws ParseError {
        consume(EbsTokenType.LBRACE, "Expected '{' to start record literal.");
        
        StringBuilder jsonBuilder = new StringBuilder("{");
        boolean first = true;
        
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            if (!first) {
                jsonBuilder.append(",");
            }
            first = false;
            
            // Parse field name (unquoted identifier)
            EbsToken fieldName = consume(EbsTokenType.IDENTIFIER, "Expected field name in record literal.");
            consume(EbsTokenType.COLON, "Expected ':' after field name in record literal.");
            
            // Add field name with quotes to JSON (escape field name for JSON safety)
            String fieldNameStr = fieldName.literal != null ? (String) fieldName.literal : "";
            jsonBuilder.append("\"").append(escapeJsonString(fieldNameStr)).append("\":");
            
            // Parse field value - could be another record literal, string, number, etc.
            String fieldValue = parseRecordFieldValue();
            jsonBuilder.append(fieldValue);
            
            // Optional comma between fields
            if (check(EbsTokenType.COMMA)) {
                advance();
            }
        }
        
        consume(EbsTokenType.RBRACE, "Expected '}' to end record literal.");
        jsonBuilder.append("}");
        
        // Parse the constructed JSON string
        String jsonText = jsonBuilder.toString();
        Object value = Json.parse(jsonText, false);  // false = case-sensitive field names
        return new com.eb.script.interpreter.expression.LiteralExpression(com.eb.script.token.DataType.JSON, value);
    }
    
    /**
     * Escape a string for safe inclusion in JSON.
     * Handles quotes, backslashes, and control characters.
     */
    private String escapeJsonString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Parse a field value in a record literal, which could be:
     * - A string literal (quoted)
     * - A number
     * - A boolean
     * - A nested record literal (TypeName { ... })
     * - An identifier followed by { } (recursive record literal)
     */
    private String parseRecordFieldValue() throws ParseError {
        // Check if this is a nested record literal: identifier followed by {
        if (check(EbsTokenType.IDENTIFIER)) {
            int savedCurrent = current;
            EbsToken savedCurrToken = currToken;
            EbsToken savedPrevToken = prevToken;
            
            EbsToken identifier = advance();
            if (check(EbsTokenType.LBRACE)) {
                // Validate that the identifier is a type alias
                String typeName = identifier.literal != null ? (String) identifier.literal : "";
                TypeRegistry.TypeAlias alias = TypeRegistry.getTypeAlias(typeName);
                if (alias == null) {
                    throw error(identifier, "Unknown type '" + typeName + "' in record literal. Only type aliases can be used.");
                }
                // This is a nested record literal, parse it recursively
                LiteralExpression nestedRecord = parseRecordLiteral();
                // Convert the parsed JSON object back to string
                return Json.compactJson(nestedRecord.value);
            } else {
                // Not a record literal, restore position and treat as error
                current = savedCurrent;
                currToken = savedCurrToken;
                prevToken = savedPrevToken;
                throw error(peek(), "Expected literal value in record literal. Variables are not allowed.");
            }
        }
        
        // String literal
        if (match(EbsTokenType.QUOTE1, EbsTokenType.QUOTE2)) {
            EbsToken quoteType = previous();
            if (match(EbsTokenType.STRING, EbsTokenType.DATE)) {
                Object literalValue = previous().literal;
                String strValue = literalValue != null ? (String) literalValue : "";
                consume(quoteType.type, "Expected closing quote.");
                // Use proper JSON string escaping
                return "\"" + escapeJsonString(strValue) + "\"";
            } else {
                throw error(peek(), "Expected string value after opening quote.");
            }
        }
        
        // Number literals
        if (match(EbsTokenType.INTEGER)) {
            return previous().literal.toString();
        }
        if (match(EbsTokenType.LONG)) {
            return previous().literal.toString();
        }
        if (match(EbsTokenType.FLOAT)) {
            return previous().literal.toString();
        }
        if (match(EbsTokenType.DOUBLE)) {
            return previous().literal.toString();
        }
        
        // Boolean literals
        if (match(EbsTokenType.BOOL_TRUE)) {
            return "true";
        }
        if (match(EbsTokenType.BOOL_FALSE)) {
            return "false";
        }
        
        // Null literal
        if (match(EbsTokenType.NULL)) {
            return "null";
        }
        
        throw error(peek(), "Expected literal value (string, number, boolean, null, or nested record) in record literal.");
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

        // Process builtin calls before parsing JSON
        jsonText = processBuiltinCallsInJson(jsonText);

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
        Expression value = null;
        // Check if there's a semicolon immediately after return (return;)
        if (!check(EbsTokenType.SEMICOLON)) {
            value = expression();
        }
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

    private ReturnTypeInfo blockParameterReturn() throws ParseError {
        EbsToken ret = consumeOptional(EbsTokenType.RETURN);
        if (ret != null) {
            // Parse return type similar to how varDeclaration parses types
            DataType elemType = null;
            RecordType recordType = null;
            BitmapType bitmapType = null;
            IntmapType intmapType = null;
            boolean consumedBraces = false;
            
            EbsToken t = peek();
            EbsTokenType tokenType = t.type;
            
            // Check for bitmap type
            if (tokenType == EbsTokenType.BITMAP || 
                (t.literal instanceof String && "bitmap".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.BITMAP;
                advance(); // consume 'bitmap'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'bitmap' keyword.");
                bitmapType = parseBitmapFields();
                consume(EbsTokenType.RBRACE, "Expected '}' after bitmap field definitions.");
                consumedBraces = true;
                
                return new ReturnTypeInfo(elemType, null, bitmapType, null);
            }
            
            // Check for intmap type
            if (tokenType == EbsTokenType.INTMAP || 
                (t.literal instanceof String && "intmap".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.INTMAP;
                advance(); // consume 'intmap'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'intmap' keyword.");
                intmapType = parseIntmapFields();
                consume(EbsTokenType.RBRACE, "Expected '}' after intmap field definitions.");
                consumedBraces = true;
                
                return new ReturnTypeInfo(elemType, null, null, intmapType);
            }
            
            // Check for record type
            if (tokenType == EbsTokenType.RECORD || 
                (t.literal instanceof String && "record".equals(((String)t.literal).toLowerCase()))) {
                elemType = DataType.RECORD;
                advance(); // consume 'record'
                
                // Expect opening brace for field definitions
                consume(EbsTokenType.LBRACE, "Expected '{' after 'record' keyword.");
                recordType = parseRecordFields();
                consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
                consumedBraces = true;
                
                return new ReturnTypeInfo(elemType, recordType, null, null);
            }
            
            // Check if it's a datatype keyword token
            if (tokenType.getDataType() != null) {
                elemType = tokenType.getDataType();
                advance();
            }
            // Handle DATATYPE token (datatype keywords like "string", "int", etc.)
            else if (tokenType == EbsTokenType.DATATYPE) {
                advance();
                EbsTokenType type = getTokenType((String) t.literal);
                if (type != null) {
                    elemType = type.getDataType();
                }
            }
            // Handle IDENTIFIER token (might be type name or qualified type)
            else if (tokenType == EbsTokenType.IDENTIFIER) {
                String typeName = (String) t.literal;
                
                // Check for qualified type names like "array.int", "array.record", "queue.string"
                if (typeName.startsWith("array.")) {
                    String subType = typeName.substring(6); // Remove "array." prefix
                    switch (subType.toLowerCase()) {
                        case "any" -> elemType = DataType.ARRAY;
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "bitmap" -> elemType = DataType.BITMAP;
                        case "intmap" -> elemType = DataType.INTMAP;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        case "record" -> {
                            elemType = DataType.RECORD;
                            // For array.record, field definitions come after
                        }
                        default -> throw error(t, "Unknown array element type: " + subType);
                    }
                    advance();
                } else if (typeName.startsWith("queue.")) {
                    String subType = typeName.substring(6); // Remove "queue." prefix
                    switch (subType.toLowerCase()) {
                        case "any" -> elemType = DataType.QUEUE;
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        default -> throw error(t, "Unknown queue element type: " + subType);
                    }
                    advance();
                } else {
                    // Check if it's a type alias
                    TypeRegistry.TypeAlias alias = TypeRegistry.getTypeAlias(typeName);
                    if (alias != null) {
                        elemType = alias.dataType;
                        recordType = alias.recordType;
                        advance();
                        return new ReturnTypeInfo(elemType, recordType, null, null);
                    }
                    
                    // Look up as a regular type name
                    EbsTokenType type = getTokenType(typeName);
                    if (type != null) {
                        elemType = type.getDataType();
                        advance();
                    } else {
                        throw error(t, "Unknown type name: " + typeName);
                    }
                }
            }
            
            // Check for array.type or queue.type syntax when type is not already qualified
            if (elemType == DataType.ARRAY && match(EbsTokenType.DOT)) {
                EbsToken subType = peek();
                
                if (subType.type.getDataType() != null) {
                    elemType = subType.type.getDataType();
                    advance();
                } else if (subType.type == EbsTokenType.IDENTIFIER || subType.type == EbsTokenType.DATATYPE) {
                    String subTypeName = (String) subType.literal;
                    advance();
                    
                    switch (subTypeName.toLowerCase()) {
                        case "any" -> elemType = DataType.ARRAY;
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "bitmap" -> elemType = DataType.BITMAP;
                        case "intmap" -> elemType = DataType.INTMAP;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        case "record" -> elemType = DataType.RECORD;
                        default -> throw error(subType, "Unknown array element type: " + subTypeName);
                    }
                } else {
                    throw error(subType, "Expected type name after 'array.'");
                }
            }
            
            // Check for queue.type syntax when type is not already qualified
            if (elemType == DataType.QUEUE && match(EbsTokenType.DOT)) {
                EbsToken subType = peek();
                
                if (subType.type.getDataType() != null) {
                    elemType = subType.type.getDataType();
                    advance();
                } else if (subType.type == EbsTokenType.IDENTIFIER || subType.type == EbsTokenType.DATATYPE) {
                    String subTypeName = (String) subType.literal;
                    advance();
                    
                    switch (subTypeName.toLowerCase()) {
                        case "any" -> elemType = DataType.QUEUE;
                        case "string" -> elemType = DataType.STRING;
                        case "byte" -> elemType = DataType.BYTE;
                        case "int", "integer" -> elemType = DataType.INTEGER;
                        case "long" -> elemType = DataType.LONG;
                        case "float" -> elemType = DataType.FLOAT;
                        case "double", "number" -> elemType = DataType.DOUBLE;
                        case "bool", "boolean" -> elemType = DataType.BOOL;
                        case "date" -> elemType = DataType.DATE;
                        case "image" -> elemType = DataType.IMAGE;
                        default -> throw error(subType, "Unknown queue element type: " + subTypeName);
                    }
                } else {
                    throw error(subType, "Expected type name after 'queue.'");
                }
            }
            
            // For array.record or record, parse field definitions if braces follow
            if (elemType == DataType.RECORD && !consumedBraces && check(EbsTokenType.LBRACE)) {
                consume(EbsTokenType.LBRACE, "Expected '{' for record field definitions.");
                recordType = parseRecordFields();
                consume(EbsTokenType.RBRACE, "Expected '}' after record field definitions.");
            }
            
            if (elemType != null) {
                return new ReturnTypeInfo(elemType, recordType, bitmapType, intmapType);
            }
        }
        return null;
    }

    private Statement functionDeclaration() throws ParseError {
        // After matching 'function' keyword, parse the function definition
        // Supports: function name { ... } or function name(...) return type { ... }
        EbsToken n = consume(EbsTokenType.IDENTIFIER, "Expected function name after 'function'.");
        String name = (String) n.literal;
        
        if (match(EbsTokenType.LBRACE)) {
            // function name { ... }
            return block(name);
        } else if (match(EbsTokenType.RETURN)) {
            // function name return type { ... }
            ReturnTypeInfo typeInfo = blockParameterReturn();
            consume(EbsTokenType.LBRACE, "Expected { after return type.");
            BlockStatement bs = block(name);
            if (typeInfo != null) {
                bs.setReturnType(typeInfo.dataType, typeInfo.recordType, typeInfo.bitmapType, typeInfo.intmapType);
            }
            return bs;
        } else if (match(EbsTokenType.LPAREN)) {
            // function name(...) [return type] { ... }
            return blockParameters(name);
        } else {
            throw error(peek(), "Expected '{', 'return', or '(' after function name.");
        }
    }

    private Statement blockParameters(String name) throws ParseError {
        int line = currToken.line;
        List<Parameter> parameters = getBlockParameters();
        ReturnTypeInfo typeInfo = blockParameterReturn();
        BlockStatement bs;
        if (typeInfo != null) {
            consume(EbsTokenType.LBRACE, "Expected '{' after return.");
            List<Statement> s = getBlockStatements();
            bs = new BlockStatement(line, name, parameters, s, typeInfo.dataType);
            // Set additional type information for complex return types
            bs.setReturnType(typeInfo.dataType, typeInfo.recordType, typeInfo.bitmapType, typeInfo.intmapType);
        } else {
            consume(EbsTokenType.LBRACE, "Expected '{' after parameters.");
            List<Statement> s = getBlockStatements();
            bs = new BlockStatement(line, name, parameters, s);
        }
        
        // Check for optional exceptions block after function
        parseOptionalExceptionHandlers(bs);
        
        return bs;
    }

    private BlockStatement block(String name) throws ParseError {
        int line = currToken.line;
        List<Statement> s = getBlockStatements();
        BlockStatement bs = new BlockStatement(line, name, s);
        
        // Check for optional exceptions block after named block
        parseOptionalExceptionHandlers(bs);
        
        return bs;
    }
    
    /**
     * Parse optional exception handlers after a named block or function.
     * Syntax:
     *   myFunction() {
     *       // code
     *   } exceptions {
     *       when ERROR_TYPE { handler }
     *   }
     */
    private void parseOptionalExceptionHandlers(BlockStatement block) throws ParseError {
        // Check if there's an 'exceptions' keyword following this block
        if (match(EbsTokenType.EXCEPTIONS)) {
            consume(EbsTokenType.LBRACE, "Expected '{' after 'exceptions'.");
            
            List<ExceptionHandler> handlers = new ArrayList<>();
            while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
                handlers.add(parseExceptionHandler());
            }
            
            consume(EbsTokenType.RBRACE, "Expected '}' to close exceptions block.");
            
            if (handlers.isEmpty()) {
                throw error(peek(), "At least one 'when' handler is required in exceptions block.");
            }
            
            block.setExceptionHandlers(handlers);
        }
    }

    private Statement printStatement() throws ParseError {
        int line = currToken.line;
        Expression value = expression();
        consume(EbsTokenType.SEMICOLON, "Expected ';' after print value.");
        return new PrintStatement(line, value);
    }

    private Statement assignmentStatement() throws ParseError {
        return assignmentStatement(true);
    }

    private Statement assignmentStatement(boolean requireSemicolon) throws ParseError {
        // We need to parse an lvalue:
        // either IDENTIFIER, or IDENTIFIER followed by one or more [ index-list ] and/or .property
        // Supports: variable, array[0], record.field, array[0].field, array[0].field.nested, etc.
        EbsToken name = consume(EbsTokenType.IDENTIFIER, "Expected variable name.");
        String varName = (String) name.literal;
        
        // The lexer may have combined dot-separated identifiers into one token
        // (e.g., "customer.address.city" as a single IDENTIFIER token)
        // Split them and create nested PropertyExpressions
        Expression lvalue;
        if (varName.contains(".")) {
            String[] parts = varName.split("\\.");
            lvalue = new VariableExpression(name.line, parts[0]);
            for (int i = 1; i < parts.length; i++) {
                lvalue = new PropertyExpression(name.line, lvalue, parts[i]);
            }
        } else {
            lvalue = new VariableExpression(name.line, varName);
        }

        // Attach any number of bracketed index-suffixes and property accesses to the variable
        // This allows: array[0], array[0].field, array[0].field.nested, array[0][1].field, etc.
        while (check(EbsTokenType.LBRACKET) || check(EbsTokenType.DOT)) {
            if (check(EbsTokenType.LBRACKET)) {
                // Parse the index list (parseIndexList will consume the '[')
                Expression[] indices = parseIndexList();
                lvalue = new IndexExpression(name.line, lvalue, indices);
            } else if (match(EbsTokenType.DOT)) {
                // Property access
                EbsToken propToken = consume(EbsTokenType.IDENTIFIER, "Expected property name after '.'.");
                String propName = (String) propToken.literal;
                
                // The lexer may have combined multiple dot-separated identifiers into one
                // (e.g., "department.name" as a single token). Split them and create nested PropertyExpressions.
                if (propName.contains(".")) {
                    String[] parts = propName.split("\\.");
                    for (String part : parts) {
                        lvalue = new PropertyExpression(name.line, lvalue, part);
                    }
                } else {
                    lvalue = new PropertyExpression(name.line, lvalue, propName);
                }
            }
        }

        // Check for ++, --, or compound assignment operators
        Expression value;
        if (match(EbsTokenType.PLUS_PLUS)) {
            // i++ becomes i = i + 1
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.PLUS, "+", name.line, 0, 0),
                new LiteralExpression(EbsTokenType.INTEGER, 1));
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after increment.");
            }
        } else if (match(EbsTokenType.MINUS_MINUS)) {
            // i-- becomes i = i - 1
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.MINUS, "-", name.line, 0, 0),
                new LiteralExpression(EbsTokenType.INTEGER, 1));
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after decrement.");
            }
        } else if (match(EbsTokenType.PLUS_EQUAL)) {
            // i += x becomes i = i + x
            Expression right = expression();
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.PLUS, "+", name.line, 0, 0),
                right);
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after compound assignment.");
            }
        } else if (match(EbsTokenType.MINUS_EQUAL)) {
            // i -= x becomes i = i - x
            Expression right = expression();
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.MINUS, "-", name.line, 0, 0),
                right);
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after compound assignment.");
            }
        } else if (match(EbsTokenType.STAR_EQUAL)) {
            // i *= x becomes i = i * x
            Expression right = expression();
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.STAR, "*", name.line, 0, 0),
                right);
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after compound assignment.");
            }
        } else if (match(EbsTokenType.SLASH_EQUAL)) {
            // i /= x becomes i = i / x
            Expression right = expression();
            value = new BinaryExpression(name.line, lvalue, 
                new EbsToken(EbsTokenType.SLASH, "/", name.line, 0, 0),
                right);
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after compound assignment.");
            }
        } else {
            // Regular assignment
            consume(EbsTokenType.EQUAL, "Expected '=', '+=', '-=', '*=', '/=', '++', or '--' after variable name or index.");
            if (check(EbsTokenType.SELECT)) {
                value = parseSqlSelectFromSource();
            } else {
                value = expression();
            }
            if (requireSemicolon) {
                consume(EbsTokenType.SEMICOLON, "Expected ';' after assignment.");
            }
        }

        if (value instanceof ArrayLiteralExpression array) {
            array.array = lvalue;
        }

        if (lvalue instanceof VariableExpression ve) {
            return new AssignStatement(name.line, (String) name.literal, value);
        } else if (lvalue instanceof PropertyExpression pe) {
            // Property assignment (e.g., record.field or array[0].field)
            return new IndexAssignStatement(name.line, lvalue, value);
        } else {
            // Index assignment (e.g., array[0])
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

    /**
     * Parse a try-exceptions statement.
     * Syntax:
     *   try {
     *       statements
     *   } exceptions {
     *       when ERROR_TYPE { handler statements }
     *       when ERROR_TYPE(errorVar) { handler statements }
     *   }
     */
    private Statement tryStatement() throws ParseError {
        int line = previous().line; // the 'try' token we just matched

        // Parse the try block
        consume(EbsTokenType.LBRACE, "Expected '{' after 'try'.");
        BlockStatement tryBlock = (BlockStatement) block();

        // Require 'exceptions' keyword
        consume(EbsTokenType.EXCEPTIONS, "Expected 'exceptions' after try block.");

        // Parse the exceptions block
        consume(EbsTokenType.LBRACE, "Expected '{' after 'exceptions'.");

        // Parse exception handlers (when clauses)
        List<ExceptionHandler> handlers = new ArrayList<>();
        while (!check(EbsTokenType.RBRACE) && !isAtEnd()) {
            handlers.add(parseExceptionHandler());
        }

        consume(EbsTokenType.RBRACE, "Expected '}' to close exceptions block.");

        if (handlers.isEmpty()) {
            throw error(peek(), "At least one 'when' handler is required in exceptions block.");
        }

        return new TryStatement(line, tryBlock, handlers);
    }

    /**
     * Parse a single exception handler (when clause).
     * Syntax:
     *   when ERROR_TYPE { statements }
     *   when ERROR_TYPE(errorVar) { statements }
     *   when CUSTOM_EXCEPTION { statements }
     *   when CUSTOM_EXCEPTION(errorVar) { statements }
     */
    private ExceptionHandler parseExceptionHandler() throws ParseError {
        consume(EbsTokenType.WHEN, "Expected 'when' keyword in exception handler.");

        // Parse error type (e.g., IO_ERROR, ANY_ERROR, or custom exception name)
        EbsToken errorTypeToken = consume(EbsTokenType.IDENTIFIER, "Expected error type name after 'when'.");
        String errorTypeName = (String) errorTypeToken.literal;

        // Check if this is a standard error type
        ErrorType errorType = ErrorType.fromName(errorTypeName);

        // Check for optional error variable: when ERROR_TYPE(varName)
        String errorVarName = null;
        if (match(EbsTokenType.LPAREN)) {
            EbsToken varNameToken = consume(EbsTokenType.IDENTIFIER, "Expected variable name in exception handler.");
            errorVarName = (String) varNameToken.literal;
            consume(EbsTokenType.RPAREN, "Expected ')' after error variable name.");
        }

        // Parse the handler block
        consume(EbsTokenType.LBRACE, "Expected '{' after error type.");
        BlockStatement handlerBlock = (BlockStatement) block();

        // Create appropriate handler based on whether this is a standard or custom exception
        if (errorType != null) {
            return new ExceptionHandler(errorType, errorVarName, handlerBlock);
        } else {
            // Custom exception handler
            return new ExceptionHandler(errorTypeName, errorVarName, handlerBlock);
        }
    }

    /**
     * Parse a raise exception statement.
     * Syntax:
     *   raise exception ERROR_TYPE("message");              // Standard exception
     *   raise exception CUSTOM_EXCEPTION(param1, param2);   // Custom exception
     * 
     * Standard exceptions (defined in ErrorType enum) only take a message parameter.
     * Custom exceptions can have multiple parameters.
     */
    private Statement raiseStatement() throws ParseError {
        // matchAll(RAISE, EXCEPTION) matched but didn't consume tokens
        // Need to advance past both RAISE and EXCEPTION
        advance(); // RAISE
        int line = currToken.line;
        advance(); // EXCEPTION
        
        // Parse exception name
        EbsToken exceptionNameToken = consume(EbsTokenType.IDENTIFIER, "Expected exception type name after 'raise exception'.");
        String exceptionName = (String) exceptionNameToken.literal;
        
        // Check if this is a standard exception type
        ErrorType errorType = ErrorType.fromName(exceptionName);
        
        // Expect opening paren
        consume(EbsTokenType.LPAREN, "Expected '(' after exception type name.");
        
        // Parse parameters
        List<Expression> parameters = new ArrayList<>();
        if (!check(EbsTokenType.RPAREN)) {
            do {
                parameters.add(expression());
            } while (match(EbsTokenType.COMMA));
        }
        
        consume(EbsTokenType.RPAREN, "Expected ')' after exception parameters.");
        consume(EbsTokenType.SEMICOLON, "Expected ';' after raise statement.");
        
        // Validate standard exceptions only take a single message parameter
        if (errorType != null) {
            if (parameters.size() > 1) {
                throw error(exceptionNameToken, "Standard exception '" + exceptionName + 
                    "' only accepts a single message parameter. For multiple parameters, use a custom exception.");
            }
            Expression message = parameters.isEmpty() ? null : parameters.get(0);
            return new RaiseStatement(line, errorType, message);
        } else {
            // Custom exception
            return new RaiseStatement(line, exceptionName, parameters);
        }
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

        while (match(EbsTokenType.STAR, EbsTokenType.SLASH, EbsTokenType.PERCENT)) {
            EbsToken operator = previous();
            Expression right = unary();
            expr = new BinaryExpression(currToken.line, expr, operator, right);
        }

        return expr;
    }

    private Expression unary() throws ParseError {
        // Include BANG only if your lexer supports it
        // Also include TYPEOF as a unary operator to get the type of an expression
        if (match(EbsTokenType.MINUS, EbsTokenType.PLUS, EbsTokenType.BOOL_BANG, EbsTokenType.TYPEOF)) {
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
            } else if (!callEmpty) {
                // Property access (e.g., record field access)
                base = new PropertyExpression(line, base, name);
            } else {
                // Unknown method call
                throw error(prop, "Unknown method '" + name + "()'");
            }
        }

        return base;
    }

    private Expression primary() throws ParseError {
        EbsTokenType type = peek().type;
        Expression expr = null;
        
        // Check for type casting: int(value), string(value), etc.
        // When type keywords like 'int', 'string' appear in source code, the lexer
        // adds them to the keywords map as DATATYPE tokens via addKeywords()
        if (check(EbsTokenType.DATATYPE)) {
            EbsToken typeToken = advance();
            String typeName = (String) typeToken.literal;
            
            // Check if this is a cast expression (type followed by parentheses)
            if (check(EbsTokenType.LPAREN)) {
                // Determine the DataType from the type name string
                DataType dataType = getDataTypeFromString(typeName);
                if (dataType == null) {
                    throw error(typeToken, "Unknown type name '" + typeName + "'");
                }
                
                consume(EbsTokenType.LPAREN, "Expected '(' after type name for cast.");
                Expression valueExpr = expression();
                consume(EbsTokenType.RPAREN, "Expected ')' after cast expression.");
                return new CastExpression(typeToken.line, dataType, valueExpr);
            } else {
                // Type name not followed by parentheses - this is an error in expression context
                throw error(typeToken, "Unexpected type name '" + typeName + "' in expression context. Use " + typeName + "(...) for type casting.");
            }
        } else if (match(EbsTokenType.INTEGER, EbsTokenType.LONG, EbsTokenType.FLOAT, EbsTokenType.DOUBLE, EbsTokenType.DATE, EbsTokenType.BOOL_TRUE, EbsTokenType.BOOL_FALSE, EbsTokenType.NULL)) {
            expr = new LiteralExpression(type, previous().literal);
        } else if (match(EbsTokenType.IDENTIFIER)) {
            EbsToken p = previous();
            String varName = (String) p.literal;
            
            // Check if this is a record literal: TypeName { ... }
            if (check(EbsTokenType.LBRACE) && !varName.contains(".")) {
                // Check if this is a type alias (for record literals)
                TypeRegistry.TypeAlias alias = TypeRegistry.getTypeAlias(varName);
                if (alias != null) {
                    // This is a record literal with the syntax: TypeName { field: value, ... }
                    return parseRecordLiteral();
                }
            }
            
            // Check if this is a type alias being used for casting (e.g., myBitmapType(byteVar), myIntmapType(intVar))
            if (check(EbsTokenType.LPAREN) && !varName.contains(".")) {
                TypeRegistry.TypeAlias alias = TypeRegistry.getTypeAlias(varName);
                if (alias != null && alias.intmapType != null) {
                    // This is an intmap type alias cast
                    consume(EbsTokenType.LPAREN, "Expected '(' after type name for cast.");
                    Expression valueExpr = expression();
                    consume(EbsTokenType.RPAREN, "Expected ')' after cast expression.");
                    return new CastExpression(p.line, DataType.INTMAP, alias.intmapType, varName, valueExpr);
                } else if (alias != null && alias.bitmapType != null) {
                    // This is a bitmap type alias cast
                    consume(EbsTokenType.LPAREN, "Expected '(' after type name for cast.");
                    Expression valueExpr = expression();
                    consume(EbsTokenType.RPAREN, "Expected ')' after cast expression.");
                    return new CastExpression(p.line, DataType.BITMAP, alias.bitmapType, varName, valueExpr);
                }
            }
            
            // The lexer may have combined dot-separated identifiers into one token
            // (e.g., "customer.address.city" as a single IDENTIFIER token)
            // Split them and create nested PropertyExpressions
            if (varName.contains(".")) {
                String[] parts = varName.split("\\.");
                expr = new VariableExpression(p.line, parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    expr = new PropertyExpression(p.line, expr, parts[i]);
                }
            } else {
                expr = new VariableExpression(p.line, varName);
            }
        } else if (match(EbsTokenType.LPAREN)) {
            expr = expression();
            consume(EbsTokenType.RPAREN, "Expected ')' after expression.");
            return expr;
        } else if (match(EbsTokenType.CALL) || check(EbsTokenType.BUILTIN)) {
            // Handle both explicit call statements and builtin function calls (e.g., array.asBitmap, array.asByte)
            CallStatement c = (CallStatement) call();
            expr = new CallExpression(c);
            postParseExpressions.add(expr);
            return expr;
        } else if (checkAny(EbsTokenType.LBRACE, EbsTokenType.LBRACKET)) {
            // Check if this is a JSON literal (object or array) vs array literal
            if (check(EbsTokenType.LBRACE) && looksLikeJsonObject()) {
                expr = parseJsonLiteralFromSource();
            } else if (check(EbsTokenType.LBRACKET) && looksLikeJsonArray()) {
                expr = parseJsonLiteralFromSource();
            } else {
                expr = parseArrayLiteral();
            }
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
        Expression expr = null;
        // Accept both STRING and DATE tokens inside quoted strings.
        // The lexer automatically converts date-formatted strings like '2025-12-04 10:30'
        // to DATE tokens, but we need to treat them as strings within quoted literals
        // for consistent string handling in the parser.
        if (match(EbsTokenType.STRING, EbsTokenType.DATE)) {
            EbsToken matchedToken = previous();
            expr = new LiteralExpression(matchedToken.type, matchedToken.literal);
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
                    // Check if this is a JSON literal (object or array) vs array literal
                    if (check(EbsTokenType.LBRACE) && looksLikeJsonObject()) {
                        elements.add(parseJsonLiteralFromSource());
                    } else if (check(EbsTokenType.LBRACKET) && looksLikeJsonArray()) {
                        elements.add(parseJsonLiteralFromSource());
                    } else {
                        elements.add(parseArrayLiteral());
                    }
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

    private Statement forStatement() throws ParseError {
        int line = previous().line; // 'for'

        consume(EbsTokenType.LPAREN, "Expected '(' after 'for'.");

        // Initializer (var declaration or assignment)
        Statement initializer;
        if (match(EbsTokenType.SEMICOLON)) {
            initializer = null;  // No initializer
        } else if (match(EbsTokenType.VAR)) {
            initializer = varDeclaration(false);
        } else {
            initializer = assignmentStatement();
        }

        // Condition
        Expression condition = null;
        if (!check(EbsTokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(EbsTokenType.SEMICOLON, "Expected ';' after loop condition.");

        // Increment
        Statement increment = null;
        if (!check(EbsTokenType.RPAREN)) {
            increment = assignmentStatement(false);
        }
        consume(EbsTokenType.RPAREN, "Expected ')' after for clauses.");

        // Body
        consume(EbsTokenType.LBRACE, "Expected '{' to start for body.");
        loopDepth++;
        BlockStatement body = (BlockStatement) block();
        loopDepth--;

        return new ForStatement(line, initializer, condition, increment, body);
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
        // matchAll(CLOSE, CONNECTION) matched but didn't consume tokens
        // Need to advance past both CLOSE and CONNECTION
        advance(); // CLOSE
        int line = currToken.line; // CONNECTION token line
        advance(); // CONNECTION
        
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected connection name after 'close connection'.");
        consume(EbsTokenType.SEMICOLON, "Expected ';' after close connection.");
        return new CloseConnectionStatement(line, (String) nameTok.literal);
    }

    private Statement screenStatement(boolean replaceExisting) throws ParseError {
        int line = previous().line; // the 'screen' token

        // Parse screen name
        EbsToken nameTok = consume(EbsTokenType.IDENTIFIER, "Expected screen name after 'screen'.");
        String screenName = (String) nameTok.literal;

        // Expect '=' for screen definition
        if (match(EbsTokenType.EQUAL)) {
            // screen <name> = {...}; or new screen <name> = {...};
            // Parse the spec:
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

            // ';'
            consume(EbsTokenType.SEMICOLON, "Expected ';' after screen statement.");

            return new ScreenStatement(line, screenName, spec, replaceExisting);
        } else {
            throw error(peek(), "Expected '=' after screen name. Use 'show screen <name>' to show or 'hide screen <name>' to hide.");
        }
    }

    private Statement showScreenStatement() throws ParseError {
        int line = previous().line; // the 'show' token

        // Expect 'screen' keyword
        consume(EbsTokenType.SCREEN, "Expected 'screen' after 'show'.");

        // Check if there's a screen name or if it's just "show screen;"
        String screenName = null;
        if (check(EbsTokenType.IDENTIFIER)) {
            EbsToken nameTok = advance();
            screenName = (String) nameTok.literal;
        }

        // Check for optional parameters: (param1, param2, ...)
        List<Expression> parameters = null;
        if (match(EbsTokenType.LPAREN)) {
            parameters = new ArrayList<>();
            if (!check(EbsTokenType.RPAREN)) {
                do {
                    parameters.add(expression());
                } while (match(EbsTokenType.COMMA));
            }
            consume(EbsTokenType.RPAREN, "Expected ')' after parameters.");
        }

        // Check for optional callback: callback name
        String callbackName = null;
        if (match(EbsTokenType.CALLBACK)) {
            EbsToken callbackTok = consume(EbsTokenType.IDENTIFIER, "Expected callback function name after 'callback'.");
            callbackName = (String) callbackTok.literal;
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after 'show screen'.");
        return new ScreenShowStatement(line, screenName, parameters, callbackName);
    }

    private Statement hideScreenStatement() throws ParseError {
        int line = previous().line; // the 'hide' token

        // Expect 'screen' keyword
        consume(EbsTokenType.SCREEN, "Expected 'screen' after 'hide'.");

        // Check if there's a screen name or if it's just "hide screen;"
        String screenName = null;
        if (check(EbsTokenType.IDENTIFIER)) {
            EbsToken nameTok = advance();
            screenName = (String) nameTok.literal;
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after 'hide screen'.");
        return new ScreenHideStatement(line, screenName);
    }

    private Statement closeScreenStatement() throws ParseError {
        // matchAll(CLOSE, SCREEN) matched but didn't consume tokens
        // Need to advance past both CLOSE and SCREEN
        advance(); // CLOSE
        int line = currToken.line; // SCREEN token line
        advance(); // SCREEN
        
        // Check if there's a screen name or if it's just "close screen;"
        String screenName = null;
        if (check(EbsTokenType.IDENTIFIER)) {
            EbsToken nameTok = advance();
            screenName = (String) nameTok.literal;
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after 'close screen'.");
        return new ScreenCloseStatement(line, screenName);
    }

    private Statement submitScreenStatement() throws ParseError {
        // matchAll(SUBMIT, SCREEN) matched but didn't consume tokens
        // Need to advance past both SUBMIT and SCREEN
        advance(); // SUBMIT
        int line = currToken.line; // SCREEN token line
        advance(); // SCREEN
        
        // Check if there's a screen name or if it's just "submit screen;"
        String screenName = null;
        if (check(EbsTokenType.IDENTIFIER)) {
            EbsToken nameTok = advance();
            screenName = (String) nameTok.literal;
        }

        consume(EbsTokenType.SEMICOLON, "Expected ';' after 'submit screen'.");
        return new ScreenSubmitStatement(line, screenName);
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
        StringBuilder sb = new StringBuilder();
        
        // Check if we have a CALL token (the # symbol)
        // The # is just a shorthand for "call" and should be consumed but not included in the name
        consumeOptional(EbsTokenType.CALL);
        
        // Now check for BUILTIN or IDENTIFIER
        // Also accept DATATYPE tokens for builtins like queue.enqueue or array.sort
        EbsToken first = consumeOptional(EbsTokenType.BUILTIN);
        if (first == null) {
            // Accept IDENTIFIER or DATATYPE (for type-prefixed builtins like queue.*, array.*)
            if (check(EbsTokenType.DATATYPE) || check(EbsTokenType.QUEUE) || check(EbsTokenType.ARRAY)) {
                first = advance();
                sb.append((String) first.literal);
            } else {
                first = consume(EbsTokenType.IDENTIFIER, "Expected identifier.");
                sb.append((String) first.literal);
            }
            while (match(EbsTokenType.DOT)) {
                // Accept various token types that could be method names (size, length, etc.)
                // These are keywords but can also be method names in builtins
                EbsToken seg;
                if (check(EbsTokenType.SIZE) || check(EbsTokenType.LENGTH)) {
                    seg = advance();
                    sb.append('.').append((String) seg.literal);
                } else {
                    seg = consume(EbsTokenType.IDENTIFIER, "Expected identifier after '.'.");
                    sb.append('.').append((String) seg.literal);
                }
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

    /**
     * Check for 'new screen' pattern (identifier 'new' followed by SCREEN keyword).
     * Used for the 'new screen <name> = {...};' syntax to replace existing screen definitions.
     */
    private boolean checkNewScreen() {
        if (isAtEnd()) {
            return false;
        }
        // Check if current token is identifier 'new'
        if (currToken.type == EbsTokenType.IDENTIFIER && 
            currToken.literal instanceof String literal && 
            literal.equalsIgnoreCase("new")) {
            // Check if next token is SCREEN using bounds-checked access
            if (current + 1 < tokens.size()) {
                return tokens.get(current + 1).type == EbsTokenType.SCREEN;
            }
        }
        return false;
    }

    /**
     * Parse a builtin call expression for use in JSON preprocessing.
     * Expected format: qualifiedName(param1, param2, ...)
     */
    private Expression parseBuiltinCallExpression() throws ParseError {
        // Parse the qualified name (e.g., "string.concat")
        String qname = parseQualifiedName();
        
        // Expect opening paren
        consume(EbsTokenType.LPAREN, "Expected '(' after builtin name.");
        
        // Parse parameters
        List<Parameter> parameters = new ArrayList<>();
        while (!check(EbsTokenType.RPAREN) && !isAtEnd()) {
            // Parse parameter expression
            Expression paramExpr = expression();
            parameters.add(new Parameter(paramExpr));
            
            // Check for comma
            if (!check(EbsTokenType.RPAREN)) {
                consume(EbsTokenType.COMMA, "Expected ',' or ')' in parameter list.");
            }
        }
        
        consume(EbsTokenType.RPAREN, "Expected ')' after parameters.");
        
        // Create a CallStatement and wrap it in a CallExpression
        CallStatement callStmt = new CallStatement(1, qname, parameters);
        return new CallExpression(callStmt);
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
    
    /**
     * Peek at the next token without consuming it
     */
    private EbsToken peekNext() {
        if (current + 1 < tokens.size()) {
            return tokens.get(current + 1);
        }
        return null;
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

    /**
     * Convert a type name string to a DataType enum value.
     * Supports common type name variants (e.g., "int" and "integer", "bool" and "boolean")
     */
    private DataType getDataTypeFromString(String typeName) {
        if (typeName == null) {
            return null;
        }
        String lower = typeName.toLowerCase();
        return switch (lower) {
            case "byte" -> DataType.BYTE;
            case "int", "integer" -> DataType.INTEGER;
            case "long" -> DataType.LONG;
            case "float" -> DataType.FLOAT;
            case "double" -> DataType.DOUBLE;
            case "string" -> DataType.STRING;
            case "date" -> DataType.DATE;
            case "bool", "boolean" -> DataType.BOOL;
            case "json" -> DataType.JSON;
            case "array" -> DataType.ARRAY;
            case "queue" -> DataType.QUEUE;
            case "record" -> DataType.RECORD;
            case "map" -> DataType.MAP;
            case "image" -> DataType.IMAGE;
            default -> null;
        };
    }
    
    /**
     * Helper class to hold parsed return type information including complex types.
     */
    private static class ReturnTypeInfo {
        DataType dataType;
        RecordType recordType;
        BitmapType bitmapType;
        IntmapType intmapType;
        
        ReturnTypeInfo(DataType dataType) {
            this.dataType = dataType;
            this.recordType = null;
            this.bitmapType = null;
            this.intmapType = null;
        }
        
        ReturnTypeInfo(DataType dataType, RecordType recordType, BitmapType bitmapType, IntmapType intmapType) {
            this.dataType = dataType;
            this.recordType = recordType;
            this.bitmapType = bitmapType;
            this.intmapType = intmapType;
        }
    }

}
