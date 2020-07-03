package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.typdef.Type;

public abstract  class Member {
    public final ContentFile cFile;
    public final Type type;
    public Token token;

    boolean isPrivate, isPublic, isAbstract, isFinal, isStatic, isLet, isDefault;

    public Member(Type type, ContentFile cFile) {
        this.type = type;
        this.cFile = cFile;
    }

    public boolean load() {
        return false;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isLet() {
        return isLet;
    }

    public boolean isDefault() {
        return isDefault;
    }

    void readModifier(ContentFile cFile, Token token, boolean hasPrivate, boolean hasPublic,
                      boolean hasAbstract, boolean hasFinal, boolean hasStatic, boolean hasLet, boolean hasDefault) {
        if ((token.key == Key.PUBLIC && hasPublic) || (token.key == Key.PRIVATE && hasPrivate)) {
            if (isPublic || isPrivate) {
                cFile.erro(token, "Repeated acess modifier", this);
            } else {
                isPublic = (token.key == Key.PUBLIC);
                isPrivate = (token.key == Key.PRIVATE);
            }
        } else if ((token.key == Key.ABSTRACT && hasAbstract) || (token.key == Key.FINAL && hasFinal)) {
            if (isAbstract || isFinal) {
                cFile.erro(token, "Repeated modifier", this);
            } else {
                isAbstract = (token.key == Key.ABSTRACT);
                isFinal = (token.key == Key.FINAL);
            }
        } else if (token.key == Key.STATIC && hasStatic) {
            if (isStatic) {
                cFile.erro(token, "Repeated modifier", this);
            } else {
                isStatic = true;
            }
        } else if (token.key == Key.LET && hasLet) {
            if (isLet) {
                cFile.erro(token, "Repeated modifier", this);
            } else {
                isLet = true;
            }
        } else if (token.key == Key.DEFAULT && hasDefault) {
            if (isDefault) {
                cFile.erro(token, "Repeated modifier", this);
            } else {
                isDefault = true;
            }
        } else {
            cFile.erro(token, "Unexpected modifier", this);
        }
    }
}
