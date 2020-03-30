import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketAddress;

public class Client implements Runnable {
    /**
     * Sets default Host address and port
     */
    private final static String DEFAULTHOST = "127.0.0.1";
    private final static int DEFAULTPORT = 2000;

    private final BufferedReader socketReader;

    private Client(BufferedReader socketReader) {
        this.socketReader = socketReader;
    }

    /**
     * starts a receiver thread for the client
     */
    public void run() {
        try {
            /**
             * Keeps the receiver alive for the client
             */
            while (true) {
                if (socketReader.ready()) {
                    String line = socketReader.readLine();
                    System.out.println(line);
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String host;
        int port;
        System.out.println("Client started");

        /**
         * Creating connection to remote server
         */
        Socket socket = null;
        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;
        BufferedReader consoleReader = null;

        try {
            /**
             * Parse start arguments for host and port if arguments are missing sets default values
             */
            if (args.length >= 1) {
                host = args[0];
                if (args.length == 2) {
                    port = Integer.parseInt(args[1]);
                } else {
                    port = DEFAULTPORT;
                }
            } else {
                host = DEFAULTHOST;
                port = DEFAULTPORT;
            }

            /**
             * connects to the server on address:host and port:port
             */
            socket = new Socket(host, port);
            SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
            System.out.println("Connected to server" + remoteSocketAddress);

            socketWriter = new PrintWriter(socket.getOutputStream(), true);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            /**
             * Creates a sending thread
             */
            Thread sendThread = new Thread(new Client(socketReader));
            sendThread.start();

            /**
             * sending messages
             */
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String message = consoleReader.readLine();
            while (message != null && !message.equals("close")) {
                if (message != null && !message.isEmpty()) {
                    socketWriter.println(message);
                }
                message = consoleReader.readLine();
            }

            /**
             * Ends
             */
            sendThread.interrupt();
            sendThread.join();
            System.out.println("Closing connection to server:" + remoteSocketAddress);

            /**
             * catch if there is no server response when connecting
             */
        } catch (ConnectException e) {
            System.out.println("No server available!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**
         * Closes all opened connections and sockets
         */ finally {
            try {
                if (socketWriter != null) {
                    socketWriter.close();
                }
                if (socketReader != null) {
                    socketReader.close();
                }
                if (socket != null) {
                    socket.close();
                }
                if (consoleReader != null) {
                    consoleReader.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
