package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;
import logic.stack.line.LineExpression;
import logic.stack.line.LineVar;

public class BlockFor extends Block {

    Token paramToken;
    TokenGroup contentTokenGroup;

    Line firstLine;
    Expression condition, loop;
    LineVar foreachVar;

    public BlockFor(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("FOR");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.FOR) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                paramToken = token;
                if (paramToken.getChild() != null) {
                    Token colon = readIsForeach(paramToken.getChild(), paramToken.getLastChild());
                    if (colon != null) {
                        System.out.println("EACH");
                        readForeach(paramToken.getChild(), paramToken.getLastChild(), colon);
                    } else {
                        readFor(paramToken.getChild(), paramToken.getLastChild());
                    }
                }
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing loop entry");
                }
                contentTokenGroup = new TokenGroup(token, next);
                state = 4; // completed
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 4; // empty for
            }  else if (state == 2) {

                state = 3; // waiting [;]
            } else if (state == 3 && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }

                contentTokenGroup = new TokenGroup(paramToken.getNext(), next);
                state = 4; // completed
            } else if (state != 3) {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }

        if (contentTokenGroup != null) {
            if (contentTokenGroup.start.key == Key.BRACE) {
                Parser.parseLines(this, contentTokenGroup.start.getChild(), contentTokenGroup.start.getLastChild());
            }
        }
    }

    private Token readIsForeach(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.FINAL) {
                state = 1;
            } else if ((state == 0 || state == 1) && token.key == Key.LET) {
                state = 2;
            } else if ((state == 0 || state == 1) && token.key == Key.VAR) {
                state = 3;
            } else if ((state == 0 || state == 1 || state == 2) && token.key == Key.WORD) {
                next = TokenGroup.nextType(next, end);
                state = 3;
            } else if (state == 3 && token.key == Key.WORD) {
                state = 4;
            } else if (state == 4 && token.key == Key.COLON) {
                return token;
            } else {
                return null;
            }
            token = next;
        }
        return null;
    }

    private void readFor(Token start, Token end) {
        Token contentStart = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.SEMICOLON) {
                if (contentStart != next) firstLine = readFirstForLine(contentStart, next);
                contentStart = next;
                state = 1;
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                if (contentStart != token) condition = new Expression(this, contentStart, token);
                contentStart = next;
                state = 2;
            } else if (state == 2 && next == end) {
                loop = new Expression(this, contentStart, next);
                state = 3;
            }
            if (next == end) {
                if (state == 0) {
                    if (contentStart != token) {
                        firstLine = readFirstForLine(contentStart, token);
                        // -> LineVar/LineExpression already call unexpected
                    } else {
                        cFile.erro(token, "Unexpected end of tokens");
                    }
                } else if (state == 1) {
                    if (contentStart != token) condition = new Expression(this, contentStart, token);
                    cFile.erro(token, "Unexpected end of tokens");
                } else if (state == 2) {
                    // empty loop
                }
            }
            token = next;
        }
    }

    private Line readFirstForLine(Token start, Token end) {
        if (start.key == Key.LET || start.key == Key.VAR || start.key == Key.FINAL) {
            return new LineVar(this, start, end);
        } else {
            // Var/Expression
            Token token = start;
            int state = 0;
            while (token != end) {
                if (state == 0 && token.key == Key.WORD) {
                    state = 1;
                } else if (state == 1 && (token.key == Key.WORD || token.key == Key.GENERIC)) {
                    return new LineVar(this, start, end);
                } else if (state == 1 && token.key == Key.INDEX) {
                    state = 2;
                } else if (state == 2 && token.key == Key.WORD) {
                    return new LineVar(this, start, end);
                } else {
                    return new LineExpression(this, start, end);
                }
                token = token.getNext();
            }
            return new LineExpression(this, start, end);
        }
    }

    private void readForeach(Token start, Token end, Token colon) {
        foreachVar = new LineVar(this, start, colon, false);
        if (colon.getNext() != end) {
            loop = new Expression(this, colon.getNext(), end);
        } else {
            cFile.erro(colon, "Unexpected end of tokens");
        }
    }
}
