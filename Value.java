// Handles int and float types in our language
public class Value {
    private int integer;
    private float dec;
    // True = float, False = int
    private int flag;

    public Value(int n) {
        integer = n;
        flag = 0;
    }

    public Value(float d) {
        dec = d;
        flag = 1;
    }

    public boolean isInt() {
        return flag == 0;
    }

    public boolean isFloat() {
        return flag == 1;
    }

    // Setter Methods
    public void setFloatValue(float d) {
        dec = d;
    }

    public void setIntegerValue(int n) {
        integer = n;
    }

    public Number getValue() {
       if (flag == 0)
            return integer;
        if (flag == 1)
            return dec;
        return 0;
    }
}
