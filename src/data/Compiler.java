package data;

import content.Key;
import content.Token;
import logic.Namespace;
import logic.typdef.Type;

import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {

    private Library langLibrary;
    private HashMap<String, Library> libraries = new HashMap<>();

    private Namespace langSpace;
    private Type langObject, langWrapper, langArray, langInt, langBool;

    public void setMainFile(ContentFile main) {

    }

    public Library libAdd(Library library) {
        if (library.name.equals("lang")) {
            langLibrary = library;
        }
        library.compiler = this;

        Library old = libraries.put(library.name, library);
        if (old != null) {
            old.release();
            old.compiler = null;
        }
        return old;
    }

    public Library libRemove(String name) {
        if (name.equals("lang")) {
            langLibrary = null;
        }

        Library lib = libraries.get(name);
        if (lib != null) {
            lib.release();
            lib.compiler = null;
        }
        return lib;
    }

    public Library findLibrary(String name) {
        return libraries.get(name);
    }

    public void build() {

    }

    public Type findType(Library library, Token typeToken) {
        String name = typeToken.toString();
        int i = name.lastIndexOf("::");
        if (i > -1) {
            Namespace namespace = findNamespace(library, name.substring(0, i));
            if (namespace != null) {
                return namespace.findType(new Token(name, i + 1, name.length(), Key.WORD, false));
            }
        }
        return null;
    }

    public Namespace findNamespace(Library library, String name) {
        if (library != null) {
            Namespace namespace = library.findNamespace(name);
            if (namespace != null) {
                return namespace;
            }
        }

        for (Library lib : libraries.values()) {
            if (lib != library) {
                Namespace namespace = lib.findNamespace(name);
                if (namespace != null) {
                    return namespace;
                }
            }
        }
        return null;
    }

    public Library getLangLibrary() {
        return langLibrary;
    }

    public Type getLangType(Token typeToken) {
        if (langSpace == null) {
            langSpace = langLibrary.findNamespace(null);
        }
        return langSpace.findType(typeToken);
    }

    public Type getLangObject() {
        if (langObject == null) {
            langObject = getLangType(new Token("Object"));
        }

        return langObject;
    }

    public Type getLangWrapper() {
        if (langWrapper == null) {
            langWrapper = getLangType(new Token("Wrapper"));
        }

        return langArray;
    }

    public Type getLangArray() {
        if (langArray == null) {
            langArray = getLangType(new Token("Array"));
        }

        return langArray;
    }

    public void langInvalidate(Library library) {
        if (library == langLibrary) {
            langSpace = null;
            langObject = null;
            langArray = null;
        }
    }
}
