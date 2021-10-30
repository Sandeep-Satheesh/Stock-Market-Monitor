package screens;

import utils.CommonUtils;
import utils.Constants;

import javax.swing.*;

public class MainScreen {
    private JPanel mainPanel;
    private JButton btnStockMon;
    private JButton btnRateMon;
    private JLabel lblTitle;
    JFrame windowFrame;

    public MainScreen() {
        //NOTE: Constructor used by UI designer to init components

        windowFrame = new JFrame("Stock Monitor v1.0");
        windowFrame.setResizable(false);
        windowFrame.setSize(900, 500);
        windowFrame.setContentPane(mainPanel);
        windowFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        windowFrame.setVisible(true);

        //Other essential code below
        btnRateMon.setFocusPainted(false);
        btnStockMon.setFocusPainted(false);

        //Button listeners
        btnRateMon.addActionListener(e -> {
            if (initAPIKeySuccessful())
                loadRateMon();
        });

        btnStockMon.addActionListener(e -> {
            if (initAPIKeySuccessful())
                loadExchangeMon();
        });
    }

    private void loadRateMon() {
        ForexRateMonScreen fm = new ForexRateMonScreen();
        //panelMain.setVisible(false);
        fm.showWindow();
    }

    private void loadExchangeMon() {
        ExchangeMonScreen em = new ExchangeMonScreen();
        //panelMain.setVisible(false);
        em.showWindow();
    }

    private boolean initAPIKeySuccessful() {
        String apiKey = CommonUtils.input(
                "Enter your API Key:",
                "Please enter your API Key for Twelve Data API authentication:",
                JOptionPane.PLAIN_MESSAGE
        );
        if (apiKey == null) return false;

        if (!CommonUtils.isAPIKeyValid(apiKey)) {
            CommonUtils.msgBox(
                    "Invalid API Key",
                    "Please enter a valid API Key!",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }
        Constants.API_KEY = apiKey;
        return true;
    }
}
