package com.github.lxquaver.stardisfactory;

/**
 * Représente un bâtiment posé sur la carte.
 * Un bâtiment peut être une simple structure (HQ), une zone de production (Jardinière)
 * ou un élément logistique (Convoyeur).
 */
public class Building {

    /**
     * Tous les types de bâtiments disponibles dans le jeu.
     * Chaque type a une taille (largeur x hauteur) et une catégorie.
     */
    public enum Type {
        // --- Animaux (Doivent être sur de l'herbe) ---
        COW_COOP(2, 2, BuildingCategory.ANIMAL),
        CHICKEN_COOP(1, 1, BuildingCategory.ANIMAL),

        // --- Gestion & Vente ---
        MAIN_HQ(4, 4, BuildingCategory.LOGISTICS), // Le QG principal
        SELL_POINT(1, 1, BuildingCategory.LOGISTICS), // Pour vendre les récoltes

        // --- Transport ---
        CONVEYOR_BELT(1, 1, BuildingCategory.TRANSPORT), // Tapis roulant

        // --- Agriculture ---
        PLANTER(1, 1, BuildingCategory.FARMING); // Jardinière pour faire pousser des plantes

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

    /**
     * Les types de plantes qu'on peut faire pousser.
     */
    public enum PlanterCrop {
        NONE,   // Rien
        TOMATO, // Tomates (Long à pousser, rapporte plus)
        WHEAT   // Blé (Rapide, rapporte moins)
    }

    private final Type type;

    // Position sur la grille (coin inférieur gauche)
    private final int gridX;
    private final int gridY;

    // Orientation (0: Nord, 1: Est, 2: Sud, 3: Ouest)
    private int rotation;

    // ====== ÉTAT JARDINIÈRE (PLANTER) ======
    // Ces variables ne servent que si le bâtiment est une jardinière
    private PlanterCrop planterCrop = PlanterCrop.NONE;
    private float growTimerSeconds = 0f; // Temps écoulé depuis la plantation
    private boolean ready = false; // La plante est-elle mûre ?

    // ====== ÉTAT CONVOYEUR (CONVEYOR_BELT) ======
    // Ces variables ne servent que si le bâtiment est un convoyeur
    private PlanterCrop heldItem = PlanterCrop.NONE; // L'objet transporté actuellement
    private float transportTimer = 0f; // Progression du transport (0 à 1 sec)
    private static final float TRANSPORT_TIME = 1.0f; // Temps pour traverser une case (en secondes)

    // ====== ÉTAT HQ (MAIN_HQ) ======
    // Stockage des ressources dans le QG
    private int hqTomatoes = 0;
    private int hqWheat = 0;

    /**
     * Crée un nouveau bâtiment.
     * @param type Le type de bâtiment (ex: PLANTER)
     * @param gridX Position X sur la grille
     * @param gridY Position Y sur la grille
     */
    public Building(Type type, int gridX, int gridY) {
        this(type, gridX, gridY, 0);
    }

