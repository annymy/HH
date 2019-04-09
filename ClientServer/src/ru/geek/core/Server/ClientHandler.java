package ru.geek.core.Server;


import sun.net.www.protocol.http.logging.HttpLogFormatter;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String nick;
    public static final Logger logger = Logger.getLogger(Server.class.getName());

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            Handler h = new FileHandler("mylog.log");
            h.setFormatter(new HttpLogFormatter());
            logger.addHandler(h);
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String msg = in.readUTF();
                            if (msg.startsWith("/auth ")) {
                                String[] data = msg.split("\\s");
                                String newNick  = server.getAuthServise().getNickByLoginAndPassword(data[1], data[2]);
                                if (newNick != null) {
                                    if (!server.isNickBusy(newNick)) {
                                        nick = newNick;
                                        sendMsg("/authok "+ newNick);
                                        server.subscribe(ClientHandler.this);
                                        logger.info("Client connected " + nick);
                                        break;
                                    } else {
                                        sendMsg("Учетная запись уже занята");
                                        logger.info("Учетная запись уже занята");
                                    }
                                } else {
                                    sendMsg("Неверный логин/пароль");
                                    logger.info("Неверный логин/пароль");

                                }
                            }
                        }
                        while (true) {
                            String msg = in.readUTF();

                            System.out.println(nick + ": " + msg);
                            if(msg.startsWith("/")) {
                                if (msg.equals("/end")) {
                                    server.broadcastMsg(nick + " Покинул чат");
                                    logger.info(nick + " Покинул чат");
                                    break;
                                }
                                if(msg.startsWith("/w")) {
                                    String[] data = msg.split("\\s", 3);
                                    server.sendPrivateMsg(ClientHandler.this, data[1], data[2]);
                                    logger.info("private msg from " + nick + " to " + data[1] + ": " + data[2]);
                                }
                                if(msg.startsWith("/change")){
                                    String[] data = msg.split("\\s");
                                    server.authServise.chageNick(ClientHandler.this, data[1]);
                                    nick = data[1];
                                    server.broadcastClientsList();
                                    logger.info(nick + " change nick " + data[1]);
                                }
                            } else {
                                server.broadcastMsg(nick + ": " + msg);
                                logger.info(nick + ": " + msg);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally{
                        nick = null;
                        server.unsubscribe(ClientHandler.this);
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            executorService.shutdown();
        } catch (IOException e){
            e.printStackTrace();
        }

    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
