package pro.magisk.net;

public interface ResponseListener<T> {
    void onResponse(T response);
}
