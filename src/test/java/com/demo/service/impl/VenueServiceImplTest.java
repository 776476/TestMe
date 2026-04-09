package com.demo.service.impl;

import com.demo.dao.VenueDao;
import com.demo.entity.Venue;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VenueServiceImplTest {

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private VenueServiceImpl venueService;

    @Test
    @DisplayName("findByVenueID: 应透传场馆 ID 并返回 DAO 结果")
    void findByVenueID_shouldDelegateToDao() {
        Venue expected = buildVenue(1, "Arena A", 100);
        when(venueDao.getOne(1)).thenReturn(expected);

        Venue actual = venueService.findByVenueID(1);

        assertSame(expected, actual);
        verify(venueDao).getOne(1);
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("findByVenueName: 应根据场馆名查询场馆")
    void findByVenueName_shouldDelegateToDao() {
        Venue expected = buildVenue(2, "Arena B", 120);
        when(venueDao.findByVenueName("Arena B")).thenReturn(expected);

        Venue actual = venueService.findByVenueName("Arena B");

        assertSame(expected, actual);
        verify(venueDao).findByVenueName("Arena B");
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("findAll(Pageable): 应按 pageable 查询分页场馆")
    void findAllPageable_shouldDelegateToDao() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Venue> expectedPage = new PageImpl<>(Collections.singletonList(buildVenue(3, "Arena C", 130)));
        when(venueDao.findAll(pageable)).thenReturn(expectedPage);

        Page<Venue> actual = venueService.findAll(pageable);

        assertSame(expectedPage, actual);
        verify(venueDao).findAll(pageable);
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("findAll(): 应返回全部场馆列表")
    void findAll_shouldDelegateToDao() {
        List<Venue> expected = Arrays.asList(
                buildVenue(4, "Arena D", 140),
                buildVenue(5, "Arena E", 150)
        );
        when(venueDao.findAll()).thenReturn(expected);

        List<Venue> actual = venueService.findAll();

        assertSame(expected, actual);
        verify(venueDao).findAll();
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("create: 应保存场馆并返回持久化后的 venueID")
    void create_shouldSaveVenueAndReturnGeneratedId() {
        Venue input = buildVenue(0, "Arena F", 160);
        Venue persisted = buildVenue(100, "Arena F", 160);
        when(venueDao.save(input)).thenReturn(persisted);

        int createdId = venueService.create(input);

        assertEquals(100, createdId);
        verify(venueDao).save(input);
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("update: 应保存修改后的场馆对象")
    void update_shouldSaveVenue() {
        Venue venue = buildVenue(6, "Arena G", 170);

        venueService.update(venue);

        verify(venueDao).save(venue);
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("delById: 应调用 DAO 删除指定场馆")
    void delById_shouldDelegateDelete() {
        venueService.delById(7);

        verify(venueDao).deleteById(7);
        verifyNoMoreInteractions(venueDao);
    }

    @Test
    @DisplayName("countVenueName: 应返回指定场馆名的计数结果")
    void countVenueName_shouldDelegateToDao() {
        when(venueDao.countByVenueName("Arena H")).thenReturn(2);

        int result = venueService.countVenueName("Arena H");

        assertEquals(2, result);
        verify(venueDao).countByVenueName("Arena H");
        verifyNoMoreInteractions(venueDao);
    }

    private Venue buildVenue(int venueId, String venueName, int price) {
        Venue venue = new Venue();
        venue.setVenueID(venueId);
        venue.setVenueName(venueName);
        venue.setDescription("desc-" + venueName);
        venue.setPrice(price);
        venue.setPicture("file/venue/" + venueName + ".png");
        venue.setAddress("address-" + venueName);
        venue.setOpen_time("08:00");
        venue.setClose_time("22:00");
        return venue;
    }
}
