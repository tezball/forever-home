package com.example.foreverhome.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Structured logger for tracking user journeys through the application.
 * Uses MDC (Mapped Diagnostic Context) to add contextual information to logs.
 */
@Component
public class UserJourneyLogger {

    private static final Logger logger = LoggerFactory.getLogger(UserJourneyLogger.class);

    // Action constants for user journey tracking
    public static final String ACTION_REGISTER = "USER_REGISTER";
    public static final String ACTION_LOGIN = "USER_LOGIN";
    public static final String ACTION_LOGOUT = "USER_LOGOUT";
    public static final String ACTION_EMAIL_VERIFY = "EMAIL_VERIFY";
    public static final String ACTION_RESEND_VERIFICATION = "RESEND_VERIFICATION";
    public static final String ACTION_PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_PROFILE_COMPLETE = "PROFILE_COMPLETE";

    public static final String ACTION_PET_CREATE = "PET_CREATE";
    public static final String ACTION_PET_UPDATE = "PET_UPDATE";
    public static final String ACTION_PET_SUBMIT = "PET_SUBMIT";
    public static final String ACTION_PET_ACCEPT = "PET_ACCEPT";
    public static final String ACTION_PET_DECLINE = "PET_DECLINE";
    public static final String ACTION_PET_VIEW = "PET_VIEW";
    public static final String ACTION_PET_SIGNOFF = "PET_VET_SIGNOFF";
    public static final String ACTION_PET_WITHDRAW = "PET_WITHDRAW";

    public static final String ACTION_APPLICATION_CREATE = "APPLICATION_CREATE";
    public static final String ACTION_APPLICATION_APPROVE = "APPLICATION_APPROVE";
    public static final String ACTION_APPLICATION_REJECT = "APPLICATION_REJECT";
    public static final String ACTION_APPLICATION_WITHDRAW = "APPLICATION_WITHDRAW";

    public static final String ACTION_ADOPTION_COMPLETE = "ADOPTION_COMPLETE";

    public static final String ACTION_FAVORITE_ADD = "FAVORITE_ADD";
    public static final String ACTION_FAVORITE_REMOVE = "FAVORITE_REMOVE";

    public static final String ACTION_ADMIN_APPROVE_ORG = "ADMIN_APPROVE_ORG";
    public static final String ACTION_ADMIN_APPROVE_VET = "ADMIN_APPROVE_VET";
    public static final String ACTION_ADMIN_SUSPEND_USER = "ADMIN_SUSPEND_USER";
    public static final String ACTION_ADMIN_REACTIVATE_USER = "ADMIN_REACTIVATE_USER";
    public static final String ACTION_ADMIN_RESET_PASSWORD = "ADMIN_RESET_PASSWORD";
    public static final String ACTION_ADMIN_APPROVE_RESCUE = "ADMIN_APPROVE_RESCUE";
    public static final String ACTION_ADMIN_REJECT_RESCUE = "ADMIN_REJECT_RESCUE";
    public static final String ACTION_ADMIN_DELETE_USER = "ADMIN_DELETE_USER";

    // Content moderation actions
    public static final String ACTION_FLAG_CREATE = "FLAG_CREATE";
    public static final String ACTION_FLAG_APPROVE = "FLAG_APPROVE";
    public static final String ACTION_FLAG_DISMISS = "FLAG_DISMISS";

    // Email events
    public static final String ACTION_EMAIL_SENT = "EMAIL_SENT";
    public static final String ACTION_EMAIL_FAILED = "EMAIL_FAILED";

    /**
     * Log a user journey event with full context.
     */
    public void logAction(String action, String message) {
        logger.info("[{}] {}", action, message);
    }

    /**
     * Log a user journey event with timing information.
     */
    public void logActionWithDuration(String action, String message, long durationMs) {
        try {
            MDC.put("durationMs", String.valueOf(durationMs));
            logger.info("[{}] {} ({}ms)", action, message, durationMs);
        } finally {
            MDC.remove("durationMs");
        }
    }

