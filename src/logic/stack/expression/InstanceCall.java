package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.ConstructorView;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.StackExpansion;

import java.util.ArrayList;

public class InstanceCall extends Call {

    TokenGroup typeToken;
    ConstructorView constructorView;
    ArrayList<Expression> indexArguments = new ArrayList<>();
    ArrayList<Expression> initArguments = new ArrayList<>();
    ArrayList<Expression> arguments = new ArrayList<>();

    StackExpansion initStack;
    Pointer typePtr;

    public InstanceCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("INSTANCE : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NEW) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (next != end && next.key == Key.GENERIC) {
                    next = next.getNext();
                }
                this.token = token;
                typeToken = new TokenGroup(token, next);

                while (next != end && next.key == Key.INDEX) {
                    if (next.getChild() == null) {
                        cFile.erro(next, "Unexpected token", this);
                    } else if (next.isEmptyParent()) {
                        indexArguments.add(null);
                    } else {
                        readArguments(getLine(), this.indexArguments, next.getChild(), next.getLastChild());
                    }
                    next = next.getNext();
                }
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM && token.getChild() != null) {
                readArguments(getLine(), this.arguments, token.getChild(), token.getLastChild());
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.BRACE && token.getChild() != null) {
                initStack = new StackExpansion(getStack(), Pointer.voidPointer);
                initStack.read(token.getChild(), token.getLastChild(), false);
                readArguments(initStack.block, this.initArguments, token.getChild(), token.getLastChild());

                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && (state != 2 && state != 3 && state != 4)) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    private void readArguments(Line line, ArrayList<Expression> arguments, Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 2) && token.key != Key.COMMA) {
                while (next != null && next != end && next.key != Key.COMMA) {
                    next = next.getNext();
                }
                arguments.add(new Expression(line, token, next));
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
        if (typeToken == null) {
            context.jumpTo(null);
        } else {
            typePtr = context.getPointer(typeToken);

            // TODO - ARRAY/INIT BEHAVIOR
            // TODO [DEFAULT CONSTRUCTOR PROBLEMATIC]
            ArrayList<ConstructorView> constructors = context.findConstructor(typePtr, arguments);
            if (constructors == null || constructors.size() == 0) {
                cFile.erro(token, "Constructor Not Found", this);
            } else if (constructors.size() > 1) {
                cFile.erro(token, "Ambigous Constructor Call", this);
                constructorView = constructors.get(0);
            } else {
                constructorView = constructors.get(0);
            }
            context.jumpTo(typePtr);
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return typePtr == null ? 0 : pointer.canReceive(typePtr);
    }

    @Override
    public Pointer getNaturalPtr(Pointer convertFlag) {
        naturalPtr = typePtr;
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
