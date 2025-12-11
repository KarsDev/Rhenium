package me.kuwg.re.token;

import me.kuwg.re.constants.Constants;

import java.util.*;
import java.util.regex.Matcher;

import static me.kuwg.re.constants.Constants.Tokens.*;

public class Tokenizer {
    private static final Set<String> WORD_OPERATORS = new HashSet<>();
    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> BOOLEANS = new HashSet<>();
    private static final Set<String> DIVIDERS = new HashSet<>();

    static {
        for (String op : Constants.Tokens.OPERATORS) {
            if (op.matches("^[A-Za-z_].*")) {
                WORD_OPERATORS.add(op);
            }
        }
        Collections.addAll(KEYWORDS, Constants.Tokens.KEYWORDS);
        Collections.addAll(BOOLEANS, Constants.Tokens.BOOLEANS);
        Collections.addAll(DIVIDERS, Constants.Tokens.DIVIDERS);

        Arrays.sort(Constants.Tokens.OPERATORS, (a, b) -> Integer.compare(b.length(), a.length()));
    }

    public static Token[] tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int line = 1;
        final int n = source.length();

        List<Integer> indentStack = new ArrayList<>();
        indentStack.add(0);

        while (i < n) {
            int lineStart = i;
            int currentIndent = 0;

            while (i < n && source.charAt(i) == ' ') {
                currentIndent++;
                i++;
            }

            int scan = i;
            boolean isBlank = true;

            while (scan < n && source.charAt(scan) != '\n') {
                if (!Character.isWhitespace(source.charAt(scan))) {
                    isBlank = false;
                    break;
                }
                scan++;
            }

            if (isBlank) {
                if (scan < n && source.charAt(scan) == '\n') {
                    scan++;
                    line++;
                }
                i = scan;
                continue;
            }

            if (lineStart == 0 || source.charAt(lineStart - 1) == '\n') {
                int lastIndent = indentStack.get(indentStack.size() - 1);

                if (currentIndent > lastIndent) {
                    indentStack.add(currentIndent);
                    tokens.add(new Token(TokenType.INDENT, "", line));
                } else if (currentIndent < lastIndent) {
                    while (!indentStack.isEmpty() && currentIndent < indentStack.get(indentStack.size() - 1)) {
                        indentStack.remove(indentStack.size() - 1);
                        tokens.add(new Token(TokenType.DEDENT, "", line));
                    }
                    if (currentIndent != indentStack.get(indentStack.size() - 1)) {
                        except("Invalid indentation at line " + line);
                    }
                }
            }

            char c = source.charAt(i);

            if (Character.isWhitespace(c)) {
                if (c == '\n') line++;
                i++;
                continue;
            }

            if (c == '/') {
                if (i + 1 < n && source.charAt(i + 1) == '/') {
                    i += 2;
                    while (i < n && source.charAt(i) != '\n') i++;
                    continue;
                } else if (i + 1 < n && source.charAt(i + 1) == '*') {
                    i += 2;
                    while (i < n) {
                        if (source.charAt(i) == '\n') line++;
                        if (i + 1 < n && source.charAt(i) == '*' && source.charAt(i + 1) == '/') {
                            i += 2;
                            break;
                        } else {
                            i++;
                        }
                    }
                    continue;
                }
            }

            if (c == '"') {
                int startLine = line;
                StringBuilder sb = new StringBuilder();
                boolean isTriple = (i + 2 < n && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"');
                if (isTriple) i += 3;
                else i++;
                boolean terminated = false;

                while (i < n) {
                    char ch = source.charAt(i);
                    if (ch == '\n') {
                        line++;
                        if (!isTriple) except("Unterminated string literal at line " + startLine);
                        sb.append(ch);
                        i++;
                        continue;
                    }

                    if (!isTriple && ch == '\\' && i + 1 < n) {
                        i++;
                        sb.append(unescapeChar(source.charAt(i)));
                        i++;
                        continue;
                    }

                    if (isTriple) {
                        if (i + 2 < n && source.charAt(i) == '"' && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
                            i += 3;
                            terminated = true;
                            break;
                        } else {
                            sb.append(ch);
                            i++;
                        }
                    } else if (ch == '"') {
                        i++;
                        terminated = true;
                        break;
                    } else {
                        sb.append(ch);
                        i++;
                    }
                }

                if (!terminated)
                    except("Unterminated " + (isTriple ? "multi-line" : "string") + " literal at line " + startLine);

                String content = sb.toString();
                if (isTriple) {
                    if (content.startsWith("\n")) content = content.substring(1);
                    if (content.endsWith("\n")) content = content.substring(0, content.length() - 1);
                }

                tokens.add(new Token(TokenType.STRING, content, startLine));
                continue;
            }

            if (c == '\'') {
                int startLine = line;
                StringBuilder sb = new StringBuilder();
                i++;
                boolean terminated = false;

                while (i < n) {
                    char ch = source.charAt(i);
                    if (ch == '\n') line++;
                    if (ch == '\\') {
                        if (i + 1 < n) {
                            i++;
                            sb.append(unescapeChar(source.charAt(i)));
                            i++;
                        } else {
                            except("Unterminated escape in character literal at line " + startLine);
                        }
                    } else if (ch == '\'') {
                        i++;
                        terminated = true;
                        break;
                    } else {
                        sb.append(ch);
                        i++;
                    }
                }

                if (!terminated) except("Unterminated character literal at line " + startLine);

                String content = sb.toString();
                if (!(content.length() == 1)) except("Invalid character literal at line " + startLine);
                tokens.add(new Token(TokenType.CHARACTER, content, startLine));
                continue;
            }

            String s1 = String.valueOf(c);
            if (DIVIDERS.contains(s1)) {
                tokens.add(new Token(TokenType.DIVIDER, s1, line));
                i++;
                continue;
            }

            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(source.charAt(i + 1)))) {
                String remaining = source.substring(i);
                Matcher m = NUMBER_PATTERN.matcher(remaining);
                if (m.lookingAt()) {
                    String lex = m.group();
                    tokens.add(new Token(TokenType.NUMBER, lex, line));
                    for (char ch : lex.toCharArray()) if (ch == '\n') line++;
                    i += lex.length();
                    continue;
                }
            }

