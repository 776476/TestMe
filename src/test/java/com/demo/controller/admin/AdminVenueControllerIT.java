package com.demo.controller.admin;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.demo.support.ITDataFactory.firstPage;
import static com.demo.support.ITDataFactory.page;
import static com.demo.support.ITDataFactory.venue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminVenueController.class)
class AdminVenueControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @Test
    void venueManageReturnsViewAndTotalPages() throws Exception {
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(firstPage(Arrays.asList(venue(1, "Arena A")), 10, 21));

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attribute("total", 3));
    }

    @Test
    void venueEditReturnsViewForExistingVenue() throws Exception {
        Venue existingVenue = venue(1, "Arena A");
        when(venueService.findByVenueID(1)).thenReturn(existingVenue);

        mockMvc.perform(get("/venue_edit").param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attribute("venue", existingVenue));
    }

    @Test
    void venueEditReturnsBadRequestWhenIdIsMissing() throws Exception {
        assertThrows(Exception.class, () -> mockMvc.perform(get("/venue_edit")).andReturn());
    }

    @Test
    void venueAddReturnsView() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    void getVenueListReturnsDefaultPage() throws Exception {
        List<Venue> venues = Arrays.asList(venue(1, "Arena A"), venue(2, "Arena B"));
        when(venueService.findAll(any(Pageable.class))).thenReturn(firstPage(venues, 10, 12));

        mockMvc.perform(get("/venueList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueID").value(1));
    }

    @Test
    void getVenueListReturnsRequestedPage() throws Exception {
        when(venueService.findAll(any(Pageable.class)))
                .thenReturn(page(Arrays.asList(venue(11, "Arena K")), 1, 10, 12));

        mockMvc.perform(get("/venueList.do").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueID").value(11));
    }

    @Test
    void addVenueSavesUploadedPictureAndRedirectsToManagePage() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "arena.png", "image/png", "abc".getBytes());
        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        when(venueService.create(any(Venue.class))).thenReturn(1);

        mockMvc.perform(multipart("/addVenue.do")
                .file(picture)
                .param("venueName", "Arena A")
                .param("address", "Address A")
                .param("description", "Description A")
                .param("price", "120")
                .param("open_time", "08:00")
                .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).create(venueCaptor.capture());
        assertEquals("Arena A", venueCaptor.getValue().getVenueName());
        assertTrue(venueCaptor.getValue().getPicture().startsWith("file/venue/"));
    }

    @Test
    void addVenueUsesEmptyPictureWhenUploadIsMissing() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        when(venueService.create(any(Venue.class))).thenReturn(1);

        mockMvc.perform(multipart("/addVenue.do")
                .file(picture)
                .param("venueName", "Arena A")
                .param("address", "Address A")
                .param("description", "Description A")
                .param("price", "120")
                .param("open_time", "08:00")
                .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).create(venueCaptor.capture());
        assertEquals("", venueCaptor.getValue().getPicture());
    }

    @Test
    void addVenueRedirectsBackToAddPageWhenCreateFails() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        when(venueService.create(any(Venue.class))).thenReturn(0);

        mockMvc.perform(multipart("/addVenue.do")
                .file(picture)
                .param("venueName", "Arena A")
                .param("address", "Address A")
                .param("description", "Description A")
                .param("price", "120")
                .param("open_time", "08:00")
                .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_add"));
    }

    @Test
    void modifyVenueReplacesPictureWhenUploadIsProvided() throws Exception {
        Venue existingVenue = venue(1, "Arena A");
        MockMultipartFile picture = new MockMultipartFile("picture", "arena.png", "image/png", "abc".getBytes());
        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        when(venueService.findByVenueID(1)).thenReturn(existingVenue);

        mockMvc.perform(multipart("/modifyVenue.do")
                .file(picture)
                .param("venueID", "1")
                .param("venueName", "Arena A Updated")
                .param("address", "Address B")
                .param("description", "Description B")
                .param("price", "180")
                .param("open_time", "09:00")
                .param("close_time", "23:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).update(venueCaptor.capture());
        assertEquals("Arena A Updated", venueCaptor.getValue().getVenueName());
        assertTrue(venueCaptor.getValue().getPicture().startsWith("file/venue/"));
    }

    @Test
    void modifyVenueKeepsPictureWhenUploadIsMissing() throws Exception {
        Venue existingVenue = venue(1, "Arena A");
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        when(venueService.findByVenueID(1)).thenReturn(existingVenue);

        mockMvc.perform(multipart("/modifyVenue.do")
                .file(picture)
                .param("venueID", "1")
                .param("venueName", "Arena A Updated")
                .param("address", "Address B")
                .param("description", "Description B")
                .param("price", "180")
                .param("open_time", "09:00")
                .param("close_time", "23:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).update(venueCaptor.capture());
        assertEquals("file/venue/venue-1.png", venueCaptor.getValue().getPicture());
    }

    @Test
    void deleteVenueReturnsTrue() throws Exception {
        mockMvc.perform(post("/delVenue.do").param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(venueService).delById(1);
    }

    @Test
    void checkVenueNameReturnsTrueWhenValueIsAvailable() throws Exception {
        when(venueService.countVenueName("Arena Z")).thenReturn(0);

        mockMvc.perform(post("/checkVenueName.do").param("venueName", "Arena Z"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkVenueNameReturnsFalseWhenValueAlreadyExists() throws Exception {
        when(venueService.countVenueName("Arena A")).thenReturn(1);

        mockMvc.perform(post("/checkVenueName.do").param("venueName", "Arena A"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
