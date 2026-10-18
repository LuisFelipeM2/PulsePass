package edu.unimagdalena.PulsePass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unimagdalena.PulsePass.domain.UserRole;
import edu.unimagdalena.PulsePass.domain.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole,UserRoleId> {
    List<UserRole> findByIdUserId(Long userId);
}
