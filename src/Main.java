import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;
import javax.swing.RowFilter;
import javax.swing.event.DocumentListener;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.List;

import dao.TaskDAO;
import model.Task;

public class Main extends JFrame {

    private static final long serialVersionUID = 1L;

    // Color palette (CSS-like variables)
    private static final Color BG = new Color(245, 247, 250);           // app background
    private static final Color CARD = new Color(255, 255, 255);         // panel/card bg
    private static final Color PRIMARY = new Color(30, 116, 230);       // primary blue
    private static final Color SUCCESS = new Color(46, 125, 50);        // green (kept for theme references)
    private static final Color DANGER  = new Color(211, 47, 47);        // red (kept for theme references)
    private static final Color ACCENT  = new Color(100, 116, 139);      // neutral
    private static final Font   HEAD_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font   UI_FONT   = new Font("Segoe UI", Font.PLAIN, 15);

    private JPanel contentPane;
    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private String statusFilter = "All";
    private JButton filterAllBtn;
    private JButton filterPendingBtn;
    private JButton filterCompletedBtn;
    private JLabel countsLabel;
    private JButton editBtn;
    private JButton completeBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;
    private JButton exportBtn;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Main frame = new Main();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Main() {
        setTitle("Modern To-Do (Swing) — No FlatLaf");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        // Root
        contentPane = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = new Color(241, 245, 255);
                Color c2 = new Color(236, 252, 203);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        contentPane.setBackground(BG);
        contentPane.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(contentPane);

        // Top card: title + subtitle + search
        JPanel topCard = buildCard(new BorderLayout(12, 12));
        topCard.setPreferredSize(new Dimension(0, 140));
        contentPane.add(topCard, BorderLayout.NORTH);

        JPanel titleWrap = new JPanel(new GridLayout(0,1));
        titleWrap.setOpaque(false);
        JLabel lblTitle = new JLabel("My Tasks");
        lblTitle.setFont(HEAD_FONT.deriveFont(28f));
        lblTitle.setForeground(PRIMARY);
        titleWrap.add(lblTitle);
        topCard.add(titleWrap, BorderLayout.WEST);

        // Search + Add row
        JPanel controlsRow = new JPanel(new BorderLayout(8, 8));
        controlsRow.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(UI_FONT);
        searchField.setToolTipText("Search: title, description, or status (Enter clears)");
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220,220,220), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        controlsRow.add(searchField, BorderLayout.CENTER);

        JButton addBtn = new RoundedButton("➕ Add Task");
        styleActionButton(addBtn, PRIMARY);
        controlsRow.add(addBtn, BorderLayout.EAST);

        topCard.add(controlsRow, BorderLayout.SOUTH);

        // Sidebar filters
        JPanel sidebar = buildCard(new GridLayout(0,1,8,8));
        sidebar.setPreferredSize(new Dimension(200, 0));
        JLabel fl = new JLabel("Filters"); fl.setFont(UI_FONT.deriveFont(Font.BOLD, 14f)); fl.setForeground(ACCENT);
        sidebar.add(fl);

        filterAllBtn = new RoundedButton("🌐 All");
        filterPendingBtn = new RoundedButton("🕒 Pending");
        filterCompletedBtn = new RoundedButton("✅ Completed");
        styleActionButton(filterAllBtn, new Color(99, 102, 241));
        styleActionButton(filterPendingBtn, new Color(234, 179, 8));
        styleActionButton(filterCompletedBtn, new Color(34, 197, 94));
        filterAllBtn.setPreferredSize(new Dimension(160, 38));
        filterPendingBtn.setPreferredSize(new Dimension(160, 38));
        filterCompletedBtn.setPreferredSize(new Dimension(160, 38));
        filterAllBtn.setFont(UI_FONT.deriveFont(Font.BOLD, 14f));
        filterPendingBtn.setFont(UI_FONT.deriveFont(Font.BOLD, 14f));
        filterCompletedBtn.setFont(UI_FONT.deriveFont(Font.BOLD, 14f));
        sidebar.add(filterAllBtn);
        sidebar.add(filterPendingBtn);
        sidebar.add(filterCompletedBtn);

