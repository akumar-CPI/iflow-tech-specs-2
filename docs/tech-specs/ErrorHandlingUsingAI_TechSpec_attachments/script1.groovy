import com.sap.gateway.ip.core.customdev.util.Message



def Message processData(Message message) {

    // Retrieve properties from the message

    def camelExceptionCaught = message.getProperty("CamelExceptionCaught")

    def genAIPrompt = message.getProperty("GenAIPrompt")

    def startPrompt = message.getProperty("StartPrompt")



    // Initialize updatedGenAIPrompt as null or empty, it'll be used to set message body eventually

    def updatedGenAIPrompt = null

   

    if (genAIPrompt && startPrompt && camelExceptionCaught) {

        // Find the position of startPrompt in genAIPrompt

        def startIndex = genAIPrompt.indexOf(startPrompt)



        if (startIndex != -1) {

            // Insert space and CamelExceptionCaught after StartPrompt

            updatedGenAIPrompt = genAIPrompt.substring(0, startIndex + startPrompt.length()) +

                                 " " +

                                 camelExceptionCaught +

                                 genAIPrompt.substring(startIndex + startPrompt.length())

                                

            // Set the updatedGenAIPrompt back to a message property

            message.setProperty("UpdatedGenAIPrompt", updatedGenAIPrompt)

        }

    }

   

    if (updatedGenAIPrompt != null) {

        // Set the modified prompt as the message body

        message.setBody(updatedGenAIPrompt)

    }

   

    return message

}