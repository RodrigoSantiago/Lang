package logic.params;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;

import java.util.ArrayList;

public class Parameters {

    public final ContentFile cFile;
    public ArrayList<Arg> args = new ArrayList<>();

    public Parameters(ContentFile cFile, Token param) {
        this.cFile = cFile;
        Token start = param.getChild();
        Token end = param.getLastChild();

        boolean isLet = false;
        Token nameToken = null;
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
                nameToken = token;
                state = 3;
            } else if (state == 3 && (token.key == Key.SEMICOLON || next == end)) {
                args.add(new Arg(nameToken, typeToken, isLet));
                nameToken = null;
                typeToken = null;
                isLet = false;
                state = 0;
            } else if (token.key == Key.SEMICOLON || next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
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
                if (!arg.typePointer.equals(otherArg.typePointer)) return false;
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

    public boolean load() {
        return false;
    }
}
