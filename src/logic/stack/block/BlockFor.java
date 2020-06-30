package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;
import logic.stack.line.LineExpression;
import logic.stack.line.LineVar;

public class BlockFor extends Block {

    Token label;
    Token paramToken;
    TokenGroup contentToken;

    Line firstLine;
    Expression conditionExp, loopExp;
    TokenGroup conditionToken, loopToken;
    LineVar foreachVar;

    public BlockFor(Block block, Token start, Token end) {
        super(block, start, end);

        Token colon = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.FOR) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                paramToken = token;
                Token foreach = readIsForeach(paramToken.getChild(), paramToken.getLastChild());
                if (foreach != null) {
                    readForeach(paramToken.getChild(), paramToken.getLastChild(), foreach);
                } else {
                    readFor(paramToken.getChild(), paramToken.getLastChild());
                }
                state = 2;
            } else if (state == 2 && token.key == Key.COLON) {
                colon = token;
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.WORD && next != end && next.key == Key.BRACE) {
                if (state == 2) cFile.erro(token, "Missing colon [:]", this);
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);

                label = token;
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.BRACE) {
                if (state == 1) cFile.erro(token.start, token.start + 1, "Missing condition", this);
                if (state == 3) cFile.erro(colon, "Label Statment expected", this);

                if (token.getChild() == null) {
                    if (next != end) {
                        contentToken = new TokenGroup(next, end);
                        next = end;
                    }
                    cFile.erro(token, "Brace closure expected", this);
                } else {
                    if (token.isOpen()) cFile.erro(token, "Brace closure expected", this);
                    contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                }
                state = 5;
            } else if ((state == 1 || state == 2 || state == 3) && token.key == Key.SEMICOLON) {
                if (state == 1) cFile.erro(token, "Missing condition", this);
                if (state == 3) cFile.erro(colon, "Unexpected Token", this);

                state = 5;
            } else if ((state == 2 || state == 3)) {
                if (state == 3) cFile.erro(colon, "Unexpected Token", this);

                contentToken = new TokenGroup(token, end);
                next = end;
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (contentToken != null) {
            Parser.parseLines(this, contentToken.start, contentToken.end);
        }
    }

    @Override
    public void load() {
        if (firstLine != null) {
            firstLine.load();
        }
        if (conditionExp != null) {
            conditionExp.load(new Context(stack));
            conditionExp.requestGet(cFile.langBoolPtr());
        }
        if (loopExp != null) {
            loopExp.load(new Context(stack));
            if (foreachVar != null) {

                Pointer rType = null;
                if (foreachVar.typeToken != null) {
                    rType = stack.getPointer(foreachVar.typeToken, false);
                }
                Pointer rIterable = null;
                if (rType != null) {
                    rIterable = cFile.langIterablePtr(rType);
                }

                Pointer naturalPtr = loopExp.getNaturalPtr(rIterable);

                if (naturalPtr != null && rIterable != null && naturalPtr.equalsIgnoreLet(rIterable)) {
                    loopExp.requestGet(rIterable);
                    foreachVar.typePtr = rType;
                } else if (naturalPtr != null && naturalPtr.equals(cFile.langStringPtr())) {
                    if (rType == null || rType.equals(cFile.langBytePtr())) {
                        rType = cFile.langBytePtr();
                    } else if (rType.equals(cFile.langShortPtr())) {
                        rType = cFile.langShortPtr();
                    } else if (rType.equals(cFile.langIntPtr())) {
                        rType = cFile.langIntPtr();
                    } else {
                        rType = cFile.langBytePtr();
                        cFile.erro(foreachVar.typeToken, "The string can iterate only by int, short or byte", this);
                    }
                    loopExp.requestGet(cFile.langStringPtr());
                    foreachVar.typePtr = rType;
                } else {
                    loopExp.requestGet(rIterable);
                    if (rType != null) {
                        foreachVar.typePtr = rType;
                    } else {
                        foreachVar.typePtr = cFile.langObjectPtr();
                        cFile.erro(loopExp.getTokenGroup(), "Cannot determine the Iterator Type", this);
                    }
                }

                if (foreachVar.nameTokens.size() > 0) {
                    if (!stack.addField(foreachVar.nameTokens.get(0), foreachVar.typePtr, foreachVar.isFinal, parent)) {
                        cFile.erro(foreachVar.nameTokens.get(0), "Repeated field name", this);
                    }
                }
            } else {
                conditionExp.requestGet(null);
            }
        }
        super.load();
    }

    @Override
    public boolean isLoopStatment() {
        return true;
    }

    @Override
    public Line isBreakble(Token label) {
        if (label == null || label.equals(this.label)) {
            return this;
        } else {
            return super.isBreakble(label);
        }
    }

    @Override
    public Line isContinuable(Token label) {
        if (label == null || label.equals(this.label)) {
            return this;
        } else {
            return super.isBreakble(label);
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
                if (contentStart != next && contentStart.key != Key.SEMICOLON) {
                    firstLine = readFirstForLine(contentStart, next);
                }

                contentStart = next;
                state = 1;
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                if (contentStart != token && contentStart.key != Key.SEMICOLON) {
                    conditionToken = new TokenGroup(contentStart, token);
                    conditionExp = new Expression(this, contentStart, token);
                }
                contentStart = next;
                state = 2;
            } else if (state == 2 && next == end) {
                loopToken = new TokenGroup(contentStart, next);
                loopExp = new Expression(this, contentStart, next);
                state = 3;
            }
            if (next == end) {
                if (state == 0) {
                    if (contentStart != token) {
                        firstLine = readFirstForLine(contentStart, token); // [INTERNAL MISSING SEMICOLON]
                    } else {
                        cFile.erro(token, "Unexpected end of tokens", this);
                    }
                } else if (state == 1) {
                    if (contentStart != token) {
                        conditionToken = new TokenGroup(contentStart, token);
                        conditionExp = new Expression(this, contentStart, token);
                    }
                    cFile.erro(token, "Unexpected end of tokens", this);
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
        foreachVar = new LineVar(this, start, colon, true);
        if (colon.getNext() != end) {
            loopToken = new TokenGroup(colon.getNext(), end);
            loopExp = new Expression(this, colon.getNext(), end);
        } else {
            cFile.erro(colon, "Unexpected end of tokens", this);
        }
    }
}
