import com.sap.gateway.ip.core.customdev.util.Message



def Message processData(Message message) {

    // Retrieve properties

    def camelExceptionCaught = message.getProperty("CamelExceptionCaught")

    def extractedCustomErrorMessage = message.getProperty("ExtractedCustomErrorMessage")

    def extractedPossibleReason = message.getProperty("ExtractedPossibleReason")

    def extractedPossibleSolution = message.getProperty("ExtractedPossibleSolution")

    def extractedPHTTPCode = message.getProperty("ExtractedPHTTPCode")



    // Set the CamelHttpResponseCode header

    if (extractedPHTTPCode) {

        message.setHeader("CamelHttpResponseCode", extractedPHTTPCode)

    }



    // Create custom headers

    def messageLog = messageLogFactory.getMessageLog(message)

    if (messageLog != null) {

        messageLog.addCustomHeaderProperty("CustomError", extractedCustomErrorMessage ?: "No custom error message available.")

        messageLog.addCustomHeaderProperty("PossibleReason", extractedPossibleReason ?: "No possible reason available.")

        messageLog.addCustomHeaderProperty("PossibleSolution", extractedPossibleSolution ?: "No possible solution available.")

    }



    // Attach extracted values as message attachments

    def possibleReasonAttachment = extractedPossibleReason ?: "No possible reason available."

    def possibleSolutionAttachment = extractedPossibleSolution ?: "No possible solution available."

   

    if (messageLog != null) {

        messageLog.addAttachmentAsString("PossibleReason", possibleReasonAttachment, "text/plain")

        messageLog.addAttachmentAsString("PossibleSolution", possibleSolutionAttachment, "text/plain")

    }



    // Create dynamic response in XML format

    def responseXml = """<?xml version="1.0" encoding="UTF-8"?>

<Error_Response>

    <SystemError>${camelExceptionCaught}</SystemError>

    <CustomError>${extractedCustomErrorMessage ?: "No custom error message available."}</CustomError>

    <PossibleReason>${extractedPossibleReason ?: "No possible reason available."}</PossibleReason>

    <PossibleSolution>${extractedPossibleSolution ?: "No possible solution available."}</PossibleSolution>

</Error_Response>"""



    // Set XML response as the message body

    message.setBody(responseXml)





if (camelExceptionCaught) {

        throw new Exception("\nSystem Error: ${camelExceptionCaught}\nCustom Error from GenAI: ${extractedCustomErrorMessage ?: 'No custom error message available.'}")

    }



    return message

}