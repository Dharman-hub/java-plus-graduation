package ru.practicum.ewm.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.model.User;

public interface UserRepository extends JpaRepository<User, Long>,
        QuerydslPredicateExecutor<User> {

    boolean existsByEmail(String email);
}