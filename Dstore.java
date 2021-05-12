import java.io.*;
import java.net.*;
import java.util.List;

public class Dstore {
    private static int port;
    private static int cport;
    private static int timeout;
    private static File file_folder;

    public int socketPort;
    public int serverPort;
    
    public Dstore(int serverPort, int socketPort){
        this.serverPort = serverPort;
        this.socketPort = socketPort;
    }
    
    public static void main(String[] args) throws IOException{
        DstoreLogger.init(Logger.LoggingType.ON_TERMINAL_ONLY, port);
        
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
                    sendMessage(controller, "JOIN " + port);
                    Socket client = dstoreSS.accept();
    
                    while(true){
                        String line = receiveMessage(client);
                        if(line == null) continue;
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(command.equals("STORE")){
                            String filename = parsedLine[1];
                            int filesize = Integer.parseInt(parsedLine[2]); 

                            File file = new File(file_folder, filename);
                            OutputStream fileOutput = new FileOutputStream(file);
                            
                            sendMessage(client, "ACK");
                            InputStream clientIS = client.getInputStream();
                            fileOutput.write(clientIS.readNBytes(filesize));
                            fileOutput.close();

                            sendMessage(controller, "STORE_ACK " + filename);
                        } else if (command.equals("LOAD_DATA")){
                            String filename = parsedLine[1];
                            File file = new File(file_folder, filename);
                            
                            byte[] bytearray = new byte[(int) file.length()];
                            BufferedInputStream fileBIS = new BufferedInputStream(new FileInputStream(file));
                            fileBIS.read(bytearray);
                            fileBIS.close();

                            OutputStream clientOS = client.getOutputStream();
                            clientOS.write(bytearray);
                            clientOS.flush();

                        } else if (command.equals("REMOVE")){
                            String filename = parsedLine[1];
                            File file = new File(file_folder, filename);
                            file.delete();
                            sendMessage(controller, "REMOVE_ACK " + filename);
                        } else {
                            System.out.println("Malformed message received: '" + line + "'");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Error setting up serversocket: " + e);
        }
    }

    private static void sendMessage(Socket socket, String message) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
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