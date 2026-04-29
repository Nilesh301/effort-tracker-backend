package net.enjoy.springboot.registrationlogin.repository;

import net.enjoy.springboot.registrationlogin.entity.Effort;
import net.enjoy.springboot.registrationlogin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EffortRepository extends JpaRepository<Effort,Long> {

    List<Effort> findByUser(User user);
    List<Effort> findByDateBetween(LocalDate from, LocalDate to);

    List<Effort> findByUserEmailAndDateBetween(String email, LocalDate from, LocalDate to);

    Page<Effort> findByDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<Effort> findByUserAndDateBetween(User user, LocalDate from, LocalDate to, Pageable pageable);

    Effort findByUserAndDate(User user, LocalDate date);

    Page<Effort> findByUser(User user, Pageable pageable);
    // For admin: find efforts of a specific user between two dates (no pagination)
    List<Effort> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // Alternatively, fetch by user ID (more efficient)
    @Query("SELECT e FROM Effort e WHERE e.user.id IN :userIds AND e.date BETWEEN :startDate AND :endDate")
    List<Effort> findByUserIdsAndDateBetween(@Param("userIds") List<Long> userIds,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);




}
