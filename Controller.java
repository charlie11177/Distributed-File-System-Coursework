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
                    while(true){
                        String line = receiveMessage(client);
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(parsedLine.length != 1){
                            String[] commandargs = Arrays.copyOfRange(parsedLine, 1, parsedLine.length - 1);
                        }
                        if(command.equals("STORE")){
                            //System.out.println("Client would like to store! first argument ");
                            //outToClient.write("Store command received");
                            //outToClient.newLine();
                            //outToClient.flush();
                        }else if(command.equals("LIST")){
                            sendMessage(client, "LIST " + file_list);
                        }else{

                        }
                    }
                } catch (Exception e) {
                    System.out.println("Dstore Disconnected");
                }
            }
        } catch (Exception e) {
            System.out.println("error " + e);
        }

    }

    public static void sendMessage(Socket socket, String message) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
        ControllerLogger.getInstance().messageSent(socket, message);
    }

    public static String receiveMessage(Socket socket) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message = in.readLine();
        if(message != null){
            ControllerLogger.getInstance().messageReceived(socket, message);
        }
        return message;
    }
}