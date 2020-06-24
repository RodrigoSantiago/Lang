package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
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

        System.out.println("LITERAL : "+ TokenGroup.toString(start, end));
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
            token = new Token(start.getSource(), start.start, TokenGroup.lastToken(start, end).end, Key.NOONE, false);
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
        context.jumpTo(typePtr);
    }

    @Override
    public int verify(Pointer pointer) {
        if (pointer == Pointer.voidPointer) return 0;
        if (pointer == Pointer.nullPointer) return literalType == NULL ? 1 : 0;
        if (pointer.isOpen()) return literalType == DEFAULT ? 1 : -1; // [Negative]

        if (literalType == DEFAULT) return 1;

        if (pointer.type == cFile.langBool()) return literalType == BOOL ? 1 : 0;
        if (pointer.type == cFile.langString()) return literalType == STRING ? 1 : 0;

        if (pointer.type == cFile.langByte()) {
            return literalType == LONG && resultNum < 128 && resultNum >= -128 && !isLong ? 1 : 0;
        } else if (pointer.type == cFile.langShort()) {
            return literalType == LONG && resultNum < 32768 && resultNum >= -32768 && !isLong ? 1 : 0;
        } else if (pointer.type == cFile.langInt()) {
            return literalType == LONG && resultNum < 2147483648L && resultNum >= -2147483648L && !isLong ? 1 : 0;
        } else if (pointer.type == cFile.langLong()) {
            return literalType == LONG ? 1 : 0;
        } else if (pointer.type == cFile.langFloat()) {
            return literalType == DOUBLE && !isDouble ? 1 : literalType == LONG ? 2 : 0;
        } else if (pointer.type == cFile.langDouble()) {
            return literalType == DOUBLE && !isFloat ? 1 : literalType == DOUBLE || literalType == LONG ? 2 : 0;
        } else if (pointer.type.isPointer()) {
            return literalType == NULL ||  literalType == DEFAULT ? 1 : 0;
        }
        return 0;
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (pointer == null) {
            returnPtr = literalType == NULL ? Pointer.nullPointer
                    : literalType == DEFAULT ? Pointer.openPointer
                    : literalType == BOOL ? cFile.langBoolPtr()
                    : literalType == LONG ? cFile.langIntPtr()
                    : literalType == DOUBLE ? cFile.langDoublePtr()
                    : literalType == STRING ? cFile.langStringPtr() : cFile.langObjectPtr();
        } else if (verify(pointer) > 0) {
            returnPtr = pointer;
        }
        return returnPtr;
    }

    @Override
    public boolean requestSet(Pointer pointer) {
        request(pointer);
        return false;
    }

    @Override
    public String toString() {
        return "LiteralCall{" + resultNum + ", " + resultDouble + "}";
    }
}
