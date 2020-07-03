package data;

import builder.CppBuilder;
import logic.Namespace;
import logic.member.Method;
import logic.typdef.Type;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Library {

    public final String name;
    public final long version;

    public HashMap<String, ContentFile> cFiles = new HashMap<>();
    public HashMap<String, Dependency> dependencies = new HashMap<>();
    public HashMap<String, Namespace> namespaces = new HashMap<>();

    public ArrayList<ContentFile> invalidate = new ArrayList<>();
    public HashMap<String, String> adds = new HashMap<>();
    public HashSet<String> subs = new HashSet<>();

    Compiler compiler;
    ContentFile mainFile;
    private File srcDir, objDir;
    private boolean loaded, mainErroRelated;

    public Library(String name, long version) {
        this.name = name;
        this.version = version;
    }

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public File getSrcDir() {
        return srcDir;
    }

    public void setObjDir(File objDir) {
        this.objDir = objDir;
    }

    public File getObjDir() {
        return objDir;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public Dependency dependencyAdd(Dependency dependency) {
        return dependencies.put(dependency.name, dependency);
    }

    public Dependency dependencyRemove(String name) {
        return dependencies.remove(name);
    }

    public boolean isDependency(String name) {
        return dependencies.containsKey(name);
    }

    public boolean isDependency(Library library) {
        return dependencies.containsKey(library.name);
    }

    public void fileAdd(String fileName, String content) {
        ContentFile cFile = cFiles.get(fileName);
        if (cFile != null) {
            cFile.invalidate();
        }

        adds.put(fileName, content);
        subs.add(fileName);
    }

    public void fileRemove(String fileName) {
        ContentFile cFile = cFiles.get(fileName);
        if (cFile != null) {
            cFile.invalidate();
        }

        subs.add(fileName);
        adds.remove(fileName);
    }

    Namespace getNamespace(String name) {
        if (name == null || name.isEmpty()) {
            name = this.name;
        }

        Namespace namespace = namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(this, name);
            namespaces.put(name, namespace);
        }

        return namespace;
    }

    void delNamespace(String name) {
        namespaces.remove(name);
    }

    public Namespace findNamespace(String name) {
        return namespaces.get(name == null ? this.name : name);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean setMainFile(ContentFile mainFile) {
        if (this.mainFile == null) {
            this.mainFile = mainFile;
            return true;
        } else {
            if (!mainErroRelated) {
                mainErroRelated = true;
                mainFile.erro(mainFile.mainMethodToken, "Duplicated Main Method", this);
            }
            return false;
        }
    }

    /**
     * Lexer : Read TextFiles to create Tokens
     * Parser : Read Tokens to create Types, Members and Lines
     * preload : Identify cyclic types
     *
     * */
    public void read() {
        unload();

        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 0) {
                cFile.read();
            }
        }
    }

    /**
     * preload : Prepare Templates
     *
     * */
    public void preload() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 1) {
                cFile.preload();
            }
        }
    }

    /**
     * Load : Prepare inheritences
     *
     * */
    public void load() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 2) {
                cFile.load();
            }
        }
    }

    /**
     * Internal : Read members
     *
     * */
    public void internal() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 3) {
                cFile.internal();
            }
        }
    }

    /**
     * Cross : Identify Hinreritence, Members
     *
     * */
    public void cross() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 4) {
                cFile.cross();
            }
        }
    }

    /**
     * Make : Read Tokens to create Lines and Block
     *
     * */
    public void make() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.getState() == 5) {
                cFile.make();
            }
        }
    }

    /**
     * Build : Transpiler the code to C++ language
     *
     * */
    public void build(CppBuilder cBuilder) {
        for (ContentFile cFile : cFiles.values()) {
            for (Type type : cFile.types) {
                if (type.isLangBase()) {
                    continue;
                }
                cBuilder.reset(type);
                type.build(cBuilder);

                File header = new File(srcDir, type.fileName + ".h");
                try (PrintWriter pw = new PrintWriter(header)) {
                    pw.print(cBuilder.getHeader());
                } catch (Exception e) {
                    cFile.erro(0, 0, "Unable to write to file[" + header + "]", this);
                }

                File source = new File(srcDir, type.fileName + ".cpp");
                try (PrintWriter pw = new PrintWriter(source)) {
                    pw.print(cBuilder.getSource());
                } catch (Exception e) {
                    cFile.erro(0, 0, "Unable to write to file[" + source + "]", this);
                }

                if (type.hasGenericFile()) {
                    File generic = new File(srcDir, type.fileName + ".hpp");
                    try (PrintWriter pw = new PrintWriter(generic)) {
                        pw.print(cBuilder.getGeneric());
                    } catch (Exception e) {
                        cFile.erro(0, 0, "Unable to write to file[" + generic + "]", this);
                    }
                }
            }
        }
    }

    public void buildMain(CppBuilder cBuilder) {
        if (mainFile != null) {
            cBuilder.reset(getCompiler().getLangObject());
            Method method = mainFile.getMainMethod();
            method.buildMain(cBuilder);

            File main = new File(srcDir, "main.cpp");
            try (PrintWriter pw = new PrintWriter(main)) {
                pw.print(cBuilder.getSource());
            } catch (Exception e) {
                mainFile.erro(0, 0, "Unable to write to file[" + main + "]", this);
            }

        }
    }

    /**
     * Destroi todas as dependencias dos arquivos deletados
     * Destroi os arquivos deletados
     *
     */
    public void unload() {
        for (ContentFile cFile : cFiles.values()) {
            if (cFile.isInvalided()) {
                cFile.unload();
            }
        }

        for (String fileName : subs) {
            cFiles.remove(fileName);
        }

        for (Map.Entry<String,String> entry : adds.entrySet()) {
            cFiles.put(entry.getKey(), new ContentFile(this, entry.getKey(), entry.getValue()));
        }

        mainFile = null;
        adds.clear();
        subs.clear();

        compiler.langInvalidate(this);
    }

    public void unmake() {

    }

    public void release() {
        for (ContentFile cFile : cFiles.values()) {
            cFile.invalidate();
        }

        for (ContentFile cFile : cFiles.values()) {
            if (cFile.isInvalided()) {
                cFile.unload();
            }
        }

        compiler.langInvalidate(this);
    }
}
