package com.demo.controller;

import com.demo.controller.user.UserController;
import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import static com.demo.support.ITDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 组长负责模块：登录 / 注册 / 权限 集成测试
 * 聚焦于成员B未覆盖的边界场景、权限验证和安全缺陷验证
 */
@WebMvcTest(UserController.class)
class LoginAuthIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // ==================== 一、登录边界与异常场景 ====================

    @Nested
    @DisplayName("登录边界场景")
    class LoginEdgeCases {

        @Test
        @DisplayName("LA-001: 用户名和密码均为空字符串时应返回 false")
        void loginWithEmptyCredentials() throws Exception {
            when(userService.checkLogin("", "")).thenReturn(null);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "")
                            .param("password", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(userService).checkLogin("", "");
        }

        @Test
        @DisplayName("LA-002: 仅用户名为空时应返回 false")
        void loginWithEmptyUserID() throws Exception {
            when(userService.checkLogin("", "secret")).thenReturn(null);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "")
                            .param("password", "secret"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("LA-003: 仅密码为空时应返回 false")
        void loginWithEmptyPassword() throws Exception {
            when(userService.checkLogin("user01", "")).thenReturn(null);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "user01")
                            .param("password", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("LA-004: 不存在的用户名应返回 false 且不写入 Session")
        void loginWithNonExistentUser() throws Exception {
            when(userService.checkLogin("ghost", "any")).thenReturn(null);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "ghost")
                            .param("password", "any"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"))
                    .andExpect(request().sessionAttributeDoesNotExist("user"))
                    .andExpect(request().sessionAttributeDoesNotExist("admin"));
        }

        @Test
        @DisplayName("LA-005: 用户存在但密码错误应返回 false")
        void loginWithWrongPassword() throws Exception {
            when(userService.checkLogin("user01", "wrong")).thenReturn(null);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "user01")
                            .param("password", "wrong"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("LA-006: isadmin 既非 0 也非 1 时，当前实现返回 false（潜在缺陷）")
        void loginWithUnknownIsadminValueReturnsFalse() throws Exception {
            User weirdUser = user();
            weirdUser.setIsadmin(2);
            when(userService.checkLogin("user01", "secret")).thenReturn(weirdUser);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "user01")
                            .param("password", "secret"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"))
                    .andExpect(request().sessionAttributeDoesNotExist("user"))
                    .andExpect(request().sessionAttributeDoesNotExist("admin"));
        }

        @ParameterizedTest
        @CsvSource({
                "user01, secret,  0, /index,       user",
                "admin1, secret,  1, /admin_index,  admin"
        })
        @DisplayName("LA-007/008: 正常登录应返回正确路径并写入正确的 Session 属性")
        void loginSuccessWritesCorrectSessionKey(String userId, String pwd,
                                                  int isadmin, String expectedPath,
                                                  String sessionKey) throws Exception {
            User loginUser = user();
            loginUser.setUserID(userId);
            loginUser.setIsadmin(isadmin);
            when(userService.checkLogin(userId, pwd)).thenReturn(loginUser);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", userId)
                            .param("password", pwd))
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedPath))
                    .andExpect(request().sessionAttribute(sessionKey, loginUser));
        }

        @Test
        @DisplayName("LA-009: 普通用户登录不应写入 admin Session")
        void userLoginDoesNotSetAdminSession() throws Exception {
            User normalUser = user();
            when(userService.checkLogin("user01", "secret")).thenReturn(normalUser);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "user01")
                            .param("password", "secret"))
                    .andExpect(request().sessionAttribute("user", normalUser))
                    .andExpect(request().sessionAttributeDoesNotExist("admin"));
        }

        @Test
        @DisplayName("LA-010: 管理员登录不应写入 user Session")
        void adminLoginDoesNotSetUserSession() throws Exception {
            User adminUser = admin();
            when(userService.checkLogin("admin01", "secret")).thenReturn(adminUser);

            mockMvc.perform(post("/loginCheck.do")
                            .param("userID", "admin01")
                            .param("password", "secret"))
                    .andExpect(request().sessionAttribute("admin", adminUser))
                    .andExpect(request().sessionAttributeDoesNotExist("user"));
        }
    }

    // ==================== 二、注册边界与异常场景 ====================

    @Nested
    @DisplayName("注册边界场景")
    class RegisterEdgeCases {

        @BeforeEach
        void resetRegisterMocks() {
            reset(userService);
        }

        @Test
        @DisplayName("LA-011: 注册成功后应重定向到 login 页面")
        void registerRedirectsToLogin() throws Exception {
            mockMvc.perform(post("/register.do")
                            .param("userID", "newuser")
                            .param("userName", "New User")
                            .param("password", "pw123")
                            .param("email", "new@example.com")
                            .param("phone", "13800000099"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("login"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userService).create(captor.capture());
            User created = captor.getValue();
            assertEquals("newuser", created.getUserID());
            assertEquals("New User", created.getUserName());
            assertEquals("pw123", created.getPassword());
            assertEquals("new@example.com", created.getEmail());
            assertEquals("13800000099", created.getPhone());
            assertEquals("", created.getPicture());
            assertEquals(0, created.getIsadmin());
        }

        @Test
        @DisplayName("LA-012: 注册时使用空字符串字段仍可创建用户（当前无校验）")
        void registerWithEmptyFieldsStillCreatesUser() throws Exception {
            mockMvc.perform(post("/register.do")
                            .param("userID", "")
                            .param("userName", "")
                            .param("password", "")
                            .param("email", "")
                            .param("phone", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("login"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userService).create(captor.capture());
            assertEquals("", captor.getValue().getUserID());
            assertEquals("", captor.getValue().getPassword());
        }

        @Test
        @DisplayName("LA-013: Service 层 create 抛异常时应向上传播")
        void registerPropagatesServiceException() throws Exception {
            doThrow(new RuntimeException("duplicate userID"))
                    .when(userService).create(any(User.class));

            assertThrows(NestedServletException.class, () ->
                    mockMvc.perform(post("/register.do")
                            .param("userID", "dup")
                            .param("userName", "Dup")
                            .param("password", "pw")
                            .param("email", "dup@example.com")
                            .param("phone", "13800000000")).andReturn());
        }

        @Test
        @DisplayName("LA-014: 注册创建的用户 isadmin 默认为 0（普通用户），无法通过注册获取管理员权限")
        void registerAlwaysCreatesNonAdminUser() throws Exception {
            mockMvc.perform(post("/register.do")
                            .param("userID", "hacker")
                            .param("userName", "Hacker")
                            .param("password", "pw")
                            .param("email", "h@example.com")
                            .param("phone", "13800000000"))
                    .andExpect(status().is3xxRedirection());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userService).create(captor.capture());
            assertEquals(0, captor.getValue().getIsadmin());
        }
    }

    // ==================== 三、登出场景 ====================

    @Nested
    @DisplayName("登出场景")
    class LogoutCases {

        @Test
        @DisplayName("LA-015: 普通用户登出后 Session 中不再有 user 属性")
        void logoutRemovesUserSession() throws Exception {
            MockHttpSession session = userSession();
            assertNotNull(session.getAttribute("user"));

            mockMvc.perform(get("/logout.do").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/index"));

            assertNull(session.getAttribute("user"));
        }

        @Test
        @DisplayName("LA-016: 管理员退出后 Session 中不再有 admin 属性")
        void quitRemovesAdminSession() throws Exception {
            MockHttpSession session = adminSession();
            assertNotNull(session.getAttribute("admin"));

            mockMvc.perform(get("/quit.do").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/index"));

            assertNull(session.getAttribute("admin"));
        }

        @Test
        @DisplayName("LA-017: 未登录状态下调用 logout 不应报错，仍重定向到首页")
        void logoutWithoutSessionDoesNotFail() throws Exception {
            mockMvc.perform(get("/logout.do"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/index"));
        }

        @Test
        @DisplayName("LA-018: 未登录状态下调用 quit 不应报错，仍重定向到首页")
        void quitWithoutSessionDoesNotFail() throws Exception {
            mockMvc.perform(get("/quit.do"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/index"));
        }
    }

    // ==================== 四、密码校验接口 ====================

    @Nested
    @DisplayName("密码校验接口")
    class CheckPasswordCases {

        @Test
        @DisplayName("LA-019: 密码正确时返回 true")
        void checkPasswordReturnsTrue() throws Exception {
            when(userService.findByUserID("user01")).thenReturn(user());

            mockMvc.perform(get("/checkPassword.do")
                            .param("userID", "user01")
                            .param("password", "secret"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("LA-020: 密码错误时返回 false")
        void checkPasswordReturnsFalse() throws Exception {
            when(userService.findByUserID("user01")).thenReturn(user());

            mockMvc.perform(get("/checkPassword.do")
                            .param("userID", "user01")
                            .param("password", "wrong"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("LA-021: 用户不存在时 checkPassword 抛出 NPE（缺陷：未做空判断）")
        void checkPasswordThrowsWhenUserNotFound() throws Exception {
            when(userService.findByUserID("ghost")).thenReturn(null);

            assertThrows(NestedServletException.class, () ->
                    mockMvc.perform(get("/checkPassword.do")
                            .param("userID", "ghost")
                            .param("password", "any")).andReturn());
        }
    }

    // ==================== 五、页面访问权限 ====================

    @Nested
    @DisplayName("页面访问权限")
    class PageAccessCases {

        @Test
        @DisplayName("LA-022: 未登录访问 /signup 应正常返回注册页")
        void signupAccessibleWithoutLogin() throws Exception {
            mockMvc.perform(get("/signup"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("signup"));
        }

        @Test
        @DisplayName("LA-023: 未登录访问 /login 应正常返回登录页")
        void loginPageAccessibleWithoutLogin() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("LA-024: 已登录用户访问 /user_info 应正常返回个人信息页")
        void userInfoAccessibleWhenLoggedIn() throws Exception {
            mockMvc.perform(get("/user_info").session(userSession()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user_info"));
        }

        @Test
        @DisplayName("LA-025: 未登录访问 /user_info 会在模板渲染阶段抛异常（缺陷：页面依赖 session.user）")
        void userInfoFailsWithoutLoginBecauseTemplateUsesSessionUser() {
            assertThrows(NestedServletException.class, () ->
                    mockMvc.perform(get("/user_info")).andReturn());
        }
    }
}
