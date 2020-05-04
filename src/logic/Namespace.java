package logic;

import content.Token;
import data.Library;
import logic.typdef.Type;

import java.util.HashMap;

public class Namespace {

    public String name;
    public Library library;
    public HashMap<Token, Type> types = new HashMap<>();

    public Namespace(Library library, String name) {
        this.library = library;
        this.name = name;
    }

    public Type add(Type type) {
        return types.put(type.nameToken, type);
    }

    public Type findType(Token tokenType) {
        return types.get(tokenType);
    }

    @Override
    public String toString() {
        return "[namespace]"+name;
    }
}