    /**
     * Crée un nouveau bâtiment avec une rotation spécifique.
     */
    public Building(Type type, int gridX, int gridY, int rotation) {
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        // On s'assure que la rotation est toujours entre 0 et 3
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
    // LOGIQUE JARDINIÈRE (PLANTER)
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

    /**
     * Retourne la progression de la pousse entre 0 (début) et 1 (fin).
     * Utile pour afficher une barre de progression ou changer l'image.
     */
    public float getPlanterGrowProgress01() {
        float total = getGrowTimeSecondsFor(planterCrop);
        if (total <= 0f) return 0f;
        return Math.min(1f, growTimerSeconds / total);
    }

    /**
     * Plante une graine dans la jardinière.
     */
    public void plant(PlanterCrop crop) {
        if (!isPlanter()) return;
        if (crop == null || crop == PlanterCrop.NONE) return;

        planterCrop = crop;
        growTimerSeconds = 0f;
        ready = false;
    }

    /**
     * Met à jour l'état du bâtiment (appelé à chaque image du jeu).
     * @param dt Temps écoulé depuis la dernière image (en secondes)
     */
    public void update(float dt) {
        if (isPlanter()) {
            updatePlanter(dt);
        } else if (isConveyor()) {
            updateConveyor(dt);
        }
    }

    private void updatePlanter(float dt) {
        if (planterCrop == PlanterCrop.NONE) return; // Rien ne pousse
        if (ready) return; // Déjà mûr, on attend la récolte

        growTimerSeconds += dt;

        float total = getGrowTimeSecondsFor(planterCrop);
        if (growTimerSeconds >= total) {
            growTimerSeconds = total;
            ready = true; // C'est prêt !
        }
    }

    /**
     * Récolte la plante et vide la jardinière.
     * @return Le type de plante récoltée (ex: TOMATO), ou NONE si vide/pas prêt.
     */
    public PlanterCrop harvest() {
        if (!isPlanter()) return PlanterCrop.NONE;
        if (planterCrop == PlanterCrop.NONE) return PlanterCrop.NONE;
        if (!ready) return PlanterCrop.NONE;

        PlanterCrop harvested = planterCrop;

        // On vide la jardinière pour la prochaine plantation
        planterCrop = PlanterCrop.NONE;
        growTimerSeconds = 0f;
        ready = false;

        return harvested;
    }

    /**
     * Temps de pousse en secondes pour chaque plante.
     */
    public static float getGrowTimeSecondsFor(PlanterCrop crop) {
        if (crop == null) return 0f;
        switch (crop) {
            case TOMATO: return 120f; // 2 minutes
            case WHEAT:  return 60f;  // 1 minute
            default:     return 0f;
        }
    }

    /**
     * Quantité produite par chaque plante (pour les stats).
     */
    public static int getYieldFor(PlanterCrop crop) {
        if (crop == null) return 0;
        switch (crop) {
            case TOMATO: return 10;
            case WHEAT:  return 5;
            default:     return 0;
        }
    }

    public static int getSellPriceFor(PlanterCrop crop) {
        if (crop == null) return 0;
        switch (crop) {
            case TOMATO: return 10;
            case WHEAT:  return 5;
            default:     return 0;
        }
    }

    // =========================
    // LOGIQUE CONVOYEUR (CONVEYOR)
    // =========================

    public boolean isConveyor() {
        return type == Type.CONVEYOR_BELT;
    }

    public boolean hasItem() {
        return heldItem != PlanterCrop.NONE;
    }

    public PlanterCrop getHeldItem() {
        return heldItem;
    }

    /**
     * Progression du transport de l'objet sur le tapis (0 à 1).
     * 0 = début du tapis, 1 = fin du tapis (prêt à sortir).
     */
    public float getTransportProgress() {
        if (!hasItem()) return 0f;
        return Math.min(1f, transportTimer / TRANSPORT_TIME);
    }

    /**
     * Le convoyeur peut-il recevoir un nouvel objet ?
     * Oui s'il est vide.
     */
    public boolean canReceiveItem() {
        return isConveyor() && !hasItem();
    }

    /**
     * Place un objet sur le convoyeur (entrée).
     */
    public void receiveItem(PlanterCrop item) {
        if (!canReceiveItem()) return;
        heldItem = item;
        transportTimer = 0f; // On commence au début du tapis
    }

    /**
     * Retire l'objet du convoyeur (sortie).
     * @return L'objet qui était sur le tapis.
     */
    public PlanterCrop takeItem() {
        if (!hasItem()) return PlanterCrop.NONE;
        PlanterCrop item = heldItem;
        heldItem = PlanterCrop.NONE;
        transportTimer = 0f;
        return item;
    }

    private void updateConveyor(float dt) {
        if (hasItem()) {
            transportTimer += dt;
            // On bloque à 100% tant que l'objet n'est pas pris par le suivant
            if (transportTimer > TRANSPORT_TIME) {
                transportTimer = TRANSPORT_TIME;
            }
        }
    }

    // =========================
    // LOGIQUE HQ (MAIN_HQ)
    // =========================

    public boolean isHQ() {
        return type == Type.MAIN_HQ;
    }

    public void addToHQStock(PlanterCrop crop, int amount) {
        if (!isHQ()) return;
        if (crop == PlanterCrop.TOMATO) hqTomatoes += amount;
        if (crop == PlanterCrop.WHEAT) hqWheat += amount;
    }

    public int getHQStock(PlanterCrop crop) {
        if (!isHQ()) return 0;
        if (crop == PlanterCrop.TOMATO) return hqTomatoes;
        if (crop == PlanterCrop.WHEAT) return hqWheat;
        return 0;
    }

    public int removeFromHQStock(PlanterCrop crop, int amount) {
        if (!isHQ()) return 0;
        if (amount <= 0) return 0;

        if (crop == PlanterCrop.TOMATO) {
            int sold = Math.min(amount, hqTomatoes);
            hqTomatoes -= sold;
            return sold;
        }
        if (crop == PlanterCrop.WHEAT) {
            int sold = Math.min(amount, hqWheat);
            hqWheat -= sold;
            return sold;
        }
        return 0;
    }

    public void clearHQStock() {
        hqTomatoes = 0;
        hqWheat = 0;
    }
}
