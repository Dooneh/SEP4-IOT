package via.sep4;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Greenhouse Sensor Data Simulator");
        System.out.println("================================");

        try {
            while (true) {
                System.out.println("\nSelect an option:");
                System.out.println("0. Create experiment");
                System.out.println("1. Send single sensor reading");
                System.out.println("2. Send multiple random readings");
                System.out.println("3. Test validation of sensor data");
                System.out.println("4. Get latest measurements");
                System.out.println("5. Export measurements as CSV");
                System.out.println("6. Exit");
                System.out.print("\nYour choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 0:
                        createExperiment(scanner);
                        break;
                    case 1:
                        sendSingleReading(scanner);
                        break;
                    case 2:
                        sendMultipleReadings(scanner);
                        break;
                    case 3:
                        sendValidationTest(scanner);
                        break;
                    case 4:
                        getLatestMeasurements(scanner);
                        break;
                    case 5:
                        exportMeasurements(scanner);
                        break;
                    case 6:
                        System.out.println("Exiting.");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void createExperiment(Scanner scanner) throws Exception {
        System.out.println("\nCreate a new experiment");
        System.out.print("Enter name: ");
        String name = scanner.nextLine();

        System.out.print("Enter description: ");
        String description = scanner.nextLine();

        System.out.print("Enter plant species: ");
        String plantSpecies = scanner.nextLine();

        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startDate = scanner.nextLine();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.nextLine();

        Map<String, Object> experimentData = new HashMap<>();
        experimentData.put("name", name);
        experimentData.put("description", description);
        experimentData.put("plantSpecies", plantSpecies);
        experimentData.put("startDate", startDate);
        experimentData.put("endDate", endDate);

        String jsonBody = mapper.writeValueAsString(experimentData);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/experiments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nResponse status code: " + response.statusCode());
        if (response.statusCode() == 201) {
            Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
            System.out.println("Created experiment with ID: " + responseMap.get("id"));
            System.out.println("Use this ID for sending sensor data");
        } else {
            System.out.println("Response body: " + response.body());
        }
    }

    private static void sendSingleReading(Scanner scanner) throws Exception {
        System.out.print("Enter experiment ID: ");
        long experimentId = scanner.nextLong();
        scanner.nextLine();

        Map<String, String> sensorData = generateRandomSensorData();

        System.out.println("\nSending the following sensor data:");
        sensorData.forEach((key, value) -> System.out.println(key + ": " + value));

        String jsonBody = mapper.writeValueAsString(sensorData);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/measurements/" + experimentId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nResponse status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());
    }

    private static void sendMultipleReadings(Scanner scanner) throws Exception {
        System.out.print("Enter experiment ID: ");
        long experimentId = scanner.nextLong();
        scanner.nextLine();

        System.out.print("How many readings to send? ");
        int count = scanner.nextInt();
        scanner.nextLine();

        System.out.println("\nSending " + count + " random sensor readings...");

        for (int i = 0; i < count; i++) {
            Map<String, String> sensorData = generateRandomSensorData();

            LocalDateTime timestamp = LocalDateTime.now().minusMinutes(i * 15);
            sensorData.put("Tidsstempel", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String jsonBody = mapper.writeValueAsString(sensorData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/measurements/" + experimentId))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Reading " + (i + 1) + ": " +
                    (response.statusCode() == 201 ? "Success" : "Failed (" + response.statusCode() + ")"));

            Thread.sleep(500);
        }

        System.out.println("\nFinished sending " + count + " readings.");
    }

    private static void sendValidationTest(Scanner scanner) throws Exception {
        System.out.print("Enter experiment ID: ");
        long experimentId = scanner.nextLong();
        scanner.nextLine();

        // Temperature out of range
        Map<String, String> invalidTempData = new HashMap<>();
        invalidTempData.put("Luft_temperatur", "10.5"); // Below minimum (15°C)
        invalidTempData.put("Luftfugtighed", "50");
        invalidTempData.put("Jord_fugtighed", "60");
        invalidTempData.put("Lys_højeste_intensitet", "800");
        invalidTempData.put("Lys_laveste_intensitet", "200");
        invalidTempData.put("Lys_indstilling", "5");
        invalidTempData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Humidity out of range
        Map<String, String> invalidHumidityData = new HashMap<>();
        invalidHumidityData.put("Luft_temperatur", "25.0");
        invalidHumidityData.put("Luftfugtighed", "120"); // Above maximum (100%)
        invalidHumidityData.put("Jord_fugtighed", "60");
        invalidHumidityData.put("Lys_højeste_intensitet", "800");
        invalidHumidityData.put("Lys_laveste_intensitet", "200");
        invalidHumidityData.put("Lys_indstilling", "5");
        invalidHumidityData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Light intensity invalid
        Map<String, String> invalidLightData = new HashMap<>();
        invalidLightData.put("Luft_temperatur", "25.0");
        invalidLightData.put("Luftfugtighed", "50");
        invalidLightData.put("Jord_fugtighed", "60");
        invalidLightData.put("Lys_højeste_intensitet", "400");
        invalidLightData.put("Lys_laveste_intensitet", "600"); // Higher than highest
        invalidLightData.put("Lys_indstilling", "5");
        invalidLightData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Negative values for non-negative fields
        Map<String, String> negativeValueData = new HashMap<>();
        negativeValueData.put("Luft_temperatur", "25.0");
        negativeValueData.put("Luftfugtighed", "50");
        negativeValueData.put("Jord_fugtighed", "60");
        negativeValueData.put("Lys_højeste_intensitet", "800");
        negativeValueData.put("Lys_laveste_intensitet", "-50"); // Negative
        negativeValueData.put("Lys_indstilling", "5");
        negativeValueData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Invalid timestamp format
        Map<String, String> invalidTimestampData = new HashMap<>();
        invalidTimestampData.put("Luft_temperatur", "25.0");
        invalidTimestampData.put("Luftfugtighed", "50");
        invalidTimestampData.put("Jord_fugtighed", "60");
        invalidTimestampData.put("Lys_højeste_intensitet", "800");
        invalidTimestampData.put("Lys_laveste_intensitet", "200");
        invalidTimestampData.put("Lys_indstilling", "5");
        invalidTimestampData.put("Tidsstempel", "25-04-2025 18:00:00"); // Wrong format

        // Missing required fields
        Map<String, String> missingFieldsData = new HashMap<>();
        missingFieldsData.put("Luft_temperatur", "25.0");
        // Missing Luftfugtighed
        missingFieldsData.put("Jord_fugtighed", "60");
        missingFieldsData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Non-numeric values for numeric fields
        Map<String, String> nonNumericData = new HashMap<>();
        nonNumericData.put("Luft_temperatur", "twenty five"); // Not a number
        nonNumericData.put("Luftfugtighed", "50");
        nonNumericData.put("Jord_fugtighed", "60");
        nonNumericData.put("Tidsstempel", "2025-04-25T18:00:00");

        // Test all the invalid scenarios
        testScenario("Temperature out of range", invalidTempData, experimentId);
        testScenario("Humidity out of range", invalidHumidityData, experimentId);
        testScenario("Invalid light intensity", invalidLightData, experimentId);
        testScenario("Negative values", negativeValueData, experimentId);
        testScenario("Invalid timestamp", invalidTimestampData, experimentId);
        testScenario("Missing required fields", missingFieldsData, experimentId);
        testScenario("Non-numeric values", nonNumericData, experimentId);

        // Test valid data
        Map<String, String> validData = new HashMap<>();
        validData.put("Luft_temperatur", "25.0");
        validData.put("Luftfugtighed", "50");
        validData.put("Jord_fugtighed", "60");
        validData.put("Lys_højeste_intensitet", "800");
        validData.put("Lys_laveste_intensitet", "200");
        validData.put("Lys_indstilling", "5");
        validData.put("Afstand_til_Højde", "30");
        validData.put("Vand_tid_fra_sidste", "48");
        validData.put("Vand_mængde", "250");
        validData.put("Vand_frekvens", "24");
        validData.put("Tidsstempel", "2025-04-25T18:00:00");
        testScenario("Valid data", validData, experimentId);
    }

    private static void testScenario(String description, Map<String, String> data, long experimentId) throws Exception {
        System.out.println("\n--- Testing: " + description + " ---");
        System.out.println("Sending data: " + data);

        String jsonBody = mapper.writeValueAsString(data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/measurements/" + experimentId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response status: " + response.statusCode());
        System.out.println("Response body: " + response.body());
    }

    private static void getLatestMeasurements(Scanner scanner) throws Exception {
        System.out.print("Enter experiment ID: ");
        long experimentId = scanner.nextLong();
        scanner.nextLine();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/experiments/" + experimentId + "/measurements/latest"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nResponse status code: " + response.statusCode());

        if (response.statusCode() == 200) {
            Object json = mapper.readValue(response.body(), Object.class);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println("Latest measurements:");
            System.out.println(prettyJson);
        } else {
            System.out.println("Response body: " + response.body());
        }
    }

    private static void exportMeasurements(Scanner scanner) throws Exception {
        System.out.print("Enter experiment ID: ");
        long experimentId = scanner.nextLong();
        scanner.nextLine();

        System.out.println("\nSelect export format:");
        System.out.println("1. CSV");
        System.out.println("2. JSON");
        System.out.print("\nYour choice: ");
        int formatChoice = scanner.nextInt();
        scanner.nextLine();

        String format = formatChoice == 1 ? "csv" : "json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/experiments/" + experimentId + "/export/" + format))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nResponse status code: " + response.statusCode());

        if (response.statusCode() == 200) {
            System.out.println("Exported data:");
            System.out.println(response.body());

            // Also save to file
            String filename = "experiment_" + experimentId + "_data." + format;
            java.nio.file.Files.writeString(java.nio.file.Path.of(filename), response.body());
            System.out.println("\nData saved to file: " + filename);
        } else {
            System.out.println("Response body: " + response.body());
        }
    }

    private static Map<String, String> generateRandomSensorData() {
        Map<String, String> data = new HashMap<>();

        data.put("Luft_temperatur", String.format("%.1f", 15 + random.nextDouble() * 25)); // 15-40°C
        data.put("Luftfugtighed", String.valueOf(random.nextInt(101))); // 0-100%
        data.put("Jord_fugtighed", String.valueOf(random.nextInt(101))); // 0-100%

        int highestLight = 500 + random.nextInt(501); // 500-1000
        int lowestLight = random.nextInt(400); // 0-399
        data.put("Lys_højeste_intensitet", String.valueOf(highestLight));
        data.put("Lys_laveste_intensitet", String.valueOf(lowestLight));
        data.put("Lys_gennemsnit", String.valueOf((highestLight + lowestLight) / 2));

        data.put("Lys_indstilling", String.valueOf(random.nextInt(11)));
        data.put("Afstand_til_Højde", String.valueOf(10 + random.nextInt(41))); // 10-50
        data.put("Vand_tid_fra_sidste", String.valueOf(random.nextInt(73))); // 0-72 hours
        data.put("Vand_mængde", String.valueOf(100 + random.nextInt(201))); // 100-300ml
        data.put("Vand_frekvens", String.valueOf(12 + random.nextInt(13))); // 12-24 hours

        data.put("Tidsstempel", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        return data;
    }
}
