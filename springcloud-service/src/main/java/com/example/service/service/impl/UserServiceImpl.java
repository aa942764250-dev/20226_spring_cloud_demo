package com.example.service.service.impl;

import com.example.api.entity.User;
import com.example.service.dao.UserDao;
import com.example.service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Override
    public User getById(Long id) {
        return userDao.selectById(id);
    }

    @Override
    public void create(User user) {
        userDao.insert(user);
    }

    @Override
    public void update(User user) {
        userDao.updateById(user);
    }

    @Override
    public void delete(Long id) {
        userDao.deleteById(id);
    }
}