package com.github.lxquaver.stardisfactory;

import java.util.ArrayList;
import java.util.List;

public class GameSave {
    public int mapSize;
    public int mapOffset;

    // Legacy: tableau flatten (mapSize * mapSize) d'ordinal Terrain.Type
    public int[] terrainTypes;

    // Nouveau format: tableau flatten (mapSize * mapSize) de noms Terrain.Type
    public String[] terrainTypeNames;

    public float playerX;
    public float playerY;

    public int money;
    public int buildingStockPlanter;
    public int buildingStockConveyor;
    public int seedBagsPotato;
    public int seedBagsStrawberry;
    public int seedBagsLeek;
    public int carriedPotato;
    public int carriedStrawberry;
    public int carriedLeek;

    public List<BuildingSave> buildings = new ArrayList<>();

    public static class BuildingSave {
        public String type; // ex: "MAIN_HQ"
        public int x;       // gridX (index dans terrainGrid)
        public int y;       // gridY
        public int rotation;

        public String planterCrop;
        public float growTimerSeconds;
        public boolean planterReady;

        public String heldItem;
        public int heldAmount;
        public float transportTimer;

        public int hqPotatoes;
        public int hqStrawberries;
        public int hqLeeks;
    }
}
