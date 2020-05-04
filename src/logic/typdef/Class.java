package logic.typdef;

import content.Key;
import content.Token;
import data.ContentFile;

public class Class extends Type {
    public Class(ContentFile cFile, Token start, Token end) {
        super(cFile, Key.CLASS, start, end);
    }
}
