package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Operator;
import logic.member.Operator;
import logic.typdef.Type;

public class OperatorView {
    public final Pointer caller;
    public final Operator operator;
    public final ParamView paramView;
    private Pointer typePtr;

    public OperatorView overrided;

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
        if (operatorView.typePtr != null) {
            typePtr = Pointer.byGeneric(operatorView.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, operatorView.getParams());
        this.overrided = operatorView.overrided;
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
