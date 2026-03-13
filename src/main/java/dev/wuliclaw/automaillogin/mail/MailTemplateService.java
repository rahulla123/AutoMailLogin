package dev.wuliclaw.automaillogin.mail;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MailTemplateService {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
    private static final Set<String> KNOWN_VARIABLES = Set.of("code", "player", "email", "server_name", "expire_seconds", "support_email");
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

    public File getTemplateDirectory() {
        return templateDirectory;
    }

    public boolean hasTemplate(MailTemplateType type, String suffix) {
        return new File(templateDirectory, type.filePrefix() + suffix).exists();
    }

    public Set<String> findUnknownVariables(MailTemplateType type) {
        Set<String> unknown = new HashSet<>();
        scanUnknownVariables(type.filePrefix() + ".subject.txt", unknown);
        scanUnknownVariables(type.filePrefix() + ".text.txt", unknown);
        scanUnknownVariables(type.filePrefix() + ".html", unknown);
        return unknown;
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

    private void scanUnknownVariables(String fileName, Set<String> unknown) {
        String content = readTemplate(fileName);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!KNOWN_VARIABLES.contains(key)) {
                unknown.add(key);
            }
        }
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
