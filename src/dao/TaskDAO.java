package dao;

import db.DBConnection;
import model.Task;
import java.sql.*;
import java.util.*;

public class TaskDAO {

    // ✅ Add Task
    public static void addTask(Task task) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO tasks (title, description, status) VALUES (?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Get All Tasks
    public static List<Task> getAllTasks() {
        List<Task> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return list; // fail safe
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tasks");
            while (rs.next()) {
                list.add(new Task(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Update Task (edit title/desc/status)
    public static void updateTask(Task task) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE tasks SET title=?, description=?, status=? WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus());
            ps.setInt(4, task.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Delete Task
    public static void deleteTask(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM tasks WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
