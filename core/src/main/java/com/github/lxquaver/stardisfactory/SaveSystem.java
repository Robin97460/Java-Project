package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class SaveSystem {

    private static final String SAVE_FILE = "savegame.json";

    public static void save(GameSave data) {
        Json json = new Json();
        json.setUsePrototypes(false);

        FileHandle fh = Gdx.files.local(SAVE_FILE);
        fh.writeString(json.prettyPrint(data), false);
    }

    public static GameSave load() {
        FileHandle fh = Gdx.files.local(SAVE_FILE);
        if (!fh.exists()) return null;

        Json json = new Json();
        json.setUsePrototypes(false);

        return json.fromJson(GameSave.class, fh);
    }
}