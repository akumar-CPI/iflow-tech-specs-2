import com.sap.gateway.ip.core.customdev.util.Message

import groovy.json.JsonSlurper



def Message processData(Message message) {

    // Retrieve properties

    def captureCustomErrorKey = message.getProperty("CaptureCustomError")

    def capturePossibleReasonKey = message.getProperty("CapturePossibleReason")

    def capturePossibleSolutionKey = message.getProperty("CapturePossibleSolution")

    def captureHTTPCodeKey = message.getProperty("CaptureHTTPCode")  // This should be "HTTP Response:"



    // Get the message body and parse it as JSON

    def payload = message.getBody(String)

    def jsonSlurper = new JsonSlurper()



    // Initialize extracted values

    def extractedCustomErrorMessage = null

    def extractedPossibleReason = null

    def extractedPossibleSolution = null

    def extractedPHTTPCode = null



    try {

        // Try parsing the JSON

        def parsedJson = jsonSlurper.parseText(payload)

       

        // Extract the text section from the JSON

        def textContent = parsedJson.candidates[0]?.content?.parts[0]?.text



        if (textContent) {

            // Log the text content to verify structure

            messageLogFactory.getMessageLog(message)?.addAttachmentAsString("Text Content", textContent, "text/plain")



            // Extract values based on keys

            extractedCustomErrorMessage = extractValueFollowingKey(textContent, captureCustomErrorKey)

            extractedPossibleReason = extractValueFollowingKey(textContent, capturePossibleReasonKey)

            extractedPossibleSolution = extractValueFollowingKey(textContent, capturePossibleSolutionKey)



            // Specifically extract the HTTP response code after "HTTP Response:"

            extractedPHTTPCode = extractHTTPResponseCode(textContent, captureHTTPCodeKey)

        }

    } catch (Exception e) {

        // Log exception and proceed with null outputs

        messageLogFactory.getMessageLog(message)?.addAttachmentAsString("Exception", e.toString(), "text/plain")

    }



    // Handle extraction results

    message.setProperty("ExtractedCustomErrorMessage", extractedCustomErrorMessage)

    message.setProperty("ExtractedPossibleReason", extractedPossibleReason)

    message.setProperty("ExtractedPossibleSolution", extractedPossibleSolution)

    message.setProperty("ExtractedPHTTPCode", extractedPHTTPCode)

   

    // Set HTTP Code as the message body for demonstration; it will be null if extraction fails

    message.setBody(extractedPHTTPCode)



    return message

}



// Function to extract value that follows a given key in the text content

def extractValueFollowingKey(String textContent, String key) {

    if (key) {

        def keyIndex = textContent.indexOf(key)

        if (keyIndex != -1) {

            def startIndex = keyIndex + key.length()

            if (startIndex != -1) {

                def endIndex = textContent.indexOf("\n\n", startIndex)

                endIndex = endIndex != -1 ? endIndex : textContent.length()



                def extractedText = textContent.substring(startIndex, endIndex).trim()

                extractedText = extractedText.replaceAll('\\*', '')  // Clean up undesired characters

                return extractedText

            }

        }

    }

    return null

}



// Specific extraction utility for HTTP Code.

def extractHTTPResponseCode(String textContent, String key) {

    if (key) {

        def keyIndex = textContent.indexOf(key)

        if (keyIndex != -1) {

            def startIndex = keyIndex + key.length()

            if (startIndex != -1) {

                // Find the end, which is the next newline after the code and description

                def endIndex = textContent.indexOf("\n", startIndex)

                endIndex = endIndex != -1 ? endIndex : textContent.length()



                return textContent.substring(startIndex, endIndex).trim()

            }

        }

    }

    return null

}