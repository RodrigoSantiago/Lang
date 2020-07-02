package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Field;
import logic.stack.StackExpansion;

import java.util.ArrayList;

public class LambdaCall extends Call {

    StackExpansion innerStack;

    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<Pointer> typePointers = new ArrayList<>();

    ArrayList<TokenGroup> typeTokens = new ArrayList<>();
    ArrayList<Boolean> letPointers = new ArrayList<>();

    Token letToken;
    TokenGroup typeToken;
    Pointer typePtr;

    TokenGroup contentToken;
    Pointer functionPtr;

    boolean isAutomatic;
    boolean isNoTyped;

    public LambdaCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM && token.getChild() != null) {
                readParams(token.getChild(), token.getLastChild());
                state = 1;
            } else if (state == 1 && token.key == Key.LAMBDA) {
                state = 2;
            } else if (state == 2 && token.key == Key.LET) {
                letToken = token;
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 4;
            } else if ((state == 2 || state == 3) && token.key == Key.VOID) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                if (letToken != null) cFile.erro(letToken, "Unexpected token", this);
                state = 4;
            } else if ((state >= 2 && state <= 4) && token.key == Key.BRACE && token.getChild() != null) {
                contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                if (state == 3) {
                    cFile.erro(letToken, "Type expected", this);
                }
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    private void readParams(Token start, Token end) {
        Token letToken = null;
        Token nameToken = null;
        TokenGroup typeToken = null;

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 5) && token.key == Key.LET) {
                letToken = token;
                state = 1;
            } else if ((state == 0 || state == 1 || state == 5) && token.key == Key.WORD) {
                if (next != end && (next.key == Key.INDEX || next.key == Key.GENERIC)) {
                    typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                    state = 3;
                } else {
                    nameToken = token;
                    state = 2;
                }
            } else if (state == 2 && token.key == Key.WORD) {
                typeToken = new TokenGroup(nameToken, nameToken.getNext());
                nameToken = token;
                typeTokens.add(typeToken);
                nameTokens.add(nameToken);
                letPointers.add(letToken != null);
                typeToken = null;
                nameToken = null;
                letToken = null;

                state = 4;
            } else if (state == 2 && token.key == Key.COMMA) {
                typeTokens.add(null);
                nameTokens.add(nameToken);
                letPointers.add(letToken != null);
                typeToken = null;
                nameToken = null;
                if (letToken != null) cFile.erro(letToken, "Cannot use let in automatic pointer", this);
                letToken = null;

                state = 5; // [next]
            } else if (state == 3 && token.key == Key.WORD) {
                nameToken = token;
                typeTokens.add(typeToken);
                nameTokens.add(nameToken);
                letPointers.add(letToken != null);
                typeToken = null;
                nameToken = null;
                letToken = null;

                state = 4;
            } else if (state == 4 && token.key == Key.COMMA) {
                state = 5; // [next]
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end) {
                if (state == 2) {
                    typeTokens.add(null);
                    nameTokens.add(nameToken);
                    letPointers.add(letToken != null);
                    if (letToken != null) cFile.erro(letToken, "Cannot use let in automatic pointer", this);
                } else if (state != 4) {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        boolean isDefined = false;
        boolean isAuto = false;

        for (int i = 0; i < nameTokens.size(); i++) {
            if (typeTokens.get(i) == null) {
                isAuto = true;
            } else {
                isDefined = true;
            }
            if (isAuto && isDefined) {
                cFile.erro(nameTokens.get(i), "Cannot mix type inference", this);
            }
        }

        if (isAuto) {
            isAutomatic = true;
        } else {
            for (int i = 0; i < typeTokens.size(); i++) {
                Pointer p = context.getPointer(typeTokens.get(i));
                if (p == null) p = cFile.langObjectPtr();
                if (letPointers.get(i)) p = p.toLet();

                typePointers.add(p);
            }

            if (typeToken != null) {
                Pointer p;
                if (typeToken.start.key == Key.VOID) {
                    p = Pointer.voidPointer;
                } else {
                    p = context.getPointer(typeToken);
                    if (p == null) p = cFile.langObjectPtr();
                    if (letToken != null) p = p.toLet();
                }

                typePtr = p;
            } else {
                isNoTyped = true;
                typePtr = Pointer.voidPointer;
            }

            Pointer[] pointers = new Pointer[nameTokens.size() + 1];
            pointers[0] = typePtr;
            for (int i = 0; i < nameTokens.size(); i++) {
                pointers[i + 1] = typePointers.get(i);
            }
            functionPtr = cFile.langFunctionPtr(pointers);
        }
    }

    @Override
    public int verify(Pointer pointer) {
        if (pointer == null) { // [impossible]
            if (isAutomatic) return 0;
        } else if (pointer.type == cFile.langFunction()) {
            if (isAutomatic) {
                if (pointer.pointers != null && pointer.pointers.length == nameTokens.size() + 1) {
                    return 1;
                } else {
                    return 0;
                }
            } else if (isNoTyped) {
                if (pointer.pointers != null && pointer.pointers.length == nameTokens.size() + 1) {
                    boolean dif = false;
                    for (int i = 1; i < functionPtr.pointers.length; i++) {
                        if (!functionPtr.pointers[i].equals(pointer.pointers[i])) {
                            dif = true;
                            break;
                        }
                    }
                    return dif ? 0 : 1;
                } else {
                    return 0;
                }
            } else {
                pointer.canReceive(functionPtr);
            }
        } else if (pointer.equals(cFile.langFunction().parent)) {
            return isAutomatic ? 0 : 2;
        } else if (pointer.equals(cFile.langObjectPtr())) {
            return isAutomatic ? 0 : 3;
        }
        return 0;
    }

    @Override
    public Pointer getNaturalPtr(Pointer convertFlag) {
        if (isAutomatic && convertFlag != null && convertFlag.type == cFile.langFunction()) {
            if (convertFlag.pointers != null && convertFlag.pointers.length == nameTokens.size() + 1) {
                naturalPtr = convertFlag;
            } else {
                naturalPtr = null;
            }
        } else if (isNoTyped && convertFlag != null && convertFlag.type == cFile.langFunction()) {
            boolean dif = true;
            if (convertFlag.pointers != null && convertFlag.pointers.length == nameTokens.size() + 1) {
                dif = false;
                for (int i = 1; i < functionPtr.pointers.length; i++) {
                    if (!functionPtr.pointers[i].equals(convertFlag.pointers[i])) {
                        dif = true;
                        break;
                    }
                }
            }
            if (dif) {
                naturalPtr = functionPtr;
            } else {
                naturalPtr = convertFlag;
            }
        } else {
            naturalPtr = functionPtr;
        }
        return naturalPtr;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) {
            cFile.erro(getToken(), "Automatic inference not found", this);
            return;
        }
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }

        if (contentToken != null) {
            innerStack = new StackExpansion(getStack(), token, typePtr);
            innerStack.read(contentToken.start, contentToken.end, true);
            for (int i = 0; i < nameTokens.size(); i++) {
                innerStack.addParam(nameTokens.get(i), naturalPtr.pointers[i + 1], false);
            }
            innerStack.load();
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) {
            cFile.erro(getToken(), "Automatic inference not found", this);
            return;
        }
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }

        if (contentToken != null) {
            innerStack = new StackExpansion(getStack(), token, naturalPtr.pointers[0]);
            innerStack.read(contentToken.start, contentToken.end, true);
            for (int i = 0; i < nameTokens.size(); i++) {
                innerStack.addParam(nameTokens.get(i), naturalPtr.pointers[i + 1], false);
            }
            innerStack.load();
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }

    @Override
    public void build(CppBuilder cBuilder, int idt) {
        if (innerStack.shadowFields.size() > 0) {
            cBuilder.add("(");
            for (Field shadow : innerStack.shadowFields.values()) {
                shadow.buildParam(cBuilder);
                cBuilder.add(", ");
            }
        }
        cBuilder.add("[=](");
        for (int i = 0; i < nameTokens.size(); i++) {
            if (i > 0) cBuilder.add(", ");
            cBuilder.add(naturalPtr.pointers[i + 1]).add(" ").nameParam(nameTokens.get(i));
        }
        cBuilder.add(") mutable -> ").add(naturalPtr.pointers[0]).add(" ").in(idt + 1);
        innerStack.build(cBuilder, idt + 1);
        cBuilder.out();
        if (innerStack.shadowFields.size() > 0) {
            cBuilder.add(")");
        }
    }
}
