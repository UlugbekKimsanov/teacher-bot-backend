package uz.sevenEdu.teacherBot.rating.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

public class RatingDto {

    @Data @Builder
    public static class AttendanceDto {
        private String courseName;
        private long weeklyMissed;
        private long monthlyMissed;
        private long quarterlyMissed;
    }

    @Data @Builder
    public static class PointsSummary {
        private List<PointEntry> entries;
        private long total;

        @Data @Builder
        public static class PointEntry {
            private String activity;
            private int amount;
        }
    }

    @Data @Builder
    public static class ProgressDto {
        private String courseName;
        private int vocabularyScore;
        private int testScore;
        private int questionsScore;
        private int maxScore;
    }

    @Data @Builder
    public static class CertificateDto {
        private Long id;
        private Long courseId;
        private String courseName;
        private String issuedAt;
    }
}
