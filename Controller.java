import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Controller {
    //Parameters
    static int cport;
    static int R;
    static int timeout;
    static int rebalance_period;

    static ArrayList<DStoreThread> dstores;
    static HashMap<String, StorageThread> ongoingUploads;
    static Index index;
    static String file_list = "";

    public static void main(String[] args) throws IOException{
        ControllerLogger.init(Logger.LoggingType.ON_TERMINAL_ONLY);
        
        args = Parser.parse(args, 4);
        dstores = new ArrayList<DStoreThread>();
        index = new Index();
        
        cport = Integer.parseInt(args[0]);
        R = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        rebalance_period = Integer.parseInt(args[3]);

        try {
            ServerSocket controllerSS = new ServerSocket(cport);
            for (;;) {
                try {
                    Socket client = controllerSS.accept();
                    String line = receiveMessage(client);
                    String[] parsedLine = Parser.parse(line);
                    String command = parsedLine[0];
                    if(command.equals("JOIN")){
                        ControllerLogger.getInstance().dstoreJoined(client, Integer.parseInt(parsedLine[1]));
                        DStoreThread dStoreThread = new DStoreThread(client, Integer.parseInt(parsedLine[1]));
                        dstores.add(dStoreThread);
                        dStoreThread.start();
                    }else{
                        while(line != null){
                            executeClientCommand(command, parsedLine, client);
                            line = receiveMessage(client);
                            if(line == null) break;
                            parsedLine = Parser.parse(line);
                            command = parsedLine[0];
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

    private static void executeClientCommand(String command, String[] parsedLine, Socket client) throws IOException{
        if(command.equals("STORE")){     
            String filename = parsedLine[1];                       
            if(dstores.size() < R){
                sendMessage(client, "ERROR_NOT_ENOUGH_DSTORES");
            }else if (index.getFileInfo(filename) != null){
                sendMessage(client, "ERROR_FILE_ALREADY_EXISTS");
            }else{
                index.add(filename, "store in progress");
                int[] ports = new int[R];
                for(int i = 0; i < R; i++){
                    ports[i] = dstores.get(i).port;
                }
                String portsList = "";
                for(int i = 0; i < R - 1; i++){
                    portsList += ports[i] + " ";
                }
                portsList += ports[R-1];
                sendMessage(client, "STORE_TO " + portsList);
                ongoingUploads.put(filename, new StorageThread(filename, ports, client));
            }
        }else if(command.equals("LIST")){
            sendMessage(client, "LIST " + file_list);
        }
    }

    static class DStoreThread extends Thread {
        private Socket client;
        public int port;

        public DStoreThread(Socket client, int port) {
            this.client = client;
            this.port = port;
        }

        @Override
        public void run() {
            String line;
            String[] parsedLine;
            String command;
            while(true){
                try{
                    line = receiveMessage(client);
                    if(line == null){ break; }
                    parsedLine = Parser.parse(line);
                    command = parsedLine[0];
                    executeDstoreCommand(command, parsedLine, client);
                }catch(IOException e) {
                    break;
                }
            }
            dstores.remove(this);
        }

        private void executeDstoreCommand(String command, String[] parsedLine, Socket client){
            if(command.equals("STORE_ACK")){
                ongoingUploads.get(parsedLine[1]).ack(port);
            } 
        }

    }

    static class StorageThread extends Thread {
        HashMap<Integer, Boolean> acks;
        String filename;
        Socket client;
        public StorageThread(String filename, int[] ports, Socket client){
            acks = new HashMap<Integer, Boolean>();
            for(int port : ports){
                acks.put(port, false);
            }
            this.filename = filename;
            this.client = client;
        }
        @Override
        public void run(){
            for(;;){
                if(!acks.values().contains(false)){
                    try {
                        sendMessage(client, "STORAGE_COMPLETE");
                        index.changeStatus(filename, "store complete");
                        if (file_list == ""){
                            file_list = filename;
                        }else{
                            file_list += " " + filename;
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }

        public void ack(int port){
            acks.put(port, true);
        }
    }

}

class Index {
    class FileInfo {
        public String name;
        public String status;

        public FileInfo(String name, String status){
            this.name = name;
            this.status = status;
        }
    }

    private ArrayList<FileInfo> index;
    
    public Index(){
        index = new ArrayList<FileInfo>();
    }

    public boolean add(String name, String status){
        if(getFileInfo(name) == null){
            index.add(new FileInfo(name, status));
            return true;
        }else{
            return false;
        }
    }

    public void changeStatus(String name, String status){
        getFileInfo(name).status = status;
    }

    public FileInfo getFileInfo(String name){
        for(FileInfo fileInfo : index){
            if(fileInfo.name == name){
                return fileInfo;
            }
        }
        return null;
    }




}