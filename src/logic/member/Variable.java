package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Variable extends Member {

    private TokenGroup typeToken;
    private Pointer typePtr;

    private ArrayList<Token> nameTokens = new ArrayList<>();
    private ArrayList<TokenGroup> initTokens = new ArrayList<>();

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
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
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
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, isStatic() ? null : type, isLet());
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet());
            }

            return nameTokens.size() > 0 ;
        }
        return false;
    }

    public void build(CppBuilder cBuilder) {
        if (typePtr.type != null && typePtr.typeSource == null && typePtr.type.isStruct()) {
            cBuilder.dependence(typePtr);
        }

        cBuilder.toHeader();
        cBuilder.idt(1);
        if (isStatic()) {
            cBuilder.add("static ");
        }
        for (Token name : nameTokens) {
            cBuilder.add(typePtr)
                    .add(" f_").add(name).add(";").ln();
        }

        if (isStatic()) {
            for (Token name : nameTokens) {
                cBuilder.toSource(type.template != null);
                if (!isStatic()) {
                    cBuilder.add(type.template);
                }
                cBuilder.add(typePtr)
                        .add(" ").path(type.self, isStatic()).add("::f_").add(name).add(" = ");
                if (typePtr.typeSource != null) {
                    cBuilder.add("lang::generic<").add(typePtr).add(">::def();").ln();
                } else if (typePtr.type != null && (typePtr.type.isPointer() || typePtr.type.isFunction())) {
                    cBuilder.add("nullptr;").ln();
                } else if (typePtr.type != null && typePtr.type.isValue() && !typePtr.type.isLangBase()) {
                    cBuilder.add(typePtr).add("();").ln();
                } else {
                    cBuilder.add("0;").ln();
                }

                // Static Init Values should be on Constructors. Always start with 0
            }
        }
    }
    public void buildInit(CppBuilder cBuilder) {
        if (isStatic()) {
            // TODO - Use Init Block
            for (Token name : nameTokens) {
                cBuilder.idt(1).add("f_").add(name).add(" = ");
                if (typePtr.typeSource != null) {
                    cBuilder.add("lang::generic<").add(typePtr).add(">::def()");
                } else if (typePtr.type != null && (typePtr.type.isPointer() || typePtr.type.isFunction())) {
                    cBuilder.add("nullptr");
                } else if (typePtr.type != null && typePtr.type.isValue() && !typePtr.type.isLangBase()) {
                    cBuilder.path(typePtr, false).add("()");
                } else {
                    cBuilder.add("0");
                }
                cBuilder.add(";").ln();
            }
        } else {
            for (int i = 0; i < nameTokens.size(); i++) {
                Token name = nameTokens.get(i);
                if (i > 0) {
                    cBuilder.add(", ").ln();
                }

                cBuilder.idt(1).add("f_").add(name).add("(");
                if (typePtr.typeSource != null) {
                    cBuilder.add("lang::generic<").add(typePtr).add(">::def()");
                } else if (typePtr.type != null && (typePtr.type.isPointer() || typePtr.type.isFunction())) {
                    cBuilder.add("nullptr");
                } else if (typePtr.type != null && typePtr.type.isValue() && !typePtr.type.isLangBase()) {

                } else {
                    cBuilder.add("0");
                }
                cBuilder.add(")");
            }
        }
    }

    public ArrayList<FieldView> getFields() {
        ArrayList<FieldView> fields =  new ArrayList<>();
        for (int i = 0; i < nameTokens.size(); i++) {
            fields.add(new FieldView(nameTokens.get(i), typePtr, this, i));
        }
        return fields;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public TokenGroup getTypeToken() {
        return typeToken;
    }
}
