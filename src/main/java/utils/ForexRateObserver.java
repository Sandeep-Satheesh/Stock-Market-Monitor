package utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class ForexRateObserver {
    String currPair;
    JTextArea txtLog;
    TimeSeriesPlotter graphPlotter;
    Observable<Pair<Long, Double>> dataSource = null;

    private boolean isObserving = false;

    public ForexRateObserver(String currPair, JTextArea txtLog) {
        this.currPair = currPair;
        this.txtLog = txtLog;
    }

    public void startObserving() {
        initDataSource();
        isObserving = true;
        graphPlotter = new TimeSeriesPlotter(dataSource, "Currency Rate Trend for " + currPair, "Time", "Unit Rate", currPair + " trend");
    }

    public boolean isObserving() {
        return isObserving;
    }

    private void initDataSource() {
        CommonUtils.logMessage(txtLog, "Started requesting data for currency pair " + currPair + " with period: 8 seconds.", "ForexRateObserver Backend");
        dataSource = Observable.interval(8, TimeUnit.SECONDS)
                .map(e -> {
                    String response = getRealtimePrice();
                    if (response == null) {
                        CommonUtils.logMessage(txtLog, "Error occurred while receiving data:\n" + response, "ForexRateObserver");
                        CommonUtils.logMessage(txtLog, "Stopping observation...", "ForexRateObserver");
                        stopObserving();
                    } else {
                        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
                        try {
                            Double price = Double.parseDouble(jsonObject.get("rate").getAsString());
                            Long timestamp = System.currentTimeMillis(); //Long.parseLong(jsonObject.get("timestamp").getAsString());
                            CommonUtils.logMessage(txtLog, "Received data point: (" + price + ", " + timestamp + ")", "ForexRateObserver");

                            return new Pair<>(timestamp, price);

                        } catch (Exception exception) {
                            CommonUtils.logMessage(txtLog, "Received response: " + response + " from server!", "ForexRateObserver");
                            CommonUtils.logMessage(txtLog, "ERROR: " + exception.getMessage() + "\nCAUSE: " + exception.getCause(), "ForexRateObserver");
                        }
                    }
                    stopObserving();
                    Schedulers.shutdown();
                    CommonUtils.logMessage(txtLog, "Stopped requesting data!", "ForexRateObserver Backend");
                    throw new Exception("Stopped Observing!");
                }).doOnError(throwable -> CommonUtils.logMessage(txtLog, "ERROR: " + throwable.getMessage() + "\nCAUSE: " + throwable.getCause(), "ForexRateObserver Backend"))
                .doOnComplete(() -> {
                    CommonUtils.logMessage(txtLog, "Completed!", "ForexRateObserver");
                });
    }

    public void stopObserving() {
        graphPlotter.stopPlotting();
        CommonUtils.logMessage(txtLog, "Stopped plotting data.", "ForexRateObserver Backend");
        isObserving = false;
    }

    public String getRealtimePrice() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", currPair);
        params.addProperty("apikey", Constants.API_KEY);
        params.addProperty("timezone", "Asia/Kolkata");
        params.addProperty("precision", 5);
        return HTTPHelper.sendSynchronousRequest(
                Constants.SERVER_URL + "/exchange_rate",
                "get",
                null,
                4000,
                10000,
                true,
                false,
                params
        );
    }
}