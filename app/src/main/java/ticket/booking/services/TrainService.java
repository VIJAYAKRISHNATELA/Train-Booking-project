package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import ticket.booking.entities.Train;
import ticket.booking.util.AppConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {
    List<Train> trainList;
    private final ObjectMapper om = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TrainService() throws IOException {
        loadTrains();
    }

    private void loadTrains() throws IOException {
        // Create directory if it doesn't exist
        File directory = new File(AppConfig.DATA_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Try to load from external file
        File trainsFile = new File(AppConfig.TRAINS_FILE_PATH);
        if (trainsFile.exists()) {
            trainList = om.readValue(trainsFile, new TypeReference<List<Train>>() {});
        } else {
            // If external file doesn't exist, try to load from resources
            InputStream is = getClass().getResourceAsStream("/localDb/trains.json");
            if (is != null) {
                trainList = om.readValue(is, new TypeReference<List<Train>>() {});
                // Save to external location for future use
                saveTrainList();
            } else {
                // Start with empty list if no file exists
                trainList = new ArrayList<>();
            }
        }
    }

    private void saveTrainList() {
        try {
            File directory = new File(AppConfig.DATA_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            om.writeValue(new File(AppConfig.TRAINS_FILE_PATH), trainList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<Train> searchTrains(String src, String des){
        return trainList.stream().filter(train -> validateTrain(train, src, des)).collect(Collectors.toList());
    }

    public void addTrain(Train newTrain){
        Optional<Train> exist = trainList.stream().filter(train -> train.getTrainId().equalsIgnoreCase(newTrain.getTrainId())).findFirst();
        if(exist.isPresent()){
            updateTrain(newTrain);
        }else{
            trainList.add(newTrain);
            saveTrainList();
        }
    }
    // In TrainService class
    public boolean updateTrain(Train updatedTrain) {
        try {
            boolean found = false;
            for (int i = 0; i < trainList.size(); i++) {
                if (trainList.get(i).getTrainId().equals(updatedTrain.getTrainId())) {
                    trainList.set(i, updatedTrain);
                    found = true;
                    break;
                }
            }

            if (found) {
                saveTrainList();
                return true;
            } else {
                System.out.println("Train not found for update: " + updatedTrain.getTrainId());
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error updating train: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateTrain(Train train, String src, String des){
        List<String> stationOrder = train.getStations();
        int srcIndex = stationOrder.indexOf(src.toLowerCase());
        int desIndex = stationOrder.indexOf(des.toLowerCase());
        return srcIndex != -1 && desIndex != -1 && srcIndex < desIndex;
    }
}
