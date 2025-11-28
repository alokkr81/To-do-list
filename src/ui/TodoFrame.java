package ui;

import dao.TaskDAO;
import model.Task;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

@SuppressWarnings("serial")
public class TodoFrame extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public TodoFrame() {
        setTitle("📝 To-Do App");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Styling
        getContentPane().setBackground(new Color(240, 248, 255));

        // Table
        model = new DefaultTableModel(new String[]{"ID", "Title", "Description", "Status"}, 0);
        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));

        // Buttons
        JButton addBtn = new JButton("➕ Add Task");
        JButton refreshBtn = new JButton("🔄 Refresh");

        addBtn.setBackground(new Color(50, 150, 250));
        addBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(34, 200, 120));
        refreshBtn.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(refreshBtn);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // Load data
        loadTasks();

        refreshBtn.addActionListener(e -> loadTasks());
        addBtn.addActionListener(e -> addTask());
    }

    private void loadTasks() {
        model.setRowCount(0);
        List<Task> list = TaskDAO.getAllTasks();
        for (Task t : list) {
            model.addRow(new Object[]{t.getId(), t.getTitle(), t.getDescription(), t.getStatus()});
        }
    }

    private void addTask() {
        String title = JOptionPane.showInputDialog("Enter Task Title:");
        String desc = JOptionPane.showInputDialog("Enter Description:");
        TaskDAO.addTask(new Task(0, title, desc, "Pending"));
        loadTasks();
    }
}
