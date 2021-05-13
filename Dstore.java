import java.io.*;
import java.net.*;

public class Dstore {
    public static int port;
    public static int cport;
    public static int timeout;
    public static File file_folder;


    public static void main(String[] args) throws IOException{
        DstoreLogger.init(Logger.LoggingType.ON_TERMINAL_ONLY, port);
            
        args = Parser.parse(args, 4);
        
        port = Integer.parseInt(args[0]);
        cport = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        file_folder = new File(args[3]);

        ServerSocket dstoreServer = new ServerSocket(port);

        while(true){
            try {
                Socket controller = new Socket("localhost", cport);
                sendMessage(controller, "JOIN " + port);
                ControllerListener controllerListener = new ControllerListener(controller);
                controllerListener.start();
                while(true){
                    Socket client = dstoreServer.accept();
                    ClientListener clientListener = new ClientListener(client, controller);
                    clientListener.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                //TODO: handle exception
            }
        }
    }

    //SOCKET SEND AND RECEIVE
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

    static class ClientListener extends Thread {
        Socket client;
        Socket controller;
    
        public ClientListener(Socket client, Socket controller){
            this.client = client;
            this.controller = controller;
        }
    
        @Override
        public void run() {
            while(client.isConnected()){
                try {
                    String line = receiveMessage(client);
                    if(line != null){
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(command.equals(Protocol.STORE_TOKEN)){
                            sendMessage(client, Protocol.ACK_TOKEN);
                            String filename = parsedLine[1];
                            int filesize = Integer.parseInt(parsedLine[2]);
    
                            File file = new File(file_folder, filename);
                            OutputStream fileOutput = new FileOutputStream(file);
                            InputStream clientInput = client.getInputStream();
                            fileOutput.write(clientInput.readNBytes(filesize));
                            fileOutput.close();
    
                            sendMessage(controller, Protocol.STORE_ACK_TOKEN + " " + filename);
                        }else if(command.equals(Protocol.LOAD_DATA_TOKEN)){
                            String filename = parsedLine[1];
    
                            File file = new File(file_folder, filename);
                            if(!file.exists()){
                                client.close();
                            }
    
                            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                            byte[] bytearray = bis.readAllBytes();
    
                            OutputStream os = client.getOutputStream();
                            os.write(bytearray);
                            os.flush();
                            bis.close();
                        }
                        

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO: handle exception
                }
            }
            
        }
    }

    static class ControllerListener extends Thread {
         Socket controller;

         public ControllerListener(Socket controller){
             this.controller = controller;
         }

         @Override
         public void run(){
             while(controller.isConnected()){
                 try {
                    String line = receiveMessage(controller);
                    if(line != null){
                        String[] parsedLine = Parser.parse(line);
                        String command = parsedLine[0];
                        if(command.equals(Protocol.REMOVE_TOKEN)){
                            String filename = parsedLine[1];
    
                            File file = new File(file_folder, filename);
                            if(file.delete()){
                                sendMessage(controller, Protocol.REMOVE_ACK_TOKEN + " " + filename);
                            }else{
                                sendMessage(controller, Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN + " " + filename);
                            }
                        }else if(command.equals(Protocol.LIST_TOKEN)){
                            String file_list = "";
                            for(File file : file_folder.listFiles()){
                                file_list += file.getName() + " ";
                            }
                            file_list = file_list.trim();
                            sendMessage(controller, Protocol.LIST_TOKEN + " " + file_list);
                        }
                    }
                } catch (Exception e) {
                     //TODO: handle exception
                 }
             }
         }
    }
}
