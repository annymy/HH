package ru.geek.core.Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextField msgField;
    @FXML
    TextArea textArea;
    @FXML
    HBox authPanel;
    @FXML
    HBox msgPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    ListView<String> clientsListView;


    private boolean authorized;

    public void setAutorized(boolean authorized) {
        this.authorized = authorized;
        if (authorized) {
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            clientsListView.setVisible(true);
            clientsListView.setManaged(true);
            readMsg();
        } else {
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            clientsListView.setVisible(false);
            clientsListView.setManaged(false);
        }
    }

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private ObservableList<String> clientsList;
    String myNick;

    final String SERVER_IP = "localhost";
    final int SERVER_PORT = 8190;

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            String msg = myNick + ": " + msgField.getText() + "\n";
            String filePath = "data.txt";

            try {
                FileWriter writer = new FileWriter(filePath, true);
                BufferedWriter bufferWriter = new BufferedWriter(writer);
                bufferWriter.write(msg);
                bufferWriter.close();
            } catch (IOException e) {
                System.out.println(e);
            }
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMsg() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data.txt"))) {
            String str;
            while ((str = br.readLine()) != null) {
                lines.add(str);
            }
        } catch (IOException e) {
            System.out.println("lalala");
        }

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                for (int i = lines.size() - 5; i < lines.size(); i++)
                    textArea.appendText(lines.get(i) + "\n");
            }
        });
    }

    public void sendAuthMsg() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {
            showAlert("Не удaлось подключиться к серверу. Проверьте сетевое соединение");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAutorized(false);
    }

    public void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientsList = FXCollections.observableArrayList();
            clientsListView.setItems(clientsList);
            clientsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
                @Override
                public ListCell<String> call(ListView<String> param) {
                    return new ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!empty) {
                                setText(item);
                                if (item.equals(myNick)) {
                                    setStyle("-fx-font-weight: bold");
                                }
                            } else {
                                setGraphic(null);
                                setText(null);
                            }
                        }
                    };
                }
            });
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        while (true) {
                            String s = null;
                            s = in.readUTF();
                            if (s.startsWith("/authok ")) {
                                myNick = s.split("\\s")[1];
                                setAutorized(true);
                                break;
                            }
                            textArea.appendText(s + "\n");
                        }

                        //readMsg();
                        while (true) {
                            String s = null;
                            s = in.readUTF();
                            if (s.startsWith("/clientslist")) {
                                String[] data = s.split("\\s");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        clientsList.clear();
                                        for (int i = 1; i < data.length; i++) {
                                            clientsList.addAll(data[i]);
                                        }
                                    }
                                });

                            } else {
                                textArea.appendText(s + "\n");
                            }
                        }
                    } catch (IOException e) {
                        showAlert("Сервер перестал отвечать");
                    } finally {
                        setAutorized(false);
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Controller.this.showAlert("Не удалось подключиться  серверу. Проверьте сетевое соединение");
                        }
                    }

                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            showAlert("Не удалось подключиться  серверу. Проверьте сетевое соединение!");
        }
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Возникли проблемы");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    public void clientsListCllicked(javafx.scene.input.MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            msgField.setText("/w " + clientsListView.getSelectionModel().getSelectedItem() + " ");
            msgField.requestFocus();
            msgField.selectEnd();
        }
    }


}
