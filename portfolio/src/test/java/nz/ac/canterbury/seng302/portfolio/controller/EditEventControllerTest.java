package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.Event;
import nz.ac.canterbury.seng302.portfolio.service.DateValidationService;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.time.LocalTime;
import java.util.Date;

import static nz.ac.canterbury.seng302.portfolio.controller.SprintLifetimeController.getUpdatedDate;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * Junit testing to test the Edit Event Controller
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = EditEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EditEventControllerTest {

    /**
     * AuthState object to be used when we mock security context
     */
    public AuthState validAuthState = AuthState.newBuilder()
            .setIsAuthenticated(true)
            .setNameClaimType("name")
            .setRoleClaimType("role")
            .addClaims(ClaimDTO.newBuilder().setType("role").setValue("ADMIN").build()) // Set the mock user's role
            .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue("123456").build()) // Set the mock user's ID
            .setAuthenticationType("AuthenticationTypes.Federation")
            .setName("validtesttoken")
            .build();

    /**
     * Create mockEvent to be returned when a function return an Event object
     */
    private Date startEvent = new Date();
    private Date endEvent = getUpdatedDate(startEvent, 5, 0);
    private LocalTime startTime = LocalTime.now();
    private LocalTime endTime = LocalTime.now();
    private Event mockEvent = new Event(1,0,"test event", startEvent, endEvent,startTime, endTime);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserAccountClientService userAccountClientService;

    @MockBean
    private DateValidationService dateValidationService;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(AccountController.class).build();
    }

    /**
     * unit testing to test the get method when calling "/edit-event/{id}"
     * Expect to return 200 status code and edit event page with some event's information in the model
     */
    @Test
    void whenBrowseToEditEventPage_showEditEventPage_andReturn200StatusCode() throws Exception {
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        when(mockedSecurityContext.getAuthentication()).thenReturn(new PreAuthenticatedAuthenticationToken(validAuthState, ""));

        SecurityContextHolder.setContext(mockedSecurityContext);

        when(eventService.getEventById(any(Integer.class))).thenReturn(mockEvent);

        mockMvc.perform(get("/edit-event/1"))
                .andExpect(status().isOk()) // Whether to return the status "200 OK"
                .andExpect(view().name("editEvent")) // Whether to return the template "account"
                .andExpect(model().attribute("event", mockEvent))
                .andExpect(model().attribute("EventDateError", ""))
                .andExpect(model().attribute("eventId", 1));
    }

    /**
     * unit testing to test the get method when calling "/edit-event/{id}"
     * Expect to return 302 status code and project details page
     * This test also verifies some function is called
     */
    @Test
    void whenEditEventRequestIsSent_thenRedirectToDetailsPage_andReturn200StatusCode() throws Exception {
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        when(mockedSecurityContext.getAuthentication()).thenReturn(new PreAuthenticatedAuthenticationToken(validAuthState, ""));

        SecurityContextHolder.setContext(mockedSecurityContext);

        when(eventService.getEventById(any(Integer.class))).thenReturn(mockEvent);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        when(eventService.updateEvent(any(Event.class))).thenReturn(mockEvent);


        mockMvc.perform(post("/edit-event/1").flashAttr("event",mockEvent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/details")); // Whether to return the status "200 OK";
        Mockito.verify(eventService).updateEvent(eventArgumentCaptor.capture());
    }
}