package com.demo.controller.user;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
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
import static com.demo.support.ITDataFactory.page;
import static com.demo.support.ITDataFactory.venue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(VenueController.class)
class VenueControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;

    @Test
    void venueLoadsDetailsForExistingId() throws Exception {
        Venue venue = venue(1, "Arena A");
        when(venueService.findByVenueID(1)).thenReturn(venue);

        mockMvc.perform(get("/venue").param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void venueReturnsBadRequestWhenIdIsMissing() throws Exception {
        assertThrows(NestedServletException.class, () -> mockMvc.perform(get("/venue")).andReturn());
    }

    @Test
    void getVenueListUsesDefaultPageWhenPageIsMissing() throws Exception {
        List<Venue> venues = Arrays.asList(venue(1, "Arena A"), venue(2, "Arena B"));
        when(venueService.findAll(any(Pageable.class))).thenReturn(firstPage(venues, 5, 7));

        mockMvc.perform(get("/venuelist/getVenueList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].venueID").value(1))
                .andExpect(jsonPath("$.content[1].venueID").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getVenueListReturnsRequestedPage() throws Exception {
        List<Venue> venues = Arrays.asList(venue(6, "Arena F"), venue(7, "Arena G"));
        when(venueService.findAll(any(Pageable.class))).thenReturn(page(venues, 1, 5, 7));

        mockMvc.perform(get("/venuelist/getVenueList").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].venueID").value(6))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void venueListReturnsListViewAndTotalPages() throws Exception {
        List<Venue> venues = Arrays.asList(venue(1, "Arena A"), venue(2, "Arena B"));
        when(venueService.findAll(any(Pageable.class))).thenReturn(firstPage(venues, 5, 11));

        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", venues))
                .andExpect(model().attribute("total", 3));
    }
}
