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

    @Data @Builder
    public static class StreakDto {
        private int currentStreak;
        private int longestStreak;
        /** ISO date strings of studied days (last 30) */
        private List<String> studiedDays;
        /** Weekday bitmask [Mon..Sun] — true = studied */
        private List<Boolean> weekDays;
    }

    @Data @Builder
    public static class LeaderboardDto {
        private List<LeaderboardEntry> entries;
        private int myRank;

        @Data @Builder
        public static class LeaderboardEntry {
            private int rank;
            private String name;
            private int points;
            private boolean isMe;
            private String avatarInitials;
        }
    }

    @Data @Builder
    public static class DailyGoalsDto {
        private int lessonsGoal;
        private int lessonsDone;
        private int minutesGoal;
        private int minutesDone;
        private int wordsGoal;
        private int wordsDone;
    }
}
