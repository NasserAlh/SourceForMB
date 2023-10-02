package nasser;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.*;
import velox.api.layer1.common.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

@Layer1SimpleAttachable
@Layer1StrategyName("VP DB")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION1)
public class OnTrade implements CustomModule, TradeDataListener {

    private final ConcurrentSkipListMap<Integer, Integer> volumeProfile = new ConcurrentSkipListMap<>();

    @Override
    public void initialize(String alias, InstrumentInfo instrumentInfo, Api api, InitialState initialState) {
        // Initialization logic here
    }

    public void stop() {
        // Cleanup logic here
    }

    @Override
    public void onTrade(double price, int size, TradeInfo tradeInfo) {
        int priceInTicks = (int) (price * 4);  // Assuming 1 tick = 0.25, adjust as needed
        volumeProfile.merge(priceInTicks, size, Integer::sum);
        logVolumeProfile();
    }

    private void logVolumeProfile() {
        String volumeProfileData = buildVolumeProfileData();
        sendDataToServer(volumeProfileData);
    }

    private String buildVolumeProfileData() {
        StringBuilder volumeProfileBuilder = new StringBuilder();
        for (Entry<Integer, Integer> entry : volumeProfile.entrySet()) {
            double price = entry.getKey() / 4.0 * 0.25;  // Adjusted conversion back to price
            int volume = entry.getValue();
            volumeProfileBuilder.append(String.format("%.2f,%d\n", price, volume));
        }
        return volumeProfileBuilder.toString();
    }

    private void sendDataToServer(String data) {
        try {
            URL url = new URL("http://localhost:8080/volume-profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            conn.setRequestMethod("POST");

            // Enable input and output streams
            conn.setDoOutput(true);

            // Write the data to the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            checkResponse(conn);

        } catch (IOException e) {
            Log.error("Failed to send data to HTTP server", e);
        }
    }

    private void checkResponse(HttpURLConnection conn) throws IOException {
        // Get the response code to ensure the request was successful
        int responseCode = conn.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK) {
            Log.info("Data sent successfully.");
        } else {
            Log.error("Failed to send data. HTTP response code: " + responseCode);
        }
    }
}
