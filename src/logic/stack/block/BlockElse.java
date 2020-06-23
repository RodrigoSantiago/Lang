package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.expression.Expression;

public class BlockElse extends Block {

    TokenGroup contentToken;
    Token conditionToken;
    Expression conditionExp;
    private boolean isElseIf;

    public BlockElse(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("ELSE");

        Token key = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.ELSE) {
                key = token;
                state = 1;
            } else if (state == 1 && token.key == Key.IF) {
                System.out.println("AND IF");
                isElseIf = true;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM && token.getChild() != null) {
                conditionToken = token;
                conditionExp = new Expression(this, token.getChild(), token.getLastChild());
                state = 3;
            } else if ((state == 1 || state == 2 || state == 3) && token.key == Key.BRACE) {
                if (state == 2) {
                    cFile.erro(token.start, token.start + 1, "Missing condition", this);
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
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3) && token.key == Key.SEMICOLON) {
                if (state == 2) {
                    cFile.erro(token.start, token.start + 1, "Missing condition", this);
                }
                state = 4;
            } else if (state == 2 || state == 3) {
                contentToken = new TokenGroup(token, end);
                next = end;
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
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
    public boolean isIfStatment() {
        return isElseIf;
    }
}
