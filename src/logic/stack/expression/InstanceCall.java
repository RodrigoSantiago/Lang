package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import builder.Temp;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.view.ConstructorView;
import logic.member.view.FieldView;
import logic.stack.Context;

import java.util.ArrayList;

public class InstanceCall extends Call {

    TokenGroup typeToken;
    TokenGroup paramToken;
    Token initToken;

    ConstructorView constructorView;
    ArrayList<Expression> indexArguments = new ArrayList<>();
    ArrayList<Expression> arguments = new ArrayList<>();
    ArrayList<Expression>  arrayInitArgs = new ArrayList<>();

    ArrayList<TokenGroup> initTokens = new ArrayList<>();
    ArrayList<FieldView> initFields = new ArrayList<>();
    ArrayList<Expression> initArguments = new ArrayList<>();

    Pointer typePtr;
    private int indexArgs;
    private boolean isNullRequest;

    public InstanceCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

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
                    indexArgs++;
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
                    readInitTokens(initToken.getChild(), initToken.getLastChild());
                    for (TokenGroup initArg : initTokens) {
                        Expression arg = new Expression(getLine(), initArg.start, initArg.end, typePtr);
                        arg.load(new Context(context));
                        arg.requestOwn(typePtr.pointers[0]);
                        arrayInitArgs.add(arg);
                    }

                    if (indexArgs > 0) {
                        cFile.erro(initToken, "Indexed parameters not allowed with Array Init", this);
                    }
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
                        arg.requestOwn(fieldView.getTypePtr());

                        if (fieldView.hasSet()) {
                            if (!fieldView.isSetPublic() && !fieldView.isSetPrivate()) {
                                if (!getStack().cFile.library.equals(fieldView.getSetFile().library)) {
                                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                                }
                            } else if (fieldView.isSetPrivate()) {
                                if (!getStack().cFile.equals(fieldView.getSetFile())) {
                                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                                }
                            } else if (fieldView.isVariable() && fieldView.srcVar.isFinal()) {
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
                            if (constructorView == ConstructorView.structInit || !constructorView.isDefault()) {
                                cFile.erro(token, "A Generic Constructor should be Default", this);
                            }
                        } else if (typePtr.type.isAbstract()) {
                            cFile.erro(token, "Cannot Instanteate a Abstract Type", this);
                        }
                    }
                }
            }
            if (typePtr == null) {
                context.jumpTo(null);
            }
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
        if (pointer == null) {
            pointer = naturalPtr;
        }
        pointer = pointer.toLet();
        isNullRequest = true;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }

        if (constructorView != null && constructorView != ConstructorView.structInit) {
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
        if (pointer == null) {
            isNullRequest = true;
            pointer = naturalPtr;
        }

        requestPtr = pointer;
        if (requestPtr.let) isNullRequest = true;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }

        if (constructorView != null && constructorView != ConstructorView.structInit) {
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

    @Override
    public void build(CppBuilder cBuilder, int idt, boolean next) {
        if (typePtr.isPointer() || isPathLine || isNullRequest) {
            cBuilder.add(typePtr).add("(");
        }

        if (typePtr.isPointer()) {
            if (indexArgs > 0) {
                if (indexArgs == 1) {
                    cBuilder.add("(new ").path(typePtr).add("())->create(").add(indexArguments.get(0), idt).add(")");
                } else {
                    Temp t = cBuilder.temp(cFile.langIntPtr(), indexArguments.size());
                    cBuilder.add("array<").path(typePtr).add(">::fill(").add("new ").path(typePtr).add("(), (");
                    for (int i = 0; i < indexArgs; i++) {
                        if (i > 0) cBuilder.add(", ");
                        cBuilder.add(t).add("[").add(i).add("] = ").add(indexArguments.get(i), idt);
                    }
                    if (indexArgs < indexArguments.size()) {
                        cBuilder.add(", ").add(t).add("[").add(indexArgs).add("] = 0");
                    }
                    cBuilder.add(", ").add(t).add("), 0)");
                }
            } else if (arrayInitArgs.size() > 0) {
                Temp t = cBuilder.temp(typePtr, true);
                Temp t2 = cBuilder.temp(typePtr.pointers[0], true);
                cBuilder.add("(").add(t).add(" = ")
                        .add("(new ").path(typePtr).add("())->create(").add(arrayInitArgs.size()).add(")");
                cBuilder.add(", ").add(t2).add(" = ").add(t).add("->data");
                for (int i = 0; i < arrayInitArgs.size(); i++) {
                    Expression arg = arrayInitArgs.get(i);
                    cBuilder.add(", ").add(t2).add("[").add(i).add("] = ").add(arg, idt);
                }
                cBuilder.add(", ").add(t).add(")");
            } else if (initFields.size() > 0) {
                Temp t = cBuilder.temp(typePtr, true);
                cBuilder.add("(").add(t).add(" = ")
                        .add("(new ").path(typePtr).add("())->create(").add(arguments, idt).add(")");
                for (int i = 0; i < initFields.size(); i++) {
                    FieldView field = initFields.get(i);
                    cBuilder.add(", ").add(t).add("->");
                    if (field.isProperty()) {
                        cBuilder.nameSet(field.getName()).add("(").add(initArguments.get(i), idt).add(")");
                    } else {
                        cBuilder.nameField(field.getName()).add(" = ").add(initArguments.get(i), idt);
                    }
                }
                cBuilder.add(", ").add(t).add(")");
            } else {
                cBuilder.add("(new ").path(typePtr).add("())->create(").add(arguments, idt).add(")");
            }
        } else {
            if (initFields.size() > 0) {
                Temp t = cBuilder.temp(typePtr);
                cBuilder.add("(").add(t).add(" = ")
                        .path(typePtr).add("(");

                if (constructorView == ConstructorView.structInit) {
                    cBuilder.add(")");
                } else if (constructorView.isEmpty()) {
                    cBuilder.add("empty()").add(")");
                } else if (constructorView.isCopy()) {
                    cBuilder.add("empty(), ").add(arguments, idt).add(")");
                } else {
                    cBuilder.add(arguments, idt).add(")");
                }

                for (int i = 0; i < initFields.size(); i++) {
                    FieldView field = initFields.get(i);
                    cBuilder.add(", ").add(t).add(".");
                    if (field.isProperty()) {
                        cBuilder.nameSet(field.getName()).add("(").add(initArguments.get(i), idt).add(")");
                    } else {
                        cBuilder.nameField(field.getName()).add(" = ").add(initArguments.get(i), idt);
                    }
                }
                cBuilder.add(", ").add(t).add(")");
            } else {
                cBuilder.path(typePtr).add("(");
                if (constructorView == ConstructorView.structInit) {
                    cBuilder.add(")");
                } else if (constructorView.isEmpty()) {
                    cBuilder.add("empty()").add(")");
                } else if (constructorView.isCopy()) {
                    cBuilder.add("empty(), ").add(arguments, idt).add(")");
                } else {
                    cBuilder.add(arguments, idt).add(")");
                }
            }
        }
        if (typePtr.isPointer() || isPathLine || isNullRequest) {
            cBuilder.add(")");
        }
        if (next) {
            cBuilder.add(requestPtr.isPointer() ? "->" : ".");
        }
    }
}
