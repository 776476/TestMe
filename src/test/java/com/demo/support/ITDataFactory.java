package com.demo.support;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.entity.vo.OrderVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public final class ITDataFactory {
    private ITDataFactory() {
    }

    public static LocalDateTime fixedTime() {
        return LocalDateTime.of(2026, 4, 7, 10, 30, 0);
    }

    public static User user() {
        User user = new User();
        user.setId(1);
        user.setUserID("user01");
        user.setUserName("User One");
        user.setPassword("secret");
        user.setEmail("user01@example.com");
        user.setPhone("13800000001");
        user.setIsadmin(0);
        user.setPicture("file/user/original.png");
        return user;
    }

    public static User admin() {
        User admin = user();
        admin.setId(2);
        admin.setUserID("admin01");
        admin.setUserName("Admin One");
        admin.setIsadmin(1);
        return admin;
    }

    public static News news(int newsId) {
        News news = new News();
        news.setNewsID(newsId);
        news.setTitle("News " + newsId);
        news.setContent("Content " + newsId);
        news.setTime(fixedTime().minusDays(newsId));
        return news;
    }

    public static Venue venue(int venueId, String venueName) {
        Venue venue = new Venue();
        venue.setVenueID(venueId);
        venue.setVenueName(venueName);
        venue.setDescription("Venue " + venueId);
        venue.setPrice(100 + venueId);
        venue.setPicture("file/venue/venue-" + venueId + ".png");
        venue.setAddress("Address " + venueId);
        venue.setOpen_time("08:00");
        venue.setClose_time("22:00");
        return venue;
    }

    public static Order order(int orderId, String userId, int venueId, int state) {
        Order order = new Order();
        order.setOrderID(orderId);
        order.setUserID(userId);
        order.setVenueID(venueId);
        order.setState(state);
        order.setOrderTime(fixedTime().minusHours(orderId));
        order.setStartTime(fixedTime().plusDays(1).withHour(9));
        order.setHours(2);
        order.setTotal(200);
        return order;
    }

    public static OrderVo orderVo(int orderId, String userId, int venueId, String venueName, int state) {
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderID(orderId);
        orderVo.setUserID(userId);
        orderVo.setVenueID(venueId);
        orderVo.setVenueName(venueName);
        orderVo.setState(state);
        orderVo.setOrderTime(fixedTime().minusHours(orderId));
        orderVo.setStartTime(fixedTime().plusDays(1).withHour(9));
        orderVo.setHours(2);
        orderVo.setTotal(200);
        return orderVo;
    }

    public static Message message(int messageId, String userId, int state) {
        Message message = new Message();
        message.setMessageID(messageId);
        message.setUserID(userId);
        message.setContent("Message " + messageId);
        message.setTime(fixedTime().minusMinutes(messageId));
        message.setState(state);
        return message;
    }

    public static MessageVo messageVo(int messageId, String userId, int state) {
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageID(messageId);
        messageVo.setUserID(userId);
        messageVo.setContent("Message " + messageId);
        messageVo.setTime(fixedTime().minusMinutes(messageId));
        messageVo.setUserName("User " + userId);
        messageVo.setPicture("file/user/" + userId + ".png");
        messageVo.setState(state);
        return messageVo;
    }

    public static <T> Page<T> page(List<T> content, int page, int size, long total) {
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    public static <T> Page<T> firstPage(List<T> content, int size, long total) {
        return page(content, 0, size, total);
    }

    public static <T> Page<T> emptyPage(int size) {
        return page(Collections.emptyList(), 0, size, 0);
    }

    public static MockHttpSession userSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user());
        return session;
    }

    public static MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("admin", admin());
        return session;
    }
}
