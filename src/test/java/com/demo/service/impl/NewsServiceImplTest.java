package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NewsServiceImplTest {

    @Mock
    private NewsDao newsDao;

    @InjectMocks
    private NewsServiceImpl newsService;

    @Test
    @DisplayName("findAll: 正常行为，应按给定 pageable 查询分页新闻")
    void findAll_shouldDelegateToDao() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<News> expectedPage = new PageImpl<>(Collections.singletonList(buildNews(1, "title1")));
        when(newsDao.findAll(pageable)).thenReturn(expectedPage);

        Page<News> actual = newsService.findAll(pageable);

        assertSame(expectedPage, actual);
        verify(newsDao).findAll(pageable);
        verifyNoMoreInteractions(newsDao);
    }

    @Test
    @DisplayName("findById: 正常行为，传 newsID 并返回 DAO 结果")
    void findById_shouldDelegateToDao() {
        News expected = buildNews(2, "detail");
        when(newsDao.getOne(2)).thenReturn(expected);

        News actual = newsService.findById(2);

        assertSame(expected, actual);
        verify(newsDao).getOne(2);
        verifyNoMoreInteractions(newsDao);
    }


    /**
     * 透传测试，其它方法同，因此仅针对findByID进行测试用例的编写
     * @param newsId
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    @DisplayName("findById: 对边界风格的 newsID 当前无校验，直接委托 DAO")
    void findById_shouldReturnNullWhenDaoReturnsNull(int newsId) {
        when(newsDao.getOne(newsId)).thenReturn(null);

        News actual = newsService.findById(newsId);

        assertNull(actual);
        verify(newsDao).getOne(newsId);
        verifyNoMoreInteractions(newsDao);
    }

    @Test
    @DisplayName("findById: DAO 抛出异常时，service 应原样向外抛出")
    void findById_shouldPropagateExceptionFromDao() {
        RuntimeException ex = new RuntimeException("dao failure");
        when(newsDao.getOne(3)).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> newsService.findById(3));

        assertSame(ex, thrown);
        verify(newsDao).getOne(3);
        verifyNoMoreInteractions(newsDao);
    }

    @Test
    @DisplayName("create: 正常行为，应保存新闻并返回持久化后的 newsID")
    void create_shouldSaveNewsAndReturnGeneratedId() {
        News input = buildNews(0, "new title");
        News persisted = buildNews(100, "new title");
        when(newsDao.save(input)).thenReturn(persisted);

        int createdId = newsService.create(input);

        assertEquals(100, createdId);
        verify(newsDao).save(input);
        verifyNoMoreInteractions(newsDao);
    }

    @Test
    @DisplayName("delById: 正常行为，应调用 DAO 删除指定新闻")
    void delById_shouldDelegateDelete() {
        newsService.delById(9);

        verify(newsDao).deleteById(9);
        verifyNoMoreInteractions(newsDao);
    }

    @Test
    @DisplayName("update: 正常行为，应保存修改后的新闻对象")
    void update_shouldSaveNews() {
        News news = buildNews(10, "updated title");

        newsService.update(news);

        verify(newsDao).save(news);
        verifyNoMoreInteractions(newsDao);
    }

    private News buildNews(int newsId, String title) {
        return new News(newsId, title, "content-" + title, LocalDateTime.of(2026, 4, 8, 10, 0));
    }
}
