package logic.member.view;

import data.ContentFile;
import logic.Pointer;
import logic.member.Constructor;

public class ConstructorView {
    public static final ConstructorView structInit = new ConstructorView();

    public final Pointer caller;
    public final Constructor constructor;

    private final ParamView paramView;

    ConstructorView() {
        caller = null;
        constructor = null;
        paramView = null;
    }

    public ConstructorView(Pointer caller, Constructor constructor) {
        this.caller = caller;
        this.constructor = constructor;
        paramView = new ParamView(caller, constructor.getParams());
    }

    public ConstructorView(Pointer caller, ConstructorView constructorView) {
        this.caller = caller;
        this.constructor = constructorView.constructor;
        paramView = new ParamView(caller, constructorView.getParams());
    }

    public ContentFile getFile() {
        return constructor.type.cFile;
    }

    public ParamView getParams() {
        return paramView;
    }

    public boolean isPrivate() {
        return constructor.isPrivate();
    }

    public boolean isPublic() {
        return constructor.isPublic();
    }

    public boolean isDefault() {
        return constructor.isDefault();
    }

    public boolean isStatic() {
        return constructor.isStatic();
    }

    public boolean isEmpty() {
        return constructor.getParams().isEmpty();
    }

    public boolean isCopy() {
        return constructor.getParams().getCount() == 1 &&
                constructor.getParams().getTypePtr(0).equals(constructor.type.self);
    }
}
