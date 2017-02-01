package tivi.cis;

import java.util.regex.*;

/**
 * Ein Parser für arithmetische Ausdrücke,
 * der die vier Grundrechenarten +, -, *, /
 * sowie einfache Klammern ( ) und Zahlen
 * verarbeiten kann. Variablen können mit
 * Hilfe des {@link MathListener} eingesetzt
 * werden.
 * 
 * @author Florian
 */
public abstract class MathEval
{
  /**
   * Liste aller gültigen Operatoren
   */
  static final String[] OPERATORS = new String[]{"*", "/", "+", "-"};
  /**
   * Akzeptiertes Zeichen für öffnende Klammern
   */
  static final String OPENING_BRACKET = "(";
  /**
   * Akzeptiertes Zeichen für schließende Klammern
   */
  static final String CLOSING_BRACKET =")";
  
  /**
   * Hauptfunktion der MathEval-Klasse,
   * berechnet den Wert eines als String übergebenen arithmetischen Ausrucks
   * und gibt ihn als double zurück, wenn der Ausdruck gültig ist und nur unterstützte Operatoren enthält.
   * 
   * @throws ArithmeticException Bei unbekannten Operatoren oder ungültigen Operationen
   * @throws IllegalArgumentException Bei nicht geöffneten oder nicht geschlossenen Klammern
   * @param expression Zu parsender mathematischer Ausdruck als String
   * @return Den Wert des übergebenen Ausdrucks als double, wenn der Ausdruck gültig ist und nur unterstützte Operatoren enthält
   */
  public static double evaluate(String expression)
  {
    if(expression.contains(OPENING_BRACKET) ^ expression.contains(CLOSING_BRACKET))
    {
      throw new IllegalArgumentException("Given expression is invalid");
    }
    else if(expression.contains(OPENING_BRACKET) && expression.contains(CLOSING_BRACKET))
    {
      String[] sp = expression.split("[" + OPENING_BRACKET + "]");
      sp = sp[sp.length - 1].split("[" + CLOSING_BRACKET + "]");
      
      expression = evaluate(expression.replace(OPENING_BRACKET + sp[0] + CLOSING_BRACKET, evaluate(sp[0]) + "")) + "";
    }
    else
    {
      for(String operator : OPERATORS)
      {
        Pattern p = Pattern.compile("(([-])?\\d+(\\.\\d+)?)[" + operator + "](([-])?\\d+(\\.\\d+)?)");
        Matcher m = p.matcher(expression);
        
        while(m.find())
        {
          expression = expression.replace(m.group(0), calc(Double.parseDouble(m.group(1)), operator, Double.parseDouble(m.group(4))) + "");
          m = p.matcher(expression);
        }
      }
    }
    
    return Double.parseDouble(expression);
  }
  
  /**
   * Private Hilfsfunktion, liefert aus zwei Zahlen und einem
   * Operator das Ergebnis der daraus zusammengesetzten Rechnung.
   * 
   * @throws ArithmeticException Bei unbekannten Operatoren oder ungültigen Operationen
   * @param left Linker Operand
   * @param op Operator, unterstützt werden +, -, * und /
   * @param right Rechter Operand
   * @return Das Ergebnis der übergebenen Operation, wenn die Eingabe gültig war
   */
  private static double calc(double left, String op, double right)
  {
    switch(op)
    {
      case "^": return Math.pow(left, right);
      case "*": return left * right;
      case "/": return left / right;
      case "+": return left + right;
      case "-": return left - right;
    }
    
    throw new ArithmeticException("Unknown operand " + op);
  }
}
