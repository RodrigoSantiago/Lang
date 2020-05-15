package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import data.CppBuilder;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Num extends Member {

    private ArrayList<Token> nameTokens = new ArrayList<>();
    private ArrayList<Token> initTokens = new ArrayList<>();

    public Num(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
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

            last = token;
            token = next;
        }

        if (state != 3) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    @Override
    public boolean load() {
        return nameTokens.size() > 0;
    }

    public void build(CppBuilder cBuilder) {

    }

    public ArrayList<FieldView> getFields() {
        ArrayList<FieldView> fields = new ArrayList<>();
        for (int i = 0; i < nameTokens.size(); i++) {
            Token nameToken = nameTokens.get(i);
            fields.add(new FieldView(nameToken, type.self, this, i));
        }
        return fields;
    }
}
