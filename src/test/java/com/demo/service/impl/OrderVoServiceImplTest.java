package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 和 MessageVoServiceImplTest 几乎相同
 */
@ExtendWith(MockitoExtension.class)
public class OrderVoServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @Spy
    @InjectMocks
    private OrderVoServiceImpl orderVoService;


    @Test
    @DisplayName("returnOrderVoByOrderID: 应根据 order 与 venue 信息正确组装 OrderVo")
    void returnOrderVoByOrderID_shouldAssembleOrderVo() {
        Order order = buildOrder(1, "u001", 99, 2);
        Venue venue = buildVenue(99, "Arena A");
        when(orderDao.findByOrderID(1)).thenReturn(order);
        when(venueDao.findByVenueID(99)).thenReturn(venue);

        OrderVo actual = orderVoService.returnOrderVoByOrderID(1);

        assertAll(
                () -> assertEquals(1, actual.getOrderID()),
                () -> assertEquals("u001", actual.getUserID()),
                () -> assertEquals(99, actual.getVenueID()),
                () -> assertEquals("Arena A", actual.getVenueName()),
                () -> assertEquals(2, actual.getState()),
                () -> assertEquals(LocalDateTime.of(2026, 4, 8, 10, 0), actual.getOrderTime()),
                () -> assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), actual.getStartTime()),
                () -> assertEquals(3, actual.getHours()),
                () -> assertEquals(450, actual.getTotal())
        );
        verify(orderDao).findByOrderID(1);
        verify(venueDao).findByVenueID(99);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("returnOrderVoByOrderID: 订单不存在时当前实现会抛出空指针异常")
    void returnOrderVoByOrderID_shouldThrowWhenOrderNotFound() {
        when(orderDao.findByOrderID(999)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderVoService.returnOrderVoByOrderID(999));

        verify(orderDao).findByOrderID(999);
        verify(venueDao, never()).findByVenueID(anyInt());
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("returnOrderVoByOrderID: 场馆不存在时当前实现会抛出空指针异常")
    void returnOrderVoByOrderID_shouldThrowWhenVenueNotFound() {
        Order order = buildOrder(2, "u002", 88, 1);
        when(orderDao.findByOrderID(2)).thenReturn(order);
        when(venueDao.findByVenueID(88)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderVoService.returnOrderVoByOrderID(2));

        verify(orderDao).findByOrderID(2);
        verify(venueDao).findByVenueID(88);
        verifyNoMoreInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("returnVo: 应按输入顺序逐条转换为 OrderVo 列表")
    void returnVo_shouldConvertOrdersInOrder() {
        Order first = buildOrder(10, "u010", 1, 1);
        Order second = buildOrder(20, "u020", 2, 2);
        OrderVo firstVo = buildOrderVo(10, "u010", 1, "Arena 1", 1);
        OrderVo secondVo = buildOrderVo(20, "u020", 2, "Arena 2", 2);
        doReturn(firstVo).when(orderVoService).returnOrderVoByOrderID(10);
        doReturn(secondVo).when(orderVoService).returnOrderVoByOrderID(20);

        List<OrderVo> actual = orderVoService.returnVo(Arrays.asList(first, second));

        assertEquals(2, actual.size());
        assertSame(firstVo, actual.get(0));
        assertSame(secondVo, actual.get(1));
        verify(orderVoService).returnOrderVoByOrderID(10);
        verify(orderVoService).returnOrderVoByOrderID(20);
        verifyNoInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("returnVo: 空列表应返回空结果且不访问 DAO")
    void returnVo_shouldReturnEmptyListWhenInputIsEmpty() {
        List<OrderVo> actual = orderVoService.returnVo(Collections.emptyList());

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
        verifyNoInteractions(orderDao, venueDao);
    }

    @Test
    @DisplayName("returnVo: 输入列表为 null 时当前实现会抛出空指针异常")
    void returnVo_shouldThrowWhenInputListIsNull() {
        assertThrows(NullPointerException.class,
                () -> orderVoService.returnVo(null));

        verifyNoInteractions(orderDao, venueDao);
    }

    private Order buildOrder(int orderId, String userId, int venueId, int state) {
        return new Order(
                orderId,
                userId,
                venueId,
                state,
                LocalDateTime.of(2026, 4, 8, 10, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0),
                3,
                450
        );
    }

    private Venue buildVenue(int venueId, String venueName) {
        Venue venue = new Venue();
        venue.setVenueID(venueId);
        venue.setVenueName(venueName);
        venue.setDescription("desc-" + venueName);
        venue.setPrice(150);
        venue.setPicture("file/venue/" + venueName + ".png");
        venue.setAddress("address-" + venueName);
        venue.setOpen_time("08:00");
        venue.setClose_time("22:00");
        return venue;
    }

    private OrderVo buildOrderVo(int orderId, String userId, int venueId, String venueName, int state) {
        return new OrderVo(
                orderId,
                userId,
                venueId,
                venueName,
                state,
                LocalDateTime.of(2026, 4, 8, 10, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0),
                3,
                450
        );
    }
}
