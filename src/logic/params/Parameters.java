package logic.params;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.GenericOwner;
import logic.Pointer;
import logic.member.view.ParamView;

import java.util.ArrayList;

public class Parameters {

    private final ContentFile cFile;
    private final ArrayList<Arg> args = new ArrayList<>();
    private boolean hasGeneric;

    public final Token token;

    public Parameters(ContentFile cFile, Token parameters) {
        this.cFile = cFile;
        this.token = parameters;
        Token start = parameters.getChild();
        Token end = parameters.getLastChild();

        boolean isLet = false;
        TokenGroup typeToken = null;

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.LET) {
                isLet = true;
                state = 1;
            } else if ((state == 0 || state == 1) && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                args.add(new Arg(token, typeToken, isLet));
                isLet = false;
                typeToken = null;

                if (next == end) {
                    state = 0;
                } else {
                    state = 3;
                }
            } else if (state == 3 && token.key == Key.COMMA) {
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }

            if (next == end && state != 0) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    public boolean load(GenericOwner owner) {
        for (Arg arg : args) {
            arg.typePtr = cFile.getPointer(arg.typeToken.start, arg.typeToken.end, null, owner, arg.isLet);
            if (!hasGeneric && arg.typePtr != null && arg.typePtr.hasGeneric()) {
                hasGeneric = true;
            }
        }
        return true;
    }

    public int getCount() {
        return args.size();
    }

    public Pointer getTypePtr(int pos) {
        return args.get(pos).typePtr;
    }

    public Token getNameToken(int pos) {
        return args.get(pos).nameToken;
    }

    public boolean hasGeneric() {
        return hasGeneric;
    }

    public boolean isEmpty() {
        return args.size() == 0;
    }

    public boolean canOverload(Parameters other) {
        if (other.args.size() == args.size()) {
            boolean dif = false;
            for (int i = 0; i < args.size(); i++) {
                Arg argA = args.get(i);
                Arg argB = other.args.get(i);
                if (!argA.typePtr.isGenericEquivalent(argB.typePtr) && !argA.typePtr.overloadEquals(argB.typePtr)) {
                    dif = true;
                    break;
                }
            }
            return dif;
        } else {
            return true;
        }
    }

    public boolean canOverride(Parameters other) {
        if (args.size() == other.args.size()) {
            for (int i = 0; i < other.args.size(); i++) {
                Pointer ptrA = args.get(i).typePtr;
                Pointer ptrB = other.args.get(i).typePtr;
                if (!ptrA.equals(ptrB)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean canOverride(ParamView other) {
        if (args.size() == other.getArgsCount()) {
            for (int i = 0; i < args.size(); i++) {
                Pointer ptrA = args.get(i).typePtr;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Parameters) {
            Parameters other = (Parameters) obj;
            if (other.args.size() != args.size()) {
                return false;
            }
            for (int i = 0; i < args.size(); i++) {
                Arg arg = args.get(i);
                Arg otherArg = other.args.get(i);
                if (!arg.typePtr.equals(otherArg.typePtr)) return false;
                if (arg.isLet != otherArg.isLet) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) str.append(", ");
            str.append(args.get(i));
        }
        return str + ")";
    }

    public boolean equals(Parameters other) {
        if (other.args.size() != args.size()) return false;
        for (int i = 0; i < args.size(); i++) {
            Arg argA = args.get(i);
            Arg argB = other.args.get(i);
            if (!argA.typePtr.equals(argB.typePtr)) {
                return false;
            }
        }
        return true;
    }
}
