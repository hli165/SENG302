package nz.ac.canterbury.seng302.portfolio.repository;

import nz.ac.canterbury.seng302.portfolio.model.Event;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends CrudRepository<Event, Integer> {
    List<Event> findByEventName(String eventName);
    Event findById(int id);
    List<Event> findByParentProjectId(int parentProjectId);
    void deleteById(int id);

    List<Event> findAllByOrderByEventStartDate();
    List<Event> findAllByOrderByEventEndDate();
}
