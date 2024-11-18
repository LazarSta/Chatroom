package com.chatroom;
// Package declaration for MAVEN project in VSCode

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;
// Package used for UI

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
// Packages used to store/read data in/from json files

public class Client extends JFrame {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private JTextArea chatArea;
    private JTextField messageField;
    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    private ChatHistory chatHistory;
    private JScrollPane scrollPane;

    public Client(Socket socket, String username) {
        super("Chat Client - " + username);
        this.socket = socket;
        this.username = username;
        this.chatHistory = new ChatHistory();

        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeAll(socket, bufferedReader, bufferedWriter);
        }

        setupUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                messageField.requestFocusInWindow();
            }
        });
    }
    // Represents a client in the chat system
    // Creates a new client instance with provided socket and username, then creates
    // a ChatHistory object alongside setting up the UI

    private void setupUI() {
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        loadChatHistory();
        displayChatHistory();

        setLocationRelativeTo(null);
        // Center the UI on the window
    }
    // Sets up the graphical user interface and configures size and layout for UI
    // Calls for methods to load the chat history and display said chat history to
    // fill out the chatarea.
    // Finally centers the window on the monitor/display

    public void sendMessage() {
        try {
            String messageToSend = messageField.getText();
            if (!messageToSend.isEmpty()) {
                bufferedWriter.write(username + ": " + messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                messageField.setText("");
                Date date = new Date();
                chatArea.append("[" + formatter.format(date) + "] " + username + ": " + messageToSend + "\n");
                scrollChatToBottom();
                messageField.requestFocusInWindow();
                // Requests focus to the message input field for a seamless chatting experience
                chatHistory.addMessage(new ChatMessage(username, messageToSend));
                // Add the sent message to chat history
                chatHistory.saveToFile("chat_history.json");
                // Save chat history to file after sending message
            }
        } catch (IOException e) {
            closeAll(socket, bufferedReader, bufferedWriter);
        }
    }
    // Sends a message to the server and updates the chat interface

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageFromGroup;

                while (socket.isConnected()) {
                    try {
                        messageFromGroup = bufferedReader.readLine();
                        if (messageFromGroup != null) {
                            Date date = new Date();
                            chatArea.append("[" + formatter.format(date) + "] " + messageFromGroup + "\n");
                            scrollChatToBottom();
                            chatHistory.addMessage(new ChatMessage("Group", messageFromGroup));
                            // Add received message to chat history
                            chatHistory.saveToFile("chat_history.json");
                            // Save chat history to file after receiving message
                        }
                    } catch (IOException e) {
                        closeAll(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }
    // Listens for messages from the server and updates the chat interface

    private void loadChatHistory() {
        chatHistory.loadFromFile("chat_history.json");
    }
    // Loads chat history from a JSON file

    private void displayChatHistory() {
        for (ChatMessage message : chatHistory.getMessages()) {
            chatArea.append("[" + formatter.format(message.getTimestamp()) + "] " +
                    message.getSender() + ": "
                    + message.getMessage() + "\n");
        }
    }
    // Retrieves messages from the chatHistory object and displays it in the chat
    // interface

    private void definiteScrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
    // Always scrolls the chat window to the bottom to display new messages on
    // startup

    private void scrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            int maximumValue = verticalBar.getMaximum();
            int currentValue = verticalBar.getValue();
            int extent = verticalBar.getModel().getExtent();

            if (currentValue + extent >= maximumValue - 20) {
                verticalBar.setValue(maximumValue);
            }
            // Check if the user is already close to the bottom
        });
    }
    // Scrolls the chat window to the bottom to display new messages if user is
    // already scrolled near to the bottom

    public void closeAll(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
            JOptionPane.showMessageDialog(null, "Server has shut down", "Server Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Closes all streams and sockets associated with the client

    public static void main(String[] args) {
        JFrame startupFrame = new JFrame();
        startupFrame.setTitle("Instance options");
        startupFrame.setSize(400, 300);
        startupFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField hostField = new JTextField(20);
        JTextField portField = new JTextField(20);
        JCheckBox defaultCheckBox = new JCheckBox("Use default values (127.0.0.1:2000)");
        JTextField usernameField = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(defaultCheckBox);
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);

        JFrame connectingFrame = new JFrame();
        connectingFrame.setTitle("Connecting");
        JLabel connectingLabel = new JLabel("Establishing connection... Please wait.", SwingConstants.CENTER);
        connectingFrame.add(connectingLabel);
        connectingFrame.setSize(300, 100);
        connectingFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        connectingFrame.setLocationRelativeTo(null);
        // Center the connectingFrame on the window

        connectingFrame.setVisible(false);
        // Set connectingFrame to visible

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Instance options", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION)
                break;

            String host;
            int port;
            String username;

            if (defaultCheckBox.isSelected()) {
                host = "127.0.0.1";
                port = 2000;
                // Default values if default checkbox is selected
            } else {
                host = hostField.getText();
                port = 0;

                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Host cannot be empty.");
                    continue;
                }
                // Checks to see if host input is left empty

                try {
                    port = Integer.parseInt(portField.getText());
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Invalid port number.");
                    continue;
                }
                // Checks to see if port input is valid

                if (port <= 0 || port > 65535) {
                    JOptionPane.showMessageDialog(null, "Port number must be between 1 and 65535.");
                    continue;
                }
                // Checks to see if port input is less than or equal to 0 or greater than 65535
            }

            username = usernameField.getText();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username cannot be empty.");
                continue;
            }
            // Checks to see if username input is left empty

            connectingFrame.setVisible(true);

            try {
                Socket socket = new Socket(host, port);
                System.out.println(
                        "Successfully connected to host: " + socket.getInetAddress() + " at Port: " + socket.getPort());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                writer.write(username + "\n");
                // Send the username to the server immediately after connection
                writer.flush();
                // Send username with a newline

                connectingFrame.dispose();
                // Hide connectingFrame as is not relevant after successful connection

                Client client = new Client(socket, username);
                client.setVisible(true);
                client.listenForMessage();
                client.definiteScrollChatToBottom();
                break;
                // Break out of the loop if connection is successful
            } catch (IOException e) {
                connectingFrame.dispose();
                int option = JOptionPane.showConfirmDialog(null,
                        "<html><div style='text-align: center;'>Could not find server at host: " + "'" + host + "'"
                                + " or port: " + "'" + port
                                + "'.<br>Would you like to try another host/port?</div></html>",
                        "Connection Error", JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.NO_OPTION || option == JOptionPane.CLOSED_OPTION)
                    break;
            }
        }
    }
    // Main method to start the chat client
}

