package com.github.lxquaver.stardisfactory.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.lxquaver.stardisfactory.GameMain;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // Requis pour macOS
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new GameMain(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Apprentissage LibGDX - Grille 2D");

        // Utilise le taux de rafraîchissement du moniteur
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);

        // Taille initiale de la fenêtre (en pixels)
        configuration.setWindowedMode(1280, 720);

        // Icones (optionnel, commentez si les fichiers n'existent pas)
        // configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}
