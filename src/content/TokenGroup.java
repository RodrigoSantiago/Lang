package content;

public class TokenGroup {
    public final Token start;
    public final Token end;

    public TokenGroup(Token start, Token end) {
        this.start = start;
        this.end = end;
    }

    public static Token nextType(Token next, Token end) {
        if (next != end && (next.key == Key.GENERIC)) {
            next = next.getNext();
        }
        while (next != end && next.key == Key.INDEX) {
            next = next.getNext();
        }
        return next;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        Token s = start;
        while (s != end) {
            str.append(s);
            s = s.getNext();
        }
        return str.toString();
    }
}
