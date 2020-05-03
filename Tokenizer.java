import java.util.*;
import java.util.regex.*;

public class Tokenizer {

    private Token secondToLastToken;
    private Token lastToken;

    private static Pattern alphanumeric = Pattern.compile("^[a-zA-Z0-9]*$");

    private static boolean isAlphaNumeric(String c) {
        return alphanumeric.matcher(c).find();
    }

    private TreeMap<String, String> varNamesToIDs = new TreeMap<>();
    private TreeMap<String, String> funNamesToIDs = new TreeMap<>();
    private int varIndex = 0;
    private int funIndex = 0;
    private int index = 0;

    private Stack<Integer> scope = new Stack<>();


    private Token getToken(String stream, int tokenIndex) {
        char curr = stream.charAt(index);
        if ((curr >= '0' && curr <= '9') || curr == '.') {
            String str = "";
            while ((curr >= '0' && curr <= '9') || curr == '.') {
                index++;
                str+=curr;
                curr = stream.charAt(index);
            }
            if (str.matches("\\d*\\.\\d+")) {
                //we have a float
                Value v = new Value(Double.parseDouble(str));
                return new Token(Token.Types.VAL, v);
            }
            else if (str.matches("\\d+")) {
                //we have an int
                Value v = new Value(Integer.parseInt(str));
                return new Token(Token.Types.VAL, v);
            }

        } else if (isAlphaNumeric(""+curr)) {
            int tick = index + 1;
            while (tick < stream.length() && (isAlphaNumeric(""+stream.charAt(tick)) || stream.charAt(tick) == '<' || stream.charAt(tick) == '>')) {
                tick++;
            }
            String str = stream.substring(index, tick);


            if (str.equals("int")) {
                index += str.length();
                return new Token(Token.Types.INT);
            }
            else if (str.matches("Stack<\\w+>")) {
                index += str.length();
                String out = "v"+(varIndex++)+((scope.peek() == -1) ? "" : "_"+scope.peek());
                str = str.substring(0, str.indexOf("<"));
                varNamesToIDs.put(str, out);
                if (str.matches("Stack<int>"))
                    return new Token(Token.StackDataTypes.INT, out);
                else
                    return new Token(Token.StackDataTypes.FLOAT, out);
            }
            else if (str.equals("MDrive")) {
                index += str.length();
                return new Token(Token.Types.MDRIVE);
            }
            else if (str.equals("LDrive")) {
                index += str.length();
                return new Token(Token.Types.LDRIVE);
            }
            else if (str.equals("Steering")) {
                index += str.length();
                return new Token(Token.Types.STEER);
            }
            else if (str.equals("Tank")) {
                index += str.length();
                return new Token(Token.Types.TANK);
            }
            else if (str.equals("Wait")) {
                index += str.length();
                return new Token(Token.Types.WAIT);
            }
            else if (str.equals("Touch")) {
                index += str.length();
                return new Token(Token.Types.TOUCH);
            }
            else if (str.equals("Infrared")) {
                index += str.length();
                return new Token(Token.Types.INFRA);
            }
            else if (str.equals("MRotation")) {
                index += str.length();
                return new Token(Token.Types.MROT);
            }
            else if (str.equals("float")) {
                index += str.length();
                return new Token(Token.Types.FLOAT);
            }
            else if (str.equals("else")) {
                index += str.length();
                Token t = new Token(Token.Types.ELSE, scope.peek());
                scope.push(tokenIndex);
                return t;
            }
            else if (str.equals("return")) {
                index += str.length();
                return new Token(Token.Types.RET);
            }
            else if (str.equals("if")) {
                index += str.length();
                Token t = new Token(Token.Types.IF, scope.peek());
                scope.push(tokenIndex);
                return t;
            }
            else if (str.equals("while")) {
                index += str.length();
                Token t = new Token(Token.Types.WHILE, scope.peek());
                scope.push(tokenIndex);
                return t;
            }
            else if (str.equals("fun")) {
                index += str.length();
                Token t = new Token(Token.Types.FUN, scope.peek());
                scope.push(tokenIndex);
                return t;
            }
            else {
                if (!(scope.peek() == -1) && secondToLastToken.getType() == Token.Types.FUN) {
                    index += str.length();
                    String out = "f"+funIndex++;
                    funNamesToIDs.put(str, out);
                    return new Token(Token.Types.ID, out);
                }
                else if (lastToken.getType() == Token.Types.INT || lastToken.getType() == Token.Types.FLOAT) {
                    //we have a var declaration;
                    index += str.length();
                    String out = "v"+(varIndex++)+((scope.peek() == -1) ? "" : "_"+scope.peek());
                    varNamesToIDs.put(str, out);
                    return new Token(Token.Types.ID, out);
                }
                else if (stream.charAt(tick) == '(') {
                    index += str.length();
                    //function call, need to look in function ID's
                    return new Token(Token.Types.ID, funNamesToIDs.get(str));
                }
                else {
                    index += str.length();
                    return new Token(Token.Types.ID, varNamesToIDs.get(str));
                }
            }
        } else {
            switch (curr) {
                case '=': {
                    if (stream.charAt(index+1) == '=') {
                        index+= 2;
                        return new Token(Token.Types.EQEQ);
                    } else {
                        index++;
                        return new Token(Token.Types.EQ);
                    }

                }
                case ',': {
                    index++;
                    return new Token(Token.Types.COMMA);
                }
                case '{': {
                    index++;
                    return new Token(Token.Types.LBRACE);
                }
                case '}': {
                    index++;
                    scope.pop();
                    return new Token(Token.Types.RBRACE);
                }
                case '(': {
                    index++;
                    return new Token(Token.Types.LPAREN);
                }
                case ')': {
                    index++;
                    return new Token(Token.Types.RPAREN);
                }
                case '*': {
                    index++;
                    return new Token(Token.Types.MUL);
                }
                case '+': {
                    index++;
                    return new Token(Token.Types.PLUS);
                }
                case '/': {
                    index++;
                    return new Token(Token.Types.DIV);
                }
                case '-': {
                    index++;
                    return new Token(Token.Types.MINUS);
                }
                case '<': {
                    if (stream.charAt(index + 1) == '=') {
                        index+=2;
                        return new Token(Token.Types.LEQ);
                    } else {
                        index++;
                        return new Token(Token.Types.LT);
                    }
                }
                case '>': {
                    if (stream.charAt(index + 1) == '=') {
                        index = index + 2;
                        return new Token(Token.Types.GEQ);
                    } else {
                        index = index + 1;
                        return new Token(Token.Types.GT);
                    }
                }
                case '!': {
                    if (stream.charAt(index + 1) == '=') {
                        index = index + 2;
                        return new Token(Token.Types.NEQ);
                    } else {
                        index = index + 1;
                        System.err.println("invalid use of !");
                        System.exit(1);
                    }
                    break;
                }
                case ';': {
                    index = index + 1;
                    return new Token(Token.Types.SEMI);
                }
            }
        }
        return null;
    }

    public ArrayList<Token> tokenize(String stream) {
        scope.push(-1);
        ArrayList<Token> res = new ArrayList<>();
        if (stream.length() == 0) {
            return null;
        }
        char curr = stream.charAt(0);
        do {
            while ((""+stream.charAt(index)).matches("\\s")) {
                index++;
                curr = stream.charAt(index);
            }
            Token t = getToken(stream, res.size());
            if (lastToken != null) {
                secondToLastToken = lastToken;
            }
            lastToken = t;
            res.add(t);
        } while (index < stream.length());
        res.add(new Token(Token.Types.END));
        return res;
    }
}
