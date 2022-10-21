package kennw.andern;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Смена пароля одним вызовом SUSR_USER_CHANGE_PASSWORD_RFC: вход под старым паролем,
 * установка нового. Временные пароли и BAPI_USER_CHANGE не используются.
 */
public final class PasswordChanger {

    /** Провайдер параметров: для каждого вызова регистрируется уникальное имя назначения. */
    private static final class Provider implements DestinationDataProvider {
        private final Map<String, Properties> destinations = new ConcurrentHashMap<String, Properties>();

        @Override
        public Properties getDestinationProperties(String name) {
            Properties p = destinations.get(name);
            if (p == null) {
                throw new IllegalStateException("Неизвестное назначение: " + name);
            }
            return p;
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener listener) {
            // события не поддерживаются
        }
    }

    private static final Provider PROVIDER = new Provider();

    static {
        // регистрировать провайдер можно только один раз за время работы JVM
        Environment.registerDestinationDataProvider(PROVIDER);
    }

    private PasswordChanger() {
    }

    /**
     * @throws JCoException            если соединение или вызов завершились ошибкой
     * @throws PasswordChangeException если SAP вернул сообщение об ошибке
     */
    public static void change(SapSystem system, String login, String oldPassword, String newPassword)
            throws JCoException {
        String name = system + "#" + UUID.randomUUID();
        Properties p = new Properties();
        p.setProperty(DestinationDataProvider.JCO_ASHOST, system.getHost());
        p.setProperty(DestinationDataProvider.JCO_SYSNR, system.getSysnr());
        p.setProperty(DestinationDataProvider.JCO_CLIENT, system.getClient());
        p.setProperty(DestinationDataProvider.JCO_USER, login);
        p.setProperty(DestinationDataProvider.JCO_PASSWD, oldPassword);
        p.setProperty(DestinationDataProvider.JCO_LANG, "EN");
        // без пула: учётные данные у каждого вызова свои
        p.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, "0");
        p.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, "1");

        PROVIDER.destinations.put(name, p);
        try {
            JCoDestination destination = JCoDestinationManager.getDestination(name);
            JCoFunction function = destination.getRepository().getFunction("SUSR_USER_CHANGE_PASSWORD_RFC");
            if (function == null) {
                throw new PasswordChangeException("Функция SUSR_USER_CHANGE_PASSWORD_RFC не найдена");
            }
            function.getImportParameterList().setValue("BNAME", login);
            function.getImportParameterList().setValue("PASSWORD", oldPassword);
            function.getImportParameterList().setValue("NEW_PASSWORD", newPassword);
            function.execute(destination);

            checkReturn(function);
        } finally {
            PROVIDER.destinations.remove(name);
        }
    }

    /** Таблица RETURN (BAPIRET2): типы E и A означают ошибку. */
    private static void checkReturn(JCoFunction function) {
        if (function.getTableParameterList() == null) {
            return;
        }
        JCoTable ret = function.getTableParameterList().getTable("RETURN");
        for (int i = 0; i < ret.getNumRows(); i++) {
            ret.setRow(i);
            String type = ret.getString("TYPE");
            if ("E".equals(type) || "A".equals(type)) {
                throw new PasswordChangeException(ret.getString("MESSAGE"));
            }
        }
    }

    /** Ошибка, о которой сообщил сам SAP. */
    public static final class PasswordChangeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PasswordChangeException(String message) {
            super(message);
        }
    }
}
