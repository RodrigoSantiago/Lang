package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Constructor;
import logic.typdef.Type;

public class ConstructorView {
    public final Pointer caller;
    public final Constructor constructor;
    public final ParamView paramView;

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

    public boolean isFrom(Type type) {
        return constructor.type == type;
    }

    public boolean canOverload(ConstructorView other) {
        return getParams().canOverload(other.getParams());
    }

    public boolean canOverride(ConstructorView other) {
        return getParams().canOverride(other.getParams());
    }

    public boolean canAcess(Type type) {
        return (isPrivate() && constructor.cFile == type.cFile) ||
                (!isPublic() && !isPrivate() && constructor.cFile.library == type.cFile.library) || isPublic();
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
}
