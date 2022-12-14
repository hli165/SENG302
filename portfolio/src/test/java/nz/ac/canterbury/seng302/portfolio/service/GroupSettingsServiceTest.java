package nz.ac.canterbury.seng302.portfolio.service;

import org.hibernate.ObjectNotFoundException;
import nz.ac.canterbury.seng302.portfolio.model.GroupSettings;
import nz.ac.canterbury.seng302.portfolio.repository.GroupSettingsRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link GitLabApiService} class.
 * Mock the GroupSettingsService class and the GitLabAPI.
 */
@ExtendWith(MockitoExtension.class)
class GroupSettingsServiceTest {

    @Mock
    private GroupSettingsRepository repository;

    @InjectMocks
    private GroupSettingsService groupSettingsService;

    @Spy
    private GroupSettingsService groupSettingsServiceMock;

    private static final GroupSettings testGroupSettings = new GroupSettings(12345, "test repo", "kjbfdsouoih321312ewln", 1, "https://eng-git.canterbury.ac.nz");

    private static final String validRepoName = new String(new char[30]).replace('\0', 't');
    private static final String invalidRepoName = new String(new char[31]).replace('\0', 't');

    private static final String validRepoToken = new String(new char[50]).replace('\0', 't');
    private static final String invalidRepoToken = new String(new char[51]).replace('\0', 't');

    private static final int validRepoId = 12345;
    private static final int invalidRepoId = 2147483647;

    @BeforeAll
    static void setUp() {
        testGroupSettings.setGroupSettingsId(1);
    }

    /**
     * Test the getGroupSettingsByGroupId method when passing in a valid group id.
     */
    @Test
    void testGetGroupSettingsByValidGroupId() {
        when(repository.findByGroupId(testGroupSettings.getGroupId())).thenReturn(Optional.of(testGroupSettings));
        assertEquals(testGroupSettings, groupSettingsService.getGroupSettingsByGroupId(testGroupSettings.getGroupId()));
    }

    /**
     * Tests that the save method is called by the group settings repository with the correct argument when calling
     * the saveGroupSettings method.
     */
    @Test
    void saveGroupSettings() {
        when(repository.save(testGroupSettings)).thenReturn(testGroupSettings);
        GroupSettings savedGroupSettings = groupSettingsService.saveGroupSettings(testGroupSettings);
        assertEquals(testGroupSettings, savedGroupSettings);
        verify(repository).save(testGroupSettings);
    }

    /**
     * Tests that the deleteById method is called by the group settings repository with the correct argument when the
     * group it's trying to delete exists.
     */
    @Test
    void deleteGroupSettingsExists() {
        int groupSettingsId = testGroupSettings.getGroupSettingsId();
        doReturn(Optional.of(testGroupSettings)).when(repository).findById(groupSettingsId);
        doNothing().when(repository).deleteById(groupSettingsId);
        boolean success = groupSettingsService.deleteGroupSettings(groupSettingsId);
        assertTrue(success);
        verify(repository).deleteById(groupSettingsId);
    }

    /**
     * Tests that the deleteById method is not called by the group settings repository when the group it's trying to
     * delete does not exist.
     */
    @Test
    void deleteGroupSettingsDoesNotExist() {
        int groupSettingsId = testGroupSettings.getGroupSettingsId();
        doReturn(Optional.empty()).when(repository).findById(groupSettingsId);
        boolean success = groupSettingsService.deleteGroupSettings(groupSettingsId);
        assertFalse(success);
        verify(repository, times(0)).deleteById(groupSettingsId);
    }

    /**
     * Tests that the getGroupSettingsByGroupId method return false if the group settings does not exist.
     */
    @Test
    void checkRepositoryHasBeenSetUpWhenItIsNot() {
        GroupSettings test = new GroupSettings(0, "test repo", null, 1234, "https://eng-git.canterbury.ac.nz");
        lenient().when(groupSettingsService.getGroupSettingsByGroupId(test.getGroupId())).thenReturn(test);
        assertFalse(groupSettingsService.doesGroupHaveRepo(test.getGroupId()));
    }

    /**
     * Tests that the getGroupSettingsByGroupId method return true if the group settings does exist.
     */
    @Test
    void checkRepositoryHasBeenSetUpWhenItIs() {
        GroupSettings test = new GroupSettings(123, "test repo", "testKey", 1234,"https://eng-git.canterbury.ac.nz");
        doReturn(test).when(groupSettingsServiceMock).getGroupSettingsByGroupId(test.getGroupId());
        assertTrue(groupSettingsServiceMock.doesGroupHaveRepo(test.getGroupId()));
    }

    /**
     * Tests that the isGroupSettingSaved method returns true if given a valid group settings object.
     */
    @Test
    void checkIsGroupSettingSavedWhenItIs() {
        GroupSettings test = new GroupSettings(1, "test repo", "testKey", 1234, "https://eng-git.canterbury.ac.nz");
        test.setGroupSettingsId(1);
        assertTrue(groupSettingsService.isGroupSettingSaved(1,1, "test", "test", 1234, "https://eng-git.canterbury.ac.nz"));
    }

    /**
     * Checks that the isValidGroupSettings method returns true when all arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsValid() {
        assertTrue(groupSettingsService.isValidGroupSettings(validRepoId, validRepoName, validRepoToken));
    }

    /**
     * Checks that the isValidGroupSettings method returns false when the repo id is too large, but the rest of the
     * arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsInvalidRepoId() {
        assertFalse(groupSettingsService.isValidGroupSettings(invalidRepoId, validRepoName, validRepoToken));
    }

    /**
     * Checks that the isValidGroupSettings method returns false when the repo name is too long, but the rest of the
     * arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsInvalidRepoName() {
        assertFalse(groupSettingsService.isValidGroupSettings(validRepoId, invalidRepoName, validRepoToken));
    }

    /**
     * Checks that the isValidGroupSettings method returns false when the repo token is too long, but the rest of the
     * arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsInvalidRepoToken() {
        assertFalse(groupSettingsService.isValidGroupSettings(validRepoId, validRepoName, invalidRepoToken));
    }

    /**
     * Checks that the isValidGroupSettings method returns false when the repo name is blank, and the rest of the
     * arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsIsValidWhenRepoNameIsBlank() {
        assertTrue(groupSettingsService.isValidGroupSettings(validRepoId, "", validRepoToken));
    }

    /**
     * Checks that the isValidGroupSettings method returns false when the repo token is blank, and the rest of the
     * arguments are valid.
     */
    @Test
    void checkIsValidGroupSettingsIsValidWhenRepoTokenIsBlank() {
        assertTrue(groupSettingsService.isValidGroupSettings(validRepoId, validRepoName, ""));
    }
}