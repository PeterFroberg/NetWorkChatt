import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class Client2 implements Runnable {
    private final static String DEFAULTHOST = "127.0.0.1";
    private final static int DEFAULTPORT = 2000;

    private final BufferedReader socketReader;

    private Client2(BufferedReader socketReader) {this.socketReader = socketReader; }

    public void run() {
        while(true) {
            try {
                if(socketReader.ready()){
                    String line = socketReader.readLine();
                    if(line != "kollarOmKlientenLever"){
                        System.out.println(line);
                    }
                }
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        /**
         * Parse start arguments
         */
        String host;
        int port;
        System.out.println("Client started");
        if(args.length >= 1) {
            host = args[0];
            if(args.length == 2){
                port = Integer.parseInt(args[1]);
            }else{
                port = DEFAULTPORT;
            }
        }else {
            host = DEFAULTHOST;
            port = DEFAULTPORT;
        }

        /**
         * Creating connection to remote server
         */
        Socket socket = null;
        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;
        BufferedReader consoleReader = null;

        try {
            socket =new Socket(host, port);
            SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
            System.out.println("Connected to server" + remoteSocketAddress);

            socketWriter = new PrintWriter(socket.getOutputStream(),true);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            /**
             * Creates a sending thread
             */
            Thread sendThread = new Thread(new Client2(socketReader));
            sendThread.start();

            /**
             * sending messages
             */

            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String message = consoleReader.readLine();
            while (message != "closechatt"){
                if(message != null && !message.isEmpty()){
                    socketWriter.println(message);
                }
                message = consoleReader.readLine();
            }

            sendThread.interrupt();
            sendThread.join();
            System.out.println("Closing connection to server:" +  remoteSocketAddress);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        /**
         * Closes all opened connections
         */
        finally {
            try {
                if (socketWriter != null) {
                    socketWriter.close();
                }
                if (socketReader != null) {
                    socketReader.close();
                }
                if (socket != null){
                    socket.close();
                }
                if(consoleReader != null){
                    consoleReader.close();
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
