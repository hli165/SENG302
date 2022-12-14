package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.*;
import nz.ac.canterbury.seng302.portfolio.service.ElementService;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotAcceptableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Controller for evidence endpoints.
 */
@Controller
public class EvidenceController {

    Logger logger = LoggerFactory.getLogger(EvidenceController.class);

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RegisterClientService registerClientService;

    @Autowired
    private ElementService elementService;

    @Autowired
    private TagService tagService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserAccountClientService userAccountClientService;

    private static final String ADD_EVIDENCE_MODAL_FRAGMENT = "fragments/evidenceModal::evidenceModalBody";

    public static final String ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE = "evidenceTitleAlertMessage";

    public static final String ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE = "evidenceDescriptionAlertMessage";

    public static final String ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE = "evidenceDateAlertMessage";

    public static final String ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE = "evidenceWebLinksAlertMessage";

    public static final String ADD_EVIDENCE_MODAL_FRAGMENT_SKILL_TAGS_MESSAGE = "evidenceSkillTagsAlertMessage";

    public static final String ACCOUNT_EVIDENCE = "fragments/evidenceList::evidenceList";


    /**
     * Method tries to add and save the new evidence piece to the database.
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @param httpServletResponse for adding status codes to
     * @return redirect user to evidence tab, or keep up modal if there are errors
     */
    @PostMapping("/add-evidence")
    public String addEvidence(
            @ModelAttribute("evidence") Evidence evidence,
            Model model,
            HttpServletResponse httpServletResponse,
            @AuthenticationPrincipal AuthState principal
    ) {
        try {
            evidenceService.validateEvidence(evidence, model);
            boolean wasAdded = evidenceService.addEvidence(evidence);
            if (wasAdded) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            } else {
                String errorMessage = "Evidence Not Added. Saving Error Occurred.";
                model.addAttribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, errorMessage);
                httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (NotAcceptableException e) {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.error("Attributes of evidence not formatted correctly. Not adding evidence. ");
        }

        return ADD_EVIDENCE_MODAL_FRAGMENT;
    }

    /**
     * Method for getting all highfivers IDs from a list of evidences.
     * This method filters so that only evidences with a given userID are used.
     * @param evidences The list of evidences.
     * @param id    The id of the user who made the evidence.
     * @return  A list of all highfivers id found from valid evidences.
     */
    private List<Integer> getHighFiveIds(List<Evidence> evidences, int id) {
        List<Integer> ids = new ArrayList<>();
        if (evidences != null) {
            for (Evidence eachEvidence:evidences) {
                if (eachEvidence.getHighFivers().stream().map(HighFivers::getUserId).anyMatch(x -> x.equals(id))) {
                    ids.add(eachEvidence.getEvidenceId());
                }
            }
        }
        return ids;
    }

    /**
     * Method to display the main page for viewing skill or category specific pieces of evidence.
     * @param model         Parameters sent to thymeleaf template to be rendered into HTML.
     * @param principal     Used for authentication of a user.
     * @param viewedUserId  The id of the current user.
     * @param tagId         The id of the current tag being searched for.
     * @param tagType       The type of tag (category or skill) being searched for.
     * @return              redirect user to evidence tab, or keep up modal if there are errors.
     */
    @GetMapping("/evidence-tags")
    public String evidenceSkillPage(
            Model model,
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(value = "userId") int viewedUserId,
            @RequestParam(value = "tagId") int tagId,
            @RequestParam(value = "tagType") String tagType
    ) {
        // This code is duplicated in the update evidence list section as this method needs both the userAccount and
        // the returned ID while the swap list method needs only the returned ID.
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);
        UserResponse userAccount = registerClientService.getUserData(viewedUserId);
        // This is done as the UserResponse has a default of 0 for ID when the user doesn't exist. Note the database needs to not have an ID of 0.
        int returnId = ((userAccount.getId() == 0) ?  id : userAccount.getId());

        List<Evidence> evidenceList;
        String tagName;

