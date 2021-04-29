package edu.utexas.tacc.tapis.search.parser;

import java.util.Locale;

/*
 * Class representing nodes in the AST
 */
public class ASTBinaryExpression extends ASTNode
{
  private final String op;
  private final ASTNode left;
  private final ASTNode right;
  ASTBinaryExpression(String o, ASTNode l, ASTNode r)
  {
    op = o.toUpperCase();
    left = l;
    right = r;
  }

  public String getOp() { return op; }

  public ASTNode getLeft() { return left; }

  public ASTNode getRight() { return right; }

  public String toString() { return "(" + left + "." + op + "." + right + ")"; }
}