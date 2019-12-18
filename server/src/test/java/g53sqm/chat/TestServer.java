package g53sqm.chat;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class TestServer {

    private Server test_server;
    private int test_port_no;
    private Thread test_server_thread;

    public class test_runnable implements Runnable{
        public void run() {

            System.out.println("Test server thread started");
            test_server.listen();
        }
    }

    private Socket createMockUsers(String username, int portNo){
        Socket user = null;
        try{
            user = new Socket("localhost",test_port_no);
            userEnterCommandAndText(user, "IDEN " + username);
        }catch (IOException e) {
            Assert.fail("Mock user setup failed");
        }
        return user;
    }

    private void userEnterCommandAndText(Socket user, String text){
        try{
            PrintWriter user_out = new PrintWriter(user.getOutputStream(), true);
            user_out.println(text);
            Thread.sleep(1000);
        }catch(IOException|InterruptedException ie) {
            Assert.fail("Failed to send command and text");
        }
    }

    private String userReceiveMessage(Socket user){
        String text = "";
        try{
            InputStreamReader is_reader =new InputStreamReader(user.getInputStream());
            BufferedReader b_reader = new BufferedReader(is_reader);
            text = b_reader.readLine();
        }catch(IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    // Initialise server
    @Before
    public void initialiseServer(){

        test_server = new Server(0);
        test_port_no = test_server.getPortNo();
        Runnable runnable = new test_runnable();
        test_server_thread = new Thread(runnable);

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

    @Test
    public void getUserList_noUsersOnline_returnEmptyList(){
        ArrayList<String> actual_users_online = test_server.getUserList();
        String[] expected_users_online = new String[]{};
        assertArrayEquals(actual_users_online.toArray(),expected_users_online);
    }

    @Test
    public void getUserList_multipleUsersOnline_returnCorrectUsernameList(){
        //Create mock clients
        Socket client1 = createMockUsers("client1",test_port_no);
        Socket client2 = createMockUsers("client2",test_port_no);

        ArrayList<String> actual_users_online = test_server.getUserList();
        String[] expected_users_online = new String[2];
        expected_users_online[0] = "client1";
        expected_users_online[1] = "client2";

        assertEquals(expected_users_online[0],actual_users_online.get(0));
        assertEquals(expected_users_online[1],actual_users_online.get(1));
        assertArrayEquals(expected_users_online,actual_users_online.toArray());

    }

    @Test
    public void getUserList_usersOnlineQuit_returnUsernameListWithoutQuit() {
        //Create mock clients
        Socket client1 = createMockUsers("client1",test_port_no);
        Socket client2 = createMockUsers("client2",test_port_no);

        //client1 quits
        userEnterCommandAndText(client1, "QUIT");
        ArrayList<String> actual_users_online = test_server.getUserList();
        String[] expected_users_online = new String[1];
        expected_users_online[0] = "client2";
        assertEquals(expected_users_online[0],actual_users_online.get(0));
        assertArrayEquals(expected_users_online,actual_users_online.toArray());
    }

    @Test
    public void doesUserExist_userOnline_returnTrue(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);

        boolean userFound = test_server.doesUserExist("existing_client1");
        assertTrue(userFound);
    }

    @Test
    public void doesUserExist_multipleUserOnline_returnTrue(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        Socket client2 = createMockUsers("existing_client2",test_port_no);

        boolean userFound = test_server.doesUserExist("existing_client1");
        assertTrue(userFound);
        boolean user2Found = test_server.doesUserExist("existing_client2");
        assertTrue(user2Found);
    }

    @Test
    public void doesUserExist_multipleUserOnlineThenQuit_returnCorrectExist(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        Socket client2 = createMockUsers("existing_client2",test_port_no);

        boolean userFound = test_server.doesUserExist("existing_client1");
        assertTrue(userFound);
        boolean user2Found = test_server.doesUserExist("existing_client2");
        assertTrue(user2Found);

        userEnterCommandAndText(client1, "QUIT");
        userFound = test_server.doesUserExist("existing_client1");
        assertFalse(userFound);
        user2Found = test_server.doesUserExist("existing_client2");
        assertTrue(user2Found);

        userEnterCommandAndText(client2, "QUIT");
        userFound = test_server.doesUserExist("existing_client1");
        assertFalse(userFound);
        user2Found = test_server.doesUserExist("existing_client2");
        assertFalse(user2Found);
    }

    @Test
    public void broadcastMessage_1User1Message_userReceiveMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        String message = "Hi";
        test_server.broadcastMessage(message);

        String actual_message = userReceiveMessage(client1);

        assertEquals(message, actual_message);

    }

    @Test
    public void broadcastMessage_1UserMultipleMessages_userReceiveMultipleMessages(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        String message1 = "Hi";
        String message2 = "Can we be friends?";

        test_server.broadcastMessage(message1);
        String actual_message1 = userReceiveMessage(client1);
        assertEquals(message1, actual_message1);

        test_server.broadcastMessage(message2);
        String actual_message2 = userReceiveMessage(client1);
        assertEquals(message2, actual_message2);

    }

    @Test
    public void broadcastMessage_multipleUsers1Message_usersReceiveMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message
        Socket client2 = createMockUsers("client2",test_port_no);
        userReceiveMessage(client2); //welcome message

        String message = "Hi";
        test_server.broadcastMessage(message);

        String actual_message_client1 = userReceiveMessage(client1);
        assertEquals(message, actual_message_client1);

        String actual_message_client2 = userReceiveMessage(client2);
        assertEquals(message, actual_message_client2);

    }

    @Test
    public void sendPrivateMessage_message1UserValidUsername_userReceivesMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        String message = "Hi";
        test_server.sendPrivateMessage(message,"client1");
        String actual_message_client1 = userReceiveMessage(client1);
        assertEquals(message, actual_message_client1);
    }

    @Test
    public void sendPrivateMessage_message1UserInvalidUsername_userDoesNotReceivesMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        String message = "Hi";
        test_server.sendPrivateMessage(message,"client2");
        test_server.broadcastMessage("Sorry I don't know your username"); //to check if private message sent
        String actual_message_client1 = userReceiveMessage(client1);
        assertNotEquals(message, actual_message_client1);
    }

    @Test
    public void sendPrivateMessage_message1UserMultipleMessages_userReceivesMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        String message1 = "Hi";
        String message2 = "Can we be friends?";

        test_server.sendPrivateMessage(message1,"client1");
        String actual_message1_client1 = userReceiveMessage(client1);
        assertEquals(message1, actual_message1_client1);

        test_server.sendPrivateMessage(message2,"client1");
        String actual_message2_client1 = userReceiveMessage(client1);
        assertEquals(message2, actual_message2_client1);
    }

    @Test
    public void sendPrivateMessage_messageMultipleUsers_userReceivesIntendedMessage(){
        Socket client1 = createMockUsers("client1",test_port_no);
        userReceiveMessage(client1); //welcome message

        Socket client2 = createMockUsers("client2",test_port_no);
        userReceiveMessage(client2); //welcome message

        String message1 = "Hi";
        test_server.sendPrivateMessage(message1,"client1");
        test_server.broadcastMessage("Secret message for client1"); //to check if private message sent
        String actual_message1_client1 = userReceiveMessage(client1);
        assertEquals(message1, actual_message1_client1);
        String actual_message1_client2 = userReceiveMessage(client2);
        assertNotEquals(message1, actual_message1_client2);

        String message2 = "Can we be friends?";
        test_server.sendPrivateMessage(message2,"client2");
        test_server.broadcastMessage("Secret message for client2"); //to check if private message sent
        String actual_message2_client1 = userReceiveMessage(client1);
        assertNotEquals(message2, actual_message2_client1);
        String actual_message2_client2 = userReceiveMessage(client2);
        assertEquals(message2, actual_message2_client2);
    }

    @Test
    public void removeDeadUsers_1userOnline_userNotRemoved(){
        Socket client1 = createMockUsers("client1",test_port_no);
        int no_of_users_before_remove = test_server.getNumberOfUsers();

        test_server.removeDeadUsers();

        int no_of_users_after_remove = test_server.getNumberOfUsers();
        assertEquals(no_of_users_before_remove,no_of_users_after_remove);
    }

    @Test
    public void removeDeadUsers_1userOnlineAndQuit_userRemoved() {
        Socket client1 = createMockUsers("client1",test_port_no);

        userEnterCommandAndText(client1, "QUIT");
        test_server.removeDeadUsers();
        int no_of_users_after_remove = test_server.getNumberOfUsers();
        assertEquals(0,no_of_users_after_remove);
    }

    @Test
    public void removeDeadUsers_1OfMultipleUsersQuit_1userRemoved() {
        Socket client1 = createMockUsers("client1",test_port_no);
        Socket client2 = createMockUsers("client2",test_port_no);

        userEnterCommandAndText(client1, "QUIT");
        test_server.removeDeadUsers();
        int no_of_users_after_remove = test_server.getNumberOfUsers();
        assertEquals(1,no_of_users_after_remove);
        assertEquals("client2",test_server.getUserList().get(0));
    }

    @Test
    public void getNumberOfUsers_noUsersOnline_return0(){
        int actual_no_users_online = test_server.getNumberOfUsers();
        assertEquals(0,actual_no_users_online);
    }

    @Test
    public void getNumberOfUsers_2UsersOnline_return2(){
        //Create mock clients
        Socket client1 = createMockUsers("client1",test_port_no);
        Socket client2 = createMockUsers("client2",test_port_no);

        int actual_no_users_online = test_server.getNumberOfUsers();
        assertEquals(2,actual_no_users_online);
    }

    @Test
    public void getNumberOfUsers_usersOnlineQuit_returnCorrectNo() {
        //Create mock clients
        Socket client1 = createMockUsers("client1",test_port_no);
        Socket client2 = createMockUsers("client2",test_port_no);

        //client1 quits
        userEnterCommandAndText(client1, "QUIT");
        int actual_no_users_online = test_server.getNumberOfUsers();
        assertEquals(1,actual_no_users_online);

        //client2 quits
        userEnterCommandAndText(client2, "QUIT");
        actual_no_users_online = test_server.getNumberOfUsers();
        assertEquals(0,actual_no_users_online);
    }

    @After
    public void closeConnection(){
        test_server.stopListening();

    }

}