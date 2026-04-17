package com.demo.controller.user;

import com.demo.entity.News;
import com.demo.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.news;
import static com.demo.support.ITDataFactory.page;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NewsController.class)
class NewsControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Test
    void newsLoadsDetailsForExistingId() throws Exception {
        News news = news(1);
        when(newsService.findById(1)).thenReturn(news);

        mockMvc.perform(get("/news").param("newsID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attribute("news", news));
    }

    @Test
    void newsReturnsBadRequestWhenIdIsMissing() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/news")).andReturn());
    }

    @Test
    void getNewsListUsesDefaultPageWhenPageIsMissing() throws Exception {
        List<News> newsList = Arrays.asList(news(1), news(2));
        when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(newsList, 5, 7));

        mockMvc.perform(get("/news/getNewsList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsID").value(1))
                .andExpect(jsonPath("$.content[1].newsID").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getNewsListReturnsRequestedPage() throws Exception {
        List<News> newsList = Arrays.asList(news(6), news(7));
        when(newsService.findAll(any(Pageable.class))).thenReturn(page(newsList, 1, 5, 7));

        mockMvc.perform(get("/news/getNewsList").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsID").value(6))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void newsListReturnsListViewAndTotalPages() throws Exception {
        List<News> newsList = Arrays.asList(news(1), news(2));
        when(newsService.findAll(any(Pageable.class))).thenReturn(firstPage(newsList, 5, 11));

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", newsList))
                .andExpect(model().attribute("total", 3));
    }
}
