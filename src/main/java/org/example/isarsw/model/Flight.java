package org.example.isarsw.model;

import javafx.beans.property.*;

import java.time.Instant;
import java.util.Objects;

public class Flight {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty number = new SimpleStringProperty();
    private final StringProperty route = new SimpleStringProperty();
    private final LongProperty arriveTs = new SimpleLongProperty();
    private final IntegerProperty standingTime = new SimpleIntegerProperty();
    private final StringProperty platform = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final LongProperty createdAt = new SimpleLongProperty();
    private final LongProperty updatedAt = new SimpleLongProperty();
    private final LongProperty lastArrivalCheck = new SimpleLongProperty(0);

    public Flight() {}

    public Flight(Long id, String number, String route,
                  long arriveTs, int standingTime,
                  String platform, String status,
                  long createdAt, long updatedAt, long lastArrivalCheck) {
        this.id.set(id == null ? 0 : id);
        this.number.set(number);
        this.route.set(route);
        this.arriveTs.set(arriveTs);
        this.standingTime.set(standingTime);
        this.platform.set(platform);
        this.status.set(status);
        this.createdAt.set(createdAt);
        this.updatedAt.set(updatedAt);
        this.lastArrivalCheck.set(lastArrivalCheck);
    }

    public Flight(String number, String route, long arriveTs, int standingTime,
                  String platform, String status) {
        this(null, number, route, arriveTs, standingTime, platform, status,
                Instant.now().getEpochSecond(),
                Instant.now().getEpochSecond(),
                0);
    }

    // Геттеры и сеттеры
    public Long getId() { return id.get(); }
    public void setId(Long v) { this.id.set(v == null ? 0 : v); }
    public LongProperty idProperty() { return id; }

    public String getNumber() { return number.get(); }
    public void setNumber(String v) { this.number.set(v); }
    public StringProperty numberProperty() { return number; }

    public String getRoute() { return route.get(); }
    public void setRoute(String v) { this.route.set(v); }
    public StringProperty routeProperty() { return route; }

    public long getArriveTs() { return arriveTs.get(); }
    public void setArriveTs(long v) { this.arriveTs.set(v); }
    public LongProperty arriveTsProperty() { return arriveTs; }

    public int getStandingTime() { return standingTime.get(); }
    public void setStandingTime(int v) { this.standingTime.set(v); }
    public IntegerProperty standingTimeProperty() { return standingTime; }

    public long getDepartureTs() {
        return arriveTs.get() + (standingTime.get() * 60L);
    }

    public String getPlatform() { return platform.get(); }
    public void setPlatform(String v) { this.platform.set(v); }
    public StringProperty platformProperty() { return platform; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { this.status.set(v); }
    public StringProperty statusProperty() { return status; }

    public long getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(long v) { this.createdAt.set(v); }
    public LongProperty createdAtProperty() { return createdAt; }

    public long getUpdatedAt() { return updatedAt.get(); }
    public void setUpdatedAt(long v) { this.updatedAt.set(v); }
    public LongProperty updatedAtProperty() { return updatedAt; }

    public long getLastArrivalCheck() { return lastArrivalCheck.get(); }
    public void setLastArrivalCheck(long v) { this.lastArrivalCheck.set(v); }
    public LongProperty lastArrivalCheckProperty() { return lastArrivalCheck; }

    // Метод для создания JSON строки для истории
    public String toPayload() {
        return "{"
                + "\"id\":" + getId() + ","
                + "\"number\":\"" + escape(getNumber()) + "\","
                + "\"route\":\"" + escape(getRoute()) + "\","
                + "\"arriveTs\":" + getArriveTs() + ","
                + "\"standingTime\":" + getStandingTime() + ","
                + "\"departureTs\":" + getDepartureTs() + ","
                + "\"platform\":\"" + escape(getPlatform()) + "\","
                + "\"status\":\"" + escape(getStatus()) + "\","
                + "\"createdAt\":" + getCreatedAt() + ","
                + "\"updatedAt\":" + getUpdatedAt() + ","
                + "\"lastArrivalCheck\":" + getLastArrivalCheck()
                + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Вспомогательные методы
    public boolean isArrived() {
        return getArriveTs() <= Instant.now().getEpochSecond();
    }

    public boolean isDeparted() {
        return getDepartureTs() <= Instant.now().getEpochSecond();
    }

    public boolean needsArrivalCheck() {
        long now = Instant.now().getEpochSecond();
        return getArriveTs() <= now && getLastArrivalCheck() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flight)) return false;
        Flight flight = (Flight) o;
        return getId() == flight.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Flight{" +
                "id=" + getId() +
                ", number='" + getNumber() + '\'' +
                ", route='" + getRoute() + '\'' +
                ", arriveTs=" + getArriveTs() +
                ", standingTime=" + getStandingTime() +
                ", platform='" + getPlatform() + '\'' +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}