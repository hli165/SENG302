package nz.ac.canterbury.seng302.portfolio.controller;

import com.google.protobuf.Timestamp;
import nz.ac.canterbury.seng302.portfolio.model.Category;
import nz.ac.canterbury.seng302.portfolio.model.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.Tag;
import nz.ac.canterbury.seng302.portfolio.model.WebLink;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.service.EvidenceService;
import nz.ac.canterbury.seng302.portfolio.service.RegisterClientService;
import nz.ac.canterbury.seng302.portfolio.service.TagService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.service.EvidenceService;
import nz.ac.canterbury.seng302.portfolio.service.RegisterClientService;
import nz.ac.canterbury.seng302.portfolio.service.TagService;
import nz.ac.canterbury.seng302.portfolio.service.UserAccountClientService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static nz.ac.canterbury.seng302.portfolio.controller.EvidenceController.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the evidence controller.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = EvidenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc = MockMvcBuilders.standaloneSetup(EvidenceController.class).build();

    @MockBean
    private EvidenceService evidenceService;

    @MockBean
    private TagService tagService;

    @MockBean
    private PermissionService permissionService; // needed to load application context

    @MockBean
    private ElementService elementService; // needed to load application context

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private UserAccountClientService userAccountClientService; // needed to load application context

    @MockBean
    private RegisterClientService registerClientService; // needed to load application context

    /**
     * Mocked user response which contains the data of the user
     */
    private final UserResponse mockUser = UserResponse.newBuilder()
            .setBio("default bio")
            .setCreated(Timestamp.newBuilder().setSeconds(55))
            .setEmail("hello@test.com")
            .setFirstName("firsttestname")
            .setLastName("lasttestname")
            .setMiddleName("middlettestname")
            .setNickname("niktestname")
            .setPersonalPronouns("He/him")
            .addRoles(UserRole.STUDENT)
            .build();

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

    private static final Evidence testEvidence = new Evidence(0 ,0, "test evidence", "test description", new Date());

    private static final List<Category> testCategories = List.of(
            new Category("test_category1"),
            new Category("test_category2"),
            new Category("test_category3")
    );

    @BeforeEach
    public void setup() {
        // Mock the security context
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        when(mockedSecurityContext.getAuthentication()).thenReturn(new PreAuthenticatedAuthenticationToken(validAuthState, ""));
        SecurityContextHolder.setContext(mockedSecurityContext);

        when(userAccountClientService.getUserIDFromAuthState(any(AuthState.class))).thenReturn(1);
        when(registerClientService.getUserData(1)).thenReturn(mockUser);
    }

    /**
     * Tests blue sky scenario for adding evidence.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence200() throws Exception {
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(true);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(testEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", testEvidence))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/evidenceModal::evidenceModalBody"));

        verify(evidenceService, times(1)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests blue sky scenario for adding evidence with a valid web link.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence200ValidWebLink() throws Exception {
        List<WebLink> validWebLinks = List.of(new WebLink("https://www.google.com"), new WebLink("https://localhost:9000/"),
                new WebLink("https://dbadmin.csse.canterbury.ac.nz/index.php?route=/sql&db=jth141_portfolio-test&table=web_link&pos=0"),
                new WebLink("http://www.site.com:8008"), new WebLink("http://www.example.com/~product?id=1&page=2"));
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(true);
        for (WebLink validWebLink : validWebLinks) {
            Evidence evidence = new Evidence(0, 0, "test evidence", "test description", new Date());
            evidence.addWebLink(validWebLink);
            doCallRealMethod().when(evidenceService).validateEvidence(eq(evidence), any(Model.class));

            mockMvc.perform(post("/add-evidence").flashAttr("evidence", evidence))
                    .andExpect(status().isOk())
                    .andExpect(view().name("fragments/evidenceModal::evidenceModalBody"));
        }
        verify(evidenceService, times(validWebLinks.size())).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence service failing to add the evidence.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence500() throws Exception {
        when(evidenceService.addEvidence(any(Evidence.class))).thenReturn(false);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(testEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", testEvidence)).andExpect(status().isInternalServerError())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Evidence Not Added. Saving Error Occurred."));

        verify(evidenceService, times(1)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data missing.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400MissingData() throws Exception {
        doCallRealMethod().when(evidenceService).validateEvidence(any(Evidence.class), any(Model.class));

        mockMvc.perform(post("/add-evidence")).andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title is required"))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description is required"))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE, "Correctly formatted date is required"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title that exceeds 30 characters.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooLongTitle() throws Exception {
        String stringWith31Chars = new String(new char[31]).replace('\0', 't');
        Evidence invalidEvidence = new Evidence(0 ,0, stringWith31Chars, "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must be less than 30 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description that exceeds 250 characters.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooLongDescription() throws Exception {
        String stringWith251Chars = new String(new char[251]).replace('\0', 't');
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", stringWith251Chars, new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must be less than 250 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data not including a date.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400MissingDate() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "test description", null);
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE, "Correctly formatted date is required"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title is only one character.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooShortTitle() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "a", "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must be at least 2 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description is only one character.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooShortDescription() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "a", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must be at least 2 characters"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a title with only punctuation.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400PunctuationTitle() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, ",.!@#$%^&*';:", "test description", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must contain at least one letter"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having a description with only punctuation.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400PunctuationDescription() throws Exception {
        Evidence invalidEvidence = new Evidence(0, 0, "test evidence", ".,!@#$%^&*';:", new Date());
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must contain at least one letter"))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having an invalid URL.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400InvalidWebLink() throws Exception {
        List<WebLink> invalidWebLinks = List.of(new WebLink(".,!@#$%^&*';:"), new WebLink("www.test.com"),
                new WebLink("something.ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"),
                new WebLink("mke.ccc"));
        for (WebLink invalidWebLink: invalidWebLinks) {
            Evidence invalidEvidence = new Evidence(0, 0, "test evidence", "test description", new Date());
            invalidEvidence.addWebLink(invalidWebLink);
            doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

            mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                    .andExpect(status().isBadRequest())
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                    .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                    .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE, "Web links must be valid URLs"));
        }
        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }

    /**
     * Tests adding evidence failing due to the evidence data having too many web links.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TooManyWebLinks() throws Exception {
        Evidence invalidEvidence = new Evidence(0 ,0, "test evidence", "test description", new Date());
        for (int i=0; i<11; i++) {
            invalidEvidence.addWebLink(new WebLink("https://www.google.com"));
        }
        doCallRealMethod().when(evidenceService).validateEvidence(eq(invalidEvidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", invalidEvidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE, "You can only have up to 10 web links"));

        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }


    /**
     * Tests adding the evidence failing due to the evidence title and description have emojis
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testAddEvidence400TitleAndDescriptionHasEmojiCharacters() throws Exception {
        Evidence evidence = new Evidence(0, 0, "\uD83D\uDC7B\uD83D\uDC7B\uD83D\uDC7B\uD83D\uDC7Bsdsadad", "\uD83D\uDC7B\uD83D\uDC7B\uD83D\uDC7B\uD83D\uDC7Bsdsadad", new Date());
        evidence.addWebLink(new WebLink("https://www.google.com"));
        doCallRealMethod().when(evidenceService).validateEvidence(eq(evidence), any(Model.class));

        mockMvc.perform(post("/add-evidence").flashAttr("evidence", evidence))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_WEB_LINKS_MESSAGE))
                .andExpect(model().attributeDoesNotExist(ADD_EVIDENCE_MODAL_FRAGMENT_DATE_MESSAGE))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_TITLE_MESSAGE, "Title must not contain emojis"))
                .andExpect(model().attribute(ADD_EVIDENCE_MODAL_FRAGMENT_DESCRIPTION_MESSAGE, "Description must not contain emojis"));
        verify(evidenceService, times(0)).addEvidence(any(Evidence.class));
    }


    /**
     * Tests that the evidence skills page is able to be reached when a valid data is given (Blue Sky Scenario).
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsPage200() throws Exception {
        int userId = 1;
        String userName = "testUser";
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        Tag tag = new Tag("Test");
        tag.setTagId(1);
        UserResponse testUser = UserResponse.newBuilder().setId(userId).setUsername(userName).build();

        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenReturn(evidences);
        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(tagService.getTag(any(Integer.class))).thenReturn(tag);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences))
                .andExpect(model().attribute("tagName", tag.getTagName()))
                .andExpect(model().attribute("viewableUser", testUser.getId()))
                .andExpect(model().attribute("tagType", "Skills"))
                .andExpect(model().attribute("tagId", tag.getTagId()))
                .andExpect(model().attribute("validViewedUser", true));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(tagService, times(1)).getTag(any(Integer.class));
        verify(registerClientService, times(3)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills page is able to be reached and display all evidence with no skills.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceNoSkillsPage200() throws Exception {
        int userId = 1;
        String userName = "testUser";
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).setUsername(userName).build();

        when(evidenceService.getEvidencesWithoutSkills()).thenReturn(evidences);
        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=-1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences))
                .andExpect(model().attribute("tagName", "No Skills"))
                .andExpect(model().attribute("viewableUser", testUser.getId()))
                .andExpect(model().attribute("tagType", "Skills"))
                .andExpect(model().attribute("tagId", -1))  // Tags must be -1 to get the "No Skills" page.
                .andExpect(model().attribute("validViewedUser", true));

        verify(evidenceService, times(1)).getEvidencesWithoutSkills();
        verify(registerClientService, times(3)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills page is able to be reached when a valid tag but invalid user is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsWithInvalidUserPage200() throws Exception {
        int userId = 0;
        String userName = "testUser";
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        Tag tag = new Tag("Test");
        tag.setTagId(1);
        UserResponse testUser = UserResponse.newBuilder().setId(userId).setUsername(userName).build();

        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenReturn(evidences);
        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(tagService.getTag(any(Integer.class))).thenReturn(tag);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences))
                .andExpect(model().attribute("tagName", tag.getTagName()))
                .andExpect(model().attribute("viewableUser", 1)) // Logged-in user given by AuthState.
                .andExpect(model().attribute("tagType", "Skills"))
                .andExpect(model().attribute("tagId", tag.getTagId()))
                .andExpect(model().attribute("validViewedUser", false));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(tagService, times(1)).getTag(any(Integer.class));
        verify(registerClientService, times(3)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills page will redirect a user to the account page provided when an invalid tag is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsPageRedirectToAccountWhenSkillInvalid300() throws Exception {
        int userId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenReturn(evidences);
        when(tagService.getTag(any(Integer.class))).thenReturn(null);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Skills"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + userId));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(tagService, times(1)).getTag(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills page will redirect a user to their account page when an invalid tag and user ID is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsPageRedirectToAccountWhenSkillAndUserInvalid300() throws Exception {
        int providedUserId = 0; // 0 is used as an invalid user as it is the default set by the UserResponse Object.
        int realUserId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, realUserId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenReturn(evidences);
        when(tagService.getTag(any(Integer.class))).thenReturn(null);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + providedUserId + "&tagId=1&tagType=Skills"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + realUserId));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(tagService, times(1)).getTag(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh is able to be reached when a valid data is given (Blue Sky Scenario).
     * This also tests that when given the correct values all data will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsUpdateToViewAll200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=true&tagId=1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh is able to be reached when a valid data is given (Blue Sky Scenario).
     * This also tests that when given the correct values only data matching to a users ID will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsUpdateToViewSingleUser200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add( new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkillAndUser(any(Integer.class), any(Integer.class))).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=false&tagId=1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));

        verify(evidenceService, times(1)).getEvidencesWithSkillAndUser(any(Integer.class), any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh will redirect a user to a provided account page when an invalid tag is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsUpdateRedirectToAccountWhenSkillInvalid300() throws Exception {
        int userId = 1;
        int providedUserId = 2;
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenThrow(new NullPointerException("Invalid skill tag."));
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=" + userId + "&viewedUserId=" + providedUserId + "&listAll=true&tagId=1&tagType=Skills"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + providedUserId));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh will redirect a user to their account page when an invalid tag and user ID is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceSkillsUpdateRedirectToAccountWhenSkillAndUserInvalid300() throws Exception {
        int providedUserId = 0; // 0 is used as an invalid user as it is the default set by the UserResponse Object.
        int realUserId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithSkill(any(Integer.class))).thenThrow(new NullPointerException("Invalid skill tag."));
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=" + realUserId + "&viewedUserId=" + providedUserId + "&listAll=true&tagId=1&tagType=Skills"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + realUserId));

        verify(evidenceService, times(1)).getEvidencesWithSkill(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh is able to be reached when a valid data is given
     * for finding all evidence without a skill attached.
     * This also tests that when given the correct values all data will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceNoSkillsUpdateToViewAll200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithoutSkills()).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=true&tagId=-1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));

        verify(evidenceService, times(1)).getEvidencesWithoutSkills();
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence skills partial refresh is able to be reached when a valid data is given
     * for finding all evidence without a skill attached.
     * This also tests that when given the correct values only data matching to a users ID will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceNoSkillsUpdateToViewSingleUser200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add( new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithUserAndWithoutSkills(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=false&tagId=-1&tagType=Skills"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));

        verify(evidenceService, times(1)).getEvidencesWithUserAndWithoutSkills(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the get skills endpoint returns a list of skill ids and names.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testGetSkills() throws Exception {
        List<Tag> skills = List.of(new Tag("test skill 1"), new Tag("test skill 2"));
        int userId = 5;

        when(tagService.getTagsFromUserId(userId)).thenReturn(skills);
        when(categoryService.getAllCategories()).thenReturn(testCategories);


        mockMvc.perform(get("/get-skills").param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format("[[\"%s\",\"%s\"],[\"%s\",\"%s\"]]",
                        skills.get(0).getTagId(), skills.get(1).getTagId(), skills.get(0).getTagName(), skills.get(1).getTagName())));
    }

    /**
     * Tests that the get skills endpoint returns a list of skill ids and names.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testGetAllSkills() throws Exception {
        List<Tag> skills = List.of(new Tag("test skill 1"), new Tag("test skill 2"));
        when(tagService.getAllTags()).thenReturn(skills);

        mockMvc.perform(get("/get-all-skills"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format("[[\"%s\",\"%s\"],[\"%s\",\"%s\"]]",
                        skills.get(0).getTagId(), skills.get(1).getTagId(), skills.get(0).getTagName(), skills.get(1).getTagName())));
    }

    /**
     * Tests that the evidence categories page is able to be reached when a valid data is given (Blue Sky Scenario).
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesPage200() throws Exception {
        int userId = 1;
        String userName = "testUser";
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        Category category = new Category("Test");
        category.setCategoryId(1);
        UserResponse testUser = UserResponse.newBuilder().setId(userId).setUsername(userName).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getCategory(any(Integer.class))).thenReturn(category);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences))
                .andExpect(model().attribute("tagName", category.getCategoryName()))
                .andExpect(model().attribute("viewableUser", testUser.getId()))
                .andExpect(model().attribute("tagType", "Categories"))
                .andExpect(model().attribute("tagId", category.getCategoryId()))
                .andExpect(model().attribute("validViewedUser", true));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(categoryService, times(1)).getCategory(any(Integer.class));
        verify(registerClientService, times(3)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence categories page is able to be reached when a valid tag ID but invalid user ID is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesWithInvalidUserPage200() throws Exception {
        int userId = 0;
        String userName = "testUser";
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        Category category = new Category("Test");
        category.setCategoryId(1);
        UserResponse testUser = UserResponse.newBuilder().setId(userId).setUsername(userName).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getCategory(any(Integer.class))).thenReturn(category);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences))
                .andExpect(model().attribute("tagName", category.getCategoryName()))
                .andExpect(model().attribute("viewableUser", 1)) // Logged-in user given by AuthState.
                .andExpect(model().attribute("tagType", "Categories"))
                .andExpect(model().attribute("tagId", category.getCategoryId()))
                .andExpect(model().attribute("validViewedUser", false));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(categoryService, times(1)).getCategory(any(Integer.class));
        verify(registerClientService, times(3)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence Categories page will redirect a user to provided account page when an invalid tag is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesPageRedirectToAccountWhenCategoryInvalid300() throws Exception {
        int userId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getCategory(any(Integer.class))).thenReturn(null);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + userId + "&tagId=1&tagType=Categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + userId));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(categoryService, times(1)).getCategory(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence Categories page will redirect a user to their account page when an invalid tag and user ID is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesPageRedirectToAccountWhenCategoryAndUserInvalid300() throws Exception {
        int providedUserId = 0; // 0 is used as an invalid user as it is the default set by the UserResponse Object.
        int realUserId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, providedUserId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getCategory(any(Integer.class))).thenReturn(null);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/evidence-tags?userId=" + providedUserId + "&tagId=1&tagType=Categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + realUserId));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(categoryService, times(1)).getCategory(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }


    /**
     * Tests that the evidence Categories partial refresh is able to be reached when a valid data is given (Blue Sky Scenario).
     * This also tests that when given the correct values all data will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesUpdateToViewAll200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add(new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=true&tagId=1&tagType=Categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));
        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }


    /**
     * Tests that the evidence Categories partial refresh is able to be reached when a valid data is given (Blue Sky Scenario).
     * This also tests that when given the correct values only data matching to a users ID will be returned to the page.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesUpdateToViewSingleUser200() throws Exception {
        int userId = 1;
        ArrayList<Evidence> evidences = new ArrayList<>();
        evidences.add( new Evidence(0, userId, "test", "test-desc", Date.from(Instant.now())));
        UserResponse testUser = UserResponse.newBuilder().setId(userId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategoryAndUser(any(Integer.class), any(Integer.class))).thenReturn(evidences);
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=1&viewedUserId=" + userId + "&listAll=false&tagId=1&tagType=Categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("evidencesExists", true))
                .andExpect(model().attribute("evidences", evidences));

        verify(evidenceService, times(1)).getEvidencesWithCategoryAndUser(any(Integer.class), any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence Categories partial refresh will redirect a user to the provided account page when an invalid tag is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesUpdateRedirectToAccountWhenCategoryInvalid300() throws Exception {
        int userId = 1;
        int providedUserId = 2;
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenThrow(new NullPointerException("Invalid category tag."));
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=" + userId + "&viewedUserId=" + providedUserId + "&listAll=true&tagId=1&tagType=Categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + providedUserId));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

    /**
     * Tests that the evidence Categories partial refresh will redirect a user to their account page when an invalid tag and user ID is given.
     * @throws Exception If mocking the MVC fails.
     */
    @Test
    void testViewEvidenceCategoriesUpdateRedirectToAccountWhenCategoryAndUserInvalid300() throws Exception {
        int providedUserId = 0; // 0 is used as an invalid user as it is the default set by the UserResponse Object.
        int realUserId = 1; // Same as the userAccountClientService.getUserIDFromAuthState id.
        UserResponse testUser = UserResponse.newBuilder().setId(providedUserId).build();

        when(registerClientService.getUserData(any(Integer.class))).thenReturn(testUser);
        when(evidenceService.getEvidencesWithCategory(any(Integer.class))).thenThrow(new NullPointerException("Invalid category tag."));
        when(categoryService.getAllCategories()).thenReturn(testCategories);

        mockMvc.perform(get("/switch-evidence-list?userId=" + realUserId + "&viewedUserId=" + providedUserId + "&listAll=true&tagId=1&tagType=Categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("account?userId=" + realUserId));

        verify(evidenceService, times(1)).getEvidencesWithCategory(any(Integer.class));
        verify(registerClientService, times(1)).getUserData(any(Integer.class));
    }

}
