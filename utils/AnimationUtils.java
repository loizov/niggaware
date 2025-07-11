package ru.niggaware.utils;

public class AnimationUtils {
    
    public static float smoothStep(float from, float to, float speed) {
        return from + (to - from) * speed;
    }
    
    public static float easeInOut(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
    
    public static float easeIn(float t) {
        return t * t;
    }
    
    public static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }
    
    public static class Animation {
        private float current;
        private float target;
        private float speed;
        
        public Animation(float initial, float speed) {
            this.current = initial;
            this.target = initial;
            this.speed = speed;
        }
        
        public void update() {
            current = smoothStep(current, target, speed);
        }
        
        public void setTarget(float target) {
            this.target = target;
        }
        
        public float getCurrent() {
            return current;
        }
        
        public boolean isFinished() {
            return Math.abs(current - target) < 0.01f;
        }
    }
}