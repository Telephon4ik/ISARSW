module org.example.isarsw {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports org.example.isarsw.service;
    exports org.example.isarsw.app;
    exports org.example.isarsw.controller;
    exports org.example.isarsw.dao;
    exports org.example.isarsw.db;
    exports org.example.isarsw.model;
    exports org.example.isarsw.util;

    // Открываем для JavaFX
    opens org.example.isarsw.app to javafx.fxml, javafx.graphics;
    opens org.example.isarsw.controller to javafx.fxml;

    // Открываем для тестов (рефлексия)
    opens org.example.isarsw.service;
    opens org.example.isarsw.dao;
    opens org.example.isarsw.model;
    opens org.example.isarsw.util;
    opens org.example.isarsw.db;
}