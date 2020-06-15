package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;

public class Class extends Type {
    public Class(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.CLASS, start, end);
    }

    @Override
    public void load() {
        if (isLoaded) return;
        isLoaded = true;
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this, false);
            if ((parent.type.isPrivate() && parent.type.cFile != cFile)
                    || (!parent.type.isPublic() && parent.type.cFile.library != cFile.library)) {
                cFile.erro(pTypeToken.start, "Invalid acess permisison");
            } else if (parents.contains(parent)) {
                cFile.erro(pTypeToken.start, "Repeated parent");
            } else if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined parent");
            } else if (parent.typeSource != null) {
                cFile.erro(pTypeToken.start, "A class could not inherit it's generic");
            } else if (parent.type.isFinal()) {
                cFile.erro(pTypeToken.start, "A class could not inherit from a Final Type");
            } else if (parent.type.isClass()) {
                if (this.parent == null) {
                    if (parents.size() > 0) {
                        cFile.erro(pTypeToken.start, "The class parent must come before interfaces");
                    }
                    this.parent = parent;
                    this.parents.add(0, parent);
                    this.parentTokens.add(0, pTypeToken.start);
                } else {
                    cFile.erro(pTypeToken.start, "A class cannot have multiple class parents");
                }
            } else if (parent.type.isInterface()) {
                this.parents.add(parent);
                this.parentTokens.add(pTypeToken.start);
            } else {
                cFile.erro(pTypeToken.start, "Undefined type");
            }
        }

        if (this.parent == null && cFile.langObject().type != this) {
            this.parent = cFile.langObject();
            this.parents.add(0, parent);
            this.parentTokens.add(0, nameToken);
        }
    }

    @Override
    public final boolean isClass() {
        return true;
    }
}
