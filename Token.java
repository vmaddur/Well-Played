import java.lang.reflect.Type;
import java.util.ArrayList;

public class Token {

    public enum StackDataTypes {
        INT,
        FLOAT
    }

    private Types type;
    private Value value;
    private String name;
    private StackDataTypes stackDataType;
    private int parentScope;
    public enum Types {
        ELSE,
        END,
        EQ,
        EQEQ,
        NEQ,
        LEQ,
        GEQ,
        GT,
        LT,
        ID,
        IF,
        VAL,
        LBRACE,
        LPAREN,
        MUL,
        NONE,
        PLUS,
        MINUS,
        DIV,
        RBRACE,
        RPAREN,
        STACK,
        SEMI,
        FUN,
        WHILE,
        INT,
        FLOAT,
        COMMA,
        RET,
        MDRIVE,
        LDRIVE,
        STEER,
        TANK,
        WAIT,
        TOUCH,
        INFRA,
        MROT

    }

    public Value getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public int getParentScope() {
        return parentScope;
    }

    public Types getType() {
        return type;
    }

    //for the simpletons
    public Token(Types type1) {
        type = type1;
    }

    public Token(Types type, int pScope) {
        this.type = type;
        parentScope = pScope;
    }

    //for the stacks
    public Token(StackDataTypes st, String name) {
        type = Types.STACK;
        stackDataType = st;
        this.name = name;
    }

    //for the values
    public Token(Types type1, Value val) {
        type = type1;
        value = val;
    }

    //for the variables
    public Token(Types type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value=" + ((value != null) ? value.getValue() : 0) +
                ", name='" + name + '\'' +
                ", stackDataType=" + stackDataType +
                ", parentScope=" + parentScope +
                '}';
    }
}