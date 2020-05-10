package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Method;

public class MethodView {
    public final Pointer caller;
    public final Method method;
    private Pointer ptr;

    public MethodView(Pointer caller, Method method) {
        this.caller = caller;
        this.method = method;
        if (method.returnType != null && !method.returnType.isDefault()) {
            ptr = Pointer.byGeneric(method.returnType, caller);
        } else {
            ptr = method.returnType;
        }
    }

    public Token getName() {
        return method.nameToken;
    }

    public Pointer getType() {
        return ptr;
    }

    public int getParamsCount() {
        return method.params.args.size();
    }

    public Pointer getParamType(int index) {
        return null;
    }

    public Token getParamName(int index) {
        return null;
    }

    public boolean isParamLet(int index) {
        return false;
    }

    public int getGenericsCount() {
        return 0;
    }

    public Token getGenericName(int index) {
        return null;
    }

    public Pointer getGenericType(int index) {
        return null;
    }

    public boolean isPrivate() {
        return method.isPrivate();
    }

    public boolean isPublic() {
        return method.isPublic();
    }

    public boolean isFinal() {
        return method.isFinal();
    }

    public boolean isAbstract() {
        return method.isAbstract();
    }

    public boolean isStatic() {
        return method.isStatic();
    }

    public boolean isLet() {
        return method.isLet();
    }
}
