package passwd_changer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Главное окно: логин, пароли, список систем со статусом и журнал. */
public class MainFrame extends JFrame {

    private final FileLog log;
    private final SystemsModel model = new SystemsModel();
    private final JTextField login = new JTextField(20);
    private final JPasswordField oldPassword = new JPasswordField(20);
    private final JPasswordField newPassword = new JPasswordField(20);
    private final JPasswordField confirmPassword = new JPasswordField(20);
    private final JButton go = new JButton("Сменить пароль");
    private final JTextArea results = new JTextArea(8, 40);

    public MainFrame(File systemsFile, FileLog log) {
        super("passwd_changer 2.2");
        this.log = log;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        addRow(form, 0, "Логин", login);
        addRow(form, 1, "Старый пароль", oldPassword);
        addRow(form, 2, "Новый пароль", newPassword);
        addRow(form, 3, "Повтор нового", confirmPassword);

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Список систем"));
        tableScroll.setPreferredSize(new java.awt.Dimension(500, 130));

        results.setEditable(false);
        JScrollPane resultsScroll = new JScrollPane(results);

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.NORTH);
        north.add(tableScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.add(go);

        add(north, BorderLayout.NORTH);
        add(resultsScroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        go.addActionListener(e -> start());
        loadSystems(systemsFile);

        pack();
        setLocationRelativeTo(null);
    }

    private static void addRow(JPanel p, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(field, c);
    }

    private void loadSystems(File file) {
        try {
            model.setSystems(SystemsRepository.load(file));
        } catch (Exception ex) {
            print("Не удалось загрузить " + file.getAbsolutePath() + ": " + ex.getMessage());
        }
    }

    private void print(String text) {
        results.append(log.write(text) + System.lineSeparator());
    }

    private void start() {
        final String user = login.getText().trim();
        final String oldPw = new String(oldPassword.getPassword());
        final String newPw = new String(newPassword.getPassword());
        if (user.isEmpty() || oldPw.isEmpty() || newPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Необходимо заполнить логин, старый и новый пароли");
            return;
        }
        if (!newPw.equals(new String(confirmPassword.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Новый пароль и его повтор не совпадают");
            return;
        }
        final List<SapSystem> selected = model.selected();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Не выбрана ни одна система");
            return;
        }

        go.setEnabled(false);
        // обращение к SAP идёт вне потока интерфейса, результат по каждой системе публикуется сразу
        new SwingWorker<Void, String[]>() {
            @Override
            protected Void doInBackground() {
                for (SapSystem s : selected) {
                    String status;
                    try {
                        PasswordChanger.change(s, user, oldPw, newPw);
                        status = "OK";
                    } catch (Exception ex) {
                        status = "Ошибка: " + ex.getMessage();
                    }
                    publish(new String[]{s.toString(), status});
                }
                return null;
            }

            @Override
            protected void process(List<String[]> chunks) {
                for (String[] c : chunks) {
                    model.setStatus(c[0], c[1]);
                    print(user + " @ " + c[0] + ": " + c[1]);
                }
            }

            @Override
            protected void done() {
                go.setEnabled(true);
            }
        }.execute();
    }

    /** Модель таблицы: флажок, система, хост, статус. */
    private static final class SystemsModel extends AbstractTableModel {
        private final List<SapSystem> systems = new ArrayList<SapSystem>();
        private final List<Boolean> checked = new ArrayList<Boolean>();
        private final List<String> status = new ArrayList<String>();
        private final List<String> names = Arrays.asList("", "Система", "Хост", "Статус");

        void setSystems(List<SapSystem> list) {
            systems.clear();
            checked.clear();
            status.clear();
            for (SapSystem s : list) {
                systems.add(s);
                checked.add(Boolean.TRUE);
                status.add("");
            }
            fireTableDataChanged();
        }

        List<SapSystem> selected() {
            List<SapSystem> r = new ArrayList<SapSystem>();
            for (int i = 0; i < systems.size(); i++) {
                if (checked.get(i)) {
                    r.add(systems.get(i));
                }
            }
            return r;
        }

        void setStatus(String systemName, String text) {
            for (int i = 0; i < systems.size(); i++) {
                if (systems.get(i).toString().equals(systemName)) {
                    status.set(i, text);
                    fireTableRowsUpdated(i, i);
                }
            }
        }

        @Override public int getRowCount() { return systems.size(); }
        @Override public int getColumnCount() { return names.size(); }
        @Override public String getColumnName(int c) { return names.get(c); }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int r, int c) { return c == 0; }

        @Override
        public Object getValueAt(int r, int c) {
            switch (c) {
                case 0: return checked.get(r);
                case 1: return systems.get(r).toString();
                case 2: return systems.get(r).getHost();
                default: return status.get(r);
            }
        }

        @Override
        public void setValueAt(Object v, int r, int c) {
            if (c == 0) {
                checked.set(r, (Boolean) v);
            }
        }
    }
}
