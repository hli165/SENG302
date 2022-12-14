package nz.ac.canterbury.seng302.portfolio.controller;


import nz.ac.canterbury.seng302.portfolio.model.NotificationGroup;
import nz.ac.canterbury.seng302.portfolio.model.GroupSettings;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.utility.ToastUtility;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.ModifyGroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Contributor;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


/**
 * Controller for group setting page.
 */
@Controller
public class GroupSettingsController {

    @Autowired
    private ElementService elementService;

    @Autowired
    private UserAccountClientService userAccountClientService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupSettingsService groupSettingsService;

    @Autowired
    private GitLabApiService gitLabApiService;

    @Autowired
    private RegisterClientService registerClientService;

    @Autowired
    private PermissionService permissionService;

    private static final String GROUP_SETTING_ALERT_MESSAGE = "groupSettingsAlertMessage";

    private static final String GROUP_ID = "groupId";

    private static final String IS_REPO_EXIST = "isRepoExist";

    private static final String IS_CONNECTION_SUCCESSFUL = "isConnectionSuccessful";

    private static final String CURRENT_USER_ROLE = "currentUserRole";

    /**
     * Method to handle GetMapping request from frontend, and return the group settings page.
     * @param groupId current group id
     * @param model group setting page model
     * @return group settings page
     */
    @GetMapping("/groupSettings")
    public String groupSettings(
            @RequestParam(value = "groupId") int groupId,
            @AuthenticationPrincipal AuthState principal,
            Model model
    ) {
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);
        UserResponse user = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(user);

        model.addAttribute(GROUP_ID, groupId);
        model.addAttribute(CURRENT_USER_ROLE, role);

        // Non-existent group will have a group id of 0 when calling getGroupDetails
        if (0 <= groupService.getGroupDetails(groupId).getGroupId() &&
                groupService.getGroupDetails(groupId).getGroupId() <= 2) {
            return "redirect:/groups";
        }
        ToastUtility.addToastsToModel(model, new ArrayList<>(), 3);

        long repoId = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoId();
        String repoToken = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoApiKey();
        String repoUrl = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoUrl();
        gitLabApiService.checkGitLabToken(model, repoId, repoToken, repoUrl);

        boolean isValidToModify = permissionService.isValidToModifyGroupSettingPage(groupId, id);
        model.addAttribute("isValidToModify", isValidToModify);

        groupService.addGroupDetailToModel(model, groupId);

        GroupSettings groupSettings = groupSettingsService.getGroupSettingsByGroupId(groupId);
        groupSettingsService.addSettingAttributesToModel(model, groupSettings);

        addGroupSettingAttributeToModel(model, groupId);

        model.addAttribute("userId", id);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("userFirstName", user.getFirstName());
        model.addAttribute("userLastName", user.getLastName());

