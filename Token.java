import java.lang.reflect.Type;
import java.util.ArrayList;

public class Token {

    public enum DataTypes {
        INT,
        FLOAT
    }

    private Types type;
    private Value value;
    private String name;
    private DataTypes dataType;
    private int parentScope;
    private int scope;
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

    public int getScope() {
        return scope;
    }

    public void setDataType(DataTypes type) {
        dataType = type;
    }

    public DataTypes getDataType() {
        return dataType;
    }

    public Types getType() {
        return type;
    }

    //for the simpletons
    public Token(Types type1, int scope) {
        this.scope =scope;
        type = type1;
    }

    public Token(Types type, int pScope, int scope) {
        this.scope = scope;
        this.type = type;
        parentScope = pScope;
    }

    public Token(Types type, String name, int scope, DataTypes dataType) {
        this.scope = scope;
        this.type = type;
        this.name = name;
        this.dataType = dataType;
    }

    //for the stacks
    public Token(DataTypes st, String name, int scope) {
        this.scope = scope;
        type = Types.STACK;
        dataType = st;
        this.name = name;
    }

    //for the values
    public Token(Types type1, Value val, int scope) {
        this.scope = scope;
        type = type1;
        value = val;
    }

    //for the variables
    public Token(Types type, String name, int scope) {
        this.scope = scope;
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value=" + ((value != null) ? value.getValue() : 0) +
                ", name='" + name + '\'' +
                ", dataType=" + dataType +
                ", parentScope=" + parentScope +
                ", scope=" + scope +
                '}';
    }
}