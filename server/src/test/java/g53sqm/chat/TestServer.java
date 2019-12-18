package g53sqm.chat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.*;

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
        }catch(IOException |InterruptedException ie) {
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

        // To terminate automatically
        test_server_thread.setDaemon(true);
        test_server_thread.start();


        // Prevent race condition
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //register new user
    @Test
    public void shouldUser_register(){
        Socket user = null;
        String username = "User_1";
        try{
            user = new Socket("localhost",test_port_no);
            userReceiveMessage(user);
            userEnterCommandAndText(user, "IDEN " + username);
        }catch (IOException e) {

        }
        String reply = userReceiveMessage(user);
        String expected = "OK Welcome to the chat server "+ username;
        assertEquals(expected,reply);
    }

    //same username entered twice
    @Test
    public void shouldUser_register_sameUsername(){
        Socket user1;
        String username = "User";
        Socket user2 = null;
        try{
            user1 = new Socket("localhost",test_port_no);
            userEnterCommandAndText(user1, "IDEN " + username);

            user2 = new Socket("localhost",test_port_no);
            userReceiveMessage(user2);
            userEnterCommandAndText(user2, "IDEN " + username);
        }catch (IOException e) {

        }
        String reply = userReceiveMessage(user2);
        String expected = "BAD username is already taken";
        assertEquals(expected,reply);
    }

    //register empty space as username
    @Test
    public void shouldUsername_equals_blank(){
        Socket user =null;
        String username = " ";
        try{
            user = new Socket("localhost",test_port_no);
            userReceiveMessage(user);
            userEnterCommandAndText(user, "IDEN " + username);
        }catch (IOException e) {
        }
        String reply = userReceiveMessage(user);
        System.out.println(reply);
        String expected = "BAD must enter a username";
        assertEquals(expected,reply);
    }

    //list of online user when no client is initiated
    @Test
    public void getUserList_noUsersOnline_returnEmptyList(){
        ArrayList<String> actual_users_online = test_server.getUserList();
        String[] expected_users_online = new String[]{};
        assertArrayEquals(actual_users_online.toArray(),expected_users_online);
    }

    //return correct list of username
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

    //get correct list of username after one client quits
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

    //return true if user is online
    @Test
    public void doesUserExist_userOnline_returnTrue(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);

        boolean userFound = test_server.doesUserExist("existing_client1");
        assertTrue(userFound);
    }

    //check if online username exists
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
    public void shouldSend_broadcast(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        Socket client2 = createMockUsers("existing_client2",test_port_no);

        //absorb initial line
        userReceiveMessage(client2);

        userEnterCommandAndText(client1, "HAIL hi");
        String reply1 = userReceiveMessage(client2);

        assertEquals("Broadcast from existing_client1: "+"hi", reply1);
    }

    @Test
    public void shouldSend_BADcommand(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        userReceiveMessage(client1);
        userEnterCommandAndText(client1, "JUNK TEZXT");
        String reply1 = userReceiveMessage(client1);

        assertEquals("BAD command not recognised", reply1);
    }

    @Test
    public void shouldSend_privateMessage_receive(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        Socket client2 = createMockUsers("existing_client2",test_port_no);

        userReceiveMessage(client2);//absorb message
        userEnterCommandAndText(client1, "MESG existing_client2 hi");

        String reply1 = userReceiveMessage(client2);
        String expected = "PM from existing_client1:hi";

        assertEquals(expected, reply1);
    }

    @Test
    public void shouldSend_privateMessage_responseFromServer(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        Socket client2 = createMockUsers("existing_client2",test_port_no);

        userReceiveMessage(client1);//absorb line

        userEnterCommandAndText(client1, "MESG existing_client2 hi");

        String reply1 = userReceiveMessage(client1);
        String expected = "OK your message has been sent";

        assertEquals(expected, reply1);
    }

    //receive correct list of active user when list is inputted
    @Test
    public void shouldSend_list(){
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        userReceiveMessage(client1);//absorb line
        Socket client2 = createMockUsers("existing_client2",test_port_no);
        Socket client3 = createMockUsers("existing_client3",test_port_no);

        userEnterCommandAndText(client1, "LIST");

        String actualReply = userReceiveMessage(client1);
        ArrayList<String> expected_users_online = test_server.getUserList();
        String namelist=(expected_users_online.toString().substring(1, expected_users_online.toString().length() - 1));
        String s = "OK "+namelist+", " ;

        assertEquals(s,actualReply);
    }

    //display stat when 1 user active
    @Test
    public void shouldSend_STAT_singleUser(){
        int msgCount=0;
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        userReceiveMessage(client1);

        userEnterCommandAndText(client1, "STAT");
        String actualReply = userReceiveMessage(client1);


        ArrayList<String> expected_users_online = test_server.getUserList();
        String expected = "OK There are currently "+expected_users_online.size()+" user(s) on the server You are logged in and have sent "+msgCount+ " message(s)";


        assertEquals(expected, actualReply);
    }

    //display stat when multiple user active
    @Test
    public void shouldSend_STAT_with_multiUser(){
        int msgCount=0;
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        userReceiveMessage(client1);//absorb line
        Socket client2 = createMockUsers("existing_client2",test_port_no);
        userEnterCommandAndText(client1, "STAT");

        String actualReply = userReceiveMessage(client1);

        ArrayList<String> actual_users_online = test_server.getUserList();
        String expected = "OK There are currently "+actual_users_online.size()+" user(s) on the server You are logged in and have sent "+msgCount+ " message(s)";

        assertEquals(expected, actualReply);
    }

    //display correct number of messages sent
    @Test
    public void should_send_STAT_msg_sent(){
        int msgCount=0;
        Socket client1 = createMockUsers("existing_client1",test_port_no);
        userReceiveMessage(client1);


        userEnterCommandAndText(client1, "HAIL hi");
        msgCount++;
        userReceiveMessage(client1);


        userEnterCommandAndText(client1, "HAIL hi");
        msgCount++;
        userReceiveMessage(client1);


        userEnterCommandAndText(client1, "STAT");
        String actualReply = userReceiveMessage(client1);


        ArrayList<String> actual_users_online = test_server.getUserList();
        String expected = "OK There are currently "+actual_users_online.size()+" user(s) on the server You are logged in and have sent "+msgCount+ " message(s)";


        assertEquals(expected, actualReply);
    }

    //send a broadcast message to all client
    @Test
    public void broadcastMessage_MultipleUsers_UsersReceiveMessage() {
        Socket user1 = createMockUsers("user1", test_port_no);
        userReceiveMessage(user1);//absorb line
        Socket user2 = createMockUsers("user2", test_port_no);
        userReceiveMessage(user2);//absorb line

        String msg = "1 message!";
        test_server.broadcastMessage(msg);
        String actualUser1 = userReceiveMessage(user1);
        String actualUser2 = userReceiveMessage(user2);

        assertEquals(msg, actualUser1);
        assertEquals(msg, actualUser2);
    }

    //send single broadcast message from a client
    @Test
    public void broadcastMessage_SingleUser_UserReceiveMessage() {
        Socket user1 = createMockUsers("user1", test_port_no);
        userReceiveMessage(user1);//absorb line

        String msg = "1 message!";
        test_server.broadcastMessage(msg);
        String actualUser1 = userReceiveMessage(user1);

        assertEquals(msg, actualUser1);
    }

    //send multiple broadcast message from a client
    @Test
    public void broadcastMessage_SingleUserMultipleMessage_UserReceiveMessage() {
        Socket user1 = createMockUsers("user1", test_port_no);
        userReceiveMessage(user1);//absorb line

        String msg1 = "1 message!";
        test_server.broadcastMessage(msg1);
        String actualUser1 = userReceiveMessage(user1);

        String msg2 = "2 message!";
        test_server.broadcastMessage(msg2);
        String actualUser2 = userReceiveMessage(user1);

        assertEquals(msg1, actualUser1);
        assertEquals(msg2, actualUser2);
    }
}