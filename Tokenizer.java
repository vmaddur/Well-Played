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
                return new Token(Token.Types.VAL, v, scope.peek());
            }
            else if (str.matches("\\d+")) {
                //we have an int
                Value v = new Value(Integer.parseInt(str));
                return new Token(Token.Types.VAL, v, scope.peek());
            }

        } else if (isAlphaNumeric(""+curr)) {
            int tick = index + 1;
            while (tick < stream.length() && (isAlphaNumeric(""+stream.charAt(tick)) || stream.charAt(tick) == '<' || stream.charAt(tick) == '>')) {
                tick++;
            }
            String str = stream.substring(index, tick);


            if (str.equals("int")) {
                index += str.length();
                return new Token(Token.Types.INT, scope.peek());
            }
            else if (str.matches("Stack<\\w+>")) {
                index += str.length();
                String out = "v"+(varIndex++)+((scope.peek() == -1) ? "" : "_"+scope.peek());
                str = str.substring(0, str.indexOf("<"));
                varNamesToIDs.put(str, out);
                if (str.matches("Stack<int>"))
                    return new Token(Token.DataTypes.INT, out, scope.peek());
                else
                    return new Token(Token.DataTypes.FLOAT, out, scope.peek());
            }
            else if (str.equals("MDrive")) {
                index += str.length();
                return new Token(Token.Types.MDRIVE,scope.peek());
            }
            else if (str.equals("LDrive")) {
                index += str.length();
                return new Token(Token.Types.LDRIVE,scope.peek());
            }
            else if (str.equals("Steering")) {
                index += str.length();
                return new Token(Token.Types.STEER, scope.peek());
            }
            else if (str.equals("Tank")) {
                index += str.length();
                return new Token(Token.Types.TANK, scope.peek());
            }
            else if (str.equals("Wait")) {
                index += str.length();
                return new Token(Token.Types.WAIT, scope.peek());
            }
            else if (str.equals("Touch")) {
                index += str.length();
                return new Token(Token.Types.TOUCH, scope.peek());
            }
            else if (str.equals("Infrared")) {
                index += str.length();
                return new Token(Token.Types.INFRA, scope.peek());
            }
            else if (str.equals("MRotation")) {
                index += str.length();
                return new Token(Token.Types.MROT, scope.peek());
            }
            else if (str.equals("float")) {
                index += str.length();
                return new Token(Token.Types.FLOAT, scope.peek());
            }
            else if (str.equals("else")) {
                index += str.length();
                scope.push(tokenIndex);
                Token t = new Token(Token.Types.ELSE, scope.peek());
                return t;
            }
            else if (str.equals("return")) {
                index += str.length();
                return new Token(Token.Types.RET, scope.peek());
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
                    return new Token(Token.Types.ID, out, scope.peek());
                }
                else if (lastToken.getType() == Token.Types.INT || lastToken.getType() == Token.Types.FLOAT) {
                    //we have a var declaration;
                    index += str.length();
                    String out = "v"+(varIndex++)+((scope.peek() == -1) ? "" : "_"+scope.peek());
                    varNamesToIDs.put(str, out);
                    return new Token(Token.Types.ID, out, scope.peek(), (lastToken.getType() == Token.Types.INT) ? Token.DataTypes.INT : Token.DataTypes.FLOAT);
                }
                else if (stream.charAt(tick) == '(') {
                    index += str.length();
                    //function call, need to look in function ID's
                    return new Token(Token.Types.ID, funNamesToIDs.get(str), scope.peek());
                }
                else {
                    index += str.length();
                    return new Token(Token.Types.ID, varNamesToIDs.get(str), scope.peek());
                }
            }
        } else {
            switch (curr) {
                case '=': {
                    if (stream.charAt(index+1) == '=') {
                        index+= 2;
                        return new Token(Token.Types.EQEQ, scope.peek());
                    } else {
                        index++;
                        return new Token(Token.Types.EQ, scope.peek());
                    }

                }
                case ',': {
                    index++;
                    return new Token(Token.Types.COMMA, scope.peek());
                }
                case '{': {
                    index++;
                    return new Token(Token.Types.LBRACE, scope.peek());
                }
                case '}': {
                    index++;
                    scope.pop();
                    return new Token(Token.Types.RBRACE, scope.peek());
                }
                case '(': {
                    index++;
                    return new Token(Token.Types.LPAREN, scope.peek());
                }
                case ')': {
                    index++;
                    return new Token(Token.Types.RPAREN, scope.peek());
                }
                case '*': {
                    index++;
                    return new Token(Token.Types.MUL, scope.peek());
                }
                case '+': {
                    index++;
                    return new Token(Token.Types.PLUS, scope.peek());
                }
                case '/': {
                    index++;
                    return new Token(Token.Types.DIV, scope.peek());
                }
                case '-': {
                    index++;
                    return new Token(Token.Types.MINUS, scope.peek());
                }
                case '<': {
                    if (stream.charAt(index + 1) == '=') {
                        index+=2;
                        return new Token(Token.Types.LEQ, scope.peek());
                    } else {
                        index++;
                        return new Token(Token.Types.LT, scope.peek());
                    }
                }
                case '>': {
                    if (stream.charAt(index + 1) == '=') {
                        index = index + 2;
                        return new Token(Token.Types.GEQ, scope.peek());
                    } else {
                        index = index + 1;
                        return new Token(Token.Types.GT, scope.peek());
                    }
                }
                case '!': {
                    if (stream.charAt(index + 1) == '=') {
                        index = index + 2;
                        return new Token(Token.Types.NEQ, scope.peek());
                    } else {
                        index = index + 1;
                        System.err.println("invalid use of !");
                        System.exit(1);
                    }
                    break;
                }
                case ';': {
                    index = index + 1;
                    return new Token(Token.Types.SEMI, scope.peek());
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
        do {
            while ((""+stream.charAt(index)).matches("\\s")) {
                index++;
            }
            Token t = getToken(stream, res.size());
            if (lastToken != null && !(t.getType() == Token.Types.INT || t.getType() == Token.Types.FLOAT)) {
                secondToLastToken = lastToken;
            }

            if (lastToken != null && (t.getType() == Token.Types.INT || t.getType() == Token.Types.FLOAT)) {
                lastToken.setDataType(t.getType() == Token.Types.INT ? Token.DataTypes.INT : Token.DataTypes.FLOAT);
            }

            lastToken = t;
            res.add(t);
        } while (index < stream.length());
        res.add(new Token(Token.Types.END, -1));
        for (int i = res.size() - 1; i >= 0; i--) {
            if (res.get(i).getType() == Token.Types.INT || res.get(i).getType() == Token.Types.FLOAT)
                res.remove(i);
        }
        return res;
    }
}
