import java.io.*;
import java.util.*;

public class Driver {

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(new File("input.txt"));
        String s = new String();
        while (scan.hasNextLine()) {
            s += scan.nextLine();
        }
        Tokenizer tokenizer = new Tokenizer();
        ArrayList<Token> arr = tokenizer.tokenize(s);
        for (Token t : arr){
            System.out.println(t.toString());
        }
    }
}
