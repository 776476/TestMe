package com.demo.service.impl;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("findByUserID(String): 应根据 userID 查询用户")
    void findByUserID_shouldDelegateByStringId() {
        User expected = buildUser(1, "u001", 0);
        when(userDao.findByUserID("u001")).thenReturn(expected);

        User actual = userService.findByUserID("u001");

        assertSame(expected, actual);
        verify(userDao).findByUserID("u001");
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("findById: 应根据主键 id 查询用户")
    void findById_shouldDelegateToDao() {
        User expected = buildUser(2, "u002", 0);
        when(userDao.findById(2)).thenReturn(expected);

        User actual = userService.findById(2);

        assertSame(expected, actual);
        verify(userDao).findById(2);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("findByUserID(Pageable): 应只查询 isadmin=0 的普通用户")
    void findByUserID_shouldQueryNormalUsersPage() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<User> expectedPage = new PageImpl<>(Collections.singletonList(buildUser(3, "u003", 0)));
        when(userDao.findAllByIsadmin(0, pageable)).thenReturn(expectedPage);

        Page<User> actual = userService.findByUserID(pageable);

        assertSame(expectedPage, actual);
        verify(userDao).findAllByIsadmin(0, pageable);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("checkLogin: 应按 userID 和 password 查询用户")
    void checkLogin_shouldDelegateToDao() {
        User expected = buildUser(4, "u004", 0);
        when(userDao.findByUserIDAndPassword("u004", "secret")).thenReturn(expected);

        User actual = userService.checkLogin("u004", "secret");

        assertSame(expected, actual);
        verify(userDao).findByUserIDAndPassword("u004", "secret");
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("create: 应先保存用户，再返回当前用户总数")
    void create_shouldSaveUserThenReturnUserCount() {
        User user = buildUser(0, "u005", 0);
        List<User> allUsers = Arrays.asList(
                buildUser(1, "u001", 0),
                buildUser(2, "u002", 0),
                buildUser(3, "u003", 0)
        );
        when(userDao.findAll()).thenReturn(allUsers);

        int result = userService.create(user);

        assertEquals(3, result);
        InOrder inOrder = inOrder(userDao);
        inOrder.verify(userDao).save(user);
        inOrder.verify(userDao).findAll();
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("delByID: 应根据 id 删除用户")
    void delByID_shouldDelegateDelete() {
        userService.delByID(6);

        verify(userDao).deleteById(6);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("updateUser: 应保存修改后的用户对象")
    void updateUser_shouldSaveUser() {
        User user = buildUser(7, "u007", 0);

        userService.updateUser(user);

        verify(userDao).save(user);
        verifyNoMoreInteractions(userDao);
    }

    @Test
    @DisplayName("countUserID: 应返回指定 userID 的计数结果")
    void countUserID_shouldDelegateToDao() {
        when(userDao.countByUserID("u008")).thenReturn(2);

        int result = userService.countUserID("u008");

        assertEquals(2, result);
        verify(userDao).countByUserID("u008");
        verifyNoMoreInteractions(userDao);
    }

    private User buildUser(int id, String userId, int isadmin) {
        User user = new User();
        user.setId(id);
        user.setUserID(userId);
        user.setUserName("User-" + userId);
        user.setPassword("secret");
        user.setEmail(userId + "@example.com");
        user.setPhone("13800000000");
        user.setIsadmin(isadmin);
        user.setPicture("file/user/" + userId + ".png");
        return user;
    }
}
