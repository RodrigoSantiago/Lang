package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class BlockWhile extends Block {

    Token label;
    Token conditionToken;
    Expression conditionExp;
    TokenGroup contentToken;

    public BlockWhile(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("WHILE");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WHILE) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                conditionToken = token;
                conditionExp = new Expression(this, conditionToken.getChild(), conditionToken.getLastChild());
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
                    cFile.erro(token.start, token.start + 1, "Missing condition", this);
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
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.SEMICOLON) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing condition", this);
                } else if (state == 3) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
                state = 5;
            } else if (state == 2 || state == 4) {
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
        if (conditionExp != null) {
            conditionExp.load(new Context(stack));
            if (conditionExp.request(cFile.langBoolPtr()) == null) {
                cFile.erro(conditionToken, "The condition must be a bool", this);
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
}
