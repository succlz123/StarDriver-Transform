package org.succlz123.stardriver.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProcessClassNode implements Comparable<ProcessClassNode> {
    int count;
    ClassParameter value;
    List<ProcessClassNode> next = new ArrayList<>();

    public ProcessClassNode(ClassParameter value) {
        this.value = value;
    }

    @Override
    public int compareTo(@NotNull ProcessClassNode processClassNode) {
        return count - processClassNode.count;
    }
}
