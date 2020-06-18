package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Method;
import logic.typdef.Type;

public class MethodView {
    public final Pointer caller;
    public final Method method;
    public final ParamView paramView;
    public final TemplateView templateView;
    private Pointer typePtr;

    public MethodView overrided;

    public MethodView(Pointer caller, Method method) {
        this.caller = caller;
        this.method = method;
        if (method.getTypePtr() != null) {
            typePtr = Pointer.byGeneric(method.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, method.getParams());
        templateView = method.getTemplate() != null ? new TemplateView(caller, method.getTemplate()) : null;
    }

    public MethodView(Pointer caller, MethodView methodView) {
        this.caller = caller;
        this.method = methodView.method;
        if (methodView.typePtr != null) {
            typePtr = Pointer.byGeneric(methodView.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, methodView.getParams());
        templateView = method.getTemplate() != null ? new TemplateView(caller, method.getTemplate()) : null;
        this.overrided = methodView.overrided;
    }

    public boolean isFrom(Type type) {
        return method.type == type;
    }

    public boolean canOverload(MethodView other) {
        if ((method.getTemplate() != null || other.method.getTemplate() != null) &&
                getParams().getArgsCount() == other.getParams().getArgsCount()) {
            return false;
        }
        return getParams().canOverload(other.getParams());
    }

    public boolean canOverride(MethodView other) {
        if (method.getTemplate() != null || other.method.getTemplate() != null) {
            return false;
        }
        if (getTypePtr().equals(other.getTypePtr())) {
            return getParams().canOverride(other.getParams());
        }

        return false;
    }

    public boolean canAcess(Type type) {
        return (isPrivate() && method.cFile == type.cFile) ||
                (!isPublic() && !isPrivate() && method.cFile.library == type.cFile.library) || isPublic();
    }

    public void markOverrided(MethodView mw) {
        overrided = mw;
    }

    public Method original() {
        if (overrided != null) return overrided.original();
        return method;
    }

    public Token getName() {
        return method.getName();
    }

    public Pointer getTypePtr() {
        return typePtr != null ? typePtr : method.getTypePtr();
    }

    public ParamView getParams() {
        return paramView;
    }

    public TemplateView getTemplate() {
        return templateView;
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
