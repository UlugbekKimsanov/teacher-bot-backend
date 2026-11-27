package uz.sevenEdu.teacherBot.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin hisobotlari uchun umumiy agregat qator: label + qiymat. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRow {
    private String label;
    private Long value;
}
