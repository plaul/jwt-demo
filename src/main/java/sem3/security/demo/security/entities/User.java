package sem3.security.demo.security.entities;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;


@Entity
//@Table(name = "users", schema = "swc3_webshop")
@Table(name = "users")
public class User {


    @Basic
    @Column(nullable = false, length = 50)
    private String email;

    @Basic
    @Column(nullable = false, length = 72)  //72 == Max length of a bcrypt encoded password
    private String password;

    @Id
    private String username;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(	name = "user_roles",
            joinColumns = @JoinColumn(name = "user_username"),
            inverseJoinColumns = @JoinColumn(name = "role_name"))
    private Set<Role> roles = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(){}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role){
        roles.add(role);
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }


}
