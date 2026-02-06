package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TermRepository extends JpaRepository<Term, String> {

    List<Term> findTop4ByTermEndDateAfterOrderByTermBeginDateAsc(LocalDateTime now);

    Term findByName(String termName);
}
