package test;
import model.User;
import service.UserService;

public class InsertUser {
    public static void main(String[] args) {
        try {
            UserService us = new UserService();
            User u = new User("TestUser", "TestUser", "testuser@gmail.com", "12345678", "user123");
            u.setRoles("[\"ROLE_USER\"]");
            us.ajouter(u);
            System.out.println("Normal user inserted: testuser@gmail.com / user123");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
