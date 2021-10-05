package sem3.security.demo.security.entities;

import javax.persistence.*;
import java.util.Objects;

@Entity
//@Table(name = "roles", schema = "swc3_webshop")
@Table(name = "roles")
public class Role {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(nullable = false)
//    private int id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Id
    private ERole name;

    public Role(ERole name) {
        this.name = name;
    }

    public Role() { }

//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }


    public ERole getName() {
        return name;
    }

    public void setName(ERole name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        return name == role.name;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
