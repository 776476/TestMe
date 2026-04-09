package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.dao.UserDao;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageVoServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserDao userDao;

    @Spy
    @InjectMocks
    private MessageVoServiceImpl messageVoService;

    @Test
    @DisplayName("returnMessageVoByMessageID: 正常流程，应根据 message 与 user 信息正确组装 MessageVo")
    void returnMessageVoByMessageID_shouldAssembleMessageVo() {
        Message message = buildMessage(1, "u001", "hello", 1);
        User user = buildUser("u001", "Alice", "file/user/alice.png");
        when(messageDao.findByMessageID(1)).thenReturn(message);
        when(userDao.findByUserID("u001")).thenReturn(user);

        MessageVo actual = messageVoService.returnMessageVoByMessageID(1);

        assertAll(
                () -> assertEquals(1, actual.getMessageID()),
                () -> assertEquals("u001", actual.getUserID()),
                () -> assertEquals("hello", actual.getContent()),
                () -> assertEquals(LocalDateTime.of(2026, 4, 8, 10, 0), actual.getTime()),
                () -> assertEquals("Alice", actual.getUserName()),
                () -> assertEquals("file/user/alice.png", actual.getPicture()),
                () -> assertEquals(1, actual.getState())
        );
        verify(messageDao).findByMessageID(1);
        verify(userDao).findByUserID("u001");
        verifyNoMoreInteractions(messageDao, userDao);
    }

    /**
     * 存在缺陷
     */
    @Test
    @DisplayName("returnMessageVoByMessageID: 留言不存在时当前实现会抛出空指针异常")
    void returnMessageVoByMessageID_shouldThrowWhenMessageNotFound() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> messageVoService.returnMessageVoByMessageID(999));

        verify(messageDao).findByMessageID(999);
        verify(userDao, never()).findByUserID(anyString());
        verifyNoMoreInteractions(messageDao, userDao);
    }

    /**
     * 存在缺陷
     */
    @Test
    @DisplayName("returnMessageVoByMessageID: 用户不存在时当前实现会抛出空指针异常")
    void returnMessageVoByMessageID_shouldThrowWhenUserNotFound() {
        Message message = buildMessage(2, "u002", "world", 2);
        when(messageDao.findByMessageID(2)).thenReturn(message);
        when(userDao.findByUserID("u002")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> messageVoService.returnMessageVoByMessageID(2));

        verify(messageDao).findByMessageID(2);
        verify(userDao).findByUserID("u002");
        verifyNoMoreInteractions(messageDao, userDao);
    }

    @Test
    @DisplayName("returnVo: 正常流程，应按输入顺序逐条转换为 MessageVo 列表")
    void returnVo_shouldConvertMessagesInOrder() {
        Message first = buildMessage(10, "u010", "first", 1);
        Message second = buildMessage(20, "u020", "second", 2);
        MessageVo firstVo = buildMessageVo(10, "u010", "first", "User10", "file/user/u010.png", 1);
        MessageVo secondVo = buildMessageVo(20, "u020", "second", "User20", "file/user/u020.png", 2);
        doReturn(firstVo).when(messageVoService).returnMessageVoByMessageID(10);
        doReturn(secondVo).when(messageVoService).returnMessageVoByMessageID(20);

        List<MessageVo> actual = messageVoService.returnVo(Arrays.asList(first, second));

        assertEquals(2, actual.size());
        assertSame(firstVo, actual.get(0));
        assertSame(secondVo, actual.get(1));
        verify(messageVoService).returnMessageVoByMessageID(10);
        verify(messageVoService).returnMessageVoByMessageID(20);
        verifyNoInteractions(messageDao, userDao);
    }

    @Test
    @DisplayName("returnVo: 空列表应返回空结果且不访问 DAO")
    void returnVo_shouldReturnEmptyListWhenInputIsEmpty() {
        List<MessageVo> actual = messageVoService.returnVo(Collections.emptyList());

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
        verifyNoInteractions(messageDao, userDao);
    }

    /**
     * 存在缺陷
     */
    @Test
    @DisplayName("returnVo: 输入列表为 null 时当前实现会抛出空指针异常")
    void returnVo_shouldThrowWhenInputListIsNull() {
        assertThrows(NullPointerException.class,
                () -> messageVoService.returnVo(null));

        verifyNoInteractions(messageDao, userDao);
    }

    private Message buildMessage(int messageId, String userId, String content, int state) {
        return new Message(messageId, userId, content, LocalDateTime.of(2026, 4, 8, 10, 0), state);
    }

    private User buildUser(String userId, String userName, String picture) {
        User user = new User();
        user.setId(1);
        user.setUserID(userId);
        user.setUserName(userName);
        user.setPassword("secret");
        user.setEmail("demo@example.com");
        user.setPhone("13800000000");
        user.setIsadmin(0);
        user.setPicture(picture);
        return user;
    }

    private MessageVo buildMessageVo(int messageId, String userId, String content,
                                     String userName, String picture, int state) {
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageID(messageId);
        messageVo.setUserID(userId);
        messageVo.setContent(content);
        messageVo.setTime(LocalDateTime.of(2026, 4, 8, 10, 0));
        messageVo.setUserName(userName);
        messageVo.setPicture(picture);
        messageVo.setState(state);
        return messageVo;
    }
}
