package social.pla.jedibot;

public class Feed {
    private String channelDescription;
    private String channelTitle;
    private String channelUrl;
    private String description;
    private String feedUrl;
    private boolean found = false;
    private int id;
    private String label;
    private String logTimeDisplay;
    private long logTimeMilliseconds;
    private String mediaUrl;
    private String title;
    private String updated;
    private String uploadedMedialId;
    private String uploadedMedialUrl;
    private String url;

    public String getChannelDescription() {
        return channelDescription;
    }

    public void setChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getUploadedMedialId() {
        return uploadedMedialId;
    }

    public void setUploadedMedialId(String uploadedMedialId) {
        this.uploadedMedialId = uploadedMedialId;
    }

    public String getUploadedMedialUrl() {
        return uploadedMedialUrl;
    }

    public void setUploadedMedialUrl(String uploadedMedialUrl) {
        this.uploadedMedialUrl = uploadedMedialUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public boolean isYouTube() {
        return (url != null && url.contains("youtube.com"));
    }

    public String toString() {
        return String.format("%d %s %s %s %s %s %s %s", id, label, url, title, mediaUrl, uploadedMedialId, uploadedMedialUrl, description);
    }
}
