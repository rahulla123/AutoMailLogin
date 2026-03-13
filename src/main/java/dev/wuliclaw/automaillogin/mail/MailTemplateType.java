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

    public static MailTemplateType fromInput(String value) {
        for (MailTemplateType type : values()) {
            if (type.filePrefix.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value.replace('-', '_'))) {
                return type;
            }
        }
        return null;
    }
}
