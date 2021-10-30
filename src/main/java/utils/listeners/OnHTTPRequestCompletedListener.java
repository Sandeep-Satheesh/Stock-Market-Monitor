package utils.listeners;

public interface OnHTTPRequestCompletedListener {
    void onRequestFailed(int errorCode, String response);
    void onRequestSuccess(int status, String response);
}
