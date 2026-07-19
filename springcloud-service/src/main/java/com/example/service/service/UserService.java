package com.example.service.service;

import com.example.api.entity.User;

public interface UserService {

    User getById(Long id);

    void create(User user);

    void update(User user);

    void delete(Long id);
}