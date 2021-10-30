package utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import models.StockSymbol;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class StockPriceObserver {
    StockSymbol company;
    JTextArea txtLog;
    TimeSeriesPlotter graphPlotter;
    Observable<Pair<Long, Double>> dataSource = null;

    private boolean isObserving = false;

    public StockPriceObserver(StockSymbol company, JTextArea txtLog) {
        this.company = company;
        this.txtLog = txtLog;
    }dataSource

    public void startObserving() {
        initDataSource();
        isObserving = true;
        graphPlotter = new TimeSeriesPlotter(, "Share Price Trend for " + company.getName() + " (" + company.getSymbol() + ")", "Time", "Share Price (USD)", company.getSymbol());
    }

    public boolean isObserving() {
        return isObserving;
    }

    private void initDataSource() {
        CommonUtils.logMessage(txtLog, "Started requesting data for instrument " + company.getName() + " (" + company.getSymbol() + ") with period: 8 seconds.", "StockPriceObserver Backend");
        dataSource = Observable.interval(8, TimeUnit.SECONDS)
                .map(e -> {
                    String response = getRealtimePrice();
                    if (response == null) {
                        CommonUtils.logMessage(txtLog, "Error occurred while receiving data:\n" + response, "StockPriceObserver");
                        CommonUtils.logMessage(txtLog, "Stopping observation...", "StockPriceObserver");
                        stopObserving();
                    } else {
                        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
                        try {
                            Double price = Double.parseDouble(jsonObject.get("price").getAsString());
                            CommonUtils.logMessage(txtLog, "Received data point: " + price, "StockPriceObserver");

                            return new Pair<>(System.currentTimeMillis(), price);

                        } catch (Exception exception) {
                            CommonUtils.logMessage(txtLog, "Received response: " + response + " from server!", "StockPriceObserver");
                            CommonUtils.logMessage(txtLog, "ERROR: " + exception.getMessage() + "\nCAUSE: " + exception.getCause(), "StockPriceObserver");
                        }
                    }
                    stopObserving();
                    Schedulers.shutdown();
                    CommonUtils.logMessage(txtLog, "Stopped requesting data!", "StockPriceObserver Backend");
                    throw new Exception("Stopped Observing!");
                }).doOnError(throwable -> CommonUtils.logMessage(txtLog, "ERROR: " + throwable.getMessage() + "\nCAUSE: " + throwable.getCause(), "StockPriceObserver Backend"))
                .doOnComplete(() -> {
                    CommonUtils.logMessage(txtLog, "Completed!", "StockPriceObserver");
                });
    }

    public void stopObserving() {
        graphPlotter.stopPlotting();
        CommonUtils.logMessage(txtLog, "Stopped plotting data.", "StockPriceObserver Backend");
        isObserving = false;
    }

    public String getRealtimePrice() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", company.getSymbol());
        params.addProperty("apikey", Constants.API_KEY);

        return HTTPHelper.sendSynchronousRequest(
                Constants.SERVER_URL + "/price",
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