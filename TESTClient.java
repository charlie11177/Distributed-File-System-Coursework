import java.io.*;
import java.net.*;

public class TESTClient {
    public static void main(String[] args){
        int port = Integer.parseInt(args[0]); 
        try {
            Socket  socket = new Socket("localhost", port);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            File myFile = new File("FILE.txt");
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(mybytearray, 0, mybytearray.length);

            out.write("STORE FILE.txt " + myFile.length());
            out.newLine();
            out.flush();

            String response = in.readLine();
            System.out.println("Response: " + response);
            
            OutputStream os = socket.getOutputStream();
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();

            socket.close();

        } catch (Exception e) {
            System.out.println("error" + e);
        }
    }
}
