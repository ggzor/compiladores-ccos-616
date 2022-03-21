import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public class Common {
  static class Item {
    public final int x;
    public final int y;
    public final String item;

    public Item(int x, int y, String item) {
      this.x = x;
      this.y = y;
      this.item = item;
    }
  }

  private static boolean useDebug = false;

  public static boolean usesDebug() {
    return useDebug;
  }

  public static String[] config(String[] args) {
    var argsSet = new ArrayList<>(Arrays.asList(args));
    useDebug = argsSet.contains("-d");
    argsSet.remove("-d");
    return argsSet.toArray(String[]::new);
  }

  public static Supplier<Stream<Item>> readTable(String file) throws IOException {
    var table = Files.lines(Paths.get(file)).map(s -> s.trim()) //
        .filter(s -> !s.isBlank()).map(s -> s.split("\\s+")).toArray(String[][]::new);

    return () -> IntStream.range(0, table.length).mapToObj(y -> IntStream.range(0, table[0].length) //
        .mapToObj(x -> new Item(x, y, table[y][x]))).flatMap(s -> s);
  }

  public static void debug(String msg, Object... args) {
    if (useDebug)
      System.out.println(String.format(msg, args));
  }

  public static void error(String msg, Object... args) {
    System.err.println(String.format(msg, args));
    System.exit(1);
  }

  public static String escape(String s) {
    if (s.isBlank())
      s = String.format("\"%s\"", s);
    s = s.replace("\n", "\\n");
    return s;
  }
}
