package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
import logic.Pointer;

public class Class extends Type {
    public Class(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.CLASS, start, end);
    }

    @Override
    public void load() {
        super.load();

        for (TokenGroup pTypeToken : parentTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if (parents.contains(parent)) {
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
                } else {
                    cFile.erro(pTypeToken.start, "A class cannot have multiple class parents");
                }
            } else if (parent.type.isInterface()) {
                this.parents.add(parent);
            } else {
                cFile.erro(pTypeToken.start, "Undefined type");
            }
        }

        if (this.parent == null && cFile.langObject().type != this) {
            this.parent = cFile.langObject();
        }

        if (contentToken != null && contentToken.getChild() != null) {
            Parser parser = new Parser();
            parser.parseMembers(this, contentToken.getChild(), contentToken.getLastChild());
        }
    }

    @Override
    public void build(CppBuilder cBuilder) {
        cBuilder.toHeader();
        cBuilder.add("\\\\").add(fileName).add(".h").ln();

        cBuilder.add("")
                .add("#ifndef H_").add(fileName).ln()
                .add("#define H_").add(fileName).ln()
                .add("#include \"langCore.h\"").ln()
                .ln();

        cBuilder.add(template)
                .add("class ").add(nameToken).add(" : public ").parent(parent);
        for (Pointer parent : parents) {
            cBuilder.add(", public ").parent(parent);
        }
        cBuilder.add(" {").ln()
                .add("public :").ln();
        cBuilder.add("};").ln();
        cBuilder.add("#endif").ln();
    }

    @Override
    public final boolean isClass() {
        return true;
    }
}
