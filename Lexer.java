import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class Lexer {
  public static void main(String[] args) throws Exception {
    args = Common.config(args);

    var tab = Common.readTable(args[0]);

    var initial = removePrefixes(tab.get().filter(i -> i.x == 0 && i.item.matches(IS_INITIAL_RE)) //
        .findFirst().orElseThrow().item);
    var finals = tab.get().filter(i -> i.x == 0 && i.item.matches(IS_FINAL_RE)) //
        .map(i -> i.item).map(Lexer::removePrefixes).collect(toSet());

    var states = tab.get().filter(i -> i.x == 0) //
        .map(i -> i.item).map(Lexer::removePrefixes).collect(toList());
    var transitions = tab.get().filter(i -> i.y == 0).map(i -> i.item).collect(toList());

    var delta = new HashMap<String, HashMap<Integer, String>>();
    tab.get().filter(i -> i.x >= 1 && i.y >= 1).forEach(i -> {
      var row = delta.computeIfAbsent(states.get(i.y), k -> new HashMap<>());

      expand(transitions.get(i.x)).forEach(c -> row.put(c, i.item));
    });

    int c = System.in.read();
    do {
      String state = initial;
      StringBuilder lexeme = new StringBuilder();

      while (c != -1) {
        String nextState = delta.get(state).get(c);

        if (nextState == null)
          Common.error("Unknown char: '%s'", Common.escape(String.valueOf((char) c)));
        if (nextState.equals("_")) {
          if (state.equals(initial) && lexeme.length() == 0)
            Common.error("No initial transition for: '%s'", Common.escape(String.valueOf((char) c)));
          break;
        }

        state = nextState;
        lexeme.append((char) c);
        c = System.in.read();
      }

      if (finals.contains(state) && lexeme.length() > 0)
        System.out.printf("%s %s\n", state, lexeme);
    } while (c != -1);
  }

  static final String IS_INITIAL_RE = "^\\*?>.*", IS_FINAL_RE = "^>?\\*.*", PREFIX_RE = "^>?\\*?";

  static String removePrefixes(String s) {
    return s.replaceFirst(PREFIX_RE, "");
  }

  static final Map<Character, int[]> ESCAPE_SEQUENCES = //
      Map.of('s', " \t\n\r\f".chars().toArray());

  static IntStream expand(String s) {
    // Char range
    if (s.length() == 3 && s.charAt(1) == '-')
      return IntStream.rangeClosed(s.charAt(0), s.charAt(2));
    // Escape sequence
    if (s.length() == 2 && s.charAt(0) == '\\') {
      if (ESCAPE_SEQUENCES.containsKey(s.charAt(1)))
        return IntStream.of(ESCAPE_SEQUENCES.get(s.charAt(1)));
      else
        Common.error("Unknown escape sequence: '%s'", s);
    }
    // Single char
    if (s.length() == 1)
      return IntStream.of(s.charAt(0));

    Common.error("Unsupported char sequence: '%s'", s);
    return null;
  }
}
