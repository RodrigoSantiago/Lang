package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.LambdaResolve;
import logic.stack.Stack;
import logic.stack.expression.LambdaCall;
import logic.typdef.Type;

import java.util.ArrayList;

public class Variable extends Member {

    private TokenGroup typeToken;
    private Pointer typePtr;

    private ArrayList<Token> nameTokens = new ArrayList<>();
    private ArrayList<TokenGroup> initTokens = new ArrayList<>();
    private ArrayList<FieldView> fields;

    private ArrayList<Stack> initStacks = new ArrayList<>();

    public Variable(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                this.token = token;
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                nameTokens.add(token);
                state = 2;
            } else if (state == 2 && token.key == Key.SETVAL) {
                Token initStart = next;
                while (next != end && (next.key != Key.COMMA && next.key != Key.SEMICOLON)) {
                    next = next.getNext();
                }
                if (initStart == next) {
                    cFile.erro(token, "Expression expected", this);
                } else {
                    initTokens.add(new TokenGroup(initStart, next));
                }

                if (next != end && next.key == Key.COMMA) {
                    next = next.getNext();
                    state = 1;
                } else if (next != end && next.key == Key.SEMICOLON) {
                    next = next.getNext();
                    state = 3;
                }
            } else if (state == 2 && token.key == Key.COMMA) {
                initTokens.add(null);
                state = 1;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                initTokens.add(null);
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, isStatic() ? null : type, isLet());
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet());
                return false;
            }
            if (isSync() && !typePtr.type.isSync()) {
                cFile.erro(typeToken, "A Sync variable must be a Sync Type", this);
                return false;
            }

            return nameTokens.size() > 0 ;
        }
        return false;
    }

    public void make() {
        for (TokenGroup initToken : initTokens) {
            if (initToken != null && initToken.start != null && initToken.start != initToken.end) {
                Stack stack = new Stack(cFile, token, type.self, typePtr, isStatic() ? null : type,
                        true, isStatic(), false, null, null);
                stack.read(initToken.start, initToken.end);
                stack.load();
                initStacks.add(stack);
            } else {
                initStacks.add(null);
            }
        }
    }

    public void build(CppBuilder cBuilder) {
        if (typePtr.type != null && typePtr.typeSource == null && typePtr.type.isStruct()) {
            cBuilder.dependenceAdd(typePtr);
        }

        cBuilder.toHeader();
        for (int i = 0; i < nameTokens.size(); i++) {
            Token name = nameTokens.get(i);
            cBuilder.idt(1);
            if (isStatic()) {
                cBuilder.add("static ").add(!isSync(), "thread_local ");
            }
            cBuilder.add(typePtr).add(" ").nameField(name).add(";").ln();

            if (isStatic() && !isLiteral(i)) {
                cBuilder.idt(1).add("static ").add(typePtr).add("& s_").add(name).add("();").ln();
            }
        }

        if (isStatic()) {
            for (int i = 0; i < nameTokens.size(); i++) {
                Token name = nameTokens.get(i);
                cBuilder.toSource();
                cBuilder.add(isStatic() && !isSync(), "thread_local ").add(typePtr)
                        .add(" ").path(type.self, isStatic()).add("::").nameField(name).add(" = ");
                if (isLiteral(i)) {
                    initStacks.get(i).build(cBuilder, 1);
                    cBuilder.add(";").ln();
                } else if (typePtr.typeSource != null) {
                    cBuilder.add("lang::value<").add(typePtr).add(">::def();").ln();
                } else if (typePtr.type != null && (typePtr.type.isPointer() || typePtr.type.isFunction())) {
                    cBuilder.add("nullptr;").ln();
                } else if (typePtr.type != null && typePtr.type.isValue() && !typePtr.type.isLangBase()) {
                    cBuilder.add(typePtr).add("();").ln();
                } else {
                    cBuilder.add("0;").ln();
                }
                cBuilder.ln();

                if (!isLiteral(i)) {
                    cBuilder.add(typePtr).add("& ")
                            .path(type.self, isStatic()).add("::s_").add(name).add("() ").in(1)
                            .idt(1).add(isSync() ? "syncInit();" : "init();").ln()
                            .idt(1).add("return ").nameField(name).add(";").ln()
                            .out().ln()
                            .ln();
                }
            }
        }
    }

    // Call on Creation
    public void buildDefault(CppBuilder cBuilder) {
        for (int i = 0; i < nameTokens.size(); i++) {
            Token name = nameTokens.get(i);
            if (i > 0) {
                cBuilder.add(", ").ln();
            }

            cBuilder.idt(1).nameField(name).add("(");
            if (isLiteral(i)) {
                initStacks.get(i).build(cBuilder, 1);
            } else if (typePtr.typeSource != null) {
                cBuilder.add("lang::value<").add(typePtr).add(">::def()");
            } else if (typePtr.type != null && (typePtr.type.isPointer() || typePtr.type.isFunction())) {
                cBuilder.add("nullptr");
            } else if (typePtr.type != null && typePtr.type.isValue() && !typePtr.type.isLangBase()) {
                cBuilder.add(typePtr).add("()");
            } else {
                cBuilder.add("0");
            }
            cBuilder.add(")");
        }
    }

    // Call on Constructors
    public void buildInit(CppBuilder cBuilder) {
        if (isStatic()) {
            for (int i = 0; i < nameTokens.size(); i++) {
                Token name = nameTokens.get(i);

                if (isInitialized(i) && !isLiteral(i)) {
                    cBuilder.idt(1).nameField(name).add(" = ").add(initStacks.get(i), 1).add(";").ln();
                }
            }
        } else {
            for (int i = 0; i < nameTokens.size(); i++) {
                Token name = nameTokens.get(i);

                if (isInitialized(i) && !isLiteral(i)) {
                    cBuilder.idt(1).add("this->").nameField(name).add(" = ").add(initStacks.get(i), 1).add(";").ln();
                }
            }
        }
    }

    public void buildLambdas(CppBuilder cBuilder) {
        for (Stack initStack : initStacks) {
            if (initStack != null) {
                for (LambdaCall lambdaCall : initStack.getLambdas()) {
                    LambdaResolve.build(cBuilder, 1, lambdaCall);
                }
            }
        }
    }

    public ArrayList<FieldView> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
            for (int i = 0; i < nameTokens.size(); i++) {
                fields.add(new FieldView(nameTokens.get(i), typePtr, this, i));
            }
        }
        return fields;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public Token getName(int pos) {
        return nameTokens.get(pos);
    }

    public TokenGroup getTypeToken() {
        return typeToken;
    }

    public int getCount() {
        return nameTokens.size();
    }

    public boolean isLiteral(int pos) {
        return initTokens.get(pos) != null && initTokens.get(pos).isLiteral();
    }

    public boolean isConstant(int pos) {
        return isFinal() && initTokens.get(pos) != null && initTokens.get(pos).isLiteral();
    }

    public boolean isInitialized(int pos) {
        return initTokens.get(pos) != null;
    }
}
