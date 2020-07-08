package logic.stack.expression;

import content.Key;
import content.Token;
import builder.CppBuilder;
import data.Error;
import logic.Pointer;
import logic.stack.Context;

public class LiteralCall extends Call {
    public static int LONG = 1;
    public static int DOUBLE = 2;
    public static int STRING = 3;
    public static int BOOL = 4;
    public static int NULL = 5;
    public static int DEFAULT = 6;

    Pointer typePtr;

    public String resultStr;
    public long resultNum = 0;
    public double resultDouble = 0;
    public boolean resultBool = false;
    public boolean isLong, isFloat, isDouble;
    public Error longLimit;

    private int literalType;

    public LiteralCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NUMBER) {
                readNumner(token);
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.STRING) {
                typePtr = cFile.langStringPtr();
                this.token = token;
                literalType = STRING;
                resultStr = token.toString(1, token.length - 1);
                state = 1;
            } else if (state == 0 && (token.key == Key.TRUE || token.key == Key.FALSE)) {
                typePtr = cFile.langBoolPtr();
                this.token = token;
                literalType = BOOL;
                resultBool = token.key == Key.TRUE;
                resultStr = token.toString();
                state = 1;
            } else if (state == 0 && token.key == Key.NULL) {
                typePtr = Pointer.nullPointer;
                this.token = token;
                literalType = NULL;
                resultStr = "null";
                state = 1;
            } else if (state == 0 && token.key == Key.DEFAULT) {
                typePtr = Pointer.openPointer;
                this.token = token;
                literalType = DEFAULT;
                resultStr = "";
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

    LiteralCall(CallGroup group, Token start, Token end, int type,
                boolean resultBool, long resultNum, double resultDouble, String resultStr,
                boolean isLong, boolean isFloat, boolean isDouble) {
        super(group, start, end);
        this.token = start;
        if (end != null && (start.getSource() == end.getSource())) {
            token = new Token(start.getSource(), start.start, end.start, Key.NOONE, false);
        }
        this.literalType = type;
        this.resultBool = resultBool;
        this.resultNum = resultNum;
        this.resultDouble = resultDouble;
        this.resultStr = resultStr;
        this.isLong = isLong;
        this.isFloat = isFloat;
        this.isDouble = isDouble;
    }

    public void readNumner(Token tokenNumber) {
        StringBuilder builder = new StringBuilder();
        // TODO - ADD HEXADECIAL + BINARY + OCTAL

        int start = 0;
        int end = tokenNumber.length;
        int state = 0;
        boolean incorrect = false, dot = false, exp = false;
        for (int i = start; i < end; i++) {
            char chr = tokenNumber.at(i);
            if (state == 0 && isNumber(chr)) {
                builder.append(chr);
                state = 1;
            } else if (state == 1 && isNumber(chr)) {
                builder.append(chr);
            } else if (state == 1 && chr == '.') {
                builder.append(chr);
                dot = true;
                state = 2;
            } else if ((state == 2 || state == 3) && isNumber(chr)) {
                builder.append(chr);
                state = 3;
            } else if ((state == 1 || state == 3) && (chr == 'e' || chr == 'E')) {
                builder.append(chr);
                exp = true;
                state = 4;
            } else if (state == 4 && chr == '-') {
                builder.append(chr);
                state = 5;
            } else if ((state == 4 || state == 5 || state == 6) && isNumber(chr)) {
                builder.append(chr);
                state = 6 ;
            } else if (state == 1 && (chr == 'l' || chr == 'L')) {
                isLong = true;
                state = 7;
            } else if ((state == 1 || state == 3 || state == 6) && (chr == 'f' || chr == 'F')) {
                isFloat = true;
                state = 7;
            } else if ((state == 1 || state == 3 || state == 6) && (chr == 'd' || chr == 'D')) {
                isDouble = true;
                state = 7;
            } else {
                incorrect = true;
            }
            if (i + 1 == end) {
                if (state == 0 || state == 2 || state == 4 || state == 5) {
                    incorrect = true;
                    if (state == 0) builder.append("0");
                    if (state == 2 || state == 4) builder.setLength(builder.length() -1);
                    if (state == 5) builder.setLength(builder.length() - 2);
                }
            }
        }

        literalType = isFloat || isDouble || dot || exp ? DOUBLE : LONG;

        if (incorrect) {
            cFile.erro(token, "Malformated number", this);
        }
        resultStr = builder.toString();
        if (!incorrect) {
            try {
                resultNum = Long.parseLong(resultStr);
                resultDouble = resultNum;
            } catch (Exception e) {
                try {
                    resultDouble = Double.parseDouble(resultStr);
                    literalType = DOUBLE;
                } catch (Exception e2) {
                    cFile.erro(token, "Malformated number", this);
                }
            }
        }
    }

    private boolean isNumber(int chr) {
        return chr >= '0' && chr <= '9';
    }

    private Pointer getRelativePointer() {
        return literalType == NULL ? Pointer.nullPointer
                : literalType == BOOL ? cFile.langBoolPtr()
                : literalType == LONG && (isLong || resultNum >= 2147483648L || resultNum < -2147483648L) ? cFile.langLongPtr()
                : literalType == LONG ? cFile.langIntPtr()
                : literalType == DOUBLE && isFloat ? cFile.langFloatPtr()
                : literalType == DOUBLE ? cFile.langDoublePtr()
                : literalType == STRING ? cFile.langStringPtr() : Pointer.openPointer;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public int getLiteralType() {
        return literalType;
    }

    @Override
    public void load(Context context) {

    }

    @Override
    public int verify(Pointer pointer) {
        if (pointer == Pointer.voidPointer) return 0;
        if (pointer == Pointer.nullPointer) return literalType == NULL ? 1 : 0;
        if (pointer.isOpen()) return literalType == DEFAULT ? 1 : -1; // [Negative]

        if (literalType == DEFAULT) return 1;

        if (pointer.type.isPointer()) {
            if (literalType == NULL || literalType == DEFAULT) {
                return 1;
            } else {
                return pointer.canReceive(getRelativePointer());
            }
        } else if (pointer.type == cFile.langBool()) {
            return literalType == BOOL ? 1 : 0;
        } else if (pointer.type == cFile.langString()) {
            return literalType == STRING ? 1 : 0;
        } else if (pointer.type == cFile.langByte()) {
            return literalType == LONG && resultNum < 128 && resultNum >= -128 && !isLong ? 6 : 0;
        } else if (pointer.type == cFile.langShort()) {
            return literalType == LONG && resultNum < 32768 && resultNum >= -32768 && !isLong ? 5 : 0;
        } else if (pointer.type == cFile.langInt()) {
            return literalType == LONG && resultNum < 2147483648L && resultNum >= -2147483648L && !isLong ? 1 : 0;
        } else if (pointer.type == cFile.langLong()) {
            return literalType == LONG ? isLong ? 1 : 2 : 0;
        } else if (pointer.type == cFile.langFloat()) {
            return literalType == DOUBLE && isFloat ? 1 :
                    literalType == DOUBLE && !isDouble ? 2 : literalType == LONG ? 3 : 0;
        } else if (pointer.type == cFile.langDouble()) {
            return literalType == DOUBLE && !isFloat ? 1 :
                    literalType == DOUBLE && isFloat ? 2 : literalType == LONG ? 4 : 0;
        } else {
            return 0;
        }
    }

    @Override
    public Pointer getNaturalPtr(Pointer pointer) {
        if (pointer == null) {
            naturalPtr = getRelativePointer();
        } else if (literalType == NULL) {
            naturalPtr = Pointer.nullPointer;
        } else if (literalType == DEFAULT) {
            naturalPtr = pointer;
        } else if (pointer.type != null && pointer.type.isPointer()) {
            naturalPtr = getRelativePointer();
        } else if (literalType == BOOL) {
            naturalPtr = cFile.langBoolPtr();
        } else if (literalType == STRING) {
            naturalPtr = cFile.langStringPtr();
        } else if (pointer.type == cFile.langByte() && literalType == LONG &&
                resultNum < 128 && resultNum >= -128 && !isLong) {
            naturalPtr = cFile.langBytePtr();
        } else if (pointer.type == cFile.langShort() && literalType == LONG &&
                resultNum < 32768 && resultNum >= -32768 && !isLong) {
            naturalPtr = cFile.langShortPtr();
        } else if (literalType == LONG &&
                resultNum < 2147483648L && resultNum >= -2147483648L && !isLong) {
            naturalPtr = cFile.langIntPtr();
        } else if (literalType == LONG) {
            naturalPtr = cFile.langLongPtr();
        } else if (pointer.type == cFile.langFloat() && literalType == DOUBLE &&
                (isFloat || !isDouble)) {
            naturalPtr = cFile.langFloatPtr();
        } else if (literalType == DOUBLE) {
            naturalPtr = cFile.langDoublePtr();
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
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (naturalPtr != pointer && pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }

    @Override
    public String toString() {
        return "LiteralCall{" + resultNum + ", " + resultDouble + "}";
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, boolean next) {
        if (literalType == NULL) {
            cBuilder.add("nullptr");
        } else if (literalType == DEFAULT) {
            cBuilder.add("lang::value<GPtr<").add(requestPtr).add(">>::def()");
        } else if (literalType == BOOL) {
            cBuilder.add(resultBool ? "true" : "false");
        } else if (literalType == STRING) {
            if (isPathLine) {
                cBuilder.path(cFile.langStringPtr()).add("(");
            }
            cBuilder.dependence(cFile.langStringPtr());
            cBuilder.add("\"").add(resultStr).add("\"");
            if (isPathLine) cBuilder.add(")");

        } else if (literalType == LONG) {
            if (naturalPtr.equals(cFile.langLongPtr())) {
                cBuilder.add(resultNum).add("l");
            } else if (naturalPtr.equals(cFile.langIntPtr())) {
                if (isArg) cBuilder.add("(").add(cFile.langIntPtr()).add(")");
                cBuilder.add(resultNum);
            } else if (naturalPtr.equals(cFile.langBytePtr())) {
                if (isArg) cBuilder.add("(").add(cFile.langBytePtr()).add(")");
                cBuilder.add(resultNum);
            } else if (naturalPtr.equals(cFile.langShortPtr())) {
                if (isArg) cBuilder.add("(").add(cFile.langShortPtr()).add(")");
                cBuilder.add(resultNum);
            } else if (naturalPtr.equals(cFile.langFloatPtr())) {
                cBuilder.add(resultNum).add(".0f");
            } else if (naturalPtr.equals(cFile.langDoublePtr())) {
                cBuilder.add(resultNum).add(".0");
            } else {
                cBuilder.add(resultNum);
            }
        } else if (literalType == DOUBLE) {
            if (naturalPtr.equals(cFile.langFloatPtr())) {
                cBuilder.add(resultDouble).add("f");
            } else if (naturalPtr.equals(cFile.langDoublePtr())) {
                cBuilder.add(resultDouble);
            } else {
                cBuilder.add(resultDouble);
            }
        }
        if (next) {
            cBuilder.add(requestPtr.isPointer() ? "->" : ".");
        }
    }

    public boolean compareTo(LiteralCall other) {
        return (literalType == other.literalType &&
                ((literalType == LONG && resultNum == other.resultNum) ||
                        (literalType == STRING && resultStr != null && resultStr.equals(other.resultStr))));
    }
}
