package logic.typdef;

import content.Key;
import content.Parser;
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
        super.load();

        for (TokenGroup pTypeToken : parentTypeTokens) {
            Pointer parent = cFile.getPointer(pTypeToken.start, pTypeToken.end, this, this);
            if (parent.type == null) {
                cFile.erro(pTypeToken.start, "Undefined type");
            } else {
                cFile.erro(pTypeToken.start, "A enum cannot inherit");
            }
        }

        parent = cFile.langWrapper(this);
        self = new Pointer(this, null);

        if (contentToken != null && contentToken.getChild() != null) {
            Parser parser = new Parser();
            parser.parseMembers(this, contentToken.getChild(), contentToken.getLastChild());
        }
    }

    @Override
    public final boolean isEnum() {
        return true;
    }
}
