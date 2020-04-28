package content;

public class Key {
    public static final Key NOONE = new Key("");

    public static final Key USING = new Key("using", true);
    public static final Key NAMESPACE = new Key("namespace", true);
    public static final Key CLASS = new Key("class", true);
    public static final Key STRUCT = new Key("struct", true);
    public static final Key INTERFACE = new Key("interface", true);
    public static final Key ENUM = new Key("enum", true);

    public static final Key PUBLIC = new Key("public", true, true, true);
    public static final Key PRIVATE = new Key("private", true, true, true);
    public static final Key STATIC = new Key("static", true, true, true);
    public static final Key FINAL = new Key("final", true, true, true);
    public static final Key ABSTRACT = new Key("abstract", true, true, true);

    public static final Key OPERATOR = new Key("operator", false, false, true);
    public static final Key GET = new Key("get");
    public static final Key SET = new Key("set");

    public static final Key IF = new Key("if");
    public static final Key ELSE = new Key("else");
    public static final Key FOR = new Key("for");
    public static final Key WHILE = new Key("while");
    public static final Key DO = new Key("do");
    public static final Key SWITCH = new Key("switch");
    public static final Key CASE = new Key("case");
    public static final Key LOCK = new Key("lock");
    public static final Key YIELD = new Key("yield");

    public static final Key BREAK = new Key("break");
    public static final Key CONTINUE = new Key("continue");
    public static final Key RETURN = new Key("return");
    public static final Key VAR = new Key("var");
    public static final Key LET = new Key("let");
    public static final Key NEW = new Key("new");
    public static final Key THIS = new Key("this", false, false, true);
    public static final Key SUPER = new Key("super");
    public static final Key DEFAULT = new Key("default", false, false, true);
    public static final Key NATIVE = new Key("native");

    public final String string;
    public final boolean isNamespace;
    public final boolean isAttribute;
    public final boolean isTypedef;

    private Key(String string) {
        this(string, false);
    }

    private Key(String string, boolean isNamespace) {
        this(string, isNamespace, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute) {
        this(string, isNamespace, isAttribute, false);
    }

    private Key(String string, boolean isNamespace, boolean isAttribute, boolean isTypedef) {
        this.string = string;
        this.isNamespace = isNamespace;
        this.isAttribute = isAttribute;
        this.isTypedef = isTypedef;
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
}
