package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class BlockElse extends Block {

    TokenGroup contentTokenGroup;

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
            } else if (state == 1 && token.key == Key.BRACE) {
                contentTokenGroup = new TokenGroup(token, next);
                state = 3; // completed
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                state = 3; // empty else
            } else if (state == 1) {

                state = 2; // waiting [;]
            } else if (state == 2 && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }

                contentTokenGroup = new TokenGroup(key.getNext(), next);
                state = 3; // completed
            } else if (state != 2) {
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
