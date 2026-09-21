package uz.sevenEdu.teacherBot.landing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;

import static org.assertj.core.api.Assertions.assertThat;

class LandingContentNormalizerTest {

    private ObjectMapper objectMapper;
    private Validator validator;
    private LandingContentNormalizer normalizer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        normalizer = new LandingContentNormalizer(objectMapper, validator);
    }

    @Test
    void versionlessLegacyContentIsConvertedToCanonicalV2() throws Exception {
        JsonNode legacy = objectMapper.readTree("""
                {
                  "features": [{"title":"Eski afzallik","description":""}],
                  "courseInfo": [{"title":"Eski kurs ma'lumoti","description":""}],
                  "goals": [{"title":"Eski maqsad","description":""}],
                  "courses": [{
                    "flag":"GB", "title":"Eski kurs", "students":7,
                    "price":"100 so'm", "rating":4.5
                  }],
                  "bonusText":"Admin saqlagan matn"
                }
                """);

        LandingContentDto result = normalizer.normalize(legacy);

        assertThat(result.getSchemaVersion()).isEqualTo(2);
        assertThat(result.getFeatures().get(0).getIcon()).isEqualTo("GraduationCap");
        assertThat(result.getFeatures().get(0).getText()).isEqualTo("Eski afzallik");
        assertThat(result.getCourseInfo().get(0).getText()).isEqualTo("Eski kurs ma'lumoti");
        assertThat(result.getGoals().get(0).getIcon()).isEqualTo("MessagesSquare");
        assertThat(result.getCourses().get(0).getId()).isEqualTo("en");
        assertThat(result.getCourses().get(0).getName()).isEqualTo("Eski kurs");
        assertThat(result.getBonusText()).isEqualTo("Admin saqlagan matn");
        assertThat(result.getCourseCard().getStudentsLabel()).isEqualTo("o'quvchi");
        assertThat(result.getLeadForm().getOptionalLabel()).isEqualTo("ixtiyoriy");
        assertThat(validator.validate(result)).isEmpty();
    }

    @Test
    void canonicalV2RoundTripKeepsContent() throws Exception {
        LandingContentDto original = normalizer.defaultContent();

        LandingContentDto result = normalizer.normalize(normalizer.write(original));

        assertThat(result).usingRecursiveComparison().isEqualTo(original);
        assertThat(validator.validate(result)).isEmpty();
    }

    @Test
    void futureSchemaIsNotSilentlyDownConverted() throws Exception {
        JsonNode future = objectMapper.valueToTree(normalizer.defaultContent());
        ((com.fasterxml.jackson.databind.node.ObjectNode) future).put("schemaVersion", 3);
        ((com.fasterxml.jackson.databind.node.ObjectNode) future).put("featuresTitle", "Future-only title");

        LandingContentDto result = normalizer.normalize(future);

        assertThat(result.getSchemaVersion()).isEqualTo(2);
        assertThat(result.getFeaturesTitle()).isNotEqualTo("Future-only title");
        assertThat(result).usingRecursiveComparison().isEqualTo(normalizer.defaultContent());
    }

    @Test
    void malformedStoredValueFallsBackToDefault() {
        LandingContentDto result = normalizer.normalize("not-json");

        assertThat(result).usingRecursiveComparison().isEqualTo(normalizer.defaultContent());
    }
}
