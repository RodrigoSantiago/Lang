import content.Lexer;
import content.Token;
import data.TextFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Main {
    public static void main(String... args) {
        File file = new File(System.getProperty("user.dir"), "test/lang/Object.txt");
        try {
            String str = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
            TextFile textFile = new TextFile(str);
            Lexer lexer = new Lexer(textFile);
            Token t = lexer.read();
            print(t, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Builder builder = new Builder();
        // builder.add("name", InputStream);
        // builder.Load();
        // builder.Cross();
        // builder.Make();
        // builder.Build();

        // Affecteds files = builder.Update("name", InputStream);
        // builder.Load(files);
        // builder.Cross(files);
        // builder.Make(files);
        // builder.Build(files);

        // builder.Pack(); // Remove content links (less memory usage)

        // Builder builder2 = new Builder();
        // builder2.AddLibrary(builder);
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
        /*
        while (t != null) {
            if (t.getNext() == null && (t.getParent() != null && t.getParent().getChild() != t)) {
                System.out.println();
                for (int i = 0; i < off - 1; i++) {
                    System.out.print("    ");
                }
            }
            if (t.getChild() != null && (t.getPrev() != null && t.getPrev().getChild() == null)) {
                System.out.println();
                for (int i = 0; i < off; i++) {
                    System.out.print("    ");
                }
            }
            System.out.print(t);

            if (t.getChild() != null) {
                System.out.println();
                for (int i = 0; i < off; i++) {
                    System.out.print("    ");
                }
                print(t.getChild(), off + 1);
                if (t.getNext() == null || t.getNext().getNext() != null) {
                    System.out.println();
                    for (int i = 0; i < off; i++) {
                        System.out.print("    ");
                    }
                }
            }
            t = t.getNext();
        }*/
    }
}
