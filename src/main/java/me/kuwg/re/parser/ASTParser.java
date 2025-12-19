package me.kuwg.re.parser;

import me.kuwg.re.ast.AST;
import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.array.ArrayAccessNode;
import me.kuwg.re.ast.nodes.array.ArrayNode;
import me.kuwg.re.ast.nodes.array.ArraySetNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.ReturnNode;
import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.constants.*;
import me.kuwg.re.ast.nodes.expression.BinaryExpressionNode;
import me.kuwg.re.ast.nodes.expression.BitwiseNotNode;
import me.kuwg.re.ast.nodes.extern.NativeCPPNode;
import me.kuwg.re.ast.nodes.function.*;
import me.kuwg.re.ast.nodes.global.GlobalVariableDeclarationNode;
import me.kuwg.re.ast.nodes.instance.IsNode;
import me.kuwg.re.ast.nodes.ir.IRDeclarationNode;
import me.kuwg.re.ast.nodes.len.LenNode;
import me.kuwg.re.ast.nodes.loop.BreakNode;
import me.kuwg.re.ast.nodes.loop.ContinueNode;
import me.kuwg.re.ast.nodes.loop.ForLoopNode;
import me.kuwg.re.ast.nodes.loop.WhileNode;
import me.kuwg.re.ast.nodes.module.UsingNode;
import me.kuwg.re.ast.nodes.pointer.DereferenceAssignNode;
import me.kuwg.re.ast.nodes.pointer.DereferenceNode;
import me.kuwg.re.ast.nodes.pointer.PointerCreationNode;
import me.kuwg.re.ast.nodes.pointer.ReferenceNode;
import me.kuwg.re.ast.nodes.raise.RaiseNode;
import me.kuwg.re.ast.nodes.range.RangeNode;
import me.kuwg.re.ast.nodes.sizeof.SizeofNode;
import me.kuwg.re.ast.nodes.statement.IfStatementNode;
import me.kuwg.re.ast.nodes.statement.TryCatchNode;
import me.kuwg.re.ast.nodes.struct.StructDeclarationNode;
import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.ast.nodes.struct.StructInitNode;
import me.kuwg.re.ast.nodes.ternary.TernaryOperatorNode;
import me.kuwg.re.ast.nodes.type.TypeofNode;
import me.kuwg.re.ast.nodes.variable.DirectVariableReferenceNode;
import me.kuwg.re.ast.nodes.variable.VariableDeclarationNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.expr.RImplNotFunctionError;
import me.kuwg.re.error.errors.parser.RParserError;
import me.kuwg.re.operator.BinaryOperators;
import me.kuwg.re.operator.ops.add.AddBO;
import me.kuwg.re.operator.ops.add.SubBO;
import me.kuwg.re.token.Token;
import me.kuwg.re.token.TokenType;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.kuwg.re.token.TokenType.*;

public class ASTParser {
    public final Map<String, TypeRef> typeMap;
    private final String file;
    private final Token[] tokens;
    private final boolean initial;
    private int tokenIndex;

    public ASTParser(final String file, final Token[] tokens) {
        this.file = file;
        this.tokens = tokens;
        this.initial = true;
        this.typeMap = new HashMap<>();
    }

    public ASTParser(final String file, final Token[] tokens, final Map<String, TypeRef> typeMap) {
        this.file = file;
        this.tokens = tokens;
        this.initial = true;
        this.typeMap = typeMap;
    }

    private static void includeInitialModules(AST ast) {
        ast.addChild(new UsingNode(0, null, "default\\io", null));     // io
        ast.addChild(new UsingNode(0, null, "default\\system", null)); // system
    }

    public AST parse() {
        AST ast = new AST();

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
            return new RParserError("Expected indented block", file, line()).raise();
        }

        consume();

        List<ASTNode> statements = new ArrayList<>();