    /**
     * Set user context for the current request.
     * Should be called early in request processing (e.g., in a filter).
     */
    public void setUserContext(UUID userId, String email, String role) {
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }
        if (email != null) {
            MDC.put("userEmail", email);
        }
        if (role != null) {
            MDC.put("userRole", role);
        }
    }

    /**
     * Set a session ID for tracking user sessions.
     */
    public void setSessionId(String sessionId) {
        if (sessionId != null) {
            MDC.put("sessionId", sessionId);
        }
    }

    /**
     * Set a trace ID for distributed tracing.
     */
    public void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
    }

    /**
     * Set resource context for the current operation.
     */
    public void setResourceContext(String resourceType, String resourceId) {
        if (resourceType != null) {
            MDC.put("resourceType", resourceType);
        }
        if (resourceId != null) {
            MDC.put("resourceId", resourceId);
        }
    }

    /**
     * Set pet-specific context.
     */
    public void setPetContext(UUID petId, String petStatus) {
        if (petId != null) {
            MDC.put("petId", petId.toString());
        }
        if (petStatus != null) {
            MDC.put("petStatus", petStatus);
        }
    }

    /**
     * Set application-specific context.
     */
    public void setApplicationContext(UUID applicationId) {
        if (applicationId != null) {
            MDC.put("applicationId", applicationId.toString());
        }
    }

    /**
     * Clear all MDC context. Should be called at the end of request processing.
     */
    public void clearContext() {
        MDC.clear();
    }

    /**
     * Log auth events.
     */
    public void logAuth(String action, String email, boolean success, String details) {
        try {
            MDC.put("action", action);
            MDC.put("userEmail", email);
            if (success) {
                logger.info("[{}] Success: {} - {}", action, email, details);
            } else {
                logger.warn("[{}] Failed: {} - {}", action, email, details);
            }
        } finally {
            MDC.remove("action");
        }
    }

    /**
     * Log pet workflow events.
     */
    public void logPetAction(String action, UUID petId, String petName, String status, String details) {
        try {
            MDC.put("action", action);
            MDC.put("petId", petId != null ? petId.toString() : "");
            MDC.put("petStatus", status != null ? status : "");
            MDC.put("resourceType", "PET");
            MDC.put("resourceId", petId != null ? petId.toString() : "");
            logger.info("[{}] Pet: {} ({}) - Status: {} - {}", action, petName, petId, status, details);
        } finally {
            MDC.remove("action");
            MDC.remove("petId");
            MDC.remove("petStatus");
            MDC.remove("resourceType");
            MDC.remove("resourceId");
        }
    }

    /**
     * Log adoption application events.
     */
    public void logApplicationAction(String action, UUID applicationId, UUID petId, UUID adopterId, String details) {
        try {
            MDC.put("action", action);
            MDC.put("applicationId", applicationId != null ? applicationId.toString() : "");
            MDC.put("petId", petId != null ? petId.toString() : "");
            MDC.put("resourceType", "APPLICATION");
            MDC.put("resourceId", applicationId != null ? applicationId.toString() : "");
            logger.info("[{}] Application: {} for Pet: {} by Adopter: {} - {}",
                action, applicationId, petId, adopterId, details);
        } finally {
            MDC.remove("action");
            MDC.remove("applicationId");
            MDC.remove("petId");
            MDC.remove("resourceType");
            MDC.remove("resourceId");
        }
    }

    /**
     * Log admin actions.
     */
    public void logAdminAction(String action, String targetType, UUID targetId, String details) {
        try {
            MDC.put("action", action);
            MDC.put("resourceType", targetType);
            MDC.put("resourceId", targetId != null ? targetId.toString() : "");
            logger.info("[{}] Admin action on {}: {} - {}", action, targetType, targetId, details);
        } finally {
            MDC.remove("action");
            MDC.remove("resourceType");
            MDC.remove("resourceId");
        }
    }

    /**
     * Log content moderation events.
     */
    public void logModeration(String action, UUID flagId, String contentType, UUID contentId, String details) {
        try {
            MDC.put("action", action);
            MDC.put("flagId", flagId != null ? flagId.toString() : "");
            MDC.put("contentType", contentType);
            MDC.put("resourceType", "FLAG");
            MDC.put("resourceId", flagId != null ? flagId.toString() : "");
            logger.info("[{}] Flag {} on {} {} - {}", action, flagId, contentType, contentId, details);
        } finally {
            MDC.remove("action");
            MDC.remove("flagId");
            MDC.remove("contentType");
            MDC.remove("resourceType");
            MDC.remove("resourceId");
        }
    }

    /**
     * Log email events.
     */
    public void logEmail(String action, String emailType, String recipient, boolean success, String details) {
        try {
            MDC.put("action", action);
            MDC.put("emailType", emailType);
            MDC.put("recipient", recipient);
            MDC.put("success", String.valueOf(success));
            if (success) {
                logger.info("[{}] {} to {} - {}", action, emailType, recipient, details);
            } else {
                logger.warn("[{}] {} to {} failed - {}", action, emailType, recipient, details);
            }
        } finally {
            MDC.remove("action");
            MDC.remove("emailType");
            MDC.remove("recipient");
            MDC.remove("success");
        }
    }
}
