package data;

import content.Key;
import content.Token;
import logic.Namespace;
import logic.typdef.Type;

import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {

    private static Library langLibrary;
    private static HashMap<String, Library> libraries = new HashMap<>();

    private static Namespace langSpace;
    private static Type langObject, langArray, langInt, langBool;

    public static void setMainFile(ContentFile main) {

    }

    public static Library libAdd(Library library) {
        if (library.name.equals("lang")) {
            langLibrary = library;
        }

        return libraries.put(library.name, library);
    }

    public static Library libRemove(String name) {
        if (name.equals("lang")) {
            langLibrary = null;
        }

        Library lib = libraries.get(name);
        if (lib != null) {
            lib.release();
        }
        return lib;
    }

    public static Library findLibrary(String name) {
        return libraries.get(name);
    }

    public static void build() {

    }

    public static Type findType(Library library, Token typeToken) {
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

    public static Namespace findNamespace(Library library, String name) {
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

    public static Library getLangLibrary() {
        return langLibrary;
    }

    public static Type getLangType(Token typeToken) {
        if (langSpace == null) {
            langSpace = langLibrary.findNamespace(null);
        }
        return langSpace.findType(typeToken);
    }

    public static Type getLangObject() {
        if (langObject == null) {
            langObject = getLangType(new Token("Object"));
        }

        return langObject;
    }

    public static Type getLangArray() {
        if (langArray == null) {
            langArray = getLangType(new Token("Array"));
        }

        return langArray;
    }

    public static void langInvalidate(Library library) {
        if (library == langLibrary) {
            langSpace = null;
            langObject = null;
            langArray = null;
        }
    }
}
