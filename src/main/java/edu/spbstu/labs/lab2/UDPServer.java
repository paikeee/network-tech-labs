package edu.spbstu.labs.lab2;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class UDPServer implements Closeable {

    private static DatagramSocket socket;

    public static void main(String[] args) throws IOException {
        socket = new DatagramSocket(60000);
        System.out.println("S: Server has started at port " + socket.getLocalPort());
        Random random = new Random();

        while (true) {
            byte[] receivingDataBuffer = new byte[1048];
            DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);

            System.out.println("---------------------------------");
            System.out.println("S: Waiting for a client's message");

            if (random.nextInt(10) < 4) {
                System.out.println("S: Packet drop!");
                continue;
            }

            try {
                socket.receive(inputPacket);
            } catch (IOException e) {
                break;
            }

            String receivedData = new String(inputPacket.getData(), 0, inputPacket.getLength());

            System.out.println("S: Received a client's (" + inputPacket.getAddress().getHostAddress() +
                    ":" + inputPacket.getPort() + ")" + " message: " + receivedData);

            byte[] sendingDataBuffer = receivedData.toUpperCase().getBytes();

            InetAddress senderAddress = inputPacket.getAddress();
            int senderPort = inputPacket.getPort();

            DatagramPacket outputPacket = new DatagramPacket(sendingDataBuffer, sendingDataBuffer.length, senderAddress, senderPort);
            socket.send(outputPacket);

            System.out.println("S: Sent a response to a client: " + outputPacket.getAddress().getHostAddress() + ":" + outputPacket.getPort());
        }
    }

    @Override
    public void close() {
        socket.close();
    }
}
