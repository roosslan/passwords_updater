package kennw.andern;

/** Описание SAP-системы: SID, клиент, номер системы и хост сервера приложений. */
public final class SapSystem {
    private final String sid;
    private final String client;
    private final String sysnr;
    private final String host;

    public SapSystem(String sid, String client, String sysnr, String host) {
        this.sid = require(sid, "sid");
        this.client = require(client, "client");
        this.sysnr = require(sysnr, "sysnr");
        this.host = require(host, "host");
        if (!this.client.matches("\\d{3}")) {
            throw new IllegalArgumentException("Клиент должен состоять из 3 цифр: " + client);
        }
        if (!this.sysnr.matches("\\d{2}")) {
            throw new IllegalArgumentException("Номер системы должен состоять из 2 цифр: " + sysnr);
        }
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Не задан атрибут " + name);
        }
        return value.trim();
    }

    public String getSid() { return sid; }
    public String getClient() { return client; }
    public String getSysnr() { return sysnr; }
    public String getHost() { return host; }

    /** Краткое имя вида PER300/00. */
    @Override
    public String toString() {
        return sid + client + "/" + sysnr;
    }
}
