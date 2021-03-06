package social.pla.jedibot;

public class Subscription {

    private int feedId;
    private boolean found = false;
    private int id;
    private String logTimeDisplay;
    private long logTimeMilliseconds;
    private String user;

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogTimeDisplay() {
        return logTimeDisplay;
    }

    public void setLogTimeDisplay(String logTimeDisplay) {
        this.logTimeDisplay = logTimeDisplay;
    }

    public long getLogTimeMilliseconds() {
        return logTimeMilliseconds;
    }

    public void setLogTimeMilliseconds(long logTimeMilliseconds) {
        this.logTimeMilliseconds = logTimeMilliseconds;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }
}
