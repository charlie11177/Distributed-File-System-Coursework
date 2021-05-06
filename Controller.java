import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Controller {
    //Parameters
    static int cport;
    static int R;
    static int timeout;
    static int rebalance_period;

    static String file_list = "";

    public static void main(String[] args) throws IOException{
        ControllerLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL);
        
        args = Parser.parse(args, 4);
        
        cport = Integer.parseInt(args[0]);
        R = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        rebalance_period = Integer.parseInt(args[3]);

        try {
            ServerSocket controllerSS = new ServerSocket(cport);
            for (;;) {
                try {
                    Socket client = controllerSS.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                    
                    String line;
                    while((line = in.readLine()) != null){
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(parsedLine.length != 1){
                            String[] commandargs = Arrays.copyOfRange(parsedLine, 1, parsedLine.length - 1);
                        }
                        switch(command){
                            case "STORE":
                                System.out.println("Client would like to store! first argument ");
                                out.write("Store command received");
                                out.newLine();
                                out.flush();
                                break;
                            case "LIST":
                                out.write(file_list);
                                out.newLine();
                                out.flush();
                            default:
                                System.out.println("Received command: " + command);
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("error" + e);
                }
            }
        } catch (Exception e) {
            System.out.println("error " + e);
        }

    }

    private static void log(String message){
        String ts = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss]: ").format(new Date());
        ControllerLogger.getInstance().log(ts + message);
    }
}