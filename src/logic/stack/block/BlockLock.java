package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.expression.Expression;

public class BlockLock extends Block {

    Token lockerToken;
    Expression lockerExp;
    TokenGroup contentToken;


    public BlockLock(Block block, Token start, Token end) {
        super(block, start, end);

        Token colon = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.LOCK) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                lockerToken = token;
                if (token.isEmptyParent()) {
                    cFile.erro(token, "Empty locker", this);
                } else {
                    lockerExp = new Expression(this, token.getChild(), token.getLastChild());
                }
                state = 2;
            } else if (state == 2 && token.key == Key.COLON) {
                colon = token;
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.WORD && next != end && next.key == Key.BRACE) {
                cFile.erro(token, "Label Not Allowed", this);
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.BRACE) {
                if (state == 1) cFile.erro(token.start, token.start + 1, "Missing condition", this);
                if (state == 3) cFile.erro(colon, "Unexpected Token", this);

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
                if (state == 1) cFile.erro(token, "Missing condition", this);
                if (state == 3) cFile.erro(colon, "Unexpected Token", this);

                state = 5;
            } else if ((state == 2 || state == 3 || state == 4)) {
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
        if (lockerExp != null) {
            lockerExp.load(new Context(stack));
            lockerExp.requestGet(cFile.langLockerPtr().toLet());
        }
        super.load();
    }
}
