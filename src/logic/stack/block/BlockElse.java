package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;

public class BlockElse extends Block {

    TokenGroup contentTokenGroup;
    Expression condition;
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
            } else if (state == 2 && token.key == Key.PARAM) {
                if (token.getChild() != null) {
                    condition = new Expression(this, token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 4;
            } else if ((state == 1 || state == 4) && token.key == Key.BRACE) {
                contentTokenGroup = new TokenGroup(token, next);
                state = 6; // ENDED
            } else if ((state == 1 || state == 4) && token.key == Key.SEMICOLON) {
                state = 6; // ENDED empty
            } else if ((state == 1 || state == 4)) {
                state = 7;
            } else if (state == 7 && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }

                contentTokenGroup = new TokenGroup(key.getNext(), next);
                state = 6;
            } else if (state != 7) {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 6) {
                if (state == 7) {
                    cFile.erro(token, "Semicolon expected");
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
            }
            token = next;
        }

        if (contentTokenGroup != null) {
            if (contentTokenGroup.start.key == Key.BRACE) {
                Parser.parseLines(this, contentTokenGroup.start.getChild(), contentTokenGroup.start.getLastChild());
            }
        }
    }

    @Override
    public boolean isIfStatment() {
        return isElseIf;
    }
}
