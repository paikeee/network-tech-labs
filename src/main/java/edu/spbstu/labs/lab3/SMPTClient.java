package edu.spbstu.labs.lab3;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class SMPTClient {

    private static Socket socket;
    private static BufferedReader reader;
    private static PrintWriter writer;
    private static boolean isOpened = false;
    private static final String HOST = "smtp.gmail.com";
    private static final int PORT = 587;

    public static void main(String[] args) throws IOException {
        openConnection(HOST, 10000, PORT);
        send(new SMTPMessage("from@test.ru", "to@test.ru", "MESSAGE!"));
    }

    public static void openConnection(String server, int timeout, int port) throws IOException {
        socket = new Socket(server, port);
        isOpened = true;
        socket.setSoTimeout(timeout);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println(reader.readLine());
    }

    public static void send(SMTPMessage message) throws IOException {
        if (isOpened) {
            writer.println("HELO test");
            writer.flush();
            System.out.println(reader.readLine());

            writer.println("STARTTLS");
            writer.flush();
            System.out.println(reader.readLine());

            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, HOST, PORT, true);

            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream()));

            out.println("AUTH LOGIN");
            out.flush();
            System.out.println(in.readLine());

            out.println("VXNlcm5hbWU6");
            out.flush();
            System.out.println(in.readLine());

            out.println("UGFzc3dvcmQ6");
            out.flush();
            System.out.println(in.readLine());

            out.println("MAIL FROM:<" + message.from() + ">");
            out.flush();
            System.out.println(in.readLine());

            out.println("RCPT TO:<" + message.to() + ">");
            out.flush();
            System.out.println(in.readLine());

            out.println("DATA");
            out.flush();
            System.out.println(in.readLine());

            out.println(message.message());
            out.flush();
            System.out.println(in.readLine());

            out.println("QUIT");
            out.flush();
            System.out.println(in.readLine());
        } else {
            throw new IllegalArgumentException("Connection should be opened");
        }
    }

    private record SMTPMessage(String from, String to, String message) { }
}