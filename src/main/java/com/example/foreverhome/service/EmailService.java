package com.example.foreverhome.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendPasswordResetByAdmin(String to, String temporaryPassword);
    void sendNotificationEmail(String to, String subject, String body);
    void sendWelcomeEmail(String to, String name);
}
