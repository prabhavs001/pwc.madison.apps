package com.pwc.madison.core.models;

public class LevelModel {
    /**
     * level for docsearch
     */
    String level;

    /**
     * counter for docserch
     */
    int counter = 1;

    public LevelModel(String level){
        this.level = level;
    }

    /**
     * @return level
     */
    public String getLevel() {
        return level;
    }

    /**
     * set level for docsearch
     * @param level
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return counter
     */
    public int getCounter() {
        return counter;
    }

    /**
     * sets counter for docsearch
     * @param counter
     */
    public void setCounter(int counter) {
        this.counter = counter;
    }

}
