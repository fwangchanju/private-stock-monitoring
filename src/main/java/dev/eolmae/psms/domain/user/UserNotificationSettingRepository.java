package dev.eolmae.psms.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

	Optional<UserNotificationSetting> findByUserUserKey(String userKey);
}
