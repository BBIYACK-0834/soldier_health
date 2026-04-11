package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserUnitSettingRepository extends JpaRepository<UserUnitSetting, Long> {
    Optional<UserUnitSetting> findByUserAndIsPrimaryTrue(User user);
}
