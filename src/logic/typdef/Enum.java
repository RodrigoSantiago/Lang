package logic.typdef;

import content.Key;
import content.Token;
import data.ContentFile;

public class Enum extends Type {
    public Enum(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.ENUM, start, end);
    }

    @Override
    public void load() {
        super.load();
    }
}
