package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Random;


public class ServerThread extends Thread {

    private Socket socket;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean work;
    private long id;
    private DBOperations dbOperations;
    private DBFilms dbFilms;
    private LinkedList<ServerThread> serverThreads;
    private String client;
    private EmailSender emailSender;

    public ServerThread(Socket socket, LinkedList<ServerThread> serverThreads, DBOperations dbOperations, DBFilms dbFilms) {
        this.socket = socket;
        this.dbOperations = dbOperations;
        this.dbFilms = dbFilms;
        this.emailSender = new EmailSender();
        this.serverThreads = serverThreads;
        
        try {
            this.reader = new DataInputStream(socket.getInputStream());
            this.writer = new DataOutputStream(socket.getOutputStream());
            this.work = true;
        } catch (IOException ex) {
            System.out.println("reader/writer error: " + ex.getMessage());
            this.work = false;
        }
    }

    @Override
    public void run() {
        this.id = Thread.currentThread().getId();
        System.out.println("New client connected " + id);

        while (work) {
            try {
                decideForInput(reader.readUTF());
            } catch (IOException ex) {
                System.out.println("Event listener error: " + ex.getMessage());
                disconnect();
            }
        }
    }

    public void decideForInput(String data) {
        String[] array = data.split(" ");

        switch (array[0]) {
            case "exit_server":
                disconnect();
                break;
            case "login":
                logged_in(array[1], array[2]);
                break;
            case "unlogin":
                System.out.println("Logged out by: "+this.client);
                this.client = null;
                break;
            case "register":
                register(array[1], array[2], array[3], array[4], array[5],array[6]);
                break;
            case "delete":
                deleteAccount();
                break;
            case "forgotten":
                forgottenPassword(array[1]);
                break;
            case "change":
                changePassword(array[1],array[2]);
                break;
            case "films":
                getFilms();
                break;
            case "dates":
                getDates(array[1]);
                break;
            case "places":
                getPlaces(array[1], array[2], array[3]);
                break;
            case "order":
                makeOrder(array[1],array[2],array[3],array[4],array[5]);
                break;
            case "tickets":
                getTickets();
                break;
            case "image":
                getFilmsImage(Integer.parseInt(array[1]));
                break;
            case "info":
                getInfo(array[1]);
                break;
            case "update":
                updateInfo(array[1],array[2],array[3],array[4]);
                break;
            default:
                System.out.println("Unknown message: "+data);
                sendUnknown();
                break;
        }
    }

    private void sendDataEncrypted(String data) {
        try {
            writer.writeUTF(data);
        } catch (IOException ex) {
            System.out.println("Send data error: " + ex.getMessage());
            disconnect();
        }
    }

    private String getAlphaNumericString() {
        int index;
        String alphaNumericString ;
        StringBuilder sb;

        alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
        sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            index = ((i * new Random().nextInt(150)) % alphaNumericString.length());
            sb.append(alphaNumericString.charAt(index));
        }

        return sb.toString();
    }

    private void updateInfo(String name, String surname, String birthday, String sex){
        this.dbOperations.updateInfo(client,name,surname,birthday,sex);
        sendDataEncrypted("update");
        System.out.println("Updating info by: "+this.client);
    }

    private void getFilmsImage(int id){
        String image = dbFilms.getFilmsImage(id);
        sendDataEncrypted(image);
        System.out.println("Films image sent to: "+ this.client);
    }

    private void getInfo(String email){
        String info = dbOperations.getInfo(email);
        sendDataEncrypted(info);
        System.out.println("Info listing by: "+ this.client);
    }

    private void getTickets(){
        String tickets;

        tickets = dbFilms.getTickets(client);
        sendDataEncrypted(tickets);
        System.out.println("Bought tickets request by: " + client);
    }

    private void makeOrder(String place, String owner, String day, String time, String filmName){
        String lastOrder;

        dbFilms.makeOrder(client,place,owner,day,time,filmName);
        lastOrder = dbFilms.getLastTicketIndex().split(" ")[1];
        emailSender.aboutOrder(client, lastOrder,owner,owner);
        System.out.println("Order made by: "+ client);
    }

    private void getPlaces(String filmName, String date, String time){
        String places = dbFilms.getPlaces(date, time, filmName);
        sendDataEncrypted(places);
        System.out.println("Places listing by: "+this.client);
    }

    private void getDates(String filmName){
        String dates = dbFilms.getFilmsDates(filmName);
        sendDataEncrypted(dates);
        System.out.println("Dates listing by: "+this.client);
    }

    private void getFilms(){
        String films = dbFilms.getFilms();
        sendDataEncrypted(films);
        System.out.println("Film listing by: "+this.client);
    }

    private void forgottenPassword(String email){
        String newPassword;
        newPassword = getAlphaNumericString();
        dbOperations.update_password(email, newPassword);
        emailSender.aboutForgottenPassword(email,newPassword);
        System.out.println("Trying to reset pass by: " + email);
    }

    private void changePassword( String oldPassword, String newPassword){
        if (dbOperations.logIn(this.client, oldPassword)){
            dbOperations.update_password(this.client,newPassword);
            sendDataEncrypted("change");
            System.out.println("Changed password by: " + this.client);
        } else {
            sendUnknown();
        }
    }

    private void deleteAccount(){
        if (dbOperations.deleteClient(this.client)){
            System.out.println("Deleted: " + this.client);
            sendDataEncrypted("deleted");
        } else {
            sendUnknown();
        }
    }

    private void register(String name,String surname, String sex, String date, String password, String email) {
        if (this.dbOperations.addClient(name, surname, sex, date, password, email)) {
            System.out.println("Registered: " + email);
            sendDataEncrypted("registered");
            emailSender.aboutRegistering(email);
        } else {
            sendUnknown();
        }
    }

    private void logged_in(String email, String password) {
        if (this.dbOperations.logIn(email, password)) {
            this.client = email;
            System.out.println("Logged: " + email);
            sendDataEncrypted("logged");
        }else {
           sendUnknown();
        }
    }

    private void sendUnknown() {
        sendDataEncrypted("unknown");
    }

    public void disconnect() {
        if (work) {
            try {
                sendDataEncrypted("exit_client");
                work = false;
                socket.close();
                serverThreads.remove(this);
                System.out.println("Client disconnected: " + id);
            } catch (IOException ex) {
                System.out.println("Disconnect error: " + ex.getMessage());
                System.exit(-1);
            }
        }
    }
}
