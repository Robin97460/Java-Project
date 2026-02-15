package com.github.lxquaver.stardisfactory;

public class Crop {

    public enum Type {
        WHEAT(5.0f),  // Blé
        CORN(10.0f);  // Maïs

        public final float growthTimeSeconds;

        Type(float growthTimeSeconds) {
            this.growthTimeSeconds = growthTimeSeconds;
        }
    }

    private Type type;
    private float currentGrowthTimer;
    private boolean isReady;

    public Crop(Type type) {
        this.type = type;
        this.currentGrowthTimer = 0f;
        this.isReady = false;
    }

    public void update(float deltaTime) {
        if (!isReady) {
            currentGrowthTimer += deltaTime;
            if (currentGrowthTimer >= type.growthTimeSeconds) {
                currentGrowthTimer = type.growthTimeSeconds;
                isReady = true;
            }
        }
    }

    public boolean isReady() {
        return isReady;
    }

    public Type getType() {
        return type;
    }
}
