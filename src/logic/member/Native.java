package logic.member;

import content.Key;
import content.Token;
import data.CppBuilder;
import logic.typdef.Type;

public class Native extends Member {

    private boolean isHeader;   // in Header Implementation
    private boolean isSource;   // in Source Implementation OR internal non returning block
    private boolean isReturn;   // in Source internal with returning block

    private Token sourceToken;
    private Token contentToken;

    public Native(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, false, false, false, false, true, false, false);
            } else if (state == 0 && token.key == Key.NATIVE) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM) {
                readSourceToken(token.getChild(), token.getLastChild());
                state = 3;
            } else if (state == 3 && token.key == Key.BRACE) {
                if (token.getChild() == null) {
                    cFile.erro(token, "Unexpected token", this);
                } else {
                    contentToken = token;
                }
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }

            last = token;
            token = next;
        }

        if (state != 4) {
            cFile.erro(last, "Unexpected end of tokens", this);
        }
    }

    private void readSourceToken(Token init, Token end) {

        int state = 0;
        Token next;
        Token last = init;
        Token token = init;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.equals("source")) {
                sourceToken = token;
                isSource = true;
                state = 1;
            } else if (state == 0 && token.equals("header")) {
                sourceToken = token;
                isHeader = true;
                state = 1;
            } else if (state == 0 && token.equals("return")) {
                sourceToken = token;
                isReturn = true;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }

            last = token;
            token = next;
        }

        if (state != 1) {
            cFile.erro(last, "Unexpected end of tokens", this);
        }
    }

    @Override
    public boolean load() {
        if (sourceToken != null && isReturn) {
            cFile.erro(sourceToken, "Returning block are not allowed here", this);
        }
        return contentToken != null && sourceToken != null && !isReturn;
    }

    public void build(CppBuilder cBuilder) {

        if (isHeader()) {
            cBuilder.toHeader();
        } else if (isSource()) {
            cBuilder.toSource(type.template != null);
        }
        int start = contentToken.end;
        int end = contentToken.getLastChild().start;

        boolean text = false;
        String in = cFile.content.substring(start, end);
        for (int i = 0; i < in.length(); i++) {
            char chr = in.charAt(i);
            if (chr != ' ' && chr != '\t' && chr != '\n' && chr != '\r') {
                text = true;
                break;
            }
        }
        if (text) {
            String[] lines = in.split("\\r?\\n");
            if (lines.length == 1) {
                cBuilder.add(lines[0]).ln();
            } else if (lines.length > 1) {
                int first = 0;
                String firstLine = cFile.content.substring(contentToken.end, contentToken.getChild().start);
                if (firstLine.contains("\n")) {
                    first = 1;
                }
                int sub = 0;
                for (int i = 0; i < lines[first].length(); i++) {
                    if (!Character.isSpaceChar(lines[first].charAt(i))) {
                        sub = i;
                        break;
                    }
                }
                for (String line : lines) {
                    int s = 0;
                    for (int j = 0; j < line.length() && j < sub; j++) {
                        if (Character.isSpaceChar(line.charAt(j))) {
                            s = j + 1;
                        }
                    }
                    line = line.substring(s);
                    cBuilder.idt(isHeader() ? 1 : 0).add(line).ln();
                }
            }
        }

        cBuilder.toHeader();
    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean isSource() {
        return isSource;
    }

    public boolean isReturn() {
        return isReturn;
    }
}
