package nz.ac.canterbury.seng302.portfolio.controller.rest;

import nz.ac.canterbury.seng302.portfolio.controller.CalendarController;
import nz.ac.canterbury.seng302.portfolio.service.DateValidationService;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Unit tests for SprintLifetimeRestController.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SprintLifetimeRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class SprintLifetimeRestControllerTest {

    @Autowired
    private MockMvc mockMvc = MockMvcBuilders.standaloneSetup(SprintLifetimeRestController.class).build();

    @MockBean
    private DateValidationService dateValidationService;

    /**
     * Needed so tests can load application context.
     */
    @MockBean
    private UserAccountClientService userAccountClientService;

    /**
     * Checks that the rest controller returns a response of the concatenation of the date validation methods it calls.
     * @throws Exception when an exception is thrown while performing the post request
     */
    @Test
    void testAddSprintErrorMessage() throws Exception {
        when(dateValidationService.validateDateRangeNotEmpty(anyString(), anyString())).thenReturn("1");
        when(dateValidationService.validateStartDateNotAfterEndDate(anyString(), anyString())).thenReturn("2");
        when(dateValidationService.validateSprintDateRange(anyString(), anyString(), anyInt())).thenReturn("3");
        when(dateValidationService.validateDatesInProjectDateRange(anyString(), anyString())).thenReturn("4");
        mockMvc.perform(get("/add-sprint/error")
                        .param("id", "1")
                        .param("sprintStartDate", "2001-10-20")
                        .param("sprintEndDate", "2001-10-21"))
                .andExpect(content().string("1 2 3 4"));
    }
}