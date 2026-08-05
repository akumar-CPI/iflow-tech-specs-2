/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-US/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-US/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import org.apache.commons.codec.binary.Base64

def Message processData(Message message) {
    //Body
    byte[] body = message.getBody(byte[]);
        // Encode the string to Base64
    String encodedBody = Base64.encodeBase64String(body);
        // Set the encoded body back to the message
    message.setBody(encodedBody);
    
    
    return message;
}