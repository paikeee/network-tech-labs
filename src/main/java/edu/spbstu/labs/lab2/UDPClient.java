package edu.spbstu.labs.lab2;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

public class UDPClient implements Closeable {

    private static DatagramSocket socket;
    private static int pingIdx = -1;
    private static final byte[] receivingDataBuffer = new byte[1048];
    private static final String SERVER = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        socket = new DatagramSocket();
        pingIdx++;
        socket.setSoTimeout(10000);

        LocalDateTime startTime = LocalDateTime.now();
        String message = "C: Pinging " + SERVER + ", #" + pingIdx + " at " + startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        byte[] messageArr = message.getBytes();
        InetAddress serverAddress = InetAddress.getByName(SERVER);
        DatagramPacket packet = new DatagramPacket(messageArr, messageArr.length, serverAddress, 60000);

        socket.send(packet);
        System.out.println("C: Sent ping to a server " + serverAddress.getHostAddress() + ":" + 60000);

        DatagramPacket receivedPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        try {
            socket.receive(receivedPacket);
        } catch (SocketTimeoutException e) {
            System.out.println("C: #" + pingIdx + " Request timeout");
            return;
        }
        LocalDateTime endTime = LocalDateTime.now();
        String receivedMsg = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
        long rtt = endTime.getLong(ChronoField.MILLI_OF_SECOND) - startTime.getLong(ChronoField.MILLI_OF_SECOND);
        System.out.println("C: #" + pingIdx + " | " + receivedMsg + " | RTT: " + rtt);
    }

    @Override
    public void close() {
        socket.close();
    }
}
