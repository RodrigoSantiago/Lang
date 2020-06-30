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
            ArrayList<MethodView> methods = context.findMethod(nameToken, arguments);
            if (methods == null || methods.size() == 0) {
                cFile.erro(token, "Method Not Found", this);
            } else if (methods.size() > 1) {
                cFile.erro(token, "Ambigous Method Call", this);
                methodView = methods.get(0);
            } else {
                methodView = methods.get(0);
                if (context.isStatic() && !methodView.isStatic()) {
                    cFile.erro(token, "Cannot use a Instance Member on a Static Environment", this);
                }
                if (!context.isBegin() && !context.isStatic() && methodView.isStatic()) {
                    cFile.erro(token, "Cannot use a Static Member on a Instance Environment", this);
                }
            }
            if (methodView == null) {
                context.jumpTo(null);
            }
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return methodView == null ? 0 : pointer.canReceive(methodView.getTypePtr());
    }

    @Override
    public Pointer getNaturalPtr(Pointer pointer) {
        if (methodView != null) {
            naturalPtr = methodView.getTypePtr();
            if (methodView.isTemplateReturnEntry()) {
                if (pointer != null) {
                    Pointer[] captureList = methodView.getCaptureList();
                    naturalPtr = methodView.getTypePtr();
                    for (int j = 0; j < methodView.getTemplate().getGenCount(); j++) {
                        Pointer ptr = Pointer.capture(
                                methodView.getTemplate().getGeneric(j),
                                methodView.getTypePtr(), pointer);
                        if (ptr != null && (captureList[j] == null || captureList[j].typeSource == methodView.getTemplate().getGeneric(j))) {
                            captureList[j] = ptr;
                        }
                    }
                    for (int j = 0; j < methodView.getTemplate().getGenCount(); j++) {
                        naturalPtr = Pointer.apply(
                                methodView.getTemplate().getGeneric(j),
                                captureList[j], naturalPtr);
                    }
                    if (naturalPtr != null) {
                        for (int j = 0; j < methodView.getTemplate().getGenCount(); j++) {
                            naturalPtr = Pointer.force(methodView.getTemplate().getGeneric(j), naturalPtr);
                        }
                    }
                } else {
                    naturalPtr = cFile.langObjectPtr().toLet(methodView.isLet());
                }
            }
        }
        return naturalPtr;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (naturalPtr != pointer && pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        if (methodView != null) {
            Method method = methodView.method;
            if (!methodView.isPublic() && !methodView.isPrivate() &&
                    !getStack().cFile.library.equals(method.cFile.library)) {
                cFile.erro(token, "Cannot acess a Internal member from other Library", this);
            } else if (methodView.isPrivate() &&
                    !getStack().cFile.equals(method.cFile)) {
                cFile.erro(token, "Cannot acess a Private member from other file", this);
            }
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (naturalPtr != pointer && pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        if (Pointer.OwnTable(pointer, naturalPtr) == -1) {
            cFile.erro(token, "Cannot convert a STRONG reference to a WEAK reference", this);
        }

        if (methodView != null) {
            Method method = methodView.method;
            if (!methodView.isPublic() && !methodView.isPrivate() &&
                    !getStack().cFile.library.equals(method.cFile.library)) {
                cFile.erro(token, "Cannot acess a Internal member from other Library", this);
            } else if (methodView.isPrivate() &&
                    !getStack().cFile.equals(method.cFile)) {
                cFile.erro(token, "Cannot acess a Private member from other file", this);
            }
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }
}
