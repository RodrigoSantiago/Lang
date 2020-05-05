package logic.typdef;

import content.Key;
import content.Token;
import content.TypeToken;
import data.ContentFile;
import logic.Pointer;

public class Interface extends Type {
    public Interface(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.INTERFACE, start, end);
    }

    @Override
    public void load() {
        super.load();

        for (TypeToken pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if (parents.contains(parent)) {
                cFile.erro(pTypeToken.start, "Repeated parent");
            } else if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined parent");
            } else if (parent.typeSource != null) {
                cFile.erro(pTypeToken.start, "A interface could not inherit it's generic");
            } else if (parent.type.isClass()) {
                cFile.erro(pTypeToken.start, "A interface could not inherit from a Class");
            } else if (parent.type.isInterface()) {
                this.parents.add(parent);
            } else if (parent.type.isFinal()) {
                cFile.erro(pTypeToken.start, "A Interface could not inherit from a Final Type");
            } else {
                cFile.erro(pTypeToken.start, "Undefined type");
            }
        }

        this.parent = cFile.langObject();
    }

    @Override
    public final boolean isInterface() {
        return true;
    }
}
