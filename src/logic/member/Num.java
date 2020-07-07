package logic.member;

import content.Key;
import content.Token;
import builder.CppBuilder;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Num extends Member {

    private ArrayList<Token> nameTokens = new ArrayList<>();
    private ArrayList<Token> initTokens = new ArrayList<>();
    private ArrayList<FieldView> fields;

    public Num(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                nameTokens.add(token);
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                initTokens.add(null);
                state = 0;
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                initTokens.add(null);
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                initTokens.add(token);
                state = 2;
            } else if (state == 2 && token.key == Key.COMMA) {
                state = 0;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

    }

    @Override
    public boolean load() {
        return nameTokens.size() > 0;
    }

    @Override
    public void make() {

    }

    @Override
    public void build(CppBuilder cBuilder) {

    }

    public int getCount() {
        return nameTokens.size();
    }

    public Token getName(int pos) {
        return nameTokens.get(pos);
    }

    public ArrayList<FieldView> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
            for (int i = 0; i < nameTokens.size(); i++) {
                Token nameToken = nameTokens.get(i);
                fields.add(new FieldView(nameToken, type.self, this, i));
            }
        }
        return fields;
    }
}
