package net.jr.api.client.ui;

public interface UiDataSource {
    UiDataFile dataFile();

    default UiDataValues readData() {
        return this.dataFile().readData();
    }
}
