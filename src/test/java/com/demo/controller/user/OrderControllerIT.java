package com.demo.controller.user;

import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.order;
import static com.demo.support.ITDataFactory.orderVo;
import static com.demo.support.ITDataFactory.user;
import static com.demo.support.ITDataFactory.userSession;
import static com.demo.support.ITDataFactory.venue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(OrderController.class)
class OrderControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService;

    @MockBean
    private VenueService venueService;

    @Test
    void orderManageReturnsViewForLoggedInUser() throws Exception {
        User loginUser = user();
        Page<Order> userOrders = firstPage(Collections.singletonList(order(1, loginUser.getUserID(), 1, 1)), 5, 11);
        when(orderService.findUserOrder(eq(loginUser.getUserID()), any(Pageable.class))).thenReturn(userOrders);

        mockMvc.perform(get("/order_manage").session(userSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void orderManageFailsWhenUserIsNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/order_manage")).andReturn());
    }

    @Test
    void orderPlaceLoadsVenueDetails() throws Exception {
        Venue venue = venue(1, "Arena A");
        when(venueService.findByVenueID(1)).thenReturn(venue);

        mockMvc.perform(get("/order_place.do").param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void orderPlaceWithoutIdReturnsView() throws Exception {
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));
    }

    @Test
    void getOrderListReturnsCurrentUserOrders() throws Exception {
        User loginUser = user();
        List<Order> orders = Arrays.asList(order(1, loginUser.getUserID(), 1, 1),
                order(2, loginUser.getUserID(), 1, 2));
        List<OrderVo> orderVos = Arrays.asList(
                orderVo(1, loginUser.getUserID(), 1, "Arena A", 1),
                orderVo(2, loginUser.getUserID(), 1, "Arena A", 2));
        when(orderService.findUserOrder(eq(loginUser.getUserID()), any(Pageable.class)))
                .thenReturn(firstPage(orders, 5, 7));
        when(orderVoService.returnVo(orders)).thenReturn(orderVos);

        mockMvc.perform(get("/getOrderList.do").session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(1))
                .andExpect(jsonPath("$[1].state").value(2));
    }

    @Test
    void getOrderListFailsWhenUserIsNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/getOrderList.do")).andReturn());
    }

    @Test
    void addOrderRedirectsAfterSuccessfulSubmission() throws Exception {
        User loginUser = user();
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        mockMvc.perform(post("/addOrder.do")
                .session(userSession())
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "2026-04-09 09:00")
                .param("hours", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        verify(orderService).submit(eq("Arena A"), timeCaptor.capture(), eq(2), eq(loginUser.getUserID()));
        assertEquals(LocalDateTime.of(2026, 4, 9, 9, 0, 0), timeCaptor.getValue());
    }

    @Test
    void addOrderFailsWhenUserIsNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/addOrder.do")
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "2026-04-09 09:00")
                .param("hours", "2")).andReturn());

        verify(orderService, never()).submit(any(), any(), eq(2), any());
    }

    @Test
    void addOrderFailsForInvalidStartTimeFormat() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/addOrder.do")
                .session(userSession())
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "bad")
                .param("hours", "2")).andReturn());
    }

    @Test
    void finishOrderCallsService() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(orderService).finishOrder(1);
    }

    @Test
    void finishOrderFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("missing order")).when(orderService).finishOrder(999);

        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/finishOrder.do").param("orderID", "999")).andReturn());
    }

    @Test
    void modifyOrderPageLoadsOrderAndVenue() throws Exception {
        Order order = order(1, "user01", 2, 1);
        Venue venue = venue(2, "Arena B");
        when(orderService.findById(1)).thenReturn(order);
        when(venueService.findByVenueID(2)).thenReturn(venue);

        mockMvc.perform(get("/modifyOrder.do").param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void modifyOrderUpdatesOrderForLoggedInUser() throws Exception {
        User loginUser = user();
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        mockMvc.perform(post("/modifyOrder")
                .session(userSession())
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "2026-04-10 10:00")
                .param("hours", "3")
                .param("orderID", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        verify(orderService).updateOrder(eq(1), eq("Arena A"), timeCaptor.capture(), eq(3), eq(loginUser.getUserID()));
        assertEquals(LocalDateTime.of(2026, 4, 10, 10, 0, 0), timeCaptor.getValue());
    }

    @Test
    void modifyOrderFailsWhenUserIsNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/modifyOrder")
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "2026-04-10 10:00")
                .param("hours", "3")
                .param("orderID", "1")).andReturn());
    }

    @Test
    void modifyOrderFailsForInvalidStartTimeFormat() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/modifyOrder")
                .session(userSession())
                .param("venueName", "Arena A")
                .param("date", "ignored")
                .param("startTime", "bad")
                .param("hours", "3")
                .param("orderID", "1")).andReturn());
    }

    @Test
    void deleteOrderReturnsTrue() throws Exception {
        mockMvc.perform(post("/delOrder.do").param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).delOrder(1);
    }

    @Test
    void deleteOrderFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("delete failed")).when(orderService).delOrder(999);

        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/delOrder.do").param("orderID", "999")).andReturn());
    }

    @Test
    void getVenueOrdersReturnsVenueOrderPayload() throws Exception {
        Venue venue = venue(1, "Arena A");
        List<Order> orders = Collections.singletonList(order(1, "user01", 1, 1));
        when(venueService.findByVenueName("Arena A")).thenReturn(venue);
        when(orderService.findDateOrder(eq(1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(orders);

        mockMvc.perform(get("/order/getOrderList.do")
                .param("venueName", "Arena A")
                .param("date", "2026-04-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.venueID").value(1))
                .andExpect(jsonPath("$.orders[0].orderID").value(1));
    }

    @Test
    void getVenueOrdersFailsWhenVenueIsMissing() throws Exception {
        when(venueService.findByVenueName("Missing Arena")).thenReturn(null);

        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/order/getOrderList.do")
                .param("venueName", "Missing Arena")
                .param("date", "2026-04-09")).andReturn());
    }

    @Test
    void getVenueOrdersFailsForInvalidDateFormat() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/order/getOrderList.do")
                .param("venueName", "Arena A")
                .param("date", "bad")).andReturn());
    }
}
