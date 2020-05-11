package logic.params;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.GenericOwner;

import java.util.ArrayList;

public class Parameters {

    public final ContentFile cFile;
    public ArrayList<Arg> args = new ArrayList<>();
    private boolean hasGeneric;

    public Parameters(ContentFile cFile, Token parameters) {
        this.cFile = cFile;
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
                cFile.erro(token, "Unexpected token");
            }
            token = next;
        }

        if (state != 0) {
            cFile.erro(end != null ? end : parameters, "Unexpected end of tokens");
        }
    }

    public boolean load(GenericOwner owner) {
        for (Arg arg : args) {
            arg.type = cFile.getPointer(arg.typeToken.start, arg.typeToken.end, null, owner);
            if (!hasGeneric && arg.type != null && arg.type.hasGeneric()) {
                hasGeneric = true;
            }
        }
        return true;
    }

    public boolean hasGeneric() {
        return hasGeneric;
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
                if (!arg.type.equals(otherArg.type)) return false;
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
}
