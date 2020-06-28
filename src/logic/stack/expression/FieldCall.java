package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.Context;
import logic.stack.Field;
import logic.typdef.Type;

public class FieldCall extends Call {

    private Type staticCall;
    private FieldView fieldView;
    private Field field;

    private boolean useGet, useSet, useOwn;

    public FieldCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("FIELD : "+ TokenGroup.toString(start, end));

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.BASE)) {
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
    public boolean isTypeCall() {
        return staticCall != null;
    }

    public Pointer getTypePtr() {
        return staticCall == null ? null : staticCall.self;
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
                    if (context.isStatic() && !fieldView.isStatic()) {
                        cFile.erro(token, "Cannot use a Instance Member on a Static Environment", this);
                    }
                    context.jumpTo(fieldView.getTypePtr());
                }
            } else {
                if (!getLine().isChildOf(field.getSource())) {
                    cFile.erro(token, "Field Not Acessible", this);
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
    public Pointer getNaturalPtr(Pointer convertFlag) {
        if (naturalPtr == null) {
            if (field != null) {
                naturalPtr = field.getTypePtr();
            } else if (fieldView != null) {
                naturalPtr = fieldView.getTypePtr();
            }
        }
        return naturalPtr;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (staticCall != null) {
            cFile.erro(getToken(), "Unexpected identifier", this);
        }

        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        useGet = true;

        if (fieldView != null) {
            if (!fieldView.hasGet()) {
                cFile.erro(token, "GET member not defined", this); // [impossible ?]
            } else if (!fieldView.isGetPublic() && !fieldView.isGetPrivate() &&
                    !getStack().cFile.library.equals(fieldView.getGetFile().library)) {
                cFile.erro(token, "Cannot acess a Internal member from other Library", this);
            } else if (fieldView.isGetPrivate() &&
                    !getStack().cFile.equals(fieldView.getGetFile())) {
                cFile.erro(token, "Cannot acess a Private member from other file", this);
            }
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (staticCall != null) {
            cFile.erro(getToken(), "Unexpected identifier", this);
        }

        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        int table = Pointer.OwnTable(pointer, naturalPtr);

        if (table == 0) useOwn = true;
        else if (table == 1) useGet = true;
        else if (table == 2) useOwn = useGet = true;
        else cFile.erro(token, "Cannot convert a STRONG reference to a WEAK reference", this);

        if (fieldView != null) {
            if (useOwn && fieldView.hasOwn()) {
                if (!fieldView.isOwnPublic() && !fieldView.isOwnPrivate() &&
                        !getStack().cFile.library.equals(fieldView.getOwnFile().library)) {
                    if (useGet) {
                        useOwn = false;
                    } else {
                        cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                    }
                } else if (fieldView.isOwnPrivate() &&
                        !getStack().cFile.equals(fieldView.getOwnFile())) {
                    if (useGet) {
                        useOwn = false;
                    } else {
                        cFile.erro(token, "Cannot acess a Private member from other file", this);
                    }
                } else if (fieldView.isReadOnly(getStack())) {
                    cFile.erro(token, "Cannot SET a final variable", this);
                }
            } else {
                if (useGet) {
                    useOwn = false;
                } else {
                    cFile.erro(token, "OWN member not defined", this);
                }
            }

            if (useGet && !useOwn) {
                if (!fieldView.hasGet()) {
                    cFile.erro(token, "GET member not defined", this); // [impossible ?]
                } else if (!fieldView.isGetPublic() && !fieldView.isGetPrivate() &&
                        !getStack().cFile.library.equals(fieldView.getGetFile().library)) {
                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                } else if (fieldView.isGetPrivate() &&
                        !getStack().cFile.equals(fieldView.getGetFile())) {
                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                }
            }
        } else if (field != null && field.isReadOnly(getStack())) {
            if (useGet) {
                useOwn = false;
            } else {
                cFile.erro(token, "Cannot OWN a final variable", this);
            }
        }
    }

    @Override
    public void requestSet() {
        if (staticCall != null) {
            cFile.erro(getToken(), "Unexpected identifier", this);
        }

        useSet = true;

        if (fieldView != null) {
            if (fieldView.hasSet()) {
                if (!fieldView.isSetPublic() && !fieldView.isSetPrivate() &&
                        !getStack().cFile.library.equals(fieldView.getSetFile().library)) {
                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                } else if (fieldView.isSetPrivate() &&
                        !getStack().cFile.equals(fieldView.getSetFile())) {
                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                } else if (fieldView.isReadOnly(getStack())) {
                    cFile.erro(token, "Cannot SET a final variable", this);
                }
            }  else {
                cFile.erro(token, "SET member not defined", this);
            }
        } else if (field != null && field.isReadOnly(getStack())) {
            cFile.erro(token, "Cannot SET a final variable", this);
        }
    }
}
