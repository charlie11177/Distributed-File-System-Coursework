import java.io.IOException;

public class Parser {
    public static void main(String[] args) throws IOException {

    }

    public static String[] parse(String args, int amount) throws IOException {
        String[] splitArgs = args.split(" ");
        if(splitArgs.length != (amount + 1)) throw new IOException("Invalid paramters\nExpected:" + amount + "\nActual:" + splitArgs.length);
        return splitArgs;
    }

    public static String[] parse(String[] args, int amount) throws IOException {
        if(args.length != (amount)) throw new IOException("Invalid paramters\nExpected:" + amount + "\nActual:" + args.length);
        return args;
    }

    public static String[] parse(String args){
        return args.split(" ");
    }
}