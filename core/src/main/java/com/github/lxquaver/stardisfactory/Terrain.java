package com.github.lxquaver.stardisfactory;

public class Terrain {

    public enum Type {
        DIRT(false, false),      // Terre meuble (Base)
        GRASS(true, false),      // Herbe (Pour animaux)
        TILLED(false, true),     // Labouré (Pour plantes)
        ROAD(false, false);      // Route (Bloqué)

        public final boolean supportsAnimals;
        public final boolean supportsCrops;

        Type(boolean supportsAnimals, boolean supportsCrops) {
            this.supportsAnimals = supportsAnimals;
            this.supportsCrops = supportsCrops;
        }
    }

    private Type currentType;

    public Terrain(Type startType) {
        this.currentType = startType;
    }

    public Type getType() {
        return currentType;
    }

    /**
     * Tente de labourer le terrain.
     * La terre meuble devient labourée.
     */
    public void till() {
        if (currentType == Type.DIRT) {
            currentType = Type.TILLED;
        }
    }

    /**
     * Tente de planter de l'herbe.
     * La terre labourée redevient de l'herbe.
     */
    public void plantGrass() {
        if (currentType == Type.TILLED) {
            currentType = Type.GRASS;
        }
    }

    /**
     * Construit une route.
     * Possible uniquement sur la terre meuble.
     */
    public void buildRoad() {
        if (currentType == Type.DIRT) {
            currentType = Type.ROAD;
        }
    }

    /**
     * Retire une route ou un labour pour revenir à la terre meuble.
     */
    public void clear() {
        if (currentType == Type.ROAD || currentType == Type.TILLED || currentType == Type.GRASS) {
            currentType = Type.DIRT;
        }
    }

    public boolean canPlaceAnimalBuilding() {
        return currentType.supportsAnimals;
    }

    public boolean canPlantCrop() {
        return currentType.supportsCrops;
    }
}
