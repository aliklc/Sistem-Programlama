package dist_servers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import comm.struct.info.CapacityProto.Capacity;
import comm.struct.info.ConfigurationProto;
import comm.struct.info.ConfigurationProto.Configuration;
import comm.struct.info.MessageProto;
import comm.struct.info.SubscriberProto.Subscriber;
import comm.struct.info.MessageProto.Message;

public class ServerBase {

    public static final int[] ports;
    private int serverNumber;
    private int port;
    private int toleranceLevel;
    private boolean CanServe = false;
    private volatile boolean serverConnectionTime = true;
    private long saving_id = 1;
    private ServerSocket serverSocket;
    private List<Socket> serverSockets;
    private long startTime = System.currentTimeMillis();
    private Map<Long, Subscribers> clientsInfo;

    static {
        ports = loadPortsFromFile("C:\\SistemProgramlama\\src\\main\\java\\dist_servers\\servers.txt");
    }

    public class Subscribers {
        private String status;
        private String name;
        private long id;

        public Subscribers(long id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String toString() {
            return "Subscribers{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

    ServerBase(int serverId) {
        this.serverNumber = serverId;
        this.port = ports[serverId - 1];
        this.clientsInfo = new TreeMap<Long, Subscribers>();
        this.serverSockets = new ArrayList<Socket>();
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(this.port);
        connectAdmin();
    }

    private static int[] loadPortsFromFile(String fileName) {
        List<Integer> portList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    portList.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return portList.stream().mapToInt(i -> i).toArray();
    }



    private void connectOtherServers(int toleranceLevel) {
        for (int port : ports) {
            if (port == this.port)
                continue;
            if (toleranceLevel > 0)
            {
                new Thread(() -> {connectServerSocket(port);}).start();
            } else {
                break;
            }
            toleranceLevel--;
        }
        Thread thread = new Thread(() -> {acceptServerSocket();});
        thread.start();
        try {
            thread.join(2000);
            if (thread.isAlive()) {
                serverConnectionTime = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void connectServerSocket(int port) {
        try {
            serverSockets.add(new Socket("localhost", port));
            System.out.println("Connected to server on port: " + port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptServerSocket() {
        try {
            serverSocket.setSoTimeout(1000);
            while (serverConnectionTime) {
                Socket socket = serverSocket.accept();
                System.out.println("Connected to a server.");
                new Thread(() -> {distrubutedServerHandler(socket);}).start();
            }
        } catch (IOException e) {
            try {
                serverSocket.setSoTimeout(0);
                System.out.println("Connection timeout exceeded.");
            } catch (SocketException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void distrubutedServerHandler(Socket socket) {
        try {
            while (CanServe) {
                Subscriber sub = Subscriber.parseFrom(get(socket));
                Long matchingId = clientsInfo.values().stream()
                        .filter(subscriber -> subscriber.getName().equals(sub.getName()))
                        .map(Subscribers::getId)
                        .findFirst()
                        .orElse(null);

                if (matchingId == null) {
                    switch (sub.getStatus()) {
                        case "SUB":
                            addSub(sub);
                            System.out.println("A new sub has just added. Sub ID : " + (saving_id - 1));
                            break;
                        default:
                            System.out.println("Unresonable Request " + sub.getStatus());
                            break;
                    }
                } else {
                    switch (sub.getStatus()) {
                        case "DEL":
                            removeSub(matchingId);
                            System.out.println("A user just deleted. Sub ID : " + matchingId);
                            break;
                        case "ONLN":
                            clientsInfo.get(matchingId).setStatus("ONLN");
                            System.out.println("State update: ONLN");
                            break;
                        case "OFLN":
                            clientsInfo.get(matchingId).setStatus("OFLN");
                            System.out.println("State update: OFLN");
                            break;
                        default:
                            System.out.println("Unresonable Request " + sub.getStatus());
                            break;
                    }
                }
                System.out.println("Subs count: " + clientsInfo.size());
            }
        } catch (IOException e) {
            System.out.println("A connection has been croupted");
            e.printStackTrace();
        }
    }


    private synchronized void sendToServer(Subscriber subscriber) throws IOException {
        for (Socket socket : serverSockets) {
            send(subscriber, socket);
        }
    }

    private Message createMessage(Configuration configuration) {
        toleranceLevel = configuration.getFaultToleranceLevel();
        CanServe = configuration.getMethodType() == ConfigurationProto.MethodType.START;
        return Message.newBuilder().setDemand(MessageProto.Demand.STRT).setResponse(CanServe ? MessageProto.Response.YEP : MessageProto.Response.NOP).build();
    }

    private synchronized void addSub(Subscriber subscriber) {
        Subscribers addSubscriber = new Subscribers(saving_id, subscriber.getName(), "SUB");
        clientsInfo.put(saving_id, addSubscriber);
        saving_id += 1;
    }

    private synchronized void removeSub(Long subId) {
        clientsInfo.remove(subId);
        saving_id -= 1;
    }

    public static byte[] get(Socket socket) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        int length = input.readInt();
        byte[] requestBytes = new byte[length];
        input.readFully(requestBytes);
        return requestBytes;
    }


    private void clientHandler(Socket clientSocket) throws IOException {
        Subscriber sub = Subscriber.parseFrom(get(clientSocket));
        Long matchingId = clientsInfo.values().stream()
                .filter(subscriber -> subscriber.getName().equals(sub.getName()))
                .map(Subscribers::getId)
                .findFirst()
                .orElse(null);
        if (matchingId == null) {
            switch (sub.getStatus()) {
                case "SUB":
                    addSub(sub);
                    System.out.println("New subscriber added. ID: " + (saving_id - 1));
                    sendToServer(sub);
                    send(Subscriber.newBuilder().setName(sub.getName()).setSubscriberId(saving_id - 1).setStatus("NEW SUB IS ADDED").build(), clientSocket);
                    break;
                default:
                    System.out.println("Unresponsable request");
                    send(Subscriber.newBuilder().setName(sub.getName()).setStatus("UNRESPONSABLE REQUEST").build(), clientSocket);
                    break;
            }
            System.out.println(clientsInfo.size());
        }
        else {
            switch (sub.getStatus()) {
                case "DEL":
                    removeSub(matchingId);
                    System.out.println("Subscriber deleted. ID: " + matchingId);
                    System.out.println(clientsInfo.size());
                    sendToServer(sub);
                    send(Subscriber.newBuilder().setSubscriberId(matchingId).setName(sub.getName()).setStatus("DELETED USER").build(), clientSocket);
                    break;
                case "CHECK":
                    System.out.println("SENDING USER DATA");
                    send(Subscriber.newBuilder().setSubscriberId(matchingId).setName(sub.getName()).setStatus(clientsInfo.get(matchingId).getStatus()).build(), clientSocket);
                case "ONLN":
                    clientsInfo.get(matchingId).setStatus("ONLN");
                    System.out.println(clientsInfo.get(matchingId).getStatus());
                    sendToServer(sub);
                    send(Subscriber.newBuilder().setSubscriberId(matchingId).setName(sub.getName()).setStatus("STATUS: ONLN").build(), clientSocket);
                    break;
                case "OFLN":
                    clientsInfo.get(matchingId).setStatus("OFLN");
                    System.out.println(clientsInfo.get(matchingId).getStatus());
                    sendToServer(sub);
                    send(Subscriber.newBuilder().setSubscriberId(matchingId).setName(sub.getName()).setStatus("STATUS: OFLN").build(), clientSocket);
                    break;
                case "SUB":
                    System.out.println("This user is already registered");
                    send(Subscriber.newBuilder().setSubscriberId(matchingId).setName(sub.getName()).setStatus("THIS USER IS ALREADY REGISTERED").build(), clientSocket);
                default:
                    System.out.println("Unresponsable request");
                    send(Subscriber.newBuilder().setName(sub.getName()).setStatus("UNRESPONSABLE REQUEST").build(), clientSocket);
                    break;
            }
            System.out.println(clientsInfo.size());
        }
    }

    public static <T extends MessageLite> void send(T response, Socket socket) throws IOException {
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        byte[] responseBytes = response.toByteArray();
        output.writeInt(responseBytes.length);
        output.write(responseBytes);
    }

    private Capacity createCapacity() {
        long endTime = System.currentTimeMillis();
        return Capacity.newBuilder().setServerXStatus(clientsInfo.size()).setTimestamp((endTime - startTime)/1000).setServerId(serverNumber).build();
    }

    private void connectClients() {
        new Thread(() -> {
            while (CanServe) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("A connection: " + clientSocket.getRemoteSocketAddress());
                    new Thread(() -> {
                        try {
                            clientHandler(clientSocket);
                        } catch (IOException e) {
                            System.out.println("client is not here");
                            e.printStackTrace();
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void connectAdmin() {
        try (Socket clientSocket = serverSocket.accept()) {
            System.out.println("Admin has connected to the server");
            Configuration conf = Configuration.parseFrom(get(clientSocket));
            Message message = createMessage(conf);
            send(message, clientSocket);
            System.out.println("message sent");
            adminHandler(clientSocket);
        } catch (IOException | InterruptedException e) {
            System.out.println("Admin connection has been croupted. Server is shutting down");
            System.exit(1);
        }
    }

    private void adminHandler(Socket adminSocket) throws InvalidProtocolBufferException, IOException, InterruptedException {
        Message message;
        Capacity recivedCapacity;
        if (CanServe) {
            connectOtherServers(toleranceLevel);
            Thread.sleep(1000);
            connectClients();
            System.out.println("Server can respond admin's requests");
            while (CanServe) {
                message = Message.parseFrom(get(adminSocket));
                recivedCapacity = Capacity.parseFrom(get(adminSocket));
                if (message.getDemand() == MessageProto.Demand.CPCTY && recivedCapacity.getServerId() == serverNumber) {
                    Capacity capacity = createCapacity();
                    send(capacity, adminSocket);
                }
            }
        }
    }
}