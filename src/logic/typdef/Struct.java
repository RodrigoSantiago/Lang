package logic.typdef;

import content.Key;
import content.Token;
import data.ContentFile;

public class Struct extends Type {
    public Struct(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.STRUCT, start, end);
    }

    @Override
    public void load() {
        super.load();
    }
}
