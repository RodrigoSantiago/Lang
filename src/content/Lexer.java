package content;

import data.TextFile;

public class  Lexer {

    private int chr;
    private int index;

    private int nChr;
    private int nextIndex;

    private int pChr;
    private int tkStart, tkEnd, tkType;
    private Key tkKeyword;

    private int pIndex;
    private int ppChr;

    private TextFile file;
    private String content;

    public Lexer(TextFile file) {
        this.file = file;
        this.content = file.content;
        index = 0;
    }

    public Token read() {
        Token begin = null;
        Token last = null;

        while (!eof() && nextToken()) {
            Token token = new Token(file, tkStart, tkEnd, tkKeyword, tkType);
            if (last == null) {
                begin = token;
            } else {
                last.setNext(token);
            }
            last = token;
        }

        Token token = begin;
        while (token != null) {
            token.setParent(null);

            if (token.compare('(') || token.compare('{') || token.compare('[') || token.compare('<')) {
                if (token.getChild() == null) {
                    Token lastChild = read(token, 0, token.at(0));
                    if (lastChild != null) {
                        Token nNext = lastChild.getNext();
                        lastChild.setNext(null);
                        token.setNext(nNext);
                    }
                }
            }

            token = token.getNext();
        }

        return begin;
    }

    private Token read(Token parent, int level, int group) {
        if (parent.getNext() != null) {
            parent.setChild(parent.getNext());
            parent.setNext(null);
        } else {
            return null;
        }

        Token last = null;
        Token token = parent.getChild();
        while (token != null) {

            token.setParent(parent);

            int tk = token.length > 0 ? token.at(0) : 0;
            if (token.getChild() != null) {
                // ignored
            } else if (group == '<') {
                if (tk == '<') {
                    Token lastChild = read(token, level, tk);
                    if (lastChild != null) {
                        Token nNext = lastChild.getNext();
                        lastChild.setNext(null);
                        token.setNext(nNext);
                    } else {
                        return null;
                    }
                } else if (tk == '>') {
                    return token;
                } else if (tk == ':' && level > 1) {
                    parent.setNext(parent.getChild());
                    parent.setChild(null);
                    return null;

                } else if (tk == ',' && parent.getParent() != null) {
                    boolean isNew = parent.getPrev() != null && parent.getPrev().type == Token.WORD &&
                            parent.getPrev().getPrev() != null && parent.getPrev().getPrev().key == Key.NEW;
                    if (!isNew) {
                        Token pp = parent.getParent();
                        Token pprev = pp.getPrev();
                        if ((pp.type == Token.PARAM && pprev != null && pprev.type == Token.WORD)
                                || (pp.type == Token.INDEX && level > 1)
                                || (pp.type == Token.BRACE && pprev != null && pprev.type == Token.INDEX)) {
                            parent.setNext(parent.getChild());
                            parent.setChild(null);
                            return null;
                        }
                    }
                } else if (token.type != Token.WORD && tk != ',' && tk != ':') {
                    parent.setNext(parent.getChild());
                    parent.setChild(null);
                    return null;
                }

            } else if (tk == '(' || tk == '{' || tk == '[') {
                Token lastChild = read(token, level + 1, tk);
                if (lastChild != null) {
                    Token nNext = lastChild.getNext();
                    lastChild.setNext(null);
                    token.setNext(nNext);
                }

            } else if ((group == '[' && tk == ']') || (group == '{' && tk == '}') || (group == '(' && tk == ')')) {
                return token;

            } else if (tk == '<') {
                Token lastChild = read(token, level, tk);
                if (lastChild != null) {
                    Token nNext = lastChild.getNext();
                    lastChild.setNext(null);
                    token.setNext(nNext);
                }

            } else if (tk == '>') {
                if (token.getPrev() != null && token.getPrev().endsWith('>') && token.getPrev().end == token.start) {
                    token.getPrev().end = token.end;
                    token.getPrev().type = Token.OPERATOR;
                    token.getPrev().setNext(token.getNext());
                    token = token.getPrev();
                }
            }
            last = token;
            token = token.getNext();
        }

        return group == '[' || group == '{' || group == '(' ? last : null;
    }

