package logic.typdef;

import content.Key;
import content.Token;
import data.ContentFile;

public class Interface extends Type {
    public Interface(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.INTERFACE, start, end);
    }

    @Override
    public void load() {
        super.load();

        if (parents.size() == 0) {
            parents.add(cFile.langObject());
        }
    }
}
