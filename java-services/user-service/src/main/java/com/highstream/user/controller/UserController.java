package com.highstream.user.controller;

import com.highstream.user.entity.User;
import com.highstream.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 用户 REST 接口：创建用户、按 id 查询
@RestController
@RequestMapping("/users")
public class UserController {

    public record CreateUserRequest(String name, String avatar) {
    }

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 创建用户，name 为空时返回 400
    @PostMapping
    public User create(@RequestBody CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setAvatar(request.avatar());
        return userRepository.save(user);
    }

    // 按 id 查询用户，不存在时返回 404
    @GetMapping("/{id}")
    public User get(@PathVariable long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }
}
