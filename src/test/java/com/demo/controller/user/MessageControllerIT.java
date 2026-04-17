package com.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.message;
import static com.demo.support.ITDataFactory.messageVo;
import static com.demo.support.ITDataFactory.userSession;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(MessageController.class)
class MessageControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    @Test
    void messageListReturnsViewForLoggedInUser() throws Exception {
        List<Message> publicMessages = Arrays.asList(message(1, "user01", 2), message(2, "user02", 2));
        List<MessageVo> publicMessageVos = Arrays.asList(messageVo(1, "user01", 2), messageVo(2, "user02", 2));
        when(messageService.findPassState(any(Pageable.class))).thenReturn(firstPage(publicMessages, 5, 16));
        when(messageVoService.returnVo(publicMessages)).thenReturn(publicMessageVos);
        when(messageService.findByUser(eq("user01"), any(Pageable.class)))
                .thenReturn(firstPage(Collections.singletonList(message(3, "user01", 1)), 5, 6));

        mockMvc.perform(get("/message_list").session(userSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"))
                .andExpect(model().attribute("total", 4))
                .andExpect(model().attribute("user_total", 2));
    }

    @Test
    void messageListFailsWhenUserIsNotLoggedIn() throws Exception {
        when(messageService.findPassState(any(Pageable.class)))
                .thenReturn(firstPage(Collections.singletonList(message(1, "user01", 2)), 5, 1));
        when(messageVoService.returnVo(any())).thenReturn(Collections.singletonList(messageVo(1, "user01", 2)));

        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/message_list")).andReturn());
    }

    @Test
    void getPublicMessageListUsesDefaultPage() throws Exception {
        List<Message> messages = Arrays.asList(message(1, "user01", 2), message(2, "user02", 2));
        List<MessageVo> messageVos = Arrays.asList(messageVo(1, "user01", 2), messageVo(2, "user02", 2));
        when(messageService.findPassState(any(Pageable.class))).thenReturn(firstPage(messages, 5, 8));
        when(messageVoService.returnVo(messages)).thenReturn(messageVos);

        mockMvc.perform(get("/message/getMessageList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(1))
                .andExpect(jsonPath("$[1].state").value(2));
    }

    @Test
    void getPublicMessageListReturnsRequestedPage() throws Exception {
        List<Message> messages = Arrays.asList(message(6, "user01", 2), message(7, "user02", 2));
        List<MessageVo> messageVos = Arrays.asList(messageVo(6, "user01", 2), messageVo(7, "user02", 2));
        when(messageService.findPassState(any(Pageable.class)))
                .thenReturn(com.demo.support.ITDataFactory.page(messages, 1, 5, 8));
        when(messageVoService.returnVo(messages)).thenReturn(messageVos);

        mockMvc.perform(get("/message/getMessageList").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(6))
                .andExpect(jsonPath("$[1].messageID").value(7));
    }

    @Test
    void getUserMessageListReturnsAllCurrentUserMessages() throws Exception {
        List<Message> messages = Arrays.asList(message(1, "user01", 1), message(2, "user01", 3));
        List<MessageVo> messageVos = Arrays.asList(messageVo(1, "user01", 1), messageVo(2, "user01", 3));
        when(messageService.findByUser(eq("user01"), any(Pageable.class))).thenReturn(firstPage(messages, 5, 2));
        when(messageVoService.returnVo(messages)).thenReturn(messageVos);

        mockMvc.perform(get("/message/findUserList").session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value(1))
                .andExpect(jsonPath("$[1].state").value(3));
    }

    @Test
    void getUserMessageListFailsWhenUserIsNotLoggedIn() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/message/findUserList")).andReturn());
    }

    @Test
    void sendMessageCreatesPendingMessageAndRedirects() throws Exception {
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        mockMvc.perform(post("/sendMessage")
                .param("userID", "user01")
                .param("content", "A new message"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        verify(messageService).create(messageCaptor.capture());
        Message created = messageCaptor.getValue();
        assertEquals("user01", created.getUserID());
        assertEquals("A new message", created.getContent());
        assertEquals(1, created.getState());
        assertNotNull(created.getTime());
    }

    @Test
    void sendMessageAllowsEmptyContent() throws Exception {
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        mockMvc.perform(post("/sendMessage")
                .param("userID", "user01")
                .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        verify(messageService).create(messageCaptor.capture());
        assertEquals("", messageCaptor.getValue().getContent());
    }

    @Test
    void modifyMessageResetsStateAndReturnsTrue() throws Exception {
        Message existing = message(1, "user01", 2);
        when(messageService.findById(1)).thenReturn(existing);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", "1")
                .param("content", "Updated text"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(messageService).update(messageCaptor.capture());
        Message updated = messageCaptor.getValue();
        assertEquals("Updated text", updated.getContent());
        assertEquals(1, updated.getState());
        assertNotNull(updated.getTime());
    }

    @Test
    void modifyMessageFailsWhenMessageDoesNotExist() throws Exception {
        when(messageService.findById(999)).thenReturn(null);

        assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", "999")
                .param("content", "Updated text")).andReturn());

        verify(messageService, never()).update(any(Message.class));
    }

    @Test
    void deleteMessageReturnsTrue() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(messageService).delById(1);
    }
}
