package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;

import java.util.ArrayList;

public class LineVar extends Line {

    TokenGroup typeToken;
    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<TokenGroup> initTokens = new ArrayList<>();

    public LineVar(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("VAR");

        boolean isLet = false, isFinal = false;

        TokenGroup initToken = null;
        Token nameToken = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.FINAL && !isFinal) {
                isFinal = true;
            } else if (state == 0 && token.key == Key.LET) {
                isLet = true;
                state = 1;
            } else if (state == 0 && token.key == Key.VAR) {
                state = 1;
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 2;
            } else if (state == 1 && token.key == Key.WORD) {
                if (isLet) {
                    if (next == end || next.key == Key.SEMICOLON || next.key == Key.SETVAL || next.key == Key.COMMA) {
                        nameToken = token;
                        state = 3;
                    } else {
                        typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                        state = 2;
                    }
                } else {
                    nameToken = token;
                    state = 3;
                }
            } else if (state == 2 && token.key == Key.WORD) {
                nameToken = token;
                state = 3;
            } else if (state == 3 && token.key == Key.SETVAL) {
                if (next != end && next.key != Key.COMMA && next.key != Key.SEMICOLON) {
                    while (next != end && next.key != Key.COMMA && next.key != Key.SEMICOLON) {
                        next = next.getNext();
                    }
                    initToken = new TokenGroup(token.getNext(), next);
                }
            } else if (state == 3 && token.key == Key.COMMA) {
                nameTokens.add(nameToken);
                initTokens.add(initToken);
                nameToken = null;
                initToken = null;
                state = 2;
            } else if (state == 3 && token.key == Key.SEMICOLON) {
                nameTokens.add(nameToken);
                initTokens.add(initToken);
                nameToken = null;
                initToken = null;
                state = 4;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token");
            }
            if (state != 4 && next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
