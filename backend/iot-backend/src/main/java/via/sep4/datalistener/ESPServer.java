package via.sep4.datalistener;

import via.sep4.datalistener.ClientHandler;
import via.sep4.datalistener.ESPDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class ESPServer {
    private final int port;
    private final ESPDataService espDataService;

    @Autowired
    public ESPServer(ESPDataService espDataService) {
        this.port = 23;
        this.espDataService = espDataService;
    }

    public void start() {
        System.out.println("Starting ESP TCP Server on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, espDataService)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
