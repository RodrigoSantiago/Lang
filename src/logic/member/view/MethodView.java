package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Method;
import logic.stack.expression.Expression;
import logic.templates.Generic;
import logic.typdef.Type;

import java.util.ArrayList;

public class MethodView {

    private Pointer typePtr;

    public final Pointer caller;
    public final Method method;

    private final ParamView paramView;
    private final TemplateView templateView;

    private MethodView overrided;
    private Pointer[] captureList;

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


    public MethodView(Method functionMethod, Pointer caller) {
        this.caller = caller;
        this.method = functionMethod;
        this.paramView = null;
        this.templateView = null;
        this.typePtr = caller.pointers[0];
    }

    public MethodView(Pointer[] captureList, MethodView methodView) {
        this.caller = methodView.caller;
        this.method = methodView.method;
        this.templateView = methodView.templateView;
        this.captureList = captureList;

        typePtr = method.getTypePtr();
        this.paramView = new ParamView(templateView.template, captureList, methodView.paramView);
        for (int j = 0; j < templateView.getGenCount(); j++) {
            typePtr = Pointer.apply(templateView.getGeneric(j), captureList[j], typePtr);
        }
    }

    public static MethodView byTemplate(ArrayList<Expression> expressions, MethodView methodView) {
        ParamView paramView = methodView.getParams();
        TemplateView templateView = methodView.getTemplate();
        Pointer[] captureList = new Pointer[templateView.getGenCount()];

        for (int k = 0; k < templateView.getGenCount(); k++) {
            Generic gen = templateView.getGeneric(k);
            captureList[k] = gen.typePtr;

            loop1 : for (int i = 0; i < expressions.size(); i++) {
                Pointer input = expressions.get(i).getNaturalPtr(null);
                for (int j = 0; j < paramView.getArgsCount(); j++) {
                    Pointer original = paramView.getArgTypePtr(i);
                    Pointer capture = Pointer.capture(gen, original, input);
                    if (capture != null) {
                        captureList[k] = capture;
                        break loop1;
                    }
                }
            }
        }

        return new MethodView(captureList, methodView);
    }

    public Pointer[] getCaptureList() {
        return captureList;
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

    public boolean isTemplateReturnEntry() {
        return method.isTemplateReturnEntry();
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

    public Type getType() {
        return method.type;
    }

    @Override
    public String toString() {
        return "MethodView{" +caller + method.getName() + "(" + paramView + ") : " +getTypePtr()+ "}";
    }
}
