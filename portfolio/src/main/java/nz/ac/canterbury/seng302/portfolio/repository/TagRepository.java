package nz.ac.canterbury.seng302.portfolio.repository;

import java.util.List;
import nz.ac.canterbury.seng302.portfolio.model.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Repository of Tag objects.
 */
public interface TagRepository extends CrudRepository<Tag, Integer> {
    Optional<Tag> findById(int id);
    List<Tag> findByTagNameIgnoreCase(String tagName);
    List<Tag> findAll();
    void deleteById(int id);

}
