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
        if (cFile.name.contains("Test")) {
            System.out.println("");
        }
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
                    Key pkey = parent.getPrev() != null ? parent.getPrev().key : null;
                    Key ppkey = pkey != null && parent.getPrev().getPrev() != null ? parent.getPrev().getPrev().key : null;
                    boolean isType = pkey == Key.WORD && (ppkey == Key.NEW || ppkey == Key.IS || ppkey == Key.ISNOT);

                    if (!isType) {
                        Token pp = parent.getParent();
                        if (!isParameterStatment(level, pp) && !isIndexerStatment(level, pp) &&
                                (pp.key != Key.BRACE || isArray(pp))) {
                            parent.setNext(parent.getChild());
                            parent.setChild(null);
                            return null;
                        }
                    }
                } else if (token.key != Key.WORD && token.key != Key.VOID && token.key != Key.LET &&
                        tk != ',' && tk != ':') {
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

            } else if (tk == ']' || tk == '}' || tk == ')') {
                if ((group == '[' && tk == ']') || (group == '{' && tk == '}') || (group == '(' && tk == ')')) {
                    parent.setLastChild(token);
                    return token;
                } else if (group == '(' || group == '[') { // break [better reading for braces { }]
                    parent.setNext(parent.getChild());
                    parent.setChild(null);
                    return null;
                }
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

        /*parent.setLastChild(null);
        return last;*/
        if (group == '{') {
            parent.setLastChild(null);
            return last;
        } else if (group == '[' || group == '(') {
            /*parent.setLastChild(null);
            return last;*/
            parent.setNext(parent.getChild());
            parent.setChild(null);
            return null;
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
            // 123[l]
            // 123.123[D|F]
            // 123e123[D|F]
            // 123e-123[D|F]
            // 0xABCDEF09[L]
            // 0b01[L]
            if (isNumber(chr)) {
                tkStart = index;
                while (!eof()) {
                    if (!isLiteral(readNext())) {
                        readPrev();
                        break;
                    }
                }
                tkEnd = nextIndex;
                tkKeyword = Key.NUMBER; // Expressoes (0x00, 0b00, 0.0, 0.0e00, 0.00e-00)
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
                            if (pChr == '!' && chr == 'i' && nChr == 's') {
                                readNext();
                            } else {
                                readPrev();
                                break;
                            }
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

    private static boolean isLiteral(int chr) {
        return (chr >= '0' && chr <= '9') ||
                (chr >= 'A' && chr <= 'F') ||
                (chr >= 'a' && chr <= 'f') ||
                chr == '.' || chr == '-' ||
                chr == 'x' || chr == 'X' ||
                chr == 'l' || chr == 'L';
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

    private static boolean isParameterStatment(int level, Token parent) {
        if (parent == null) return false;
        if (parent.key != Key.PARAM) return false;

        Token parentPrev = parent.getPrev();
        if (parentPrev == null || parentPrev.key != Key.WORD) {
            return true;
        }
        if (level > 1) return false;

        Token back = parentPrev.getPrev();
        while (back != null) {
            if (back.key == Key.SETVAL) {
                return false;
            } else if (back.key == Key.SEMICOLON || back.key == Key.BRACE || back.key == Key.CBRACE) {
                return true;
            }
            back = back.getPrev();
        }

        return true;
    }

    private static boolean isIndexerStatment(int level, Token parent) {
        if (level > 1) return false;
        if (parent == null) return false;
        if (parent.key != Key.INDEX) return false;

        Token parentPrev = parent.getPrev();
        if (parentPrev == null || parentPrev.key != Key.THIS) return false;

        Token back = parentPrev.getPrev();
        while (back != null) {
            if (back.key == Key.SETVAL) {
                return false;
            } else if (back.key == Key.SEMICOLON || back.key == Key.BRACE || back.key == Key.CBRACE) {
                return true;
            }
            back = back.getPrev();
        }

        return true;
    }

    private static boolean isArray(Token parent) {
        if (parent == null) return false;
        if (parent.key != Key.BRACE) return false;

        Token parentPrev = parent.getPrev();
        if (parentPrev == null) {
            return isArray(parent.getParent());
        }
        // new int[][] { {a, b} , {c, d} }
        if (parentPrev.key == Key.COMMA) {
            return true;
        }

        // [new int[][] { }]  vs [-> int[][] { }] vs [this[] { } * not a problem]
        if (parentPrev.key == Key.INDEX) {
            int s = 0;
            Token back = parentPrev.getPrev();
            while (back != null) {
                if (back.key == Key.NEW) {
                    return true;
                } else if (back.key == Key.INDEX) {
                    if (s != 0) return false;
                } else if (back.key == Key.GENERIC) {
                    if (s != 0) return false;
                    s = 1;
                } else if (back.key == Key.WORD) {
                    s = 2;
                } else {
                    return false;
                }
                back = back.getPrev();
            }
            return false;
        }

        return false;
    }
}
