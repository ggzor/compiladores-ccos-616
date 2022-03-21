<!-- vim: set tw=60 spell: -->
# Table driven lexer and LL(1) parser

An implementation of a simple table-driven lexer and a
table-driven LL(1) parser.

## Usage

It should run in any Java `>=11`. Older versions are not
supported because the `var` keyword is used in many places.

```bash
# Compile everything
javac *.java

# Run the lexer (input is taken from stdin)
echo '(a + b) * c' | java Lexer './samples/expr/lexer.txt'

# Run the lexer and the parser (-d is used to print debug data)
echo '(a + b) * c' | java Lexer './samples/expr/lexer.txt' | java Parser './samples/expr/parser.txt' -d
```

An example of the supported table format for the lexer can
be found in [./samples/expr/lexer.txt](./samples/expr/lexer.txt).

The output of the lexer are pairs of token types and lexemes:

```
( (
ident a
+ +
ident b
) )
* *
ident c
```

This output can be fed directly to the parser, which prints
the following lines:

```
Stack: $,E                            Accept: 
Stack: $,E',T                         Accept: 
Stack: $,E',T',F                      Accept: 
Stack: $,E',T',),E,(                  Accept: 
Stack: $,E',T',),E                    Accept: (
Stack: $,E',T',),E',T                 Accept: (
Stack: $,E',T',),E',T',F              Accept: (
Stack: $,E',T',),E',T',ident          Accept: (
Stack: $,E',T',),E',T'                Accept: ( ident
Stack: $,E',T',),E'                   Accept: ( ident
Stack: $,E',T',),E',T,+               Accept: ( ident
Stack: $,E',T',),E',T                 Accept: ( ident +
Stack: $,E',T',),E',T',F              Accept: ( ident +
Stack: $,E',T',),E',T',ident          Accept: ( ident +
Stack: $,E',T',),E',T'                Accept: ( ident + ident
Stack: $,E',T',),E'                   Accept: ( ident + ident
Stack: $,E',T',)                      Accept: ( ident + ident
Stack: $,E',T'                        Accept: ( ident + ident )
Stack: $,E',T',F,*                    Accept: ( ident + ident )
Stack: $,E',T',F                      Accept: ( ident + ident ) *
Stack: $,E',T',ident                  Accept: ( ident + ident ) *
Stack: $,E',T'                        Accept: ( ident + ident ) * ident
Stack: $,E'                           Accept: ( ident + ident ) * ident
Stack: $                              Accept: ( ident + ident ) * ident
Accepted
E
.T
..F
...( (
...E
....T
.....F
......ident a
.....T'
....E'
.....+ +
.....T
......F
.......ident b
......T'
.....E'
...) )
..T'
...* *
...F
....ident c
...T'
.E'
```

The first lines are the stack states and the accepted tokens
so far. The last lines are the resulting tree.

To properly feed the output to the next stage, the `-d` flag
must be removed giving the following output.

```
E
 T
  F
   ( (
   E
    T
     F
      ident a
     T'
    E'
     + +
     T
      F
       ident b
      T'
     E'
   ) )
  T'
   * *
   F
    ident c
   T'
 E'
```

> Note: The compiler is not finished yet, the parse output
> will be fed to the next stage.

