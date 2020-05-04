package data;

import content.Token;
import logic.Namespace;
import logic.typdef.Type;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Library {

    public String name;

    public ArrayList<ContentFile> cFiles = new ArrayList<>();
    public HashMap<String, Namespace> namespaces = new HashMap<>();

    public Library(String name) {
        this.name = name;
    }

    public void add(String fileName, String content) {
        cFiles.add(new ContentFile(this, fileName, content));
    }

    public void add(String fileName, InputStream input) {

    }

    public String[] update(String fileName, InputStream input) {
        return null;
    }

    Namespace getNamespace(String name) {
        Namespace namespace = namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(this, name);
            namespaces.put(name, namespace);
        }

        return namespace;
    }

    public Namespace findNamespace(String name) {
        return namespaces.get(name);
    }

    public Type findType(Token typeName) {
        return null;
    }

    /**
     * Lexer : Read TextFiles to create Tokens
     * Parser : Read Tokens to create Types, Members and Lines
     * preload : Identify cyclic types
     *
     * */
    public void read() {
        for (ContentFile cFile : cFiles) {
            cFile.read();
        }
    }

    /** Load : Prepare inheritences
     *
     * */
    public void load() {
        for (ContentFile cFile : cFiles) {
            cFile.load();
        }
    }

    /**
     * Cross : Identify Hinreritence, Members
     *
     * */
    public void cross() {

    }

    /**
     * Make : Reak Tokens to create Lines and Block
     *
     * */
    public void make() {

    }

    /**
     * Build : Transpiler the code to C++ language
     *
     * */
    public void build() {

    }

    /**
     * Release memory reference for "Make" lines and Blocks
     *
     * */
    public void releaseMake() {

    }

    public void releaseAll() {

    }
}
