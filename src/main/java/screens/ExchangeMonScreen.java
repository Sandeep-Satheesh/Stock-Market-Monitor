package screens;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import models.StockSymbol;
import utils.CommonUtils;
import utils.Constants;
import utils.HTTPHelper;
import utils.StockPriceObserver;
import utils.listeners.OnHTTPRequestCompletedListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ExchangeMonScreen {
    public JPanel mainPanel, controlPane;
    public JComboBox cbxSym;
    private JProgressBar pbWorking;
    private JTextArea txtLog;
    private JButton btnStartMon;
    public JFrame windowFrame;

    private StockPriceObserver stockPriceObserver = null;
    private HashMap<String, StockSymbol> companiesMap = null;

    public ExchangeMonScreen() {
        windowFrame = new JFrame("Stock Exchange Monitor");

        //windowFrame.setResizable(false);
        windowFrame.setSize(900, 500);
        windowFrame.setContentPane(mainPanel);
        windowFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        windowFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (stockPriceObserver == null) { windowFrame.dispose(); return; }
                if (stockPriceObserver.isObserving()) {
                    int i = JOptionPane.showConfirmDialog(
                            windowFrame,
                            "Stop monitoring and close?",
                            "Stop monitoring and close?",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (i == JOptionPane.YES_OPTION)
                        stockPriceObserver.stopObserving();
                }
                windowFrame.dispose();
            }
        });

        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        //get the list of companies to show the trends for.
        initCompanyList();


        btnStartMon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnStartMon.getText().equals("Start Monitor!")) {
                    //Check if selected company symbol exists and is valid.
                    String item = (String) cbxSym.getSelectedItem();
                    if (item == null || item.isEmpty()) {
                        CommonUtils.logMessage(txtLog, "No symbol selected!", "Exchange Monitor UI");
                        return;
                    }

                    final String finalItem = item.substring(0, item.indexOf(':'));
                    initMonitoring(finalItem);
                    btnStartMon.setText("Stop Monitor!");
                }
                else {
                    if (stockPriceObserver != null) stockPriceObserver.stopObserving();
                    btnStartMon.setText("Start Monitor!");
                }
            }
        });
    }

    private void initMonitoring(String symbol) {
        if (stockPriceObserver != null && stockPriceObserver.isObserving())
            stockPriceObserver.stopObserving();

        stockPriceObserver = new StockPriceObserver(companiesMap.get(symbol), txtLog);
        stockPriceObserver.startObserving();
    }

    private void initCompanyList() {
        pbWorking.setIndeterminate(true);
        pbWorking.setStringPainted(true);
        cbxSym.setEnabled(false);
        btnStartMon.setEnabled(false);

        pbWorking.setString("Connecting to server...");

        JsonObject params = new JsonObject();
        params.addProperty("country", "United States");

        CommonUtils.logMessage(txtLog, "Attempting to connect to Twelve API server...", "Exchange Monitor Backend");

        HTTPHelper.sendRequest(
                Constants.SERVER_URL + "/stocks",
                "GET",
                null,
                10000,
                10000,
                true,
                false,
                params,
                new OnHTTPRequestCompletedListener() {
                    @Override
                    public void onRequestFailed(int errorCode, String response) {
                        btnStartMon.setEnabled(false);
                        pbWorking.setIndeterminate(false);
                        cbxSym.setEnabled(true);
                        pbWorking.setValue(pbWorking.getMaximum());
                        CommonUtils.logMessage(txtLog, "Data failed to load: " + response + "\nError Code: " + errorCode, "Exchange Monitor Backend");
                        pbWorking.setString("Done");
                    }

                    @Override
                    public void onRequestSuccess(int status, String response) {
                        CommonUtils.logMessage(txtLog, "Received data with status: " + status + ", processing...", "Exchange Monitor Backend");

                        pbWorking.setString("Populating data...");
                        JsonObject object = new Gson().fromJson(response, JsonObject.class);

                        ArrayList<StockSymbol> symbols = new Gson().fromJson(object.get("data"), new TypeToken<List<StockSymbol>>() {
                        }.getType());
                        CommonUtils.logMessage(txtLog, "Symbols list received! Received total: " + symbols.size() + " items.", "Exchange Monitor Backend");

                        populateSymbolList(symbols);

                        pbWorking.setIndeterminate(false);
                        cbxSym.setEnabled(true);
                        pbWorking.setValue(pbWorking.getMaximum());

                        CommonUtils.logMessage(txtLog, "Companies list was fetched successfully.", "Exchange Monitor Backend");
                        pbWorking.setString("Done!");
                        btnStartMon.setEnabled(true);
                    }
                }
        );
    }

    private void populateSymbolList(ArrayList<StockSymbol> symbols) {
        HashSet<String> rejects = new HashSet<>(3);
        companiesMap = new HashMap<>();

        for (StockSymbol symbol : symbols) {
            if (rejects.contains(symbol.getSymbol())) continue;
            String s = symbol.getName().toLowerCase().trim();
            s = s.replaceAll("\\.", "").replaceAll(",", "").replaceAll(" ", "#");

            if (symbol.getName().length() <= 3) {
                rejects.add(s);
                continue;
            }
            if (rejects.contains(s)) continue;

            boolean flg = false;
            for (char c : symbol.getSymbol().toCharArray())
                if (Character.isDigit(c)) {
                    rejects.add(s);
                    flg = true;
                    break;
                }
            if (flg) continue;

            companiesMap.put(symbol.getSymbol(), symbol);
            cbxSym.addItem(symbol.getSymbol() + ": " + symbol.getName());

            rejects.add(s);
            rejects.add(symbol.getSymbol());
        }
    }

    public void showWindow() {
        windowFrame.setVisible(true);
    }
}
