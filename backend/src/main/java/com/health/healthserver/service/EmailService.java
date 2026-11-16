package com.health.healthserver.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件发送服务。
 * 发件凭证通过环境变量注入（MAIL_USERNAME / MAIL_PASSWORD），
 * 不写入任何配置文件，避免敏感信息落盘。
 */
@Service
public class EmailService {

    @Value("${MAIL_USERNAME:}")
    private String mailUsername;

    @Value("${MAIL_PASSWORD:}")
    private String mailPassword;

    private JavaMailSender mailSender;

    /** 懒加载构建 JavaMailSender（host/port 固定为 QQ 邮箱 SMTP） */
    private JavaMailSender getMailSender() {
        if (mailSender == null) {
            JavaMailSenderImpl impl = new JavaMailSenderImpl();
            impl.setHost("smtp.qq.com");
            impl.setPort(587);
            impl.setUsername(mailUsername);
            impl.setPassword(mailPassword);
            impl.setDefaultEncoding("UTF-8");
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            impl.setJavaMailProperties(props);
            mailSender = impl;
        }
        return mailSender;
    }

    /**
     * 发送提醒邮件。未配置发件凭证或收件人为空时安全跳过（不抛异常）。
     */
    /** 是否已配置发件凭证（环境变量 MAIL_USERNAME / MAIL_PASSWORD） */
    public boolean isConfigured() {
        return !mailUsername.isBlank() && !mailPassword.isBlank();
    }

    public void sendReminder(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank() || mailUsername.isBlank() || mailPassword.isBlank()) {
            // 发件凭证或收件人缺失，静默跳过
            return;
        }
        try {
            MimeMessage message = getMailSender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            getMailSender().send(message);
        } catch (Exception e) {
            // 邮件发送失败不影响主流程，仅打印日志
            System.err.println("发送提醒邮件失败 -> " + to + " : " + e.getMessage());
        }
    }
}
