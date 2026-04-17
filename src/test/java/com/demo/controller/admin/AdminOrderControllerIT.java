package com.demo.controller.admin;

import com.demo.entity.Order;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.order;
import static com.demo.support.ITDataFactory.orderVo;
import static com.demo.support.ITDataFactory.page;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService;

    @Test
    void reservationManageReturnsViewAndPendingOrderCount() throws Exception {
        List<Order> auditedOrders = Arrays.asList(order(1, "user01", 1, 2), order(2, "user02", 1, 3));
        List<OrderVo> orderVos = Arrays.asList(
                orderVo(1, "user01", 1, "Arena A", 2),
                orderVo(2, "user02", 1, "Arena A", 3));
        when(orderService.findAuditOrder()).thenReturn(auditedOrders);
        when(orderVoService.returnVo(auditedOrders)).thenReturn(orderVos);
        when(orderService.findNoAuditOrder(any(Pageable.class)))
                .thenReturn(firstPage(Arrays.asList(order(3, "user03", 1, 1)), 10, 21));

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attribute("order_list", orderVos))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void getNoAuditOrderUsesDefaultPage() throws Exception {
        List<Order> orders = Arrays.asList(order(1, "user01", 1, 1), order(2, "user02", 1, 1));
        List<OrderVo> orderVos = Arrays.asList(
                orderVo(1, "user01", 1, "Arena A", 1),
                orderVo(2, "user02", 1, "Arena B", 1));
        when(orderService.findNoAuditOrder(any(Pageable.class))).thenReturn(firstPage(orders, 10, 12));
        when(orderVoService.returnVo(orders)).thenReturn(orderVos);

        mockMvc.perform(get("/admin/getOrderList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(1))
                .andExpect(jsonPath("$[1].venueName").value("Arena B"));
    }

    @Test
    void getNoAuditOrderReturnsRequestedPage() throws Exception {
        List<Order> orders = Arrays.asList(order(11, "user11", 1, 1));
        List<OrderVo> orderVos = Arrays.asList(orderVo(11, "user11", 1, "Arena K", 1));
        when(orderService.findNoAuditOrder(any(Pageable.class))).thenReturn(page(orders, 1, 10, 12));
        when(orderVoService.returnVo(orders)).thenReturn(orderVos);

        mockMvc.perform(get("/admin/getOrderList.do").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(11));
    }

    @Test
    void passOrderReturnsTrue() throws Exception {
        mockMvc.perform(post("/passOrder.do").param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).confirmOrder(1);
    }

    @Test
    void passOrderFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("missing order")).when(orderService).confirmOrder(999);

        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/passOrder.do").param("orderID", "999")).andReturn());
    }

    @Test
    void rejectOrderReturnsTrue() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).rejectOrder(1);
    }

    @Test
    void rejectOrderFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("missing order")).when(orderService).rejectOrder(999);

        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/rejectOrder.do").param("orderID", "999")).andReturn());
    }
}
