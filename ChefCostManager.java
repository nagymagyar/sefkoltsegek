import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ChefCostManager extends JFrame {

    private static final String CSV_FILE_NAME = "chef_koltsegek_2025.csv";
    private static final String[] HEADER = {"id", "chefname", "datum", "kategoria", "osszeg", "megjegyzes"};
    private static final String[] CATEGORIES = {"Travel", "Ingredients", "Accommodation", "Equipment", "Other"};

    private final DefaultTableModel tableModel;
    private final List<ChefCostRecord> records;

    private final JTextField chefnameField = new JTextField(20);
    private final JTextField datumField = new JTextField(12);
    private final JComboBox<String> categoryCombo = new JComboBox<>(CATEGORIES);
    private final JTextField osszegField = new JTextField(10);
    private final JTextField megjegyzesField = new JTextField(30);

    public ChefCostManager() {
        super("Chef költségkezelő");
        records = new ArrayList<>();
        tableModel = new DefaultTableModel(HEADER, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadRecords();
        initUi();
        refreshTable();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(980, 580));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Új költség rögzítése"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Séf neve:"), gbc);
        gbc.gridx = 1;
        formPanel.add(chefnameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Dátum (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        formPanel.add(datumField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Költség típusa:"), gbc);
        gbc.gridx = 1;
        formPanel.add(categoryCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Összeg (EUR):"), gbc);
        gbc.gridx = 1;
        formPanel.add(osszegField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Megjegyzés:"), gbc);
        gbc.gridx = 1;
        formPanel.add(megjegyzesField, gbc);

        JButton addButton = new JButton("Hozzáadás");
        addButton.addActionListener(e -> addRecord());
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(addButton, gbc);

        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Költségadatok"));

        getContentPane().add(formPanel, BorderLayout.NORTH);
        getContentPane().add(tableScroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private void loadRecords() {
        Path csvPath = Paths.get(CSV_FILE_NAME);
        if (!Files.exists(csvPath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty() && !line.startsWith("id;")) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= HEADER.length) {
                        records.add(new ChefCostRecord(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            showError("Nem sikerült beolvasni a CSV fájlt: " + e.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (ChefCostRecord record : records) {
            tableModel.addRow(new Object[] {
                record.getId(),
                record.getChefname(),
                record.getDatum(),
                record.getKategoria(),
                record.getOsszeg(),
                record.getMegjegyzes()
            });
        }
    }

    private void addRecord() {
        String chefname = chefnameField.getText().trim();
        String datum = datumField.getText().trim();
        String kategoria = (String) categoryCombo.getSelectedItem();
        String osszeg = osszegField.getText().trim();
        String megjegyzes = megjegyzesField.getText().trim();

        if (chefname.isEmpty()) {
            showWarning("A séf neve kötelező.");
            return;
        }
        if (datum.isEmpty()) {
            showWarning("A dátum kötelező.");
            return;
        }
        if (osszeg.isEmpty()) {
            showWarning("Az összeg kötelező.");
            return;
        }

        try {
            Double.parseDouble(osszeg.replace(",", "."));
        } catch (NumberFormatException ex) {
            showWarning("Kérjük, érvényes számot adjon meg az összeghez.");
            return;
        }

        int newId = getNextId();
        ChefCostRecord record = new ChefCostRecord(Integer.toString(newId), chefname, datum, kategoria, osszeg, megjegyzes);
        records.add(record);
        saveRecords();
        refreshTable();
        clearForm();
    }

    private int getNextId() {
        int maxId = 0;
        for (ChefCostRecord record : records) {
            try {
                int value = Integer.parseInt(record.getId());
                if (value > maxId) {
                    maxId = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return maxId + 1;
    }

    private void saveRecords() {
        Path csvPath = Paths.get(CSV_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write(String.join(";", HEADER));
            writer.newLine();
            for (ChefCostRecord record : records) {
                writer.write(record.toCsvLine());
                writer.newLine();
            }
        } catch (IOException e) {
            showError("Nem sikerült menteni a CSV fájlt: " + e.getMessage());
        }
    }

    private void clearForm() {
        chefnameField.setText("");
        datumField.setText("");
        categoryCombo.setSelectedIndex(0);
        osszegField.setText("");
        megjegyzesField.setText("");
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Figyelem", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Hiba", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChefCostManager manager = new ChefCostManager();
            manager.setVisible(true);
        });
    }

    private static class ChefCostRecord {

        private final String id;
        private final String chefname;
        private final String datum;
        private final String kategoria;
        private final String osszeg;
        private final String megjegyzes;

        public ChefCostRecord(String id, String chefname, String datum, String kategoria, String osszeg, String megjegyzes) {
            this.id = id;
            this.chefname = chefname;
            this.datum = datum;
            this.kategoria = kategoria;
            this.osszeg = osszeg;
            this.megjegyzes = megjegyzes;
        }

        public String getId() {
            return id;
        }

        public String getChefname() {
            return chefname;
        }

        public String getDatum() {
            return datum;
        }

        public String getKategoria() {
            return kategoria;
        }

        public String getOsszeg() {
            return osszeg;
        }

        public String getMegjegyzes() {
            return megjegyzes;
        }

        public String toCsvLine() {
            return String.join(";", id, chefname, datum, kategoria, osszeg, megjegyzes);
        }
    }
}
