package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
import com.demo.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    @DisplayName("findById: 正常流程")
    void findById_shouldDelegateToDaoAndReturnMessage() {
        Message expected = buildMessage(1, "u001", "hello", MessageService.STATE_NO_AUDIT);
        when(messageDao.getOne(1)).thenReturn(expected);

        Message actual = messageService.findById(1);

        assertSame(expected, actual);
        verify(messageDao).getOne(1);
        verifyNoMoreInteractions(messageDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    @DisplayName("findById: 对边界/异常风格的 messageID 当前无校验，都会直接委托给 DAO")
    void findById_shouldDirectlyDelegateForBoundaryIds(int messageId) {
        Message expected = buildMessage(messageId, "u001", "boundary", MessageService.STATE_NO_AUDIT);
        when(messageDao.getOne(messageId)).thenReturn(expected);

        Message actual = messageService.findById(messageId);

        assertSame(expected, actual);
        verify(messageDao).getOne(messageId);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("findByUser: 正常流程")
    void findByUser_shouldDelegateWithSameArguments() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Message> expectedPage = new PageImpl<>(Collections.singletonList(
                buildMessage(2, "u002", "message", MessageService.STATE_PASS)
        ));
        when(messageDao.findAllByUserID("u002", pageable)).thenReturn(expectedPage);

        Page<Message> actualPage = messageService.findByUser("u002", pageable);

        assertSame(expectedPage, actualPage);
        verify(messageDao).findAllByUserID("u002", pageable);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("findByUser: Pageable为null时，当前无校验，直接委托 DAO")
    void findByUser_shouldDelegateWithNullPageable() {
        Page<Message> expectedPage = new PageImpl<>(Collections.emptyList());
        when(messageDao.findAllByUserID("u002", null)).thenReturn(expectedPage);

        Page<Message> actualPage = messageService.findByUser("u002", null);

        assertSame(expectedPage, actualPage);
        verify(messageDao).findAllByUserID("u002", null);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("create: 正常流程，应保存留言并返回持久化后的 messageID")
    void create_shouldSaveMessageAndReturnGeneratedId() {
        Message input = buildMessage(0, "u003", "new content", MessageService.STATE_NO_AUDIT);
        Message persisted = buildMessage(100, "u003", "new content", MessageService.STATE_NO_AUDIT);
        when(messageDao.save(input)).thenReturn(persisted);

        int createdId = messageService.create(input);

        assertEquals(100, createdId);
        verify(messageDao).save(input);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("delById: 正常流程，应调用 DAO 删除指定留言")
    void delById_shouldDelegateDelete() {
        messageService.delById(9);

        verify(messageDao).deleteById(9);
        verifyNoMoreInteractions(messageDao);
    }

    /**
     * 这种参数的边界测试在 Service 层都没有特殊处理逻辑，后面几个方法的测试用例就不写了。
     */
//    @ParameterizedTest
//    @ValueSource(ints = {0, -1})
//    @DisplayName("delById: 对非正数 ID 当前不做拦截，直接委托 DAO")
//    void delById_shouldDirectlyDelegateForNonPositiveIds(int messageId) {
//        messageService.delById(messageId);
//
//        verify(messageDao).deleteById(messageId);
//        verifyNoMoreInteractions(messageDao);
//    }

    @Test
    @DisplayName("update: 正常流程，应保存修改后的留言对象")
    void update_shouldSaveMessage() {
        Message message = buildMessage(10, "u010", "updated", MessageService.STATE_NO_AUDIT);

        messageService.update(message);

        verify(messageDao).save(message);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("confirmMessage: 正常流程，留言存在时应更新为审核通过状态")
    void confirmMessage_shouldUpdateStateToPassWhenMessageExists() {
        Message existing = buildMessage(11, "u011", "待审核", MessageService.STATE_NO_AUDIT);
        when(messageDao.findByMessageID(11)).thenReturn(existing);

        messageService.confirmMessage(11);

        verify(messageDao).findByMessageID(11);
        verify(messageDao).updateState(MessageService.STATE_PASS, 11);
        verifyNoMoreInteractions(messageDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999})
    @DisplayName("confirmMessage: 留言不存在时应抛出异常且不更新状态")
    void confirmMessage_shouldThrowWhenMessageNotFound(int messageId) {
        when(messageDao.findByMessageID(messageId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.confirmMessage(messageId));

        assertEquals("留言不存在", exception.getMessage());
        verify(messageDao).findByMessageID(messageId);
        verify(messageDao, never()).updateState(anyInt(), anyInt());
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("rejectMessage: 留言存在时应更新为拒绝状态")
    void rejectMessage_shouldUpdateStateToRejectWhenMessageExists() {
        Message existing = buildMessage(12, "u012", "待审核", MessageService.STATE_NO_AUDIT);
        when(messageDao.findByMessageID(12)).thenReturn(existing);

        messageService.rejectMessage(12);

        verify(messageDao).findByMessageID(12);
        verify(messageDao).updateState(MessageService.STATE_REJECT, 12);
        verifyNoMoreInteractions(messageDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999})
    @DisplayName("rejectMessage: 留言不存在时应抛出异常且不得更新状态")
    void rejectMessage_shouldThrowWhenMessageNotFound(int messageId) {
        when(messageDao.findByMessageID(messageId)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageService.rejectMessage(messageId));

        assertEquals("留言不存在", exception.getMessage());
        verify(messageDao).findByMessageID(messageId);
        verify(messageDao, never()).updateState(anyInt(), anyInt());
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("findWaitState: 正常流程，应按未审核状态查询")
    void findWaitState_shouldQueryNoAuditState() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> expectedPage = new PageImpl<>(Collections.singletonList(
                buildMessage(13, "u013", "wait", MessageService.STATE_NO_AUDIT)
        ));
        when(messageDao.findAllByState(MessageService.STATE_NO_AUDIT, pageable)).thenReturn(expectedPage);

        Page<Message> actualPage = messageService.findWaitState(pageable);

        assertSame(expectedPage, actualPage);
        verify(messageDao).findAllByState(MessageService.STATE_NO_AUDIT, pageable);
        verifyNoMoreInteractions(messageDao);
    }

    @Test
    @DisplayName("findPassState: 正常流程，应按通过状态查询")
    void findPassState_shouldQueryPassState() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> expectedPage = new PageImpl<>(Collections.singletonList(
                buildMessage(14, "u014", "pass", MessageService.STATE_PASS)
        ));
        when(messageDao.findAllByState(MessageService.STATE_PASS, pageable)).thenReturn(expectedPage);

        Page<Message> actualPage = messageService.findPassState(pageable);

        assertSame(expectedPage, actualPage);
        verify(messageDao).findAllByState(MessageService.STATE_PASS, pageable);
        verifyNoMoreInteractions(messageDao);
    }

    private Message buildMessage(int messageId, String userId, String content, int state) {
        return new Message(messageId, userId, content, LocalDateTime.of(2026, 4, 8, 10, 0), state);
    }
}
