package com.example.foreverhome.moderation.config;

import com.example.foreverhome.moderation.domain.*;
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
                new StringToSeverityConverter()
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
}
