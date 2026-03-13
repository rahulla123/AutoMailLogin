package dev.wuliclaw.automaillogin.mail;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public final class MailTemplateService {
    private final AutoMailLoginPlugin plugin;
    private File templateDirectory;

    public MailTemplateService(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
        refreshTemplateDirectory();
    }

    public void reload() {
        refreshTemplateDirectory();
        initializeDefaults();
    }

    public void initializeDefaults() {
        if (!templateDirectory.exists() && !templateDirectory.mkdirs()) {
            throw new IllegalStateException("Failed to create template directory: " + templateDirectory.getAbsolutePath());
        }
        copyIfMissing("templates/register.subject.txt");
        copyIfMissing("templates/register.text.txt");
        copyIfMissing("templates/register.html");
        copyIfMissing("templates/reset-password.subject.txt");
        copyIfMissing("templates/reset-password.text.txt");
        copyIfMissing("templates/reset-password.html");
        copyIfMissing("templates/second-factor.subject.txt");
        copyIfMissing("templates/second-factor.text.txt");
        copyIfMissing("templates/second-factor.html");
        copyIfMissing("templates/test-smtp.subject.txt");
        copyIfMissing("templates/test-smtp.text.txt");
        copyIfMissing("templates/test-smtp.html");
    }

    public RenderedMailTemplate render(MailTemplateType type, Map<String, String> variables) {
        String prefix = type.filePrefix();
        String subject = replaceVariables(readTemplate(prefix + ".subject.txt"), variables);
        String textBody = replaceVariables(readTemplate(prefix + ".text.txt"), variables);
        String htmlBody = replaceVariables(readTemplate(prefix + ".html"), variables);
        return new RenderedMailTemplate(subject, textBody, htmlBody == null || htmlBody.isBlank() ? null : htmlBody);
    }

    private void refreshTemplateDirectory() {
        this.templateDirectory = new File(plugin.getDataFolder(), plugin.getConfig().getString("mail.template-dir", "templates"));
    }

    private String readTemplate(String fileName) {
        File file = new File(templateDirectory, fileName);
        if (!file.exists()) {
            plugin.getLogger().warning("Mail template missing: " + file.getAbsolutePath());
            return "";
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to read mail template: " + file.getAbsolutePath() + " - " + exception.getMessage());
            return "";
        }
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private void copyIfMissing(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        plugin.saveResource(resourcePath.replace('\\', '/'), false);
    }
}
