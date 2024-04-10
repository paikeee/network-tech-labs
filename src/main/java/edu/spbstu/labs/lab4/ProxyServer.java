package edu.spbstu.labs.lab4;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProxyServer implements Closeable {

    private Socket socket;
    private final ServerSocket serverSocket;
    private final String server;

    private BufferedReader remoteReader;
    private PrintWriter remoteWriter;
    private volatile boolean isReady = true;
    private final AtomicInteger clients = new AtomicInteger(0);

    public ProxyServer(String server, int remotePort, int port) throws IOException {
        this.server = server;
        serverSocket = new ServerSocket(port);
        reopenRemote(remotePort);
    }

    public static void main(String[] args) throws IOException {
        boolean isUsingBrowser = false;
        try (ProxyServer proxyServer = new ProxyServer("google.com", 80, 60000);
             Socket socket = new Socket("localhost", 60000);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            new Thread(() -> {
                try {
                    proxyServer.start(!isUsingBrowser);
                } catch (IOException e) {
                    System.out.println("Server error");
                    throw new RuntimeException(e);
                }
            }).start();
            if (!isUsingBrowser) {
                new Thread(() -> {
                    while (!proxyServer.isReady()) ;
                    System.out.println("Sending GET request");
                    writer.println("GET /index.html");

                    System.out.println("Response from server: ");
                    try {
                        while (proxyServer.isReady()) {
                            System.out.println(reader.readLine());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
            while (proxyServer.isReady()) ;
        }
    }

    private void reopenRemote(int remotePort) throws IOException {
        if (socket != null) socket.close();
        socket = new Socket(server, remotePort);
        remoteReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        remoteWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    public void start(boolean stopAfter) throws IOException {
        if (socket.isClosed()) return;
        isReady = true;

        while (isReady) {
            System.out.println("Waiting for a connection at port: " + serverSocket.getLocalPort());
            Socket client;

            try {
                client = serverSocket.accept();
            } catch (Exception e) {
                break;
            }

            new Thread(() -> {
                clients.incrementAndGet();
                System.out.println("Established a connection with a client: " + client.getInetAddress() + ":" + client.getPort());

                try {
                    handleClient(client);
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                int clientsAfter = clients.decrementAndGet();
                if (clientsAfter == 0 && stopAfter) {
                    isReady = false;
                }
            }).start();
        }
        isReady = false;
    }

    private void handleClient(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true);

        String clientRequest = in.readLine();
        if (clientRequest == null) return;
        clientRequest = clientRequest.replace("HTTP/1.1", "");

        System.out.println("Received request from a client: " + clientRequest);
        String[] request = clientRequest.split("\\s+");

        if (!request[0].equalsIgnoreCase("GET")) {
            out.println("HTTP/1.1 501\n\n");
            return;
        }

        String systemFile = request[1].replace("http://", "").replaceAll("/", "_");
        System.out.println("Received request from a client: " + clientRequest);
        File file = new File(Path.of("").toAbsolutePath().toString(), systemFile);

        if (file.isFile()) {
            System.out.println("File is found in cache: " + file);
            String cachedContent = "HTTP/1.1 200 OK\n\n" + Files.readString(file.toPath());
            out.println(cachedContent);
            System.out.println("Cached content: ");
            System.out.println(cachedContent);
        } else {
            System.out.println("File does NOT found in cache: " + file);
            System.out.println("Requesting file from server: " + server + ":" + socket.getPort());

            remoteWriter.println(clientRequest);
            String remoteResponse = remoteReader.lines().collect(Collectors.joining("\n"));
            System.out.println("Remote response: ");
            System.out.println(remoteResponse);

            String content = remoteResponse.substring(remoteResponse.indexOf("\n\n"));
            Files.write(file.toPath(), content.getBytes());
            System.out.println("File is saved in cache: " + file);
            out.println("HTTP/1.1 200 OK\n\n" + content);
        }
    }

    @Override
    public void close() throws IOException {
        System.out.println("Server is closed");
        isReady = false;
        try {
            if (!socket.isClosed()) socket.close();
        } finally {
            if (!serverSocket.isClosed()) serverSocket.close();
        }
    }

    public boolean isReady() {
        return isReady;
    }
}
