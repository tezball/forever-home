package com.example.foreverhome.moderation.config;

import com.example.foreverhome.moderation.domain.*;
import com.example.foreverhome.moderation.domain.admin.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Arrays;

/**
 * JDBC configuration for custom type conversions and PostgreSQL dialect.
 */
@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @Bean
    @Override
    public JdbcDialect jdbcDialect(NamedParameterJdbcOperations operations) {
        return JdbcPostgresDialect.INSTANCE;
    }

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
                // JobStatus converters
                new JobStatusToStringConverter(),
                new StringToJobStatusConverter(),
                // ModerationStatus converters
                new ModerationStatusToStringConverter(),
                new StringToModerationStatusConverter(),
                // ContentType converters
                new ContentTypeToStringConverter(),
                new StringToContentTypeConverter(),
                // ModerationCategory converters
                new ModerationCategoryToStringConverter(),
                new StringToModerationCategoryConverter(),
                // Severity converters
                new SeverityToStringConverter(),
                new StringToSeverityConverter(),
                // Admin domain converters
                new UserRoleToStringConverter(),
                new StringToUserRoleConverter(),
                new AccountStatusToStringConverter(),
                new StringToAccountStatusConverter(),
                new PetStatusToStringConverter(),
                new StringToPetStatusConverter(),
                new AuditActionToStringConverter(),
                new StringToAuditActionConverter(),
                new AdminFlagStatusToStringConverter(),
                new StringToAdminFlagStatusConverter(),
                new AdminFlagReasonToStringConverter(),
                new StringToAdminFlagReasonConverter(),
                new AdminContentTypeToStringConverter(),
                new StringToAdminContentTypeConverter()
        ));
    }

    // JobStatus converters
    @WritingConverter
    static class JobStatusToStringConverter implements Converter<JobStatus, String> {
        @Override
        public String convert(JobStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToJobStatusConverter implements Converter<String, JobStatus> {
        @Override
        public JobStatus convert(String source) {
            return JobStatus.valueOf(source);
        }
    }

    // ModerationStatus converters
    @WritingConverter
    static class ModerationStatusToStringConverter implements Converter<ModerationStatus, String> {
        @Override
        public String convert(ModerationStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToModerationStatusConverter implements Converter<String, ModerationStatus> {
        @Override
        public ModerationStatus convert(String source) {
            return ModerationStatus.valueOf(source);
        }
    }

    // ContentType converters
    @WritingConverter
    static class ContentTypeToStringConverter implements Converter<ContentType, String> {
        @Override
        public String convert(ContentType source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToContentTypeConverter implements Converter<String, ContentType> {
        @Override
        public ContentType convert(String source) {
            return ContentType.valueOf(source);
        }
    }

    // ModerationCategory converters
    @WritingConverter
    static class ModerationCategoryToStringConverter implements Converter<ModerationCategory, String> {
        @Override
        public String convert(ModerationCategory source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToModerationCategoryConverter implements Converter<String, ModerationCategory> {
        @Override
        public ModerationCategory convert(String source) {
            return ModerationCategory.valueOf(source);
        }
    }

    // Severity converters
    @WritingConverter
    static class SeverityToStringConverter implements Converter<Severity, String> {
        @Override
        public String convert(Severity source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToSeverityConverter implements Converter<String, Severity> {
        @Override
        public Severity convert(String source) {
            return Severity.valueOf(source);
        }
    }

    // Admin domain converters
    @WritingConverter
    static class UserRoleToStringConverter implements Converter<UserRole, String> {
        @Override
        public String convert(UserRole source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToUserRoleConverter implements Converter<String, UserRole> {
        @Override
        public UserRole convert(String source) {
            return UserRole.valueOf(source);
        }
    }

    @WritingConverter
    static class AccountStatusToStringConverter implements Converter<AccountStatus, String> {
        @Override
        public String convert(AccountStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToAccountStatusConverter implements Converter<String, AccountStatus> {
        @Override
        public AccountStatus convert(String source) {
            return AccountStatus.valueOf(source);
        }
    }

    @WritingConverter
    static class PetStatusToStringConverter implements Converter<PetStatus, String> {
        @Override
        public String convert(PetStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToPetStatusConverter implements Converter<String, PetStatus> {
        @Override
        public PetStatus convert(String source) {
            return PetStatus.valueOf(source);
        }
    }

    @WritingConverter
    static class AuditActionToStringConverter implements Converter<AuditLog.AuditAction, String> {
        @Override
        public String convert(AuditLog.AuditAction source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToAuditActionConverter implements Converter<String, AuditLog.AuditAction> {
        @Override
        public AuditLog.AuditAction convert(String source) {
            return AuditLog.AuditAction.valueOf(source);
        }
    }

    @WritingConverter
    static class AdminFlagStatusToStringConverter implements Converter<ContentFlag.FlagStatus, String> {
        @Override
        public String convert(ContentFlag.FlagStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToAdminFlagStatusConverter implements Converter<String, ContentFlag.FlagStatus> {
        @Override
        public ContentFlag.FlagStatus convert(String source) {
            return ContentFlag.FlagStatus.valueOf(source);
        }
    }

    @WritingConverter
    static class AdminFlagReasonToStringConverter implements Converter<ContentFlag.FlagReason, String> {
        @Override
        public String convert(ContentFlag.FlagReason source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToAdminFlagReasonConverter implements Converter<String, ContentFlag.FlagReason> {
        @Override
        public ContentFlag.FlagReason convert(String source) {
            return ContentFlag.FlagReason.valueOf(source);
        }
    }

    @WritingConverter
    static class AdminContentTypeToStringConverter implements Converter<ContentFlag.ContentType, String> {
        @Override
        public String convert(ContentFlag.ContentType source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToAdminContentTypeConverter implements Converter<String, ContentFlag.ContentType> {
        @Override
        public ContentFlag.ContentType convert(String source) {
            return ContentFlag.ContentType.valueOf(source);
        }
    }
}
