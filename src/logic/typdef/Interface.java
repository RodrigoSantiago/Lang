package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.member.Indexer;
import logic.member.Method;
import logic.member.Property;

public class Interface extends Type {

    public Interface(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.INTERFACE, start, end);
    }

    @Override
    public void load() {
        if (isLoaded) return;
        isLoaded = true;
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this, false);
            if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined type", this);
            } else {
                cFile.erro(pTypeToken.start, "An Interface cannot inherit", this);
            }
        }

        this.parent = cFile.langObjectPtr();
        this.parents.add(0, parent);
        this.parentTokens.add(0, nameToken);
    }

    @Override
    public final boolean isInterface() {
        return true;
    }

    @Override
    public void add(Method method) {
        if (method.isStatic() || method.toAbstract()) {
            super.add(method);
        }
    }

    @Override
    public void add(Indexer indexer) {
        if (!indexer.isStatic()) {
            indexer.toAbstract();
        }
        super.add(indexer);
    }

    @Override
    public void add(Property property) {
        if (!property.isStatic()) {
            property.toAbstract();
        }
        super.add(property);
    }
}
