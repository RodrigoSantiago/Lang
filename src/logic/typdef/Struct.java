package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;

public class Struct extends Type {

    public Struct(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.STRUCT, start, end);
    }

    @Override
    public void load() {
        if (isLoaded) return;
        isLoaded = true;
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this, false);
            if (parent == null) continue;
            if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined Wrapper", this);
            } else if (parent.typeSource != null) {
                cFile.erro(pTypeToken.start, "A struct could not have it's template as Wrapper", this);
            } else if (parent.type.isClass()) {
                if (this.parent == null) {
                    this.parent = parent;
                } else {
                    cFile.erro(pTypeToken.start, "A struct could not have multiple Wrappers", this);
                }
            } else {
                cFile.erro(pTypeToken.start, "A struct Wrapper should be a Class", this);
            }
        }

        if (parent == null) {
            parent = cFile.langWrapperPtr(this);
        }
    }

    @Override
    public final boolean isStruct() {
        return true;
    }
}
