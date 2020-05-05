package logic;

import content.Token;
import data.ContentFile;
import data.Library;
import logic.typdef.Type;

import java.util.HashMap;
import java.util.HashSet;

public class Namespace {

    public String name;
    public Library library;
    public HashMap<Token, Type> types = new HashMap<>();
    private HashSet<ContentFile> usedBy = new HashSet<>();
    private HashSet<ContentFile> linkedBy = new HashSet<>();

    public Namespace(Library library, String name) {
        this.library = library;
        this.name = name;
    }

    public Type add(Type type) {
        return types.put(type.nameToken, type);
    }

    public void remove(Type type) {
        types.remove(type.nameToken);
    }

    public Type findType(Token tokenType) {
        return types.get(tokenType);
    }

    public void link(ContentFile cFile) {
        linkedBy.add(cFile);
    }

    public void unlink(ContentFile cFile) {
        linkedBy.remove(cFile);
    }

    public void mark(ContentFile cFile) {
        usedBy.add(cFile);
    }

    public boolean unmark(ContentFile cFile) {
        usedBy.remove(cFile);
        return usedBy.size() == 0;
    }

    @Override
    public String toString() {
        return "[namespace]"+name;
    }

    public void invalidate() {
        for (ContentFile cFile : usedBy) {
            cFile.invalidate();
        }
        for (ContentFile cFile : linkedBy) {
            cFile.invalidate();
        }
    }
}
