package logic.stack.expression;

import content.Key;
import content.Token;
import logic.Pointer;
import logic.stack.Context;

import java.util.ArrayList;

public class InitCall extends Call {

    public ArrayList<Expression> arguments = new ArrayList<>();
    Pointer requestType;
    Pointer requestArray;

    public InitCall(CallGroup group, Token start, Token end, Pointer array) {
        super(group, start, end);
        requestArray = array;
        requestType = array.pointers[0];
        Pointer innerRequest = null;
        if (requestType.type == cFile.langArray()) {
            innerRequest = requestType;
        }

        end = start.getLastChild();
        Token token = start.getChild();
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 2) && token.key != Key.COMMA) {
                while (next != null && next != end && next.key != Key.COMMA) {
                    next = next.getNext();
                }
                arguments.add(new Expression(getLine(), token, next, innerRequest));
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 2) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        for (Expression arg : arguments) {
            arg.load(new Context(context));
            arg.requestOwn(requestType);
        }
    }

    @Override
    public Pointer getNaturalPtr(Pointer convertFlag) {
        naturalPtr = requestArray;
        return naturalPtr;
    }

    @Override
    public int verify(Pointer pointer) {
        return requestArray != null ? pointer.canReceive(requestArray) : 0;
    }

    @Override
    public void requestGet(Pointer pointer) {
        // [never gona happen]
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }
}
