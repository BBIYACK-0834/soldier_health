package com.teukgeupjeonsa.backend.equipment;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEquipmentRepository extends JpaRepository<UserEquipment, Long> {
    List<UserEquipment> findByUser(User user);
    void deleteByUser(User user);
}
