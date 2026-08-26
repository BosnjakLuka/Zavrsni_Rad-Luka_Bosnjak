package hr.lukabosnjak.ui.controller;

import hr.lukabosnjak.domain.entities.MachiningJob;

public interface ApplicationNavigation {
    void showLogin();

    void showRegistration();

    void showMain();

    default void showMainWithJob(MachiningJob job) {
        showMain();
    }

    void showSavedPrograms();

    void showCatalog();

    void showUserManagement();
}
