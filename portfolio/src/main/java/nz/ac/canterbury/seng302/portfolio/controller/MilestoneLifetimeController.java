package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.Milestone;
import nz.ac.canterbury.seng302.portfolio.service.ElementService;
import nz.ac.canterbury.seng302.portfolio.service.MilestoneService;
import nz.ac.canterbury.seng302.portfolio.service.PermissionService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotAcceptableException;

/**
 * Controller that handles adding and deleting milestone.
 */
@Controller
public class MilestoneLifetimeController {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneLifetimeController.class);

    /**
     * Service for persisting milestone.
     */
    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserAccountClientService userAccountClientService;

    @Autowired
    private ElementService elementService;

    /**
     * Saves the given milestone to the database and redirects the user to the details page.
     * @param milestone new milestone to be saved
     */
    @PostMapping("/add-milestone")
    public String milestoneSave(
            @ModelAttribute("milestone") Milestone milestone,
            @AuthenticationPrincipal AuthState principal,
            Model model,
            HttpServletResponse httpServletResponse
    ) {
        Integer userID = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, userID);

        try {
            milestoneService.validateMilestone(milestone, model);
            if (permissionService.isValidToModify(userID)) {
                milestoneService.addMilestone(milestone);
            }
        } catch (NotAcceptableException e) {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.error(String.format("Error adding milestone: %s", e.getMessage()));
            return "fragments/milestoneModal::milestoneModalBody";
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        return "fragments/milestoneModal::milestoneModalBody";
    }

    /**
     * Tries to delete a milestone with given id.
     * @param id id of milestone being deleted
     */
    @GetMapping("/delete-milestone/{id}")
    public String milestoneRemove(@PathVariable("id") Integer id,
                                  @AuthenticationPrincipal AuthState principal,
                                  Model model) {
        Integer userID = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, userID);
        if (permissionService.isValidToModify(userID)) {
            milestoneService.removeMilestone(id);
        }
        return "redirect:/details";
    }
}
