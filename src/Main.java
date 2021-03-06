import builder.CppBuilder;
import content.Token;
import data.*;
import data.Compiler;
import data.Error;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Main {

    public static void main(String... args) {
        Library lang = new Library("lang", 0);
        File dir = new File(System.getProperty("user.dir"), "test/lang/");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                lang.fileAdd(file.getName(), read(file));
            }
        }
        lang.setSrcDir(new File("C:/Dropbox/Cpp/Lang/lang"));
        lang.setObjDir(new File(System.getProperty("user.dir"), "test/output/lang/obj"));

        Compiler compiler = new Compiler();
        compiler.libAdd(lang);

        // The compiler should pass all dependencies at least at same stage before go to next
        lang.read();
        lang.preload();
        lang.load();
        lang.internal();
        lang.cross();

        // The compiler can choose wich one will make
        lang.make();

        boolean erros = false;
        for (ContentFile cFile : lang.cFiles.values()) {
            if (cFile.erros.size() > 0) {
                erros = true;
                System.out.println("Erros at " + cFile.name);
                for (Error error : cFile.erros) {
                    System.out.println(error+" ["+cFile.content.substring(error.start, error.end)+"]"
                            +"["+error.start+", "+ error.end+"] line :" +
                            (1 + cFile.content.substring(0, error.start).length() - cFile.content.substring(0, error.start).replace("\n", "").length()));
                }
            }
        }
        if (!erros) {
            File[] fls = lang.getSrcDir().listFiles();
            if (fls != null) {
                for (File file : fls) {
                    file.delete();
                }
            }

            CppBuilder cBuilder = new CppBuilder();
            lang.build(cBuilder);
            lang.buildMain(cBuilder);

            for (ContentFile cFile : lang.cFiles.values()) {
                if (cFile.erros.size() > 0) {
                    System.out.println("Erros at " + cFile.name);
                    for (Error error : cFile.erros) {
                        System.out.println(error+" ["+cFile.content.substring(error.start, error.end)+"]"
                        +"["+error.start+", "+ error.end+"]");
                    }
                }
            }
        }
    }

    private static String read(File file) {
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
