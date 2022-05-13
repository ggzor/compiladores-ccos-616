import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class Semantic {
  static enum NodeType {
    // Statement
    BLOCK, DECLARE, ASSIGN,
    // Op
    PLUS, AND,
    // Terminals
    ID, NUM, GROUP
  }

  static enum VarType {
    INT, BOOL;

    @Override
    public String toString() {
      return this == INT ? "int" : "bool";
    }
  }

  static class Node {
    public final NodeType type;
    public final Node[] children;
    public final VarType varType;
    public final Integer value;
    public final String name;

    public Node(NodeType type, Node[] children, VarType varType, Integer value, String name) {
      this.type = type;
      this.children = children;
      this.varType = varType;
      this.value = value;
      this.name = name;
    }
  }

  static class TreeGenerator {
    public final ArrayDeque<String[]> items;

    public TreeGenerator(ArrayDeque<String[]> items) {
      this.items = items;
    }

    public Node parseProgram() {
      ArrayList<Node> children = new ArrayList<>();
      match("S");
      children.add(parseStatement());
      while (match("S'")) {
        match(";");
        children.add(parseStatement());
      }
      return new Node(NodeType.BLOCK, children.toArray(Node[]::new), null, null, null);
    }

    public Node parseStatement() {
      match("L");
      var line = next();
      var ty = line[0];

      if (ty.equals("int")) {
        var name = next()[1];
        return new Node(NodeType.DECLARE, new Node[] {}, VarType.INT, null, name);
      } else if (ty.equals("bool")) {
        var name = next()[1];
        return new Node(NodeType.DECLARE, new Node[] {}, VarType.BOOL, null, name);
      } else if (ty.equals("id")) {
        var name = line[1];
        match("=");
        return new Node(NodeType.ASSIGN, new Node[] { parseExpression() }, null, null, name);
      } else {
        throw new RuntimeException("Impossible");
      }
    }

    public Node parseExpression() {
      match("E");
      Node expr = parseTerm();
      while (match("E'")) {
        match("+");
        expr = new Node(NodeType.PLUS, new Node[] { expr, parseTerm() }, null, null, null);
      }
      return expr;
    }

    public Node parseTerm() {
      match("T");
      Node expr = parseFactor();
      while (match("T'")) {
        match("and");
        expr = new Node(NodeType.AND, new Node[] { expr, parseFactor() }, null, null, null);
      }
      return expr;
    }

    public Node parseFactor() {
      match("F");
      var line = next();
      var ty = line[0];
      var value = line[1];

      if (ty.equals("num")) {
        return new Node(NodeType.NUM, new Node[] {}, null, Integer.parseInt(value), null);
      } else if (ty.equals("id")) {
        return new Node(NodeType.ID, new Node[] {}, null, null, value);
      } else if (ty.equals("(")) {
        var expr = parseExpression();
        match(")");
        return new Node(NodeType.GROUP, new Node[] { expr }, null, null, null);
      } else {
        throw new RuntimeException("Impossible");
      }
    }

    private String[] next() {
      return items.poll();
    }

    private boolean match(String token) {
      if (!items.isEmpty() && items.peek()[0].equals(token)) {
        items.poll();
        return true;
      }
      return false;
    }
  }

  static class CodeGen {
    private final HashMap<String, VarType> userSymtab = new HashMap<>();
    private final HashMap<String, VarType> tempSymtab = new HashMap<>();

    private void ensureExistsUserSymbol(String name) {
      if (!userSymtab.containsKey(name))
        error(String.format("Undefined symbol: %s", name));
    }

    public Stream<Entry<String, VarType>> allSymbols() {
      return Stream.concat(userSymtab.entrySet().stream(),
          tempSymtab.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey())));
    }

    private int placeCounter = 0;

    private String newPlace(VarType type) {
      String finalName;

      // Search free name
      do {
        finalName = String.format("t%d", placeCounter++);
      } while (tempSymtab.containsKey(finalName));

      // Add to symtab
      tempSymtab.put(finalName, type);

      return finalName;
    }

    private final ArrayList<String> code = new ArrayList<>();

    public List<String> getCode() {
      return code;
    }

    public void generate(Node node) {
      if (node.type == NodeType.BLOCK) {
        for (var child : node.children) {
          generate(child);
        }
      } else if (node.type == NodeType.DECLARE) {
        if (userSymtab.containsKey(node.name))
          error(String.format("Duplicated declaration: %s", node.name));

        userSymtab.put(node.name, node.varType);
      } else if (node.type == NodeType.ASSIGN) {
        ensureExistsUserSymbol(node.name);

        var place = generateExpr(node.children[0], userSymtab.get(node.name));
        code.add(String.format("%s = %s", node.name, place));
      } else {
        throw new RuntimeException("Impossible");
      }
    }

    private String generateExpr(Node node, VarType exprType) {
      if (node.type == NodeType.PLUS) {
        if (exprType == VarType.BOOL)
          error(String.format("Sums are not allowed in bool expressions"));

        var newPlace = newPlace(VarType.INT);
        var placeLeft = generateExpr(node.children[0], exprType);
        var placeRight = generateExpr(node.children[1], exprType);
        code.add(String.format("%s = %s + %s", newPlace, placeLeft, placeRight));
        return newPlace;
      } else if (node.type == NodeType.AND) {
        var newPlace = newPlace(VarType.BOOL);
        var placeLeft = generateExpr(node.children[0], VarType.BOOL);
        var placeRight = generateExpr(node.children[1], VarType.BOOL);
        code.add(String.format("%s = %s and %s", newPlace, placeLeft, placeRight));
        return newPlace;
      } else if (node.type == NodeType.ID) {
        ensureExistsUserSymbol(node.name);

        if (exprType == VarType.BOOL && userSymtab.get(node.name) != VarType.BOOL)
          error(String.format("Cannot use int variables inside a bool expression: %s", //
              node.name));

        return node.name;
      } else if (node.type == NodeType.NUM) {
        if (exprType == VarType.BOOL && (node.value != 0 && node.value != 1))
          error(String.format("Bools can only be assigned to 0 or 1"));

        return node.value.toString();
      } else if (node.type == NodeType.GROUP) {
        return generateExpr(node.children[0], exprType);
      } else {
        throw new RuntimeException("Impossible");
      }
    }
  }

  public static void error(String msg) {
    System.err.println(msg);
    System.exit(1);
  }

  public static void main(String[] args) {
    ArrayDeque<String[]> parts = new BufferedReader(new InputStreamReader(System.in)) //
        .lines().map(String::strip).map(s -> s.split(" ", 2)) //
        .collect(ArrayDeque::new, ArrayDeque::add, ArrayDeque::addAll);

    if (parts.isEmpty()) {
      return;
    }

    TreeGenerator generator = new TreeGenerator(parts);
    Node program = generator.parseProgram();
    CodeGen codeGen = new CodeGen();
    codeGen.generate(program);

    System.out.println("Symbols:");
    codeGen.allSymbols().forEach(entry -> {
      System.out.printf("%s: %s\n", entry.getKey(), entry.getValue());
    });

    System.out.println("\nCode:");
    for (var l : codeGen.getCode()) {
      System.out.println(l);
    }
  }
}
