package via.sep4.service;

import org.springframework.stereotype.Service;

@Service
public class WaterPump {

    private boolean isPumpRunning = false;

    public boolean startWatering() {
        if (isPumpRunning) {
            System.out.println("Water pump is already running.");
            return false;
        }

        try {
            // Simulate sending a signal to start the pump
            System.out.println("Sending signal to start the water pump...");
            // Add actual hardware control logic here, e.g., GPIO or API call
            isPumpRunning = true;
            System.out.println("Water pump started successfully.");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start the water pump: " + e.getMessage());
            return false;
        }
    }

    public boolean stopWatering() {
        if (!isPumpRunning) {
            System.out.println("Water pump is not running.");
            return false;
        }

        try {
            // Simulate sending a signal to stop the pump
            System.out.println("Sending signal to stop the water pump...");
            // Add actual hardware control logic here, e.g., GPIO or API call
            isPumpRunning = false;
            System.out.println("Water pump stopped successfully.");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to stop the water pump: " + e.getMessage());
            return false;
        }
    }

    public boolean isPumpRunning() {
        return isPumpRunning;
    }
}