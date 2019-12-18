package g53sqm.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

    private Socket socket = null;
    private BufferedReader console = null; // client's terminal
    private PrintWriter streamOut = null;  // send messages to server via socket output stream
    private BufferedReader streamIn = null; // get responses from server via socket input stream
    private ServerResponse serverResponse;
    private ConsoleInput consoleInput;
    private ClientDriverGUI cgui = null; // GUI reference for easy manipulation of UI elements
    private boolean connected = false; // flag to check connection status

    private String username = "";

    // constructor for basic Client using computer terminal
    public Client(String serverIp, int serverPort) {

        startConnection(serverIp, serverPort);

        // use threads for both server response and client input as IO operations are blocking calls
        serverResponse = new ServerResponse();
        Thread serverResponseThread = new Thread(serverResponse);
        serverResponseThread.start();

        consoleInput = new ConsoleInput();
        Thread consoleInputThread = new Thread(consoleInput);
        consoleInputThread.start();


    }

    public Client(String serverIp, int serverPort, ClientDriverGUI cg){

        startConnection(serverIp, serverPort);

        cgui = cg;

        // we do not need ConsoleInput here as the GUI will be responsible for sending messages
        serverResponse = new ServerResponse();
        Thread serverResponseThread = new Thread(serverResponse);
        serverResponseThread.start();

    }

    public void startConnection(String serverIp, int serverPort) {

        try{
            // establish connection
            socket = new Socket(serverIp,serverPort);

            // set-up input and output streams
            console = new BufferedReader(new InputStreamReader(System.in));
            streamOut = new PrintWriter(socket.getOutputStream(),true);
            streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
        }
        catch(UnknownHostException uhe){
            System.out.println("Connection Error :" + uhe.getMessage());
        }
        catch(IOException ioe){
            System.out.println("IO Error :" + ioe.getMessage());
        }


    }

    // graceful exit of a Client
    public void closeConnection(){

        // clean up threads
        if(serverResponse!=null) serverResponse.shutdown();

        if(consoleInput!=null) consoleInput.shutdown();

        // clean up IO streams
        try{
            if(console!=null) console.close();
            if(streamOut!=null) streamOut.close();
            if(streamIn!=null) streamIn.close();
        }
        catch(IOException ioe){
            System.out.println("Error closing IO streams: " + ioe.getMessage());
        }


        // finally we close the sockets
        try{
            if(socket!=null) socket.close();
        }
        catch(IOException ioe){
            System.out.println("Error closing socket: " + ioe.getMessage());
        }




    }

    public void sendMessage(String message){
        streamOut.println(message);
    }

    public boolean getConnectionStatus(){
        return connected;
    }

    public void validateUsername(String username){
        this.username = username;
        sendMessage("IDEN " + username);
    }

    private class ServerResponse implements Runnable {

        private volatile boolean running = true; // volatile to force thread to read from main memory

        @Override
        public void run() {

            while(running){

                if(streamIn != null) {

                    try{
                        String response = streamIn.readLine();

                        // check if we are using gui or not
                        if(cgui != null){
                            if(response.equals("BAD username is already taken")){
                                cgui.setSplashErrorMessage("Username already exists!");
                                cgui.transition(false);
                            }
                            else if(response.equals("OK Welcome to the chat server " + cgui.getUsername())){
                                cgui.appendChat(response);
                                cgui.transition(true);
                            }
                            else{
                                cgui.appendChat(response);
                            }
                        }
                        else{
                            System.out.println(response);
                        }

                    }
                    catch(IOException ioe){
                        System.out.println("Error reading from server socket: " + ioe.getMessage());
                    }

                }

            }
        }

        public void shutdown() {
            running = false;
        }

    }

    private class ConsoleInput implements Runnable {

        private String input = "";
        private volatile boolean running = true; // volatile to force thread to read from main memory

        @Override
        public void run() {

            while(running){

                if(streamOut != null && console != null) {

                    try{
                        input = console.readLine();
                        sendMessage(input);

                        if(input.equals("QUIT")){
                            closeConnection(); // try to exit gracefully after calling QUIT to prevent NullPointerException in server
                        }
                    }
                    catch(IOException ioe){
                        System.out.println("Error sending to server socket: " + ioe.getMessage());
                    }

                }

            }
        }

        public void shutdown() {
            running = false;
        }
    }

    public static void main(String args[]){

        Client client = null;

        if(args.length !=2){
            System.out.println("Usage: java Client ip port");
        }
        else{
            client = new Client(args[0], Integer.parseInt(args[1]));
        }

    }

}
