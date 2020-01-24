package social.pla.jedibot;

public class Feed {
    private int id;
    private String url;
    private boolean found = false;
    private String title;
    private String description ;
    private String uploadedMedialUrl;
    private String uploadedMedialId;
    private String mediaUrl;
    private String label;
    private long logTimeMilliseconds;
    private String logTimeDisplay;
    private String updated;

    public String toString() {
        return String.format("%d %s %s %s %s %s %s %s", id, label, url, title, mediaUrl, uploadedMedialId, uploadedMedialUrl, description);
    }

    public String getUploadedMedialUrl() {
        return uploadedMedialUrl;
    }

    public void setUploadedMedialUrl(String uploadedMedialUrl) {
        this.uploadedMedialUrl = uploadedMedialUrl;
    }

    public String getUploadedMedialId() {
        return uploadedMedialId;
    }

    public void setUploadedMedialId(String uploadedMedialId) {
        this.uploadedMedialId = uploadedMedialId;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public long getLogTimeMilliseconds() {
        return logTimeMilliseconds;
    }

    public void setLogTimeMilliseconds(long logTimeMilliseconds) {
        this.logTimeMilliseconds = logTimeMilliseconds;
    }

    public String getLogTimeDisplay() {
        return logTimeDisplay;
    }

    public void setLogTimeDisplay(String logTimeDisplay) {
        this.logTimeDisplay = logTimeDisplay;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
