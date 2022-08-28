package nz.ac.canterbury.seng302.portfolio.service;

import org.hibernate.ObjectNotFoundException;
import nz.ac.canterbury.seng302.portfolio.model.GroupSettings;
import nz.ac.canterbury.seng302.portfolio.repository.GroupSettingsRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private static final GroupSettings testGroupSettings = new GroupSettings(12345, "test repo", "kjbfdsouoih321312ewln", 1);

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
     * Test the getGroupSettingsByGroupId method when passing in an invalid group id.
     */
    @Test
    void testGetGroupSettingsByInvalidGroupId() {
        when(repository.findByGroupId(testGroupSettings.getGroupId())).thenReturn(Optional.empty());
        int testGroupId = testGroupSettings.getGroupId();
        assertThrows(ObjectNotFoundException.class, () -> groupSettingsService.getGroupSettingsByGroupId(testGroupId));
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
}