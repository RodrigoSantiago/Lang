package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.expression.Expression;

public class BlockLock extends Block {

    Token paramToken;
    TokenGroup contentTokenGroup;
    Expression expression;

    public BlockLock(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("LOCK");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.LOCK) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                paramToken = token;
                if (paramToken.getChild() != null) {
                    expression = new Expression(this, paramToken.getChild(), paramToken.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing locker", this);
                }
                contentTokenGroup = new TokenGroup(token.getChild(), token.getLastChild());
                state = 3;
            } else if ((state == 1 || state == 2) && token.key == Key.SEMICOLON) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing locker", this);
                }
                state = 3;
            } else if (state == 2) {
                contentTokenGroup = new TokenGroup(token, end);
                next = end;
                state = 3;
            } else  {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (contentTokenGroup != null) {
            Parser.parseLines(this, contentTokenGroup.start, contentTokenGroup.end);
        }
    }
}
