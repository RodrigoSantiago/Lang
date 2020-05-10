package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Num extends Member {

    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<Token> initTokens = new ArrayList<>();

    public Num(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                nameTokens.add(token);
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                initTokens.add(null);
                state = 0;
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                initTokens.add(null);
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM) {
                initTokens.add(token);
                state = 2;
            } else if (state == 2 && token.key == Key.COMMA) {
                state = 0;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return nameTokens.size() > 0;
    }

    public ArrayList<FieldView> getFields() {
        ArrayList<FieldView> fields = new ArrayList<>();
        for (int i = 0; i < nameTokens.size(); i++) {
            Token nameToken = nameTokens.get(i);
            fields.add(new FieldView(nameToken, null, this, i));
        }
        return fields;
    }
}
