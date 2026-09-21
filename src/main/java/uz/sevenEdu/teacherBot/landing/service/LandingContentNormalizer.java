package uz.sevenEdu.teacherBot.landing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Versionless/V1 kontentni o'qiydi va tashqariga doim kanonik V2 qaytaradi. */
@Slf4j
@Component
public class LandingContentNormalizer {

    private static final String DEFAULT_CONTENT = "landing/default-content-v2.json";
    private static final String[] FEATURE_ICONS = {
            "GraduationCap", "ClipboardCheck", "MessageCircleQuestion", "Clock"
    };
    private static final String[] COURSE_INFO_ICONS = {"Video", "Sparkles", "Bot", "Award"};
    private static final String[] GOAL_ICONS = {"MessagesSquare", "Brain", "Timer"};
    private static final String[] COURSE_IDS = {"en", "ru", "ko", "tr", "ar", "de", "zh"};

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ObjectNode defaults;

    public LandingContentNormalizer(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.defaults = readDefaults();
    }

    public LandingContentDto normalize(String storedJson) {
        if (storedJson == null || storedJson.isBlank()) return defaultContent();
        try {
            return normalize(objectMapper.readTree(storedJson));
        } catch (Exception ex) {
            log.error("Landing kontenti JSON sifatida o'qilmadi; V2 default ishlatiladi", ex);
            return defaultContent();
        }
    }

    public LandingContentDto normalize(JsonNode source) {
        if (source == null || !source.isObject()) return defaultContent();
        int sourceVersion = source.path("schemaVersion").asInt(0);
        if (sourceVersion > 2) {
            log.error("Qo'llab-quvvatlanmaydigan landing schemaVersion={}; V2 default ishlatiladi", sourceVersion);
            return defaultContent();
        }

        ObjectNode legacy = ((ObjectNode) source).deepCopy();
        normalizeTextItems(legacy, "features", FEATURE_ICONS);
        normalizeTextItems(legacy, "courseInfo", COURSE_INFO_ICONS);
        normalizeGoals(legacy);
        normalizeCourses(legacy);

        ObjectNode result = defaults.deepCopy();
        deepMerge(result, legacy);
        result.put("schemaVersion", 2);
        return convertOrDefault(result);
    }

    public LandingContentDto defaultContent() {
        return convert(defaults.deepCopy());
    }

    public String write(LandingContentDto content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            throw new IllegalStateException("Landing kontentini saqlash uchun JSON yaratib bo'lmadi", ex);
        }
    }

    private void normalizeTextItems(ObjectNode root, String field, String[] defaultIcons) {
        JsonNode value = root.get(field);
        if (value == null || !value.isArray()) return;

        ArrayNode normalized = objectMapper.createArrayNode();
        int index = 0;
        for (JsonNode item : value) {
            if (item.isObject()) {
                ObjectNode object = ((ObjectNode) item).deepCopy();
                if (!object.hasNonNull("text")) {
                    object.put("text", object.path("title").asText(""));
                }
                if (!object.hasNonNull("icon") || object.path("icon").asText().isBlank()) {
                    object.put("icon", valueAt(defaultIcons, index, "Sparkles"));
                }
                object.remove("title");
                object.remove("description");
                normalized.add(object);
            }
            index++;
        }
        root.set(field, normalized);
    }

    private void normalizeGoals(ObjectNode root) {
        JsonNode value = root.get("goals");
        if (value == null || !value.isArray()) return;

        ArrayNode normalized = objectMapper.createArrayNode();
        int index = 0;
        for (JsonNode item : value) {
            if (item.isObject()) {
                ObjectNode object = ((ObjectNode) item).deepCopy();
                if (!object.hasNonNull("icon") || object.path("icon").asText().isBlank()) {
                    object.put("icon", valueAt(GOAL_ICONS, index, "Sparkles"));
                }
                object.remove("description");
                normalized.add(object);
            }
            index++;
        }
        root.set("goals", normalized);
    }

    private void normalizeCourses(ObjectNode root) {
        JsonNode value = root.get("courses");
        if (value == null || !value.isArray()) return;

        ArrayNode normalized = objectMapper.createArrayNode();
        int index = 0;
        for (JsonNode item : value) {
            if (item.isObject()) {
                ObjectNode object = ((ObjectNode) item).deepCopy();
                if (!object.hasNonNull("name")) {
                    object.put("name", object.path("title").asText(""));
                }
                if (!object.hasNonNull("id") || object.path("id").asText().isBlank()) {
                    object.put("id", valueAt(COURSE_IDS, index, "legacy-" + (index + 1)));
                } else if (!object.get("id").isTextual()) {
                    object.put("id", object.path("id").asText());
                }
                object.remove("title");
                normalized.add(object);
            }
            index++;
        }
        root.set("courses", normalized);
    }

    private void deepMerge(ObjectNode target, ObjectNode source) {
        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode sourceValue = field.getValue();
            if (sourceValue == null || sourceValue.isNull()) continue;

            JsonNode targetValue = target.get(field.getKey());
            if (sourceValue.isObject() && targetValue != null && targetValue.isObject()) {
                deepMerge((ObjectNode) targetValue, (ObjectNode) sourceValue);
            } else {
                target.set(field.getKey(), sourceValue.deepCopy());
            }
        }
    }

    private LandingContentDto convertOrDefault(ObjectNode node) {
        try {
            LandingContentDto content = convert(node);
            Set<ConstraintViolation<LandingContentDto>> violations = validator.validate(content);
            if (violations.isEmpty()) return content;
            ConstraintViolation<LandingContentDto> violation = violations.iterator().next();
            String reason = violation.getPropertyPath() + ": " + violation.getMessage();
            log.error("Eski landing kontenti V2 sxemaga mos emas ({}); V2 default ishlatiladi", reason);
        } catch (Exception ex) {
            log.error("Eski landing kontentini V2 sxemaga o'tkazib bo'lmadi; V2 default ishlatiladi", ex);
        }
        return defaultContent();
    }

    private LandingContentDto convert(ObjectNode node) {
        try {
            return objectMapper.treeToValue(node, LandingContentDto.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Kanonik landing kontentini o'qib bo'lmadi", ex);
        }
    }

    private ObjectNode readDefaults() {
        try (InputStream input = new ClassPathResource(DEFAULT_CONTENT).getInputStream()) {
            JsonNode node = objectMapper.readTree(input);
            if (!node.isObject()) throw new IllegalStateException("Landing default kontenti obyekt emas");
            return (ObjectNode) node;
        } catch (Exception ex) {
            throw new IllegalStateException("Landing V2 default kontentini yuklab bo'lmadi", ex);
        }
    }

    private String valueAt(String[] values, int index, String fallback) {
        return index >= 0 && index < values.length ? values[index] : fallback;
    }
}
