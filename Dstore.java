import java.io.*;
import java.net.*;
import java.util.Date;

import java.text.SimpleDateFormat;

public class Dstore {
    static int port;
    static int cport;
    static int timeout;
    static File file_folder;
    public static void main(String[] args) throws IOException{
        DstoreLogger.init(Logger.LoggingType.ON_FILE_AND_TERMINAL, port);
        
        args = Parser.parse(args, 4);
        
        port = Integer.parseInt(args[0]);
        cport = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        file_folder = new File(args[3]);

        try {
            ServerSocket dstoreSS = new ServerSocket(port);
            for(;;){
                try {
                    Socket controller = new Socket("localhost", cport);
                    Socket client = dstoreSS.accept();
    
                    while(true){
                        String line = receiveMessage(client);
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(command.equals("STORE")){
                            String filename = parsedLine[1];
                            long filesize = Long.parseLong(parsedLine[2]); 

                            File file = new File(file_folder, filename);
                            OutputStream fileOutput = new FileOutputStream(file);
                            
                            sendMessage(client, "ACK");

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            InputStream clientIS = client.getInputStream();
                            while((bytesRead = clientIS.read(buffer)) != -1){
                                fileOutput.write(buffer, 0, bytesRead);
                            }
                            fileOutput.close();

                            sendMessage(controller, "STORE_ACK " + filename);
                        } else if (command.equals("LOAD_DATA")){
                            String filename = parsedLine[1];
                            File file = new File(file_folder, filename);
                            
                            byte[] bytearray = new byte[(int) file.length()];

                            FileInputStream fis = new FileInputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            bis.read(bytearray, 0, bytearray.length);

                            OutputStream clientOS = client.getOutputStream();
                            clientOS.write(bytearray, 0, bytearray.length);
                            clientOS.flush();

                        } else {
                            System.out.println("Received command: " + command);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Waiting on another client...");
                }
            }
        } catch (Exception e) {
            System.out.println("Error setting up serversocket: " + e);
        }
    }

    public static void sendMessage(Socket socket, String message) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        out.write(message);
        out.newLine();
        out.flush();

        DstoreLogger.getInstance().messageSent(socket, message);
    }

    public static String receiveMessage(Socket socket) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message = in.readLine();
        if(message != null){
            DstoreLogger.getInstance().messageReceived(socket, message);
        }
        return message;
    }
}