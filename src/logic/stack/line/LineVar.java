package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.expression.Expression;

import java.util.ArrayList;

public class LineVar extends Line {

    TokenGroup typeToken;
    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<Expression> expresions = new ArrayList<>();

    public LineVar(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("VAR");

        boolean isLet = false, isFinal = false;

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
                while (next != end && next.key != Key.COMMA && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }
                state = 4;
            } else if ((state == 3 || state == 4) && token.key == Key.COMMA) {
                nameTokens.add(nameToken);
                expresions.add(state == 4 ? new Expression(this, nameToken, token) : null);
                nameToken = null;
                state = 2;
            } else if ((state == 3 || state == 4) && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }

                nameTokens.add(nameToken);
                expresions.add(state == 4 ? new Expression(this, nameToken, token.key == Key.SEMICOLON ? token : next) : null);
                nameToken = null;
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