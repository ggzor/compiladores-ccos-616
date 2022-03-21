import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class Parser {

  public static void main(String[] args) throws IOException {
    args = Common.config(args);

    var tab = Common.readTable(args[0]);

    var nonTerminals = tab.get().filter(i -> i.x == 0).map(i -> i.item).collect(toList());
    var transitions = tab.get().filter(i -> i.y == 0) //
        .map(i -> i.item.equals("$") ? null : i.item).collect(toList());

    var parts = Stream.concat(nonTerminals.stream().skip(1), transitions.stream().skip(1))
        .sorted(Comparator.comparing((String s) -> s == null ? 0 : s.length()) //
            .reversed().thenComparing(s -> s))
        .collect(toList());

    var delta = new HashMap<String, HashMap<String, String[]>>();
    tab.get().filter(i -> i.x >= 1 && i.y >= 1).forEach(i -> {
      var row = delta.computeIfAbsent(nonTerminals.get(i.y), k -> new HashMap<>());
      row.put(transitions.get(i.x), partition(parts, i.item));
    });

    var stack = new ArrayDeque<Optional<String>>();
    stack.push(Optional.empty());
    stack.push(Optional.of(nonTerminals.get(1)));

    var accept = new ArrayList<String>();

    var tree = new ArrayList<String>();
    var depths = new ArrayDeque<Integer>();
    depths.push(-1);
    depths.push(0);

    Runnable dumpState = () -> {
      var revIter = Spliterators.spliterator(stack.descendingIterator(), stack.size(), Spliterator.ORDERED);
      var stackStr = StreamSupport.stream(revIter, false) //
          .map(s -> s.map(Common::escape).orElse("$")).collect(joining(","));
      var acceptStr = accept.stream().collect(joining(" "));
      Common.debug("Stack: %-30s Accept: %s", stackStr, acceptStr);
    };

    try (Scanner sc = new Scanner(System.in)) {
      while (!stack.isEmpty()) {
        String token, lexeme;

        if (sc.hasNext()) {
          var line = sc.nextLine().split(" ", 2);
          token = line[0];
          lexeme = line[1];
        } else {
          token = lexeme = null;
        }

        dumpState.run();

        while (!stack.isEmpty()) {
          var state = stack.pop().orElse(null);
          var depth = depths.pop();

          // End of stack
          if (state == null) {
            // End of input
            if (token == null)
              break;
            else
              Common.error("Unexpected token: '%s'", token);
          }

          tree.add(String.format("%s%s", (Common.usesDebug() ? "." : " ").repeat(depth),
              state.equals(token) ? String.format("%s %s", token, lexeme) : state));

          if (state.equals(token)) {
            accept.add(token);
            break;
          }

          var rule = delta.get(state).get(token);
          if (rule == null)
            Common.error("Unexpected %s", //
                token == null ? "EOF" : String.format("token '%s'", token));

          // Epsilon
          if (rule.length == 1 && rule[0].isEmpty()) {
            dumpState.run();
            continue;
          } else {
            for (int i = rule.length - 1; i >= 0; i--) {
              stack.push(Optional.of(rule[i]));
              depths.push(depth + 1);
            }
          }

          dumpState.run();
        }
      }
    }

    Common.debug("Accepted");
    tree.forEach(System.out::println);
  }

  public static String[] partition(List<String> parts, String value) {
    if (value.equals("\\"))
      return new String[] { "" };
    else if (value.equals("_"))
      return null;

    return partitionRec(parts, Stream.of(value)).toArray(String[]::new);
  }

  public static Stream<String> partitionRec(List<String> parts, Stream<String> value) {
    return value.flatMap(s -> {
      if (parts.contains(s)) {
        return Stream.of(s);
      } else {
        for (var p : parts) {
          var blocks = trySplit(p, s);
          if (blocks.isPresent())
            return blocks.get().stream().flatMap(ns -> partitionRec(parts, Stream.of(ns)));
        }
        return Stream.empty();
      }
    });
  }

  public static Optional<List<String>> trySplit(String part, String s) {
    if (s.contains(part)) {
      s = s.replaceAll(Pattern.quote(part), "\0");
      var result = new ArrayList<String>();

      int start = 0, i = 0;
      while (i < s.length()) {
        if (s.charAt(i) == '\0') {
          if (start < i) {
            result.add(s.substring(start, i));
          }
          start = i + 1;
          result.add(part);
        }
        i++;
      }

      if (start < i)
        result.add(s.substring(start, i));

      return Optional.of(result);
    } else {
      return Optional.empty();
    }
  }
}
