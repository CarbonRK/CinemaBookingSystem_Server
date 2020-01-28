package Server;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.util.LinkedList;

class Server extends Thread{

    private LinkedList<ServerThread> serverThreads;
    private DBOperations dbOperations;
    private DBFilms dbFilms;

    public Server() {
        System.setProperty("javax.net.ssl.keyStore", System.getProperty("user.dir") + "/SSL/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "zdarzenia");

        this.serverThreads = new LinkedList<>();
        this.dbOperations = new DBOperations();
        this.dbFilms = new DBFilms();

        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnectAll));
        waitForClients();
    }

    private void waitForClients() {
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();

        try  {
            ServerSocket serverSocket = sslServerSocketFactory.createServerSocket(54321);
            System.out.println("SSLServer is listening on port 54321");

            while (true) {
                Socket socket = serverSocket.accept();
                ServerThread client_thread = new ServerThread(socket, serverThreads, dbOperations, dbFilms);
                client_thread.start();
                serverThreads.add(client_thread);
            }
        } catch (IOException ex) {
            System.out.println("Server socket error: " + ex.getMessage());
        }
    }

    private void disconnectAll() {
        while (serverThreads.size() > 0){
            ServerThread tmp = serverThreads.remove(0);
            tmp.disconnect();
        }
        this.dbOperations.disconnect();
        this.dbFilms.disconnect();
    }

    public static void main(String[] args) {new Server().start(); }
}

