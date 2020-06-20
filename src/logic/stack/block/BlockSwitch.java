package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class BlockSwitch extends Block {

    Token paramToken;
    TokenGroup contentTokenGroup;

    public BlockSwitch(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("SWITCH");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.SWITCH) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                paramToken = token;
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing condition");
                }
                contentTokenGroup = new TokenGroup(token, next);
                state = 3; // completed
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
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
}
