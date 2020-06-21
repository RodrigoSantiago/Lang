package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Context;

public class LiteralCall extends Call {

    Token token;
    Pointer typePtr;

    public LiteralCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("LITERAL : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.STRING) {
                typePtr = getStack().cFile.langStringPtr();
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NUMBER) {
                typePtr = getStack().cFile.langIntPtr();
                this.token = token;
                state = 1;
            } else if (state == 0 && (token.key == Key.TRUE || token.key == Key.FALSE)) {
                typePtr = getStack().cFile.langBoolPtr();
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NULL) {
                typePtr = Pointer.nullPointer;
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.DEFAULT) {
                typePtr = Pointer.openPointer;
                this.token = token;
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
    public void load(Context context) {
        context.jumpTo(typePtr);
    }

    @Override
    public Pointer request(Pointer pointer) {
        return typePtr;
    }
}
