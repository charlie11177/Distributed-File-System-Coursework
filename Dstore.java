import java.io.*;
import java.net.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Dstore {
    static int port;
    static int cport;
    static int timeout;
    static String file_folder;
    public static void main(String[] args) throws IOException{
        DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
        
        args = Parser.parse(args, 4);
        
        port = Integer.parseInt(args[0]);
        cport = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        file_folder = args[3];

        try {
            ServerSocket dstoreSS = new ServerSocket(port);
            try {
                Socket socket = new Socket("localhost", cport);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.write("STORE x x");
                out.newLine();
                out.flush();
    
                String response = in.readLine();
                System.out.println(response);
       
    

            } catch (Exception e) {
                System.out.println("error" + e);
            }
        } catch (Exception e) {
            System.out.println("Error setting up serversocket: " + e);
        }
    }


    private static void log(String message){
        String ts = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss]: ").format(new Date());
        DstoreLogger.getInstance().log(ts + message);
    }
}