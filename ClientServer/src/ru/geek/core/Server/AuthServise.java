package ru.geek.core.Server;

import ru.geek.core.Client.Controller;

import java.sql.*;

public class AuthServise{
    private Connection connection;
    private Statement stmt;

    public void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:man.db");
        stmt = connection.createStatement();
    }

    public String getNickByLoginAndPassword(String login, String pass){
        try {
            ResultSet rs = stmt.executeQuery("SELECT nick FROM user WHERE login = '" + login + "' AND password = '" + pass + "';");
            if(rs.next()){
                return rs.getString("nick");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void chageNick(ClientHandler client, String newNick){
        try {
            stmt.executeUpdate("UPDATE user SET nick = '" + newNick+ "' WHERE nick = '"+ client.getNick() + "';");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
