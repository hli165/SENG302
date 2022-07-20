package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.Event;
import nz.ac.canterbury.seng302.portfolio.model.Milestone;
import nz.ac.canterbury.seng302.portfolio.model.Sprint;
import nz.ac.canterbury.seng302.portfolio.repository.DeadlinesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/***
 * Service class for saving, deleting, updating and retrieving event objects to the database.
 */
@Service
public class DeadlineService {

    @Autowired
    private DeadlinesRepository repository;

    /**
     * Updates a deadline
     * @param deadline deadline to update it to
     * @return Newly edited event
     */
    public Deadline updateDeadline(Deadline deadline) {
        Optional<Deadline> sOptional = repository.findById((Integer) deadline.getId());

        if (sOptional.isPresent()) {
            Deadline deadlineUpdate = sOptional.get();
            deadlineUpdate.setDeadlineName(deadline.getDeadlineName());
            deadlineUpdate.setDeadlineDate(deadline.getDeadlineDate());


            deadlineUpdate = repository.save(deadlineUpdate);
            return deadlineUpdate;
        } else {
            return deadline;
        }
    }

    /**
     * Get deadline by Id
     * @param id id of event
     * @return deadline with the id that is the input
     * @throws Exception If event can't be found
     */
    public Deadline getDeadlineById(Integer id) throws Exception {
        Optional<Deadline> deadline = repository.findById(id);
        if(deadline.isPresent()) {
            return deadline.get();
        } else {

            throw new Exception("Event not found");
        }
    }

    /**
     * Get list of all deadlines
     * @return List of deadlines
     */
    public List<Deadline> getAllDeadlines() {
        return (List<Deadline>) repository.findAll();
    }

    /**
     * Get list of all deadlines ordered by date
     * @return List of deadlines ordered by date
     */
    public List<Deadline> getAllDeadlinesOrdered() {
        return repository.findAllByOrderByDeadlineDate();
    }


    /**
     * Saves the given deadline to the database and returns it.
     * @param deadline deadline to be saved
     * @return saved deadline
     */
    public Deadline addDeadline(Deadline deadline) {
        return repository.save(deadline);
    }


    /**
     * Removes the deadline by the given id from the database if it exists
     * @param id of the deadline to remove
     */
    public void removeDeadline(Integer id) {
        Optional<Deadline> sOptional = repository.findById(id);

        if (sOptional.isPresent()) {
            Deadline deadline = sOptional.get();
            repository.deleteById(deadline.getId());
        }
    }

    /**
     * Gets a list of deadlines that overlap with the given sprint in some way. This is to know what deadline should be
     * displayed with this sprint. It does this by checking if either of the dates are within the sprints dates.
     * @param sprint Sprint to check events against.
     * @return List of deadline that overlap with the given sprint.
     */
    public List<Deadline> getAllDeadlinesOverLappingWithSprint(Sprint sprint) {
        ArrayList<Deadline> deadlineList = (ArrayList<Deadline>) getAllDeadlinesOrdered();
        ArrayList<Deadline> deadlinesOverlapped = new ArrayList<>();

        for (Deadline currentDeadline : deadlineList) {
            if (validateDeadlineDateInSprintDateRange(currentDeadline, sprint)) {
                deadlinesOverlapped.add(currentDeadline);
            }
        }
        return deadlinesOverlapped;
    }

    /**
     * Validate if particular deadline date is in sprint date range
     * @param deadline The update deadline
     * @param sprint The sprint to compare with
     * @return True if deadline end date is in sprint date range
     */
    public boolean validateDeadlineDateInSprintDateRange(Deadline deadline, Sprint sprint) {
        Date deadlineDate = deadline.getDeadlineDate();
        Date sprintStartDate = sprint.getStartDate();
        Date sprintEndDate = sprint.getEndDate();

        // Convert deadlineDate to Calendar object
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(deadlineDate);

        // Convert sprint Start Date to Calendar object
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(sprintStartDate);

        // Convert sprint End Date to Calendar object
        Calendar calendar3 = Calendar.getInstance();
        calendar3.setTime(sprintEndDate);

        // Check if deadline is the sprint start day
        boolean isStartDay = calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);

        // Check if deadline is the sprint end day
        boolean isEndDay = calendar1.get(Calendar.YEAR) == calendar3.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar3.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar3.get(Calendar.DAY_OF_MONTH);

        return (deadlineDate.compareTo(sprintStartDate) >= 0 && deadlineDate.compareTo(sprintEndDate) <= 0)
                || isStartDay || isEndDay;
    }
}
