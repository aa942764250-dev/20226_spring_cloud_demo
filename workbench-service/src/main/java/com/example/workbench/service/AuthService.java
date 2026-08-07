package com.example.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.workbench.dto.LoginRequest;
import com.example.workbench.entity.WbMenu;
import com.example.workbench.entity.WbUser;
import com.example.workbench.mapper.WbUserMapper;
import com.example.workbench.util.JwtUtil;
import com.example.workbench.vo.LoginVO;
import com.example.workbench.vo.MenuVO;
import com.example.workbench.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final WbUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginVO login(LoginRequest request) {
        WbUser user = userMapper.selectOne(
                new LambdaQueryWrapper<WbUser>()
                        .eq(WbUser::getUsername, request.getUsername())
                        .eq(WbUser::getStatus, 1)
        );
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        List<MenuVO> menus = getMenusByUserId(user.getId());

        return LoginVO.builder()
                .token(token)
                .user(buildUserVO(user))
                .menus(menus)
                .build();
    }

    public LoginVO getUserInfo(Long userId) {
        WbUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        List<MenuVO> menus = getMenusByUserId(userId);
        return LoginVO.builder()
                .user(buildUserVO(user))
                .menus(menus)
                .build();
    }

    public List<MenuVO> getMenusByUserId(Long userId) {
        List<WbMenu> menuList = userMapper.selectMenusByUserId(userId);
        return menuList.stream()
                .map(m -> MenuVO.builder()
                        .id(m.getId())
                        .parentId(m.getParentId())
                        .name(m.getMenuName())
                        .path(m.getMenuPath())
                        .icon(m.getMenuIcon())
                        .type(m.getMenuType())
                        .sortOrder(m.getSortOrder())
                        .sectionName(m.getSectionName())
                        .build())
                .collect(Collectors.toList());
    }

    private UserVO buildUserVO(WbUser user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }
}
