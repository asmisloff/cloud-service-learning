package NIO;

public enum Request {
    NONE(null), AUTH("./auth"), DOWNLOAD("./download"), UPLOAD("./upload"), INFO("./info");

    String value;

    private Request(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static void main(String[] args) {
        Request r = DOWNLOAD;
        System.out.printf("%s", r.value());
    }
}