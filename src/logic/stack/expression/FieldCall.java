package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.Context;
import logic.stack.Field;
import logic.stack.LocalVar;
import logic.typdef.Type;

public class FieldCall extends Call {

    Type staticCall;
    FieldView fieldView;
    Field field;

    public FieldCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("FIELD : "+ TokenGroup.toString(start, end));

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.SUPER)) {
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

    public boolean isStaticCall() {
        return staticCall != null;
    }

    @Override
    public void load(Context context) {
        if (token == null) {
            context.jumpTo(null);
        } else {
            field = token.isComplex() ? null : context.findLocalField(token);
            if (field == null) {
                fieldView = token.isComplex() ? null : context.findField(token);
                if (fieldView == null) {
                    staticCall = context.findType(token);
                    if (staticCall == null) {
                        if (token.isComplex()) {
                            cFile.erro(token, "Type Not Found", this);
                        } else {
                            cFile.erro(token, "Field Not Found", this);
                        }
                    } else {
                        context.jumpTo(staticCall, true);
                    }
                } else {
                    context.jumpTo(fieldView.getTypePtr());
                }
            } else {
                if (!getLine().isChildOf(field.getSource())) {
                    cFile.erro(token, "Field Not acessible", this);
                }
                context.jumpTo(field.getTypePtr());
            }
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return staticCall != null ? 0 : field != null ? pointer.canReceive(field.getTypePtr()) :
                fieldView != null ? pointer.canReceive(fieldView.getTypePtr()) : 0;
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (staticCall != null || (field == null && fieldView == null)) return null;
        if (returnPtr == null) {
            returnPtr = field != null ? field.getTypePtr() : fieldView.getTypePtr();
            if (returnPtr != null && pointer != null) {
                returnPtr = pointer.canReceive(returnPtr) > 0 ? pointer : null;
            }
        }
        return returnPtr;
    }

    @Override
    public Pointer requestSet(Pointer pointer) {
        return request(pointer);
    }
}
