/* Refer the link below to learn more about the use cases of script.
https://help.sap.com/viewer/368c481cd6954bdfa5d0435479fd4eaf/Cloud/en-US/148851bf8192412cba1f9d2c17f4bd25.html

If you want to know more about the SCRIPT APIs, refer the link below
https://help.sap.com/doc/a56f52e1a58e4e2bac7f7adbf45b2e26/Cloud/en-US/index.html */
import com.sap.gateway.ip.core.customdev.util.Message;
import java.net.HttpURLConnection
import java.net.URL


def Message processData(Message message) {
    //Body
  
    def artifactId = message.getProperties().get("aid");
    def originalUrl = "https://api.github.com/repos/akumar-CPI/Package_1/actions/artifacts/" + artifactId + "/zip"
    def httpMethod = "GET"
    
    def finalUrl = resolveRedirect(originalUrl,httpMethod)
    def urlAndQuery = finalUrl.split("\\?");
    def url = urlAndQuery[0]
    def query = urlAndQuery[1]
    
    message.setProperty("requestURL",url);
    message.setProperty("queryParams",query);
    
    return message;

}

def String resolveRedirect(String originalURL, String httpMethod){
    HttpURLConnection conn = null;
    try{
        URL url = new URL(originalURL)
        conn = (HttpURLConnection) url.openConnection()
        conn.setInstanceFollowRedirects(false)
        conn.setRequestMethod(httpMethod)
        conn.setRequestProperty("Authorization", "Bearer " + "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")
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
        conn.connect()

        int code = conn.getResponseCode()

        if(code in [301, 302, 307, 308]){
            def location = conn.getHeaderField("Location");

            if(location){
                
                return location;
            }else{
                throw new RuntimeException("Redirect Response but no Location header");
            }
        }
        else
        {
            return code
        }
    }
    catch(Exception e){
        throw new RuntimeException("Failed to resolve Redirect: " + e.message, e);
    }
    finally{
        if(conn != null)
            conn.disconnect();
    }

}