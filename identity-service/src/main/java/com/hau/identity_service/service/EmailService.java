package com.hau.identity_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        log.info("Đang gửi email tới {}", to);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
            log.info("Async Email '{}' gửi thành công tới {}", subject, to);
        } catch (MessagingException e) {
            log.error("Async Email gửi không thành công từ '{}' đến {}: {}", subject, to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi khi gửi email từ '{}' đến {}: {}", subject, to, e.getMessage(), e);
        }
    }
}
