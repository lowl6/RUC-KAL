package cn.edu.ruc.kal.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
        select u from User u
        where (:keyword is null or :keyword = ''
            or lower(u.email) like lower(concat('%', :keyword, '%'))
            or lower(u.name)  like lower(concat('%', :keyword, '%'))
            or lower(u.displayName) like lower(concat('%', :keyword, '%')))
          and (:role is null or u.role = :role)
          and (:status is null or u.status = :status)
        order by u.createdAt desc
        """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") User.Role role,
                      @Param("status") User.Status status,
                      Pageable pageable);

    long countByRole(User.Role role);
    long countByStatus(User.Status status);

    List<User> findByRoleAndStatusOrderByDisplayNameAsc(User.Role role, User.Status status);
}
