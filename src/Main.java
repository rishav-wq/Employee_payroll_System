import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

abstract class Employee {
    private String name;
    private int id;

    public Employee(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public abstract double calculateSalary();

    @Override
    public String toString() {
        return "Employee [name=" + name + ", id=" + id + ", salary=" + calculateSalary() + "]";
    }

    public void saveToDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/payroll_system", "root", "rishav12");
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO employees (id, name, type, monthlySalary, hoursWorked, hourlyRate) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            if (this instanceof FullTimeEmployee) {
                stmt.setString(3, "fulltime");
                stmt.setDouble(4, ((FullTimeEmployee) this).getMonthlySalary());
                stmt.setNull(5, java.sql.Types.INTEGER);
                stmt.setNull(6, java.sql.Types.DOUBLE);
            } else if (this instanceof PartTimeEmployee) {
                stmt.setString(3, "parttime");
                stmt.setNull(4, java.sql.Types.DOUBLE);
                stmt.setInt(5, ((PartTimeEmployee) this).getHoursWorked());
                stmt.setDouble(6, ((PartTimeEmployee) this).getHourlyRate());
            }
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            System.out.println("Error while saving to database: " + e.getMessage());
            e.printStackTrace();
        }

    }
}

class FullTimeEmployee extends Employee {
    private double monthlySalary;

    public FullTimeEmployee(String name, int id, double monthlySalary) {
        super(name, id);
        this.monthlySalary = monthlySalary;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }

    @Override
    public double calculateSalary() {
        return monthlySalary;
    }
}

class PartTimeEmployee extends Employee {
    private int hoursWorked;
    private double hourlyRate;

    public PartTimeEmployee(String name, int id, double hourlyRate, int hoursWorked) {
        super(name, id);
        this.hoursWorked = hoursWorked;
        this.hourlyRate = hourlyRate;
    }

    public int getHoursWorked() {
        return hoursWorked;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    @Override
    public double calculateSalary() {
        return hoursWorked * hourlyRate;
    }
}

class PayrollSystem {
    private ArrayList<Employee> employeeList;

    public PayrollSystem() {
        employeeList = new ArrayList<>();
        loadEmployeesFromDatabase();
    }

    private void loadEmployeesFromDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/payroll_system", "root", "rishav12");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM employees")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                if ("fulltime".equals(type)) {
                    double monthlySalary = rs.getDouble("monthlySalary");
                    employeeList.add(new FullTimeEmployee(name, id, monthlySalary));
                } else if ("parttime".equals(type)) {
                    int hoursWorked = rs.getInt("hoursWorked");
                    double hourlyRate = rs.getDouble("hourlyRate");
                    employeeList.add(new PartTimeEmployee(name, id, hourlyRate, hoursWorked));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEmployee(Employee employee) {
        employeeList.add(employee);
        try {
            employee.saveToDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeEmployee(int id) {
        Employee employeeToRemove = null;
        for (Employee employee : employeeList) {
            if (employee.getId() == id) {
                employeeToRemove = employee;
                break;
            }
        }
        if (employeeToRemove != null) {
            employeeList.remove(employeeToRemove);
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/payroll_system", "root", "rishav12");
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM employees WHERE id = ?")) {
                stmt.setInt(1, id);
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Rows affected by delete operation: " + rowsAffected);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void displayEmployees() {
        for (Employee employee : employeeList) {
            System.out.println(employee);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        PayrollSystem payrollSystem = new PayrollSystem();
        FullTimeEmployee emp1 = new FullTimeEmployee("John Smith", 1, 20000);
        PartTimeEmployee emp2 = new PartTimeEmployee("Ryan Do", 2, 15000, 2);

        payrollSystem.addEmployee(emp1);
        payrollSystem.addEmployee(emp2);

        System.out.println("Initial Employee Details: ");
        payrollSystem.displayEmployees();

        System.out.println("Removing Employee with ID 2");
        payrollSystem.removeEmployee(2);

        System.out.println("Remaining Employee Details: ");
        payrollSystem.displayEmployees();
    }
}
