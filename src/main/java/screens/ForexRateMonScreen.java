package screens;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import models.CurrencyPair;
import utils.CommonUtils;
import utils.Constants;
import utils.ForexRateObserver;
import utils.HTTPHelper;
import utils.listeners.OnHTTPRequestCompletedListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class ForexRateMonScreen {
    public JPanel mainPanel, controlPane;
    public JComboBox cbxSym;
    private JProgressBar pbWorking;
    private JTextArea txtLog;
    private JButton btnStartMon;
    public JFrame windowFrame;

    private ForexRateObserver forexRateObserver = null;
    private String currPair = null;

    public ForexRateMonScreen() {
        windowFrame = new JFrame("Currency Rate Monitor");

        //windowFrame.setResizable(false);
        windowFrame.setSize(900, 500);
        windowFrame.setContentPane(mainPanel);
        windowFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        windowFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (forexRateObserver == null) { windowFrame.dispose(); return; }
                if (forexRateObserver.isObserving()) {
                    int i = JOptionPane.showConfirmDialog(
                            windowFrame,
                            "Stop monitoring and close?",
                            "Stop monitoring and close?",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (i == JOptionPane.YES_OPTION)
                        forexRateObserver.stopObserving();
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
                        CommonUtils.logMessage(txtLog, "No symbol selected!", "Currency Rate Monitor UI");
                        return;
                    }

                    final String finalItem = item.substring(0, item.indexOf(' '));
                    initMonitoring(finalItem);
                    btnStartMon.setText("Stop Monitor!");
                }
                else {
                    if (forexRateObserver != null) forexRateObserver.stopObserving();
                    btnStartMon.setText("Start Monitor!");
                }
            }
        });
    }

    private void initMonitoring(String symbol) {
        if (forexRateObserver != null && forexRateObserver.isObserving())
            forexRateObserver.stopObserving();

        forexRateObserver = new ForexRateObserver(symbol, txtLog);
        forexRateObserver.startObserving();
    }

    private void initCompanyList() {
        pbWorking.setIndeterminate(true);
        pbWorking.setStringPainted(true);
        cbxSym.setEnabled(false);
        btnStartMon.setEnabled(false);

        pbWorking.setString("Connecting to server...");

        JsonObject params = new JsonObject();
        params.addProperty("country", "United States");

        CommonUtils.logMessage(txtLog, "Attempting to connect to Twelve API server...", "Currency Rate Monitor Backend");

        HTTPHelper.sendRequest(
                Constants.SERVER_URL + "/forex_pairs",
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
                        CommonUtils.logMessage(txtLog, "Data failed to load: " + response + "\nError Code: " + errorCode, "Currency Rate Monitor Backend");
                        pbWorking.setString("Done");
                    }

                    @Override
                    public void onRequestSuccess(int status, String response) {
                        CommonUtils.logMessage(txtLog, "Received data with status: " + status + ", processing...", "Currency Rate Monitor Backend");

                        pbWorking.setString("Populating data...");
                        JsonObject object = new Gson().fromJson(response, JsonObject.class);

                        ArrayList<CurrencyPair> symbols = new Gson().fromJson(object.get("data"), new TypeToken<List<CurrencyPair>>() {
                        }.getType());
                        CommonUtils.logMessage(txtLog, "Symbols list received! Received total: " + symbols.size() + " items.", "Currency Rate Monitor Backend");

                        populateSymbolList(symbols);

                        pbWorking.setIndeterminate(false);
                        cbxSym.setEnabled(true);
                        pbWorking.setValue(pbWorking.getMaximum());

                        CommonUtils.logMessage(txtLog, "Companies list was fetched successfully.", "Currency Rate Monitor Backend");
                        pbWorking.setString("Done!");
                        btnStartMon.setEnabled(true);
                    }
                }
        );
    }

    private void populateSymbolList(ArrayList<CurrencyPair> symbols) {
        for (CurrencyPair p : symbols) {
            cbxSym.addItem(p.getSymbol() + " (" + p.getCurrency_base() + " - " + p.getCurrency_quote() + ")");
        }
    }

    public void showWindow() {
        windowFrame.setVisible(true);
    }
}
