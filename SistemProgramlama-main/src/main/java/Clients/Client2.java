package Clients;

import comm.struct.info.SubscriberProto;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client2 {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5002;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream outputStream = socket.getOutputStream();
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println(SERVER_PORT);
            System.out.print("Enter your name: ");
            String name = scanner.nextLine();

            System.out.print("Enter your status (SUB, CHECK, DEL, ONLN, OFLN): ");
            String status = scanner.nextLine();

            SubscriberProto.Subscriber subscriber = SubscriberProto.Subscriber.newBuilder()
                    .setSubscriberId(1)
                    .setName(name)
                    .setStatus(status)
                    .build();

            sendMessage(outputStream, subscriber);
            System.out.println("Subscriber sent to the server.");

            SubscriberProto.Subscriber response = receiveMessage(dataInputStream);
            System.out.println("Response from server: " + response);

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendMessage(OutputStream outputStream, SubscriberProto.Subscriber message) throws IOException {
        byte[] messageBytes = message.toByteArray();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        dataOutput.writeInt(messageBytes.length);
        dataOutput.write(messageBytes);
        dataOutput.flush();
    }

    private static SubscriberProto.Subscriber receiveMessage(DataInputStream dataInputStream) throws IOException {
        int messageLength = dataInputStream.readInt();
        byte[] messageBytes = new byte[messageLength];
        dataInputStream.readFully(messageBytes);
        return SubscriberProto.Subscriber.parseFrom(messageBytes);
    }
}
