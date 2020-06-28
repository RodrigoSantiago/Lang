package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.Method;
import logic.member.view.ConstructorView;
import logic.member.view.FieldView;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.StackExpansion;

import java.util.ArrayList;

public class InstanceCall extends Call {

    TokenGroup typeToken;
    TokenGroup paramToken;
    Token initToken;

    ConstructorView constructorView;
    ArrayList<Expression> indexArguments = new ArrayList<>();
    ArrayList<Expression> arguments = new ArrayList<>();
    Expression arrayInit;

    ArrayList<TokenGroup> initTokens = new ArrayList<>();
    ArrayList<FieldView> initFields = new ArrayList<>();
    ArrayList<Expression> initArguments = new ArrayList<>();

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
                        readArguments(this.indexArguments, next.getChild(), next.getLastChild());
                    }
                    next = next.getNext();
                }
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM && token.getChild() != null) {
                paramToken = new TokenGroup(token.getChild(), token.getLastChild());
                readArguments(this.arguments, token.getChild(), token.getLastChild());
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.BRACE && token.getChild() != null) {
                initToken = token;
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

    private void readArguments(ArrayList<Expression> arguments, Token start, Token end) {
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

    private void readInitTokens(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 2) && token.key != Key.COMMA) {
                while (next != null && next != end && next.key != Key.COMMA) {
                    next = next.getNext();
                }
                initTokens.add(new TokenGroup(token, next));
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

    private void readInitField(Pointer typePtr, TokenGroup tokenGroup) {
        FieldView fieldView = null;
        Token fieldToken = null;
        Token opToken = null;
        Expression expression = null;

        Token token = tokenGroup.start;
        Token next;
        int state = 0;
        while (token != null && token != tokenGroup.end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                fieldToken = token;
                state = 1;
            } else if (state == 1 && (token.key.isOperator || token.key == Key.COLON)) {
                opToken = token;
                if (opToken.key != Key.SETVAL) {
                    cFile.erro(token, "Only SET operation is allowed", this);
                }
                state = 2;
                if (next != tokenGroup.end) {
                    expression = new Expression(getLine(), next, tokenGroup.end);
                    next = tokenGroup.end;
                    state = 3;
                }
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            token = next;
        }
        if (state == 0) {
            cFile.erro(tokenGroup.start, "Expression expected", this); // tokenGroup.start never null
        } else if (state == 1) {
            cFile.erro(tokenGroup.start, "Expression expected", this);
        } else if (state == 2) {
            cFile.erro(tokenGroup.start, "Expression expected", this);
        } else {
            fieldView = typePtr.type.getField(fieldToken);
            if (fieldView == null) {
                cFile.erro(tokenGroup.start, "Field not found", this);
            } else if (fieldView.isStatic()) {
                cFile.erro(tokenGroup.start, "Static Field not allowed", this);
            } else {
                fieldView = new FieldView(typePtr, fieldView);
            }
        }
        initFields.add(fieldView);
        initArguments.add(expression);
    }

    @Override
    public void load(Context context) {
        if (typeToken == null) {
            context.jumpTo(null);
        } else {
            typePtr = context.getPointer(typeToken);

            boolean foundEmpty = false;
            for (Expression indexArgument : indexArguments) {
                if (indexArgument != null) {
                    indexArgument.load(new Context(context));
                    indexArgument.requestOwn(cFile.langIntPtr());
                    if (foundEmpty) {
                        cFile.erro(token, "Unexpected Array Init Argument", this);
                    }
                } else {
                    foundEmpty = true;
                }
            }
            for (int i = 0; i < indexArguments.size(); i++) {
                typePtr = cFile.langArrayPtr(typePtr);
            }

            if (indexArguments.size() > 0) {
                // [Init Block] -> array
                if (initToken != null) {
                    arrayInit = new Expression(getLine(), initToken, initToken.getNext(), typePtr);
                    arrayInit.load(new Context(context));
                    arrayInit.requestOwn(typePtr);
                }

                if (paramToken != null) {
                    for (Expression argument : arguments) {
                        argument.load(new Context(context));
                        argument.requestOwn(null);
                    }
                    cFile.erro(paramToken, "Constructor parameters not allowed for Array Init", this);
                }
            } else {
                // [Init Block] -> values
                if (initToken != null) {
                    readInitTokens(initToken.getChild(), initToken.getLastChild());
                    for (TokenGroup initGroup : initTokens) {
                        readInitField(typePtr, initGroup);
                    }
                }

                for (int i = 0; i < initArguments.size(); i++) {
                    Expression arg = initArguments.get(i);
                    FieldView fieldView = initFields.get(i);

                    arg.load(new Context(context));
                    if (fieldView != null) {
                        arg.requestOwn(fieldView.typePtr);

                        if (fieldView.hasSet()) {
                            if (!fieldView.isSetPublic() && !fieldView.isSetPrivate()) {
                                if (!getStack().cFile.library.equals(fieldView.getSetFile().library)) {
                                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                                }
                            } else if (fieldView.isSetPrivate()) {
                                if (!getStack().cFile.equals(fieldView.getSetFile())) {
                                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                                }
                            } else if (fieldView.isReadOnly(getStack())) {
                                cFile.erro(token, "Cannot SET a final variable", this);
                            }
                        }  else {
                            cFile.erro(token, "SET member not defined", this);
                        }
                    } else {
                        arg.requestOwn(null);
                    }
                }

                if (paramToken == null) {
                    cFile.erro(typeToken, "Constructor parameters expected", this);
                } else {
                    ArrayList<ConstructorView> constructors = context.findConstructor(typePtr, arguments);
                    if (constructors == null || constructors.size() == 0) {
                        cFile.erro(token, "Constructor Not Found", this);
                    } else if (constructors.size() > 1) {
                        cFile.erro(token, "Ambigous Constructor Call", this);
                        constructorView = constructors.get(0);
                    } else {
                        constructorView = constructors.get(0);
                        if (typePtr.typeSource != null) {
                            if (!constructorView.isDefault()) {
                                cFile.erro(token, "A Generic Constructor should be Default", this);
                            }
                        } else if (typePtr.type.isAbstract()) {
                            cFile.erro(token, "Cannot Instanteate a Abstract Type", this);
                        }
                    }
                }
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
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
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
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }
}
