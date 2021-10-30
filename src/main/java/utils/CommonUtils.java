package utils;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
    public static String input(String title, String msg, int msgType) {
        return (String) JOptionPane.showInputDialog(
                new JFrame(),
                msg,
                title,
                msgType,
                null,
                null,
                ""
        );
    }
    public static void msgBox(String title, String msg, int msgType) {
         JOptionPane.showMessageDialog(
                new JFrame(),
                msg,
                title,
                msgType
        );
    }

    public static boolean isAPIKeyValid(String apiKey) {
        //TODO: Check with server whether given API key is valid or not!
        if (apiKey == null) return false;

        return true;
    }

    public static synchronized void logMessage(JTextArea ta, String msg, String src) {
        String m = "[";
        m += new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS ").format(new Date());
        m += " " + src + "]: ";
        m += msg;
        m += '\n';
        ta.append(m);
        ta.setCaretPosition(ta.getDocument().getLength());
    }
}
