package com.moviebooking.repository;

import com.moviebooking.entity.OutingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutingMemberRepository extends JpaRepository<OutingMember, Long> {

    List<OutingMember> findByOutingId(Long outingId);

    List<OutingMember> findByUserId(Long userId);

    boolean existsByOutingIdAndUserId(Long outingId, Long userId);
}
