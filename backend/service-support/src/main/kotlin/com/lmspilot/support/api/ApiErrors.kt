package com.lmspilot.support.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

class ApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
) : RuntimeException(message)

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val correlationId: String?,
    val fieldErrors: Map<String, String> = emptyMap(),
)

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    @ExceptionHandler(ApiException::class)
    fun api(ex: ApiException, request: HttpServletRequest) = response(ex.status, ex.code, ex.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Không hợp lệ") }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", request, fields)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraint(ex: ConstraintViolationException, request: HttpServletRequest) =
        response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.message ?: "Dữ liệu không hợp lệ", request)

    @ExceptionHandler(AccessDeniedException::class)
    fun denied(ex: AccessDeniedException, request: HttpServletRequest) =
        response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này", request)

    @ExceptionHandler(Exception::class)
    fun unknown(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error(
            "Unhandled API error path={} correlationId={}",
            request.requestURI,
            request.getHeader("X-Correlation-Id"),
            ex,
        )
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Hệ thống gặp lỗi không mong muốn", request)
    }

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        fields: Map<String, String> = emptyMap(),
    ) = ResponseEntity.status(status).body(
        ApiError(
            status = status.value(),
            code = code,
            message = message,
            path = request.requestURI,
            correlationId = request.getHeader("X-Correlation-Id"),
            fieldErrors = fields,
        )
    )
}
