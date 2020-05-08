package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Variable extends Member {

    TokenGroup typeToken;
    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<TokenGroup> initTokens = new ArrayList<>();

    public Variable(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                this.token = token;
                if (next != null && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                typeToken = new TokenGroup(token, next);
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                nameTokens.add(token);
                state = 2;
            } else if (state == 2 && token.key == Key.SETVAL) {
                while (next != end && (next.key != Key.COMMA && next.key != Key.SEMICOLON)) {
                    next = next.getNext();
                }
                initTokens.add(new TokenGroup(token, next));

                if (next != end && next.key == Key.COMMA) {
                    next = next.getNext();
                    state = 1;
                } else if (next != end && next.key == Key.SEMICOLON) {
                    next = next.getNext();
                    state = 3;
                }
            } else if (state == 2 && token.key == Key.COMMA) {
                initTokens.add(null);
                state = 1;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                initTokens.add(null);
                state = 3;
            }
            token = next;
        }
    }

    public ArrayList<FieldView> getFields() {
        ArrayList<FieldView> fields =  new ArrayList<>();
        for (int i = 0; i < nameTokens.size(); i++) {
            fields.add(new FieldView(nameTokens.get(i), null, this, i));
        }
        return fields;
    }

    @Override
    public boolean load() {
        return true;
    }
}
