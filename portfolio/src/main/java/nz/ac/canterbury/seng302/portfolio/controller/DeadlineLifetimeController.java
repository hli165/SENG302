package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.Deadline;
import nz.ac.canterbury.seng302.portfolio.service.DeadlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller that handles adding and deleting deadlines.
 */
@Controller
public class DeadlineLifetimeController {

    /**
     * Service for persisting deadlines.
     */
    @Autowired
    private DeadlineService deadlineService;

    /**
     * Saves the given deadline to the database and redirects the user to the details page.
     * @param deadline new deadline to be saved
     */
    @PostMapping("/add-deadline")
    public String deadlineSave(@ModelAttribute("deadline") Deadline deadline) {
        deadlineService.addDeadline(deadline);
        return "redirect:/details";
    }

    /**
     * Tried to delete a deadline with given id
     * @param id of the deadline being deleted
     */
    @GetMapping("/delete-deadline/{id}")
    public String deadlineRemove(@PathVariable("id") Integer id) {
        deadlineService.removeDeadline(id);
        return "redirect:/details";
    }
}