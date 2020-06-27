package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;

public class Enum extends Type {

    public Enum(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.ENUM, start, end);
    }

    @Override
    public void load() {
        if (isLoaded) return;
        isLoaded = true;
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this, false);
            if (parent != null) {
                cFile.erro(pTypeToken.start, "An Enum cannot inherit", this);
            }
        }

        parent = cFile.langWrapperPtr(this);
    }

    @Override
    public final boolean isEnum() {
        return true;
    }
}
