package uz.sevenEdu.teacherBot.landing.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("landing_content")
public class LandingContent {
    @Id
    private Long id;
    // JSON kontent TEXT ustunda saqlanadi (R2DBC bilan jsonb'dan ko'ra sodda va ishonchli)
    private String content;
}
