import java.util.*;

public class Value {
    private int integer;
    private double dec;
    private boolean flag;

    public Value(int n) {
        integer = n;
        flag = false;
    }

    public Value(double d) {
        dec = d;
        flag = true;
    }

    //shouldn't need these but just in case
    public void setValue(double d) {
        dec = d;
    }

    public void setValue(int n) {
        integer = n;
    }

    public Number getValue() {
        if (flag)
            return dec;
        else
            return integer;
    }
    public boolean isFloat() {
        return flag;
    }
}
