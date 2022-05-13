import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Compile {
  static ByteArrayOutputStream intermediate;

  static void captureOut() {
    intermediate = new ByteArrayOutputStream();
    System.setOut(new PrintStream(intermediate, true));
  }

  static void replaceStdinWithCaptured() {
    System.out.flush();
    System.setIn(new ByteArrayInputStream(intermediate.toByteArray()));
    captureOut();
  }

  public static void main(String[] args) throws Exception {
    var oldOut = System.out;

    captureOut();
    Lexer.main(new String[] { "samples/final/lexer.txt" });
    replaceStdinWithCaptured();
    FirstFirst.main(new String[] {});
    replaceStdinWithCaptured();
    Parser.main(new String[] { "samples/final/parser.txt" });
    replaceStdinWithCaptured();

    System.setOut(oldOut);
    Semantic.main(new String[] {});
  }
}
