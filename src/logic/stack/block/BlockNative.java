package logic.stack.block;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.stack.Block;

public class BlockNative extends Block {

    Token paramToken;
    TokenGroup contentToken;

    public BlockNative(Block block, Token start, Token end) {
        super(block, start, end);

        Token colon = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NATIVE) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                readSourceToken(token.getChild(), token.getLastChild());
                state = 2;
            } else if (state == 2 && token.key == Key.COLON) {
                colon = token;
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.WORD && next != end && next.key == Key.BRACE) {
                cFile.erro(token, "Label Not Allowed", this);
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.BRACE) {
                if (state == 1) cFile.erro(token.start, token.start + 1, "Missing condition", this);
                if (state == 3) cFile.erro(colon, "Unexpected Token", this);

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
                state = 5;
            } else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.SEMICOLON) {
                if (state == 1) cFile.erro(token, "Missing condition", this);
                if (state == 3) contentToken = new TokenGroup(colon, colon.getNext());

                state = 5;
            } else if ((state == 2 || state == 3 || state == 4)) {
                contentToken = new TokenGroup(state == 3 ? colon : token, end);

                next = end;
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

    private void readSourceToken(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.equals("source")) {
                paramToken = token;
                state = 1;
            } else if (state == 0 && token.equals("header")) {
                paramToken = token;
                cFile.erro(token, "The Header Parameter is not allowed here", this);
                state = 1;
            } else if (state == 0 && token.equals("return")) {
                paramToken = token;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
        if (start == end) {
            cFile.erro(token, "The parameter should not be empty", this);
        }
    }

    @Override
    public void load() {

    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
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
                cBuilder.idt(idt).add(lines[0]).ln();
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
                        cBuilder.idt(idt).add(line).ln();
                    }
                }
            }
        }
    }
}
