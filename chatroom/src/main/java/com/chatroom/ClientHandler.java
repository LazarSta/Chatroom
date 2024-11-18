package com.chatroom;
// Package declaration for MAVEN project in VSCode

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    // Keeps track of all clients in session. When a client sends a message, this
    // list is used as a reference to send other clients said message

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    public String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // Stream to send messages

            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Stream to receive messages

            this.clientUsername = bufferedReader.readLine();
            // Reads the first line from what the user sends and puts it as their username

            synchronized (clientHandlers) {
                clientHandlers.add(this);
            }
            // adds ClientHandler object to arraylist 'clientHandlers'

            broadcastMessage("[SERVER] " + clientUsername + " has entered the chat.");
            // Announces to all connected clients that a new user + their username; has
            // entered the chat

        } catch (IOException e) {
            closeAll(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                // Blocking operation waiting for messages from other clients

                broadcastMessage(messageFromClient);
                // Uses the broadcastMessage() method to send message to other clients in the
                // chatroom

            } catch (IOException e) {
                closeAll(socket, bufferedReader, bufferedWriter);
                break;
                // Breaks out of the while-loop after client disconnects
            }
        }
    }
    // Separate thread running method to scan for new messages. Is done on separate
    // thread as to not lock client into waiting for messages

    public void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(message + "\n");
                    clientHandler.bufferedWriter.flush();
                    // Flush to ensure that any buffered data is immediately written out

                }
                // If statement that ensures message is not sent back to the sender as an
                // incoming message

            } catch (IOException e) {
                closeAll(socket, bufferedReader, bufferedWriter);
            }
        }
    }
    // Method used to broadcast a message to all connected clients.

    public void removeClient() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat.");
    }
    // Method to remove client socket from clientHandlers arrayList and announces to
    // remaining users that said user has left

    public void closeAll(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClient();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Method to close the socket, bufferedReader and bufferedWriter elements after
    // removing the client from the arrayList
}
