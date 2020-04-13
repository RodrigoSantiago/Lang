package content;

import java.util.ArrayList;

public class Token {
    public TokenType type;
    public ArrayList<Token> tokens;

    public int start, length;

    public int Start() {
        return start;
    }

    public int Length() {
        return length;
    }

    public int End() {
        return start + length;
    }
}