class ChatMessage {
    private String sender;
    private String message;
    private Date timestamp;

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
        this.timestamp = new Date();
    }
    // Represents a message in the chat system

    public String getSender() {
        return sender;
    }
    // Get-method for message sender

    public String getMessage() {
        return message;
    }
    // Get-method for message

    public Date getTimestamp() {
        return timestamp;
    }
    // Get-method for message timestamp
}

class ChatHistory {
    private List<ChatMessage> messages;
    private static final String DEFAULT_FILENAME = "chat_history.json";

    public ChatHistory() {
        messages = new ArrayList<>();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
    }
    // Adds a message to the chat history

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void saveToFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = DEFAULT_FILENAME;
        }
        try (FileWriter writer = new FileWriter(new File(filename))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(messages, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Saves the chat history to a file in JSON format

    public void loadFromFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = DEFAULT_FILENAME;
        }
        File file = new File(filename);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                List<ChatMessage> loadedMessages = gson.fromJson(reader, new TypeToken<List<ChatMessage>>() {
                }.getType());
                if (loadedMessages != null) {
                    messages = loadedMessages;
                } else {
                    messages = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
                messages = new ArrayList<>();
            }
        } else {
            messages = new ArrayList<>();
        }
    }
    // Loads the chat history from a JSON file

}
// Manages the history of chat messages