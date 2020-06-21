package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.params.Arg;
import logic.params.Parameters;

public class ParamView {

    private Parameters params;
    private Pointer[] typePtrs;

    public ParamView(Pointer caller, Parameters params) {
        this.params = params;

        if (params.hasGeneric()) {
            typePtrs = new Pointer[params.getCount()];
            for (int i = 0; i < params.getCount(); i++) {
                if (Pointer.hasGeneric(params.getTypePtr(i), caller)) {
                    typePtrs[i] = Pointer.byGeneric(params.getTypePtr(i), caller);
                }
            }
        }
    }

    public ParamView(Pointer caller, ParamView paramView) {
        this.params = paramView.params;

        if (params.hasGeneric()) {
            typePtrs = new Pointer[paramView.getArgsCount()];
            for (int i = 0; i < paramView.getArgsCount(); i++) {
                if (Pointer.hasGeneric(paramView.getArgTypePtr(i), caller)) {
                    typePtrs[i] = Pointer.byGeneric(paramView.getArgTypePtr(i), caller);
                }
            }
        }
    }

    public boolean canOverload(ParamView other) {
        if (getArgsCount() == other.getArgsCount()) {
            boolean dif = false;
            for (int i = 0; i < getArgsCount(); i++) {
                Pointer ptrA = getArgTypePtr(i);
                Pointer ptrB = other.getArgTypePtr(i);
                if (!ptrA.isGenericEquivalent(ptrB) && !ptrA.overloadEquals(ptrB)) {
                    dif = true;
                    break;
                }
            }
            return dif;
        } else {
            return true;
        }
    }

    public boolean canOverride(ParamView other) {
        if (getArgsCount() == other.getArgsCount()) {
            for (int i = 0; i < getArgsCount(); i++) {
                Pointer ptrA = getArgTypePtr(i);
                Pointer ptrB = other.getArgTypePtr(i);
                if (!ptrA.equals(ptrB)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int getArgsCount() {
        return params.getCount();
    }

    public Pointer getArgTypePtr(int index) {
        if (typePtrs == null || typePtrs[index] == null) {
            return params.getTypePtr(index);
        }
        return typePtrs[index];
    }

    public Token getArgName(int index) {
        return params.getNameToken(index);
    }
}
