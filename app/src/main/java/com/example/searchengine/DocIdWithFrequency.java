package com.example.searchengine;

import java.io.Serializable;

public class DocIdWithFrequency implements Serializable {
    private int docId;
    private double frequency;

    public DocIdWithFrequency(int docId, double frequency) {
        this.docId = docId;
        this.frequency = frequency;
    }

    public int getDocId() {
        return docId;
    }


    public void setDocId(int docId) {
        this.docId = docId;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }
}
