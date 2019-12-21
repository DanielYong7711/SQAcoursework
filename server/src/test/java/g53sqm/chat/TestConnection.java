package g53sqm.chat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;

import static g53sqm.chat.Connection.STATE_REGISTERED;
import static g53sqm.chat.Connection.STATE_UNREGISTERED;
import static org.junit.Assert.*;

public class TestConnection {
    private Server test_server;
    private int test_port_no;
    private Thread test_server_thread;
    private ArrayList<Connection> connectionsList;

    private Socket createConnection(int test_port_no) {
        Socket test_socket = null;
        try {
            test_socket = new Socket("localhost", test_port_no);
        } catch (IOException e) {
            Assert.fail("Failed to create mock clients");
        }

        try {
            Field field = Server.class.getDeclaredField("list");
            field.setAccessible(true);
            connectionsList = (ArrayList<Connection>) field.get(test_server);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Cannot access connection");
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return test_socket;
    }

    private String socketReceiveMessage(Socket test_socket) {
        String text = "";
        try {
            InputStreamReader is_reader = new InputStreamReader(test_socket.getInputStream());
            BufferedReader b_reader = new BufferedReader(is_reader);
            text = b_reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    private void socketEnterCommandAndText(Socket socket, String text) {
        try {
            PrintWriter user_out = new PrintWriter(socket.getOutputStream(), true);
            user_out.println(text);
            Thread.sleep(1000);
        } catch (IOException | InterruptedException ie) {
            Assert.fail("Failed to send command and text");
        }
    }

    //initialise server
    @Before
    public void initialiseServer() {
        test_server = new Server(0);
        test_port_no = test_server.getPortNo();
        test_server_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Test server thread started");
                test_server.listen();
            }
        });

        // Set thread as Daemon to automatically terminate when JVM terminates
        test_server_thread.setDaemon(true);
        test_server_thread.start();

        // Sleep for 1 second for thread execution
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void stopServer() {
        test_server.stopListening();
    }

    @Test
    public void socket_connectFirstTime_getWelcomeMessage() {
        Socket test_socket = createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);
        assertTrue(test_connection.isRunning());
        String expectedWelcome = "OK Welcome to the chat server, there are currently 1 user(s) online";
        String actualWelcome = socketReceiveMessage(test_socket);
        assertEquals(expectedWelcome, actualWelcome);
    }

