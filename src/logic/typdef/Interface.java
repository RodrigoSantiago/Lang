package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
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
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if ((parent.type.isPrivate() && parent.type.cFile != cFile)
                    || !parent.type.isPublic() && parent.type.cFile.library != cFile.library) {
                cFile.erro(pTypeToken.start, "Invalid acess permisison");
            } else if (parents.contains(parent)) {
                cFile.erro(pTypeToken.start, "Repeated parent");
            } else if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined parent");
            } else if (parent.typeSource != null) {
                cFile.erro(pTypeToken.start, "A interface could not inherit it's generic");
            } else if (parent.type.isFinal()) {
                cFile.erro(pTypeToken.start, "A Interface could not inherit from a Final Type");
            } else if (parent.type.isClass()) {
                cFile.erro(pTypeToken.start, "A interface could not inherit from a Class");
            } else if (parent.type.isInterface()) {
                this.parents.add(parent);
                this.parentTokens.add(pTypeToken.start);
            } else {
                cFile.erro(pTypeToken.start, "Undefined type");
            }
        }

        this.parent = cFile.langObject();
        this.parents.add(0, parent);
        this.parentTokens.add(0, nameToken);

        Pointer[] p = template == null ? null : new Pointer[template.generics.size()];
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                p[i] = template.generics.get(i).ptr;
            }
        }
        self = new Pointer(this, p);

        if (contentToken != null && contentToken.getChild() != null) {
            Parser parser = new Parser();
            parser.parseMembers(this, contentToken.getChild(), contentToken.getLastChild());
        }
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
