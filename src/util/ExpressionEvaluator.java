package util;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import java.util.function.Function;

public class ExpressionEvaluator {
  // variableResolver = lookup function that map variable into actual number
  public static double evaluateInfix(
      String infixExpression, Function<String, Double> variableResolver) {

    if (infixExpression == null || infixExpression.trim().isEmpty()) {
      return 0.0;
    }

    ListInterface<String> tokens = parseTokens(infixExpression);
    ListInterface<String> postfix = convertInfixToPostfix(tokens); // use shunting-yard algorithm
    return evaluatePostfix(postfix, variableResolver);
  }

  private static ListInterface<String> parseTokens(String infix) {
    ListInterface<String> tokens = new ArrayList<>();
    if (infix == null || infix.trim().isEmpty()) return tokens;

    String[] parts = infix.trim().split("\\s+");
    for (String p : parts) {
      if (!p.isEmpty()) tokens.add(p);
    }
    return tokens;
  }

  private static ListInterface<String> convertInfixToPostfix(ListInterface<String> infixTokens) {
    ListInterface<String> postfix = new ArrayList<>();
    StackInterface<String> opStack = new LinkedStack<>();

    for (int i = 1; i <= infixTokens.getNumberOfEntries(); i++) {
      String token = infixTokens.getEntry(i);

      if (isNumeric(token) || isVariable(token)) {
        postfix.add(token);
      } else if ("(".equals(token)) {
        opStack.push(token);
      } else if (")".equals(token)) {
        while (!opStack.isEmpty() && !"(".equals(opStack.peek())) {
          postfix.add(opStack.pop());
        }
        if (!opStack.isEmpty() && "(".equals(opStack.peek())) {
          opStack.pop(); // Remove '(' from stack
        }
      } else if (isOperator(token)) {
        while (!opStack.isEmpty()
            && isOperator(opStack.peek())
            && getPrecedence(opStack.peek()) >= getPrecedence(token)) {
          postfix.add(opStack.pop());
        }
        opStack.push(token);
      }
    }

    while (!opStack.isEmpty()) {
      postfix.add(opStack.pop());
    }
    return postfix;
  }

  private static double evaluatePostfix(
      ListInterface<String> postfix, Function<String, Double> variableResolver) {

    StackInterface<Double> valStack = new LinkedStack<>();

    for (int i = 1; i <= postfix.getNumberOfEntries(); i++) {
      String token = postfix.getEntry(i);

      if (isNumeric(token)) {
        valStack.push(Double.parseDouble(token));
      } else if (isVariable(token)) {
        double resolvedVal = (variableResolver != null) ? variableResolver.apply(token) : 0.0;
        valStack.push(resolvedVal);
      } else if (isOperator(token)) {
        double op2 = valStack.isEmpty() ? 0 : valStack.pop();
        double op1 = valStack.isEmpty() ? 0 : valStack.pop();
        valStack.push(applyOperator(token, op1, op2));
      }
    }

    return valStack.isEmpty() ? 0.0 : valStack.pop();
  }

  private static double applyOperator(String op, double a, double b) {
    switch (op) {
      case "+":
        return a + b;
      case "-":
        return a - b;
      case "*":
        return a * b;
      case "/":
        if (b == 0) {
          throw new ArithmeticException("Division by zero");
        }
        return a / b;
      default:
        return 0.0;
    }
  }

  private static int getPrecedence(String op) {
    if ("+".equals(op) || "-".equals(op)) return 1;
    if ("*".equals(op) || "/".equals(op)) return 2;
    return 0;
  }

  private static boolean isOperator(String token) {
    return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
  }

  private static boolean isVariable(String token) {
    return !isOperator(token) && !"(".equals(token) && !")".equals(token) && !isNumeric(token);
  }

  private static boolean isNumeric(String token) {
    try {
      Double.parseDouble(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
