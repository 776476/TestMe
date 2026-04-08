package com.demo.controller;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.message;
import static com.demo.support.ITDataFactory.messageVo;
import static com.demo.support.ITDataFactory.news;
import static com.demo.support.ITDataFactory.venue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(IndexController.class)
class IndexControllerIT {
        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @MockBean
        private VenueService venueService;

        @MockBean
        private MessageVoService messageVoService;

        @MockBean
        private MessageService messageService;

        @Test
        void indexLoadsHomepageData() throws Exception {
                List<Venue> venues = Arrays.asList(venue(1, "Arena A"), venue(2, "Arena B"));
                List<News> newsList = Arrays.asList(news(1), news(2));
                List<Message> messages = Arrays.asList(message(1, "user01", 2), message(2, "user02", 2));
                List<MessageVo> messageVos = Arrays.asList(messageVo(1, "user01", 2), messageVo(2, "user02", 2));
                when(venueService.findAll(any(Pageable.class))).thenReturn(firstPage(venues, 5, venues.size()));
                when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(newsList, 5, newsList.size()));
                when(messageService.findPassState(any(Pageable.class)))
                                .thenReturn(firstPage(messages, 5, messages.size()));
                when(messageVoService.returnVo(messages)).thenReturn(messageVos);

                mockMvc.perform(get("/index"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("user", nullValue()))
                                .andExpect(model().attribute("news_list", newsList))
                                .andExpect(model().attribute("venue_list", venues))
                                .andExpect(model().attribute("message_list", messageVos));
        }

        @Test
        void indexHandlesEmptyDataSets() throws Exception {
                Page<Venue> emptyVenues = firstPage(Collections.emptyList(), 5, 0);
                Page<News> emptyNews = firstPage(Collections.emptyList(), 5, 0);
                Page<Message> emptyMessages = firstPage(Collections.emptyList(), 5, 0);
                when(venueService.findAll(any(Pageable.class))).thenReturn(emptyVenues);
                when(newsService.findAll(any(Pageable.class))).thenReturn(emptyNews);
                when(messageService.findPassState(any(Pageable.class))).thenReturn(emptyMessages);
                when(messageVoService.returnVo(Collections.emptyList())).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/index"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("news_list", empty()))
                                .andExpect(model().attribute("venue_list", empty()))
                                .andExpect(model().attribute("message_list", empty()));
        }

        @Test
        void indexLimitsHomepageListsToFirstPage() throws Exception {
                List<Venue> venues = Arrays.asList(
                                venue(1, "Arena A"),
                                venue(2, "Arena B"),
                                venue(3, "Arena C"),
                                venue(4, "Arena D"),
                                venue(5, "Arena E"));
                List<News> newsList = Arrays.asList(news(1), news(2), news(3), news(4), news(5));
                List<Message> messages = Arrays.asList(
                                message(1, "user01", 2),
                                message(2, "user02", 2),
                                message(3, "user03", 2),
                                message(4, "user04", 2),
                                message(5, "user05", 2));
                List<MessageVo> messageVos = Arrays.asList(
                                messageVo(1, "user01", 2),
                                messageVo(2, "user02", 2),
                                messageVo(3, "user03", 2),
                                messageVo(4, "user04", 2),
                                messageVo(5, "user05", 2));
                when(venueService.findAll(any(Pageable.class))).thenReturn(firstPage(venues, 5, 12));
                when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(newsList, 5, 12));
                when(messageService.findPassState(any(Pageable.class))).thenReturn(firstPage(messages, 5, 12));
                when(messageVoService.returnVo(messages)).thenReturn(messageVos);

                mockMvc.perform(get("/index"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("venue_list", venues))
                                .andExpect(model().attribute("news_list", newsList))
                                .andExpect(model().attribute("message_list", messageVos));
        }

        @Test
        void adminIndexReturnsAdminView() throws Exception {
                mockMvc.perform(get("/admin_index"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/admin_index"));
        }
}
