package logic.member;

import content.Key;
import content.Token;
import builder.CppBuilder;
import data.ContentFile;
import logic.GenericOwner;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.LambdaResolve;
import logic.stack.Stack;
import logic.stack.expression.LambdaCall;
import logic.typdef.Type;

import java.util.ArrayList;

public class Num extends Member {

    private ArrayList<Token> nameTokens = new ArrayList<>();
    private ArrayList<Token> initTokens = new ArrayList<>();
    private ArrayList<Stack> initStacks = new ArrayList<>();
    private ArrayList<FieldView> fields;

    public Num(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                nameTokens.add(token);
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                initTokens.add(null);
                state = 0;
            } else if (state == 1 && token.key == Key.SEMICOLON) {
                initTokens.add(null);
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                initTokens.add(token);
                state = 2;
            } else if (state == 2 && token.key == Key.COMMA) {
                state = 0;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        isStatic = true;
        isSync = true;
    }

    @Override
    public boolean load() {
        return nameTokens.size() > 0;
    }

    @Override
    public void make() {
        for (Token token : initTokens) {
            if (token != null) {
                Stack stack = new Stack(cFile, token, type.self, Pointer.voidPointer, null,
                        true, true, false, null, null);
                stack.readEnum(token.getChild(), token.getLastChild());
                stack.load();
                initStacks.add(stack);
            } else {
                initStacks.add(null);
            }
        }
    }

    @Override
    public void build(CppBuilder cBuilder) {
        cBuilder.toHeader();
        for (int i = 0; i < nameTokens.size(); i++) {
            Token name = nameTokens.get(i);
            cBuilder.idt(1).add("static ").add(type.self).add(" ").nameField(name).add(";").ln();
            if (initStacks.get(i) != null) {
                cBuilder.idt(1).add("static ").add(type.self).add("& s_").add(name).add("();").ln();
            }
        }

        for (int i = 0; i < nameTokens.size(); i++) {
            Token name = nameTokens.get(i);
            cBuilder.toSource();
            cBuilder.add(type.self).add(" ").path(type.self, true).add("::")
                    .nameField(name).add(" = ").add(type.self).add("(empty{}, ").add(i).add(");").ln();
            cBuilder.ln();

            if (initStacks.get(i) != null) {
                cBuilder.add(type.self).add("& ")
                        .path(type.self, true).add("::s_").add(name).add("() ").in(1)
                        .idt(1).add("syncInit();").ln()
                        .idt(1).add("return ").nameField(name).add(";").ln()
                        .out().ln()
                        .ln();
            }
        }
    }

    public void buildInit(CppBuilder cBuilder) {
        for (int i = 0; i < nameTokens.size(); i++) {
            Token name = nameTokens.get(i);
            if (initStacks.get(i) != null) {
                cBuilder.idt(1).add("new (&").nameField(name).add(") ");
                if (initStacks.get(i).enumConstructor.isEmpty()) {
                    cBuilder.path(type.self).add("(empty{})").ln();
                } else if (initStacks.get(i).enumConstructor.isCopy()) {
                    cBuilder.path(type.self).add("(empty{}, ").add(initStacks.get(i).expressions, 1).add(")");
                } else {
                    cBuilder.path(type.self).add("(").add(initStacks.get(i).expressions, 1).add(")");
                }
                cBuilder.add(";").ln();
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

    public int getCount() {
        return nameTokens.size();
    }

    public Token getName(int pos) {
        return nameTokens.get(pos);
    }

    public ArrayList<FieldView> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
            for (int i = 0; i < nameTokens.size(); i++) {
                Token nameToken = nameTokens.get(i);
                fields.add(new FieldView(nameToken, type.self, this, i));
            }
        }
        return fields;
    }

    public boolean isInitialized(int srcID) {
        return initStacks.get(srcID) != null;
    }
}
