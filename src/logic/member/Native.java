package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.typdef.Type;

public class Native extends Member {

    private boolean isMacro;    // in Header Implementation [Before Macros]
    private boolean isExtra;    // in Header Implementation [After Class]
    private boolean isHeader;   // in Header Implementation
    private boolean isSource;   // in Source Implementation OR internal non returning block
    private boolean isReturn;   // in Source internal with returning block

    private Token sourceToken;
    private TokenGroup contentToken;

    public Native(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, false, false, false, false, true, false, false);
            } else if (state == 0 && token.key == Key.NATIVE) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM && token.getChild() != null) {
                readSourceToken(token.getChild(), token.getLastChild());
                state = 3;
            } else if (state == 3 && token.key == Key.BRACE) {
                if (token.getChild() == null) {
                    if (next != end) {
                        contentToken = new TokenGroup(next, end);
                        next = end;
                    }
                    cFile.erro(token, "Brace closure expected", this);
                } else {
                    if (token.isOpen()) cFile.erro(token, "Brace closure expected", this);
                    contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                }
                state = 4;
            } else if (state == 3 && token.key == Key.SEMICOLON) {
                contentToken = new TokenGroup(token, next);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

    }

    private void readSourceToken(Token init, Token end) {
        int state = 0;
        Token next;
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
            } else if (state == 0 && token.equals("macro")) {
                sourceToken = token;
                isMacro = true;
                state = 1;
            } else if (state == 0 && token.equals("extra")) {
                sourceToken = token;
                isExtra = true;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

    }

    @Override
    public boolean load() {
        if (sourceToken != null && isReturn) {
            cFile.erro(sourceToken, "Returning block are not allowed here", this);
        }
        if (contentToken != null && contentToken.start.key == Key.SEMICOLON) {
            cFile.erro(contentToken.start, "A Native block should implement", this);
        }
        return contentToken != null && sourceToken != null && !isReturn;
    }

    @Override
    public void make() {

    }

    @Override
    public void build(CppBuilder cBuilder) {

        if (isHeader() || isMacro() || isExtra()) {
            cBuilder.toHeader();
        } else if (isSource()) {
            cBuilder.toSource(type.template != null);
        }
        int start = contentToken.start.getParent().end;
        int end = contentToken.end.start;

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
                cBuilder.idt(isHeader() ? 1 : 0).add(lines[0]).ln();
            } else if (lines.length > 1) {
                int sub = -1;
                for (String value : lines) {
                    int s = 0;
                    for (int i = 0; i < value.length(); i++) {
                        if (!Character.isSpaceChar(value.charAt(i))) {
                            if (s < sub || sub == -1) sub = i;
                            break;
                        }
                    }
                }
                if (sub == -1) {
                    sub = 0;
                }

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    int s = 0;
                    for (int j = 0; j < line.length() && j < sub; j++) {
                        if (Character.isSpaceChar(line.charAt(j))) {
                            s = j + 1;
                        }
                    }
                    line = line.substring(s);
                    if ((i > 0 && i < lines.length - 1) || !line.isEmpty()) {
                        cBuilder.idt(isHeader() ? 1 : 0).add(line).ln();
                    }
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

    public boolean isMacro() {
        return isMacro;
    }

    public boolean isExtra() {
        return isExtra;
    }
}
