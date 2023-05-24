module icu.megakite.vinyljfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens icu.megakite.vinyljfx to javafx.fxml;
    exports icu.megakite.vinyljfx;
}