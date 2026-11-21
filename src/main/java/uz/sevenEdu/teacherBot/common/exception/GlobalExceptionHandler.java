package uz.sevenEdu.teacherBot.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(NotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorized(UnauthorizedException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbidden(ForbiddenException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElseGet(() -> ex.getAllErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage() == null
                                ? "So'rov validatsiyadan o'tmadi"
                                : error.getDefaultMessage())
                        .orElse("So'rov validatsiyadan o'tmadi"));
        return ApiResponse.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .orElse("So'rov validatsiyadan o'tmadi");
        return ApiResponse.error(message);
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidInput(ServerWebInputException ex) {
        return ApiResponse.error("So'rov JSON formati noto'g'ri");
    }

    @ExceptionHandler({PayloadTooLargeException.class, DataBufferLimitException.class})
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handlePayloadTooLarge(Exception ex) {
        String message = ex instanceof PayloadTooLargeException
                ? ex.getMessage()
                : "Yuklanayotgan fayl ruxsat etilgan hajmdan katta";
        return ApiResponse.error(message);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** Bazadagi cheklov buzilishi (dublikat qiymat, bog'liq yozuv) — 500 emas, 409. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ApiResponse.error("Amalni bajarib bo'lmadi: bunday yozuv allaqachon mavjud yoki boshqa ma'lumotga bog'langan");
    }

    /** Framework statusli xatolari (mavjud bo'lmagan yo'l — 404, noto'g'ri metod — 405 va h.k.) o'z kodi bilan qaytadi. */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String message = code == 404 ? "Topilmadi"
                : code == 405 ? "Bu so'rov usuli qo'llab-quvvatlanmaydi"
                : "So'rovni bajarib bo'lmadi";
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.<Void>error(message));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception ex) {
        log.error("Unhandled server error", ex);
        return ApiResponse.error("Serverda kutilmagan xatolik yuz berdi");
    }
}