        try {
            if (Objects.equals(tagType, "Skills")) {
                if (tagId == -1) { // No Skills
                    evidenceList = evidenceService.getEvidencesWithoutSkills();
                    tagName = "No Skills";
                } else {
                    evidenceList = evidenceService.getEvidencesWithSkill(tagId);
                    Tag skillTag = tagService.getTag(tagId);
                    if (skillTag == null) {  // Invalid Skill
                        throw new NullPointerException("Invalid Skill Id");
                    }
                    tagName = skillTag.getSpacedTagName();
                }
            } else if (Objects.equals(tagType, "Categories")) {
                evidenceList = evidenceService.getEvidencesWithCategory(tagId);
                Category category = categoryService.getCategory(tagId);
                if (category == null) { // Invalid Category
                    throw new NullPointerException("Invalid Category Id");
                }
                tagName = category.getCategoryName();
            } else { // Invalid parameter given.
                return "redirect:account?userId=" + returnId;
            }
        } catch (NullPointerException ignore) { // Either exception from getting evidence or an invalid tag id.
            return "redirect:account?userId=" + returnId;
        }

        String userName = registerClientService.getUserData(returnId).getUsername();
        evidenceService.addUserDataToEvidence(evidenceList);
        elementService.addRoles(model, registerClientService.getUserData(id));
        model.addAttribute("userName", ((userName.toLowerCase(Locale.ROOT).endsWith("s")) ? userName + "'" : userName + "'s"));
        model.addAttribute("tagType", tagType);
        model.addAttribute("evidencesExists", ((evidenceList != null) && (!evidenceList.isEmpty())));
        model.addAttribute("evidences", evidenceList);
        model.addAttribute("tagName", tagName);
        model.addAttribute("tagId", tagId);
        model.addAttribute("viewableUser", returnId);
        model.addAttribute("validViewedUser", (userAccount.getId() != 0));
        model.addAttribute("allSkills", tagService.getTagsSortedList());
        model.addAttribute("allCategories", categoryService.getAllCategories());
        model.addAttribute("evidenceHighFivedIds", getHighFiveIds(evidenceList, id));
        // This id should be the same as the userId stored in the header but for redundancy and safety this variable is also used.
        model.addAttribute("currentUserId", id);

