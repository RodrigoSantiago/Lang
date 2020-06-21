package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.IndexerView;
import logic.member.view.MethodView;
import logic.stack.Context;

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
            if (state == 0 && token.key == Key.INDEX && token.getChild() != null) {
                this.token = token;
                readArguments(token.getChild(), token.getLastChild());
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens", this);
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
                arguments.add(new Expression(getLine(), contentStart, token));
                state = 1;
            } else if (state == 1 && token.key != Key.COMMA) {
                contentStart = token;
                state = 0;
            }
            if (next == end) {
                if (state == 0) {
                    arguments.add(new Expression(getLine(), contentStart, next));
                } else {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        ArrayList<IndexerView> indexers = context.findIndexer(arguments);
        if (indexers.size() == 0) {
            // erro
        } else if (indexers.size() > 1) {
            // erro
            indexerView = indexers.get(0);
        } else {
            indexerView = indexers.get(0);
        }
        context.jumpTo(indexerView == null ? null : indexerView.getTypePtr());
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (indexerView == null) return null;

        return indexerView.getTypePtr();
    }
}
