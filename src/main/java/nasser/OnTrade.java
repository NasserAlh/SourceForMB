package nasser;

import velox.api.layer1.annotations.*;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.simplified.*;
import velox.api.layer1.common.Log;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map.Entry;

@Layer1SimpleAttachable
@Layer1StrategyName("VP XXXXX")
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
        try {
            URL url = new URL("http://localhost:5000/volume_profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream())) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{");
                for (Entry<Integer, Integer> entry : volumeProfile.entrySet()) {
                    double price = entry.getKey() / 4.0 * 0.25;  // Adjusted conversion back to price
                    int volume = entry.getValue();
                    jsonBuilder.append(String.format("\"%.2f\": %d,", price, volume));
                }
                // Remove trailing comma and close JSON object
                jsonBuilder.setLength(jsonBuilder.length() - 1);
                jsonBuilder.append("}");

                out.write(jsonBuilder.toString());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.error("Failed to send volume profile data: " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            Log.error("Failed to send volume profile data", e);
        }
    }
}