            if (Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(source.charAt(j))) j++;
                String word = source.substring(i, j);

                if (KEYWORDS.contains(word)) tokens.add(new Token(TokenType.KEYWORD, word, line));
                else if (BOOLEANS.contains(word)) tokens.add(new Token(TokenType.BOOLEAN, word, line));
                else if (WORD_OPERATORS.contains(word)) tokens.add(new Token(TokenType.OPERATOR, word, line));
                else {
                    Matcher mid = IDENTIFIER_PATTERN.matcher(word);
                    if (mid.matches()) tokens.add(new Token(TokenType.IDENTIFIER, word, line));
                    else except("Invalid identifier at line " + line);
                }
                i = j;
                continue;
            }

            boolean matchedOp = false;
            for (String symOp : OPERATORS) {
                int len = symOp.length();
                if (i + len <= n && source.substring(i, i + len).equals(symOp)) {
                    tokens.add(new Token(TokenType.OPERATOR, symOp, line));
                    i += len;
                    matchedOp = true;
                    break;
                }
            }
            if (matchedOp) continue;

            except("Unknown operator at line " + line);
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens.toArray(new Token[0]);
    }

    private static char unescapeChar(char c) {
        return switch (c) {
            case 'b' -> '\b';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 'r' -> '\r';
            case '"' -> '"';
            case '\'' -> '\'';
            case '\\' -> '\\';
            default -> {
                except("Invalid escape sequence: \\" + c);
                yield c;
            }
        };
    }

    private static void except(String s) {
        System.err.println(s);
        System.exit(-1);
    }
}