    @Test
    public void connection_connectFirstTime_stateUnregisteredWithNoUsername() {
        createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);
        assertTrue(test_connection.isRunning());
        assertEquals(STATE_UNREGISTERED, test_connection.getState());
        assertNull(test_connection.getUserName());
    }

    @Test
    public void validateMessage_invalidCommand_sendErrorMessageForWrongCommand() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message

        String expected_short_error_message = "BAD invalid command to server";
        String expected_long_error_message = "BAD default command not recognised";

        //Empty command
        socketEnterCommandAndText(test_socket, "");
        String actual_error_message = socketReceiveMessage(test_socket);
        assertEquals(expected_short_error_message, actual_error_message);

        //Short invalid command
        socketEnterCommandAndText(test_socket, "YES");
        actual_error_message = socketReceiveMessage(test_socket);
        assertEquals(expected_short_error_message, actual_error_message);

        //Exact invalid command
        socketEnterCommandAndText(test_socket, "FAIL");
        actual_error_message = socketReceiveMessage(test_socket);
        assertEquals(expected_long_error_message, actual_error_message);

        //Long invalid command
        socketEnterCommandAndText(test_socket, "PLEASE");
        actual_error_message = socketReceiveMessage(test_socket);
        assertEquals(expected_long_error_message, actual_error_message);
    }

    @Test
    public void stat_userUnregistered_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message

        socketEnterCommandAndText(test_socket, "STAT");
        String message_p1 = "OK ";
        String message_p2 = "There are currently 1 user(s) on the server ";
        String message_p3 = "You have not logged in yet";

        String expected_message = message_p1 + message_p2 + message_p3;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void stat_userRegisteredNoMessageSent_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);

        socketEnterCommandAndText(test_socket, "IDEN client");
        socketReceiveMessage(test_socket); //welcome message

        socketEnterCommandAndText(test_socket, "STAT");
        String message_p1 = "OK ";
        String message_p2 = "There are currently 1 user(s) on the server ";
        String message_p3 = "You are logged in and have sent 0 message(s)";

        String expected_message = message_p1 + message_p2 + message_p3;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void stat_userRegisteredMessageSent_returnCorrectMessageWithCount() {
        Socket test_socket = createConnection(test_port_no);

        socketEnterCommandAndText(test_socket, "IDEN client");
        socketEnterCommandAndText(test_socket, "HAIL Message1");
        socketReceiveMessage(test_socket); //clear message

        socketEnterCommandAndText(test_socket, "STAT");
        String message_p1 = "OK ";
        String message_p2 = "There are currently 1 user(s) on the server ";
        String message_p3 = "You are logged in and have sent 1 message(s)";

        String expected_message = message_p1 + message_p2 + message_p3;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void list_userUnregistered_returnErrorMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //clear message

        socketEnterCommandAndText(test_socket, "LIST");
        String expected_message = "BAD You have not logged in yet";
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void list_1userRegistered_returnOneUser() {
        Socket test_socket = createConnection(test_port_no);

        String registered_username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + registered_username);
        socketReceiveMessage(test_socket); //clear message

        socketEnterCommandAndText(test_socket, "LIST");
        String message_p1 = "OK ";
        String message_p2 = registered_username;
        String message_p3 = ", ";
        String expected_message = message_p1 + message_p2 + message_p3;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void list_multipleUserRegistered_returnOneUser() {
        Socket test_socket1 = createConnection(test_port_no);
        Socket test_socket2 = createConnection(test_port_no);

        String registered_username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + registered_username1);
        socketReceiveMessage(test_socket1); //clear message

        String registered_username2 = "client2username";
        socketEnterCommandAndText(test_socket2, "IDEN " + registered_username2);
        socketReceiveMessage(test_socket2); //clear message

        String message_p1 = "OK ";
        String message_p2 = ", ";
        String expected_message = message_p1 + registered_username1 + message_p2 + registered_username2 + message_p2;

        //seen by client 1
        socketEnterCommandAndText(test_socket1, "LIST");
        String actual_message = socketReceiveMessage(test_socket1);
        assertEquals(expected_message, actual_message);

        //seen by client2
        socketEnterCommandAndText(test_socket2, "LIST");
        actual_message = socketReceiveMessage(test_socket2);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void iden_noUsername_returnErrorMessageForNoUsername() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message
        socketEnterCommandAndText(test_socket, "IDEN");

        String expected_error_message = "BAD enter valid username";
        String actual_error_message = socketReceiveMessage(test_socket);

        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void iden_blanksUsername_returnErrorMessageForNoUsername() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message
        socketEnterCommandAndText(test_socket, "IDEN  ");

        String expected_error_message = "BAD enter valid username";
        String actual_error_message = socketReceiveMessage(test_socket);

        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void iden_enterUsernameWhenUnregistered_returnWelcomeMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message

        String registered_username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + registered_username);

        String expected_message = "OK Welcome to the chat server " + registered_username;
        String actual_message = socketReceiveMessage(test_socket);

        assertEquals(expected_message, actual_message);
    }

    @Test
    public void iden_enterExistingUsernameWhenUnregistered_returnErrorMessageUsernameTaken() {
        Socket test_socket1 = createConnection(test_port_no);
        socketReceiveMessage(test_socket1); //welcome message
        String registered_username = "clientusername";
        socketEnterCommandAndText(test_socket1, "IDEN " + registered_username);

        Socket test_socket2 = createConnection(test_port_no);
        socketReceiveMessage(test_socket2); //welcome message
        socketEnterCommandAndText(test_socket2, "IDEN " + registered_username);

        String expected_error_message = "BAD username is already taken";
        String actual_error_message = socketReceiveMessage(test_socket2);
        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void iden_enterExistingUsernameMultipleWhenUnregistered_returnErrorMessageUsernameTaken() {
        Socket test_socket1 = createConnection(test_port_no);
        socketReceiveMessage(test_socket1); //initial welcome message

        Socket test_socket2 = createConnection(test_port_no);
        socketReceiveMessage(test_socket2); //initial welcome message

        Socket test_socket3 = createConnection(test_port_no);
        socketReceiveMessage(test_socket3); //initial welcome message


        String registered_username = "clientusername";
        socketEnterCommandAndText(test_socket1, "IDEN " + registered_username);
        socketEnterCommandAndText(test_socket2, "IDEN " + registered_username);
        socketEnterCommandAndText(test_socket3, "IDEN " + registered_username);

        String expected_error_message = "BAD username is already taken";
        String actual_error_message = socketReceiveMessage(test_socket2);
        assertEquals(expected_error_message, actual_error_message);
        actual_error_message = socketReceiveMessage(test_socket3);
        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void iden_enterUsernameWhenRegistered_returnErrorMessageEnteredUsername() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //initial welcome message

        String username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + username);
        socketReceiveMessage(test_socket); //welcome message after enter username

        socketEnterCommandAndText(test_socket, "IDEN " + username);
        String expected_error_message = "BAD you are already registered with username " + username;
        String actual_error_message = socketReceiveMessage(test_socket);
        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void hail__userUnregistered_returnErrorMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //clear message

        String message = "Is this working?";
        socketEnterCommandAndText(test_socket, "HAIL" + message);
        String expected_message = "BAD You have not logged in yet";
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void hail_1userRegisterednoPublicMessageEntered_sendErrorMessageForEmptyMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message
        socketEnterCommandAndText(test_socket, "HAIL ");

        String expected_error_message = "BAD enter valid broadcast";
        String actual_error_message = socketReceiveMessage(test_socket);

        assertEquals(expected_error_message, actual_error_message);
    }

    @Test
    public void hail_1userRegistered1PublicMessageEntered_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //clear message

        String username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + username);
        socketReceiveMessage(test_socket); //welcome message after enter username

        String message = "Is this working?";
        socketEnterCommandAndText(test_socket, "HAIL " + message);
        String expected_message = "Broadcast from " + username + ": " + message;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void hail_1userRegisteredMultiplePublicMessageEntered_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //clear message

        String username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + username);
        socketReceiveMessage(test_socket); //welcome message after enter username

        String message1 = "Is this working?";
        socketEnterCommandAndText(test_socket, "HAIL " + message1);
        String expected_message = "Broadcast from " + username + ": " + message1;
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);

        String message2 = "Can this work?";
        socketEnterCommandAndText(test_socket, "HAIL " + message2);
        expected_message = "Broadcast from " + username + ": " + message2;
        actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void hail_multipleUserRegisteredPublicMessageEntered_returnCorrectMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        String username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //welcome message after enter username

        Socket test_socket2 = createConnection(test_port_no);
        String username2 = "client2username";
        socketEnterCommandAndText(test_socket2, "IDEN " + username2);
        socketReceiveMessage(test_socket2); //welcome message after enter username

        String message1 = "Is this working?";
        socketEnterCommandAndText(test_socket1, "HAIL " + message1);
        String expected_message = "Broadcast from " + username1 + ": " + message1;
        String actual_message1 = socketReceiveMessage(test_socket1);
        assertEquals(expected_message, actual_message1);
        String actual_message2 = socketReceiveMessage(test_socket2);
        assertEquals(expected_message, actual_message2);


        String message2 = "Can this work?";
        socketEnterCommandAndText(test_socket2, "HAIL " + message2);
        expected_message = "Broadcast from " + username2 + ": " + message2;
        actual_message1 = socketReceiveMessage(test_socket1);
        assertEquals(expected_message, actual_message1);
        actual_message2 = socketReceiveMessage(test_socket2);
        assertEquals(expected_message, actual_message2);
    }

    @Test
    public void mesg_userUnregisteredPrivateMessageToActualUser_returnErrorMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        Socket test_socket2 = createConnection(test_port_no);

        String username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //clear message
        socketReceiveMessage(test_socket2); //clear message

        String message = "Is this working?";
        socketEnterCommandAndText(test_socket2, "MESG" + username1 + " " + message);
        String expected_message = "BAD You have not logged in yet";
        String actual_message = socketReceiveMessage(test_socket2);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void mesg_userRegisteredPrivateMessageToNonexistentUser_returnErrorMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        Socket test_socket2 = createConnection(test_port_no);

        String username1 = "client1username";
        String username2 = "client2username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //clear message
        socketReceiveMessage(test_socket2); //clear message

        String message = "Is this working?";
        socketEnterCommandAndText(test_socket1, "MESG " + username2 + " " + message);
        String expected_message = "BAD the user does not exist";
        String actual_message = socketReceiveMessage(test_socket1);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void mesg_userRegisteredPrivateMessageBadlyFormatted_returnErrorMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        String username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //welcome message after enter username

        Socket test_socket2 = createConnection(test_port_no);
        String username2 = "client2username";
        socketEnterCommandAndText(test_socket2, "IDEN " + username2);
        socketReceiveMessage(test_socket2); //welcome message after enter username

        String message = "Help";
        socketEnterCommandAndText(test_socket1, "MESG " + username2 + message);
        String expected_message = "BAD Your message is badly formatted";
        String actual_message = socketReceiveMessage(test_socket1);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void mesg_userRegisteredPrivateMessageExistingUser_returnErrorMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        String username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //welcome message after enter username

        Socket test_socket2 = createConnection(test_port_no);
        String username2 = "client2username";
        socketEnterCommandAndText(test_socket2, "IDEN " + username2);
        socketReceiveMessage(test_socket2); //welcome message after enter username

        String message = "Help";
        socketEnterCommandAndText(test_socket1, "MESG " + username2 + " " + message);
        String expected_sender_message = "OK your message has been sent";
        String actual_sender_message = socketReceiveMessage(test_socket1);
        assertEquals(expected_sender_message, actual_sender_message);

        String expected_receiver_message = "PM from " + username1 + ":" + message;
        String actual_receiver_message = socketReceiveMessage(test_socket2);
        assertEquals(expected_receiver_message, actual_receiver_message);
    }

    @Test
    public void mesg_userRegisterednoPrivateMessageEntered_sendErrorMessageForEmptyMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        String username1 = "client1username";
        socketEnterCommandAndText(test_socket1, "IDEN " + username1);
        socketReceiveMessage(test_socket1); //welcome message after enter username

        Socket test_socket2 = createConnection(test_port_no);
        String username2 = "client2username";
        socketEnterCommandAndText(test_socket2, "IDEN " + username2);
        socketReceiveMessage(test_socket2); //welcome message after enter username

        String message = "";
        socketEnterCommandAndText(test_socket1, "MESG " + message);
        String expected_sender_message = "BAD enter valid pm";
        String actual_sender_message = socketReceiveMessage(test_socket1);
        assertEquals(expected_sender_message, actual_sender_message);
    }

    @Test
    public void quit_userUnregisteredQuit_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        socketReceiveMessage(test_socket); //welcome message

        socketEnterCommandAndText(test_socket, "QUIT");
        String expected_message = "OK goodbye";
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void quit_userRegisteredQuit0MessageSent_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        String username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + username);
        socketReceiveMessage(test_socket); //welcome message

        socketEnterCommandAndText(test_socket, "QUIT");
        String expected_message = "OK thank you for sending 0 message(s) with the chat service, goodbye. ";
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void quit_userRegisteredQuit1MessageSent_returnCorrectMessage() {
        Socket test_socket = createConnection(test_port_no);
        String username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + username);
        socketEnterCommandAndText(test_socket, "HAIL byeee");
        socketReceiveMessage(test_socket); //welcome message

        socketEnterCommandAndText(test_socket, "QUIT");
        String expected_message = "OK thank you for sending 1 message(s) with the chat service, goodbye. ";
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);
    }

    @Test
    public void messageForConnection_sendEmptyMessage_socketReceiveEmptyMessage() {
        Socket test_socket = createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);

        socketReceiveMessage(test_socket); //welcome message

        String expected_message = "";

        test_connection.messageForConnection("");
        String actual_message = socketReceiveMessage(test_socket);
        assertEquals(expected_message, actual_message);

    }

    @Test
    public void messageForConnection_sendMultipleMessage_socketReceiveMessages() {
        Socket test_socket = createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);
        socketReceiveMessage(test_socket); //welcome message

        String expected_message1 = "Hi 1";
        test_connection.messageForConnection(expected_message1);
        String actual_message1 = socketReceiveMessage(test_socket);
        assertEquals(expected_message1, actual_message1);

        String expected_message2 = "Hi 2";
        test_connection.messageForConnection(expected_message2);
        String actual_message2 = socketReceiveMessage(test_socket);
        assertEquals(expected_message2, actual_message2);
    }

    @Test
    public void messageForConnection_sendMessageToMultipleConnections_eachSocketReceiveIntendedMessage() {
        Socket test_socket1 = createConnection(test_port_no);
        Socket test_socket2 = createConnection(test_port_no);
        Connection test_connection1 = connectionsList.get(0);
        Connection test_connection2 = connectionsList.get(1);

        socketReceiveMessage(test_socket1); //welcome message
        socketReceiveMessage(test_socket2);

        String expected_message1 = "Hi Connection 1";
        String expected_message2 = "Hi Connection 2";

        test_connection1.messageForConnection(expected_message1);
        test_connection2.messageForConnection(expected_message2);

        String actual_message1 = socketReceiveMessage(test_socket1);
        String actual_message2 = socketReceiveMessage(test_socket2);

        assertEquals(expected_message1, actual_message1);
        assertEquals(expected_message2, actual_message2);

        assertNotEquals(expected_message1, actual_message2);
        assertNotEquals(expected_message2, actual_message1);
    }

    @Test
    public void getState_userUnregistered_returnStateUnregistered() {
        createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);
        assertEquals(STATE_UNREGISTERED, test_connection.getState());
    }

    @Test
    public void getState_userRegistered_returnStateRegistered() {
        Socket test_socket = createConnection(test_port_no);
        socketEnterCommandAndText(test_socket, "IDEN client");
        Connection test_connection = connectionsList.get(0);
        assertEquals(STATE_REGISTERED, test_connection.getState());
    }

    @Test
    public void getUserName_userUnregistered_returnNull() {
        createConnection(test_port_no);
        Connection test_connection = connectionsList.get(0);
        assertNull(test_connection.getUserName());
    }

    @Test
    public void getUserName_userRegistered_returnRegisteredUsername() {
        Socket test_socket = createConnection(test_port_no);
        String registered_username = "clientusername";
        socketEnterCommandAndText(test_socket, "IDEN " + registered_username);
        Connection test_connection = connectionsList.get(0);
        assertEquals(registered_username, test_connection.getUserName());
    }
}
