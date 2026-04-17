package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import static com.demo.support.ITDataFactory.admin;
import static com.demo.support.ITDataFactory.user;
import static com.demo.support.ITDataFactory.userSession;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
class UserControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void signUpReturnsSignupView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void loginReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginCheckStoresRegularUserInSession() throws Exception {
        User loginUser = user();
        when(userService.checkLogin("user01", "secret")).thenReturn(loginUser);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user01")
                .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("/index"))
                .andExpect(request().sessionAttribute("user", loginUser));
    }

    @Test
    void loginCheckStoresAdminInSession() throws Exception {
        User loginAdmin = admin();
        when(userService.checkLogin("admin01", "secret")).thenReturn(loginAdmin);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "admin01")
                .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index"))
                .andExpect(request().sessionAttribute("admin", loginAdmin));
    }

    @Test
    void loginCheckReturnsFalseForInvalidCredentials() throws Exception {
        when(userService.checkLogin("user01", "bad")).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user01")
                .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void registerCreatesUserAndRedirectsToLogin() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(post("/register.do")
                .param("userID", "user02")
                .param("userName", "User Two")
                .param("password", "pw123")
                .param("email", "user02@example.com")
                .param("phone", "13800000002"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));

        verify(userService).create(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertEquals("user02", createdUser.getUserID());
        assertEquals("User Two", createdUser.getUserName());
        assertEquals("pw123", createdUser.getPassword());
        assertEquals("user02@example.com", createdUser.getEmail());
        assertEquals("13800000002", createdUser.getPhone());
        assertEquals("", createdUser.getPicture());
    }

    @Test
    void registerPropagatesCreateFailure() throws Exception {
        doThrow(new RuntimeException("duplicate user")).when(userService).create(any(User.class));

        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/register.do")
                .param("userID", "user02")
                .param("userName", "User Two")
                .param("password", "pw123")
                .param("email", "user02@example.com")
                .param("phone", "13800000002")).andReturn());
    }

    @Test
    void logoutRemovesUserFromSession() throws Exception {
        mockMvc.perform(get("/logout.do").session(userSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andExpect(request().sessionAttributeDoesNotExist("user"));
    }

    @Test
    void quitRemovesAdminFromSession() throws Exception {
        mockMvc.perform(get("/quit.do").session(com.demo.support.ITDataFactory.adminSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andExpect(request().sessionAttributeDoesNotExist("admin"));
    }

    @Test
    void updateUserKeepsPasswordAndPictureWhenNoNewValuesAreProvided() throws Exception {
        User existingUser = user();
        when(userService.findByUserID("user01")).thenReturn(existingUser);
        MockMultipartFile emptyPicture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(multipart("/updateUser.do")
                .file(emptyPicture)
                .session(userSession())
                .param("userName", "Updated User")
                .param("userID", "user01")
                .param("passwordNew", "")
                .param("email", "updated@example.com")
                .param("phone", "13800000003"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"))
                .andExpect(request().sessionAttribute("user",
                        hasProperty("userName", org.hamcrest.Matchers.equalTo("Updated User"))));

        verify(userService).updateUser(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("Updated User", updatedUser.getUserName());
        assertEquals("secret", updatedUser.getPassword());
        assertEquals("file/user/original.png", updatedUser.getPicture());
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals("13800000003", updatedUser.getPhone());
    }

    @Test
    void updateUserReplacesPasswordAndPictureWhenNewValuesAreProvided() throws Exception {
        User existingUser = user();
        when(userService.findByUserID("user01")).thenReturn(existingUser);
        MockMultipartFile picture = new MockMultipartFile("picture", "avatar.png", "image/png", "abc".getBytes());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(multipart("/updateUser.do")
                .file(picture)
                .session(userSession())
                .param("userName", "Updated User")
                .param("userID", "user01")
                .param("passwordNew", "new-secret")
                .param("email", "updated@example.com")
                .param("phone", "13800000003"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"))
                .andExpect(request().sessionAttribute("user", hasProperty("picture", startsWith("file/user/"))));

        verify(userService).updateUser(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("new-secret", updatedUser.getPassword());
        assertNotNull(updatedUser.getPicture());
        assertTrue(updatedUser.getPicture().startsWith("file/user/"));
    }

    @Test
    void updateUserFailsWhenTargetUserDoesNotExist() throws Exception {
        when(userService.findByUserID("missing")).thenReturn(null);
        MockMultipartFile emptyPicture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        assertThrows(NestedServletException.class, () -> mockMvc.perform(multipart("/updateUser.do")
                .file(emptyPicture)
                .session(userSession())
                .param("userName", "Updated User")
                .param("userID", "missing")
                .param("passwordNew", "")
                .param("email", "updated@example.com")
                .param("phone", "13800000003")).andReturn());

        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void checkPasswordReturnsTrueForMatchingPassword() throws Exception {
        when(userService.findByUserID("user01")).thenReturn(user());

        mockMvc.perform(get("/checkPassword.do")
                .param("userID", "user01")
                .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkPasswordReturnsFalseForWrongPassword() throws Exception {
        when(userService.findByUserID("user01")).thenReturn(user());

        mockMvc.perform(get("/checkPassword.do")
                .param("userID", "user01")
                .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void userInfoReturnsUserInfoView() throws Exception {
        mockMvc.perform(get("/user_info").session(userSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }
}
