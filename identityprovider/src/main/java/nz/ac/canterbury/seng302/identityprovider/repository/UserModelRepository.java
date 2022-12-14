package nz.ac.canterbury.seng302.identityprovider.repository;

import nz.ac.canterbury.seng302.identityprovider.model.UserModel;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@Repository
public interface UserModelRepository extends CrudRepository<UserModel, Integer> {

    UserModel findByUserId(int userId);

    boolean existsByUserId(int userId);

    List<UserModel> findByUsername(String username);
}
