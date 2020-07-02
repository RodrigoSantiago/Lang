package content;

public class TokenGroup {
    public final Token start;
    public final Token end;

    public TokenGroup(Token start) {
        this(start, start.getNext());
    }

    public TokenGroup(Token start, Token end) {
        this.start = start;
        this.end = end;
    }

    // TODO - OPEN INDEXER
    public static Token nextType(Token next, Token end) {
        if (next != end && (next.key == Key.GENERIC)) {
            next = next.getNext();
        }
        while (next != end && next.key == Key.INDEX) {
            next = next.getNext();
        }
        return next;
    }

    public static String toString(Token start, Token end) {
        String str = "";
        Token token = start;
        while (token != end) {
            str += token;
            token = token.getNext();
        }
        return str;
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

    public boolean isLiteral() {
        return recursiveLiteral(start, end);
    }

    private boolean recursiveLiteral(Token start, Token end) {
        Token token = start;
        while (token != end) {
            if (token.key == Key.PARAM && token.getChild() != null) {
                if (!recursiveLiteral(token.getChild(), token.getLastChild())) {
                    return false;
                }
            } else if (token.key != Key.NUMBER && token.key != Key.TRUE && token.key != Key.FALSE &&
                    token.key != Key.NULL && token.key != Key.DEFAULT && !token.key.isOperator) {
                return false;
            }
            token = token.getNext();
        }
        return true;
    }
}
