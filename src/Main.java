import content.Token;
import data.Compiler;
import data.ContentFile;
import data.Error;
import data.Library;
import logic.Namespace;
import logic.typdef.Type;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Main {
    public static void main(String... args) {
        Library lang = new Library("lang", 0);
        lang.fileAdd("Int", read("Int"));
        lang.fileAdd("Bool", read("Bool"));
        lang.fileAdd("List", read("List"));
        lang.fileAdd("Array", read("Array"));
        lang.fileAdd("Object", read("Object"));
        lang.fileAdd("Object", read("Object"));

        Compiler.libAdd(lang);

        lang.read();
        lang.load();
        lang.cross();
        lang.make();

        for (ContentFile cFile : lang.cFiles.values()) {
            for (Type type : cFile.types) {
                System.out.println(type + ":" + (type.parents.size() > 0 ? type.parents.get(0) : null));
            }
        }
        for (ContentFile cFile : lang.cFiles.values()) {
            if (cFile.erros.size() > 0) {
                System.out.println("Erros at " + cFile.name);
                for (Error error : cFile.erros) {
                    System.out.println(error+" ["+cFile.content.substring(error.start, error.end)+"]");
                }
            }
        }
    }

    private static String read(String name) {
        File file = new File(System.getProperty("user.dir"), "test/lang/" + name + ".txt");
        try {
            return new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static void print(Token t, int off) {
        while (t != null) {
            for (int i = 0; i < off; i++) {
                System.out.print(" ");
            }
            System.out.println(t);

            if (t.getChild() != null) {
                print(t.getChild(), off + 1);
            }
            t = t.getNext();
        }
    }
}
