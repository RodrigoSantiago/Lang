package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.templates.Template;
import logic.member.Method;

public class MethodView {
    public final Pointer caller;
    public final Method method;
    private Pointer ptr;

    public MethodView(Pointer caller, Method method) {
        this.caller = caller;
        this.method = method;
        if (method.typePtr != null && !method.typePtr.isDefault()) {
            ptr = Pointer.byGeneric(method.typePtr, caller);
        } else {
            ptr = method.typePtr;
        }
    }

    public Token getName() {
        return method.nameToken;
    }

    public Pointer getType() {
        return ptr;
    }

    public Parameter getParams() {
        return method.params;
    }

    public Template getTemplate() {
        return method.template;
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
