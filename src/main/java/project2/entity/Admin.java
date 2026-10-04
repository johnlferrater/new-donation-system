package project2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import project2.validation.OnCreate;

@Entity
@Table(name = "admins")
public class Admin {
    @Id
    @NotBlank(message = "Admin name is required", groups = OnCreate.class)
    private String admin;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required", groups = OnCreate.class)
    private String password;

    public String getAdmin() { return admin; }
    public void setAdmin(String admin) { this.admin = admin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
