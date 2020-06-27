package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.Method;
import logic.member.view.MethodView;
import logic.stack.Context;

import java.util.ArrayList;

public class MethodCall extends Call {

    Token nameToken;
    MethodView methodView;
    ArrayList<Expression> arguments = new ArrayList<>();

    public MethodCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("METHOD : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                this.token = token;
                this.nameToken = token;
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                readArguments(token.getChild(), token.getLastChild());
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 2) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    private void readArguments(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 2) && token.key != Key.COMMA) {
                while (next != null && next != end && next.key != Key.COMMA) {
                    next = next.getNext();
                }
                arguments.add(new Expression(getLine(), token, next));
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
        if (nameToken == null) {
            context.jumpTo(null);
        } else {

            // TODO - INNER CONSTRUCTOR BEHAVIOR
            ArrayList<MethodView> methods = context.findMethod(nameToken, arguments);
            if (methods == null || methods.size() == 0) {
                cFile.erro(token, "Method Not Found", this);
            } else if (methods.size() > 1) {
                cFile.erro(token, "Ambigous Method Call", this);
                methodView = methods.get(0);
            } else {
                methodView = methods.get(0);
                System.out.println(methodView);
            }
            context.jumpTo(methodView == null ? null : methodView.getTypePtr());
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return methodView == null ? 0 : pointer.canReceive(methodView.getTypePtr());
    }

    @Override
    public Pointer getNaturalPtr(Pointer pointer) {
        if (naturalPtr == null && methodView != null) {
            naturalPtr = methodView.getTypePtr();
        }
        return naturalPtr;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        if (methodView != null) {
            Method method = methodView.method;
            if (!methodView.isPublic() && !methodView.isPrivate()) {
                if (!getStack().cFile.library.equals(method.cFile.library)) {
                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                }
            } else if (methodView.isPrivate()) {
                if (!getStack().cFile.equals(method.cFile)) {
                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                }
            }
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        if (Pointer.OwnTable(pointer, naturalPtr) == -1) {
            cFile.erro(token, "Cannot convert a STRONG reference to a WEAK reference", this);
        }

        if (methodView != null) {
            Method method = methodView.method;
            if (!methodView.isPublic() && !methodView.isPrivate()) {
                if (!getStack().cFile.library.equals(method.cFile.library)) {
                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                }
            } else if (methodView.isPrivate()) {
                if (!getStack().cFile.equals(method.cFile)) {
                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                }
            }
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }
}
