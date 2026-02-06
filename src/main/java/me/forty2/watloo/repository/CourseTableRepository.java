package me.forty2.watloo.repository;

import me.forty2.watloo.entity.CourseTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseTableRepository extends JpaRepository<CourseTable, Long> {
    List<CourseTable> findAllByUserId(Long id);
}
