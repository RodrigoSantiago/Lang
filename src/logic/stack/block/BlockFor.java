package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
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

    // TODO - Priorizar braces !!!

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
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                paramToken = token;
                Token colon = readIsForeach(paramToken.getChild(), paramToken.getLastChild());
                if (colon != null) {
                    System.out.println("EACH");
                    readForeach(paramToken.getChild(), paramToken.getLastChild(), colon);
                } else {
                    readFor(paramToken.getChild(), paramToken.getLastChild());
                }
                state = 2;
            } else if (state == 2 && token.key == Key.COLON) {
                state = 3;
            } else if (state == 3 && token.key == Key.WORD) {
                System.out.println("LABEL :"+ token);
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                label = token;
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing loop entries", this);
                } else if (state == 3) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
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
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing loop entries", this);
                } else if (state == 3) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
                state = 5;
            } else if (state == 2) {
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
        if (foreachVar != null) {
            foreachVar.load();
        }
        if (conditionExp != null) {
            conditionExp.load(new Context(stack));
            conditionExp.requestGet(cFile.langBoolPtr());
        }
        if (loopExp != null) {
            loopExp.load(new Context(stack));
            if (foreachVar != null) {
                // verify string
                // verify NULL == iterable
                // cFile.erro(loopToken, "The foreach be a string or must implements Iterable", this);
                loopExp.requestGet(null);
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
        if (label == this.label || (this.label != null && this.label.equals(label))) {
            return this;
        } else {
            return super.isBreakble(label);
        }
    }

    @Override
    public Line isContinuable(Token label) {
        if (label == this.label || (this.label != null && this.label.equals(label))) {
            return this;
        } else {
            return super.isContinuable(label);
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
                if (contentStart != token) {
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
                        firstLine = readFirstForLine(contentStart, token);
                        // -> LineVar/LineExpression already call unexpected
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
        foreachVar = new LineVar(this, start, colon, false);
        if (colon.getNext() != end) {
            loopToken = new TokenGroup(colon.getNext(), end);
            loopExp = new Expression(this, colon.getNext(), end);
        } else {
            cFile.erro(colon, "Unexpected end of tokens", this);
        }
    }
}
