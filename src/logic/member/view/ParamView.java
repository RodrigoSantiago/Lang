package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.params.Arg;
import logic.params.Parameters;

public class ParamView {

    private Parameters params;
    private Pointer caller;
    private Pointer[] types;

    public ParamView(Parameters params, Pointer caller) {
        this.params = params;
        this.caller = caller;

        if (params.hasGeneric()) {
            types = new Pointer[params.args.size()];
            for (int i = 0; i < params.args.size(); i++) {
                Arg arg = params.args.get(i);
                if (Pointer.hasGeneric(arg.type, caller)) {
                    types[i] = Pointer.byGeneric(arg.type, caller);
                }
            }
        }
    }

    public ParamView(ParamView paramView, Pointer caller) {
        this.params = paramView.params;
        this.caller = caller;

        if (params.hasGeneric()) {
            types = new Pointer[paramView.getArgsCount()];
            for (int i = 0; i < paramView.getArgsCount(); i++) {
                if (Pointer.hasGeneric(paramView.getArgType(i), caller)) {
                    types[i] = Pointer.byGeneric(paramView.getArgType(i), caller);
                }
            }
        }
    }

    public boolean canOverload(ParamView other) {
        if (getArgsCount() == other.getArgsCount()) {
            boolean dif = false;
            for (int i = 0; i < getArgsCount(); i++) {
                Pointer ptrA = getArgType(i);
                Pointer ptrB = other.getArgType(i);
                if (!ptrA.hasGeneric() && !ptrB.hasGeneric() && !ptrA.equals(ptrB)) {
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
                Pointer ptrA = getArgType(i);
                Pointer ptrB = other.getArgType(i);
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
        return params.args.size();
    }

    public Pointer getArgType(int index) {
        if (types == null || types[index] == null) {
            return params.args.get(index).type;
        }
        return types[index];
    }

    public Token getArgName(int index) {
        return params.args.get(index).nameToken;
    }
}
