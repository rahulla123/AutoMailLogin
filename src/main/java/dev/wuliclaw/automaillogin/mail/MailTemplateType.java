package dev.wuliclaw.automaillogin.mail;

public enum MailTemplateType {
    REGISTER("register"),
    RESET_PASSWORD("reset-password"),
    SECOND_FACTOR("second-factor"),
    TEST_SMTP("test-smtp");

    private final String filePrefix;

    MailTemplateType(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public String filePrefix() {
        return filePrefix;
    }
}
