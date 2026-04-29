package test;
import model.User;
import service.UserService;
import java.util.List;

public class DumpUsers {
    public static void main(String[] args) {
        try {
            UserService us = new UserService();
            List<User> users = us.recuperer();
            for (User u : users) {
                if (u.getRoles().contains("ADMIN") || !u.getPassword().startsWith("$")) {
                    System.out.println("Email: " + u.getEmail() + " | Pass: " + u.getPassword() + " | Roles: " + u.getRoles());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
