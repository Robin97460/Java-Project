package com.github.lxquaver.stardisfactory;

public class Building {

    public enum Type {
        // Agriculture Animale (Nécessite Terrain.Type.GRASS)
        COW_COOP(2, 2, BuildingCategory.ANIMAL),
        CHICKEN_COOP(1, 1, BuildingCategory.ANIMAL),

        // Gestion / Vente
        MAIN_HQ(3, 3, BuildingCategory.LOGISTICS),
        SELL_POINT(1, 1, BuildingCategory.LOGISTICS),

        // Transport
        CONVEYOR_BELT(1, 1, BuildingCategory.TRANSPORT);

        public final int width;
        public final int height;
        public final BuildingCategory category;

        Type(int width, int height, BuildingCategory category) {
            this.width = width;
            this.height = height;
            this.category = category;
        }
    }

    public enum BuildingCategory {
        ANIMAL,
        LOGISTICS,
        TRANSPORT
    }

    private Type type;
    // Position (coordonnée inférieure gauche dans la grille)
    private int gridX;
    private int gridY;
    // Orientation (0: Nord, 1: Est, 2: Sud, 3: Ouest) - Utile pour les tapis roulants
    private int rotation;

    public Building(Type type, int gridX, int gridY) {
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        this.rotation = 0;
    }

    public Type getType() {
        return type;
    }

    public void rotate() {
        rotation = (rotation + 1) % 4;
    }

    public int getRotation() {
        return rotation;
    }
}