        return "evidence";
    }

    /**
     * Method to handle a partial refresh of the list of evidences to either display all the evidence with a given skill or Category ID
     * or all evidence with both a given skill or Category ID and user ID attached to it.
     * @param model         Parameters sent to thymeleaf template to be rendered into HTML
     * @param userId        The user currently logged in.
     * @param viewedUserId  The user that should be attached to the evidence.
     * @param listAll       A boolean value on if all or only a certain viewedUsers evidence should be returned.
     * @param tagId         The tag (skill or category) that needs to be attached to the evidence.
     * @param tagType       The type of tag either skill or category
     * @return A fragment of the list of evidences to display.
     */
    @GetMapping("/switch-evidence-list")
    public String membersWithoutAGroupCard(
            Model model,
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(value = "userId") int userId,
            @RequestParam(value = "viewedUserId") int viewedUserId,
            @RequestParam(value = "listAll") boolean listAll,
            @RequestParam(value = "tagId") int tagId,
            @RequestParam(value = "tagType") String tagType
    ) {
        // This code is duplicated in the update evidence list section as this method needs only
        // the returned ID while the main evidence page method need the UserAccount and the returned ID.
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);
        UserResponse userAccount = registerClientService.getUserData(viewedUserId);
        // This is done as the UserResponse has a default of 0 for ID when the user doesn't exist. Note the database needs to not have an ID of 0.
        int returnId = ((userAccount.getId() == 0) ?  id : userAccount.getId());

        List<Evidence> evidenceList;

        try {
            if (Objects.equals(tagType, "Skills")) {
                if (tagId == -1) { // No Skills
                    evidenceList = ((listAll) ? evidenceService.getEvidencesWithoutSkills() : evidenceService.getEvidencesWithUserAndWithoutSkills(viewedUserId));
                } else {
                    evidenceList = ((listAll) ? evidenceService.getEvidencesWithSkill(tagId) : evidenceService.getEvidencesWithSkillAndUser(viewedUserId, tagId));
                }
            } else if (Objects.equals(tagType, "Categories")) {
                evidenceList = ((listAll) ? evidenceService.getEvidencesWithCategory(tagId) : evidenceService.getEvidencesWithCategoryAndUser(viewedUserId, tagId));
            } else { // Invalid parameter given.
                return "redirect:account?userId=" + returnId;
            }
        } catch (NullPointerException ignore) { // Exception thrown when getting evidence.
            return "redirect:account?userId=" + returnId;
        }

        evidenceService.addUserDataToEvidence(evidenceList);
        model.addAttribute("allSkills",
                ((listAll) ?  tagService.getTagsSortedList() : tagService.getTagsByUserSortedList(userId)));
        model.addAttribute("evidencesExists", ((evidenceList != null) && (!evidenceList.isEmpty())));
        model.addAttribute("evidences", evidenceList);
        model.addAttribute("viewableUser", returnId);
        model.addAttribute("evidenceHighFivedIds", getHighFiveIds(evidenceList, id));
        model.addAttribute("allCategories", categoryService.getAllCategories());
        // This id should be the same as the userId stored in the header but for redundancy and safety this variable is also used.
        model.addAttribute("currentUserId", id);

        return ACCOUNT_EVIDENCE;
    }

    /**
     * Returns a list of the ids and names of skills used by the user.
     * @param userId user id of the user
     * @return list of ids and names of skills used by the user
     */
    @GetMapping("/get-skills")
    @ResponseBody
    public List<List<String>> getSkills(@RequestParam("userId") int userId) {
        List<Tag> skills = tagService.getTagsFromUserId(userId);
        return List.of(skills.stream().map(tag -> String.valueOf(tag.getTagId())).toList(),
                skills.stream().map(Tag::getTagName).toList());
    }

    /**
     * Returns a list of the ids and names of skills used by any user.
     * @return list of ids and names of all skills
     */
    @GetMapping("/get-all-skills")
    @ResponseBody
    public List<List<String>> getAllSkills() {
        List<Tag> skills = tagService.getAllTags();
        return List.of(skills.stream().map(tag -> String.valueOf(tag.getTagId())).toList(),
                skills.stream().map(Tag::getTagName).toList());
    }


    /**
     * Saves a piece of evidence after being high-fived.
     * @param evidenceId evidence id of the piece of evidence being high-fived
     * @param userId user id of the owner of the piece of evidence
     * @param userName userName of the owner of the piece of evidence
     * @return a redirect to load the page
     */
    @PostMapping("/saveHighFiveEvidence")
    public String saveHighFiveEvidence(
            @RequestParam("evidenceId") int evidenceId,
            @RequestParam("userId") int userId,
            @RequestParam("userName") String userName,
            Model model,
            HttpServletResponse httpServletResponse
    ) {
        boolean wasHighFived = evidenceService.saveHighFiveEvidence(evidenceId, userId, userName);
        if (wasHighFived) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return ACCOUNT_EVIDENCE;
    }

    /**
     * Saves a piece of evidence after being un-high-fived.
     * @param evidenceId evidence id of the piece of evidence being un-high-fived
     * @param userId user id of the owner of the piece of evidence
     * @param userName userName of the owner of the piece of evidence
     * @return a redirect to load the page
     */
    @PostMapping("/removeHighFiveEvidence")
    public String removeHighFiveEvidence(
            @RequestParam("evidenceId") int evidenceId,
            @RequestParam("userId") int userId,
            @RequestParam("userName") String userName,
            Model model,
            HttpServletResponse httpServletResponse,
            @AuthenticationPrincipal AuthState principal
    ) {
        boolean wasRemoved = evidenceService.removeHighFiveEvidence(evidenceId, userId, userName);
        if (wasRemoved) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return ACCOUNT_EVIDENCE;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This is triggered when a user adds a piece of evidence.
     * @param message Information about the added piece of evidence.
     * @return Returns the message given.
     */
    @MessageMapping("/evidence-add")
    @SendTo("/webSocketGet/evidence-added")
    public NotificationResponse evidenceAddNotification(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "add");
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This is triggered when a user deletes a piece of evidence.
     * @param message Information about the deleted piece of evidence.
     * @return Returns the message given.
     */
    @MessageMapping("/evidence-delete")
    @SendTo("/webSocketGet/evidence-deleted")
    public NotificationResponse evidenceDeleteNotification(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "delete");
    }

    /***
     * Used to handle the interaction between a piece of evidence being highfived
     * and the notification being shown through the header.
     *
     * @return Send a notification to the header to display a highfive notification.
     */
    @MessageMapping("/high-fived-evidence")
    @SendTo("/webSocketGet/notification-of-highfive")
    public NotificationHighFive highFiveNotification(NotificationHighFive notificationHighFive) {
        return notificationHighFive;
    }

    /***
     * Used to handle the interaction between a piece of evidence being un-highfived
     * @return Send a message to reload the page if viewing.
     */
    @MessageMapping("/remove-high-fived-evidence")
    @SendTo("/webSocketGet/notification-of-remove-highfive")
    public NotificationHighFive removeHighFiveNotification(NotificationHighFive notificationHighFive) {
        return notificationHighFive;
    }
}
