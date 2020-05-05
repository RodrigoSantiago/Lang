package logic.typdef;

import content.Key;
import content.Token;
import content.TypeToken;
import data.ContentFile;
import logic.Pointer;

public class Enum extends Type {
    public Enum(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.ENUM, start, end);
    }

    @Override
    public void load() {
        super.load();

        for (TypeToken pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined type");
            } else {
                cFile.erro(pTypeToken.start, "A enum cannot inherit");
            }
        }

        parent = cFile.langWrapper(this);
    }

    @Override
    public final boolean isEnum() {
        return true;
    }
}
