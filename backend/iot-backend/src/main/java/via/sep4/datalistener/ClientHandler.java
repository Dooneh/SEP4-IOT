package via.sep4.datalistener;

import via.sep4.datalistener.ESPDataService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ESPDataService espDataService; // <-- NEW

    public ClientHandler(Socket clientSocket, ESPDataService espDataService) {
        this.clientSocket = clientSocket;
        this.espDataService = espDataService;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                espDataService.processData(line); // <-- send to service
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected");
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
