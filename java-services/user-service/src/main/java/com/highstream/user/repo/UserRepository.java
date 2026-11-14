package com.highstream.user.repo;

import com.highstream.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 用户数据访问接口，由 Spring Data JPA 自动实现
public interface UserRepository extends JpaRepository<User, Long> {
}
