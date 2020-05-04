package content;

public class Key {
    public static final Key NOONE = new Key("");    // noone
    public static final Key INVALID = new Key("");  // invalid character
    public static final Key IOP = new Key("");      // invalid operator combination

    public static final Key WORD = new Key("");
    public static final Key NUMBER = new Key("");
    public static final Key STRING = new Key("");

    public static final Key PARAM = new Key("(");
    public static final Key BRACE = new Key("{");
    public static final Key INDEX = new Key("[");
    public static final Key GENERIC = new Key("<");

    public static final Key CPARAM = new Key(")");
    public static final Key CBRACE = new Key("}");
    public static final Key CINDEX = new Key("]");
    public static final Key CGENERIC = new Key(">");

    public static final Key USING = new Key("using",            true);
    public static final Key NAMESPACE = new Key("namespace",    true);
    public static final Key CLASS = new Key("class",            true);
    public static final Key STRUCT = new Key("struct",          true);
    public static final Key INTERFACE = new Key("interface",    true);
    public static final Key ENUM = new Key("enum",              true);

    public static final Key PUBLIC = new Key("public",          false, true);
    public static final Key PRIVATE = new Key("private",        false, true);
    public static final Key STATIC = new Key("static",          false, true);
    public static final Key FINAL = new Key("final",            false, true);
    public static final Key ABSTRACT = new Key("abstract",      false, true);

    public static final Key OPERATOR = new Key("operator",      false, false);
    public static final Key GET = new Key("get");
    public static final Key SET = new Key("set");

    public static final Key IF = new Key("if");
    public static final Key ELSE = new Key("else");
    public static final Key FOR = new Key("for");
    public static final Key WHILE = new Key("while");
    public static final Key DO = new Key("do");
    public static final Key SWITCH = new Key("switch");
    public static final Key LOCK = new Key("lock");
    public static final Key NATIVE = new Key("native");

    public static final Key YIELD = new Key("yield");
    public static final Key BREAK = new Key("break");
    public static final Key CONTINUE = new Key("continue");
    public static final Key CASE = new Key("case");
    public static final Key RETURN = new Key("return");
    public static final Key VAR = new Key("var");
    public static final Key LET = new Key("let");
    public static final Key NEW = new Key("new");
    public static final Key THIS = new Key("this",              false, false);
    public static final Key SUPER = new Key("super");
    public static final Key DEFAULT = new Key("default",        false, false);

    public static final Key SEMICOLON = new Key(";");
    public static final Key COLON = new Key(":");
    public static final Key DOT = new Key(".");
    public static final Key COMMA = new Key(",");
    public static final Key QUEST = new Key("?");
    public static final Key LAMBDA = new Key("->");
    public static final Key ADD = new Key("+");
    public static final Key SUB = new Key("-");
    public static final Key MUL = new Key("*");
    public static final Key DIV = new Key("/");
    public static final Key MOD = new Key("%");
    public static final Key RSHIFT = new Key(">>");
    public static final Key LSHIFT = new Key("<<");
    public static final Key INC = new Key("++");
    public static final Key DEC = new Key("--");
    public static final Key SETVAL = new Key("=");
    public static final Key SETADD = new Key("+=");
    public static final Key SETSUB = new Key("-=");
    public static final Key SETDIV = new Key("/=");
    public static final Key SETMOD = new Key("%=");
    public static final Key SETRSHIFT = new Key(">>=");
    public static final Key SETLSHIFT = new Key("<<=");
    public static final Key BITAND = new Key("&");
    public static final Key BITOR = new Key("|");
    public static final Key BITNOT = new Key("~");
    public static final Key BITXOR = new Key("^");
    public static final Key SETAND = new Key("&=");
    public static final Key SETOR = new Key("|=");
    public static final Key SETNOT = new Key("~=");
    public static final Key SETXOR = new Key("^=");
    public static final Key EQUAL = new Key("==");
    public static final Key MORE = new Key(">");
    public static final Key LESS = new Key("<");
    public static final Key EMORE = new Key(">=");
    public static final Key ELESS = new Key("<=");
    public static final Key DIF = new Key("!=");
    public static final Key AND = new Key("&&");
    public static final Key OR = new Key("||");
    public static final Key NOT = new Key("!");

    public final String string;
    public final boolean isNamespace;
    public final boolean isAttribute;
    public final boolean isBlock = true;
    public final boolean isLine = true;

    private Key(String string) {
        this(string, false);
    }

