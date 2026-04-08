package com.demo.controller.admin;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.page;
import static com.demo.support.ITDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void userManageReturnsViewAndTotalPages() throws Exception {
        when(userService.findByUserID(any(Pageable.class)))
                .thenReturn(firstPage(Arrays.asList(user(), user()), 10, 21));

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void userAddReturnsView() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    @Test
    void userListReturnsDefaultPage() throws Exception {
        List<User> users = Arrays.asList(user(), user());
        when(userService.findByUserID(any(Pageable.class))).thenReturn(firstPage(users, 10, 12));

        mockMvc.perform(get("/userList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userID").value("user01"));
    }

    @Test
    void userListReturnsRequestedPage() throws Exception {
        User anotherUser = user();
        anotherUser.setId(12);
        anotherUser.setUserID("user12");
        when(userService.findByUserID(any(Pageable.class))).thenReturn(page(Arrays.asList(anotherUser), 1, 10, 12));

        mockMvc.perform(get("/userList.do").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userID").value("user12"));
    }

    @Test
    void userEditReturnsViewForExistingId() throws Exception {
        User existingUser = user();
        when(userService.findById(1)).thenReturn(existingUser);

        mockMvc.perform(get("/user_edit").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attribute("user", existingUser));
    }

    @Test
    void userEditReturnsBadRequestWhenIdIsMissing() throws Exception {
        assertThrows(Exception.class, () -> mockMvc.perform(get("/user_edit")).andReturn());
    }

    @Test
    void modifyUserUpdatesExistingUser() throws Exception {
        User existingUser = user();
        when(userService.findByUserID("user01")).thenReturn(existingUser);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(post("/modifyUser.do")
                .param("userID", "user02")
                .param("oldUserID", "user01")
                .param("userName", "Updated User")
                .param("password", "new-secret")
                .param("email", "updated@example.com")
                .param("phone", "13800000008"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).updateUser(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("user02", updatedUser.getUserID());
        assertEquals("Updated User", updatedUser.getUserName());
        assertEquals("new-secret", updatedUser.getPassword());
    }

    @Test
    void modifyUserFailsWhenOldUserIdDoesNotExist() throws Exception {
        when(userService.findByUserID("missing")).thenReturn(null);

        assertThrows(Exception.class, () -> mockMvc.perform(post("/modifyUser.do")
                .param("userID", "user02")
                .param("oldUserID", "missing")
                .param("userName", "Updated User")
                .param("password", "new-secret")
                .param("email", "updated@example.com")
                .param("phone", "13800000008")).andReturn());

        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void addUserCreatesUserAndRedirects() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(post("/addUser.do")
                .param("userID", "user03")
                .param("userName", "User Three")
                .param("password", "pw123")
                .param("email", "user03@example.com")
                .param("phone", "13800000009"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).create(userCaptor.capture());
        assertEquals("user03", userCaptor.getValue().getUserID());
        assertEquals("", userCaptor.getValue().getPicture());
    }

    @Test
    void checkUserIdReturnsTrueWhenValueIsAvailable() throws Exception {
        when(userService.countUserID("user99")).thenReturn(0);

        mockMvc.perform(post("/checkUserID.do").param("userID", "user99"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkUserIdReturnsFalseWhenValueAlreadyExists() throws Exception {
        when(userService.countUserID("user01")).thenReturn(1);

        mockMvc.perform(post("/checkUserID.do").param("userID", "user01"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void deleteUserReturnsTrue() throws Exception {
        mockMvc.perform(post("/delUser.do").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).delByID(1);
    }
}
