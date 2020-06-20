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

public class BlockIf extends Block {

    Token paramToken;
    TokenGroup contentTokenGroup;
    Expression expression;

    public BlockIf(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("IF");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.IF) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                paramToken = token;
                if (paramToken.getChild() != null) {
                    expression = new Expression(this, paramToken.getChild(), paramToken.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing condition");
                }
                contentTokenGroup = new TokenGroup(token, next);
                state = 4; // completed
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 4; // empty if
            } else if (state == 2) {

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
}
