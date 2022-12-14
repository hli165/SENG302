package nz.ac.canterbury.seng302.portfolio.controller;

import com.google.protobuf.Timestamp;
import nz.ac.canterbury.seng302.portfolio.model.*;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.utility.ToastUtility;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Controller for the display project details page
 */
@Controller
public class DetailsController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SprintService sprintService;

    @Autowired
    private UserAccountClientService userAccountClientService;

    @Autowired
    private ElementService elementService;

    @Autowired
    private EventService eventService;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private DeadlineService deadlineService;

    @Autowired
    private RegisterClientService registerClientService;

    /**
     * Holds list of events information for displaying.
     */
    private final ArrayList<NotificationResponse> eventsToDisplay = new ArrayList<>();

    /**
     * Holds the number of toasts to be generated in the HTML. Must be the same as or greater than NUM_OF_TOASTS in DetailsLive.js and
     * GroupsLive.js.
     */
    public static final int NUM_OF_TOASTS = 3;


    /***
     * GET request method, followed by the request URL(../details)
     *
     * @param principal For getting the user ID
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @return projectDetails page or throw exception
     */
    @GetMapping("/details")
    public String details(@AuthenticationPrincipal AuthState principal,
                          Model model,
                          HttpServletRequest request
                          ) {
        /* Add project details to the model */
        // Gets the project with id 0 to plonk on the page
        Project project;
        try {
            project = projectService.getProjectById(0);
        } catch (ObjectNotFoundException e) {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            Instant time = Instant.now();
            Timestamp dateAdded = Timestamp.newBuilder().setSeconds(time.getEpochSecond()).build();
            LocalDate endDate = time.atZone(ZoneId.systemDefault()).toLocalDate();
            Date date8Months = java.sql.Date.valueOf(endDate.plusMonths(8));  // 8 months after the current date
            project = new Project(
                    "Project " + currentYear,
                    "Default Project",
                    new Date(dateAdded.getSeconds() * 1000),
                    date8Months
            );
            project.setId(0);
            try {
                projectService.saveProject(project);
            } catch (Exception err) {
                return "redirect:account";
            }

        }

        model.addAttribute("project", project);

        List<Sprint> sprintList = sprintService.getAllSprintsOrderedWithColour();
        List<Event> eventList = eventService.getAllEventsOrderedWithColour(sprintList);

        model.addAttribute("sprints", sprintList);
        model.addAttribute("events", eventList);

        List<List<Event>> eventsForSprints = getAllEventsForAllSprints(sprintList);
        List<List<Deadline>> deadlinesForSprints = getAllDeadlinesForAllSprints(sprintList);
        model.addAttribute("eventsForSprints", eventsForSprints);
        model.addAttribute("deadlinesForSprints", deadlinesForSprints);

        List<List<Milestone>> milestonesForSprints = getAllMilestonesForAllSprints(sprintList);
        model.addAttribute("milestonesForSprints", milestonesForSprints);

        List<Milestone> milestoneList = milestoneService.getAllEventsOrderedWithColour(sprintList);
        model.addAttribute("milestones", milestoneList);

        List<Deadline> deadlineList = deadlineService.getAllDeadlinesOrderedWithColour(sprintList);
        model.addAttribute("deadlines", deadlineList);
        UserResponse getUserByIdReply;
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);

        ToastUtility.addToastsToModel(model, eventsToDisplay, NUM_OF_TOASTS);

        getUserByIdReply = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(getUserByIdReply);
        model.addAttribute("currentUserRole", role);

        model.addAttribute("userId", id);
        UserResponse user = registerClientService.getUserData(id);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userFirstName", user.getFirstName());
        model.addAttribute("userLastName", user.getLastName());

        model.addAttribute("newMilestone", new Milestone(0, "", new Date()));

        model.addAttribute("newDeadline", new Deadline(0, "", new Date()));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 3);
        model.addAttribute("newEvent", new Event(0, "", new Date(), calendar.getTime()));

        elementService.addDeniedMessage(model, request);

        model.addAttribute("newSprint", sprintService.getSuggestedSprint());
        model.addAttribute("sprintDateError", "");


        return "projectDetails";
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint.
     * @param message NotificationMessage that holds information about the artefact being updated
     * @return returns an NotificationResponse that holds information about the event being updated.
     */
    @MessageMapping("/editing-artefact")
    @SendTo("/webSocketGet/being-edited")
    public NotificationResponse updatingArtefact(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "edit");
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This is triggered when the user is no longer editing.
     * @param message Information about the editing state.
     * @return Returns the message given.
     */
    @MessageMapping("/stop-editing-artefact")
    @SendTo("/webSocketGet/stop-being-edited")
    public NotificationResponse stopUpdatingArtefact(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "edit");
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This method also triggers some sort of re-render of the events.
     * @param message NotificationMessage that holds information about the artefact being updated
     * @return returns an NotificationResponse that holds information about the event being updated.
     */
    @MessageMapping("/saved-edited-artefact")
    @SendTo("/webSocketGet/artefact-save")
    public NotificationResponse savingUpdatedArtefact(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "save");
        // Trigger reload and save the last event's information
        eventsToDisplay.add(response);
        while (eventsToDisplay.size() > NUM_OF_TOASTS) {
            eventsToDisplay.remove(0);
        }
        return response;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when an artefact is added.
     * @param message NotificationMessage that holds information about the artefact being added
     * @return returns an NotificationResponse that holds information about the artefact being added.
     */
    @MessageMapping("/added-artefact")
    @SendTo("/webSocketGet/artefact-add")
    public NotificationResponse addingArtefact(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "add");
        // Trigger reload and save the last event's information
        eventsToDisplay.add(response);
        while (eventsToDisplay.size() > NUM_OF_TOASTS) {
            eventsToDisplay.remove(0);
        }
        return response;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when an artefact is deleted.
     * @param message NotificationMessage that holds information about the artefact being deleted
     * @return returns an NotificationResponse that holds information about the artefact being deleted.
     */
    @MessageMapping("/deleted-artefact")
    @SendTo("/webSocketGet/artefact-delete")
    public NotificationResponse deletingArtefact(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "delete");
        // Trigger reload and save the last event's information
        eventsToDisplay.add(response);
        while (eventsToDisplay.size() > NUM_OF_TOASTS) {
            eventsToDisplay.remove(0);
        }
        return response;
    }

    /**
     * Tells the calendar that sprints or projects have been updated on the details page.
     * Only sends a string because the calendar has no implementation to show the changes of the project and
     * sprints other than reloading.
     * @param message The message to send to the calendar
     * @return The message to send to the calendar
     */
    @MessageMapping("/sprint-project-details-save")
    @SendTo("/webSocketGet/sprint-project-details-save")
    public String sprintProjectDetailsChange(String message) {
        return message;
    }

    /**
     * Tells the details page that a sprint has been updated.
     * @param message The message to send to the details page containing the sprint.
     * @return The message to send to the details page containing the sprint.
     */
    @MessageMapping("/sprint-project-calendar-save")
    @SendTo("/webSocketGet/sprint-project-calendar-save")
    public NotificationResponse sprintProjectCalendarChange(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "save");
        eventsToDisplay.add(response);
        while (eventsToDisplay.size() > NUM_OF_TOASTS) {
            eventsToDisplay.remove(0);
        }
        return response;
    }

    /**
     * Gets a list where each element is a list of events that is a part of the sprint from sprintList with the same
     * index.
     * @param sprintList List of sprints to get the events of.
     * @return List of lists of events that are within their given sprint.
     */
    private List<List<Event>> getAllEventsForAllSprints(List<Sprint> sprintList) {
        List<List<Event>> allEventsList = new ArrayList<>();

        for (Sprint sprint : sprintList) {
            allEventsList.add(eventService.getAllEventsOverlappingWithSprint(sprint));
        }

        return allEventsList;
    }

    /**
     * Get a list where each element is a list of deadline that is a part of the sprint from sprintList with the same index
     * @param sprintList List of sprints to get the deadlines of.
     * @return List of lists of deadlines that are within their given sprint
     */
    private List<List<Deadline>> getAllDeadlinesForAllSprints(List<Sprint> sprintList) {
        List<List<Deadline>> allDeadlinesList = new ArrayList<>();

        for (Sprint sprint : sprintList) {
            allDeadlinesList.add(deadlineService.getAllDeadlinesOverLappingWithSprint(sprint));
        }

        return allDeadlinesList;
    }

    /**
     * Gets a list where each element is a list of milestones that is a part of the sprint from sprintList with the same
     * index.
     * @param sprintList List of sprints to get the milestones of.
     * @return List of lists of milestones that are within their given sprint.
     */
    private List<List<Milestone>> getAllMilestonesForAllSprints(List<Sprint> sprintList) {
        List<List<Milestone>> allMilestonesList = new ArrayList<>();

        for (Sprint sprint : sprintList) {
            allMilestonesList.add(milestoneService.getAllMilestonesOverlappingWithSprint(sprint));
        }

        return allMilestonesList;
    }

}
