package ru.geek.core.Server;

import sun.net.www.protocol.http.logging.HttpLogFormatter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Server {
    private Vector<ClientHandler> clients;
    AuthServise authServise;
    //private final Logger logger = Logger.getLogger(Server.class.getName());


    public AuthServise getAuthServise(){
        return authServise;
    }

    public Server() {
        try(ServerSocket serverSocket = new ServerSocket(8190)) {
            clients = new Vector<>();
            authServise = new AuthServise();
            authServise.connect();
            System.out.println("Server started... Waiting for clients");
            ClientHandler.logger.info("Server started... Waiting for clients");


            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected " + socket.getInetAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException | ClassNotFoundException e){
            System.out.println("Не удалось запустить сервис авторизации");

            ClientHandler.logger.info("Не удалось запустить сервис авторизации");
        } finally {
            authServise.disconnect();
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)) return true;
        }
        return false;
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientsList();
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void sendPrivateMsg(ClientHandler from, String nickTo, String msg){
        for (ClientHandler o: clients){
            if(o.getNick().equals(nickTo)){
                o.sendMsg("from " + from.getNick() + ": " + msg);
                from.sendMsg("to client " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Client " + nickTo + " not found");
    }

    public void broadcastClientsList(){
        StringBuilder sb = new StringBuilder("/clientslist ");
        for (ClientHandler o : clients){
            sb.append(o.getNick()+ " ");
        }
        String out = sb.toString();
        for (ClientHandler o : clients){
            o.sendMsg(out);
        }
    }
}
