package com.demo.controller;

import com.demo.controller.admin.*;
import com.demo.entity.User;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.UserService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static com.demo.support.ITDataFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 组长负责模块：管理员权限安全测试
 *
 * 验证缺陷：所有 Admin Controller 均未做管理员 Session 校验，
 * 未登录用户可直接访问管理后台的所有接口。
 */
@WebMvcTest({
        AdminUserController.class,
        AdminVenueController.class,
        AdminNewsController.class,
        AdminOrderController.class,
        AdminMessageController.class
})
class AdminPermissionIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private VenueService venueService;
    @MockBean
    private NewsService newsService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private OrderVoService orderVoService;
    @MockBean
    private MessageService messageService;
    @MockBean
    private MessageVoService messageVoService;

    // ==================== 用户管理 ====================

    @Test
    @DisplayName("AP-001: 未登录可访问用户管理页（缺陷：无权限拦截）")
    void userManageAccessibleWithoutAdmin() throws Exception {
        when(userService.findByUserID(any(Pageable.class)))
                .thenReturn(firstPage(Collections.singletonList(user()), 10, 1));

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AP-002: 未登录可删除用户（缺陷：无权限拦截）")
    void deleteUserAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/delUser.do").param("id", "1"))
                .andExpect(status().isOk());

        verify(userService).delByID(1);
    }

    @Test
    @DisplayName("AP-003: 未登录可新增用户（缺陷：无权限拦截）")
    void addUserAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .param("userID", "hacker")
                        .param("userName", "Hacker")
                        .param("password", "pw")
                        .param("email", "h@example.com")
                        .param("phone", "13800000000"))
                .andExpect(status().is3xxRedirection());

        verify(userService).create(any(User.class));
    }

    // ==================== 场馆管理 ====================

    @Test
    @DisplayName("AP-004: 未登录可访问场馆管理页（缺陷：无权限拦截）")
    void venueManageAccessibleWithoutAdmin() throws Exception {
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(firstPage(Collections.emptyList(), 10, 0));

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AP-005: 未登录可删除场馆（缺陷：无权限拦截）")
    void deleteVenueAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/delVenue.do").param("venueID", "1"))
                .andExpect(status().isOk());

        verify(venueService).delById(1);
    }

    // ==================== 新闻管理 ====================

    @Test
    @DisplayName("AP-006: 未登录可访问新闻管理页（缺陷：无权限拦截）")
    void newsManageAccessibleWithoutAdmin() throws Exception {
        when(newsService.findAll(any(Pageable.class)))
                .thenReturn(firstPage(Arrays.asList(news(1)), 10, 1));

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AP-007: 未登录可删除新闻（缺陷：无权限拦截）")
    void deleteNewsAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/delNews.do").param("newsID", "1"))
                .andExpect(status().isOk());

        verify(newsService).delById(1);
    }

    // ==================== 订单审核 ====================

    @Test
    @DisplayName("AP-008: 未登录可审核通过订单（缺陷：无权限拦截）")
    void passOrderAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/passOrder.do").param("orderID", "1"))
                .andExpect(status().isOk());

        verify(orderService).confirmOrder(1);
    }

    @Test
    @DisplayName("AP-009: 未登录可拒绝订单（缺陷：无权限拦截）")
    void rejectOrderAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").param("orderID", "1"))
                .andExpect(status().isOk());

        verify(orderService).rejectOrder(1);
    }

    // ==================== 留言审核 ====================

    @Test
    @DisplayName("AP-010: 未登录可审核通过留言（缺陷：无权限拦截）")
    void passMessageAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/passMessage.do").param("messageID", "1"))
                .andExpect(status().isOk());

        verify(messageService).confirmMessage(1);
    }

    @Test
    @DisplayName("AP-011: 未登录可删除留言（缺陷：无权限拦截）")
    void deleteMessageAccessibleWithoutAdmin() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "1"))
                .andExpect(status().isOk());

        verify(messageService).delById(1);
    }
}
