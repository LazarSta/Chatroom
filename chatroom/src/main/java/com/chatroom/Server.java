package com.chatroom;
// Package declaration for MAVEN project in VSCode

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.*;
// Package used for UI

public class Server extends JFrame {

    private ServerSocket serverSocket;
    private boolean serverRunning = false;
    // serverRunning boolean is used to prevent threads from throwing catch errors

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        System.out.println("Server started");
        serverRunning = true;
        // Setting serverRunning to true here allows remaining code to run

        Runnable serverTask = () -> {

            try {

                while (serverRunning) {

                    try {

                        Socket socket = serverSocket.accept();
                        // Blocking operation that waits until a client connects
                        // It returns a Socket object representing the client connection

                        if (!serverRunning) {
                            break;
                        }
                        // If server is stopped, break out of the loop

                        System.out.println(
                                "New client connected to " + socket.getInetAddress() + " at Port: "
                                        + serverSocket.getLocalPort());
                        System.out.println("Amount of users in chatroom: " + (ClientHandler.clientHandlers.size() + 1));
                        // Prints out that a user has connected and displays the number of current users
                        // in the chatroom (In console)

                        ClientHandler clientHandler = new ClientHandler(socket);
                        // Creates a new ClientHandler object to handle communication with the client

                        Thread thread = new Thread(clientHandler);
                        thread.start();
                        // Starts a new thread to handle communication with the client

                    } catch (SocketException se) {
                        if (!serverSocket.isClosed()) {
                            se.printStackTrace();
                        }
                        // Ignores SocketException when server socket is closed
                    }
                }
                // Above code only runs while serverRunning boolean is set to 'true'

            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        // Defines a task intended to operate within the below-defined thread

        Thread serverThread = new Thread(serverTask);
        serverThread.start();
        // Starts the server task in a new thread

    }
    // Function used start server.

    public void closeServerSocket() {
        serverRunning = false;
        // Sets serverRunning boolean to false to exit out of threads

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Method that shuts down the server socket and closes the server process

    public static void main(String[] args) throws IOException {

        SwingUtilities.invokeLater(() -> {

            JFrame serverConfigFrame = new JFrame("Server Configuration");
            serverConfigFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            serverConfigFrame.setSize(300, 100);
            serverConfigFrame.setLayout(new FlowLayout());
            // Creating a JFrame "serverConfigFrame" with FlowLayout

            JLabel portLabel = new JLabel("Port:");
            JTextField portField = new JTextField(10);
            // TextField used to input custom port number

            JCheckBox defaultPortCheckbox = new JCheckBox("Use Default Port (2000)");
            defaultPortCheckbox.addActionListener(e -> {
                portField.setEnabled(!defaultPortCheckbox.isSelected());
            });
            // CheckBox used to default the port number to the value "2000"
            // Also disabled the portField text input if the box is selected

            JButton startButton = new JButton("Start Server");
            // Button to start server

            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (portField.isEnabled() && portField.getText().isEmpty()) {

                        JOptionPane.showMessageDialog(serverConfigFrame, "Please fill in the port.");
                        // Checks if port input is enabled and empty. If so, alerts user to fill out a
                        // port number

                    } else {
                        int port;
                        if (defaultPortCheckbox.isSelected()) {
                            port = 2000;
                        } else {
                            try {
                                port = Integer.parseInt(portField.getText());
                                if (port <= 0 || port > 65535) {
                                    throw new NumberFormatException("Port number must be between 1 and 65535.");
                                }
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(serverConfigFrame,
                                        "Invalid port number. Please enter a number between 1 and 65535.");
                                return;
                            }
                        }
                        // Checks for default checkbox selection. If true port defaults to 2000,
                        // otherwise it parses the integer from the portField textfield.
                        // Also checks to see whether a number is inputted and if said number is a valid
                        // port number (> 0 or <= 65535)

                        try {
                            ServerSocket serverSocket = new ServerSocket(port);
                            // Creates ServerSocket object using above specified port instructions

                            Server server = new Server(serverSocket);
                            // Creates new Server object using above created ServerSocket object

                            server.startServer();
                            // Starts server

                            serverConfigFrame.dispose();
                            // Closes the server Configuration frame

                            JFrame serverStartedFrame = new JFrame("Server Started");
                            serverStartedFrame.setSize(300, 100);
                            serverStartedFrame.setLayout(new FlowLayout());
                            // Creates a new frame indicating server has started

                            JLabel serverStartedLabel = new JLabel("Server running on port " + port);
                            serverStartedFrame.add(serverStartedLabel);
                            // Adds a Label to the new serverStartedFrame indicating that the server is
                            // running on a specific port

                            JButton stopButton = new JButton("Stop Server");
                            stopButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    server.closeServerSocket();
                                    JOptionPane.showMessageDialog(serverStartedFrame, "Server stopped.");
                                }
                            });
                            serverStartedFrame.add(stopButton);
                            // Creates and adds a "Stop Server" Button object to the serverStartedFrame
                            // that, upon pressing, calls the function closeServerSocket()

                            serverStartedFrame.setLocationRelativeTo(null);
                            // Center serverStartedFrame on the screen

                            serverStartedFrame.setVisible(true);
                            // Sets serverStartedFrame to visible

                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(serverConfigFrame,
                                    "Failed to start server. Please check the port.");
                        }
                    }
                }
            });
            // Eventlistener for startButton

            serverConfigFrame.add(portLabel);
            serverConfigFrame.add(portField);
            serverConfigFrame.add(defaultPortCheckbox);
            serverConfigFrame.add(startButton);
            // Add port input, default checkbox and start button objects to
            // serverConfigFrame

            serverConfigFrame.setLocationRelativeTo(null);
            // Center serverConfigFrame on the screen

            serverConfigFrame.setVisible(true);
            // Sets serverConfigFrame to visible
        });
    }
}