    public boolean nextToken() {
        tkKeyword = null;

        while (!eof()) {
            readNext();

            // Name
            if (isLetter(chr)) {
                tkStart = index;
                while (!eof()) {
                    if (!isChar(readNext()) && (chr != ':' || (nChr != ':' && pChr != ':'))) {
                        readPrev();
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkType = Token.WORD;
                tkKeyword = Key.getKeyword(content, tkStart, tkEnd);

                return true;
            }

            // Number
            if (isNumber(chr)) {
                tkStart = index;
                while (!eof()) {
                    if (!(isNumber(readNext()) || (chr == '.' || chr == 'e' || chr == 'E'))) {
                        readPrev();
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkType = Token.NUMBER;
                return true;
            }

            // String
            if (chr == '"' || chr == '\'') {
                tkStart = index;

                int in = chr;
                boolean invert = false;
                while (!eof()) {
                    readNext();

                    if (chr == '\\') {
                        invert = !invert;
                    } else if (chr == in && !invert) {
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkType = Token.STRING;
                return true;
            }

            // Special Characters
            if (isSplitter(chr)) {
                tkStart = index;
                tkEnd = nextIndex;
                tkType = chr == '(' ? Token.PARAM
                        : chr == '{' ? Token.BRACE
                        : chr == '[' ? Token.INDEX
                        : Token.SPECIAL;

                return true;
            }

            // Operator
            if (isOperator(chr) && (chr != '/' || (nChr != '/' && nChr != '*'))) {
                tkStart = index;
                while (!eof()) {
                    if (!isOperatorP(readNext())) {
                        readPrev();
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkType = Token.OPERATOR;
                return true;
            }

            boolean validChar = false;
            // Space
            if (isSpace(chr)) {
                while (!eof()) {
                    if (!isSpace(readNext())) {
                        readPrev();
                        break;
                    }
                }
                validChar = true;
            }

            // Line Comment
            if (chr == '/' && nChr == '/') {
                while (!eof()) {
                    if (readNext() == '\n') break;
                }
                validChar = true;
            }

            // Block Comment
            if (chr == '/' && nChr == '*') {
                readNext();
                while (!eof()) {
                    if ((readNext() == '*' && nChr == '/')) {
                        readNext();
                        break;
                    }
                }
                validChar = true;
            }

            // Invalid Char
            if (!eof() && !validChar) {
                tkStart = index;
                tkEnd = nextIndex;
                tkType = Token.INVALID;
                return true;
            }
        }
        tkStart = tkEnd = -1;
        tkKeyword = null;
        return false;
    }

    public boolean eof() {
        return nextIndex >= content.length();
    }

    private int readNext() {
        pIndex = index;
        ppChr = pChr;

        index = nextIndex;
        pChr = chr;
        chr = content.codePointAt(index);
        nextIndex = index + Character.charCount(chr);
        nChr = nextIndex < content.length() ? content.codePointAt(nextIndex) : 0;

        return chr;
    }

    private int readPrev() {
        nChr = chr;
        chr = pChr;
        pChr = ppChr;
        ppChr = -1;

        nextIndex = index;
        index = pIndex;
        pIndex = -1;

        return chr;
    }

    private static boolean isSpace(int chr) {
        return Character.isSpaceChar(chr) || Character.isWhitespace(chr);
    }

    private static boolean isNumber(int chr) {
        return (chr >= '0' && chr <= '9');
    }

    private static boolean isLetter(int chr) {
        return (chr >= 'A' && chr <= 'Z') || (chr >= 'a' && chr <= 'z') || chr == '_';
    }

    private static boolean isChar(int chr) {
        return (chr >= '0' && chr <= '9') || (chr >= 'A' && chr <= 'Z') || (chr >= 'a' && chr <= 'z') || chr == '_';
    }

    private static boolean isSplitter(int chr) {
        return chr == '(' || chr == ')' || chr == '{' || chr == '}' || chr == '[' || chr == ']' || chr == '.'
                || chr == ',' || chr == ';' || chr == '>';
    }

    private static boolean isOperator(int chr) {
        return chr == '+' || chr == '-' || chr == '*' || chr == '/' || chr == '%'
                || chr == '=' || chr == '!' || chr == '|' || chr == '&' || chr == '^' || chr == ':' || chr == '?'
                || chr == '~' || chr == '<';
    }

    private static boolean isOperatorP(int chr) {
        return chr == '+' || chr == '-' || chr == '*' || chr == '/' || chr == '%'
                || chr == '=' || chr == '!' || chr == '|' || chr == '&' || chr == '^' || chr == ':' || chr == '?'
                || chr == '~' || chr == '<' || chr == '>';
    }
}
