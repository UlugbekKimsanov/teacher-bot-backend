package uz.sevenEdu.teacherBot.appversion;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AppVersionRepository extends ReactiveCrudRepository<AppVersion, Integer> {
}
