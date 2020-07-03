package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.Pointer;
import logic.stack.Context;

public class TypeCall extends Call {

    TokenGroup typeToken;
    Pointer typePtr;

    public TypeCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.WORD)) {
                this.token = token;
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
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

    @Override
    public boolean isTypeCall() {
        return true;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    @Override
    public void load(Context context) {
        if (token == null) {
            context.jumpTo(null);
        } else {
            typePtr = context.getPointer(typeToken);
            if (typePtr == null) {
                cFile.erro(token, "Type Not Found", this);
            }
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return 0;
    }

    @Override
    public Pointer getNaturalPtr(Pointer pointer) {
        cFile.erro(getToken(), "Unexpected identifier", this);
        return null;
    }

    @Override
    public void requestGet(Pointer pointer) {
        cFile.erro(getToken(), "Unexpected identifier", this);
    }

    @Override
    public void requestOwn(Pointer pointer) {
        cFile.erro(getToken(), "Unexpected identifier", this);
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "Unexpected identifier", this);
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, boolean next) {
        cBuilder.add(typePtr);
    }
}