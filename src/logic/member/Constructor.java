package logic.member;

import content.Key;
import content.Token;
import data.CppBuilder;
import logic.params.Parameters;
import logic.typdef.Type;

public class Constructor extends Member {

    private Parameters params;
    private Token contentToken;

    public Constructor(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();

            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, false, true, false, true);
            } else if (state == 0 && token.key == Key.THIS) {
                this.token = token;
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                params = new Parameters(cFile, token);
                state = 2;
            } else if (state == 2 && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                contentToken = token;
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
        if (token != null && params != null) {
            params.load(isStatic() ? null : type);

            if (isStatic() && params.args.size() > 0) {
                cFile.erro(token, "Static constructors cannot have parameters");
            }

            return true;
        }
        return false;
    }

    public void build(CppBuilder cBuilder) {
        cBuilder.toHeader();
        cBuilder.idt(1);

        if (isStatic()) {

        } else {
            cBuilder.add(type.self).add(" create(").add(params).add(");").ln();

            cBuilder.toSource();
            cBuilder.add(type.template)
                    .add(type.self)
                    .add(" ").path(type.self, false).add("::create(").add(params).add(") {").ln()
                    .add("}").ln()
                    .ln();
        }

        cBuilder.toHeader();
    }

    public Parameters getParams() {
        return params;
    }

    @Override
    public String toString() {
        return (isStatic() ? "static this (" : "this (") + params + ")";
    }
}