        contentPane.add(sidebar, BorderLayout.WEST);

        // Center: table inside card
        JPanel centerCard = buildCard(new BorderLayout());
        contentPane.add(centerCard, BorderLayout.CENTER);

        model = new DefaultTableModel(new String[] { "ID", "Title", "Description", "Status" }, 0) {
            private static final long serialVersionUID = 1L;
            // Make ID column non-editable in table
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE);
                }
                return c;
            }
        };
        table.setRowHeight(40);
        table.setFont(UI_FONT.deriveFont(15f));
        table.setSelectionBackground(PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(UI_FONT.deriveFont(Font.BOLD, 16f));
        table.getTableHeader().setBackground(new Color(250,250,250));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        // Chip-like colored status
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        centerCard.add(scroll, BorderLayout.CENTER);

        // Footer status bar
        JPanel statusBar = buildCard(new BorderLayout());
        countsLabel = new JLabel(" ");
        countsLabel.setFont(UI_FONT);
        countsLabel.setForeground(ACCENT);
        statusBar.add(countsLabel, BorderLayout.WEST);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        // Bottom: action buttons
        JPanel bottomCard = buildCard(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomCard.setPreferredSize(new Dimension(0, 84));
        centerCard.add(bottomCard, BorderLayout.SOUTH);

        editBtn = new RoundedButton("✏️ Edit");
        styleActionButton(editBtn, new Color(14, 165, 233));
        completeBtn = new RoundedButton("✔ Complete");
        styleActionButton(completeBtn, SUCCESS);
        deleteBtn = new RoundedButton("🗑 Delete");
        styleActionButton(deleteBtn, DANGER);
        refreshBtn = new RoundedButton("🔄 Refresh");
        styleActionButton(refreshBtn, new Color(107, 114, 128));
        exportBtn = new RoundedButton("⬇ Export CSV");
        styleActionButton(exportBtn, new Color(147, 51, 234));
        editBtn.setPreferredSize(new Dimension(140, 44));
        completeBtn.setPreferredSize(new Dimension(140, 44));
        deleteBtn.setPreferredSize(new Dimension(140, 44));
        refreshBtn.setPreferredSize(new Dimension(140, 44));
        exportBtn.setPreferredSize(new Dimension(140, 44));

        bottomCard.add(editBtn);
        bottomCard.add(completeBtn);
        bottomCard.add(deleteBtn);
        bottomCard.add(exportBtn);
        bottomCard.add(refreshBtn);

        // Wire actions
        addBtn.addActionListener(e -> { if (e != null) e.getSource(); addTask(); });
        refreshBtn.addActionListener(e -> { if (e != null) e.getSource(); loadTasks(); });
        deleteBtn.addActionListener(e -> { if (e != null) e.getSource(); deleteTask(); });
        editBtn.addActionListener(e -> { if (e != null) e.getSource(); editTask(); });
        completeBtn.addActionListener(e -> { if (e != null) e.getSource(); markCompleted(); });
        exportBtn.addActionListener(e -> { if (e != null) e.getSource(); exportCSV(); });

        // Filter button actions
        filterAllBtn.addActionListener(e -> { if (e != null) e.getSource(); statusFilter = "All"; applyFilter(); });
        filterPendingBtn.addActionListener(e -> { if (e != null) e.getSource(); statusFilter = "Pending"; applyFilter(); });
        filterCompletedBtn.addActionListener(e -> { if (e != null) e.getSource(); statusFilter = "Completed"; applyFilter(); });

        // Search filter
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { if (e != null) e.getDocument(); applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { if (e != null) e.getDocument(); applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { if (e != null) e.getDocument(); applyFilter(); }
        });
        searchField.addActionListener(e -> { if (e != null) e.getSource(); if (searchField.getText().trim().isEmpty()) applyFilter(); });

        // Selection-aware button states
        table.getSelectionModel().addListSelectionListener(e -> { if (e != null) e.getValueIsAdjusting(); updateActionButtons(); });
        updateActionButtons();

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editTask();
                }
            }
        });

        // Keyboard shortcuts
        setupKeyBindings();

        // Load initial tasks
        loadTasks();
    }

    // --- UI helpers ----------------------------------------------------

    private JPanel buildCard(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 8, 8, 8),
            BorderFactory.createLineBorder(new Color(230,230,230), 1)
        ));
        return p;
    }

    private void styleActionButton(JButton btn, Color bg) {
        btn.setFont(UI_FONT.deriveFont(Font.BOLD));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // simple hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(btn.getBackground().darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
    }

    // --- Data actions -------------------------------------------------

    private void loadTasks() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            List<Task> tasks = TaskDAO.getAllTasks(); // static in your DAO earlier
            for (Task t : tasks) {
                model.addRow(new Object[] { t.getId(), t.getTitle(), t.getDescription(), t.getStatus() });
            }
            updateCountsLabel();
        });
    }

    private void applyFilter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            // still apply status filter if not All
            if ("All".equals(statusFilter)) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        Object s = entry.getValue(3);
                        return s != null && s.toString().equalsIgnoreCase(statusFilter);
                    }
                });
            }
            updateCountsLabel();
            return;
        }
        String q = text.trim().toLowerCase();
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean matchesSearch = false;
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object v = entry.getValue(i);
                    if (v != null && v.toString().toLowerCase().contains(q)) { matchesSearch = true; break; }
                }
                if (!matchesSearch) return false;
                if ("All".equals(statusFilter)) return true;
                Object s = entry.getValue(3);
                return s != null && s.toString().equalsIgnoreCase(statusFilter);
            }
        });
        updateCountsLabel();
    }

    private void updateCountsLabel() {
        int total = model.getRowCount();
        int completed = 0;
        int pending = 0;
        for (int i = 0; i < total; i++) {
            Object s = model.getValueAt(i, 3);
            if (s != null && s.toString().equalsIgnoreCase("Completed")) completed++;
        }
        pending = total - completed;
        String filterNote = "All".equals(statusFilter) ? "All" : statusFilter;
        countsLabel.setText("Total: " + total + "   Pending: " + pending + "   Completed: " + completed + "   | Filter: " + filterNote);
    }

    private void updateActionButtons() {
        int count = table.getSelectedRowCount();
        boolean hasOne = count == 1;
        boolean hasAny = count > 0;
        editBtn.setEnabled(hasOne);
        completeBtn.setEnabled(hasAny);
        deleteBtn.setEnabled(hasAny);
    }

    private void setupKeyBindings() {
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRows");
        am.put("deleteRows", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (e == null) return; deleteTask(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "editRow");
        am.put("editRow", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (e == null) return; editTask(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "completeRows");
        am.put("completeRows", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (e == null) return; markCompleted(); }
        });

        // Focus search with Ctrl+F
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        contentPane.getActionMap().put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (e == null) return; searchField.requestFocusInWindow(); }
        });
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export tasks to CSV");
        chooser.setSelectedFile(new File("tasks.csv"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // header
            pw.println("ID,Title,Description,Status");
            for (int i = 0; i < model.getRowCount(); i++) {
                Object id = model.getValueAt(i, 0);
                Object title = escapeCsv(model.getValueAt(i, 1));
                Object desc = escapeCsv(model.getValueAt(i, 2));
                Object status = escapeCsv(model.getValueAt(i, 3));
                pw.println(id + "," + title + "," + desc + "," + status);
            }
            JOptionPane.showMessageDialog(this, "Exported to " + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(Object value) {
        String s = value == null ? "" : value.toString();
        boolean needQuotes = s.contains(",") || s.contains("\n") || s.contains("\"");
        s = s.replace("\"", "\"\"");
        return needQuotes ? "\"" + s + "\"" : s;
    }

    // Colored status renderer
    private class StatusRenderer extends JLabel implements TableCellRenderer {
        StatusRenderer() { setOpaque(true); setHorizontalAlignment(SwingConstants.CENTER); setFont(UI_FONT); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String status = value == null ? "" : value.toString();
            setText(status);
            Color bg = CARD;
            Color fg = ACCENT;
            if ("Completed".equalsIgnoreCase(status)) { bg = new Color(223, 240, 216); fg = new Color(46, 125, 50); }
            else if ("Pending".equalsIgnoreCase(status)) { bg = new Color(255, 243, 205); fg = new Color(204, 142, 0); }
            setBackground(isSelected ? table.getSelectionBackground() : bg);
            setForeground(isSelected ? table.getSelectionForeground() : fg);
            setBorder(BorderFactory.createLineBorder(new Color(0,0,0,20)));
            return this;
        }
    }

    private void addTask() {
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> statusBox = new JComboBox<>(new String[] { "Pending", "Completed" });

        JPanel form = new JPanel(new GridLayout(0,1,6,6));
        form.add(new JLabel("Title:"));
        form.add(titleField);
        form.add(new JLabel("Description:"));
        form.add(descField);
        form.add(new JLabel("Status:"));
        form.add(statusBox);

        int res = JOptionPane.showConfirmDialog(this, form, "Add Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String desc = descField.getText().trim();
            String status = (String) statusBox.getSelectedItem();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title is required", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Task t = new Task(0, title, desc, status);
            TaskDAO.addTask(t);
            loadTasks();
        }
    }

    private void deleteTask() {
        int[] sel = table.getSelectedRows();
        if (sel.length == 0) { JOptionPane.showMessageDialog(this, "Select task(s) to delete."); return; }
        int ans = JOptionPane.showConfirmDialog(this, "Delete " + sel.length + " selected task(s)?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        for (int viewRow : sel) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            int id = (int) model.getValueAt(modelRow, 0);
            TaskDAO.deleteTask(id);
        }
        loadTasks();
    }

    private void editTask() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a task to edit."); return; }
        int id = (int) model.getValueAt(r, 0);
        String oldTitle = (String) model.getValueAt(r, 1);
        String oldDesc  = (String) model.getValueAt(r, 2);
        String oldStatus= (String) model.getValueAt(r, 3);

        JTextField titleField = new JTextField(oldTitle);
        JTextField descField  = new JTextField(oldDesc);
        String[] statuses = { "Pending", "Completed" };
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        statusBox.setSelectedItem(oldStatus);

        JPanel form = new JPanel(new GridLayout(0,1,6,6));
        form.add(new JLabel("Title:")); form.add(titleField);
        form.add(new JLabel("Description:")); form.add(descField);
        form.add(new JLabel("Status:")); form.add(statusBox);

        int res = JOptionPane.showConfirmDialog(this, form, "Edit Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String newTitle = titleField.getText().trim();
            String newDesc  = descField.getText().trim();
            String newStatus = (String) statusBox.getSelectedItem();
            if (newTitle.isEmpty()) { JOptionPane.showMessageDialog(this, "Title cannot be empty."); return; }
            Task updated = new Task(id, newTitle, newDesc, newStatus);
            TaskDAO.updateTask(updated);
            loadTasks();
        }
    }

    private void markCompleted() {
        int[] sel = table.getSelectedRows();
        if (sel.length == 0) { JOptionPane.showMessageDialog(this, "Select task(s) to mark completed."); return; }
        for (int viewRow : sel) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            int id = (int) model.getValueAt(modelRow, 0);
            String title = (String) model.getValueAt(modelRow, 1);
            String desc  = (String) model.getValueAt(modelRow, 2);
            Task updated = new Task(id, title, desc, "Completed");
            TaskDAO.updateTask(updated);
        }
        loadTasks();
    }

    // --- RoundedButton inner class (simple "CSS-like" rounded button) ----

    static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private int arc = 18;

        public RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // background
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            // text
            FontMetrics fm = g2.getFontMetrics();
            Rectangle r = new Rectangle(0, 0, getWidth(), getHeight());
            String text = getText();
            g2.setColor(getForeground());
            int tx = (r.width - fm.stringWidth(text)) / 2;
            int ty = (r.height - fm.getHeight()) / 2 + fm.getAscent();
            g2.setFont(getFont());
            g2.drawString(text, tx, ty);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Insets getInsets() {
            return new Insets(6, 14, 6, 14);
        }
    }
}
