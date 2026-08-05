import com.sap.gateway.ip.core.customdev.util.Message;

def Message processData(Message message) {
    def body = message.getBody(String)

    // Check if body is not null and contains the specific string
    if (body?.contains("Inspecting 1 iflow resulted in 0 issues found.")) {
        message.setProperty("flag", "true")
    } else {
        message.setProperty("flag", "false")
    }

    return message
}