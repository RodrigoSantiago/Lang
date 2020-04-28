package content;

import java.util.logging.ErrorManager;


public class Parser {

    ErrorManager errorManager;
    String content;
    Lexer lexer;
    Token token;

    public Parser() {

    }

    public int level;

    // Namespace = 0
    // Typedef = 1
    // Stack = 2 ...

    /*

    Pre : Attributes
    Start : [namespace|using|class|interface|struct|enum]
    End : [Namespace keywords(unexpected -> start next)] [;][ nest -> {}]

     */
    public void read(int level) {

    }

    public Object readGeneric(int level, char start) {
        return null;
    }

    public Token readNest(int level, char start) {
        return null;
    }

    public boolean isKeyword() {
        return true;
    }

    public boolean isTypeName() {
        return true;
    }

}
