package logic.typdef;

import content.Token;
import logic.Namespace;
import logic.member.*;

public class Typedef {

    boolean allowNum;
    boolean allowField;
    boolean allowMethod;
    boolean allowMethodAbsOnly;
    boolean allowProperty;
    boolean allowPropertyAbsOnly;
    boolean allowOperator;
    boolean allowConstructor;
    boolean allowDestructor;
    boolean allowPublicOnly;

    Pointer[] parents;
    Num[] nums;
    Field[] fields;
    Method[] methods;
    Property[] properties;
    Operator[] operators;
    Constructor[] constructors;
    Destructor[] destructors;

    Namespace namespace;
    Token name;
    Token content;

    // FullName complete::line::path;
    // ClassName name0123;

    public Typedef() {
        // Perfect : [private|public][abstract|final](token)(Name)[<Generic Tag>][(:)(ClassName[<tag>])[,...]]({ content })
        // Type : (token)({})
        // namespace  : namespace FullName;
        // using : using FullName[.(Name|*)];
    }
}
