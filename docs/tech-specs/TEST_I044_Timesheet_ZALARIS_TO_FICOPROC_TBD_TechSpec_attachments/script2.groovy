import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import java.io.Reader

def Message processData(Message message) {

    Reader reader = message.getBody(Reader)
    def xml = new XmlSlurper().parse(reader)
    def log = messageLogFactory.getMessageLog(message)

    def returnNode = xml."**".find { it.name() == 'RETURN' }

    //if RETURN is empty → do nothing
    if (!returnNode || returnNode.children().size() == 0) {
        return message
    }

    boolean isError = false


    //Loop through RETURN items
    returnNode.item.each { item ->
        if (item.TYPE.text() == 'E' || item.TYPE.text() == 'A') {
            isError = true
        }
    }

    // Set property + logging
    if (isError) {
        message.setProperty("ErrorInUpdate", "true")
        if (log) {
            log.addAttachmentAsString("UpdateTimesheetErrorLog", message.getBody(String), "text/xml")
        }
    } else {
        if (log) {
            log.addAttachmentAsString("UpdateTimesheetWarningPayload", message.getBody(String), "text/xml")
        }
    }

    return message
}