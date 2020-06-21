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
                cFile.erro(token, "Unexpected token", this);
            }

            last = token;
            token = next;
        }

        if (state != 3) {
            cFile.erro(last, "Unexpected end of tokens", this);
        }
    }

    @Override
    public boolean load() {
        if (token != null && params != null) {
            params.load(isStatic() ? null : type);

            if (isStatic() && params.getCount() > 0) {
                cFile.erro(token, "Static constructors cannot have parameters", this);
            }

            return true;
        }
        return false;
    }

    public void build(CppBuilder cBuilder) {
        if (isStatic()) {
            cBuilder.idt(1).add("// static initializer");
        } else {
            if (type.isPointer()) {
                cBuilder.toHeader();
                cBuilder.idt(1).path(type.self, false).add("*").add(" create(").add(params).add(");").ln();

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("*")
                        .add(" ").path(type.self, false).add("::create(").add(params).add(") {").ln()
                        .idt(1).add("return this;").ln()
                        .add("}").ln()
                        .ln();
            } else {
                cBuilder.toHeader();
                if (params.isEmpty()) {
                    cBuilder.idt(1).add(type.pathToken).add("(empty e);").ln();
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.idt(1).add(type.pathToken).add("(empty e, ").add(type.self).add(" v_").add(params.getNameToken(0)).add(");").ln();
                } else{
                    cBuilder.idt(1).add(type.pathToken).add("(").add(params).add(");").ln();
                }

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("::").add(type.pathToken);
                if (params.isEmpty()) {
                    cBuilder.add("(empty e) : ").add(type.pathToken).add("() {").ln();
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.add("(empty e, ").add(type.self)
                            .add(" v_").add(params.getNameToken(0)).add(") : ").add(type.pathToken).add("() {").ln();
                } else {
                    cBuilder.add("(").add(params).add(") : ").add(type.pathToken).add("() {").ln();
                }
                cBuilder.add("}").ln()
                        .ln();
            }
        }

        cBuilder.toHeader();
    }

    public Parameters getParams() {
        return params;
    }

    public void markDefault() {
        isDefault = true;
    }

    @Override
    public String toString() {
        return (isStatic() ? "static this (" : "this (") + params + ")";
    }
}
