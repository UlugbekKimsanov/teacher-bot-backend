package uz.sevenEdu.teacherBot.landing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Landing va admin muharriri o'rtasidagi kanonik, versiyalangan kontrakt. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandingContentDto {

    private static final String HTTP_URL = "^(?:|/(?!/)[^\\s]*|https?://[^\\s]+)$";
    private static final String ASSET_URL = "^(?:|/(?!/)[^\\s]*|https?://[^\\s]+|landing/[A-Za-z0-9._-]+)$";
    private static final String NAV_URL = "^(?:#[A-Za-z0-9_-]*|/(?!/)[^\\s]*|https?://[^\\s]+)$";

    @NotNull
    @Min(2)
    @Max(2)
    private Integer schemaVersion;

    @NotNull
    @Valid
    private Navbar navbar;

    @NotNull
    @Valid
    private Hero hero;

    @NotNull
    @Size(max = 20)
    @Valid
    private List<Stat> stats;

    @NotBlank
    @Size(max = 255)
    private String featuresTitle;

    @NotNull
    @Size(max = 50)
    @Valid
    private List<Feature> features;

    @NotBlank
    @Size(max = 255)
    private String courseInfoTitle;

    @NotNull
    @Size(max = 50)
    @Valid
    private List<Feature> courseInfo;

    @NotBlank
    @Size(max = 255)
    private String goalsTitle;

    @NotNull
    @Size(max = 50)
    @Valid
    private List<Goal> goals;

    @NotBlank
    @Size(max = 255)
    private String testimonialsTitle;

    @NotNull
    @Size(max = 100)
    @Valid
    private List<Testimonial> testimonials;

    @NotBlank
    @Size(max = 255)
    private String coursesTitle;

    @NotNull
    @Valid
    private CourseCard courseCard;

    @NotNull
    @Size(max = 100)
    @Valid
    private List<Course> courses;

    @NotBlank
    @Size(max = 255)
    private String bonusTitle;

    @NotBlank
    @Size(max = 4000)
    private String bonusText;

    @NotNull
    @Size(max = 100)
    @Valid
    private List<BonusBook> bonusBooks;

    @NotNull
    @Valid
    private Cta cta;

    @NotNull
    @Valid
    private LeadForm leadForm;

    @NotNull
    @Valid
    private LeadModal leadModal;

    @NotNull
    @Valid
    private Footer footer;

    @NotNull
    @Valid
    private Contacts contacts;

    @JsonIgnore
    @AssertTrue(message = "Kurs, fikr va bonus IDlari takrorlanmasligi kerak")
    public boolean hasUniqueItemIds() {
        return unique(courses == null ? null : courses.stream().map(Course::getId).toList())
                && unique(testimonials == null ? null : testimonials.stream().map(Testimonial::getId).toList())
                && unique(bonusBooks == null ? null : bonusBooks.stream().map(BonusBook::getId).toList());
    }

    private static boolean unique(List<?> values) {
        if (values == null) return true;
        Set<Object> seen = new HashSet<>();
        return values.stream().filter(Objects::nonNull).allMatch(seen::add);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Navbar {
        @NotBlank @Size(max = 100)
        private String logo;
        @NotNull @Size(max = 20) @Valid
        private List<NavLink> links;
        @NotBlank @Size(max = 100)
        private String ctaLabel;
        @NotBlank @Size(max = 100)
        private String menuLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavLink {
        @NotBlank @Size(max = 100)
        private String label;
        @NotBlank @Size(max = 2048) @Pattern(regexp = NAV_URL, message = "xavfsiz #anchor yoki http(s) URL bo'lishi kerak")
        private String href;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hero {
        @NotBlank @Size(max = 1000)
        private String title;
        @NotBlank @Size(max = 4000)
        private String subtitle;
        @NotBlank @Size(max = 100)
        private String primaryBtn;
        @NotBlank @Size(max = 100)
        private String secondaryBtn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stat {
        @NotBlank @Size(max = 100)
        private String value;
        @NotBlank @Size(max = 100)
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feature {
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]*$", message = "Lucide ikonka nomi noto'g'ri")
        private String icon;
        @NotBlank @Size(max = 1000)
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Goal {
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]*$", message = "Lucide ikonka nomi noto'g'ri")
        private String icon;
        @NotBlank @Size(max = 1000)
        private String title;
        @Size(max = 2048) @Pattern(regexp = ASSET_URL, message = "rasm xavfsiz http(s) URL yoki landing/ yo'li bo'lishi kerak")
        private String image;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Testimonial {
        @NotNull @Positive
        private Long id;
        @NotBlank @Size(max = 255)
        private String name;
        @NotBlank @Size(max = 255)
        private String course;
        @Size(max = 2048) @Pattern(regexp = HTTP_URL, message = "video URL http(s) bo'lishi kerak")
        private String videoUrl;
        @Size(max = 2048) @Pattern(regexp = ASSET_URL, message = "rasm xavfsiz http(s) URL yoki landing/ yo'li bo'lishi kerak")
        private String image;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseCard {
        @NotBlank @Size(max = 100)
        private String categoryLabel;
        @NotBlank @Size(max = 100)
        private String studentsLabel;
        @NotBlank @Size(max = 100)
        private String buyLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Course {
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "kurs IDsi noto'g'ri")
        private String id;
        @NotBlank @Size(max = 32)
        private String flag;
        @NotBlank @Size(max = 255)
        private String name;
        @NotNull @PositiveOrZero
        private Integer students;
        @NotBlank @Size(max = 100)
        private String price;
        @NotNull @DecimalMin("0.0") @DecimalMax("5.0")
        private Double rating;
        @Size(max = 2048) @Pattern(regexp = HTTP_URL, message = "xarid URL http(s) bo'lishi kerak")
        private String buyUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusBook {
        @NotNull @Positive
        private Long id;
        @NotBlank @Size(max = 255)
        private String title;
        @Size(max = 2048) @Pattern(regexp = ASSET_URL, message = "muqova xavfsiz http(s) URL yoki landing/ yo'li bo'lishi kerak")
        private String cover;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cta {
        @NotBlank @Size(max = 255)
        private String title;
        @NotBlank @Size(max = 4000)
        private String description;
        @NotBlank @Size(max = 100)
        private String nameLabel;
        @NotBlank @Size(max = 100)
        private String phoneLabel;
        @NotBlank @Size(max = 100)
        private String submitLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadForm {
        @NotBlank @Size(max = 100)
        private String optionalLabel;
        @NotBlank @Size(max = 255)
        private String namePlaceholder;
        @NotBlank @Size(max = 255)
        private String phonePlaceholder;
        @NotBlank @Size(max = 100)
        private String loadingLabel;
        @NotBlank @Size(max = 1000)
        private String invalidPhoneMessage;
        @NotBlank @Size(max = 1000)
        private String successMessage;
        @NotBlank @Size(max = 1000)
        private String errorMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadModal {
        @NotBlank @Size(max = 255)
        private String title;
        @NotBlank @Size(max = 4000)
        private String description;
        @NotBlank @Size(max = 100)
        private String closeLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Footer {
        @NotBlank @Size(max = 255)
        private String company;
        @NotBlank @Size(max = 255)
        private String socialTitle;
        @NotBlank @Size(max = 255)
        private String contactTitle;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contacts {
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[+0-9()\\-\\s]{5,50}$", message = "telefon formati noto'g'ri")
        private String phone;
        @Size(max = 2048) @Pattern(regexp = HTTP_URL, message = "Telegram URL http(s) bo'lishi kerak")
        private String telegram;
        @Size(max = 2048) @Pattern(regexp = HTTP_URL, message = "Instagram URL http(s) bo'lishi kerak")
        private String instagram;
        @Size(max = 2048) @Pattern(regexp = HTTP_URL, message = "YouTube URL http(s) bo'lishi kerak")
        private String youtube;
    }
}
