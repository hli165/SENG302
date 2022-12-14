package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.Group;
import nz.ac.canterbury.seng302.portfolio.model.NotificationMessage;
import nz.ac.canterbury.seng302.portfolio.model.NotificationResponse;
import nz.ac.canterbury.seng302.portfolio.model.NotificationGroup;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import nz.ac.canterbury.seng302.shared.util.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for group page
 */
@Controller
public class GroupController {

    @Autowired
    private UserAccountClientService userAccountClientService;

    @Autowired
    private ElementService elementService;

    @Autowired
    public GroupService groupService;

    @Autowired
    public RegisterClientService registerClientService;

    private static final String CURRENT_USER_ROLE = "currentUserRole";

    private static final String GROUP_CARD_FRAGMENT = "group::groupCard";

    private static final String GROUP_LIST_FRAGMENT = "group::groupList";

    private static final Integer MEMBERS_WITHOUT_GROUP_ID = 1;

    private static final Integer TEACHERS_GROUP_ID = 2;

    /**
     * Get method for group page to display group list and group detail
     * @param model  Parameters sent to thymeleaf template to be rendered into HTML
     * @return Group page
     */
    @GetMapping("/groups")
    public String groups(
            Model model,
            @AuthenticationPrincipal AuthState principal
    ) {
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);

        UserResponse user = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(user);

        model.addAttribute("userId", id);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userFirstName", user.getFirstName());
        model.addAttribute("userLastName", user.getLastName());
        model.addAttribute(CURRENT_USER_ROLE, role);
        groupService.addGroupListToModel(model);

        groupService.addToastsToModel(model, 3);

