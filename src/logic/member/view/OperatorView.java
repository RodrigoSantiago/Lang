package logic.member.view;

import content.Key;
import content.Token;
import logic.Pointer;
import logic.member.Operator;
import logic.member.Operator;
import logic.typdef.Type;

public class OperatorView {

    // Native Operators
    public static final OperatorView OR = new OperatorView(Key.OR);
    public static final OperatorView AND = new OperatorView(Key.AND);
    public static final OperatorView CAST = new OperatorView(Key.CAST);
    public static final OperatorView SET = new OperatorView(Key.SETVAL);
    public static final OperatorView IS = new OperatorView(Key.IS);
    public static final OperatorView ISNOT = new OperatorView(Key.ISNOT);
    public static final OperatorView EQUAL = new OperatorView(Key.EQUAL);
    public static final OperatorView DIF = new OperatorView(Key.DIF);

    private final Pointer typePtr;
    private final ParamView paramView;

    public final Pointer caller;
    public final Operator operator;

    private OperatorView(Key key) {
        this.caller = null;
        this.operator = null;
        this.paramView = null;
        this.typePtr = null;
    }

    public OperatorView(Pointer caller, Operator operator) {
        this.caller = caller;
        this.operator = operator;
        if (Pointer.hasGeneric(operator.getTypePtr(), caller)) {
            typePtr = Pointer.byGeneric(operator.getTypePtr(), caller);
        } else {
            typePtr = operator.getTypePtr();
        }
        paramView = new ParamView(caller, operator.getParams());
    }

    public OperatorView(Pointer caller, OperatorView operatorView) {
        this.caller = caller;
        this.operator = operatorView.operator;
        if (Pointer.hasGeneric(operatorView.getTypePtr(), caller)) {
            typePtr = Pointer.byGeneric(operatorView.getTypePtr(), caller);
        } else {
            typePtr = operatorView.getTypePtr();
        }
        paramView = new ParamView(caller, operatorView.getParams());
    }

    public boolean isFrom(Type type) {
        return operator.type == type;
    }

    public boolean canAcess(Type type) {
        return (isPrivate() && operator.cFile == type.cFile) ||
                (!isPublic() && !isPrivate() && operator.cFile.library == type.cFile.library) || isPublic();
    }

    public Token getOperator() {
        return operator.getOperator();
    }

    public Pointer getTypePtr() {
        return typePtr != null ? typePtr : operator.getTypePtr();
    }

    public ParamView getParams() {
        return paramView;
    }

    public boolean isPrivate() {
        return operator.isPrivate();
    }

    public boolean isPublic() {
        return operator.isPublic();
    }

    public boolean isLet() {
        return operator.isLet();
    }
}
