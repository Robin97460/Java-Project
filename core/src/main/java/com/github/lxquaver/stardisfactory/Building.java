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
        CONVEYOR_BELT(1, 1, BuildingCategory.TRANSPORT),

        // Farming
        PLANTER(1, 1, BuildingCategory.FARMING);

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
        TRANSPORT,
        FARMING
    }

    public enum PlanterCrop {
        NONE,
        TOMATO,
        WHEAT
    }

    private final Type type;

    // Position (coordonnée inférieure gauche dans la grille)
    private final int gridX;
    private final int gridY;

    // Orientation (0: Nord, 1: Est, 2: Sud, 3: Ouest)
    private int rotation;

    // ====== PLANTER STATE (utilisé uniquement si type == PLANTER) ======
    private PlanterCrop planterCrop = PlanterCrop.NONE;
    private float growTimerSeconds = 0f;
    private boolean ready = false;

    public Building(Type type, int gridX, int gridY) {
        this(type, gridX, gridY, 0);
    }

    public Building(Type type, int gridX, int gridY, int rotation) {
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        this.rotation = rotation % 4;
        if (this.rotation < 0) this.rotation += 4;
    }

    public Type getType() { return type; }
    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }

    public void rotate() { rotation = (rotation + 1) % 4; }
    public int getRotation() { return rotation; }
    public void setRotation(int rotation) {
        this.rotation = rotation % 4;
        if (this.rotation < 0) this.rotation += 4;
    }

    // =========================
    // PLANTER LOGIC
    // =========================

    public boolean isPlanter() {
        return type == Type.PLANTER;
    }

    public PlanterCrop getPlanterCrop() {
        return planterCrop;
    }

    public boolean isPlanterEmpty() {
        return planterCrop == PlanterCrop.NONE;
    }

    public boolean isPlanterReady() {
        return ready;
    }

    public float getPlanterGrowProgress01() {
        float total = getGrowTimeSecondsFor(planterCrop);
        if (total <= 0f) return 0f;
        return Math.min(1f, growTimerSeconds / total);
    }

    public void plant(PlanterCrop crop) {
        if (!isPlanter()) return;
        if (crop == null || crop == PlanterCrop.NONE) return;

        planterCrop = crop;
        growTimerSeconds = 0f;
        ready = false;
    }

    public void update(float dt) {
        if (!isPlanter()) return;
        if (planterCrop == PlanterCrop.NONE) return;
        if (ready) return;

        growTimerSeconds += dt;

        float total = getGrowTimeSecondsFor(planterCrop);
        if (growTimerSeconds >= total) {
            growTimerSeconds = total;
            ready = true;
        }
    }

    /**
     * Récolte (pour l'instant: pas de stockage, on reset juste)
     * Retourne le nombre produit (prévu pour stockage plus tard).
     */
    public int harvest() {
        if (!isPlanter()) return 0;
        if (planterCrop == PlanterCrop.NONE) return 0;
        if (!ready) return 0;

        int amount = getYieldFor(planterCrop);

        // reset
        planterCrop = PlanterCrop.NONE;
        growTimerSeconds = 0f;
        ready = false;

        return amount;
    }

    public static float getGrowTimeSecondsFor(PlanterCrop crop) {
        if (crop == null) return 0f;
        switch (crop) {
            case TOMATO: return 120f; // 2 min
            case WHEAT:  return 60f;  // 1 min
            default:     return 0f;
        }
    }

    public static int getYieldFor(PlanterCrop crop) {
        if (crop == null) return 0;
        switch (crop) {
            case TOMATO: return 10;
            case WHEAT:  return 5;
            default:     return 0;
        }
    }
}