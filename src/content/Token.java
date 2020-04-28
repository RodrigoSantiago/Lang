package content;

import data.TextFile;

public class Token {
    public static final int INVALID = 0;
    public static final int WORD = 1;
    public static final int NUMBER = 2;
    public static final int OPERATOR = 3;
    public static final int STRING = 4;
    public static final int SPECIAL = 5;
    public static final int PARAM = 6;
    public static final int INDEX = 7;
    public static final int BRACE = 8;

    public final TextFile file;
    public int start, length, end;
    public Key key;
    public int type;

    private Token child, next;
    private Token parent, prev;

    public Token(TextFile file, int start, int end, Key key, int type) {
        this.file = file;
        this.start = start;
        this.length = end - start;
        this.end = end;
        this.key = key;
        this.type = type;
    }

    public Token getParent() {
        return parent;
    }

    public void setParent(Token token) {
        this.parent = token;
    }

    public Token getPrev() {
        return prev;
    }

    public void setNext(Token next) {
        if (this.next != null) {
            this.next.prev = null;
        }
        this.next = next;
        if (this.next != null) {
            this.next.prev = this;
        }
    }

    public Token getNext() {
        return next;
    }

    public void setChild(Token child) {
        if (this.child != null) {
            this.child.parent = null;
        }
        this.child = child;
        if (this.child != null) {
            this.child.parent = this;
        }
    }

    public Token getChild() {
        return child;
    }

    public char at(int index) {
        return file.content.charAt(start + index);
    }

    public boolean compare(int chr) {
        if (length != 1) return false;
        return file.content.charAt(start) == chr;
    }

    public boolean endsWith(int chr) {
        if (length < 1) return false;
        return file.content.charAt(start + length - 1) == chr;
    }

    @Override
    public String toString() {
        return "[" + file.content.substring(start, end) +"]";
    }
}
