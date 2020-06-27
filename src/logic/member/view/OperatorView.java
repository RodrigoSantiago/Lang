package logic.member.view;

import content.Key;
import content.Token;
import logic.Pointer;
import logic.member.Operator;
import logic.member.Operator;
import logic.typdef.Type;

public class OperatorView {
    public static final OperatorView OR = new OperatorView(Key.OR);
    public static final OperatorView AND = new OperatorView(Key.AND);
    public static final OperatorView CAST = new OperatorView(Key.CAST);
    public static final OperatorView SET = new OperatorView(Key.SETVAL);
    public static final OperatorView IS = new OperatorView(Key.IS);
    public static final OperatorView ISNOT = new OperatorView(Key.ISNOT);

    public final Pointer caller;
    public final Operator operator;
    public final ParamView paramView;
    private Pointer typePtr;

    private OperatorView(Key key) {
        this.caller = null;
        this.operator = null;
        this.paramView = null;
    }

    public OperatorView(Pointer caller, Operator operator) {
        this.caller = caller;
        this.operator = operator;
        if (operator.getTypePtr() != null) {
            typePtr = Pointer.byGeneric(operator.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, operator.getParams());
    }

    public OperatorView(Pointer caller, OperatorView operatorView) {
        this.caller = caller;
        this.operator = operatorView.operator;
        if (operatorView.getTypePtr() != null) {
            typePtr = Pointer.byGeneric(operatorView.getTypePtr(), caller);
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
