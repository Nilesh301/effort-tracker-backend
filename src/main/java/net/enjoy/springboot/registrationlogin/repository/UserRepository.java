package net.enjoy.springboot.registrationlogin.repository;

import net.enjoy.springboot.registrationlogin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByName(String name);
    User findByResetToken(String token);




}