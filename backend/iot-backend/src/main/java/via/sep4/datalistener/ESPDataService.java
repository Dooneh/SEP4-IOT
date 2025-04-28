package via.sep4.datalistener;

import org.springframework.stereotype.Service;
import via.sep4.controller.PlantMeasurementController;
import via.sep4.processing.DataValidator;
import java.util.regex.*;

@Service
public class ESPDataService {

    DataValidator dataValidator = new DataValidator();
    PlantMeasurementController plantMeasurementController = new PlantMeasurementController();

    Pattern pattern = Pattern.compile("(Distance|Temp|Humidity): (\\d+)");

    int distance = -1; // default if missing
    float temp = -1;
    int humidity = -1;

    public void processData(String data) {
        System.out.println("Processing data: " + data);
        Matcher matcher = pattern.matcher(data);


        while (matcher.find()) {
            String label = matcher.group(1);
            String number = matcher.group(2);

            switch (label) {
                case "Distance":
                    distance = Integer.parseInt(number);
                    break;
                case "Temp":
                    temp = Float.parseFloat(number);
                    break;
                case "Humidity":
                    humidity = Integer.parseInt(number);
                    break;
            }
        }

        System.out.println("Distance: " + distance); //Midlertidig til test
        System.out.println("Temp: " + temp); // Til test
        System.out.println("Humidity: " + humidity); // Til test

        //if (dataValidator.validateHeight(distance) == DataValidator.ValidationResult.VALIDATION_SUCCESS){
        //    plantMeasurementController.addMeasurement()
        //}
        //else throw new ValidationException("Validation exception type " + (dataValidator.validateTemperature(distance) == DataValidator.ValidationResult.VALIDATION_SUCCESS));


        //if (dataValidator.validateTemperature(temp) == DataValidator.ValidationResult.VALIDATION_SUCCESS){
        //    plantMeasurementController.addMeasurement()
        //}
        //else throw new ValidationException("Validation exception type " + (dataValidator.validateTemperature(temp) == DataValidator.ValidationResult.VALIDATION_SUCCESS));

        //if (dataValidator.validateHumidity(humidity) == DataValidator.ValidationResult.VALIDATION_SUCCESS){
        //    plantMeasurementController.addMeasurement()
        //}
        //else throw new ValidationException("Validation exception type " + (dataValidator.validateHumidity(humidity) == DataValidator.ValidationResult.VALIDATION_SUCCESS));


    }
}
