import java.io.*;

public class FirstFirst {
  public static void main(String[] args) {
    new BufferedReader(new InputStreamReader(System.in)).lines().map(s -> {
      var parts = s.split("\\s+", 2);
      parts[0] = parts[0].split("_", 2)[0];
      return String.join(" ", parts);
    }).forEach(System.out::println);
    ;
  }
}
