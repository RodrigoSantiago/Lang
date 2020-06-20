package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.member.view.IndexerView;

import java.util.ArrayList;

public class IndexerCall extends Call {

    Token token;
    IndexerView indexerView;
    ArrayList<Expression> arguments = new ArrayList<>();

    public IndexerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("INDEXER : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.INDEX) {
                this.token = token;
                if (token.getChild() != null) {
                    readArguments(token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readArguments(Token start, Token end) {
        Token contentStart = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.COMMA) {
                arguments.add(new Expression(getStack(), contentStart, token));
                state = 1;
            } else if (state == 1 && token.key != Key.COMMA) {
                contentStart = token;
                state = 0;
            }
            if (next == end) {
                if (state == 0) {
                    arguments.add(new Expression(getStack(), contentStart, next));
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
            }
            token = next;
        }
    }
}
