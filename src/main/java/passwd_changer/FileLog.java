package passwd_changer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Журнал в файл passwd_changer.log. Пароли в журнал не попадают. */
public final class FileLog {
    private final File file;

    public FileLog(File file) {
        this.file = file;
    }

    /** Добавляет строку с датой и возвращает её же для вывода на экран. */
    public synchronized String write(String text) {
        String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + text;
        try {
            Files.write(file.toPath(), (line + System.lineSeparator()).getBytes(Charset.forName("UTF-8")),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            return line + " (журнал недоступен: " + ex.getMessage() + ")";
        }
        return line;
    }
}
