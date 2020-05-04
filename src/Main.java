import content.Token;
import data.ContentFile;
import data.Error;
import data.Library;
import logic.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Main {
    public static void main(String... args) {
        Library library = new Library("lang");
        library.add("Int", read("Int"));
        library.add("Bool", read("Bool"));
        library.add("List", read("List"));
        library.add("Array", read("Array"));
        library.add("Object", read("Object"));

        library.read();
        for (ContentFile cFile : library.cFiles) {
            System.out.println(cFile.name);
            System.out.println(cFile.namespace);
        }
        for (Namespace namespace : library.namespaces.values()) {
            System.out.println(namespace);
        }

        library.load();
        for (ContentFile cFile : library.cFiles) {
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
