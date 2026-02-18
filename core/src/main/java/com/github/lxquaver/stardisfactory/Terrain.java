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
        DIRT(false, false),      // Terre simple (le défaut)
        GRASS(true, false),      // Herbe (nécessaire pour les animaux)
        TILLED(false, true),     // Terre labourée (nécessaire pour planter)
        ROAD(false, false);      // Route (pour faire joli et marcher vite plus tard)

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
     * Transforme l'herbe en terre, et la terre en terre labourée.
     */
    public void till() {
        if (currentType == Type.GRASS) {
            currentType = Type.DIRT;
        } else if (currentType == Type.DIRT) {
            currentType = Type.TILLED;
        }
    }

    /**
     * Action : Planter de l'herbe (Graines d'herbe).
     * Transforme la terre labourée en herbe.
     */
    public void plantGrass() {
        if (currentType == Type.TILLED) {
            currentType = Type.GRASS;
        }
    }

    /**
     * Action : Construire une route (Pelle).
     * Transforme la terre en route.
     */
    public void buildRoad() {
        if (currentType == Type.DIRT) {
            currentType = Type.ROAD;
        }
    }

    /**
     * Action : Nettoyer le terrain (Marteau/Main).
     * Remet le terrain à l'état de terre simple (DIRT).
     */
    public void clear() {
        // On peut nettoyer n'importe quoi sauf la terre de base (qui est déjà propre)
        if (currentType == Type.ROAD || currentType == Type.TILLED || currentType == Type.GRASS) {
            currentType = Type.DIRT;
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
