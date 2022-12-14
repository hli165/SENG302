package nz.ac.canterbury.seng302.portfolio.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity that models a group that users can join. A group is made up of a group id, short name, long name, and a
 * list of members.
 */
@Entity
@Table(name = "group_model",
        uniqueConstraints={
        @UniqueConstraint(columnNames = {"short_Name", "course_Id"}),
        @UniqueConstraint(columnNames = {"long_Name", "course_Id"})
})
public class Group {

    /**
     * Id of the group.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int groupId;

    /**
     * Short name of the group.
     */
    @Column(name="short_name", length=15)
    private String shortName;

    /**
     * Long name of the group.
     */
    @Column(name="long_name", length=50)
    private String longName;

    /**
     * Id of the course instance the group is a part of.
     */
    @Column(name="course_id")
    private int courseId;

    /**
     * Set of user ids of the members of the group.
     */
    @ElementCollection
    @CollectionTable(name="user_to_group", joinColumns=@JoinColumn(name="group_id"))
    @Column(name="user_id")
    private Set<Integer> memberIds = new HashSet<>();

    /**
     * Empty constructor for JPA.
     */
    public Group() {}

    /**
     * Constructs a Group object.
     * @param shortName short name of the group
     * @param longName long name of the group
     * @param courseId Id of the course instance the group is a part of
     */
    public Group(String shortName, String longName, int courseId) {
        this.shortName = shortName;
        this.longName = longName;
        this.courseId = courseId;
    }

    /**
     * Returns the group id.
     * @return group id
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Sets the group id.
     * @param groupId group id
     */
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the group's short name.
     * @return group's short name
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Sets the group's short name.
     * @param shortName group's short name
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the group's long name.
     * @return group's long name
     */
    public String getLongName() {
        return longName;
    }

    /**
     * Sets the group's long name.
     * @param longName group's long name
     */
    public void setLongName(String longName) {
        this.longName = longName;
    }

    /**
     * Returns the id of the course instance the group is a part of.
     * @return id of the course instance the group is a part of
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * Sets the id of the course instance the group is a part of.
     * @param courseId id of the course instance the group is a part of
     */
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    /**
     * Returns the ids of the users a part of the group.
     * @return ids of the users a part of the group
     */
    public Set<Integer> getMemberIds() {
        return memberIds;
    }

    /**
     * Adds the user represented by the given user id to the group.
     * @param userId id of user to add to the group
     */
    public void addMember(int userId) {
        memberIds.add(userId);
    }

    /**
     * Removes the user represented by the given user id from the group.
     * @param userId id of user to remove from the group
     */
    public void removeMember(int userId) {
        memberIds.remove(userId);
    }
}
