package content;

import data.ContentFile;

/**
 * Regras :
 * Token sempre aponta para o próximo
 * Token com pelo menos um child, deve ter um lastChild
 * Um child deve ter prev como NULL e um lastChild deve ter next como null
 * Todos os child.parent apontam para o mesmo parent
 * O ultimo token aponta para null
 * O token.prev.next sempre aponta para si mesmo
 * O primeiro token tem prev como null
 * Em um grupo, o parent é o iniciador "{(<", mas o ultimo nem sempre é o fechamento[substituir por lastChild = null]
 */
public class Token {

    private String source;
    public int start, length, end;
    public Key key;

    private Token child, next;
    private Token parent, prev, lastChild;

    private boolean complex;
    private int hash;

    public Token(String source) {
        this(source, 0, source.length(), Key.NOONE, false);
    }

    public Token(String source, boolean complex) {
        this(source, 0, source.length(), Key.NOONE, complex);
    }

    public Token(String source, int start, int end, Key key, boolean complex) {
        this.source = source;
        this.start = start;
        this.length = end - start;
        this.end = end;
        this.key = key;
        this.complex = complex;
    }

    public boolean isComplex() {
        return complex;
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

    public void setLastChild(Token lastChild) {
        this.lastChild = lastChild;
    }

    public Token getLastChild() {
        return lastChild;
    }

    public char at(int index) {
        return source.charAt(start + index);
    }

    public boolean compare(int chr) {
        if (length != 1) return false;
        return source.charAt(start) == chr;
    }

    public boolean endsWith(int chr) {
        if (length < 1) return false;
        return source.charAt(start + length - 1) == chr;
    }

    public boolean endsWith(String chr) {
        if (length < chr.length()) return false;
        int off = start + length - chr.length();

        for (int i = 0, len = chr.length(); i < len; i++) {
            if (source.charAt(off + i) != chr.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public void addToBuilder(StringBuilder builder) {
        if (length > 0) {
            builder.append(source, start, end);
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0 && length > 0) {
            for (int i = start; i < end; i++) {
                hash = 31 * hash + source.charAt(i);
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        return start == 0 && end == source.length() ? source : source.substring(start, end);
    }

    public String toString(int start, int end) {
        return source.substring(this.start + start, this.start + end);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Token) {
            Token other = (Token) obj;

            if (other.source == source && other.start == start && other.end == end) { // pointer comparation
                return true;
            } else if (other.length == length) {
                if (hash != 0 && other.hash != 0 && hash != other.hash) {
                    return false;
                }

                final int off = other.start - start;
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) != other.source.charAt(i + off)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean equalsIgnoreCase(Token other) {
        if (other == this) return true;
        if (other == null) return false;

        if (other.source == source && other.start == start && other.end == end) { // pointer comparation
            return true;
        } else if (other.length == length) {
            final int off = other.start - start;
            for (int i = start; i < end; i++) {
                if (Character.toUpperCase(source.charAt(i)) !=
                        Character.toUpperCase(other.source.charAt(i + off))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean equals(String obj) {
        if (obj == null) return false;
        if (length != obj.length()) return false;
        for (int i = 0; i < length; i++) {
            if (source.charAt(start + i) != obj.charAt(i)) return false;
        }
        return true;
    }

    public boolean equalsIgnoreCase(String obj) {
        if (obj == null) return false;
        if (length != obj.length()) return false;
        for (int i = 0; i < length; i++) {
            if (Character.toUpperCase(source.charAt(start + i)) !=
                    Character.toUpperCase(obj.charAt(i))) return false;
        }
        return true;
    }
}
