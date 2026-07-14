package me.kuwg.re.parser;

import me.kuwg.re.ast.AST;
import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.array.ArrayAccessNode;
import me.kuwg.re.ast.nodes.array.ArrayCreationNode;
import me.kuwg.re.ast.nodes.array.ArrayNode;
import me.kuwg.re.ast.nodes.array.ArraySetNode;
import me.kuwg.re.ast.nodes.async.AsyncDeclarationNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.ReturnNode;
import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.constants.*;
import me.kuwg.re.ast.nodes.enumeration.EnumDeclarationNode;
import me.kuwg.re.ast.nodes.expression.BinaryExpressionNode;
import me.kuwg.re.ast.nodes.expression.BitwiseNotNode;
import me.kuwg.re.ast.nodes.extern.NativeCPPNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.nodes.function.call.GenericFunctionCallNode;
import me.kuwg.re.ast.nodes.function.call.StructFunctionCallNode;
import me.kuwg.re.ast.nodes.function.declaration.*;
import me.kuwg.re.ast.nodes.global.GlobalVariableDeclarationNode;
import me.kuwg.re.ast.nodes.instance.IsNode;
import me.kuwg.re.ast.nodes.ir.IRDeclarationNode;
import me.kuwg.re.ast.nodes.lambda.LambdaDeclarationNode;
import me.kuwg.re.ast.nodes.len.LenNode;
import me.kuwg.re.ast.nodes.loop.BreakNode;
import me.kuwg.re.ast.nodes.loop.ContinueNode;
import me.kuwg.re.ast.nodes.loop.ForLoopNode;
import me.kuwg.re.ast.nodes.loop.WhileNode;
import me.kuwg.re.ast.nodes.module.UsingNode;
import me.kuwg.re.ast.nodes.namespace.NamespaceCallNode;
import me.kuwg.re.ast.nodes.namespace.NamespaceDeclarationNode;
import me.kuwg.re.ast.nodes.pointer.DereferenceAssignNode;
import me.kuwg.re.ast.nodes.pointer.DereferenceNode;
import me.kuwg.re.ast.nodes.pointer.PointerCreationNode;
import me.kuwg.re.ast.nodes.pointer.ReferenceNode;
import me.kuwg.re.ast.nodes.raise.RaiseNode;
import me.kuwg.re.ast.nodes.range.RangeNode;
import me.kuwg.re.ast.nodes.statement.IfStatementNode;
import me.kuwg.re.ast.nodes.statement.MatchNode;
import me.kuwg.re.ast.nodes.statement.TryCatchNode;
import me.kuwg.re.ast.nodes.struct.*;
import me.kuwg.re.ast.nodes.struct.gen.GenStructDeclarationNode;
import me.kuwg.re.ast.nodes.struct.gen.GenStructImplNode;
import me.kuwg.re.ast.nodes.struct.gen.GenStructInitNode;
import me.kuwg.re.ast.nodes.ternary.TernaryOperatorNode;
import me.kuwg.re.ast.nodes.trait.TraitDeclarationNode;
import me.kuwg.re.ast.nodes.type.TypeofNode;
import me.kuwg.re.ast.nodes.variable.DirectVariableReferenceNode;
import me.kuwg.re.ast.nodes.variable.VariableDeclarationNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.function.RDefFunction;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.compiler.struct.RConstructor;
import me.kuwg.re.compiler.trait.TraitFunction;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.constants.Constants;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.expr.RImplNotFunctionError;
import me.kuwg.re.error.errors.parser.RParserError;
import me.kuwg.re.error.errors.range.RRangeTypeError;
import me.kuwg.re.module.ModuleLoadingHelper;
import me.kuwg.re.operator.BinaryOperators;
import me.kuwg.re.operator.ops.add.AddBO;
import me.kuwg.re.operator.ops.add.SubBO;
import me.kuwg.re.operator.ops.comp.EqualsBO;
import me.kuwg.re.token.Token;
import me.kuwg.re.token.TokenType;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.iterable.range.RangeType;
import me.kuwg.re.type.lambda.LambdaType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;
import me.kuwg.re.type.struct.GenStructType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.trait.TraitType;

import java.util.*;

import static me.kuwg.re.token.TokenType.*;

public final class ASTParser {
    public final Map<String, TypeRef> typeMap;
    private final String fileName;
    private final Token[] tokens;
    private final boolean initial;
    private List<String> currentGenericTypes = new ArrayList<>();
    private int tokenIndex;

    public ASTParser(final String fileName, final Token[] tokens) {
        this.fileName = fileName;
        this.tokens = tokens;
        this.initial = true;
        this.typeMap = new HashMap<>();
    }

    public ASTParser(final String fileName, final Token[] tokens, final Map<String, TypeRef> typeMap) {
        this.fileName = fileName;
        this.tokens = tokens;
        this.initial = true;
        this.typeMap = typeMap;
        if (typeMap.isEmpty() && !fileName.contains("default")) throw new RuntimeException("name=" + fileName);
    }

    private void includeInitialModules(AST ast) {
        ModuleLoadingHelper.collectModuleTypes(fileName, 0, fileName, Constants.Parser.DEFAULT_MODULE_NAME, null, typeMap).forEach(typeMap::putIfAbsent);
        ast.addChild(new UsingNode(fileName, 0, null, Constants.Parser.DEFAULT_MODULE_NAME, null));
    }

    public AST parse() {
        AST ast = new AST(fileName);

        if (initial) {
            includeInitialModules(ast);
        }

        while (!tokens[tokenIndex].matches(EOF)) {
            ASTNode node = parseStatement();
            if (node != null) {
                ast.addChild(node);
            } else {
                break;
            }
        }

        return ast;
    }

    private BlockNode parseBlock() {
        removeNewlines();

        if (!match(INDENT)) {
            return new RParserError("Expected indented block", fileName, line()).raise();
        }

        consume();

        List<ASTNode> statements = new ArrayList<>();

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) break;