        return "group";
    }

    /**
     * Method to refresh the group table only
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @param groupId id of group to reload
     * @return Group page
     */
    @GetMapping("/groups/local")
    public String localRefresh(
            Model model,
            @RequestParam("groupId") int groupId,
            @AuthenticationPrincipal AuthState principal

    )
    {
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);

        UserResponse user = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(user);

        model.addAttribute(CURRENT_USER_ROLE, role);
        groupService.addGroupDetailToModel(model, groupId);
        groupService.addGroupListToModel(model);
        return "group::table_refresh";
    }

    /**
     * Method tries to add and sve the new group to the database
     * @param group group being added
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @param httpServletResponse for adding status codes to
     * @return redirect user to group page
     */
    @PostMapping("/add-group")
    public String addGroup(
            @ModelAttribute("group") Group group,
            Model model,
            HttpServletResponse httpServletResponse,
            @AuthenticationPrincipal AuthState principal

    ) {
        CreateGroupResponse response = groupService.createNewGroup(group.getShortName(), group.getLongName());
        if (response.getIsSuccess()) {
            groupService.addGroupDetailToModel(model, response.getNewGroupId());
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return GROUP_CARD_FRAGMENT;
        }

        List<ValidationError> errors = response.getValidationErrorsList();
        groupService.addGroupNameErrorsToModel(model, errors);
        httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return "fragments/groupModal::groupModalBody";
    }

    /**
     * Submits a request to the identity provider to delete the group with the given id from the database. Adds a
     * variable to the model indicating whether or not this was successful.
     * @param id id of the group to delete
     * @param httpServletResponse for adding status codes to
     */
    @DeleteMapping("/delete-group/{id}")
    @ResponseBody
    public void groupRemove(@PathVariable("id") Integer id,
                              HttpServletResponse httpServletResponse
    ) {
        DeleteGroupResponse response = groupService.deleteGroup(id);
        if (response.getIsSuccess()) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Submits a request to the identity provider to delete the group with the given id from the database. Adds a
     *      * variable to the model indicating whether or not this was successful.
     * @param groupId Group id of the group having users removed.
     * @param userIds A list of user ids to remove from the group.
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @param httpServletResponse for adding status codes to
     * @param principal for getting the current user
     * @return return a fragment of the group page
     */
    @PostMapping("/remove-users")
    public String removeUsers(
        @RequestParam("groupId") Integer groupId,
        @RequestParam("userIds") List<Integer> userIds,
        Model model,
        HttpServletResponse httpServletResponse,
        @AuthenticationPrincipal AuthState principal
    ) {
        Integer userId = userAccountClientService.getUserIDFromAuthState(principal);
        UserResponse currentUser = registerClientService.getUserData(userId);
        if (Objects.equals(groupId, TEACHERS_GROUP_ID)
                && !elementService.getUserHighestRole(currentUser).equals("admin")
        ) {
            userIds.remove(userId);
        }
        RemoveGroupMembersResponse response = groupService.removeMembersFromGroup(groupId, userIds);
        return returnGroupFragment(response.getIsSuccess(), httpServletResponse, groupId, model);
    }

    /**
     * Tries to save new data to group with given groupId to the database.
     * @param id id of groups edited
     * @param group Group data to be updated
     * @param model model to add attributes to for Thymeleaf to inject into the HTML
     * @param httpServletResponse for adding status codes to
     * @throws IllegalArgumentException if sprint cannot be found from the given ID or if it cannot be saved.
     */
    @PostMapping("/edit-group/{id}")
    public String groupEdit(
            @PathVariable("id") Integer id,
            @ModelAttribute("group") Group group,
            Model model,
            HttpServletResponse httpServletResponse
    ) throws IllegalArgumentException {
        ModifyGroupDetailsResponse response = groupService.editGroupDetails(id, group.getShortName(), group.getLongName());

        if (response.getIsSuccess()) {
            groupService.addGroupDetailToModel(model, id);
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return GROUP_CARD_FRAGMENT;
        }

        List<ValidationError> errors = response.getValidationErrorsList();
        groupService.addGroupNameErrorsToModel(model, errors);
        httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return "fragments/groupModal::groupModalBody";
    }

    /**
     * Post method for copying users from one group to another
     * @param model  Parameters sent to thymeleaf template to be rendered into HTML
     * @param groupId id of group to copy users to
     * @return Group detail page
     */
    @PostMapping("/copy-users")
    public String moveUsers(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("userIds") List<Integer> userIds,
            @RequestParam("currentGroupId") Integer currentGroupId,
            Model model,
            HttpServletResponse httpServletResponse,
            @AuthenticationPrincipal AuthState principal
    ) {
        Integer userId = userAccountClientService.getUserIDFromAuthState(principal);
        UserResponse currentUser = registerClientService.getUserData(userId);
        List<UserResponse> listOfTeachers = groupService.getGroupDetails(TEACHERS_GROUP_ID).getMembersList();
        List<Integer> listOfTeacherIds = listOfTeachers.stream().map(UserResponse::getId).toList();
        if ((   (
                Objects.equals(groupId, MEMBERS_WITHOUT_GROUP_ID) &&
                Objects.equals(currentGroupId, TEACHERS_GROUP_ID)
            ) ||
                Objects.equals(groupId, MEMBERS_WITHOUT_GROUP_ID) &&
                listOfTeacherIds.contains(userId)
        ) &&
                !elementService.getUserHighestRole(currentUser).equals("admin")
        ) {
            userIds.remove(userId);
        }
        AddGroupMembersResponse response = groupService.addMemberToGroup(groupId, userIds);
        return returnGroupFragment(response.getIsSuccess(), httpServletResponse, groupId, model);
    }

    /**
     * Returns the group card for the "members without a group" group.
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @return group card for members without a group
     */
    @GetMapping("/members-without-a-group")
    public String membersWithoutAGroupCard(
            Model model
    ) {
        groupService.addGroupDetailToModel(model, MEMBERS_WITHOUT_GROUP_ID);
        return GROUP_CARD_FRAGMENT;
    }


    /**
     * Returns the list of groups for the group page.
     * @param principal authentication principal
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @return group list
     */
    @GetMapping("/group-list")
    public String getGroupList(
            @AuthenticationPrincipal AuthState principal,
            Model model
    ) {
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        UserResponse user = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(user);
        model.addAttribute(CURRENT_USER_ROLE, role);

        groupService.addGroupListToModel(model);
        return GROUP_LIST_FRAGMENT;
    }

    /**
     * Returns a fragment of the group list if the response was successful and the group id is for the special
     * non-members group. If the response was successful and the group id is for another group, a fragment of the group
     * card is returned. If the response was unsuccessful, null is returned.
     * @param responseSuccess whether a GRPC request and IDP operation was successful
     * @param httpServletResponse for adding status codes to
     * @param groupId id of group modified in GRPC request
     * @param model model to add attributes to for Thymeleaf to inject into the HTML
     * @return group list/card fragment if successful, otherwise null
     */
    private String returnGroupFragment(boolean responseSuccess, HttpServletResponse httpServletResponse, int groupId,
                                       Model model) {
        if (responseSuccess) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            if (Objects.equals(groupId, MEMBERS_WITHOUT_GROUP_ID)) {
                groupService.addGroupListToModel(model);
                return GROUP_LIST_FRAGMENT;
            } else {
                groupService.addGroupDetailToModel(model, groupId);
                return GROUP_CARD_FRAGMENT;
            }
        } else {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        return null;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint.
     * @param message NotificationMessage that holds information about the group being updated
     * @return returns an NotificationResponse that holds information about the group being updated.
     */
    @MessageMapping("/editing-group")
    @SendTo("/webSocketGet/group-being-edited")
    public NotificationResponse updatingGroupWebsocket(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "edit");
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This is triggered when the user is no longer editing.
     * @param message Information about the editing state.
     * @return Returns the message given.
     */
    @MessageMapping("/stop-editing-group")
    @SendTo("/webSocketGet/group-stop-being-edited")
    public NotificationResponse stopUpdatingGroupWebsocket(NotificationMessage message) {
        return NotificationResponse.fromMessage(message, "edit");
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when something is sent to
     * the MessageMapping endpoint. This method also triggers some sort of re-render of the groups.
     * @param message NotificationMessage that holds information about the group being updated
     * @return returns an NotificationResponse that holds information about the group being updated.
     */
    @MessageMapping("/saved-edited-group")
    @SendTo("/webSocketGet/group-save-edit")
    public NotificationResponse savingUpdatedGroupWebsocket(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "save");
        // Trigger reload and save the last group's information
        groupService.addNotification(response, 3);
        return response;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when a group is added.
     * @param message NotificationMessage that holds information about the group being added
     * @return returns an NotificationResponse that holds information about the group being added.
     */
    @MessageMapping("/added-group")
    @SendTo("/webSocketGet/group-add")
    public NotificationResponse addingGroupWebsocket(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "add");
        groupService.addNotification(response, 3);
        return response;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when a group is deleted.
     * @param message NotificationMessage that holds information about the group being deleted
     * @return returns an NotificationResponse that holds information about the group being deleted.
     */
    @MessageMapping("/deleted-group")
    @SendTo("/webSocketGet/group-delete")
    public NotificationResponse deletingGroupWebsocket(NotificationMessage message) {
        NotificationResponse response = NotificationResponse.fromMessage(message, "delete");
        groupService.addNotification(response, 3);
        return response;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint. Called when a groups members are updated.
     * @param message NotificationMessage that holds information about the groups being updated.
     * @return returns an NotificationResponse that holds information about the groups being updated.
     */
    @MessageMapping("/changed-members-group")
    @SendTo("/webSocketGet/group-change-users")
    public NotificationGroup changeGroupMembersWebsocket(NotificationGroup message) {
        return message;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint.
     * Called when a groups long name is updated within the group settings page.
     * @param message NotificationMessage that holds information about the groups being updated.
     * @return returns an NotificationResponse that holds information about the groups being updated.
     */
    @MessageMapping("/save-group-settings-outside")
    @SendTo("/webSocketGet/saved-group-settings-outside")
    public NotificationGroup changeGroupSettingsWebsocket(NotificationGroup message) {
        return message; }

}
