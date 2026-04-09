package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("findById: 正常行为，传 orderID 并返回 DAO 结果")
    void findById_shouldDelegateToDao() {
        Order expected = buildOrder(1, "u001", 10, OrderService.STATE_NO_AUDIT);
        when(orderDao.getOne(1)).thenReturn(expected);

        Order actual = orderService.findById(1);

        assertSame(expected, actual);
        verify(orderDao).getOne(1);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("findDateOrder: 应按 venueID 和时间区间查询订单")
    void findDateOrder_shouldDelegateToDao() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 10, 8, 0);
        LocalDateTime end = start.plusHours(12);
        List<Order> expected = Arrays.asList(
                buildOrder(2, "u001", 10, OrderService.STATE_WAIT),
                buildOrder(3, "u002", 10, OrderService.STATE_FINISH)
        );
        when(orderDao.findByVenueIDAndStartTimeIsBetween(10, start, end)).thenReturn(expected);

        List<Order> actual = orderService.findDateOrder(10, start, end);

        assertSame(expected, actual);
        verify(orderDao).findByVenueIDAndStartTimeIsBetween(10, start, end);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("findUserOrder: 应按 userID 和 pageable 查询用户订单")
    void findUserOrder_shouldDelegateToDao() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Order> expectedPage = new PageImpl<>(Collections.singletonList(
                buildOrder(4, "u004", 20, OrderService.STATE_NO_AUDIT)
        ));
        when(orderDao.findAllByUserID("u004", pageable)).thenReturn(expectedPage);

        Page<Order> actual = orderService.findUserOrder("u004", pageable);

        assertSame(expectedPage, actual);
        verify(orderDao).findAllByUserID("u004", pageable);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("updateOrder: 正常行为，应基于场馆与原订单重建关键字段并保存")
    void updateOrder_shouldMutateExistingOrderAndSave() {
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 12, 9, 0);
        Venue venue = buildVenue(88, "Arena A", 150);
        Order existing = buildOrder(9, "legacy", 1, OrderService.STATE_WAIT);
        existing.setOrderTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(venueDao.findByVenueName("Arena A")).thenReturn(venue);
        when(orderDao.findByOrderID(9)).thenReturn(existing);

        LocalDateTime before = LocalDateTime.now();
        orderService.updateOrder(9, "Arena A", startTime, 3, "u009");
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(venueDao).findByVenueName("Arena A");
        verify(orderDao).findByOrderID(9);
        verify(orderDao).save(captor.capture());
        verifyNoMoreInteractions(orderDao, venueDao);

        Order saved = captor.getValue();
        assertSame(existing, saved);
        assertAll(
                () -> assertEquals(OrderService.STATE_NO_AUDIT, saved.getState()),
                () -> assertEquals(3, saved.getHours()),
                () -> assertEquals(88, saved.getVenueID()),
                () -> assertEquals(startTime, saved.getStartTime()),
                () -> assertEquals("u009", saved.getUserID()),
                () -> assertEquals(450, saved.getTotal()),
                () -> assertNotNull(saved.getOrderTime()),
                () -> assertFalse(saved.getOrderTime().isBefore(before)),
                () -> assertFalse(saved.getOrderTime().isAfter(after))
        );
    }

    /**
     * 当前实现不够好，会导致执行无效的set操作直到执行setTotal抛出异常
     */
    @Test
    @DisplayName("updateOrder: 场馆不存在时当前实现会在最后setTotal时抛出异常，过晚")
    void updateOrder_shouldThrowWhenVenueNotFound() {

        Order existing = buildOrder(9, "legacy", 1, OrderService.STATE_WAIT);
        existing.setOrderTime(LocalDateTime.of(2026, 1, 1, 8, 0));

        when(venueDao.findByVenueName("Missing Venue")).thenReturn(null);
        when(orderDao.findByOrderID(10)).thenReturn(existing);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(10, "Missing Venue", LocalDateTime.of(2026, 4, 12, 10, 0), 2, "u010"));

        verify(venueDao).findByVenueName("Missing Venue");
        verify(orderDao).findByOrderID(anyInt());
        verify(orderDao, never()).save(any(Order.class));
        verifyNoMoreInteractions(venueDao, orderDao);
    }

    @Test
    @DisplayName("updateOrder: 订单不存在时当前实现会抛出空指针异常，且不会保存")
    void updateOrder_shouldThrowWhenOrderNotFound() {
        when(venueDao.findByVenueName("Arena B")).thenReturn(buildVenue(66, "Arena B", 200));
        when(orderDao.findByOrderID(11)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(11, "Arena B", LocalDateTime.of(2026, 4, 12, 11, 0), 2, "u011"));

        verify(venueDao).findByVenueName("Arena B");
        verify(orderDao).findByOrderID(11);
        verify(orderDao, never()).save(any(Order.class));
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("submit: 应创建新订单并按场馆单价计算总价")
    void submit_shouldCreateOrderAndSave() {
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 13, 14, 0);
        Venue venue = buildVenue(55, "Arena C", 120);
        when(venueDao.findByVenueName("Arena C")).thenReturn(venue);

        LocalDateTime before = LocalDateTime.now();
        orderService.submit("Arena C", startTime, 4, "u012");
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(venueDao).findByVenueName("Arena C");
        verify(orderDao).save(captor.capture());
        verifyNoMoreInteractions(orderDao, venueDao);

        Order saved = captor.getValue();
        assertAll(
                () -> assertEquals(OrderService.STATE_NO_AUDIT, saved.getState()),
                () -> assertEquals(4, saved.getHours()),
                () -> assertEquals(55, saved.getVenueID()),
                () -> assertEquals(startTime, saved.getStartTime()),
                () -> assertEquals("u012", saved.getUserID()),
                () -> assertEquals(480, saved.getTotal()),
                () -> assertNotNull(saved.getOrderTime()),
                () -> assertFalse(saved.getOrderTime().isBefore(before)),
                () -> assertFalse(saved.getOrderTime().isAfter(after))
        );
    }

    @Test
    @DisplayName("submit: 场馆不存在时当前实现会抛出空指针异常且不会保存订单")
    void submit_shouldThrowWhenVenueNotFound() {
        when(venueDao.findByVenueName("Missing Venue")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.submit("Missing Venue", LocalDateTime.of(2026, 4, 13, 15, 0), 1, "u013"));

        verify(venueDao).findByVenueName("Missing Venue");
        verify(orderDao, never()).save(any(Order.class));
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("delOrder: 应调用 DAO 删除指定订单")
    void delOrder_shouldDelegateDelete() {
        orderService.delOrder(20);

        verify(orderDao).deleteById(20);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("confirmOrder: 订单存在时应更新为已审核状态")
    void confirmOrder_shouldUpdateStateToWaitWhenOrderExists() {
        Order existing = buildOrder(21, "u021", 7, OrderService.STATE_NO_AUDIT);
        when(orderDao.findByOrderID(21)).thenReturn(existing);

        orderService.confirmOrder(21);

        verify(orderDao).findByOrderID(21);
        verify(orderDao).updateState(OrderService.STATE_WAIT, 21);
        verifyNoMoreInteractions(orderDao, venueDao);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999})
    @DisplayName("confirmOrder: 订单不存在时应抛出异常且不得更新状态")
    void confirmOrder_shouldThrowWhenOrderNotFound(int orderId) {
        when(orderDao.findByOrderID(orderId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.confirmOrder(orderId));

        assertEquals("订单不存在", exception.getMessage());
        verify(orderDao).findByOrderID(orderId);
        verify(orderDao, never()).updateState(anyInt(), anyInt());
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("finishOrder: 订单存在时应更新为已完成状态")
    void finishOrder_shouldUpdateStateToFinishWhenOrderExists() {
        Order existing = buildOrder(22, "u022", 7, OrderService.STATE_WAIT);
        when(orderDao.findByOrderID(22)).thenReturn(existing);

        orderService.finishOrder(22);

        verify(orderDao).findByOrderID(22);
        verify(orderDao).updateState(OrderService.STATE_FINISH, 22);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999})
    @DisplayName("finishOrder: 订单不存在时应抛出异常且不得更新状态")
    void finishOrder_shouldThrowWhenOrderNotFound(int orderId) {
        when(orderDao.findByOrderID(orderId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.finishOrder(orderId));

        assertEquals("订单不存在", exception.getMessage());
        verify(orderDao).findByOrderID(orderId);
        verify(orderDao, never()).updateState(anyInt(), anyInt());
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("rejectOrder: 订单存在时应更新为拒绝状态")
    void rejectOrder_shouldUpdateStateToRejectWhenOrderExists() {
        Order existing = buildOrder(23, "u023", 7, OrderService.STATE_WAIT);
        when(orderDao.findByOrderID(23)).thenReturn(existing);

        orderService.rejectOrder(23);

        verify(orderDao).findByOrderID(23);
        verify(orderDao).updateState(OrderService.STATE_REJECT, 23);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999})
    @DisplayName("rejectOrder: 订单不存在时应抛出异常且不得更新状态")
    void rejectOrder_shouldThrowWhenOrderNotFound(int orderId) {
        when(orderDao.findByOrderID(orderId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.rejectOrder(orderId));

        assertEquals("订单不存在", exception.getMessage());
        verify(orderDao).findByOrderID(orderId);
        verify(orderDao, never()).updateState(anyInt(), anyInt());
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("findNoAuditOrder: 应按未审核状态查询")
    void findNoAuditOrder_shouldQueryNoAuditState() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> expectedPage = new PageImpl<>(Collections.singletonList(
                buildOrder(24, "u024", 8, OrderService.STATE_NO_AUDIT)
        ));
        when(orderDao.findAllByState(OrderService.STATE_NO_AUDIT, pageable)).thenReturn(expectedPage);

        Page<Order> actual = orderService.findNoAuditOrder(pageable);

        assertSame(expectedPage, actual);
        verify(orderDao).findAllByState(OrderService.STATE_NO_AUDIT, pageable);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("findAuditOrder: 应查询已审核与已完成两种状态")
    void findAuditOrder_shouldQueryWaitAndFinishStates() {
        List<Order> expected = Arrays.asList(
                buildOrder(25, "u025", 9, OrderService.STATE_WAIT),
                buildOrder(26, "u026", 9, OrderService.STATE_FINISH)
        );
        when(orderDao.findAudit(OrderService.STATE_WAIT, OrderService.STATE_FINISH)).thenReturn(expected);

        List<Order> actual = orderService.findAuditOrder();

        assertSame(expected, actual);
        verify(orderDao).findAudit(OrderService.STATE_WAIT, OrderService.STATE_FINISH);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    private Order buildOrder(int orderId, String userId, int venueId, int state) {
        return new Order(
                orderId,
                userId,
                venueId,
                state,
                LocalDateTime.of(2026, 4, 8, 9, 0),
                LocalDateTime.of(2026, 4, 9, 10, 0),
                2,
                200
        );
    }

    private Venue buildVenue(int venueId, String venueName, int price) {
        Venue venue = new Venue();
        venue.setVenueID(venueId);
        venue.setVenueName(venueName);
        venue.setDescription("desc-" + venueName);
        venue.setPrice(price);
        venue.setPicture("file/venue/" + venueName + ".png");
        venue.setAddress("address-" + venueName);
        venue.setOpen_time("08:00");
        venue.setClose_time("22:00");
        return venue;
    }
}