            ASTNode stmt = parseStatement();
            if (stmt == null) break;
            statements.add(stmt);
        }

        if (match(EOF)) return new BlockNode(fileName, statements);
        if (match(DEDENT)) {
            consume();
        } else {
            return new RParserError("Expected dedent to close block", fileName, line()).raise();
        }

        return new BlockNode(fileName, statements);
    }

    private ValueNode parseExpression(int minPrecedence) {
        ValueNode left = parsePrimary();

        while (match(OPERATOR)) {
            int line = line();

            if (match(OPERATOR, ".")) {
                left = parseSubExpr(line, false, null, left);
                continue;
            }

            String opSymbol = current().value();

            var op = BinaryOperators.getBySymbol(opSymbol);

            if (op == null) break;

            if (op.getPrecedence() < minPrecedence) break;
            consume();
            ValueNode right = parseExpression(op.getPrecedence() + 1);

            left = new BinaryExpressionNode(line, fileName, left, op, right);
        }

        if (matchAndConsume(KEYWORD, "is")) {
            left = new IsNode(fileName, line(), left, parseType(false));
        }

        if (matchAndConsume(KEYWORD, "if")) return parseTernaryOperator(left);

        return left;
    }

    private ASTNode parseStatement() {
        removeNewlines();

        if (outOfBounds(0)) return null;

        int line = line();

        ASTNode n = switch (current().type()) {
            case INDENT -> new RParserError("Unexpected indent", fileName, line).raise();
            case KEYWORD -> parseKeyword();
            case IDENTIFIER -> parseIdentifier(false);
            case NUMBER -> parseNumber();
            case STRING -> parseString();
            case CHARACTER -> parseCharacter();
            case OPERATOR -> {
                if (matchAndConsume(OPERATOR, "@")) yield parseDereferenceOperator();
                else if (matchAndConsume(OPERATOR, "~")) yield parseBitwiseNotOperator();
                else
                    yield new RParserError("Unexpected operator in statement: " + current().value(), fileName, line).raise();
            }
            case DIVIDER -> parseDivider();
            default ->
                    new RParserError("Unexpected token: " + current().value() + ", type: " + current().type(), fileName, line).raise();
        };

        if (match(EOF) || match(DEDENT)) {
            return n;
        }
        if (!previous().matches(DEDENT)) {
            if (!match(NEWLINE)) {
                return new RParserError("Expected newline after statement, got " + current().value(), fileName, line()).raise();
            }
            consume();
        }

        return n;
    }

    private ValueNode parsePrimary() {
        ValueNode node;

        Token token = current();
        switch (token.type()) {
            case NUMBER -> node = parseNumber();
            case STRING -> node = parseString();
            case IDENTIFIER -> {
                ASTNode n2 = parseIdentifier(false);
                if (!(n2 instanceof ValueNode v)) return new RParserError("Expected a value", fileName, line()).raise();
                node = v;
            }
            case OPERATOR -> {
                switch (token.value()) {
                    case "@" -> {
                        consume();
                        node = parseDereferenceOperator();
                    }
                    case "-" -> {
                        consume();
                        node = new BinaryExpressionNode(line(), fileName, NumberNode.ZERO, SubBO.INSTANCE, parseValue());
                    }
                    case "+" -> {
                        consume();
                        node = new BinaryExpressionNode(line(), fileName, NumberNode.ZERO, AddBO.INSTANCE, parseValue());
                    }
                    case "~" -> {
                        consume();
                        node = parseBitwiseNotOperator();
                    }
                    case "not" -> {
                        consume();
                        node = new BinaryExpressionNode(line(), fileName, parseValue(), EqualsBO.INSTANCE, new BooleanNode(fileName, line(), false));
                    }
                    default -> {
                        return new RParserError("Unexpected operator: " + token.value(), fileName, line()).raise();
                    }
                }
            }
            case DIVIDER -> {
                if (matchAndConsume(DIVIDER, "[")) {
                    node = parseArrayDeclaration();
                } else if (matchAndConsume(DIVIDER, "(")) {
                    node = parseExpression(0);
                    if (!matchAndConsume(DIVIDER, ")")) {
                        return new RParserError("Expected ')'", fileName, line()).raise();
                    }
                } else {
                    return new RParserError("Unexpected divider: " + token.value(), fileName, line()).raise();
                }
            }
            case BOOLEAN -> {
                if (matchAndConsume(BOOLEAN, "true")) {
                    return new BooleanNode(fileName, previous().line(), true);
                } else if (matchAndConsume(BOOLEAN, "false")) {
                    return new BooleanNode(fileName, previous().line(), false);
                } else {
                    return new RParserError("Expected boolean value", fileName, line()).raise();
                }
            }
            case KEYWORD -> {
                ASTNode n2 = parseKeyword();
                if (n2 instanceof ValueNode value) {
                    node = value;
                } else {
                    return new RParserError("Expected value", fileName, n2.getLine()).raise();
                }
            }
            case CHARACTER -> node = parseCharacter();
            default -> {
                return new RParserError("Unexpected token: " + current().value() + ", type: " + current().type(), fileName, line()).raise();
            }
        }

        int line = outOfBounds(0) ? 0 : line();

        while (true) {
            if (match(DIVIDER, "(")) {
                var args = parseParamsCall();
                if (!(node instanceof DirectVariableReferenceNode vr)) {
                    return new RParserError("Expected reference for function call", fileName, line).raise();
                }
                node = new FunctionCallNode(fileName, line, vr.getSimpleName(), args);
            } else if (match(DIVIDER, "[")) {
                consume();
                ValueNode index = parseValue();
                if (!matchAndConsume(DIVIDER, "]")) {
                    return new RParserError("Expected ']'", fileName, line).raise();
                }

                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new ArraySetNode(fileName, line, node, index, value);
                } else {
                    node = new ArrayAccessNode(fileName, line, node, index);
                }
            } else if (match(OPERATOR, "@")) {
                consume();
                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new DereferenceAssignNode(fileName, line, node, value);
                } else {
                    if (!(node instanceof VariableReference vr)) {
                        return new RParserError("Expected any type of variable reference for pointer type declaration", fileName, line()).raise();
                    }
                    node = new DereferenceNode(fileName, line, vr);
                }
            } else {
                break;
            }
        }

        return node;
    }

    private ValueNode parseValue() {
        return parseExpression(0);
    }

    private ASTNode parseKeyword() {
        var kw = consume().value();

        return switch (kw) {
            case "_Builtin" -> parse_BuiltinKeyword();
            case "_IR" -> parse_IRKeyword();
            case "using" -> parseUsingKeyword();
            case "ptr" -> parsePtrKeyword();
            case "while" -> parseWhileKeyword();
            case "break" -> parseBreakKeyword();
            case "continue" -> parseContinueKeyword();
            case "func" -> parseFuncKeyword();
            case "for" -> parseForKeyword();
            case "range" -> parseRangeKeyword();
            case "return" -> parseReturnKeyword();
            case "struct" -> parseStructKeyword(false);
            case "init" -> parseInitKeyword();
            case "if" -> parseIfKeyword();
            case "sizeof" -> parseSizeofKeyword();
            case "len" -> parseLenKeyword();
            case "cast" -> parseCastKeyword();
            case "typeof" -> parseTypeofKeyword(false);
            case "typeofLLVM" -> parseTypeofKeyword(true);
            case "impl" -> parseImplKeyword();
            case "self" -> parseIdentifier(true);
            case "global" -> parseGlobal();
            case "raise" -> parseRaise();
            case "try" -> parseTry();
            case "_NativeCPP" -> parse_NativeCPPKeyword();
            case "null" -> parseNullKeyword();
            case "async" -> parseAsyncKeyword();
            case "generic" -> parseGenericKeyword();
            case "this" -> parseThisKeyword();
            case "match" -> parseMatchKeyword();
            case "lambda" -> parseLambdaKeyword();
            case "namespace" -> parseNamespaceKeyword();
            case "type" -> parseTypeKeyword();
            case "enum" -> parseEnumKeyword();
            case "extern" -> parseExternKeyword();
            case "trait" -> parseTraitKeyword();
            default -> new RParserError("Unexpected keyword: " + kw, fileName, line()).raise();
        };
    }

    private ASTNode parseIdentifier(boolean self) {
        int line = line();
        String name = self ? "self" : consume().value();

        ValueNode node = new DirectVariableReferenceNode(fileName, line, name);

        node = parseSubExpr(line, self, name, node);

        return node;
    }

    private @SubFunc ValueNode parseVariableAssignment(int line, VariableReference variable) {
        return switch (current().value()) {
            case "=" -> {
                consume();

                ValueNode value = parseValue();

                //if (variable instanceof StructFieldAccessNode access) {
                //    yield new StructFieldReassignmentNode(fileName, line, access, value);
                //}

                yield new VariableDeclarationNode(fileName, line, variable, false, null, value);
            }
            case ":" -> {
                consume();

                boolean mutable = matchAndConsume(KEYWORD, "mut");

                TypeRef type;

                if (!match(OPERATOR, "=")) type = parseType(false);
                else type = null;

                if (!matchAndConsume(OPERATOR, "=")) {
                    yield new RParserError("Expected '=' for variable assignment", fileName, line()).raise();
                }

                ValueNode value = parseValue();
                yield new VariableDeclarationNode(fileName, line, variable, mutable, type, value);
            }

            default -> new RParserError("TODO var reference or other assignment ops", fileName, line()).raise();
        };
    }

    private ValueNode parseNumber() {
        return new NumberNode(fileName, line(), consume().value());
    }

    private @SubFunc ASTNode parse_BuiltinKeyword() {
        int line = line();

        if (matchAndConsume(KEYWORD, "struct")) return parseStructKeyword(true);

        boolean keepName = matchAndConsume(KEYWORD, "global");

        if (!matchAndConsume(KEYWORD, "func")) {
            return new RParserError("Expected 'func' for builtin function declaration", fileName, line).raise();
        }

        String name = identifier();

        var params = parseParamsDeclare(false);

        if (!matchAndConsume(OPERATOR, "->")) {
            return new RParserError("Expected \"->\" for builtin function type declaration", fileName, line).raise();
        }

        TypeRef returnType = parseType(false);

        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for builtin function declaration", fileName, line).raise();
        }

        if (!match(STRING)) {
            return new RParserError("Expected string literal for builtin function body", fileName, line).raise();
        }

        String body = consume().value();

        return new BuiltinFunctionDeclarationNode(fileName, line, keepName, name, params, returnType, body);
    }

    private @SubFunc ASTNode parse_IRKeyword() {
        int line = line();

        if (!match(STRING)) {
            return new RParserError("Expected string literal for IR declaration", fileName, line).raise();
        }

        return new IRDeclarationNode(fileName, line, consume().value());
    }

    private ValueNode parseString() {
        return new StringNode(fileName, line(), consume().value());
    }

    private @SubFunc ASTNode parseUsingKeyword() {
        int line = line();

        StringBuilder name = new StringBuilder(identifier());

        while (matchAndConsume(OPERATOR, ".")) {
            name.append("/").append(identifier());
        }

        String pkg;

        if (matchAndConsume(OPERATOR, "in")) {
            if (matchAndConsume(KEYWORD, "self")) {
                pkg = "self";
            } else if (!match(STRING)) {
                return new RParserError("Expected string literal for using package", fileName, line).raise();
            } else {
                pkg = consume().value();
            }
        } else {
            pkg = null;
        }

        ModuleLoadingHelper.collectModuleTypes(fileName, line, fileName, name.toString(), pkg, typeMap).forEach(typeMap::putIfAbsent);

        return new UsingNode(fileName, line, fileName, name.toString(), pkg);
    }

    private @SubFunc ASTNode parsePtrKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for pointer type declaration", fileName, line).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for pointer type declaration", fileName, line).raise();

        if (value instanceof NumberNode n) return new PointerCreationNode(fileName, line, n);

        if (!(value instanceof VariableReference vr))
            return new RParserError("Expected any type of variable reference for pointer type declaration", fileName, line).raise();

        return new ReferenceNode(fileName, line, vr);
    }

    private @SubFunc ValueNode parseDereferenceOperator() {
        int line = line();
        VariableReference holder;

        if (matchAndConsume(KEYWORD, "self")) {
            holder = new DirectVariableReferenceNode(fileName, line, "self");
        } else if (matchAndConsume(KEYWORD, "this")) {
            holder = new DereferenceNode(fileName, line, new DirectVariableReferenceNode(fileName, line, "self"));
        } else if (matchAndConsume(DIVIDER, "(")) {
            ASTNode v = parseValue();
            if (!(v instanceof VariableReference vr))
                return new RParserError("Expected variable reference for dereference operator", fileName, line).raise();
            holder = vr;
            if (!matchAndConsume(DIVIDER, ")")) return new RParserError("Expected ')'", fileName, line).raise();
        } else {
            holder = new DirectVariableReferenceNode(fileName, line, identifier());
        }

        if (match(OPERATOR, ".")) {
            return parseSubExpr(line, false, null, holder);
        }

        if (match(OPERATOR, ":")) {
            return new RParserError("You can't declare mutability or type with dereference operator", fileName, line).raise();
        }

        if (matchAndConsume(OPERATOR, "=")) {
            ValueNode v2 = parseValue();
            return new DereferenceAssignNode(fileName, line, holder, v2);
        }

        return new DereferenceNode(fileName, line, holder);
    }

    private @SubFunc ASTNode parseWhileKeyword() {
        int line = line();

        ValueNode condition = parseCondition();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after while loop condition", fileName, line()).raise();
        }

        var block = parseBlock();

        return new WhileNode(fileName, line, condition, block);
    }

    private @SubFunc ASTNode parseBreakKeyword() {
        return new BreakNode(fileName, previous().line());
    }

    private @SubFunc ASTNode parseContinueKeyword() {
        return new ContinueNode(fileName, previous().line());
    }

    private @SubFunc ASTNode parseFuncKeyword() {
        int line = line();

        String name = identifier();

        var params = parseParamsDeclare(false);

        boolean inline = matchAndConsume(KEYWORD, "inline");

        TypeRef returnType = matchAndConsume(OPERATOR, "->") ? parseType(false) : NoneBuiltinType.INSTANCE;

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for function declaration", fileName, line).raise();
        }

        BlockNode block = parseBlock();

        FunctionDeclarationNode fdn = new FunctionDeclarationNode(fileName, line, false, name, params, inline, returnType, block);

        if (fdn.isMain() && inline) {
            return new RParserError("Cannot inline main function", fileName, line).raise();
        }

        return fdn;
    }

    private @SubFunc ASTNode parseForKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for for loop", fileName, line).raise();

        String name = identifier();

        if (!matchAndConsume(OPERATOR, "in")) {
            return new RParserError("Expected 'in' for for loop", fileName, line).raise();
        }

        var collection = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for for loop", fileName, line).raise();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after for loop collection", fileName, line()).raise();
        }

        var block = parseBlock();

        return new ForLoopNode(fileName, line, name, collection, block);
    }

    private @SubFunc ASTNode parseRangeKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for range declaration", fileName, line()).raise();
        }

        ValueNode first = parseValue();

        if (matchAndConsume(DIVIDER, ")")) {
            return new RangeNode(fileName, line, new NumberNode(fileName, line, "0"), first, new NumberNode(fileName, line, "1"));
        }

        if (!matchAndConsume(DIVIDER, ",")) {
            return new RParserError("Expected ',' or ')' in range declaration", fileName, line()).raise();
        }

        ValueNode second = parseValue();

        if (matchAndConsume(DIVIDER, ")")) {
            return new RangeNode(fileName, line, first, second, new NumberNode(fileName, line, "1"));
        }

        if (!matchAndConsume(DIVIDER, ",")) {
            return new RParserError("Expected ',' before step in range", fileName, line()).raise();
        }

        ValueNode step = parseValue();

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' at end of range", fileName, line()).raise();
        }

        return new RangeNode(fileName, line, first, second, step);
    }

    private @SubFunc ASTNode parseReturnKeyword() {
        int line = line();

        if (match(KEYWORD, "none") || match(NEWLINE)) {
            consume();
            return new ReturnNode(fileName, line, null);
        }

        ValueNode value = parseValue();
        return new ReturnNode(fileName, line, value);
    }

    private @SubFunc ASTNode parseStructKeyword(boolean builtin) {
        int line = line();
        String name = identifier();

        List<String> inherited = new ArrayList<>();
        if (matchAndConsume(KEYWORD, "inherits")) {
            do {
                inherited.add(identifier());
            } while (matchAndConsume(DIVIDER, ","));
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for struct declaration", fileName, line()).raise();
        }
        if (!match(NEWLINE)) {
            return new RParserError("Expected newline after struct declaration", fileName, line()).raise();
        }
        consume();

        if (!match(INDENT)) {
            return new RParserError("Expected indent for struct field declaration", fileName, line()).raise();
        }
        consume();

        List<RStructField> fields = new ArrayList<>();
        List<TypeRef> types = new ArrayList<>();

        while (!match(EOF)) {
            removeNewlines();
            if (match(DEDENT)) {
                consume();

                if (match(INDENT)) {
                    consume();
                    continue;
                } else {
                    break;
                }
            }

            String fieldName = identifier();

            if (!matchAndConsume(OPERATOR, ":")) {
                return new RParserError("Expected ':' for struct field declaration", fileName, line()).raise();
            }

            if (match(KEYWORD, "mut")) {
                return new RParserError("You can't declare fields as mutable", fileName, line()).raise();
            }

            TypeRef fieldType = parseType(false);

            fields.add(new RStructField(fieldName, fieldType));
            types.add(fieldType);
        }

        var type = new StructType(name, types);

        addType(name, type);
        return new StructDeclarationNode(fileName, line, builtin, name, inherited, type, fields);
    }

    private @SubFunc ASTNode parseInitKeyword() {
        int line = line();

        if (matchAndConsume(KEYWORD, "arr")) {
            if (!matchAndConsume(OPERATOR, "->")) {
                return new RParserError("Expected '->' for array type declaration", fileName, line()).raise();
            }
            return parseArrayCreation(parseType(false));
        }

        if (match(DIVIDER, "(")) {
            var params = parseParamsDeclare(false);

            if (!matchAndConsume(OPERATOR, ":")) {
                new RParserError("Expected ':' for function declaration", fileName, line()).raise();
            }

            BlockNode block = parseBlock();
            return new ConstructorDeclarationNode(fileName, line, params, block);
        }

        String name = identifier();

        if (matchAndConsume(OPERATOR, "<")) {
            return parseGenStructInit(name);
        }

        if (!match(DIVIDER, "(")) {
            return new RParserError("Expected '(' for struct initialization", fileName, line()).raise();
        }

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters call", fileName, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return new StructInitNode(fileName, line, name, List.of());
        }

        List<RParamValue> args = new ArrayList<>();

        do {
            String paramName;
            if (match(IDENTIFIER) && next().matches(OPERATOR, "=")) {
                paramName = identifier();
                consume();
            } else {
                paramName = null;
            }
            args.add(new RParamValue(paramName, parseValue()));
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters call", fileName, line()).raise();
        }

        return new StructInitNode(fileName, line, name, args);
    }

    private @SubFunc IfStatementNode parseIfKeyword() {
        int line = line();
        ValueNode condition = parseCondition();
        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after if condition", fileName, line()).raise();
        }

        BlockNode block = parseBlock();

        if (!matchAndConsume(KEYWORD, "else")) {
            return new IfStatementNode(fileName, line, condition, block, null, null);
        }

        if (matchAndConsume(KEYWORD, "if")) {
            IfStatementNode elif = parseIfKeyword();
            return new IfStatementNode(fileName, line, condition, block, elif, null);
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after else block", fileName, line()).raise();
        }

        BlockNode elseBlock = parseBlock();

        return new IfStatementNode(fileName, line, condition, block, null, elseBlock);
    }

    private @SubFunc ASTNode parseSizeofKeyword() {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for sizeof expression", fileName, line()).raise();

        Optional<TypeRef> type = parseOptionalType();

        if (type.isPresent()) {
            if (!matchAndConsume(DIVIDER, ")"))
                return new RParserError("Expected ')' for sizeof expression", fileName, line()).raise();

            return new SizeofNode(fileName, line, type.get());
        }

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for sizeof expression", fileName, line()).raise();
        return new SizeofNode(fileName, line, value);
    }

    private @SubFunc ASTNode parseLenKeyword() {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for len expression", fileName, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for len expression", fileName, line()).raise();
        return new LenNode(fileName, line, value);
    }

    private ValueNode parseCharacter() {
        String value = consume().value();
        if (value.length() != 1) {
            return new RParserError("Expected single character literal", fileName, line()).raise();
        }
        return new CharacterNode(fileName, line(), value.charAt(0));
    }

    private @SubFunc ASTNode parseCastKeyword() {
        int line = line();

        if (!matchAndConsume(OPERATOR, "<"))
            return new RParserError("Expected '<' for cast expression", fileName, line()).raise();

        TypeRef type = parseType(false);

        if (!matchAndConsume(OPERATOR, ">"))
            return new RParserError("Expected '>' for cast expression", fileName, line()).raise();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for cast expression", fileName, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for cast expression", fileName, line()).raise();

        return new CastNode(fileName, line, type, value);
    }

    private @SubFunc ASTNode parseTypeofKeyword(boolean llvm) {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for typeof expression", fileName, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for typeof expression", fileName, line()).raise();
        return new TypeofNode(fileName, line, value, llvm);
    }

    private @SubFunc ASTNode parseImplKeyword() {
        int line = line();

        String name = identifier();

        TypeRef tmp = typeMap.get(name);

        if (!matchAndConsume(OPERATOR, ":") && !(tmp instanceof GenStructType)) {
            return new RParserError("Expected ':' for impl declaration", fileName, line).raise();
        }

        if (tmp instanceof GenStructType gen) return parseGenericImpl(line, gen);

        if (!(tmp instanceof StructType struct))
            return new RParserError("Struct not found: '" + name + "' for impl declaration", fileName, line).raise();

        BlockNode block = parseBlock();

        var iterator = block.getNodes().iterator();

        List<RConstructor> constructors = new ArrayList<>();

        while (iterator.hasNext()) {
            var next = iterator.next();

            if (next.getClass().getSimpleName().contains("Gen"))
                return new RParserError("Generic functions in structs are not supported", fileName, line).raise();
            if (next.getClass().getSimpleName().contains("FunctionDeclarationNode")) continue;
            if (!(next instanceof ConstructorDeclarationNode cdn))
                return new RImplNotFunctionError(fileName, next.getLine()).raise();

            iterator.remove();

            String constructorName = "constructor." + constructors.size();
            RConstructor constructor = new RConstructor(constructorName, cdn.getParameters(), cdn.getBlock());
            constructors.add(constructor);
        }

        return new StructImplNode(fileName, line, struct, constructors, block.getNodes());
    }

    private @SubFunc ASTNode parseGlobal() {
        int line = line();
        String name = identifier();

        TypeRef type = null;

        if (matchAndConsume(OPERATOR, ":")) {
            if (matchAndConsume(KEYWORD, "mut"))
                return new RParserError("Global variables cannot be mutable", fileName, line).raise();
            type = parseType(false);
        }

        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for global variable declaration", fileName, line).raise();
        }

        var value = parseValue();

        return new GlobalVariableDeclarationNode(fileName, line, name, type, value);
    }

    private @SubFunc ASTNode parseRaise() {
        int line = line();

        ValueNode value;

        if (matchAndConsume(KEYWORD, "none")) {
            value = null;
        } else {
            value = parseValue();
        }

        return new RaiseNode(fileName, line, value);
    }

    private @SubFunc ASTNode parseTry() {
        int line = line();

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for try declaration", fileName, line).raise();

        BlockNode tryBlock = parseBlock();

        if (!matchAndConsume(KEYWORD, "catch"))
            return new RParserError("Expected catch block after try declaration", fileName, line).raise();

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for catch declaration", fileName, line).raise();

        BlockNode catchBlock = parseBlock();

        return new TryCatchNode(fileName, line, tryBlock, catchBlock);
    }

    private @SubFunc ASTNode parse_NativeCPPKeyword() {
        return parseExtern(true);
    }

    private ASTNode parseNullKeyword() {
        return new NullNode(fileName, previous().line());
    }

    private @SubFunc ValueNode parseBitwiseNotOperator() {
        int line = line();
        ValueNode value = parseValue();

        return new BitwiseNotNode(fileName, line, value);
    }

    private @SubFunc ASTNode parseDivider() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Unexpected divider: " + current().value(), fileName, line).raise();
        }
        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected closing divider ')'", fileName, line).raise();
        }

        return parseSubExpr(line, false, null, value);
    }

    private @SubFunc ValueNode parseSubExpr(int line, boolean self, String name, ValueNode node) {
        while (true) {
            if (outOfBounds(0)) break;
            line = line();

            if (match(DIVIDER, "(")) {
                if (self) {
                    return new RParserError("Using self as function call", fileName, line).raise();
                }
                if (name == null) {
                    return new RParserError("You can't call a function here", fileName, line).raise();
                }
                var args = parseParamsCall();

                if (node instanceof DirectVariableReferenceNode) {
                    node = new FunctionCallNode(fileName, line, name, args);
                    continue;
                }

                return new RParserError("Invalid parenthesis", fileName, line).raise();
            }

            if (matchAndConsume(DIVIDER, "[")) {
                if (self) {
                    return new RParserError("Using self as array access", fileName, line).raise();
                }

                ValueNode index = parseValue();

                if (!matchAndConsume(DIVIDER, "]")) {
                    return new RParserError("Expected ']'", fileName, line).raise();
                }

                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new ArraySetNode(fileName, line, node, index, value);
                } else {
                    node = new ArrayAccessNode(fileName, line, node, index);
                }
                continue;
            }

            if (matchAndConsume(OPERATOR, "@")) {
                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new DereferenceAssignNode(fileName, line, node, value);
                    continue;
                }

                if (!(node instanceof VariableReference vr)) {
                    return new RParserError("Expected variable reference for dereference operator", fileName, line).raise();
                }

                node = new DereferenceNode(fileName, line, vr);
                continue;
            }

            if (matchAndConsume(OPERATOR, ".")) {
                String fieldName = identifier();
                if (!(node instanceof VariableReference vr)) {
                    return new RParserError("Expected reference for struct access", fileName, line).raise();
                }

                if (!match(DIVIDER, "(")) {
                    node = new StructFieldAccessNode(fileName, line, vr, fieldName);
                    continue;
                }

                var args = parseParamsCall();

                node = new StructFunctionCallNode(fileName, line, node, fieldName, args);
                continue;
            }

            if (matchAndConsume(OPERATOR, "::")) {
                if (self) return new RParserError("Self for namespace is not allowed", fileName, line).raise();

                String fieldName = identifier();

                if (matchAndConsume(OPERATOR, "<")) {
                    List<TypeRef> genTypes = new ArrayList<>();

                    if (!match(OPERATOR, ">")) {
                        do {
                            genTypes.add(parseType(false));
                        } while (matchAndConsume(DIVIDER, ","));
                    }

                    if (!matchAndConsume(OPERATOR, ">")) {
                        return new RParserError("Expected '>' for generic function declaration", fileName, line).raise();
                    }

                    var params = parseParamsCall();

                    var inner = new GenericFunctionCallNode(fileName, line, fieldName, genTypes, params);

                    node = new NamespaceCallNode(fileName, line, name, inner);
                    continue;
                }

                if (!match(DIVIDER, "(")) {
                    node = new NamespaceCallNode(fileName, line, name, new DirectVariableReferenceNode(fileName, line, fieldName));
                    continue;
                }

                var args = parseParamsCall();

                node = new NamespaceCallNode(fileName, line, name, new FunctionCallNode(fileName, line, fieldName, args));
            }

            if (matchAndConsume(OPERATOR, "<")) {
                List<TypeRef> genTypes = new ArrayList<>();

                if (!match(OPERATOR, ">")) {
                    Optional<TypeRef> type = parseOptionalType();
                    if (type.isPresent()) {
                        genTypes.add(type.get());
                        while (matchAndConsume(DIVIDER, ",")) {
                            genTypes.add(parseType(false));
                        }
                    } else {
                        tokenIndex--;
                        continue;
                    }
                }

                if (!matchAndConsume(OPERATOR, ">")) {
                    return new RParserError("Expected '>' for generic function declaration", fileName, line).raise();
                }

                var params = parseParamsCall();

                node = new GenericFunctionCallNode(fileName, line, name, genTypes, params);
                continue;
            }

            break;
        }

        if (matchAndConsume(KEYWORD, "is")) {
            if (matchAndConsume(OPERATOR, "not")) {
                TypeRef type = parseType(false);
                return new BinaryExpressionNode(line, fileName, new IsNode(fileName, line, node, type), EqualsBO.INSTANCE, new BooleanNode(fileName, line, false));
            }
            TypeRef type = parseType(false);
            return new IsNode(fileName, line, node, type);
        }

        if (match(OPERATOR, ":")) {
            int preIndex = tokenIndex;
            consume();

            boolean mut = matchAndConsume(KEYWORD, "mut");
            Optional<TypeRef> t = parseOptionalType();

            tokenIndex = preIndex;
            if (t.isPresent() || mut) {
                if (!(node instanceof VariableReference vr)) {
                    return new RParserError("Expected reference for assignment", fileName, line).raise();
                }
                return parseVariableAssignment(line, vr);
            }
        } else if (match(OPERATOR, "=")) {
            if (!(node instanceof VariableReference vr)) {
                return new RParserError("Expected reference for assignment", fileName, line).raise();
            }
            return parseVariableAssignment(line, vr);
        }

        if (match(OPERATOR)) {
            String opSymbol = current().value();

            if (BinaryOperators.getBySymbol(opSymbol) != null || (opSymbol.length() > 2 && opSymbol.endsWith("=")))
                return node;

            String opAssignSymbol = opSymbol.substring(0, opSymbol.length() - 1);

            var opAssign = BinaryOperators.getBySymbol(opAssignSymbol);

            if (opAssign == null) return node;

            if (!(node instanceof VariableReference leftRef)) {
                return new RParserError("Expected variable reference for assignment operation", fileName, line).raise();
            }

            consume();

            ValueNode value = new BinaryExpressionNode(line, fileName, node, opAssign, parseValue());

            return new VariableDeclarationNode(fileName, line, leftRef, false, null, value);
        }

        return node;
    }

    private @SubFunc ValueNode parseTernaryOperator(ValueNode thenExpr) {
        int line = line();

        ValueNode condition = parseValue();

        if (!matchAndConsume(KEYWORD, "else")) {
            return new RParserError("Expected 'else' for ternary operator", fileName, line).raise();
        }

        ValueNode elseExpr = parseValue();

        return new TernaryOperatorNode(fileName, line, condition, thenExpr, elseExpr);
    }

    private @SubFunc ValueNode parseAsyncKeyword() {
        int line = line();

        TypeRef returnType;

        if (matchAndConsume(DIVIDER, "(")) {
            returnType = parseType(false);
            if (!matchAndConsume(DIVIDER, ")")) {
                return new RParserError("Expected ')' for async function declaration", fileName, line).raise();
            }
        } else {
            returnType = NoneBuiltinType.INSTANCE;
        }

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for async declaration", fileName, line).raise();

        if (!match(NEWLINE)) new RParserError("Expected newline after async declaration", fileName, line).raise();

        consume();

        BlockNode block = parseBlock();

        return new AsyncDeclarationNode(fileName, line, returnType, block);
    }

    private @SubFunc ASTNode parseGenericKeyword() {
        int line = line();

        if (matchAndConsume(KEYWORD, "func")) {
            return parseGenericFunc(line);
        } else if (matchAndConsume(KEYWORD, "struct")) {
            return parseGenericStruct(line);
        }

        return new RParserError("Expected 'func' or 'struct' for generic function declaration", fileName, line).raise();
    }

    private @SubFunc ASTNode parseGenericFunc(int line) {
        String name = identifier();
        var typeParameters = parseTypeParameters();

        List<String> old = currentGenericTypes;
        currentGenericTypes = new ArrayList<>(typeParameters.stream().map(TypeParameter::name).toList());

        var params = parseParamsDeclare(true);
        TypeRef returnType = matchAndConsume(OPERATOR, "->") ? parseType(true) : NoneBuiltinType.INSTANCE;

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for function declaration", fileName, line).raise();
        }

        BlockNode block = parseBlock();

        currentGenericTypes = old;

        return new GenFunctionDeclarationNode(fileName, line, name, typeParameters, params, returnType, block);
    }

    private @SubFunc ASTNode parseGenericStruct(int line) {
        String name = identifier();

        var typeParameters = parseTypeParameters();

        List<String> inherited = new ArrayList<>();
        if (matchAndConsume(KEYWORD, "inherits")) {
            do {
                inherited.add(identifier());
            } while (matchAndConsume(DIVIDER, ","));
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for struct declaration", fileName, line).raise();
        }

        if (!match(NEWLINE)) {
            return new RParserError("Expected newline after struct declaration", fileName, line).raise();
        }
        consume();

        if (!match(INDENT)) {
            return new RParserError("Expected indent for struct field declaration", fileName, line).raise();
        }
        consume();

        List<RStructField> fields = new ArrayList<>();
        List<TypeRef> fieldTypes = new ArrayList<>();

        while (!match(EOF)) {
            removeNewlines();

            if (match(DEDENT)) {
                consume();
                break;
            }

            String fieldName = identifier();

            if (!matchAndConsume(OPERATOR, ":")) {
                return new RParserError("Expected ':' for struct field declaration", fileName, line).raise();
            }

            if (match(KEYWORD, "mut")) {
                return new RParserError("You can't declare fields as mutable", fileName, line).raise();
            }

            TypeRef fieldType = parseType(true);

            fields.add(new RStructField(fieldName, fieldType));
            fieldTypes.add(fieldType);
        }

        GenStructType type = new GenStructType(typeParameters, name, fieldTypes);

        addType(name, type);

        return new GenStructDeclarationNode(fileName, line, name, inherited, type, fields);
    }

    private @SubFunc ValueNode parseArrayCreation(TypeRef type) {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for array creation", fileName, line()).raise();

        ValueNode size = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for array creation", fileName, line()).raise();

        return new ArrayCreationNode(fileName, line, type, size);
    }

    private @SubFunc ValueNode parseThisKeyword() {
        int line = line();

        ValueNode node = new DereferenceNode(fileName, line, new DirectVariableReferenceNode(fileName, line, "self"));

        node = parseSubExpr(line, false, null, node);

        return node;
    }

    private @SubFunc ValueNode parseGenStructInit(String name) {
        int line = line();

        List<TypeRef> genericTypes = new ArrayList<>();
        if (!matchAndConsume(OPERATOR, ">")) {
            do {
                genericTypes.add(parseType(false));
            } while (matchAndConsume(DIVIDER, ","));
            if (!matchAndConsume(OPERATOR, ">")) {
                return new RParserError("Expected '>' for generic struct initialization", fileName, line).raise();
            }
        }

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters call", fileName, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return new GenStructInitNode(fileName, line, name, genericTypes, List.of());
        }

        List<RParamValue> args = new ArrayList<>();

        do {
            String paramName;
            if (match(IDENTIFIER) && next().matches(OPERATOR, "=")) {
                paramName = identifier();
                consume();
            } else {
                paramName = null;
            }
            args.add(new RParamValue(paramName, parseValue()));
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters call", fileName, line()).raise();
        }

        return new GenStructInitNode(fileName, line, name, genericTypes, args);
    }

    private @SubFunc ASTNode parseGenericImpl(final int line, final GenStructType type) {
        var typeParameters = parseTypeParameters();

        if (typeParameters.size() != type.genericTypes().size()) {
            return new RParserError("Expected " + type.genericTypes().size() + " generic parameters but got " + typeParameters.size(), fileName, line).raise();
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for generic impl declaration", fileName, line).raise();
        }

        removeNewlines();

        if (!match(INDENT)) {
            return new RParserError("Expected indented block", fileName, line()).raise();
        }

        consume();

        var typeParamsStr = typeParameters.stream().map(TypeParameter::name).toList();
        List<String> old = currentGenericTypes;
        currentGenericTypes = new ArrayList<>(typeParamsStr);

        List<ASTNode> statements = new ArrayList<>();

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) break;

            ASTNode n = matchAndConsume(KEYWORD, "init") ? parseGenConstructor(line, typeParamsStr) : parseStatement();
            if (n == null) break;
            statements.add(n);
        }

        currentGenericTypes = old;

        if (!match(EOF)) {
            if (match(DEDENT)) {
                consume();
            } else {
                return new RParserError("Expected dedent to close block", fileName, line()).raise();
            }
        }


        var iterator = statements.iterator();
        List<RConstructor> constructors = new ArrayList<>();
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (next.getClass().getSimpleName().contains("FunctionDeclarationNode")) continue;

            if (!(next instanceof ConstructorDeclarationNode cdn))
                return new RImplNotFunctionError(fileName, next.getLine()).raise();

            iterator.remove();
            String constructorName = "constructor." + constructors.size();
            constructors.add(new RConstructor(constructorName, cdn.getParameters(), cdn.getBlock()));
        }

        return new GenStructImplNode(fileName, line, type, typeParameters, constructors, statements);
    }

    private @SubFunc ASTNode parseGenConstructor(final int line, final List<String> implGenerics) {
        var params = parseParamsDeclare(true);

        for (final FunctionParameter param : params) {
            TypeRef t = param.type();
            while (true) {
                if (t instanceof PointerType ptr) {
                    t = ptr.inner();
                    continue;
                }
                if (t instanceof ArrayType arr) {
                    t = arr.inner();
                    continue;
                }
                if (!(t instanceof GenericType gen)) break;

                if (implGenerics.contains(gen.name())) break;

                return new RParserError("Unknown generic type: " + gen.name(), fileName, line).raise();
            }
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            new RParserError("Expected ':' for function declaration", fileName, line()).raise();
        }

        BlockNode block = parseBlock();
        return new ConstructorDeclarationNode(fileName, line, params, block);
    }

    private @SubFunc ASTNode parseMatchKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for match declaration", fileName, line).raise();
        }
        ValueNode value = parseValue();
        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for match declaration", fileName, line).raise();
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for match declaration", fileName, line).raise();
        }

        removeNewlines();

        if (!match(INDENT)) {
            return new RParserError("Expected indented block", fileName, line()).raise();
        }

        consume();

        List<MatchNode.MatchCase> cases = new ArrayList<>();
        boolean def = false;

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) break;
            if (def)
                return new RParserError("Match cases cannot be declared after default case", fileName, line()).raise();

            var mc = parseMatchCase();
            if (mc.isDefault()) def = true;
            cases.add(mc);
        }

        if (!match(EOF)) {
            if (match(DEDENT)) {
                consume();
            } else {
                return new RParserError("Expected dedent to close block", fileName, line()).raise();
            }
        }

        return new MatchNode(fileName, line, value, cases);
    }

    private @SubFunc MatchNode.MatchCase parseMatchCase() {
        int line = line();

        List<ValueNode> values;
        if (matchAndConsume(OPERATOR, "_")) {
            values = null;
        } else {
            values = new ArrayList<>();
            do {
                values.add(parseValue());
            } while (matchAndConsume(DIVIDER, ","));
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for match case declaration", fileName, line).raise();
        }

        BlockNode b = parseBlock();

        return new MatchNode.MatchCase(values, b);
    }

    private @SubFunc LambdaDeclarationNode parseLambdaKeyword() {
        int line = line();
        var params = parseParamsDeclare(false);

        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for lambda declaration", fileName, line).raise();
        }

        ValueNode value = parseValue();

        return new LambdaDeclarationNode(fileName, line, params, value);
    }

    private @SubFunc NamespaceDeclarationNode parseNamespaceKeyword() {
        int line = line();
        String name = identifier();
        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for namespace declaration", fileName, line).raise();

        BlockNode block = parseBlock();
        return new NamespaceDeclarationNode(fileName, line, name, block);
    }

    private @SubFunc ASTNode parseTypeKeyword() {
        int line = line();
        String name = identifier();
        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for type declaration", fileName, line).raise();
        }
        TypeRef type = parseType(false);

        addType(name, type);

        return parseStatement();
    }

    private @SubFunc EnumDeclarationNode parseEnumKeyword() {
        int line = line();
        String name = identifier();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for enum declaration", fileName, line).raise();
        }

        if (!match(NEWLINE)) {
            return new RParserError("Expected newline after enum declaration", fileName, line).raise();
        }
        consume();

        if (!match(INDENT)) {
            return new RParserError("Expected indented enum body", fileName, line).raise();
        }
        consume();

        Map<String, ValueNode> fields = new LinkedHashMap<>();
        Boolean withValues = null;

        int idx = 0;
        boolean addedType = false;

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) {
                break;
            }

            int fieldLine = line();
            String fieldName = identifier();

            ValueNode value = null;

            if (matchAndConsume(OPERATOR, "=")) {
                value = parseValue();
            }

            if (withValues == null) {
                withValues = value != null;
            } else if (withValues == (value == null)) {
                return new RParserError("Either all enum fields must have values, or none of them may have values", fileName, fieldLine).raise();
            }

            if (value == null) {
                value = new NumberNode(fileName, line, String.valueOf(idx));
                idx++;
            }

            if (!addedType) {
                addedType = true;
                addType(fieldName, value.getType());
            }

            fields.put(fieldName, value);
        }

        if (match(DEDENT)) {
            consume();
        } else if (!match(EOF)) {
            return new RParserError("Expected dedent to close enum declaration", fileName, line).raise();
        }

        return new EnumDeclarationNode(fileName, line, name, fields);
    }

    private @SubFunc NativeCPPNode parseExternKeyword() {
        return parseExtern(false);
    }

    private @SubFunc NativeCPPNode parseExtern(boolean isNative) {
        int line = line();
        String ncn = isNative ? "Native CPP" : "extern";

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for " + ncn + " declaration", fileName, line).raise();

        if (!match(STRING))
            return new RParserError("Expected a string for file name in " + ncn + " declaration", fileName, line).raise();

        String name = consume().value();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for " + ncn + " declaration", fileName, line).raise();

        if (matchAndConsume(KEYWORD, "_Builtin")) {
            return new NativeCPPNode(fileName, line, isNative, name, List.of());
        }

        List<RFunction> functions = new ArrayList<>();
        do {
            TypeRef type = parseType(false);
            String funcName = identifier();
            var params = parseParamsDeclare(false);

            functions.add(new RDefFunction(funcName, funcName, type, params));
        } while (matchAndConsume(OPERATOR, "and"));

        return new NativeCPPNode(fileName, line, isNative, name, functions);
    }

    private @SubFunc ASTNode parseTraitKeyword() {
        int line = line();
        String name = identifier();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for trait declaration", fileName, line).raise();
        }

        if (!match(NEWLINE)) {
            return new RParserError("Expected newline after trait declaration", fileName, line).raise();
        }
        consume();

        if (!match(INDENT)) {
            return new RParserError("Expected indented enum body", fileName, line).raise();
        }
        consume();

        Map<String, TraitFunction> functions = new LinkedHashMap<>();

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) {
                break;
            }

            if (!matchAndConsume(KEYWORD, "func")) {
                return new RParserError("Expected 'func' for trait declaration", fileName, line).raise();
            }

            String fname = identifier();

            List<FunctionParameter> params = parseParamsDeclare(false);

            TypeRef returnType = matchAndConsume(OPERATOR, "->") ? parseType(false) : NoneBuiltinType.INSTANCE;

            if (functions.containsKey(fname)) {
                return new RParserError("Duplicate trait function: " + fname, fileName, line).raise();
            }

            functions.put(fname, new TraitFunction(fname, params, returnType));
        }

        if (match(DEDENT)) {
            consume();
        } else if (!match(EOF)) {
            return new RParserError("Expected dedent to close enum declaration", fileName, line).raise();
        }

        if (!typeMap.containsKey(name)) typeMap.put(name, new TraitType(name, functions));

        return new TraitDeclarationNode(fileName, line, name, functions);
    }

    /*
    general utils
     */

    private @SubFunc List<TypeParameter> parseTypeParameters() {
        int line = line();
        List<TypeParameter> typeParameters = new ArrayList<>();

        if (!matchAndConsume(OPERATOR, "<")) {
            return new RParserError("Expected '<' for generic struct declaration", fileName, line).raise();
        }

        if (!match(OPERATOR, ">")) {
            do {
                typeParameters.add(new TypeParameter(identifier(), matchAndConsume(KEYWORD, "inherits") ? identifier() : null));
            } while (matchAndConsume(DIVIDER, ","));
        }

        if (!matchAndConsume(OPERATOR, ">")) {
            return new RParserError("Expected '>' for generic struct declaration", fileName, line).raise();
        }

        return typeParameters;
    }

    private @SubFunc Optional<TypeRef> parseOptionalType() {
        int line = line();
        int preType = tokenIndex;

        TypeRef type;

        LABEL_TYPE:
        {
            String typeName = consume().value();

            switch (typeName) {
                case "ptr" -> {
                    type = parsePointerType(line, false);
                    break LABEL_TYPE;
                }
                case "arr" -> {
                    type = parseArrayType(line, false);
                    break LABEL_TYPE;
                }
                case "struct" -> {
                    type = typeMap.get(typeName);
                    if (!(type instanceof StructType || type instanceof GenStructType))
                        return new RParserError("Expected struct type", fileName, line()).raise();
                    break LABEL_TYPE;
                }
                case "lambda" -> {
                    type = parseLambdaType(line, false);
                    break LABEL_TYPE;
                }
            }

            if (typeMap.containsKey(typeName)) {
                type = typeMap.get(typeName);
                break LABEL_TYPE;
            }

            type = BuiltinTypes.getByName(typeName);
        }

        if (type != null) {
            return Optional.of(type);
        }

        tokenIndex = preType;
        return Optional.empty();
    }

    private @SubFunc TypeRef parseType0(final boolean generics) {
        if (!match(IDENTIFIER) && !match(KEYWORD)) {
            return new RParserError("Expected type name", fileName, line()).raise();
        }

        int line = line();

        String typeName = consume().value();

        switch (typeName) {
            case "ptr" -> {
                return parsePointerType(line, generics);
            }
            case "arr" -> {
                return parseArrayType(line, generics);
            }
            case "struct" -> {
                var type = typeMap.get(identifier());
                if (!(type instanceof StructType)) {
                    return new RParserError("Expected struct type", fileName, line()).raise();
                }
                return type;
            }
            case "lambda" -> {
                return parseLambdaType(line, generics);
            }
        }

        if (typeMap.containsKey(typeName)) {
            return typeMap.get(typeName);
        }

        TypeRef type = BuiltinTypes.getByName(typeName);

        if (type == null) {
            if (currentGenericTypes.contains(typeName) || generics) {
                return new GenericType(typeName);
            }
            return new RParserError("Unknown type: " + typeName, fileName, line).raise();
        }

        addType(typeName, type);

        return type;
    }

    private @SubFunc TypeRef parseType(final boolean generics) {
        TypeRef t = parseType0(generics);

        if (t instanceof GenStructType gen) {
            if (!matchAndConsume(OPERATOR, "<")) return t;

            List<TypeRef> genericTypes = new ArrayList<>();

            if (!match(OPERATOR, ">")) {
                do {
                    genericTypes.add(parseType(generics));
                } while (matchAndConsume(DIVIDER, ","));
            }

            if (match(OPERATOR, ">>")) {
                tokens[tokenIndex] = new Token(OPERATOR, ">", current().line());
            } else if (match(OPERATOR, ">>>")) {
                tokens[tokenIndex] = new Token(OPERATOR, ">>", current().line());
            } else if (!matchAndConsume(OPERATOR, ">")) {
                return new RParserError("Expected '>' for generic type", fileName, line()).raise();
            }

            if (gen.genericTypes().size() != genericTypes.size()) {
                return new RParserError("Expected " + gen.genericTypes().size() + " generic arguments but got " + genericTypes.size(), fileName, line()).raise();
            }

            return new AppliedGenStructType(gen, genericTypes);
        }

        return t;
    }

    private @SubFunc TypeRef parsePointerType(int line, boolean generics) {
        if (!matchAndConsume(OPERATOR, "->"))
            return new RParserError("Expected \"->\" for pointer type declaration", fileName, line).raise();

        TypeRef inner = parseType(generics);
        if (inner instanceof NoneBuiltinType)
            return new RParserError("You can't declare a void pointer, please use 'anyptr' instead", fileName, line).raise();
        return new PointerType(inner);
    }

    private @SubFunc TypeRef parseArrayType(int line, boolean generics) {
        if (!matchAndConsume(OPERATOR, "->")) {
            return new RParserError("Expected \"->\" for array type declaration", fileName, line).raise();
        }

        TypeRef inner = parseType(generics);

        if (inner instanceof NoneBuiltinType) {
            return new RArrayTypeIsNoneError(fileName, line).raise();
        }

        return new ArrayType(ArrayType.UNKNOWN_SIZE, inner);
    }

    private @SubFunc TypeRef parseLambdaType(int line, boolean generics) {
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for lambda type declaration", fileName, line).raise();
        List<TypeRef> params = new ArrayList<>();
        if (!matchAndConsume(DIVIDER, ")")) {
            do {
                params.add(parseType(generics));
            } while (matchAndConsume(DIVIDER, ","));

            if (!matchAndConsume(DIVIDER, ")"))
                return new RParserError("Expected ')' for lambda type declaration", fileName, line).raise();
        }

        if (!matchAndConsume(OPERATOR, "->"))
            return new RParserError("Expected \"->\" for lambda type declaration", fileName, line).raise();

        TypeRef inner = parseType(generics);
        return new LambdaType(params, inner);
    }

    private @SubFunc List<FunctionParameter> parseParamsDeclare(final boolean generics) {
        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters declaration", fileName, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return List.of();
        }

        List<FunctionParameter> params = new java.util.ArrayList<>();

        do {
            String name = identifier();
            if (!matchAndConsume(OPERATOR, ":")) {
                return new RParserError("Expected ':' for parameters type declaration", fileName, line()).raise();
            }

            boolean mutable = matchAndConsume(KEYWORD, "mut");

            TypeRef type = parseType(generics);

            if (type instanceof RangeType) {
                return new RRangeTypeError(fileName, line()).raise();
            }

            var param = new FunctionParameter(name, mutable, type);

            params.add(param);
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters declaration", fileName, line()).raise();
        }

        return params;
    }

    private @SubFunc List<ValueNode> parseParamsCall() {
        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters call", fileName, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return List.of();
        }

        List<ValueNode> args = new ArrayList<>();

        do {
            args.add(parseValue());
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters call", fileName, line()).raise();
        }

        return args;
    }

    private @SubFunc ValueNode parseCondition() {
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for the condition", fileName, line()).raise();
        ValueNode condition = parseValue();
        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for the condition", fileName, line()).raise();

        return condition;
    }

    private ValueNode parseArrayDeclaration() {
        int line = line();
        List<ValueNode> values = new ArrayList<>();

        if (matchAndConsume(DIVIDER, "]")) return new ArrayNode(fileName, line, List.of());

        do {
            values.add(parseValue());
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, "]"))
            return new RParserError("Expected ']' for array declaration", fileName, line()).raise();

        return new ArrayNode(fileName, line, values);
    }

    /*
       token util functions
     */

    private void removeNewlines() {
        while (match(NEWLINE)) consume();
    }

    private Token next() {
        checkTokenIndex(1);
        return tokens[tokenIndex + 1];
    }

    private String identifier() {
        if (!match(IDENTIFIER)) {
            return new RParserError("Expected identifier", fileName, line()).raise();
        }

        return consume().value();
    }

    private boolean outOfBounds(int add) {
        return tokens.length <= tokenIndex + add || tokens[tokenIndex + add].type() == EOF;
    }

    private void checkTokenIndex(int add) {
        if (!outOfBounds(add)) {
            return;
        }

        new RParserError("Unexpected end of file", fileName, previous().line()).raise();
    }

    private Token consume() {
        checkTokenIndex(0);
        return tokens[tokenIndex++];
    }

    private Token current() {
        checkTokenIndex(0);
        return tokens[tokenIndex];
    }

    private int line() {
        if (outOfBounds(0)) return tokens[tokenIndex - 1].line();
        return current().line();
    }

    private Token previous() {
        checkTokenIndex(-1);
        return tokens[tokenIndex - 1];
    }

    private boolean match(TokenType type) {
        if (outOfBounds(0)) return type == EOF;
        return current().matches(type);
    }

    private boolean match(TokenType type, String value) {

        if (outOfBounds(0)) return false;
        return current().matches(type, value);
    }

    private boolean matchAndConsume(TokenType type, String value) {
        if (match(type, value)) {
            consume();
            return true;
        }

        return false;
    }

    /*
    external utilities
     */

    public void collectTypesOnly() {
        tokenIndex = 0;

        while (!match(EOF)) {
            removeNewlines();

            if (match(KEYWORD, "generic")) {
                consume();

                if (match(KEYWORD, "struct")) {
                    consume();
                    collectStructType(true);
                    continue;
                }
            }

            if (match(KEYWORD, "_Builtin")) {
                consume();

                if (match(KEYWORD, "struct")) {
                    consume();
                    collectStructType(false);
                    continue;
                }
            }

            if (match(KEYWORD, "struct")) {
                consume();
                collectStructType(false);
                continue;
            }

            if (match(KEYWORD, "using")) {
                consume();
                collectUsingTypes();
                continue;
            }

            if (match(KEYWORD, "enum")) {
                consume();
                collectEnumTypes();
                continue;
            }

            if (outOfBounds(0)) return;
            consume();
        }
    }

    private void collectStructType(boolean generic) {
        if (!match(IDENTIFIER)) {
            return;
        }

        String name = consume().value();

        List<TypeParameter> generics = new ArrayList<>();

        if (generic) {
            generics = parseTypeParameters();
        }

        while (!match(OPERATOR, ":") && !match(EOF)) {
            consume();
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return;
        }

        removeNewlines();
        if (!match(INDENT)) {
            return;
        }
        consume();

        List<TypeRef> fieldTypes = new ArrayList<>();

        while (!match(DEDENT) && !match(EOF)) {
            removeNewlines();

            if (match(DEDENT)) break;

            if (!match(IDENTIFIER)) {
                consume();
                continue;
            }
            consume();

            if (!matchAndConsume(OPERATOR, ":")) {
                continue;
            }

            TypeRef fieldType = parseType(generic);
            fieldTypes.add(fieldType);

            if (match(OPERATOR, "=")) {
                do {
                    consume();
                } while (!match(NEWLINE) && !match(DEDENT) && !match(EOF));
            }
        }

        if (match(DEDENT)) consume();

        TypeRef type = generic ? new GenStructType(generics, name, fieldTypes) : new StructType(name, fieldTypes);

        addType(name, type);
    }

    private void addType(String name, TypeRef type) {
        typeMap.putIfAbsent(name, type);
    }

    private void collectUsingTypes() {
        int line = line();
        StringBuilder name = new StringBuilder(identifier());

        while (matchAndConsume(OPERATOR, ".")) {
            name.append("/").append(identifier());
        }

        String pkg;

        if (matchAndConsume(OPERATOR, "in")) {
            if (matchAndConsume(KEYWORD, "self")) {
                pkg = "self";
            } else {
                pkg = consume().value();
            }
        } else {
            pkg = null;
        }

        ModuleLoadingHelper.collectModuleTypes(fileName, line, fileName, name.toString(), pkg, typeMap).forEach(typeMap::putIfAbsent);
    }

    private void collectEnumTypes() {
        String enumName = identifier();

        if (!matchAndConsume(OPERATOR, ":")) {
            return;
        }

        removeNewlines();

        if (!match(INDENT)) {
            return;
        }
        consume();

        TypeRef valueType = null;

        while (!match(DEDENT) && !match(EOF)) {
            removeNewlines();

            if (match(DEDENT) || match(EOF)) {
                break;
            }

            identifier();

            if (matchAndConsume(OPERATOR, "=")) {
                ValueNode value = parseValue();

                if (value.getClass().getPackage().getName().contains("constants")) {
                    if (valueType == null) {
                        valueType = value.getType();
                    }
                }
            } else {
                if (valueType == null) {
                    valueType = BuiltinTypes.INT.getType();
                }
            }

            while (!match(NEWLINE) && !match(DEDENT) && !match(EOF)) {
                consume();
            }
        }

        if (match(DEDENT)) {
            consume();
        }

        if (valueType == null) {
            valueType = BuiltinTypes.INT.getType();
        }

        addType(enumName, valueType);
    }
}
