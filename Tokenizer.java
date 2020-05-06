import java.util.*;
import java.util.regex.*;

public class Tokenizer {

    private Token secondToLastToken;
    private Token lastToken;

    private static Pattern alphanumeric = Pattern.compile("^[a-zA-Z0-9]*$");

    private static boolean isAlphaNumeric(String c) {
        return alphanumeric.matcher(c).find();
    }

    private TreeMap<String, Stack<String>> varNamesToIDs = new TreeMap<>();
    private TreeMap<String, String> funNamesToIDs = new TreeMap<>();
    private TreeMap<String, Token> idMotherTokens = new TreeMap<>();
    private int varIndex = 0;
    private int funIndex = 13;
    private int index = 0;

    private Stack<Integer> scope = new Stack<>();
    private Stack<ArrayList<String>> varsInScope = new Stack<>();

    boolean cucked = false;

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
                if (str.matches("Stack<int>"))
                    return new Token(Token.Types.STACK,scope.peek(), Token.DataTypes.INTSTACK);
                else
                    return new Token(Token.Types.STACK,scope.peek(), Token.DataTypes.FLOATSTACK);
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
                    cucked = true;
                    index += str.length();
                    String out;
                    if (str.equals("main"))
                        out = "fmain";
                    else
                        out = "f"+funIndex++;
                    funNamesToIDs.put(str, out);
                    Token t = new Token(Token.Types.ID, out, scope.peek());
                    t.setDataType(lastToken.getType() == Token.Types.INT ? Token.DataTypes.INT : lastToken.getType() == Token.Types.FLOAT ? Token.DataTypes.FLOAT : lastToken.getDataType() == Token.DataTypes.INTSTACK ? Token.DataTypes.INTSTACK : Token.DataTypes.FLOATSTACK);
                    return t;
                }
                else if (lastToken.getType() == Token.Types.INT || lastToken.getType() == Token.Types.FLOAT || lastToken.getType() == Token.Types.STACK) {
                    //we have a var declaration;
                    index += str.length();
                    String out = "v"+(varIndex++)+((scope.peek() == -1) ? "" : "_"+scope.peek());
                    if (!(scope.peek() == -1))
                        varsInScope.peek().add(str);
                    if (varNamesToIDs.get(str) == null) {
                        Stack<String> s = new Stack<>();
                        s.push(out);
                        varNamesToIDs.put(str, s);
                    }
                    else
                        varNamesToIDs.get(str).push(out);
                    return new Token(Token.Types.ID, out, scope.peek(),lastToken.getType() == Token.Types.INT ? Token.DataTypes.INT : lastToken.getType() == Token.Types.FLOAT ? Token.DataTypes.FLOAT : lastToken.getDataType() == Token.DataTypes.INTSTACK ? Token.DataTypes.INTSTACK : Token.DataTypes.FLOATSTACK);
                }
                else if (stream.charAt(tick) == '(') {
                    index += str.length();
                    //function call, need to look in function ID's
                    return new Token(Token.Types.ID, funNamesToIDs.get(str), scope.peek());
                }
                else {
                    index += str.length();
                    return new Token(Token.Types.ID, varNamesToIDs.get(str).peek(), scope.peek());
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
                    if (!cucked)
                        varsInScope.push(new ArrayList<>());
                    else
                        cucked = false;
                    return new Token(Token.Types.LBRACE, scope.peek());
                }
                case '}': {
                    index++;
                    for (String s : varsInScope.pop()) {
                        varNamesToIDs.get(s).pop();
                    }
                    return new Token(Token.Types.RBRACE, scope.pop());
                }
                case '(': {
                    if (cucked)
                        varsInScope.push(new ArrayList<>());
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
        funNamesToIDs.put("MDrive", "f0");
        funNamesToIDs.put("LDrive", "f1");
        funNamesToIDs.put("Steering", "f2");
        funNamesToIDs.put("Tank", "f3");
        funNamesToIDs.put("Wait", "f4");
        funNamesToIDs.put("Touch", "f5");
        funNamesToIDs.put("Infrared", "f6");
        funNamesToIDs.put("MRotation", "f7");
        funNamesToIDs.put("MReverse", "f8");
        funNamesToIDs.put("pop", "f9");
        funNamesToIDs.put("peek", "f10");
        funNamesToIDs.put("push", "f11");
        funNamesToIDs.put("size", "f12");
        ArrayList<String> temp = new ArrayList<>();
        varsInScope.add(temp);
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
            if (lastToken != null) {
                secondToLastToken = lastToken;
            }

            if (lastToken != null && lastToken.getType() == Token.Types.FUN && (t.getType() == Token.Types.INT || t.getType() == Token.Types.FLOAT || t.getType() == Token.Types.STACK)) { //for functions
                lastToken.setDataType(lastToken.getType() == Token.Types.INT ? Token.DataTypes.INT : lastToken.getType() == Token.Types.FLOAT ? Token.DataTypes.FLOAT : lastToken.getDataType() == Token.DataTypes.INTSTACK ? Token.DataTypes.INTSTACK : Token.DataTypes.FLOATSTACK);
            }

            lastToken = t;
            res.add(t);
        } while (index < stream.length());
        res.add(new Token(Token.Types.END, -1));
        return res;
    }
}
