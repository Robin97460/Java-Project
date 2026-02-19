package com.github.lxquaver.stardisfactory;

/**
 * Représente une case de sol sur la carte (1x1 mètre).
 * Le sol peut changer de type (Terre -> Labouré -> Herbe...).
 */
public class Terrain {

    /**
     * Les différents types de sol possibles.
     */
    public enum Type {
        GRASS(true, false),      // Herbe
        TILLED(false, true);     // Terre labourée (nécessaire pour planter)

        // Propriétés du sol
        public final boolean supportsAnimals; // Peut-on poser un enclos ici ?
        public final boolean supportsCrops;   // Peut-on poser une jardinière ici ?

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
     * Action : Labourer le sol (Houe).
     * Transforme l'herbe en terre labourée.
     */
    public void till() {
        if (currentType == Type.GRASS) {
            currentType = Type.TILLED;
        }
    }

    /**
     * Action : Nettoyer le terrain (Marteau/Main).
     * Remet le terrain à l'état d'herbe (GRASS).
     */
    public void clear() {
        if (currentType == Type.TILLED) {
            currentType = Type.GRASS;
        }
    }

    /**
     * Vérifie si ce sol est adapté pour les animaux (Vaches, Poules...).
     */
    public boolean canPlaceAnimalBuilding() {
        return currentType.supportsAnimals;
    }

    /**
     * Vérifie si ce sol est adapté pour l'agriculture (Jardinières).
     */
    public boolean canPlantCrop() {
        return currentType.supportsCrops;
    }
}
