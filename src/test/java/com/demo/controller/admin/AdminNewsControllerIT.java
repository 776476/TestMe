package com.demo.controller.admin;

import com.demo.entity.News;
import com.demo.service.NewsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.news;
import static com.demo.support.ITDataFactory.page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(AdminNewsController.class)
class AdminNewsControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    void newsManageReturnsViewAndTotalPages() throws Exception {
        when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(Arrays.asList(news(1), news(2)), 10, 21));

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void newsAddReturnsView() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    void newsEditReturnsViewForExistingId() throws Exception {
        News existingNews = news(1);
        when(newsService.findById(1)).thenReturn(existingNews);

        mockMvc.perform(get("/news_edit").param("newsID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attribute("news", existingNews));
    }

    @Test
    void newsEditReturnsBadRequestWhenIdIsMissing() throws Exception {
        assertThrows(Exception.class, () -> mockMvc.perform(get("/news_edit")).andReturn());
    }

    @Test
    void newsListReturnsDefaultPage() throws Exception {
        List<News> newsList = Arrays.asList(news(1), news(2));
        when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(newsList, 10, 12));

        mockMvc.perform(get("/newsList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newsID").value(1));
    }

    @Test
    void newsListReturnsRequestedPage() throws Exception {
        when(newsService.findAll(any(Pageable.class))).thenReturn(page(Arrays.asList(news(11)), 1, 10, 12));

        mockMvc.perform(get("/newsList.do").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newsID").value(11));
    }

    @Test
    void deleteNewsReturnsTrue() throws Exception {
        mockMvc.perform(post("/delNews.do").param("newsID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(newsService).delById(1);
    }

    @Test
    void modifyNewsUpdatesExistingRecord() throws Exception {
        News existingNews = news(1);
        when(newsService.findById(1)).thenReturn(existingNews);
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);

        mockMvc.perform(post("/modifyNews.do")
                .param("newsID", "1")
                .param("title", "Updated title")
                .param("content", "Updated content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        verify(newsService).update(newsCaptor.capture());
        assertEquals("Updated title", newsCaptor.getValue().getTitle());
        assertEquals("Updated content", newsCaptor.getValue().getContent());
        assertNotNull(newsCaptor.getValue().getTime());
    }

    @Test
    void addNewsCreatesNewRecord() throws Exception {
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);

        mockMvc.perform(post("/addNews.do")
                .param("title", "New title")
                .param("content", "New content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        verify(newsService).create(newsCaptor.capture());
        assertEquals("New title", newsCaptor.getValue().getTitle());
        assertEquals("New content", newsCaptor.getValue().getContent());
        assertNotNull(newsCaptor.getValue().getTime());
    }
}
