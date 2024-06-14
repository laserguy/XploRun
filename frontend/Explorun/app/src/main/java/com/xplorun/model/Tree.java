package com.xplorun.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;

@Entity
public class Tree {
    @Transient
    public  static final long SINGLE_ID = 1L;

    @Id(assignable = true)
    public long id;
    public int seed;
    public double progress;

    public Tree() {
    }

    public Tree(long id, int seed, double progress) {
        this.id = id;
        this.seed = seed;
        this.progress = progress;
    }
}
