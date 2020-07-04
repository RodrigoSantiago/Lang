package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.params.Parameters;
import logic.stack.expression.CallGroup;
import logic.stack.expression.Expression;
import logic.templates.Template;

import java.util.ArrayList;

public class ParamView {

    private Pointer[] typePtrs;

    public final Parameters params;

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

    public ParamView(Template template, Pointer[] captureList, ParamView paramView) {
        this.params = paramView.params;

        typePtrs = new Pointer[paramView.getArgsCount()];
        for (int i = 0; i < paramView.getArgsCount(); i++) {
            typePtrs[i] = paramView.getArgTypePtr(i);
            for (int j = 0; j < template.getCount(); j++) {
                typePtrs[i] = Pointer.apply(template.getGeneric(j), captureList[j], typePtrs[i]);
            }
        }
    }

    public boolean canOverload(ParamView other) {
        if (getArgsCount() == other.getArgsCount()) {
            boolean dif = false;
            for (int i = 0; i < getArgsCount(); i++) {
                Pointer ptrA = getArgTypePtr(i);
                Pointer ptrB = other.getArgTypePtr(i);
                if (!ptrA.isEquivalent(ptrB)) {
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

    public int verifyArguments(int[] closer, int[] result, ArrayList<Expression> arguments, boolean exists) {
        for (int i = 0; i < params.getCount(); i++) {
            int dist = arguments.get(i).verify(getArgTypePtr(i));
            if (dist == 0) return 0; // can handle (-2)?
            result[i] = dist;
        }
        boolean equal = exists;
        boolean alredyCloser = false;
        boolean resultCloser = !exists;
        if (exists) {
            for (int i = 0; i < closer.length; i++) {
                if (result[i] < closer[i]) {
                    resultCloser = true;
                    equal = false;
                } else if (closer[i] < result[i]) {
                    alredyCloser = true;
                    equal = false;
                }
            }
        }
        if ((alredyCloser && resultCloser) || equal) {
            for (int i = 0; i < closer.length; i++) {
                closer[i] = Math.min(result[i], closer[i]);
            }
            return 2;
        } else if (resultCloser) {
            for (int i = 0; i < closer.length; i++) {
                closer[i] = result[i];
            }
            return 1;
        } else {
            return 0;
        }
    }

    public int verifyArguments(int[] closer, int[] result, CallGroup left, CallGroup right, boolean exists) {
        int dist = left.verify(getArgTypePtr(0));
        if (dist == 0) return 0; // can handle [negatives]?
        result[0] = dist;

        dist = right.verify(getArgTypePtr(1));
        if (dist == 0) return 0; // can handle [negatives]?
        result[1] = dist;

        boolean equal = true;
        boolean alredyCloser = false;
        boolean resultCloser = !exists;
        if (exists) {
            for (int i = 0; i < closer.length; i++) {
                if (result[i] < closer[i]) {
                    resultCloser = true;
                    equal = false;
                } else if (closer[i] < result[i]) {
                    alredyCloser = true;
                    equal = false;
                }
            }
        }
        if ((alredyCloser && resultCloser) || equal) {
            for (int i = 0; i < closer.length; i++) {
                closer[i] = Math.min(result[i], closer[i]);
            }
            return 2;
        } else if (resultCloser) {
            for (int i = 0; i < closer.length; i++) {
                closer[i] = result[i];
            }
            return 1;
        } else {
            return 0;
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

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < getArgsCount(); i++) {
            if (i > 0) s+= ", ";
            s+= getArgTypePtr(i);
        }
        return s;
    }
}
