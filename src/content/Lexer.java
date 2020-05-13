package content;

import data.ContentFile;

public class  Lexer {

    private int chr;
    private int index;

    private int nChr;
    private int nextIndex;

    private int pChr;
    private int tkStart, tkEnd;
    private Key tkKeyword;
    private boolean tkComplex;

    private int pIndex;
    private int ppChr;

    private ContentFile cFile;

    public Lexer(ContentFile cFile) {
        this.cFile = cFile;
        index = 0;
    }

    public Token read() {
        Token begin = null;
        Token last = null;

        while (!eof() && nextToken()) {
            Token token = new Token(cFile.content, tkStart, tkEnd, tkKeyword, tkComplex);

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
                        parent.setNext(parent.getChild());
                        parent.setChild(null);
                        return null;
                    }
                } else if (tk == '>') {
                    token.key = Key.CGENERIC;
                    parent.key = Key.GENERIC;
                    parent.setLastChild(token);
                    return token;
                } else if (tk == ':' && level > 1) {
                    parent.setNext(parent.getChild());
                    parent.setChild(null);
                    return null;

                } else if (tk == ',' && parent.getParent() != null) {
                    boolean isNew = parent.getPrev() != null && parent.getPrev().key == Key.WORD &&
                            parent.getPrev().getPrev() != null && parent.getPrev().getPrev().key == Key.NEW;
                    if (!isNew) {
                        Token pp = parent.getParent();
                        Token pprev = pp.getPrev();
                        if ((pp.key == Key.PARAM && pprev != null && pprev.key == Key.WORD)
                                || (pp.key == Key.INDEX && level > 1)
                                || (pp.key == Key.BRACE && pprev != null && pprev.key == Key.INDEX)) {
                            parent.setNext(parent.getChild());
                            parent.setChild(null);
                            return null;
                        }
                    }
                } else if (token.key != Key.WORD && tk != ',' && tk != ':') {
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
                parent.setLastChild(token);
                return token;

            } else if (tk == '<') {
                Token lastChild = read(token, level, tk);
                if (lastChild != null) {
                    Token nNext = lastChild.getNext();
                    lastChild.setNext(null);
                    token.setNext(nNext);
                }

            } else if (token.key.isOperator) {
                Token prev = token.getPrev();
                if (prev != null && prev.end == token.start && prev.key.isOperator) {
                    prev.end = token.end;
                    prev.key = Key.getSimbol(cFile.content, prev.start, prev.end);
                    prev.setNext(token.getNext());
                    token = prev;
                }
            }
            last = token;
            token = token.getNext();
        }

        if (group == '[' || group == '{' || group == '(') {
            parent.setLastChild(null);
            return last;
        } else if (group == '<') {
            parent.setNext(parent.getChild());
            parent.setChild(null);
            return null;
        } else {
            return last;
        }
    }

    public boolean nextToken() {
        tkKeyword = Key.NOONE;
        tkComplex = false;

        while (!eof()) {
            readNext();

            // Name
            if (isLetter(chr)) {
                tkStart = index;
                int ccount = 0;
                while (!eof()) {
                    if (chr != ':') {
                        ccount = 0;
                    } else if (nChr == ':' && ccount == 0) {
                        ccount = 1;
                        tkComplex = true;
                    } else if (ccount == 1) {
                        ccount = 2;
                    } else {
                        readPrev();
                        break;
                    }
                    if (!isChar(readNext()) && chr != ':') {
                        readPrev();
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkKeyword = Key.getKeyword(cFile.content, tkStart, tkEnd);
                if (tkKeyword == Key.NOONE) {
                    tkKeyword = Key.WORD;
                }

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
                tkKeyword = Key.NUMBER;
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
                tkKeyword = Key.STRING;
                return true;
            }

            // Special Characters
            if (isSplitter(chr)) {
                tkStart = index;
                tkEnd = nextIndex;
                tkKeyword = Key.getSimbol(cFile.content, tkStart, tkEnd);

                return true;
            }

            // Operator
            if (isOperator(chr) && (chr != '/' || (nChr != '/' && nChr != '*'))) {
                tkStart = index;
                if (nChr != '>') {
                    while (!eof()) {
                        if (!isOperatorP(readNext())) {
                            readPrev();
                            break;
                        }
                    }
                }
                tkEnd = nextIndex;
                tkKeyword = Key.getSimbol(cFile.content, tkStart, tkEnd);
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
                tkKeyword = Key.INVALID;
                return true;
            }
        }
        tkStart = tkEnd = -1;
        tkKeyword = Key.NOONE;
        return false;
    }

    public boolean eof() {
        return nextIndex >= cFile.content.length();
    }

    private int readNext() {
        pIndex = index;
        ppChr = pChr;

        index = nextIndex;
        pChr = chr;
        chr = cFile.content.codePointAt(index);
        nextIndex = index + Character.charCount(chr);
        nChr = nextIndex < cFile.content.length() ? cFile.content.codePointAt(nextIndex) : 0;

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
