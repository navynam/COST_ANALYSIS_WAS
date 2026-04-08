package com.costanalysis.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,   "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN,           "계정이 잠겨 있습니다. 관리자에게 문의하세요."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,          "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,          "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,           "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,                 "권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,            "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMPLOYEE_ID(HttpStatus.CONFLICT,      "이미 사용 중인 사번입니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST,          "현재 비밀번호가 일치하지 않습니다."),

    // Quotation
    QUOTATION_NOT_FOUND(HttpStatus.NOT_FOUND,       "견적서를 찾을 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST,   "지원하지 않는 파일 형식입니다. (PDF, XLSX, XLS 만 지원)"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST,          "파일 크기가 50MB를 초과합니다."),
    PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"파싱 처리 중 오류가 발생했습니다."),

    // Verification
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,    "검증 결과를 찾을 수 없습니다."),

    // Comparison
    COMPARISON_NOT_FOUND(HttpStatus.NOT_FOUND,      "비교 세션을 찾을 수 없습니다."),

    // Insight
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND,         "인사이트 세션을 찾을 수 없습니다."),
    CLAUDE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE,"AI 서비스 연결에 실패했습니다."),

    // Formula
    FORMULA_NOT_FOUND(HttpStatus.NOT_FOUND,         "수식을 찾을 수 없습니다."),
    SYSTEM_FORMULA_PROTECTED(HttpStatus.FORBIDDEN,  "시스템 수식은 삭제할 수 없습니다."),

    // ChangeRequest
    CHANGE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,  "변경 요청을 찾을 수 없습니다."),

    // ParsingNote
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND,            "노트를 찾을 수 없습니다."),

    // File Storage
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"파일 다운로드에 실패했습니다."),

    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST,             "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String     message;

    ErrorCode(HttpStatus status, String message) {
        this.status  = status;
        this.message = message;
    }
}
