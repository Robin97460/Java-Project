package com.github.lxquaver.stardisfactory;

import java.util.ArrayList;
import java.util.List;

public class GameSave {
    public int mapSize;
    public int mapOffset;

    // terrainTypes = tableau flatten (mapSize * mapSize) d'ordinal Terrain.Type
    public int[] terrainTypes;

    public float playerX;
    public float playerY;

    public List<BuildingSave> buildings = new ArrayList<>();

    public static class BuildingSave {
        public String type; // ex: "MAIN_HQ"
        public int x;       // gridX (index dans terrainGrid)
        public int y;       // gridY
        public int rotation;
    }
}