        return "groupSettings";
    }

    /**
     * Method to partial refresh the group repository commits list.
     * @param groupId current group id
     * @param branchName Current project's branches
     * @param userEmail current user's emailal
     * @param model model for group setting page
     * @return repository commits fragment
     */
    @GetMapping("/repository-commits")
    public String getRepositoryCommits(
            @RequestParam(value = "groupId") int groupId,
            @RequestParam(value = "branchName") String branchName,
            @RequestParam(value = "userEmail") String userEmail,
            @AuthenticationPrincipal AuthState principal,
            Model model) {
        try {
            String branchRequestName = null;
            String userRequestEmail = null;
            if(!branchName.equals("All Branches")) {
                branchRequestName = branchName;
            }
            if (!userEmail.equals("All Users")) {
                userRequestEmail = userEmail;
            }
            List<Commit> allCommit = gitLabApiService.getCommits(groupId, branchRequestName, userRequestEmail);
            model.addAttribute("commitList", allCommit);
            return "groupSettings::commitsListRefresh";
        } catch (GitLabApiException | ObjectNotFoundException e) {
            return "groupSettings::commitsListRefresh";
        }
    }

    /**
     * Method to partial refresh the group setting card.
     *
     * @param groupId current group id
     * @param model   model for group setting page
     * @return repository contributors fragment
     */
    @RequestMapping("/groupSettings/refreshGroupSettings")
    public String refreshGroupSetting(
            Model model,
            @RequestParam(value = "groupId") int groupId,
            @AuthenticationPrincipal AuthState principal) {

        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);
        UserResponse user = registerClientService.getUserData(id);
        String role = elementService.getUserHighestRole(user);

        long repoId = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoId();
        String repoToken = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoApiKey();
        String repoUrl = groupSettingsService.getGroupSettingsByGroupId(groupId).getRepoUrl();
        gitLabApiService.checkGitLabToken(model, repoId, repoToken, repoUrl);
        boolean isValidToModify = permissionService.isValidToModifyGroupSettingPage(groupId, id);
        model.addAttribute("isValidToModify", isValidToModify);

        groupService.addGroupDetailToModel(model, groupId);
        GroupSettings groupSettings = groupSettingsService.getGroupSettingsByGroupId(groupId);
        groupSettingsService.addSettingAttributesToModel(model, groupSettings);
        addGroupSettingAttributeToModel(model, groupId);
        model.addAttribute(GROUP_ID, groupId);
        model.addAttribute(CURRENT_USER_ROLE, role);

        if (groupSettings.getRepoId() != 0) {
            model.addAttribute("repoId", groupSettings.getRepoId());
        } else {
            model.addAttribute("repoId", 0);

        }
        model.addAttribute("repoName", groupSettings.getRepoName());
        model.addAttribute("repoApiKey", groupSettings.getRepoApiKey());
        model.addAttribute("groupSettingsId", groupSettings.getGroupSettingsId());
        model.addAttribute("repoServerUrl", groupSettings.getRepoUrl());

        return "groupSettings::groupSetting";
    }



    /**
     * POST method for group setting page to update group long name,
     * Repository name, Id, and Access Token
     * @return group setting page
     */
    @PostMapping("/saveGroupSettings")
    public String editGroupSetting(
            Model model,
            @RequestParam(name = "groupLongName") String longName,
            @RequestParam(name = "groupShortName") String shortName,
            @RequestParam(value = "groupId") int groupId,
            @RequestParam(name = "repoName", required = false, defaultValue = "") String repoName,
            @RequestParam(name = "repoID", required = false) long repoId,
            @RequestParam(name = "repoToken", required = false, defaultValue = "") String repoToken,
            @RequestParam(name = "groupSettingsId") int groupSettingsId,
            @RequestParam(name = "repoURL", required = false, defaultValue = "") String repoServerUrl,
            @AuthenticationPrincipal AuthState principal,
            HttpServletResponse httpServletResponse,
            RedirectAttributes rm
    )  {
        Integer id = userAccountClientService.getUserIDFromAuthState(principal);
        elementService.addHeaderAttributes(model, id);
        boolean isValidToModify = permissionService.isValidToModifyGroupSettingPage(groupId, id);
        // Permission check in case the user sends a POST request, but they don't have the correct permissions
        if (!isValidToModify) {
            return "redirect:/groups";
        } else {
            model.addAttribute("isValidToModify", true);
        }

        rm.addAttribute(GROUP_ID, groupId);
        model.addAttribute(GROUP_ID, groupId);
        ModifyGroupDetailsResponse groupResponse = groupService.editGroupDetails(groupId, shortName, longName);

        // First, we check the response from the server to see if edit the group long name is successful
        if (!groupResponse.getIsSuccess()) {
            model.addAttribute("groupLongNameAlertMessage", groupResponse.getMessage());
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "groupSettings::groupLongNameAlertBanner";
        }

        boolean isSaved = groupSettingsService.isGroupSettingSaved(groupSettingsId, repoId, repoName, repoToken, groupId, repoServerUrl);
        gitLabApiService.checkGitLabToken(model, repoId, repoToken, repoServerUrl);


        if (!isSaved) {
            model.addAttribute(GROUP_SETTING_ALERT_MESSAGE, "Invalid Repository Information");
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "groupSettings::groupSettingsAlertBanner";
        }
        groupService.addGroupDetailToModel(model, groupId);
        GroupSettings groupSettings = groupSettingsService.getGroupSettingsByGroupId(groupId);
        groupSettingsService.addSettingAttributesToModel(model, groupSettings);
        addGroupSettingAttributeToModel(model, groupId);

        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        model.addAttribute("successMessage", "Save changed");
        return "groupSettings::groupSetting";
    }

    /**
     * Method to get group's members
     * @param model Parameters sent to thymeleaf template to be rendered into HTML
     * @param groupId id of group to reload
     * @return Group page
     */
    @GetMapping("/getGroupMembers")
    public String getGroupMembers(
            Model model,
            @RequestParam("groupId") int groupId,
            @AuthenticationPrincipal AuthState principal

    )
    {
        GroupDetailsResponse groupDetailsResponse = groupService.getGroupDetails(groupId);
        List<UserResponse> members = groupDetailsResponse.getMembersList();
        model.addAttribute("members", members);
        return "groupSettings::table_refresh";
    }

    /**
     * Method to add model attribute for group setting page, depending on different situations.
     *
     * Situation 1: if current group has set up group setting, add branches and contributors to model
     * Situation 2: if current group has not set up group repository, add isRepoExist attribute to model
     * Situation 3: if current group has set up group setting with connection error, add error message to model
     * @param model model for group setting page
     * @param groupId current group id
     */
    public void addGroupSettingAttributeToModel(Model model, int groupId) {
        try {
            if (groupSettingsService.doesGroupHaveRepo(groupId)){
                List<Contributor> repositoryContributors = gitLabApiService.getContributors(groupId);
                model.addAttribute("repositoryContributors",repositoryContributors);
                List<String> branchesName = gitLabApiService.getBranchNames(groupId);
                model.addAttribute("branchesName", branchesName);
                model.addAttribute(IS_REPO_EXIST, true);
                model.addAttribute(GROUP_ID, groupId);
                model.addAttribute(IS_CONNECTION_SUCCESSFUL, true);
            } else {
                model.addAttribute(IS_REPO_EXIST, false);
                model.addAttribute(IS_CONNECTION_SUCCESSFUL, true);
            }
        } catch (GitLabApiException exception) {
            model.addAttribute(IS_CONNECTION_SUCCESSFUL, false);
            model.addAttribute(IS_REPO_EXIST, false);
        }
    }

    /**
     * Websocket controller to send notification to users to have their pages refreshed when new settings are saved.
     * @param notificationGroup Holds the ID of the group being refreshed.
     * @return The notificationGroup object.
     */
    @MessageMapping("/save-group-settings")
    @SendTo("/webSocketGet/group-settings-saved")
    public NotificationGroup refreshGroupSettings(NotificationGroup notificationGroup) {
        return notificationGroup;
    }

    /**
     * This method maps @MessageMapping endpoint to the @SendTo endpoint.
     * Called when a groups long name is updated outside the group settings page so that it can be reloaded.
     * @param notificationGroup NotificationGroup that holds information about the groups being updated.
     * @return returns an NotificationResponse that holds information about the groups being updated.
     */
    @MessageMapping("/outside-save-group-settings")
    @SendTo("/webSocketGet/outside-group-settings-saved")
    public NotificationGroup refreshGroupSettingsOutside(NotificationGroup notificationGroup) {
        return notificationGroup;
    }
}


