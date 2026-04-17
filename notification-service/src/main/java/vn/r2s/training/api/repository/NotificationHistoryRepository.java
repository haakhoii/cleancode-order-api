package vn.r2s.training.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.r2s.training.api.entity.NotificationHistory;

public interface NotificationHistoryRepository extends
    MongoRepository<NotificationHistory, String> {
}