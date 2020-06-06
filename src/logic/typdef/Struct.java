package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.member.Variable;

import java.util.ArrayList;

public class Struct extends Type {

    public Struct(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.STRUCT, start, end);
    }

    @Override
    public void load() {
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined wrapper");
            } else if (parent.typeSource != null) {
                cFile.erro(pTypeToken.start, "A struct could not have it's generic as wrapper");
            } else if (parent.type.isClass()) {
                if (this.parent == null) {
                    this.parent = parent;
                } else {
                    cFile.erro(pTypeToken.start, "A struct could not have multiple wrappers");
                }
            } else{
                cFile.erro(pTypeToken.start, "A struct wrapper should be a class");
            }
        }

        if (parent == null) {
            parent = cFile.langWrapper(this);
        }

        Pointer[] p = template == null ? null : new Pointer[template.generics.size()];
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                p[i] = template.generics.get(i).typePtr;
            }
        }
        self = new Pointer(this, p);

        if (contentToken != null && contentToken.getChild() != null) {
            Parser parser = new Parser();
            parser.parseMembers(this, contentToken.getChild(), contentToken.getLastChild());
        }
    }

    @Override
    public final boolean isStruct() {
        return true;
    }
}
