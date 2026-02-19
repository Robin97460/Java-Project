package com.github.lxquaver.stardisfactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class SaveSystem {

    private static final String SAVE_FILE = ".stardisfactory/savegame.json";

    private static FileHandle getSaveFileHandle() {
        return Gdx.files.external(SAVE_FILE);
    }

    public static void save(GameSave data) {
        Json json = new Json();
        json.setUsePrototypes(false);

        FileHandle fh = getSaveFileHandle();
        fh.parent().mkdirs();
        fh.writeString(json.prettyPrint(data), false);
    }

    public static GameSave load() {
        FileHandle fh = getSaveFileHandle();
        if (!fh.exists()) return null;

        Json json = new Json();
        json.setUsePrototypes(false);

        return json.fromJson(GameSave.class, fh);
    }

    public static boolean exists() {
        return getSaveFileHandle().exists();
    }

    public static void delete() {
        FileHandle fh = getSaveFileHandle();
        if (fh.exists()) {
            fh.delete();
        }
    }
}
