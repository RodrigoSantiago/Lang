package content;

public enum Key {
    NOONE(""),    // noone
    INVALID(""),  // invalid character
    IOP(""),      // invalid operator combination

    WORD(""),
    NUMBER(""),
    STRING(""),
    TRUE("true"),
    FALSE("false"),
    NULL("null"),

    PARAM("("),
    BRACE("{"),
    INDEX("["),
    GENERIC("<"),

    CPARAM(")"),
    CBRACE("}"),
    CINDEX("]"),
    CGENERIC(">"),

    USING("using",            true, false),
    NAMESPACE("namespace",    true, false),
    CLASS("class",            true, false),
    STRUCT("struct",          true, false),
    INTERFACE("interface",    true, false),
    ENUM("enum",              true, false),

    PUBLIC("public",          false, true),
    PRIVATE("private",        false, true),
    STATIC("static",          false, true),
    FINAL("final",            false, true),
    ABSTRACT("abstract",      false, true),

    OPERATOR("operator",      false, false),

    IF("if",                  false, false, false, false, true),
    ELSE("else",              false, false, false, false, true),
    FOR("for",                false, false, false, false, true),
    WHILE("while",            false, false, false, false, true),
    DO("do",                  false, false, false, false, true),
    SWITCH("switch",          false, false, false, false, true),
    LOCK("lock",              false, false, false, false, true),
    NATIVE("native",          false, false, false, false, true),

    YIELD("yield"),
    BREAK("break"),
    CONTINUE("continue"),
    CASE("case"),
    RETURN("return"),
    VAR("var"),
    LET("let",                false, true),
    NEW("new"),
    THIS("this",              false, false),
    SUPER("super"),
    DEFAULT("default",        false, true),
    VOID("void"),

    IS("is",                  false, false, true, false),
    ISNOT("isnot",            false, false, true, false),
    SEMICOLON(";"),
    COLON(":"),
    DOT("."),
    COMMA(","),
    QUEST("?",                false, false, true, false),
    LAMBDA("->"),
    MUL("*",                  false, false, true, true, false, 1),
    DIV("/",                  false, false, true, true, false, 1),
    MOD("%",                  false, false, true, true, false, 1),
    ADD("+",                  false, false, true, true, false, 2),
    SUB("-",                  false, false, true, true, false, 2),
    INC("++",                 false, false, true, true),
    DEC("--",                 false, false, true, true),
    RSHIFT(">>",              false, false, true, true, false, 3),
    LSHIFT("<<",              false, false, true, true, false, 3),
    MORE(">",                 false, false, true, true, false, 4),
    LESS("<",                 false, false, true, true, false, 4),
    EMORE(">=",               false, false, true, true, false, 4),
    ELESS("<=",               false, false, true, true, false, 4),
    EQUAL("==",               false, false, true, true, false, 5),
    DIF("!=",                 false, false, true, true, false, 5),
    BITAND("&",               false, false, true, true, false, 6),
    BITXOR("^",               false, false, true, true, false, 7),
    BITOR("|",                false, false, true, true, false, 8),
    BITNOT("~",               false, false, true, true),
    AND("&&",                 false, false, true, false, false, 9),
    OR("||",                  false, false, true, false, false, 10),
    NOT("!",                  false, false, true, true),
    SETVAL("=",               false, false, true, false, false, 11),
    SETADD("+=",              false, false, true, false, false, 11),
    SETSUB("-=",              false, false, true, false, false, 11),
    SETDIV("/=",              false, false, true, false, false, 11),
    SETMOD("%=",              false, false, true, false, false, 11),
    SETRSHIFT(">>=",          false, false, true, false, false, 11),
    SETLSHIFT("<<=",          false, false, true, false, false, 11),
    SETAND("&=",              false, false, true, false, false, 11),
    SETOR("|=",               false, false, true, false, false, 11),
    SETNOT("~=",              false, false, true, false, false, 11),
    SETXOR("^=",              false, false, true, false, false, 11),

    // GET("get"),
    // SET("set"),
    CAST("cast",              false, false, false, true),
    AUTO("auto",              false, false, false, true);

    public final String string;
    public final boolean isNamespace;
    public final boolean isAttribute;
    public final boolean isOperator;
    public final boolean isOverridable;
    public final boolean isBlock;
    public final int priority;

    private Key(String string) {
        this(string, false);
    }

    private Key(String string, boolean isNamespace) {
        this(string, isNamespace, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute) {
        this(string, isNamespace, isAttribute, false, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute, boolean isOperator, boolean isOverridable) {
        this(string, isNamespace, isAttribute, isOperator, isOverridable, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute, boolean isOperator, boolean isOverridable,
                boolean isBlock) {
        this(string, isNamespace, isAttribute, isOperator, isOverridable, isBlock, 0);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute, boolean isOperator, boolean isOverridable,
                boolean isBlock, int priority) {
        this.string = string;
        this.isNamespace = isNamespace;
        this.isAttribute = isAttribute;
        this.isOperator = isOperator;
        this.isOverridable = isOverridable;
        this.isBlock = isBlock;
        this.priority = priority;
    }

    // TODO - MODIFICAR PARA TOKEN E USAR HASH

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
        } else if (len == 3) {
            return SETRSHIFT.isEqual(str, start, end) ? SETRSHIFT
                    : SETLSHIFT.isEqual(str, start, end) ? SETLSHIFT : IOP;
        }
        return IOP;
    }

    public static Key getKeyword(String str, int start, int end) {
        int len = end - start;
        if (len == 2) {
            return  IF.isEqual(str, start, end) ? IF : DO.isEqual(str, start, end) ? DO :
                    IS.isEqual(str, start, end) ? IS : NOONE;
        } else if (len == 3) {
            return  NEW.isEqual(str, start, end) ? NEW : VAR.isEqual(str, start, end) ? VAR :
                    // GET.isEqual(str, start, end) ? GET : SET.isEqual(str, start, end) ? SET :
                    FOR.isEqual(str, start, end) ? FOR : LET.isEqual(str, start, end) ? LET : NOONE;
        } else if (len == 4) {
            return  ENUM.isEqual(str, start, end) ? ENUM : ELSE.isEqual(str, start, end) ? ELSE :
                    CASE.isEqual(str, start, end) ? CASE : LOCK.isEqual(str, start, end) ? LOCK :
                    THIS.isEqual(str, start, end) ? THIS : VOID.isEqual(str, start, end) ? VOID :
                    TRUE.isEqual(str, start, end) ? TRUE : NULL.isEqual(str, start, end) ? NULL : NOONE;
        } else if (len == 5) {
            return  USING.isEqual(str, start, end) ? USING : CLASS.isEqual(str, start, end) ? CLASS :
                    FINAL.isEqual(str, start, end) ? FINAL : WHILE.isEqual(str, start, end) ? WHILE :
                    YIELD.isEqual(str, start, end) ? YIELD : BREAK.isEqual(str, start, end) ? BREAK :
                    SUPER.isEqual(str, start, end) ? SUPER : FALSE.isEqual(str, start, end) ? FALSE :
                    ISNOT.isEqual(str, start, end) ? ISNOT : NOONE;
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
