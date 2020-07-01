package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.view.ConstructorView;
import logic.member.view.MethodView;
import logic.stack.Context;

import java.util.ArrayList;

public class ConstructorCall extends Call {

    Token nameToken;
    ConstructorView constructorView;
    ArrayList<Expression> arguments = new ArrayList<>();
    Pointer typePtr;

    public ConstructorCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.THIS || token.key == Key.BASE)) {
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

        getStack().setContainsConstructorCall();
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

    public ConstructorView getConstructorView() {
        return constructorView;
    }

    @Override
    public Pointer getNaturalPtr(Pointer convertFlag) {
        return naturalPtr = Pointer.voidPointer;
    }

    @Override
    public void load(Context context) {
        if (getStack().isConstructorAllowed()) {

            typePtr = getStack().getSourcePtr();

            if (nameToken.key == Key.BASE && typePtr.type != null && typePtr.type.parent != null) {
                typePtr = getStack().getSourcePtr().type.parent;
            }

            ArrayList<ConstructorView> constructors = context.findConstructor(typePtr, arguments);

            if (constructors == null || constructors.size() == 0) {
                cFile.erro(token, "Constructor Not Found", this);
            } else if (constructors.size() > 1) {
                cFile.erro(token, "Ambigous Constructor Call", this);
                constructorView = constructors.get(0);
            } else {
                constructorView = constructors.get(0);
            }

            if (!getStack().addConstructorCall(this)) {
                cFile.erro(nameToken, "Repeated Constructor call", this);
                context.jumpTo(null);
            }
        } else {
            cFile.erro(nameToken, "Constructor call not allowed", this);
            context.jumpTo(null);
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return 0;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (pointer != null && pointer != getNaturalPtr(pointer)) {
            cFile.erro(token, "Cannot cast void", this);
        }

        if (constructorView != null) {
            Constructor constructor = constructorView.constructor;
            if (!constructorView.isPublic() && !constructorView.isPrivate() &&
                    !getStack().cFile.library.equals(constructor.cFile.library)) {
                cFile.erro(token, "Cannot acess a Internal member from other Library", this);
            } else if (constructorView.isPrivate() &&
                    !getStack().cFile.equals(constructor.cFile)) {
                cFile.erro(token, "Cannot acess a Private member from other file", this);
            }
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (pointer != null && pointer != getNaturalPtr(pointer)) {
            cFile.erro(token, "Cannot cast void", this);
        }

        if (constructorView != null) {
            if (!constructorView.isPublic() && !constructorView.isPrivate() &&
                    !getStack().cFile.library.equals(constructorView.getFile().library)) {
                cFile.erro(token, "Cannot acess a Internal member from other Library", this);
            } else if (constructorView.isPrivate() &&
                    !getStack().cFile.equals(constructorView.getFile())) {
                cFile.erro(token, "Cannot acess a Private member from other file", this);
            }
        }
    }

    @Override
    public void requestSet() {

    }

    @Override
    public void build(CppBuilder cBuilder, int idt) {
        if (token.key == Key.THIS) {
            cBuilder.add("create(").add(arguments, idt).add(")");
        } else {
            cBuilder.path(typePtr, false).add("::create(").add(arguments, idt).add(")");
        }
    }
}
