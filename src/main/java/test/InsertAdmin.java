package test;
import model.User;
import service.UserService;

public class InsertAdmin {
    public static void main(String[] args) {
        try {
            UserService us = new UserService();
            User u = new User("TestAdmin", "TestAdmin", "testadmin@gmail.com", "12345678", "admin123");
            u.setRoles("[\"ROLE_ADMIN\"]");
            us.ajouter(u);
            System.out.println("Admin user inserted: testadmin@gmail.com / admin123");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
