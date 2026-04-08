package com.demo.controller.admin;

import com.demo.entity.Message;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.message;
import static com.demo.support.ITDataFactory.messageVo;
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

@WebMvcTest(AdminMessageController.class)
class AdminMessageControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    @Test
    void messageManageReturnsViewAndTotalPages() throws Exception {
        when(messageService.findWaitState(any(Pageable.class)))
                .thenReturn(firstPage(Arrays.asList(message(1, "user01", 1)), 10, 21));

        mockMvc.perform(get("/message_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/message_manage"))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void messageListUsesDefaultPage() throws Exception {
        List<Message> messages = Arrays.asList(message(1, "user01", 1), message(2, "user02", 1));
        List<MessageVo> messageVos = Arrays.asList(messageVo(1, "user01", 1), messageVo(2, "user02", 1));
        when(messageService.findWaitState(any(Pageable.class))).thenReturn(firstPage(messages, 10, 12));
        when(messageVoService.returnVo(messages)).thenReturn(messageVos);

        mockMvc.perform(get("/messageList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(1))
                .andExpect(jsonPath("$[1].messageID").value(2));
    }

    @Test
    void messageListReturnsRequestedPage() throws Exception {
        List<Message> messages = Arrays.asList(message(11, "user11", 1));
        List<MessageVo> messageVos = Arrays.asList(messageVo(11, "user11", 1));
        when(messageService.findWaitState(any(Pageable.class))).thenReturn(page(messages, 1, 10, 12));
        when(messageVoService.returnVo(messages)).thenReturn(messageVos);

        mockMvc.perform(get("/messageList.do").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(11));
    }

    @Test
    void passMessageReturnsTrue() throws Exception {
        mockMvc.perform(post("/passMessage.do").param("messageID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(messageService).confirmMessage(1);
    }

    @Test
    void passMessageFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("missing message")).when(messageService).confirmMessage(999);

        assertThrows(Exception.class,
                () -> mockMvc.perform(post("/passMessage.do").param("messageID", "999")).andReturn());
    }

    @Test
    void rejectMessageReturnsTrue() throws Exception {
        mockMvc.perform(post("/rejectMessage.do").param("messageID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(messageService).rejectMessage(1);
    }

    @Test
    void rejectMessageFailsWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("missing message")).when(messageService).rejectMessage(999);

        assertThrows(Exception.class,
                () -> mockMvc.perform(post("/rejectMessage.do").param("messageID", "999")).andReturn());
    }

    @Test
    void deleteMessageReturnsTrue() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(messageService).delById(1);
    }
}
