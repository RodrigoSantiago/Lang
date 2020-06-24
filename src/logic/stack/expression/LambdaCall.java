package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.params.Parameters;
import logic.stack.Context;
import logic.stack.StackExpansion;

public class LambdaCall extends Call {

    Parameters params;
    TokenGroup typeToken;
    StackExpansion innerStack;

    public LambdaCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("LAMBDA : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM && token.getChild() != null) {
                params = new Parameters(cFile, token);
                state = 1;
            } else if (state == 1 && token.key == Key.LAMBDA) {
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.BRACE && token.getChild() != null) {
                innerStack = new StackExpansion(getStack(), Pointer.voidPointer);
                innerStack.read(token.getChild(), token.getLastChild(), true);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {

    }

    @Override
    public int verify(Pointer pointer) {
        if (pointer == null) {
            // auto : show error !!! [make variables objects]
        } else {
            // count parameters
        }
        return 0;
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (pointer == null) {
            // auto : show error !!! [make variables objects]
        } else {
            // count parameters
        }
        // load STACK [com as variaveis e tipo de retorno definido]
        return null;
    }

    @Override
    public Pointer requestSet(Pointer pointer) {
        request(pointer);
        return null;
    }
}