    private Key(String string, boolean isNamespace) {
        this(string, isNamespace, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute) {
        this.string = string;
        this.isNamespace = isNamespace;
        this.isAttribute = isAttribute;
    }

    public static Key getSimbol(String str, int start, int end) {
        int len = end - start;
        if (len == 1) {
            int chr = str.charAt(start);
            switch (chr) {
                case ';': return SEMICOLON;
                case ':': return COLON;
                case '.': return DOT;
                case ',': return COMMA;
                case '?': return QUEST;
                case '+': return ADD;
                case '-': return SUB;
                case '*': return MUL;
                case '/': return DIV;
                case '%': return MOD;
                case '=': return SETVAL;
                case '&': return BITAND;
                case '|': return BITOR;
                case '~': return BITNOT;
                case '^': return BITXOR;
                case '>': return MORE;
                case '<': return LESS;
                case '!': return NOT;
                case '(': return PARAM;
                case ')': return CPARAM;
                case '{': return BRACE;
                case '}': return CBRACE;
                case '[': return INDEX;
                case ']': return CINDEX;
                default: return IOP;
            }
        } else if (len == 2) {
            return LAMBDA.isEqual(str, start, end) ? LAMBDA : RSHIFT.isEqual(str, start, end) ? RSHIFT
                    : LSHIFT.isEqual(str, start, end) ? LSHIFT : INC.isEqual(str, start, end) ? INC
                    : DEC.isEqual(str, start, end) ? DEC : SETADD.isEqual(str, start, end) ? SETADD
                    : SETSUB.isEqual(str, start, end) ? SETSUB : SETDIV.isEqual(str, start, end) ? SETDIV
                    : SETMOD.isEqual(str, start, end) ? SETMOD : SETAND.isEqual(str, start, end) ? SETAND
                    : SETOR.isEqual(str, start, end) ? SETOR : SETNOT.isEqual(str, start, end) ? SETNOT
                    : SETXOR.isEqual(str, start, end) ? SETXOR : EQUAL.isEqual(str, start, end) ? EQUAL
                    : EMORE.isEqual(str, start, end) ? EMORE : ELESS.isEqual(str, start, end) ? ELESS
                    : DIF.isEqual(str, start, end) ? DIF : AND.isEqual(str, start, end) ? AND
                    : OR.isEqual(str, start, end) ? OR : IOP;
        } else if (len == 3){
            return SETRSHIFT.isEqual(str, start, end) ? SETRSHIFT
                    : SETLSHIFT.isEqual(str, start, end) ? SETLSHIFT : IOP;
        }
        return IOP;
    }

    public static Key getKeyword(String str, int start, int end) {
        int len = end - start;
        if (len == 2) {
            return  IF.isEqual(str, start, end) ? IF : DO.isEqual(str, start, end) ? DO : NOONE;
        } else if (len == 3) {
            return  NEW.isEqual(str, start, end) ? NEW : VAR.isEqual(str, start, end) ? VAR :
                    GET.isEqual(str, start, end) ? GET : SET.isEqual(str, start, end) ? SET :
                    FOR.isEqual(str, start, end) ? FOR : LET.isEqual(str, start, end) ? LET : NOONE;
        } else if (len == 4) {
            return  ENUM.isEqual(str, start, end) ? ENUM : ELSE.isEqual(str, start, end) ? ELSE :
                    CASE.isEqual(str, start, end) ? CASE : LOCK.isEqual(str, start, end) ? LOCK :
                    THIS.isEqual(str, start, end) ? THIS : NOONE;
        } else if (len == 5) {
            return  USING.isEqual(str, start, end) ? USING : CLASS.isEqual(str, start, end) ? CLASS :
                    FINAL.isEqual(str, start, end) ? FINAL : WHILE.isEqual(str, start, end) ? WHILE :
                    YIELD.isEqual(str, start, end) ? YIELD : BREAK.isEqual(str, start, end) ? BREAK :
                    SUPER.isEqual(str, start, end) ? SUPER : NOONE;
        } else if (len == 6) {
            return  STRUCT.isEqual(str, start, end) ? STRUCT : PUBLIC.isEqual(str, start, end) ? PUBLIC :
                    STATIC.isEqual(str, start, end) ? STATIC : SWITCH.isEqual(str, start, end) ? SWITCH :
                    RETURN.isEqual(str, start, end) ? RETURN : NATIVE.isEqual(str, start, end) ? NATIVE : NOONE;
        } else if (len == 7) {
            return  PRIVATE.isEqual(str, start, end) ? PRIVATE : DEFAULT.isEqual(str, start, end) ? DEFAULT : NOONE;
        } else if (len == 8) {
            return  ABSTRACT.isEqual(str, start, end) ? ABSTRACT : OPERATOR.isEqual(str, start, end) ? OPERATOR :
                    CONTINUE.isEqual(str, start, end) ? CONTINUE : NOONE;
        } else if (len == 9) {
            return  NAMESPACE.isEqual(str, start, end) ? NAMESPACE : INTERFACE.isEqual(str, start, end) ? INTERFACE : NOONE;
        }
        return NOONE;
    }

    private boolean isEqual(String str, int start, int end) {
        for (int i = 0, len = end - start; i < len; i++) {
            if (str.charAt(start + i) != string.charAt(i)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KEY[" + string + "]";
    }
    
}