        while (!match(EOF) && !match(DEDENT)) {
            removeNewlines();

            if (match(EOF) || match(DEDENT)) break;

            ASTNode stmt = parseStatement();
            statements.add(stmt);
        }

        if (match(EOF)) return new BlockNode(statements);
        if (match(DEDENT)) {
            consume();
        } else {
            return new RParserError("Expected dedent to close block", file, line()).raise();
        }

        return new BlockNode(statements);
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
            consume(); // consume operator token
            ValueNode right = parseExpression(op.getPrecedence() + 1);

            left = new BinaryExpressionNode(line, left, op, right);
        }

        if (matchAndConsume(KEYWORD, "if")) return parseTernaryOperator(left);

        return left;
    }

    private ASTNode parseStatement() {
        removeNewlines();

        int line = line();

        ASTNode n = switch (current().type()) {
            case INDENT -> new RParserError("Unexpected indent", file, line).raise();
            case KEYWORD -> parseKeyword();
            case IDENTIFIER -> parseIdentifier(false);
            case NUMBER -> parseNumber();
            case STRING -> parseString();
            case CHARACTER -> parseCharacter();
            case OPERATOR -> {
                if (matchAndConsume(OPERATOR, "@")) yield parseDereferenceOperator();
                else if (matchAndConsume(OPERATOR, "~")) yield parseBitwiseNotOperator();
                else
                    yield new RParserError("Unexpected operator in statement: " + current().value(), file, line).raise();
            }
            case DIVIDER -> parseDivider();
            default ->
                    new RParserError("Unexpected token: " + current().value() + ", type: " + current().type(), file, line).raise();
        };

        if (match(EOF) || match(DEDENT)) {
            return n;
        }
        if (!previous().matches(DEDENT)) {
            if (!match(NEWLINE)) {
                return new RParserError(
                        "Expected newline after statement, got " + current().value(),
                        file, line()
                ).raise();
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
                if (!(n2 instanceof ValueNode v)) return new RParserError("Expected a value", file, line()).raise();
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
                        node = new BinaryExpressionNode(line(), NumberNode.ZERO, SubBO.INSTANCE, parseValue());
                    }
                    case "+" -> {
                        consume();
                        node = new BinaryExpressionNode(line(), NumberNode.ZERO, AddBO.INSTANCE, parseValue());
                    }
                    case "~" -> {
                        consume();
                        node = parseBitwiseNotOperator();
                    }
                    default -> {
                        return new RParserError("Unexpected operator: " + token.value(), file, line()).raise();
                    }
                }
            }
            case DIVIDER -> {
                if (matchAndConsume(DIVIDER, "[")) {
                    node = parseArrayDeclaration();
                } else if (matchAndConsume(DIVIDER, "(")) {
                    node = parseExpression(0);
                    if (!matchAndConsume(DIVIDER, ")")) {
                        return new RParserError("Expected ')'", file, line()).raise();
                    }
                } else {
                    return new RParserError("Unexpected divider: " + token.value(), file, line()).raise();
                }
            }
            case BOOLEAN -> {
                if (matchAndConsume(BOOLEAN, "true")) {
                    return new BooleanNode(line(), true);
                } else if (matchAndConsume(BOOLEAN, "false")) {
                    return new BooleanNode(line(), false);
                } else {
                    return new RParserError("Expected boolean value", file, line()).raise();
                }
            }
            case KEYWORD -> {
                ASTNode n2 = parseKeyword();
                if (n2 instanceof ValueNode value) {
                    node = value;
                } else {
                    return new RParserError("Expected value", file, line()).raise();
                }
            }
            case CHARACTER -> node = parseCharacter();
            default -> {
                return new RParserError("Unexpected token: " + current().value() + ", type: " + current().type(), file, line()).raise();
            }
        }

        while (true) {
            if (match(DIVIDER, "(")) {
                int line = line();
                var args = parseParamsCall();
                if (!(node instanceof DirectVariableReferenceNode vr)) {
                    return new RParserError("Expected reference for function call", file, line()).raise();
                }
                node = new FunctionCallNode(line, vr.getSimpleName(), args);
            } else if (match(DIVIDER, "[")) {
                consume();
                ValueNode index = parseValue();
                if (!matchAndConsume(DIVIDER, "]")) {
                    return new RParserError("Expected ']'", file, line()).raise();
                }

                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new ArraySetNode(line(), node, index, value);
                } else {
                    node = new ArrayAccessNode(line(), node, index);
                }
            } else if (match(OPERATOR, "@")) {
                consume();
                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new DereferenceAssignNode(line(), node, value);
                } else {
                    if (!(node instanceof VariableReference vr)) {
                        return new RParserError("Expected any type of variable reference for pointer type declaration", file, line()).raise();
                    }
                    node = new DereferenceNode(line(), vr);
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
            case "struct" -> parseStructKeyword();
            case "init" -> parseInitKeyword();
            case "if" -> parseIfKeyword();
            case "sizeof" -> parseSizeofKeyword();
            case "len" -> parseLenKeyword();
            case "cast" -> parseCastKeyword();
            case "_Typeof" -> parse_TypeofKeyword(false);
            case "_TypeofLLVM" -> parse_TypeofKeyword(true);
            case "impl" -> parseImplKeyword();
            case "self" -> parseIdentifier(true);
            case "global" -> parseGlobal();
            case "raise" -> parseRaise();
            case "try" -> parseTry();
            case "_NativeCPP" -> parse_NativeCPPKeyword();
            case "null" -> parseNullKeyword();
            default -> new RParserError("Unexpected keyword: " + kw, file, line()).raise();
        };
    }

    private ASTNode parseIdentifier(boolean self) {
        int line = line();
        String name = self ? "self" : consume().value();

        ValueNode node = new DirectVariableReferenceNode(line, name);

        node = parseSubExpr(line, self, name, node);

        return node;
    }

    private @SubFunc ValueNode parseVariableAssignment(int line, VariableReference variable) {
        return switch (current().value()) {
            case "=" -> {
                consume();

                ValueNode value = parseValue();
                yield new VariableDeclarationNode(line, variable, false, null, value);
            }
            case ":" -> {
                consume();

                boolean mutable = matchAndConsume(KEYWORD, "mut");

                TypeRef type;

                if (!match(OPERATOR, "=")) type = parseType();
                else type = null;

                if (!matchAndConsume(OPERATOR, "=")) {
                    yield new RParserError("Expected '=' for variable assignment", file, line()).raise();
                }

                ValueNode value = parseValue();
                yield new VariableDeclarationNode(line, variable, mutable, type, value);
            }

            default -> new RParserError("TODO var reference or other assignment ops", file, line()).raise();
        };
    }

    private ValueNode parseNumber() {
        return new NumberNode(line(), consume().value());
    }

    private @SubFunc ASTNode parse_BuiltinKeyword() {
        int line = line();

        boolean keepName = matchAndConsume(KEYWORD, "global");

        if (!matchAndConsume(KEYWORD, "func")) {
            return new RParserError("Expected 'func' for builtin function declaration", file, line).raise();
        }

        String name = identifier();

        var params = parseParamsDeclare();

        if (!matchAndConsume(OPERATOR, "->")) {
            return new RParserError("Expected \"->\" for builtin function type declaration", file, line).raise();
        }

        TypeRef returnType = parseType();

        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for builtin function declaration", file, line).raise();
        }

        if (!match(STRING)) {
            return new RParserError("Expected string literal for builtin function body", file, line).raise();
        }

        String body = consume().value();

        return new BuiltinFunctionDeclarationNode(line, keepName, name, params, returnType, body);
    }

    private @SubFunc ASTNode parse_IRKeyword() {
        int line = line();

        if (!match(STRING)) {
            return new RParserError("Expected string literal for IR declaration", file, line).raise();
        }

        return new IRDeclarationNode(line, consume().value());
    }

    private ValueNode parseString() {
        return new StringNode(line(), consume().value());
    }

    private @SubFunc ASTNode parseUsingKeyword() {
        int line = line();
        StringBuilder name = new StringBuilder(identifier());

        while (matchAndConsume(OPERATOR, ".")) {
            name.append("\\").append(identifier());
        }

        String pkg;

        if (matchAndConsume(OPERATOR, "in")) {
            if (matchAndConsume(KEYWORD, "self")) {
                pkg = "self";
            } else if (!match(STRING)) {
                return new RParserError("Expected string literal for using package", file, line).raise();
            } else {
                pkg = consume().value();
            }
        } else {
            pkg = null;
        }

        return new UsingNode(line, file, name.toString(), pkg);
    }

    private @SubFunc ASTNode parsePtrKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for pointer type declaration", file, line).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for pointer type declaration", file, line).raise();

        if (value instanceof ConstantNode cnst) return new PointerCreationNode(line, cnst);

        if (!(value instanceof VariableReference vr))
            return new RParserError("Expected any type of variable reference for pointer type declaration", file, line).raise();

        return new ReferenceNode(line, vr);
    }

    private @SubFunc ValueNode parseDereferenceOperator() {
        int line = line();
        VariableReference vr = new DirectVariableReferenceNode(line, matchAndConsume(KEYWORD, "self") ? "self" : identifier());

        if (match(OPERATOR, ".")) {
            return parseSubExpr(line, false, null, vr);
        }

        if (match(OPERATOR, ":")) {
            return new RParserError("You can't declare mutability or type with dereference operator", file, line).raise();
        }

        if (matchAndConsume(OPERATOR, "=")) {
            ValueNode v2 = parseValue();
            return new DereferenceAssignNode(line, vr, v2);
        }

        return new DereferenceNode(line, vr);
    }

    private @SubFunc ASTNode parseWhileKeyword() {
        int line = line();

        ValueNode condition = parseCondition();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after while loop condition", file, line()).raise();
        }

        var block = parseBlock();

        return new WhileNode(line, condition, block);
    }

    private @SubFunc ASTNode parseBreakKeyword() {
        return new BreakNode(previous().line());
    }

    private @SubFunc ASTNode parseContinueKeyword() {
        return new ContinueNode(previous().line());
    }

    private @SubFunc ASTNode parseFuncKeyword() {
        int line = line();

        String name = identifier();

        var params = parseParamsDeclare();

        TypeRef returnType = matchAndConsume(OPERATOR, "->") ? parseType() : NoneBuiltinType.INSTANCE;

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for function declaration", file, line).raise();
        }

        BlockNode block = parseBlock();

        return new FunctionDeclarationNode(line, name, params, returnType, block);
    }

    private @SubFunc ASTNode parseForKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) return new RParserError("Expected '(' for for loop", file, line).raise();

        String name = identifier();

        if (!matchAndConsume(OPERATOR, "in")) {
            return new RParserError("Expected 'in' for for loop", file, line).raise();
        }

        var collection = parseValue();

        if (!matchAndConsume(DIVIDER, ")")) return new RParserError("Expected ')' for for loop", file, line).raise();

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after for loop collection", file, line()).raise();
        }

        var block = parseBlock();

        return new ForLoopNode(line, name, collection, block);
    }

    private @SubFunc ASTNode parseRangeKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for range declaration", file, line()).raise();
        }

        ValueNode first = parseValue();

        if (matchAndConsume(DIVIDER, ")")) {
            return new RangeNode(line, new NumberNode(line, "0"), first, new NumberNode(line, "1"));
        }

        if (!matchAndConsume(DIVIDER, ",")) {
            return new RParserError("Expected ',' or ')' in range declaration", file, line()).raise();
        }

        ValueNode second = parseValue();

        if (matchAndConsume(DIVIDER, ")")) {
            return new RangeNode(line, first, second, new NumberNode(line, "1"));
        }

        if (!matchAndConsume(DIVIDER, ",")) {
            return new RParserError("Expected ',' before step in range", file, line()).raise();
        }

        ValueNode step = parseValue();

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' at end of range", file, line()).raise();
        }

        return new RangeNode(line, first, second, step);
    }

    private @SubFunc ASTNode parseReturnKeyword() {
        int line = line();
        ValueNode value = matchAndConsume(KEYWORD, "none") ? null : parseValue();
        return new ReturnNode(line, value);
    }

    private @SubFunc ASTNode parseStructKeyword() {
        int line = line();
        String name = identifier();

        //StructType inherited;

        //if (matchAndConsume(KEYWORD, "inherits")) {
        //    return new RParserError("Inheritance is still unsupported", file, line).raise();
        //    /*
        //    This works, but struct constructor implementation is needed before.

        //    String inheritedName = identifier();

        //    TypeRef tmp = typeMap.get(inheritedName);
        //    if (!(tmp instanceof StructType st)) {
        //        return new RParserError("Expected struct type for inheriting", file, line).raise();
        //    }
        //    inherited = st;
        //    */
        //} else {
        //    inherited = null;
        //}

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' for struct declaration", file, line()).raise();
        }
        if (!match(NEWLINE)) {
            return new RParserError("Expected newline after struct declaration", file, line()).raise();
        }
        consume();

        if (!match(INDENT)) {
            return new RParserError("Expected indent for struct field declaration", file, line()).raise();
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
                return new RParserError("Expected ':' for struct field declaration", file, line()).raise();
            }

            if (match(KEYWORD, "mut")) {
                return new RParserError("You can't declare fields as mutable", file, line()).raise();
            }

            TypeRef fieldType = parseType();

            ValueNode defaultValue = matchAndConsume(OPERATOR, "=") ? parseValue() : null;


            fields.add(new RStructField(fieldName, fieldType, defaultValue));
            types.add(fieldType);
        }

        var type = new StructType(name, types, null);

        typeMap.put(name, type);
        return new StructDeclarationNode(line, name, type, fields);
    }

    private @SubFunc ASTNode parseInitKeyword() {
        int line = line();
        String name = identifier();

        if (!match(DIVIDER, "(")) {
            return new RParserError("Expected '(' for struct initialization", file, line()).raise();
        }

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters call", file, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return new StructInitNode(line, name, List.of());
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
            return new RParserError("Expected ')' for parameters call", file, line()).raise();
        }

        return new StructInitNode(line, name, args);
    }

    private @SubFunc IfStatementNode parseIfKeyword() {
        int line = line();
        ValueNode condition = parseCondition();
        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after if condition", file, line()).raise();
        }

        BlockNode block = parseBlock();

        if (!matchAndConsume(KEYWORD, "else")) {
            return new IfStatementNode(line, condition, block, null, null);
        }

        if (matchAndConsume(KEYWORD, "if")) {
            IfStatementNode elif = parseIfKeyword();
            return new IfStatementNode(line, condition, block, elif, null);
        }

        if (!matchAndConsume(OPERATOR, ":")) {
            return new RParserError("Expected ':' after else block", file, line()).raise();
        }

        BlockNode elseBlock = parseBlock();

        return new IfStatementNode(line, condition, block, null, elseBlock);
    }

    private @SubFunc ASTNode parseSizeofKeyword() {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for sizeof expression", file, line()).raise();

        int preType = tokenIndex;

        TypeRef type;

        LABEL_TYPE:
        {
            String typeName = consume().value();

            switch (typeName) {
                case "ptr" -> {
                    type = parsePointerType(line);
                    break LABEL_TYPE;
                }
                case "arr" -> {
                    type = parseArrayType(line);
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
            if (!matchAndConsume(DIVIDER, ")"))
                return new RParserError("Expected ')' for sizeof expression", file, line()).raise();

            return new SizeofNode(line, type);
        }
        tokenIndex = preType;

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for sizeof expression", file, line()).raise();
        return new SizeofNode(line, value);
    }

    private @SubFunc ASTNode parseLenKeyword() {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for len expression", file, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for len expression", file, line()).raise();
        return new LenNode(line, value);
    }

    private ValueNode parseCharacter() {
        String value = consume().value();
        if (value.length() != 1) {
            return new RParserError("Expected single character literal", file, line()).raise();
        }
        return new CharacterNode(line(), value.charAt(0));
    }

    private @SubFunc ASTNode parseCastKeyword() {
        int line = line();

        if (!matchAndConsume(OPERATOR, "<"))
            return new RParserError("Expected '<' for cast expression", file, line()).raise();

        TypeRef type = parseType();

        if (!matchAndConsume(OPERATOR, ">"))
            return new RParserError("Expected '>' for cast expression", file, line()).raise();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for cast expression", file, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for cast expression", file, line()).raise();

        return new CastNode(line, type, value);
    }

    private @SubFunc ASTNode parse_TypeofKeyword(boolean llvm) {
        int line = line();
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for typeof expression", file, line()).raise();

        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for typeof expression", file, line()).raise();
        return new TypeofNode(line, value, llvm);
    }

    private @SubFunc ASTNode parseImplKeyword() {
        int line = line();

        String name = identifier();

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for impl declaration", file, line()).raise();

        BlockNode block = parseBlock();

        for (final ASTNode node : block.getNodes()) {
            if (node instanceof FunctionDeclarationNode || node instanceof BuiltinFunctionDeclarationNode) continue;
            return new RImplNotFunctionError(line()).raise();
        }

        return new StructImplNode(line, name, block.getNodes());
    }

    private @SubFunc ASTNode parseGlobal() {
        int line = line();
        String name = identifier();

        TypeRef type = null;

        if (matchAndConsume(OPERATOR, ":")) {
            if (matchAndConsume(KEYWORD, "mut"))
                return new RParserError("Global variables cannot be mutable", file, line).raise();
            type = parseType();
        }

        if (!matchAndConsume(OPERATOR, "=")) {
            return new RParserError("Expected '=' for global variable declaration", file, line).raise();
        }

        var value = parseValue();

        if (!(value instanceof ConstantNode node)) {
            return new RParserError("Global variables can only have a constant value", file, line).raise();
        }

        return new GlobalVariableDeclarationNode(line, name, type, node);
    }

    private @SubFunc ASTNode parseRaise() {
        int line = line();

        String value;

        if (matchAndConsume(KEYWORD, "none")) {
            value = null;
        } else {
            if (!match(STRING))
                return new RParserError("Expected string literal or none after raise", file, line).raise();
            value = consume().value();
        }

        return new RaiseNode(line, value);
    }

    private @SubFunc ASTNode parseTry() {
        int line = line();

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for try declaration", file, line).raise();

        BlockNode tryBlock = parseBlock();

        if (!matchAndConsume(KEYWORD, "catch"))
            return new RParserError("Expected catch block after try declaration", file, line).raise();

        if (!matchAndConsume(OPERATOR, ":"))
            return new RParserError("Expected ':' for catch declaration", file, line).raise();

        BlockNode catchBlock = parseBlock();

        return new TryCatchNode(line, tryBlock, catchBlock);
    }

    private @SubFunc ASTNode parse_NativeCPPKeyword() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for Native CPP declaration", file, line).raise();

        if (!match(STRING))
            return new RParserError("Expected a string for file name in Native CPP declaration", file, line).raise();

        String name = consume().value();

        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for Native CPP declaration", file, line).raise();

        if (matchAndConsume(KEYWORD, "_Builtin")) {
            return new NativeCPPNode(line, name, List.of());
        }

        List<RFunction> functions = new ArrayList<>();
        do {
            TypeRef type = parseType();
            String funcName = identifier();
            var params = parseParamsDeclare();

            functions.add(new RFunction(funcName, funcName, type, params));
        } while (matchAndConsume(OPERATOR, "and"));

        return new NativeCPPNode(line, name, functions);
    }

    private ASTNode parseNullKeyword() {
        return new NullNode(previous().line());
    }

    private ValueNode parseBitwiseNotOperator() {
        int line = line();
        ValueNode value = parseValue();

        return new BitwiseNotNode(line, value);
    }

    private ASTNode parseDivider() {
        int line = line();

        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Unexpected divider: " + current().value(), file, line).raise();
        }
        ValueNode value = parseValue();

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected closing divider ')'", file, line).raise();
        }

        return parseSubExpr(line, false, null, value);
    }

    private ValueNode parseSubExpr(int line, boolean self, String name, ValueNode node) {
        while (true) {
            if (outOfBounds(0)) break;
            line = line();

            if (match(DIVIDER, "(")) {
                if (self) {
                    return new RParserError("Using self as function call", file, line).raise();
                }
                if (name == null) {
                    return new RParserError("You can't call a function here", file, line).raise();
                }
                var args = parseParamsCall();

                if (node instanceof DirectVariableReferenceNode) {
                    node = new FunctionCallNode(line, name, args);
                    continue;
                }

                return new RParserError("Invalid parenthesis", file, line).raise();
            }

            if (matchAndConsume(DIVIDER, "[")) {
                if (self) {
                    return new RParserError("Using self as array access", file, line).raise();
                }

                ValueNode index = parseValue();

                if (!matchAndConsume(DIVIDER, "]")) {
                    return new RParserError("Expected ']'", file, line).raise();
                }

                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new ArraySetNode(line, node, index, value);
                } else {
                    node = new ArrayAccessNode(line, node, index);
                }
                continue;
            }

            if (matchAndConsume(OPERATOR, "@")) {
                if (matchAndConsume(OPERATOR, "=")) {
                    ValueNode value = parseValue();
                    node = new DereferenceAssignNode(line, node, value);
                    continue;
                }

                if (!(node instanceof VariableReference vr)) {
                    return new RParserError("Expected variable reference for dereference operator", file, line).raise();
                }

                node = new DereferenceNode(line, vr);
                continue;
            }

            if (matchAndConsume(OPERATOR, ".")) {
                String fieldName = identifier();
                if (!(node instanceof VariableReference vr)) {
                    return new RParserError("Expected reference for struct access", file, line).raise();
                }

                if (!match(DIVIDER, "(")) {
                    node = new StructFieldAccessNode(line, vr, fieldName);
                    continue;
                }

                var args = parseParamsCall();

                node = new StructFunctionCallNode(line, node, fieldName, args);
            }

            break;
        }

        if (matchAndConsume(KEYWORD, "is")) {
            TypeRef type = parseType();
            return new IsNode(line, node, type);
        }

        if (match(OPERATOR, "=") || match(OPERATOR, ":")) {
            if (!(node instanceof VariableReference vr)) {
                return new RParserError("Expected reference for assignment", file, line).raise();
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
                return new RParserError("Expected variable reference for assignment operation", file, line).raise();
            }

            consume();

            ValueNode value = new BinaryExpressionNode(line, node, opAssign, parseValue());

            return new VariableDeclarationNode(line, leftRef, false, null, value);
        }

        return node;
    }

    private @SubFunc ValueNode parseTernaryOperator(ValueNode thenExpr) {
        int line = line();

        ValueNode condition = parseValue();

        if (!matchAndConsume(KEYWORD, "else")) {
            return new RParserError("Expected 'else' for ternary operator", file, line).raise();
        }

        ValueNode elseExpr = parseValue();

        return new TernaryOperatorNode(line, condition, thenExpr, elseExpr);
    }

    /*
    general utils
     */

    private @SubFunc TypeRef parseType() {
        if (!match(IDENTIFIER) && !match(KEYWORD)) {
            return new RParserError("Expected type name", file, line()).raise();
        }

        int line = line();

        String typeName = consume().value();

        switch (typeName) {
            case "ptr" -> {
                return parsePointerType(line);
            }
            case "arr" -> {
                return parseArrayType(line);
            }
        }

        if (typeMap.containsKey(typeName)) {
            return typeMap.get(typeName);
        }

        TypeRef type = BuiltinTypes.getByName(typeName);

        if (type == null) {
            return new RParserError("Unknown type: " + typeName, file, line).raise();
        }

        typeMap.put(typeName, type);

        return type;
    }

    private @SubFunc TypeRef parsePointerType(int line) {
        if (!matchAndConsume(OPERATOR, "->"))
            return new RParserError("Expected \"->\" for pointer type declaration", file, line).raise();

        TypeRef inner = parseType();
        if (inner instanceof NoneBuiltinType)
            return new RParserError("You can't declare a void pointer, please use 'anyptr' instead", file, line).raise();
        return new PointerType(inner);
    }

    private @SubFunc TypeRef parseArrayType(int line) {
        if (!matchAndConsume(OPERATOR, "->")) {
            return new RParserError("Expected \"->\" for array type declaration", file, line).raise();
        }

        TypeRef inner = parseType();

        if (inner instanceof NoneBuiltinType) {
            return new RArrayTypeIsNoneError(line).raise();
        }

        return new ArrayType(ArrayType.UNKNOWN_SIZE, inner);
    }

    private @SubFunc List<FunctionParameter> parseParamsDeclare() {
        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters declaration", file, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return List.of();
        }

        List<FunctionParameter> params = new java.util.ArrayList<>();

        do {
            String name = identifier();
            if (!matchAndConsume(OPERATOR, ":")) {
                return new RParserError("Expected ':' for parameters type declaration", file, line()).raise();
            }

            boolean mutable = matchAndConsume(KEYWORD, "mut");

            TypeRef type = parseType();

            var param = new FunctionParameter(name, mutable, type);

            params.add(param);
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters declaration", file, line()).raise();
        }

        return params;
    }

    private @SubFunc List<ValueNode> parseParamsCall() {
        if (!matchAndConsume(DIVIDER, "(")) {
            return new RParserError("Expected '(' for parameters call", file, line()).raise();
        }

        if (matchAndConsume(DIVIDER, ")")) {
            return List.of();
        }

        List<ValueNode> args = new ArrayList<>();

        do {
            args.add(parseValue());
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, ")")) {
            return new RParserError("Expected ')' for parameters call", file, line()).raise();
        }

        return args;
    }

    private @SubFunc ValueNode parseCondition() {
        if (!matchAndConsume(DIVIDER, "("))
            return new RParserError("Expected '(' for the condition", file, line()).raise();
        ValueNode condition = parseValue();
        if (!matchAndConsume(DIVIDER, ")"))
            return new RParserError("Expected ')' for the condition", file, line()).raise();

        return condition;
    }

    private ValueNode parseArrayDeclaration() {
        int line = line();
        List<ValueNode> values = new ArrayList<>();

        if (matchAndConsume(DIVIDER, "]")) return new ArrayNode(line, List.of());

        do {
            values.add(parseValue());
        } while (matchAndConsume(DIVIDER, ","));

        if (!matchAndConsume(DIVIDER, "]"))
            return new RParserError("Expected ']' for array declaration", file, line()).raise();

        return new ArrayNode(line, values);
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
            return new RParserError("Expected identifier", file, line()).raise();
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

        new RParserError("Unexpected end of file", file, previous().line()).raise();
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
}
