package social.pla.jedibot;

public class Application {
    private String applicationId;
    private String clientId;
    private String clientSecret;
    private String createdDisplay;
    private long createdMilliseconds;
    private int id;
    private String instanceName;
    private String user;
    private boolean found= false;
    private String accessToken;
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCreatedDisplay() {
        return createdDisplay;
    }

    public void setCreatedDisplay(String createdDisplay) {
        this.createdDisplay = createdDisplay;
    }

    public long getCreatedMilliseconds() {
        return createdMilliseconds;
    }

    public void setCreatedMilliseconds(long createdMilliseconds) {
        this.createdMilliseconds = createdMilliseconds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

}
