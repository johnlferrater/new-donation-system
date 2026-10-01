package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.entity.User;

public interface UserRepository extends JpaRepository<User, String> {}
