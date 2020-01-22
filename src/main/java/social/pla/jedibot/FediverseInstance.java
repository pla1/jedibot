package social.pla.jedibot;

public class FediverseInstance {
    private String applicationId;
    private String clientId;
    private String clientSecret;
    private String createdDisplay;
    private long createdMilliseconds;
    private int id;
    private String instanceName;
    private String user;

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
