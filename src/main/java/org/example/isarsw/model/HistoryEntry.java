package org.example.isarsw.model;

public class HistoryEntry {
    private Long id;
    private Long flightId;
    private String action;
    private String actor;
    private long timestamp;
    private String payloadBefore;
    private String payloadAfter;

    public HistoryEntry() {}

    public HistoryEntry(Long flightId, String action, String actor, long timestamp, String payloadBefore, String payloadAfter) {
        this.flightId = flightId;
        this.action = action;
        this.actor = actor;
        this.timestamp = timestamp;
        this.payloadBefore = payloadBefore;
        this.payloadAfter = payloadAfter;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPayloadBefore() { return payloadBefore; }
    public void setPayloadBefore(String payloadBefore) { this.payloadBefore = payloadBefore; }

    public String getPayloadAfter() { return payloadAfter; }
    public void setPayloadAfter(String payloadAfter) { this.payloadAfter = payloadAfter; }
}