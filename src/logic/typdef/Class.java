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
            Pointer pointer = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this, false);
            if (pointer == null) continue;
            if (pointer.typeSource != null) {
                cFile.erro(pTypeToken.start, "Cannot inherit it's template", this);
            } else if (pointer.type == null) {
                cFile.erro(pTypeToken.start, "Undefined parent", this);
            } else if ((pointer.type.isPrivate() && pointer.type.cFile != cFile)
                    || (!pointer.type.isPublic() && pointer.type.cFile.library != cFile.library)) {
                cFile.erro(pTypeToken.start, "Invalid acess permisison", this);
            } else if (parents.contains(pointer)) {
                cFile.erro(pTypeToken.start, "Repeated parent", this);
            } else if (this.parent != null && this.parent.isDerivedFrom(pointer) > 0) {
                cFile.erro(pTypeToken.start, "Repeated parent", this);
            } else if (pointer.type.isFinal()) {
                cFile.erro(pTypeToken.start, "A Class could not inherit from a Final Type", this);
            } else if (pointer.type.isClass()) {
                if (this.parent == null) {
                    if (parents.size() > 0) {
                        cFile.erro(pTypeToken.start, "The class parent must come before interfaces", this);
                    }
                    this.parent = pointer;
                    this.parents.add(0, pointer);
                    this.parentTokens.add(0, pTypeToken.start);
                } else {
                    cFile.erro(pTypeToken.start, "A class cannot have multiple parent Classes", this);
                }
            } else if (pointer.type.isInterface()) {
                this.parents.add(pointer);
                this.parentTokens.add(pTypeToken.start);
            } else {
                cFile.erro(pTypeToken.start, "Undefined type", this);
            }
        }

        if (this.parent == null && cFile.langObjectPtr().type != this) {
            this.parent = cFile.langObjectPtr();
            this.parents.add(0, parent);
            this.parentTokens.add(0, nameToken);
        }
    }

    @Override
    public final boolean isClass() {
        return true;
    }
}
