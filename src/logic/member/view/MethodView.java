package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.templates.Template;
import logic.member.Method;

public class MethodView {
    public final Pointer caller;
    public final Method method;
    public final ParamView paramView;
    private Pointer ptr;

    public MethodView(Pointer caller, Method method) {
        this.caller = caller;
        this.method = method;
        if (method.typePtr != null) {
            ptr = Pointer.byGeneric(method.typePtr, caller);
        }
        paramView = new ParamView(method.params, caller);
    }

    public MethodView(Pointer caller, MethodView methodView) {
        this.caller = caller;
        this.method = methodView.method;
        if (methodView.ptr != null) {
            ptr = Pointer.byGeneric(methodView.getType(), caller);
        }
        paramView = new ParamView(methodView.getParams(), caller);
    }

    public Token getName() {
        return method.nameToken;
    }

    public Pointer getType() {
        return ptr != null ? ptr : method.typePtr;
    }

    public ParamView getParams() {
        return paramView;
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
