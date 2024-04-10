package edu.spbstu.labs.lab1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPServer {
    private static final int PORT = 80;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Ready to serve...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection accepted");

                Runnable worker = new RequestHandler(clientSocket);
                executor.execute(worker);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record RequestHandler(Socket clientSocket) implements Runnable {

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request = in.readLine();
                String filePath = request.split(" ")[1];
                File file = new File(filePath);

                if (file.isFile()) {
                    out.println("HTTP/1.1 200 OK\r\n\r\n");
                    out.println("Content-Type: text/plain;charset=UTF-8\n");
                    out.println(Files.readString(Path.of(filePath)));
                } else {
                    out.println("HTTP/1.1 404 Not Found\r\n\r\nFile Not Found");
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}