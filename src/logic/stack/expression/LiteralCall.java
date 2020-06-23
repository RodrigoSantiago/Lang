package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Context;

public class LiteralCall extends Call {

    Pointer typePtr;

    private boolean isByte, isShort, isInt, isLong, isFloat, isDouble, isString, isBool, isNull, isDefault;

    public LiteralCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("LITERAL : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.STRING) {
                typePtr = cFile.langStringPtr();
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NUMBER) {
                readNumner(token);
                this.token = token;
                state = 1;
            } else if (state == 0 && (token.key == Key.TRUE || token.key == Key.FALSE)) {
                typePtr = cFile.langBoolPtr();
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NULL) {
                typePtr = Pointer.nullPointer;
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.DEFAULT) {
                typePtr = Pointer.openPointer;
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

    public void readNumner(Token tokenNumber) {
        StringBuilder builder = new StringBuilder();
        // TODO - ADD HEXADECIAL + BINARY + OCTAL

        int start = 0;
        int end = tokenNumber.length;
        int state = 0;
        boolean incorrect = false, dot = false, exp = false, endL = false, endF = false, endD = false;
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
                endL = true;
                state = 7;
            } else if ((state == 1 || state == 3 || state == 6) && (chr == 'f' || chr == 'F')) {
                endF = true;
                state = 7;
            } else if ((state == 1 || state == 3 || state == 6) && (chr == 'd' || chr == 'D')) {
                endD = true;
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
        if (incorrect) {
            cFile.erro(token, "Malformated number", this);
        }
        String value = builder.toString();
        if (endL) {
            isLong = true;
            typePtr = cFile.langLongPtr();
        } else if (endF) {
            isFloat = true;
            typePtr = cFile.langFloatPtr();
        } else if (endD) {
            isDouble = true;
            typePtr = cFile.langDoublePtr();
        } else if (dot || exp) {
            isFloat = true;
            isDouble = true;
            typePtr = cFile.langDoublePtr();
        } else {
            long val = 0;
            if (!incorrect) {
                try {
                    val = Long.parseLong(value);
                } catch (Exception e) {
                    cFile.erro(token, "Malformated number", this);
                    e.printStackTrace();
                }
            }
            isByte = val <= 127;
            isShort = val <= 32767;
            isInt = val <= 2147483647;
            isLong = true;
            isFloat = true;
            isDouble = true;
            typePtr = isInt ? cFile.langIntPtr() : cFile.langLongPtr();
        }
        if (!incorrect && !isLong && (isFloat || isDouble)) {
            try {
                Double.parseDouble(value);
            } catch (Exception e) {
                cFile.erro(token, "Malformated number", this);
                e.printStackTrace();
            }
        }
    }

    private boolean isNumber(int chr) {
        return chr >= '0' && chr <= '0';
    }

    @Override
    public void load(Context context) {
        context.jumpTo(typePtr);
    }

    @Override
    public int verify(Pointer pointer) {
        if (pointer == Pointer.voidPointer) return 0;
        if (pointer == Pointer.nullPointer) return isNull ? 1 : 0;
        if (pointer.isOpen()) return isDefault ? 1 : -1; // [Negative]

        if (pointer.type == cFile.langBool()) return isBool ? 1 : 0;
        else if (pointer.type == cFile.langString()) return isString ? 1 : 0;

        boolean integer = (isLong || isInt || isShort || isByte);
        boolean real = (isDouble || isFloat) || integer;

        if (pointer.type == cFile.langByte()) {
            return isByte ? 1 : 0;
        } else if (pointer.type == cFile.langShort()) {
            return isShort || isByte ? 1 : 0;
        } else if (pointer.type == cFile.langInt()) {
            return isInt || isShort || isByte ? 1 : 0;
        } else if (pointer.type == cFile.langLong()) {
            return (isLong && !isInt && !isShort && !isByte) ? 1 : integer ? 2 : 0;
        } else if (pointer.type == cFile.langFloat()) {
            return isFloat ? 1 : integer ? 2 : 0;
        } else if (pointer.type == cFile.langDouble()) {
            return (isDouble && !isFloat) ? 1 : (isDouble || isFloat) && !integer ? 2 : real ? 3 : 0;
        }

        return 0;
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (pointer == null) {
            returnPtr = typePtr;
        } else if (verify(pointer) > 0) {
            returnPtr = pointer;
        }
        return returnPtr;
    }
}
