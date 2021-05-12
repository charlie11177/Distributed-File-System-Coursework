import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Controller {
    public static int cport;
    public static int R;
    public static int timeout;
    public static int rebalance_period;

    public static ArrayList<DstoreListener> dstores;
    public static HashMap<String, GetStorageAcks> ongoingUploads;
    public static Index index;


    public static void main(String[] args) throws IOException{
        ControllerLogger.init(Logger.LoggingType.ON_TERMINAL_ONLY);
        dstores = new ArrayList<DstoreListener>();
        ongoingUploads = new HashMap<String, GetStorageAcks>();
        index = new Index();
        
        args = Parser.parse(args, 4);
        cport = Integer.parseInt(args[0]);
        R = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        rebalance_period = Integer.parseInt(args[3]);

        ServerSocket controllerServer = new ServerSocket(cport);

        while (true){
            Socket client = controllerServer.accept();
            String line = receiveMessage(client);
            String[] parsedLine = Parser.parse(line);
            String command = parsedLine[0];
            if(command.equals(Protocol.JOIN_TOKEN)){
                int port = Integer.parseInt(parsedLine[1]);
                ControllerLogger.getInstance().dstoreJoined(client, port);
                DstoreListener dstoreListener = new DstoreListener(client, port);
                dstores.add(dstoreListener);
                dstoreListener.start();
            }else{
                ClientListener clientListener = new ClientListener(client, line);
                clientListener.start();
            }
        }

    }

    //SOCKET SEND AND RECEIVE
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

    public static String sendMessageAndAwaitReply(Socket socket, String message) throws IOException{
        sendMessage(socket, message);
        socket.setSoTimeout(timeout);
        String reply = receiveMessage(socket);
        socket.setSoTimeout(0);
        return reply;
    }


    static class DstoreListener extends Thread {
        Socket dstore;
        public int port;
    
        public DstoreListener(Socket dstore, int port){
            this.dstore = dstore;
            this.port = port;
        }
        
        @Override
        public void run() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            while(dstore.isConnected()){
                try {
                    String line = receiveMessage(dstore);
                    String[] parsedLine = Parser.parse(line);
                    String command = parsedLine[0];
                    if(command.equals(Protocol.STORE_ACK_TOKEN)){
                        String filename = parsedLine[1];
                        ongoingUploads.get(filename).ack(port);
                    }

                } catch (Exception e) {
                    //TODO: handle exception
                }
            }
        }
    }
    
    static class ClientListener extends Thread {
        Socket client;
        String line;
    
        public ClientListener(Socket client, String line){
            this.client = client;
            this.line = line;
        }
    
        @Override
        public void run() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            while(client.isConnected()){
                try {
                    String[] parsedLine = Parser.parse(line);
                    String command = parsedLine[0];
                    if(command.equals(Protocol.STORE_TOKEN)){
                        String filename = parsedLine[1];
                        if(dstores.size() < R){
                            sendMessage(client, Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
                        }else if(index.fileExists(filename)){
                            sendMessage(client, Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
                        }else{
                            index.add(filename, "store in progress");
                            int ports[] = new int[R];
                            String portsList = ""; 
                            for(int i = 0; i < R; i++){
                                ports[i] = dstores.get(i).port;
                            }
                            for(int port : ports){
                                portsList += port + " ";
                            }
                            portsList = portsList.trim();
    
                            sendMessage(client, Protocol.STORE_TO_TOKEN + " " + portsList);
                            GetStorageAcks storageAck = new GetStorageAcks(ports);
                            ongoingUploads.put(filename, storageAck);
                            Future<Boolean> future = executor.submit(storageAck);
                            if(future.get(timeout, TimeUnit.MILLISECONDS)){
                                sendMessage(client, Protocol.STORE_COMPLETE_TOKEN);
                                index.changeStatus(filename, "store complete");
                            }
                        }
                    }else if(command.equals(Protocol.LIST_TOKEN)){
                        sendMessage(client, Protocol.LIST_TOKEN + " " + index.getFileList());
                    }
                    line = receiveMessage(client);
                } catch (Exception e) {
                    break;
                }

            }

            // TODO Auto-generated method stub
            
        }
    }
    
    static class GetStorageAcks implements Callable<Boolean> {
        HashMap<Integer, Boolean> acks;

        public GetStorageAcks(int[] ports){
            acks = new HashMap<Integer, Boolean>();
            for(int port : ports){
                acks.put(port, false);
            }
        }

        public void ack(int port){
            acks.put(port, true);
        }
    
        @Override
        public Boolean call() throws Exception {
            while(true){
                if(!acks.values().contains(false)){
                    return true;
                }
            }
        }
        
    }
}


