package project2.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admins")
public class Admin {
    @Id
    private String admin;
    private String password;

    public String getAdmin() { return admin; }
    public void setAdmin(String admin) { this.admin = admin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
