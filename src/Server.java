import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.*;

public class Server implements Runnable {

    private final static int DEFAULTPORT = 2000;
    /**
     * Array that holds all messages to be sent to the different clients that is connected
     */
    private static CopyOnWriteArrayList<LinkedBlockingQueue> clientMessageQueues;

    private final static Object lock = new Object();

    private final Socket clientSocket;
    private String clientName;
    private LinkedBlockingQueue clientMessageQueue;

    private Server(Socket clientSocket, LinkedBlockingQueue clientMessageQueue, CopyOnWriteArrayList clientMessageQueues) {
        this.clientSocket = clientSocket;
        this.clientMessageQueue = clientMessageQueue;
        this.clientMessageQueues = clientMessageQueues;
    }

    /**
     * Method to start when thread is started, server is a implements the runnable interface
     * The server start one new thread for each client that connects to the server
     */
    public void run() {
        SocketAddress remoteSocketAddress = clientSocket.getRemoteSocketAddress();
        SocketAddress localSocketAddress = clientSocket.getLocalSocketAddress();
        System.out.println("Accepted client " + remoteSocketAddress
                + " (" + localSocketAddress + ").");
        System.out.println("There is now: " + clientMessageQueues.size() + " clients connected to the server.");

        /**
         * Creates reader and writer for communication to and from the client
         */
        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;

        try {
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            PrintWriter finalSocketwriter = socketWriter;
            /**
             * Creates a new thread that sends new messages to the client
             */
            new Thread(() -> {
                String mess;
                while (true) {
                    try {
                        /**
                         * wait for a new message to arrive in the clients messagequeue and the send it to the client
                         */
                        mess = (String) clientMessageQueue.take();
                        //System.out.println("skickar till client in thread " + Thread.currentThread().getName() + " : " + mess);
                        finalSocketwriter.println(mess);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String threadInfo = "(" + Thread.currentThread().getName() + ")";
            String inputLine = socketReader.readLine();
            //System.out.println("Received: \"" + inputLine + "\" from" + remoteSocketAddress + threadInfo);

            clientName = inputLine;

            while (inputLine != null) {
                synchronized (lock) {
                    for (LinkedBlockingQueue que : clientMessageQueues) {
                        if(que != clientMessageQueue) {
                            que.put(clientName + ": " + inputLine);
                        }
                    }
                }
                Thread.sleep(100);
                try {
                    inputLine = socketReader.readLine();
                }catch (SocketException e){
//                    System.out.println("Client disconnected!");
//                    System.out.println("There is now: " + clientMessageQueues.size() + " clients connected to the server.");
                    break;
                }
                //System.out.println("Received: \"" + inputLine + "\" from " + clientName + " " + remoteSocketAddress +threadInfo);
            }
            System.out.println("Closing connection " + remoteSocketAddress + " (" + localSocketAddress + ").");
        }catch (SocketException e){

        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client disconnected!");
            clientMessageQueues.remove(clientMessageQueue);
            System.out.println("There is now: " + clientMessageQueues.size() + " clients connected to the server.");
            try {
                if (socketWriter != null)
                    socketWriter.close();
                if (socketReader != null)
                    socketReader.close();
                if (clientSocket != null)
                    clientSocket.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Server started!");

        /**
         * Creates Sockets for server and client connections
         */
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        /**
         * Sets port to argument received or as DEFAULTPORT
         */
        int port;
        System.out.println("Client started");
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else {
            port = DEFAULTPORT;
        }

        try {
            /**
             * start the server on Port:port
             */
            serverSocket = new ServerSocket(port);
            SocketAddress serverSocketAdress = serverSocket.getLocalSocketAddress();
            System.out.println("Listening (" + serverSocketAdress + ")");
            CopyOnWriteArrayList<LinkedBlockingQueue> clientMessageQueues = new CopyOnWriteArrayList<>();
            LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

            /**
             * wait for incoming connections from clients and spawn a new thread for each client
             * and waits for the next client to connect
             */
            while(true){
                clientSocket = serverSocket.accept();
                LinkedBlockingQueue<String> clientMessageQueue = new LinkedBlockingQueue<>();
                clientMessageQueues.add(clientMessageQueue);
                //System.out.println("There is now: " + clientMessageQueues.size() + " clients connected to the server.");
                Server server = new Server(clientSocket, clientMessageQueue, clientMessageQueues);
                Thread thread = new Thread(server);
                thread.start();
                //executor.execute(new Server(clientSocket, clientMessageQueue, clientMessageQueues));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            /**
             * Close the socket that is waiting for clients
             */
            try {
                if(serverSocket != null){
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
