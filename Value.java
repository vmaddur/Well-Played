// Handles int and float types in our language
public class Value {
    private int integer;
    private float dec;
    // True = float, False = int
    private boolean flag;

    public Value(int n) {
        integer = n;
        flag = false;
    }

    public Value(float d) {
        dec = d;
        flag = true;
    }

    public boolean isFloat() {
        return flag;
    }

    // Setter Methods
    public void setFloatValue(float d) {
        dec = d;
    }

    public void setIntegerValue(int n) {
        integer = n;
    }

    public Number getValue() {
        if (flag)
            return dec;
        else
            return integer;
    }
}
